package com.example.network.webrtc

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * WebRTC Signaling Broker for exchanging SDP Offers, SDP Answers,
 * and ICE Candidates between Host and Remote Participants.
 */
class WebRtcSignalingHub private constructor() {

  private val TAG = "WebRtcSignaling"
  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  sealed class SignalingSignal {
    data class Offer(
      val roomCode: String,
      val senderId: String,
      val targetPeerId: String,
      val sessionDescription: RtcSessionDescription
    ) : SignalingSignal()

    data class Answer(
      val roomCode: String,
      val senderId: String,
      val targetPeerId: String,
      val sessionDescription: RtcSessionDescription
    ) : SignalingSignal()

    data class IceCandidate(
      val roomCode: String,
      val senderId: String,
      val targetPeerId: String,
      val candidate: RtcIceCandidate
    ) : SignalingSignal()

    data class PeerJoined(
      val roomCode: String,
      val peerId: String,
      val isHost: Boolean
    ) : SignalingSignal()

    data class PeerLeft(
      val roomCode: String,
      val peerId: String
    ) : SignalingSignal()
  }

  // Broadcast bus for room signaling
  private val _signals = MutableSharedFlow<SignalingSignal>(extraBufferCapacity = 64)
  val signals: SharedFlow<SignalingSignal> = _signals.asSharedFlow()

  // Active rooms registry
  private val activeRooms = ConcurrentHashMap<String, MutableSet<String>>()

  fun registerPeer(roomCode: String, peerId: String, isHost: Boolean) {
    val cleanCode = roomCode.trim().uppercase()
    val peers = activeRooms.getOrPut(cleanCode) { ConcurrentHashMap.newKeySet() }
    peers.add(peerId)
    Log.d(TAG, "Peer $peerId registered in room $cleanCode (total: ${peers.size})")

    scope.launch {
      _signals.emit(SignalingSignal.PeerJoined(cleanCode, peerId, isHost))
    }
  }

  fun unregisterPeer(roomCode: String, peerId: String) {
    val cleanCode = roomCode.trim().uppercase()
    activeRooms[cleanCode]?.remove(peerId)
    Log.d(TAG, "Peer $peerId unregistered from room $cleanCode")

    scope.launch {
      _signals.emit(SignalingSignal.PeerLeft(cleanCode, peerId))
    }
  }

  fun sendOffer(roomCode: String, senderId: String, targetPeerId: String, offer: RtcSessionDescription) {
    Log.d(TAG, "Routing SDP Offer from $senderId -> $targetPeerId in $roomCode")
    scope.launch {
      _signals.emit(SignalingSignal.Offer(roomCode.uppercase(), senderId, targetPeerId, offer))
    }
  }

  fun sendAnswer(roomCode: String, senderId: String, targetPeerId: String, answer: RtcSessionDescription) {
    Log.d(TAG, "Routing SDP Answer from $senderId -> $targetPeerId in $roomCode")
    scope.launch {
      _signals.emit(SignalingSignal.Answer(roomCode.uppercase(), senderId, targetPeerId, answer))
    }
  }

  fun sendIceCandidate(roomCode: String, senderId: String, targetPeerId: String, candidate: RtcIceCandidate) {
    scope.launch {
      _signals.emit(SignalingSignal.IceCandidate(roomCode.uppercase(), senderId, targetPeerId, candidate))
    }
  }

  companion object {
    val instance: WebRtcSignalingHub by lazy { WebRtcSignalingHub() }
  }
}
