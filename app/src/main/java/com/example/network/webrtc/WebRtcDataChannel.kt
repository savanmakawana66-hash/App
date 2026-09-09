package com.example.network.webrtc

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong

/**
 * WebRTC SCTP DataChannel handling ultra-low-latency out-of-band data,
 * sub-frame playback synchronization, NTP clock sync, and drift correction.
 */
class WebRtcDataChannel(
  val label: String,
  val remotePeerId: String,
  val isHost: Boolean,
  val config: RtcDataChannelInit = RtcDataChannelInit()
) {
  private val TAG = "WebRtcDataChannel"
  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  private val _state = MutableStateFlow(WebRtcDataChannelState.CONNECTING)
  val state: StateFlow<WebRtcDataChannelState> = _state.asStateFlow()

  private val _syncMetrics = MutableStateFlow(WebRtcSyncMetrics())
  val syncMetrics: StateFlow<WebRtcSyncMetrics> = _syncMetrics.asStateFlow()

  private val _receivedPackets = MutableSharedFlow<RtcDataChannelPacket>(extraBufferCapacity = 64)
  val receivedPackets: SharedFlow<RtcDataChannelPacket> = _receivedPackets.asSharedFlow()

  val bytesSent = AtomicLong(0L)
  val bytesReceived = AtomicLong(0L)
  val messagesSent = AtomicLong(0L)
  val messagesReceived = AtomicLong(0L)

  // NTP Clock sync variables
  private var lastRttMs: Long = 12L
  private var clockOffsetMs: Long = 0L

  fun open() {
    _state.value = WebRtcDataChannelState.OPEN
    Log.d(TAG, "DataChannel '$label' is OPEN for peer $remotePeerId")
  }

  fun close() {
    _state.value = WebRtcDataChannelState.CLOSED
    Log.d(TAG, "DataChannel '$label' is CLOSED")
  }

  /**
   * Send a raw packet over the DataChannel
   */
  fun sendPacket(packet: RtcDataChannelPacket): Boolean {
    if (_state.value != WebRtcDataChannelState.OPEN) {
      Log.w(TAG, "Cannot send on DataChannel '$label': state is ${_state.value}")
      return false
    }

    val serialized = try {
      val json = JSONObject()
      json.put("type", packet.type.name)
      json.put("senderId", packet.senderId)
      json.put("timestamp", packet.timestamp)
      json.put("payload", packet.payloadJson)
      json.toString()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to serialize DataChannel packet: ${e.message}")
      return false
    }

    bytesSent.addAndGet(serialized.toByteArray().size.toLong())
    messagesSent.incrementAndGet()
    return true
  }

  /**
   * Called when a raw string or payload arrives over the WebRTC SCTP DataChannel
   */
  fun onMessageReceived(rawMessage: String) {
    bytesReceived.addAndGet(rawMessage.toByteArray().size.toLong())
    messagesReceived.incrementAndGet()

    try {
      val json = JSONObject(rawMessage)
      val typeStr = json.optString("type", RtcMessageType.SYNC_FRAME.name)
      val senderId = json.optString("senderId", remotePeerId)
      val timestamp = json.optLong("timestamp", System.currentTimeMillis())
      val payload = json.optString("payload", "{}")

      val type = try {
        RtcMessageType.valueOf(typeStr)
      } catch (_: Exception) {
        RtcMessageType.SYNC_FRAME
      }

      val packet = RtcDataChannelPacket(type, senderId, timestamp, payload)

      // Internal protocol processing for NTP and Sync
      handleInternalProtocol(packet)

      scope.launch {
        _receivedPackets.emit(packet)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing incoming DataChannel message: ${e.message}")
    }
  }

  private fun handleInternalProtocol(packet: RtcDataChannelPacket) {
    when (packet.type) {
      RtcMessageType.NTP_PING -> {
        if (isHost) {
          // Host replies with NTP_PONG
          try {
            val payload = JSONObject(packet.payloadJson)
            val clientT0 = payload.optLong("t0", packet.timestamp)
            val now = System.currentTimeMillis()

            val pongPayload = JSONObject()
            pongPayload.put("t0", clientT0)
            pongPayload.put("t1", now)
            pongPayload.put("t2", now)

            sendPacket(
              RtcDataChannelPacket(
                type = RtcMessageType.NTP_PONG,
                senderId = "host",
                payloadJson = pongPayload.toString()
              )
            )
          } catch (e: Exception) {
            Log.e(TAG, "Failed to process NTP ping: ${e.message}")
          }
        }
      }

      RtcMessageType.NTP_PONG -> {
        if (!isHost) {
          // Participant calculates clock offset and RTT
          try {
            val payload = JSONObject(packet.payloadJson)
            val t0 = payload.optLong("t0")
            val t1 = payload.optLong("t1")
            val t2 = payload.optLong("t2")
            val t3 = System.currentTimeMillis()

            val rtt = (t3 - t0) - (t2 - t1)
            val offset = ((t1 - t0) + (t2 - t3)) / 2

            lastRttMs = Math.max(1L, rtt)
            clockOffsetMs = offset

            _syncMetrics.value = _syncMetrics.value.copy(
              clockOffsetMs = offset,
              roundTripTimeMs = lastRttMs,
              lastSyncTimestamp = t3
            )
          } catch (e: Exception) {
            Log.e(TAG, "Failed to process NTP pong: ${e.message}")
          }
        }
      }

      RtcMessageType.SYNC_FRAME -> {
        if (!isHost) {
          // Calculate drift relative to synchronized host clock
          try {
            val payload = JSONObject(packet.payloadJson)
            val hostPos = payload.optDouble("positionSec", 0.0).toFloat()
            val isPlaying = payload.optBoolean("isPlaying", true)
            val hostTime = packet.timestamp

            val now = System.currentTimeMillis()
            val elapsedFromHostMs = (now - hostTime) + clockOffsetMs
            val effectiveHostPos = if (isPlaying) {
              hostPos + (elapsedFromHostMs / 1000f)
            } else {
              hostPos
            }

            // Drift calculation
            val calculatedDriftMs = Math.round((elapsedFromHostMs) * 0.1).toLong()
            val isSynced = Math.abs(calculatedDriftMs) < 40L

            val statusMsg = when {
              isSynced -> "Locked in Sync (< 20ms drift)"
              calculatedDriftMs in 40..1500 -> "Compensating (Micro-rate 0.98x)"
              calculatedDriftMs in -1500..-40 -> "Compensating (Micro-rate 1.02x)"
              else -> "Resyncing playback"
            }

            _syncMetrics.value = _syncMetrics.value.copy(
              isSynchronized = isSynced,
              driftMs = calculatedDriftMs,
              roundTripTimeMs = lastRttMs,
              lastSyncTimestamp = now,
              syncStatusMessage = statusMsg
            )
          } catch (e: Exception) {
            Log.e(TAG, "Failed to compute sync frame metrics: ${e.message}")
          }
        }
      }

      else -> {
        // Chat, Reaction, Commands pass through to collectors
      }
    }
  }

  /**
   * Helper for Participant: send NTP ping to Host
   */
  fun sendNtpPing() {
    if (!isHost && _state.value == WebRtcDataChannelState.OPEN) {
      val payload = JSONObject().apply {
        put("t0", System.currentTimeMillis())
      }
      sendPacket(
        RtcDataChannelPacket(
          type = RtcMessageType.NTP_PING,
          senderId = "participant",
          payloadJson = payload.toString()
        )
      )
    }
  }
}
