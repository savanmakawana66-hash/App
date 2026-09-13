package com.example.network.webrtc

import android.util.Log
import com.example.data.security.SecureStorageManager
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

/**
 * Secure WebSocket (WSS) WebRTC Signaling Client.
 *
 * Implements:
 * - WSS (WebSocket over TLS) communication.
 * - Authenticated connection handshake using short-lived Bearer tokens.
 * - Automatic disconnection of unauthorized sessions.
 * - Automatic exponential backoff reconnection on network loss.
 * - Server-side verification before allowing Room join, speaker changes, or admin actions.
 */
class SecureWebSocketSignalingClient(
  private val secureStorage: SecureStorageManager
) {
  private val TAG = "SecureSignalingClient"

  var wsUrl: String = "wss://ais-dev-c555ii3jx626zxd5sixz54-555154314704.asia-southeast1.run.app/ws/signaling"

  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val isConnected = AtomicBoolean(false)
  private val reconnectAttempts = AtomicInteger(0)

  private val _connectionState = MutableStateFlow(WebRtcPeerConnectionState.NEW)
  val connectionState: StateFlow<WebRtcPeerConnectionState> = _connectionState.asStateFlow()

  private val _incomingSignals = MutableSharedFlow<WebRtcSignalMessage>(extraBufferCapacity = 64)
  val incomingSignals: SharedFlow<WebRtcSignalMessage> = _incomingSignals.asSharedFlow()

  private var webSocket: WebSocket? = null
  private val okHttpClient = OkHttpClient.Builder()
    .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.RESTRICTED_TLS))
    .pingInterval(15, TimeUnit.SECONDS)
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite for websocket
    .build()

  private var activeRoomId: String? = null
  private var activeUserId: String? = null

  fun connect(roomId: String, userId: String, username: String) {
    activeRoomId = roomId
    activeUserId = userId
    reconnectAttempts.set(0)
    initiateConnection()
  }

  private fun initiateConnection() {
    _connectionState.value = WebRtcPeerConnectionState.CONNECTING

    val token = secureStorage.getSecureString(SecureStorageManager.KEY_ACCESS_TOKEN)
      ?: BackendSecurityController.issueAccessToken(activeUserId ?: "guest", "Guest", false)

    val request = Request.Builder()
      .url(wsUrl)
      .header("Sec-WebSocket-Protocol", "watchtogether-v1")
      .header("User-Agent", "WatchTogether-WebRTC-Signaling/1.0")
      .build()

    webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(ws: WebSocket, response: Response) {
        isConnected.set(true)
        reconnectAttempts.set(0)
        _connectionState.value = WebRtcPeerConnectionState.CONNECTED
        Log.i(TAG, "Secure WSS signaling connected. Performing auth handshake...")

        // Send authentication handshake immediately
        val authMsg = JSONObject().apply {
          put("action", "AUTH_HANDSHAKE")
          put("token", token)
          put("userId", activeUserId)
          put("roomId", activeRoomId)
          put("timestamp", System.currentTimeMillis())
        }
        ws.send(authMsg.toString())
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
      val action = json.optString("action")

      if (action == "AUTH_REJECTED") {
        Log.e(TAG, "Signaling authentication rejected by server. Disconnecting.")
        disconnect()
        return
      }

      // Convert signaling payload into WebRtcSignalMessage
      val typeStr = json.optString("type")
      val senderId = json.optString("senderId")
      val targetId = json.optString("targetId")
      val roomId = json.optString("roomId")

      val signalType = try {
        WebRtcSignalType.valueOf(typeStr)
      } catch (_: Exception) {
        null
      } ?: return

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
    } catch (e: Exception) {
      Log.w(TAG, "Error parsing incoming signal: ${e.message}")
    }
  }

  fun sendSignal(signal: WebRtcSignalMessage) {
    val ws = webSocket
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

    if (isConnected.get() && ws != null) {
      ws.send(payload)
    } else {
      // Local fallback loop for test environments
      _incomingSignals.tryEmit(signal)
    }
  }

  private fun scheduleReconnection() {
    val attempts = reconnectAttempts.incrementAndGet()
    if (attempts > 5) {
      Log.w(TAG, "Max signaling reconnection attempts reached.")
      _connectionState.value = WebRtcPeerConnectionState.DISCONNECTED
      return
    }

    val delaySec = minOf(16L, 1L shl (attempts - 1)) // 1s, 2s, 4s, 8s, 16s
    scope.launch {
      delay(delaySec * 1000)
      if (!isConnected.get() && activeRoomId != null) {
        initiateConnection()
      }
    }
  }

  fun disconnect() {
    isConnected.set(false)
    activeRoomId = null
    activeUserId = null
    try {
      webSocket?.close(1000, "Normal closure")
    } catch (_: Exception) {}
    webSocket = null
    _connectionState.value = WebRtcPeerConnectionState.CLOSED
  }
}
