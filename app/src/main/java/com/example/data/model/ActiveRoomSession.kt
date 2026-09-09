package com.example.data.model

/**
 * Represents a real-time, currently active room session.
 * A room is considered active ONLY when:
 * - A real host created the room.
 * - The host is currently connected and transmitting heartbeats.
 * - The session is valid, unexpired, and not closed.
 */
data class ActiveRoomSession(
  val roomId: String,
  val name: String,
  val hostId: String,
  val hostName: String,
  val isHostConnected: Boolean = true,
  val connectionState: PartyConnectionState = PartyConnectionState.CONNECTED,
  val participantCount: Int = 1,
  val isPlaying: Boolean = true,
  val currentPositionSec: Float = 0f,
  val durationSec: Int = 1800,
  val mediaTitle: String = "Live Video Stream",
  val mediaType: StreamMediaType = StreamMediaType.VIDEO,
  val currentQuality: StreamQuality = StreamQuality.HIGH_ORIGINAL,
  val isPrivate: Boolean = false,
  val password: String = "",
  val createdAtMs: Long = System.currentTimeMillis(),
  val lastHeartbeatMs: Long = System.currentTimeMillis(),
  val isClosed: Boolean = false,
  val streamUrl: String = ""
) {
  /**
   * Heartbeat timeout threshold:
   * - Host sends a heartbeat every 3-5 seconds.
   * - If no heartbeat is received within 8 seconds, the host is marked as reconnecting.
   * - If no heartbeat is received within 18 seconds, the session is expired and removed.
   */
  val isExpired: Boolean
    get() = isClosed || (System.currentTimeMillis() - lastHeartbeatMs > 18000L)

  val isReconnecting: Boolean
    get() = !isClosed && (connectionState == PartyConnectionState.RECONNECTING ||
        (System.currentTimeMillis() - lastHeartbeatMs in 8000L..18000L))

  val formattedDuration: String
    get() {
      val m = durationSec / 60
      val s = durationSec % 60
      return String.format("%02d:%02d", m, s)
    }

  val formattedPosition: String
    get() {
      val sec = currentPositionSec.toInt()
      val m = sec / 60
      val s = sec % 60
      return String.format("%02d:%02d", m, s)
    }

  val secondsSinceLastHeartbeat: Long
    get() = ((System.currentTimeMillis() - lastHeartbeatMs) / 1000L).coerceAtLeast(0L)
}
