package com.example.network.webrtc

import android.util.Log
import com.example.data.model.LocalMediaItem
import com.example.data.model.User
import com.example.data.security.SecureStorageManager
import com.example.network.api.BackendServerConfig
import com.example.network.security.BackendSecurityController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class PlaybackSyncEvent(
  val isPlaying: Boolean,
  val positionSec: Float,
  val playbackSpeed: Float = 1.0f,
  val serverTimestamp: Long = System.currentTimeMillis(),
  val senderId: String? = null
)

/**
 * Secure WebSocket (WSS) WebRTC Signaling & Room Synchronization Client.
 *
 * Implements:
 * - WSS (WebSocket over TLS) communication with local cleartext fallback.
 * - Authenticated connection handshake using short-lived Bearer tokens.
 * - Automatic exponential backoff reconnection on network loss.
 * - Full room lifecycle: CREATE_ROOM, JOIN_ROOM, SYNC_PLAYBACK, MEDIA_CHANGED, HEARTBEAT.
 * - Voice audio chunk relay & speaking status.
 * - Ephemeral chat & emoji reactions relay.
 */
class SecureWebSocketSignalingClient(
  private val secureStorage: SecureStorageManager
) {
  private val TAG = "SecureSignalingClient"

  var wsUrl: String = BackendServerConfig.activeWsUrl

  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val isConnected = AtomicBoolean(false)
  private val reconnectAttempts = AtomicInteger(0)

  private val _connectionState = MutableStateFlow(WebRtcPeerConnectionState.NEW)
  val connectionState: StateFlow<WebRtcPeerConnectionState> = _connectionState.asStateFlow()

  // Event buses
  private val _incomingSignals = MutableSharedFlow<WebRtcSignalMessage>(extraBufferCapacity = 64)
  val incomingSignals: SharedFlow<WebRtcSignalMessage> = _incomingSignals.asSharedFlow()

  private val _playbackUpdates = MutableSharedFlow<PlaybackSyncEvent>(extraBufferCapacity = 32)
  val playbackUpdates: SharedFlow<PlaybackSyncEvent> = _playbackUpdates.asSharedFlow()

  private val _mediaChangeUpdates = MutableSharedFlow<JSONObject>(extraBufferCapacity = 16)
  val mediaChangeUpdates: SharedFlow<JSONObject> = _mediaChangeUpdates.asSharedFlow()

  private val _roomJoinedEvents = MutableSharedFlow<JSONObject>(extraBufferCapacity = 16)
  val roomJoinedEvents: SharedFlow<JSONObject> = _roomJoinedEvents.asSharedFlow()

  private val _roomCreatedEvents = MutableSharedFlow<JSONObject>(extraBufferCapacity = 16)
  val roomCreatedEvents: SharedFlow<JSONObject> = _roomCreatedEvents.asSharedFlow()

  private val _chatMessages = MutableSharedFlow<JSONObject>(extraBufferCapacity = 32)
  val chatMessages: SharedFlow<JSONObject> = _chatMessages.asSharedFlow()

  private val _reactionBursts = MutableSharedFlow<String>(extraBufferCapacity = 32)
  val reactionBursts: SharedFlow<String> = _reactionBursts.asSharedFlow()

  private val _voiceStatusEvents = MutableSharedFlow<JSONObject>(extraBufferCapacity = 32)
  val voiceStatusEvents: SharedFlow<JSONObject> = _voiceStatusEvents.asSharedFlow()

  private val _participantEvents = MutableSharedFlow<JSONObject>(extraBufferCapacity = 32)
  val participantEvents: SharedFlow<JSONObject> = _participantEvents.asSharedFlow()

  private var webSocket: WebSocket? = null
  private val okHttpClient = OkHttpClient.Builder()
    .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.RESTRICTED_TLS, ConnectionSpec.CLEARTEXT))
    .pingInterval(15, TimeUnit.SECONDS)
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite for websocket
    .build()

  private var activeRoomId: String? = null
  private var activeUserId: String? = null
  private var activeUsername: String? = null
  private var activePasswordAttempt: String = ""
  private var isHostRole: Boolean = false
  private var activeMediaInfo: LocalMediaItem? = null
  private var activeStreamUrl: String = ""

  fun connect(roomId: String, userId: String, username: String) {
    activeRoomId = roomId
    activeUserId = userId
    activeUsername = username
    reconnectAttempts.set(0)
    initiateConnection()
  }

  private fun initiateConnection() {
    _connectionState.value = WebRtcPeerConnectionState.CONNECTING

    val token = secureStorage.getSecureString(SecureStorageManager.KEY_ACCESS_TOKEN)
      ?: BackendSecurityController.issueAccessToken(activeUserId ?: "guest", activeUsername ?: "Guest", false)

    val targetUrl = BackendServerConfig.activeWsUrl
    Log.i(TAG, "Initiating WebSocket connection to: $targetUrl (room: $activeRoomId)")

    val request = Request.Builder()
      .url(targetUrl)
      .header("Sec-WebSocket-Protocol", "watchtogether-v1")
      .header("User-Agent", "WatchTogether-WebRTC-Signaling/1.0")
      .build()

    webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(ws: WebSocket, response: Response) {
        isConnected.set(true)
        reconnectAttempts.set(0)
        _connectionState.value = WebRtcPeerConnectionState.CONNECTED
        Log.i(TAG, "Secure WSS signaling connected! Performing room session sync...")

        // Send authentication handshake
        val authMsg = JSONObject().apply {
          put("action", "AUTH_HANDSHAKE")
          put("token", token)
          put("userId", activeUserId)
          put("roomId", activeRoomId)
          put("timestamp", System.currentTimeMillis())
        }
        ws.send(authMsg.toString())

        // If host, announce or re-announce room
        if (isHostRole && activeRoomId != null && activeMediaInfo != null) {
          sendCreateRoom(activeRoomId!!, activeMediaInfo!!, activeStreamUrl, activeUserId ?: "", activeUsername ?: "Host")
        } else if (!isHostRole && activeRoomId != null) {
          // If remote participant rejoining, send JOIN_ROOM
          sendJoinRoom(activeRoomId!!, activePasswordAttempt, activeUserId ?: "", activeUsername ?: "Guest")
        }
      }

      override fun onMessage(ws: WebSocket, text: String) {
        handleIncomingMessage(text)
      }

      override fun onClosing(ws: WebSocket, code: Int, reason: String) {
        isConnected.set(false)
        _connectionState.value = WebRtcPeerConnectionState.DISCONNECTED
        ws.close(code, reason)
      }

      override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
        isConnected.set(false)
        _connectionState.value = WebRtcPeerConnectionState.FAILED
        Log.w(TAG, "WSS connection failure: ${t.message}. Reconnecting with backoff...")
        scheduleReconnection()
      }
    })
  }

  private fun handleIncomingMessage(text: String) {
    try {
      val json = JSONObject(text)
      val type = json.optString("type").ifEmpty { json.optString("action") }

      when (type) {
        "AUTH_REJECTED" -> {
          Log.e(TAG, "Signaling authentication rejected by server. Disconnecting.")
          disconnect()
        }

        "ROOM_CREATED" -> {
          _roomCreatedEvents.tryEmit(json)
        }

        "ROOM_JOINED" -> {
          _roomJoinedEvents.tryEmit(json)
          // Extract playback state if provided
          if (json.has("playbackState")) {
            val pb = json.getJSONObject("playbackState")
            val isPlaying = pb.optBoolean("isPlaying", true)
            val pos = pb.optDouble("positionSec", 0.0).toFloat()
            val serverTs = pb.optLong("lastUpdated", System.currentTimeMillis())
            _playbackUpdates.tryEmit(
              PlaybackSyncEvent(isPlaying = isPlaying, positionSec = pos, serverTimestamp = serverTs)
            )
          }
        }

        "PLAYBACK_UPDATED" -> {
          val isPlaying = json.optBoolean("isPlaying", true)
          val pos = json.optDouble("positionSec", 0.0).toFloat()
          val speed = json.optDouble("playbackSpeed", 1.0).toFloat()
          val serverTs = json.optLong("serverTimestamp", System.currentTimeMillis())
          val senderId = json.optString("senderId")
          _playbackUpdates.tryEmit(
            PlaybackSyncEvent(
              isPlaying = isPlaying,
              positionSec = pos,
              playbackSpeed = speed,
              serverTimestamp = serverTs,
              senderId = senderId
            )
          )
        }

        "MEDIA_CHANGED" -> {
          _mediaChangeUpdates.tryEmit(json)
        }

        "CHAT_MESSAGE" -> {
          _chatMessages.tryEmit(json)
        }

        "REACTION_BURST" -> {
          val emoji = json.optString("emoji")
          if (emoji.isNotBlank()) {
            _reactionBursts.tryEmit(emoji)
          }
        }

        "VOICE_STATUS_CHANGED" -> {
          _voiceStatusEvents.tryEmit(json)
        }

        "PARTICIPANT_JOINED", "PARTICIPANT_LEFT", "PARTICIPANT_REMOVED", "HOST_DISCONNECTED" -> {
          _participantEvents.tryEmit(json)
        }

        // WebRTC Signals
        "SIGNALING_OFFER", "SIGNALING_ANSWER", "SIGNALING_ICE", "SIGNAL" -> {
          val senderId = json.optString("senderId")
          val targetId = json.optString("targetId")
          val roomId = json.optString("roomId")

          val signalType = when (type) {
            "SIGNALING_OFFER", "OFFER" -> WebRtcSignalType.OFFER
            "SIGNALING_ANSWER", "ANSWER" -> WebRtcSignalType.ANSWER
            "SIGNALING_ICE", "ICE_CANDIDATE" -> WebRtcSignalType.ICE_CANDIDATE
            "PEER_JOINED" -> WebRtcSignalType.PEER_JOINED
            "PEER_LEFT" -> WebRtcSignalType.PEER_LEFT
            "HEARTBEAT" -> WebRtcSignalType.HEARTBEAT
            else -> WebRtcSignalType.OFFER
          }

          val signal = WebRtcSignalMessage(
            type = signalType,
            senderId = senderId,
            targetId = targetId,
            roomId = roomId,
            sdp = json.optString("sdp").takeIf { it.isNotEmpty() },
            candidate = json.optString("candidate").takeIf { it.isNotEmpty() },
            sdpMid = json.optString("sdpMid").takeIf { it.isNotEmpty() },
            sdpMLineIndex = if (json.has("sdpMLineIndex")) json.getInt("sdpMLineIndex") else null,
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
          )
          _incomingSignals.tryEmit(signal)
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error parsing incoming signal: ${e.message}")
    }
  }

  // -------------------------------------------------------------
  // OUTGOING ROOM ACTIONS
  // -------------------------------------------------------------

  fun sendCreateRoom(
    roomCode: String,
    media: LocalMediaItem,
    streamUrl: String,
    userId: String,
    username: String,
    password: String = ""
  ) {
    activeRoomId = roomCode
    activeUserId = userId
    activeUsername = username
    isHostRole = true
    activeMediaInfo = media
    activeStreamUrl = streamUrl

    val payload = JSONObject().apply {
      put("type", "CREATE_ROOM")
      put("roomCode", roomCode)
      put("streamUrl", streamUrl)
      put("password", password)
      put("user", JSONObject().apply {
        put("id", userId)
        put("username", username)
      })
      put("mediaInfo", JSONObject().apply {
        put("title", media.title)
        put("durationSec", media.durationSec)
        put("fileSizeBytes", media.fileSizeBytes)
        put("mimeType", media.mimeType)
        put("mediaType", media.mediaType.name)
        put("uriString", media.uriString)
      })
      put("timestamp", System.currentTimeMillis())
    }
    sendJson(payload.toString())
  }

  fun sendJoinRoom(
    roomCode: String,
    password: String,
    userId: String,
    username: String
  ) {
    activeRoomId = roomCode
    activeUserId = userId
    activeUsername = username
    activePasswordAttempt = password
    isHostRole = false

    val payload = JSONObject().apply {
      put("type", "JOIN_ROOM")
      put("roomCode", roomCode)
      put("password", password)
      put("user", JSONObject().apply {
        put("id", userId)
        put("username", username)
      })
      put("timestamp", System.currentTimeMillis())
    }
    sendJson(payload.toString())
  }

  fun sendSyncPlayback(isPlaying: Boolean, positionSec: Float, playbackSpeed: Float = 1.0f) {
    val payload = JSONObject().apply {
      put("type", "SYNC_PLAYBACK")
      put("isPlaying", isPlaying)
      put("positionSec", positionSec)
      put("playbackSpeed", playbackSpeed)
      put("timestamp", System.currentTimeMillis())
    }
    sendJson(payload.toString())
  }

  fun sendMediaChanged(media: LocalMediaItem, streamUrl: String, positionSec: Float = 0f) {
    activeMediaInfo = media
    activeStreamUrl = streamUrl
    val payload = JSONObject().apply {
      put("type", "MEDIA_CHANGED")
      put("streamUrl", streamUrl)
      put("positionSec", positionSec)
      put("mediaInfo", JSONObject().apply {
        put("title", media.title)
        put("durationSec", media.durationSec)
        put("fileSizeBytes", media.fileSizeBytes)
        put("mimeType", media.mimeType)
        put("mediaType", media.mediaType.name)
        put("uriString", media.uriString)
      })
      put("timestamp", System.currentTimeMillis())
    }
    sendJson(payload.toString())
  }

  fun sendChatMessage(message: String) {
    val payload = JSONObject().apply {
      put("type", "CHAT_MESSAGE")
      put("message", message)
      put("timestamp", System.currentTimeMillis())
    }
    sendJson(payload.toString())
  }

  fun sendReaction(emoji: String) {
    val payload = JSONObject().apply {
      put("type", "REACTION_BURST")
      put("emoji", emoji)
      put("timestamp", System.currentTimeMillis())
    }
    sendJson(payload.toString())
  }

  fun sendVoiceSpeaking(isSpeaking: Boolean, isMuted: Boolean) {
    val payload = JSONObject().apply {
      put("type", "VOICE_SPEAKING_STATUS")
      put("isSpeaking", isSpeaking)
      put("isMuted", isMuted)
    }
    sendJson(payload.toString())
  }

  fun sendVoiceAudioData(audioData: String) {
    val payload = JSONObject().apply {
      put("type", "VOICE_AUDIO_DATA")
      put("audioData", audioData)
    }
    sendJson(payload.toString())
  }

  fun sendHeartbeat(positionSec: Float, isPlaying: Boolean) {
    val payload = JSONObject().apply {
      put("type", "HEARTBEAT")
      put("positionSec", positionSec)
      put("isPlaying", isPlaying)
      put("timestamp", System.currentTimeMillis())
    }
    sendJson(payload.toString())
  }

  fun sendSignal(signal: WebRtcSignalMessage) {
    val payload = JSONObject().apply {
      put("action", "SIGNAL")
      put("type", signal.type.name)
      put("senderId", signal.senderId)
      put("targetId", signal.targetId)
      put("roomId", signal.roomId)
      put("sdp", signal.sdp)
      put("candidate", signal.candidate)
      put("sdpMid", signal.sdpMid)
      signal.sdpMLineIndex?.let { put("sdpMLineIndex", it) }
      put("timestamp", signal.timestamp)
    }.toString()

    sendJson(payload)
  }

  private fun sendJson(jsonStr: String) {
    val ws = webSocket
    if (isConnected.get() && ws != null) {
      ws.send(jsonStr)
    }
  }

  private fun scheduleReconnection() {
    val attempts = reconnectAttempts.incrementAndGet()
    if (attempts > 8) {
      Log.w(TAG, "Max signaling reconnection attempts reached.")
      _connectionState.value = WebRtcPeerConnectionState.DISCONNECTED
      return
    }

    _connectionState.value = WebRtcPeerConnectionState.CONNECTING
    val delaySec = minOf(16L, 1L shl (attempts - 1).coerceAtMost(4)) // 1s, 2s, 4s, 8s, 16s
    scope.launch {
      delay(delaySec * 1000)
      if (!isConnected.get() && activeRoomId != null) {
        Log.i(TAG, "Reconnecting attempt #$attempts to $wsUrl...")
        initiateConnection()
      }
    }
  }

  fun disconnect() {
    isConnected.set(false)
    activeRoomId = null
    activeUserId = null
    activeUsername = null
    isHostRole = false
    try {
      webSocket?.close(1000, "Normal closure")
    } catch (_: Exception) {}
    webSocket = null
    _connectionState.value = WebRtcPeerConnectionState.CLOSED
  }
}
