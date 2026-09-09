package com.example.network.webrtc

/**
 * WebRTC Protocol and PeerConnection specifications for low-latency
 * peer-to-peer audio, video, and DataChannel synchronization.
 */

enum class WebRtcPeerConnectionState(val label: String) {
  NEW("Initializing"),
  CONNECTING("Establishing P2P"),
  CONNECTED("WebRTC P2P Connected"),
  DISCONNECTED("P2P Disconnected"),
  FAILED("P2P Failed"),
  CLOSED("P2P Closed")
}

enum class WebRtcIceConnectionState(val label: String) {
  NEW("New"),
  CHECKING("Checking ICE Candidates"),
  CONNECTED("ICE Connected"),
  COMPLETED("ICE Completed"),
  FAILED("ICE Failed"),
  DISCONNECTED("ICE Disconnected"),
  CLOSED("ICE Closed")
}

enum class WebRtcSignalingState {
  STABLE,
  HAVE_LOCAL_OFFER,
  HAVE_REMOTE_OFFER,
  HAVE_LOCAL_PRANSWER,
  HAVE_REMOTE_PRANSWER,
  CLOSED
}

enum class WebRtcDataChannelState {
  CONNECTING,
  OPEN,
  CLOSING,
  CLOSED
}

enum class SdpType {
  OFFER,
  ANSWER,
  PRANSWER,
  ROLLBACK
}

data class RtcIceServer(
  val urls: List<String>,
  val username: String? = null,
  val credential: String? = null
) {
  companion object {
    val DEFAULT_STUN_SERVERS = listOf(
      RtcIceServer(urls = listOf("stun:stun.l.google.com:19302")),
      RtcIceServer(urls = listOf("stun:stun1.l.google.com:19302")),
      RtcIceServer(urls = listOf("stun:stun2.l.google.com:19302")),
      RtcIceServer(urls = listOf("stun:stun.cloudflare.com:3478"))
    )
  }
}

data class RtcSessionDescription(
  val type: SdpType,
  val sdp: String
)

data class RtcIceCandidate(
  val sdpMid: String,
  val sdpMLineIndex: Int,
  val candidate: String
)

data class RtcDataChannelInit(
  val ordered: Boolean = true,
  val maxRetransmitTimeMs: Int = -1,
  val maxRetransmits: Int = -1,
  val protocol: String = "watch-party-sync-v1",
  val negotiated: Boolean = false,
  val id: Int = -1
)

/**
 * Live WebRTC RTCStats report metrics (RTT, packet loss, jitter, bitrate, fps)
 */
data class WebRtcStats(
  val peerId: String = "",
  val rttMs: Long = 18L,
  val jitterMs: Double = 1.4,
  val packetLossPercent: Double = 0.0,
  val bitrateKbps: Double = 3450.0,
  val frameRateFps: Int = 60,
  val resolution: String = "1920x1080",
  val audioCodec: String = "Opus (48 kHz stereo)",
  val videoCodec: String = "H.264 High Profile (Level 4.2)",
  val bytesSent: Long = 0L,
  val bytesReceived: Long = 0L,
  val audioLevel: Float = 0.5f,
  val syncDriftMs: Long = 6L,
  val iceCandidatePair: String = "Local UDP <-> STUN srflx (Google STUN)",
  val protocol: String = "WebRTC / SRTP + SCTP DataChannel"
) {
  val formattedBitrate: String
    get() = String.format("%.1f Mbps", bitrateKbps / 1000.0)

  val formattedRtt: String
    get() = "${rttMs} ms"

  val formattedDrift: String
    get() = if (Math.abs(syncDriftMs) < 20) {
      "Sub-Frame (${syncDriftMs} ms)"
    } else {
      "${syncDriftMs} ms"
    }
}

/**
 * Synchronization metrics between Host clock and Remote Participant playback
 */
data class WebRtcSyncMetrics(
  val isSynchronized: Boolean = true,
  val clockOffsetMs: Long = 0L,
  val roundTripTimeMs: Long = 14L,
  val driftMs: Long = 8L,
  val playbackRate: Float = 1.0f,
  val lastSyncTimestamp: Long = System.currentTimeMillis(),
  val syncStatusMessage: String = "Synchronized (< 15ms drift)"
)

/**
 * High-speed DataChannel Message Types
 */
enum class RtcMessageType {
  SYNC_FRAME,
  NTP_PING,
  NTP_PONG,
  SEEK_COMMAND,
  PLAY_PAUSE_COMMAND,
  REACTION_BURST,
  CHAT_MESSAGE,
  QUALITY_ADAPTATION,
  MEDIA_METADATA
}

/**
 * Base protocol message serialized over SCTP DataChannel
 */
data class RtcDataChannelPacket(
  val type: RtcMessageType,
  val senderId: String,
  val timestamp: Long = System.currentTimeMillis(),
  val payloadJson: String
)
