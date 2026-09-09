package com.example.network

import android.util.Log
import com.example.data.model.ActiveRoomSession
import com.example.data.model.PartyConnectionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Real-time Active Room Session Backend & Heartbeat Registry.
 *
 * Enforces strict active room rules:
 * - NO fake, demo, sample, placeholder, or hardcoded rooms are ever seeded.
 * - Rooms appear in the active rooms list ONLY when a real host is currently connected,
 *   available, and broadcasting valid heartbeats.
 * - Heartbeat timeout: Host heartbeats are expected every 3-5 seconds.
 * - If heartbeat ceases for > 8s, room enters RECONNECTING state.
 * - If heartbeat ceases for > 18s or host leaves, room is immediately PRUNED.
 */
object ActiveRoomSessionManager {
  private const val TAG = "ActiveRoomManager"
  private const val HEARTBEAT_TIMEOUT_MS = 8000L      // 8s without heartbeat -> RECONNECTING
  private const val SESSION_EXPIRY_MS = 18000L         // 18s without heartbeat -> EXPIRED & REMOVED

  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  // Thread-safe session registry (Strictly starts EMPTY)
  private val sessions = ConcurrentHashMap<String, ActiveRoomSession>()

  private val _activeRooms = MutableStateFlow<List<ActiveRoomSession>>(emptyList())
  val activeRooms: StateFlow<List<ActiveRoomSession>> = _activeRooms.asStateFlow()

  init {
    startSessionReaper()
  }

  /**
   * Registers a newly created room from a real connected Host.
   */
  fun createSession(session: ActiveRoomSession) {
    val activeSession = session.copy(
      isHostConnected = true,
      connectionState = PartyConnectionState.CONNECTED,
      createdAtMs = System.currentTimeMillis(),
      lastHeartbeatMs = System.currentTimeMillis(),
      isClosed = false
    )
    sessions[session.roomId] = activeSession
    publishActiveRooms()
    Log.d(TAG, "Active room registered: ${session.roomId} ('${session.name}') by host ${session.hostName}")
  }

  /**
   * Processes a periodic heartbeat from the Host confirming:
   * "I'm still online and this room is active."
   */
  fun sendHeartbeat(
    roomId: String,
    hostId: String,
    isPlaying: Boolean? = null,
    positionSec: Float? = null,
    participantCount: Int? = null,
    connectionState: PartyConnectionState = PartyConnectionState.CONNECTED
  ) {
    val existing = sessions[roomId] ?: return
    if (existing.hostId != hostId || existing.isClosed) return

    val updated = existing.copy(
      lastHeartbeatMs = System.currentTimeMillis(),
      isHostConnected = connectionState == PartyConnectionState.CONNECTED,
      connectionState = connectionState,
      isPlaying = isPlaying ?: existing.isPlaying,
      currentPositionSec = positionSec ?: existing.currentPositionSec,
      participantCount = participantCount ?: existing.participantCount
    )
    sessions[roomId] = updated
    publishActiveRooms()
  }

  /**
   * Updates playback state in real-time.
   */
  fun updatePlaybackState(roomId: String, isPlaying: Boolean, positionSec: Float) {
    val existing = sessions[roomId] ?: return
    sessions[roomId] = existing.copy(
      isPlaying = isPlaying,
      currentPositionSec = positionSec,
      lastHeartbeatMs = System.currentTimeMillis()
    )
    publishActiveRooms()
  }

  /**
   * Updates participant count when someone joins or leaves.
   */
  fun updateParticipantCount(roomId: String, count: Int) {
    val existing = sessions[roomId] ?: return
    sessions[roomId] = existing.copy(
      participantCount = count.coerceAtLeast(1),
      lastHeartbeatMs = System.currentTimeMillis()
    )
    publishActiveRooms()
  }

  /**
   * Immediately marks a room as closed and removes it when host ends party or disconnects.
   */
  fun closeSession(roomId: String) {
    val existing = sessions.remove(roomId)
    if (existing != null) {
      Log.d(TAG, "Room session $roomId explicitly closed and removed from active list")
      publishActiveRooms()
    }
  }

  /**
   * Retrieves active session details if valid and unexpired.
   */
  fun getActiveSession(roomId: String): ActiveRoomSession? {
    val session = sessions[roomId] ?: return null
    return if (session.isExpired) {
      sessions.remove(roomId)
      publishActiveRooms()
      null
    } else {
      session
    }
  }

  /**
   * Validates if a user can join a room.
   * Rejects expired, closed, or offline rooms.
   */
  fun validateJoin(roomId: String, passwordAttempt: String = ""): Result<ActiveRoomSession> {
    val session = getActiveSession(roomId)
      ?: return Result.failure(IllegalStateException("This room is no longer active. The host is offline or the session has ended."))

    if (session.isClosed) {
      return Result.failure(IllegalStateException("This watch party has already ended."))
    }

    if (session.isExpired) {
      sessions.remove(roomId)
      publishActiveRooms()
      return Result.failure(IllegalStateException("This room session has expired due to host inactivity."))
    }

    if (!session.isHostConnected) {
      return Result.failure(IllegalStateException("The room host is currently disconnected."))
    }

    if (session.isPrivate && session.password.isNotBlank() && session.password != passwordAttempt) {
      return Result.failure(IllegalArgumentException("Incorrect room password."))
    }

    return Result.success(session)
  }

  /**
   * Real-time Reaper watchdog loop:
   * Periodically checks all sessions, marks reconnecting states, and purges stale/expired rooms.
   */
  private fun startSessionReaper() {
    scope.launch {
      while (isActive) {
        delay(2000L)
        val now = System.currentTimeMillis()
        var changed = false

        sessions.entries.forEach { entry ->
          val session = entry.value
          val elapsed = now - session.lastHeartbeatMs

          if (session.isClosed || elapsed > SESSION_EXPIRY_MS) {
            Log.d(TAG, "Pruning inactive/expired room: ${session.roomId} (elapsed: ${elapsed}ms)")
            sessions.remove(entry.key)
            changed = true
          } else if (elapsed > HEARTBEAT_TIMEOUT_MS && session.connectionState == PartyConnectionState.CONNECTED) {
            // Heartbeat delayed: mark as reconnecting
            sessions[entry.key] = session.copy(connectionState = PartyConnectionState.RECONNECTING)
            changed = true
          }
        }

        if (changed) {
          publishActiveRooms()
        }
      }
    }
  }

  private fun publishActiveRooms() {
    // Only emit non-closed, non-expired rooms
    val activeList = sessions.values
      .filter { !it.isClosed && !it.isExpired }
      .sortedByDescending { it.createdAtMs }
    _activeRooms.value = activeList
  }

  /**
   * For testing purposes: clears all sessions.
   */
  fun clearAllSessions() {
    sessions.clear()
    publishActiveRooms()
  }
}
