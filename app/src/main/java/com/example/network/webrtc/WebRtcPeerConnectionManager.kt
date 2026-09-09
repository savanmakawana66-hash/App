package com.example.network.webrtc

import android.content.Context
import android.util.Log
import com.example.data.model.LocalMediaItem
import com.example.data.model.StreamQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Top-level WebRTC PeerConnection Infrastructure Coordinator.
 * Manages host-to-peer real-time RTP video/audio transceivers, SCTP DataChannels,
 * NTP clock synchronization, and low-latency command broadcasts.
 */
class WebRtcPeerConnectionManager(
  private val context: Context,
  private val currentUserIdProvider: () -> String
) {
  private val TAG = "WebRtcManager"
  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  private val signalingHub = WebRtcSignalingHub.instance

  private val peerConnections = ConcurrentHashMap<String, WebRtcPeerConnection>()

  private val _connectionState = MutableStateFlow(WebRtcPeerConnectionState.NEW)
  val connectionState: StateFlow<WebRtcPeerConnectionState> = _connectionState.asStateFlow()

  private val _iceConnectionState = MutableStateFlow(WebRtcIceConnectionState.NEW)
  val iceConnectionState: StateFlow<WebRtcIceConnectionState> = _iceConnectionState.asStateFlow()

  private val _dataChannelState = MutableStateFlow(WebRtcDataChannelState.CONNECTING)
  val dataChannelState: StateFlow<WebRtcDataChannelState> = _dataChannelState.asStateFlow()

  private val _aggregateStats = MutableStateFlow(WebRtcStats())
  val aggregateStats: StateFlow<WebRtcStats> = _aggregateStats.asStateFlow()

  private val _syncMetrics = MutableStateFlow(WebRtcSyncMetrics())
  val syncMetrics: StateFlow<WebRtcSyncMetrics> = _syncMetrics.asStateFlow()

  private val _incomingPackets = MutableSharedFlow<RtcDataChannelPacket>(extraBufferCapacity = 64)
  val incomingPackets: SharedFlow<RtcDataChannelPacket> = _incomingPackets.asSharedFlow()

  private var activeRoomCode: String = ""
  private var isHostRole: Boolean = false
  private var activeMedia: LocalMediaItem? = null
  private var currentQuality: StreamQuality = StreamQuality.HIGH_ORIGINAL

  private var signalingJob: Job? = null
  private var hostSyncLoopJob: Job? = null
  private var ntpLoopJob: Job? = null
  private var statsAggregatorJob: Job? = null

  private val syncSequenceNumber = AtomicLong(0L)

  // Current playback state reference for host broadcasting
  @Volatile var currentPlaybackPosSec: Float = 0f
  @Volatile var isCurrentlyPlaying: Boolean = true

  /**
   * Host starts WebRTC PeerConnection Infrastructure
   */
  fun startHosting(
    roomCode: String,
    media: LocalMediaItem,
    quality: StreamQuality
  ) {
    leave()
    activeRoomCode = roomCode.trim().uppercase()
    isHostRole = true
    activeMedia = media
    currentQuality = quality
    currentPlaybackPosSec = 0f
    isCurrentlyPlaying = true

    val hostId = currentUserIdProvider()
    signalingHub.registerPeer(activeRoomCode, hostId, isHost = true)

    startSignalingListener(hostId)
    startHostSyncBroadcaster()
    startStatsAggregator()

    _connectionState.value = WebRtcPeerConnectionState.CONNECTED
    _iceConnectionState.value = WebRtcIceConnectionState.CONNECTED
    _dataChannelState.value = WebRtcDataChannelState.OPEN

    Log.d(TAG, "Host WebRTC PeerConnection infrastructure running for room $activeRoomCode")
  }

  /**
   * Remote participant connects to Host via WebRTC
   */
  fun joinAsParticipant(roomCode: String, hostId: String) {
    leave()
    activeRoomCode = roomCode.trim().uppercase()
    isHostRole = false

    val participantId = currentUserIdProvider()
    signalingHub.registerPeer(activeRoomCode, participantId, isHost = false)

    startSignalingListener(participantId)

    // Initiate WebRtcPeerConnection with host
    val hostConnection = getOrCreatePeerConnection(hostId, isHost = false)

    startNtpSyncLoop()
    startStatsAggregator()

    _connectionState.value = WebRtcPeerConnectionState.CONNECTING
    Log.d(TAG, "Participant $participantId establishing WebRTC connection to Host $hostId")
  }

  private fun startSignalingListener(localId: String) {
    signalingJob?.cancel()
    signalingJob = scope.launch {
      signalingHub.signals.collect { signal ->
        when (signal) {
          is WebRtcSignalingHub.SignalingSignal.PeerJoined -> {
            if (signal.roomCode == activeRoomCode && signal.peerId != localId && isHostRole) {
              Log.d(TAG, "New participant ${signal.peerId} detected. Generating SDP Offer.")
              val pc = getOrCreatePeerConnection(signal.peerId, isHost = true)
              val offer = pc.createOffer()
              signalingHub.sendOffer(activeRoomCode, localId, signal.peerId, offer)
            }
          }

          is WebRtcSignalingHub.SignalingSignal.Offer -> {
            if (signal.roomCode == activeRoomCode && signal.targetPeerId == localId && !isHostRole) {
              Log.d(TAG, "Received SDP Offer from Host ${signal.senderId}. Applying and creating SDP Answer.")
              val pc = getOrCreatePeerConnection(signal.senderId, isHost = false)
              val answer = pc.setRemoteDescriptionAndCreateAnswer(signal.sessionDescription)
              signalingHub.sendAnswer(activeRoomCode, localId, signal.senderId, answer)
            }
          }

          is WebRtcSignalingHub.SignalingSignal.Answer -> {
            if (signal.roomCode == activeRoomCode && signal.targetPeerId == localId && isHostRole) {
              Log.d(TAG, "Received SDP Answer from participant ${signal.senderId}.")
              peerConnections[signal.senderId]?.setRemoteAnswer(signal.sessionDescription)
            }
          }

          is WebRtcSignalingHub.SignalingSignal.IceCandidate -> {
            if (signal.roomCode == activeRoomCode && signal.targetPeerId == localId) {
              peerConnections[signal.senderId]?.addIceCandidate(signal.candidate)
            }
          }

          is WebRtcSignalingHub.SignalingSignal.PeerLeft -> {
            if (signal.roomCode == activeRoomCode) {
              peerConnections.remove(signal.peerId)?.close()
              Log.d(TAG, "Peer ${signal.peerId} left room. Closed connection.")
            }
          }
        }
      }
    }
  }

  private fun getOrCreatePeerConnection(peerId: String, isHost: Boolean): WebRtcPeerConnection {
    return peerConnections.getOrPut(peerId) {
      val pc = WebRtcPeerConnection(peerId = peerId, isHost = isHost)

      // Listen for generated ICE candidates to forward via signaling
      scope.launch {
        pc.generatedIceCandidates.collect { candidate ->
          signalingHub.sendIceCandidate(activeRoomCode, currentUserIdProvider(), peerId, candidate)
        }
      }

      // Listen for incoming DataChannel packets
      scope.launch {
        pc.dataChannel.receivedPackets.collect { packet ->
          _incomingPackets.emit(packet)
        }
      }

      // Forward data channel state
      scope.launch {
        pc.dataChannel.state.collect { state ->
          if (state == WebRtcDataChannelState.OPEN) {
            _dataChannelState.value = WebRtcDataChannelState.OPEN
          }
        }
      }

      // Forward sync metrics
      scope.launch {
        pc.dataChannel.syncMetrics.collect { metrics ->
          _syncMetrics.value = metrics
        }
      }

      pc
    }
  }

  /**
   * Host broadcast loop: sends high-rate sub-frame SYNC_FRAME packets (every 100ms)
   */
  private fun startHostSyncBroadcaster() {
    hostSyncLoopJob?.cancel()
    hostSyncLoopJob = scope.launch {
      while (isActive && isHostRole) {
        if (peerConnections.isNotEmpty()) {
          val payload = JSONObject().apply {
            put("positionSec", currentPlaybackPosSec.toDouble())
            put("isPlaying", isCurrentlyPlaying)
            put("seq", syncSequenceNumber.incrementAndGet())
            put("durationSec", activeMedia?.durationSec ?: 0)
            put("quality", currentQuality.name)
          }

          val packet = RtcDataChannelPacket(
            type = RtcMessageType.SYNC_FRAME,
            senderId = currentUserIdProvider(),
            payloadJson = payload.toString()
          )

          peerConnections.values.forEach { pc ->
            pc.dataChannel.sendPacket(packet)
          }
        }
        delay(100) // 10Hz high-precision sync beacon
      }
    }
  }

  /**
   * Remote participant NTP synchronization loop: calculates clock offset every 3 seconds
   */
  private fun startNtpSyncLoop() {
    ntpLoopJob?.cancel()
    ntpLoopJob = scope.launch {
      while (isActive && !isHostRole) {
        peerConnections.values.firstOrNull()?.dataChannel?.sendNtpPing()
        delay(3000)
      }
    }
  }

  /**
   * Periodic stats aggregator: aggregates RTCStats across all active peer connections
   */
  private fun startStatsAggregator() {
    statsAggregatorJob?.cancel()
    statsAggregatorJob = scope.launch {
      while (isActive) {
        val pcs = peerConnections.values.toList()
        if (pcs.isNotEmpty()) {
          val firstStats = pcs.first().stats.value
          val avgRtt = pcs.map { it.stats.value.rttMs }.average().toLong()
          val avgJitter = pcs.map { it.stats.value.jitterMs }.average()
          val totalSent = pcs.sumOf { it.stats.value.bytesSent }
          val totalRecv = pcs.sumOf { it.stats.value.bytesReceived }

          _aggregateStats.value = firstStats.copy(
            rttMs = Math.max(1L, avgRtt),
            jitterMs = String.format("%.2f", avgJitter).toDouble(),
            bytesSent = totalSent,
            bytesReceived = totalRecv,
            syncDriftMs = _syncMetrics.value.driftMs
          )

          _connectionState.value = pcs.first().connectionState.value
          _iceConnectionState.value = pcs.first().iceConnectionState.value
        } else if (isHostRole) {
          _aggregateStats.value = WebRtcStats(
            peerId = "host-local",
            rttMs = 4L,
            jitterMs = 0.5,
            bitrateKbps = currentQuality.bitrateKbps.toDouble(),
            frameRateFps = 60,
            resolution = "1920x1080 FHD",
            syncDriftMs = 0L,
            protocol = "WebRTC Host P2P (Broadcasting)"
          )
        }
        delay(1000)
      }
    }
  }

  /**
   * Broadcast instantaneous seek command over WebRTC DataChannel
   */
  fun broadcastSeek(positionSec: Float) {
    currentPlaybackPosSec = positionSec
    val payload = JSONObject().apply {
      put("positionSec", positionSec.toDouble())
      put("timestamp", System.currentTimeMillis())
    }
    val packet = RtcDataChannelPacket(
      type = RtcMessageType.SEEK_COMMAND,
      senderId = currentUserIdProvider(),
      payloadJson = payload.toString()
    )
    peerConnections.values.forEach { it.dataChannel.sendPacket(packet) }
  }

  /**
   * Broadcast instantaneous play/pause toggle over WebRTC DataChannel
   */
  fun broadcastPlayPause(isPlaying: Boolean, positionSec: Float) {
    isCurrentlyPlaying = isPlaying
    currentPlaybackPosSec = positionSec
    val payload = JSONObject().apply {
      put("isPlaying", isPlaying)
      put("positionSec", positionSec.toDouble())
      put("timestamp", System.currentTimeMillis())
    }
    val packet = RtcDataChannelPacket(
      type = RtcMessageType.PLAY_PAUSE_COMMAND,
      senderId = currentUserIdProvider(),
      payloadJson = payload.toString()
    )
    peerConnections.values.forEach { it.dataChannel.sendPacket(packet) }
  }

  /**
   * Broadcast reaction burst over WebRTC DataChannel
   */
  fun broadcastReaction(emoji: String, senderName: String) {
    val payload = JSONObject().apply {
      put("emoji", emoji)
      put("senderName", senderName)
    }
    val packet = RtcDataChannelPacket(
      type = RtcMessageType.REACTION_BURST,
      senderId = currentUserIdProvider(),
      payloadJson = payload.toString()
    )
    peerConnections.values.forEach { it.dataChannel.sendPacket(packet) }
  }

  /**
   * Broadcast live chat message over WebRTC DataChannel
   */
  fun broadcastChatMessage(messageId: String, senderName: String, text: String) {
    val payload = JSONObject().apply {
      put("id", messageId)
      put("senderName", senderName)
      put("text", text)
      put("timestamp", System.currentTimeMillis())
    }
    val packet = RtcDataChannelPacket(
      type = RtcMessageType.CHAT_MESSAGE,
      senderId = currentUserIdProvider(),
      payloadJson = payload.toString()
    )
    peerConnections.values.forEach { it.dataChannel.sendPacket(packet) }
  }

  /**
   * Adapt stream quality dynamically
   */
  fun adaptQuality(quality: StreamQuality) {
    currentQuality = quality
    val payload = JSONObject().apply {
      put("quality", quality.name)
      put("bitrateKbps", quality.bitrateKbps)
    }
    val packet = RtcDataChannelPacket(
      type = RtcMessageType.QUALITY_ADAPTATION,
      senderId = currentUserIdProvider(),
      payloadJson = payload.toString()
    )
    peerConnections.values.forEach { it.dataChannel.sendPacket(packet) }
  }

  fun leave() {
    signalingJob?.cancel()
    signalingJob = null
    hostSyncLoopJob?.cancel()
    hostSyncLoopJob = null
    ntpLoopJob?.cancel()
    ntpLoopJob = null
    statsAggregatorJob?.cancel()
    statsAggregatorJob = null

    if (activeRoomCode.isNotBlank()) {
      signalingHub.unregisterPeer(activeRoomCode, currentUserIdProvider())
    }

    peerConnections.values.forEach { it.close() }
    peerConnections.clear()

    _connectionState.value = WebRtcPeerConnectionState.CLOSED
    _iceConnectionState.value = WebRtcIceConnectionState.CLOSED
    _dataChannelState.value = WebRtcDataChannelState.CLOSED

    activeRoomCode = ""
    isHostRole = false
    activeMedia = null
  }
}
