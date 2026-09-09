package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.RoomCategory
import com.example.data.model.StreamMediaType
import com.example.ui.viewmodel.BottomNavTab
import com.example.ui.viewmodel.MainViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

  private fun createViewModel(): MainViewModel {
    val app = ApplicationProvider.getApplicationContext<Application>()
    return MainViewModel(app)
  }

  @Test
  fun testGuestUserCreation() {
    val viewModel = createViewModel()
    val user = viewModel.currentUser.value
    assertTrue("User should default to guest", user.isGuest)
    assertTrue("User ID should start with USR-", user.id.startsWith("USR-"))
    assertTrue("Username should start with Guest_", user.username.startsWith("Guest_"))
  }

  @Test
  fun testRoomListSeedingAndFiltering() {
    val viewModel = createViewModel()
    // By user mandate, no fake, demo, or placeholder rooms are seeded
    val initialRooms = viewModel.rooms.value
    assertTrue("Initial rooms should be empty (no mock data)", initialRooms.isEmpty())
    assertTrue("Active sessions should be empty initially", viewModel.activeRoomSessions.value.isEmpty())

    // Create a real room
    val newRoom = viewModel.createRoom("Movie Night", "Live stream movie night", false, "")
    assertNotNull("Created room should not be null", newRoom)
    assertEquals("Movie Night", newRoom.name)
    assertEquals(1, viewModel.rooms.value.size)

    // Verify it is registered in activeRoomSessions
    val activeSessions = viewModel.activeRoomSessions.value
    assertEquals("Should have 1 active room session", 1, activeSessions.size)
    assertEquals("Movie Night", activeSessions[0].name)
    assertTrue("Host should be connected", activeSessions[0].isHostConnected)

    // Cleanup
    viewModel.deleteRoom(newRoom.id)
    assertTrue("Active sessions should be empty after deletion", viewModel.activeRoomSessions.value.isEmpty())
  }

  @Test
  fun testBottomNavigationTabs() {
    val viewModel = createViewModel()
    assertEquals(BottomNavTab.ROOM, viewModel.currentTab.value)

    viewModel.selectTab(BottomNavTab.DM)
    assertEquals(BottomNavTab.DM, viewModel.currentTab.value)

    viewModel.selectTab(BottomNavTab.PROFILE)
    assertEquals(BottomNavTab.PROFILE, viewModel.currentTab.value)
  }

  @Test
  fun testRoomJoiningAndEightVoiceSeats() {
    val viewModel = createViewModel()

    // Non-active/unregistered room join should fail
    val invalidJoin = viewModel.joinRoom("NON_EXISTENT_999")
    assertFalse("Joining nonexistent room should fail", invalidJoin)

    // Create a real active room
    val room = viewModel.createRoom("Synchronized Audio Lounge", "8-Seat voice room", false, "")
    val joined = viewModel.joinRoom(room.id)
    assertTrue("Should successfully join active room", joined)
    assertEquals(room.id, viewModel.currentActiveRoom.value?.id)

    // Verify 8 voice seats exist
    val seats = viewModel.roomSeats.value
    assertEquals("Room must strictly have 8 seats", 8, seats.size)

    // Creator is seated in seat 0 as owner
    assertEquals(viewModel.currentUser.value.id, viewModel.roomSeats.value[0].userId)

    // Leave seat
    viewModel.leaveSeat()
    assertNull("Seat 0 should be vacant after leaving", viewModel.roomSeats.value[0].userId)

    // Take seat 1
    viewModel.takeSeat(1)
    assertEquals(viewModel.currentUser.value.id, viewModel.roomSeats.value[1].userId)

    // Toggle mic
    viewModel.toggleMicMute()
    assertTrue("Seat mic state should be toggled", viewModel.roomSeats.value[1].isMuted)

    // Leave room
    viewModel.leaveRoom()
    assertNull("Current active room should be null after leaving", viewModel.currentActiveRoom.value)

    // Delete room
    viewModel.deleteRoom(room.id)
  }

  @Test
  fun testSynchronizedVideoControls() {
    val viewModel = createViewModel()
    val room = viewModel.createRoom("Video Test Room", "Testing sync", false, "")
    viewModel.joinRoom(room.id)

    assertTrue("Room owner should be able to control video", viewModel.canControlVideo())

    val initialPlaying = viewModel.currentActiveRoom.value?.isVideoPlaying ?: false
    viewModel.togglePlayPause()
    assertEquals(!initialPlaying, viewModel.currentActiveRoom.value?.isVideoPlaying)

    viewModel.seekVideo(120f)
    assertEquals(120f, viewModel.currentActiveRoom.value?.activeVideoPosSec ?: 0f, 0.01f)

    viewModel.forward10()
    assertEquals(130f, viewModel.currentActiveRoom.value?.activeVideoPosSec ?: 0f, 0.01f)

    viewModel.rewind10()
    assertEquals(120f, viewModel.currentActiveRoom.value?.activeVideoPosSec ?: 0f, 0.01f)

    viewModel.leaveRoom()
    viewModel.deleteRoom(room.id)
  }

  @Test
  fun testOtpGuestConversion() {
    val viewModel = createViewModel()
    viewModel.verifyOtp("+1 555 123 4567", "123456")

    val updatedUser = viewModel.currentUser.value
    assertFalse("User should no longer be a guest after verification", updatedUser.isGuest)
    assertTrue("User should be verified", updatedUser.isVerified)
    assertEquals("+1 555 123 4567", updatedUser.phoneNumber)
  }

  @Test
  fun testStorageAccessFrameworkMediaPresetsAndResolution() {
    val viewModel = createViewModel()
    val presets = viewModel.networkEngine.getSampleMediaPresets()

    assertTrue("Sample media presets should not be empty", presets.isNotEmpty())
    val videoPreset = presets.first { it.mediaType == StreamMediaType.VIDEO }
    val audioPreset = presets.first { it.mediaType == StreamMediaType.AUDIO }

    assertNotNull("Video preset should exist", videoPreset)
    assertNotNull("Audio preset should exist", audioPreset)
    assertTrue("Video duration should be positive", videoPreset.durationSec > 0)
    assertTrue("Audio duration should be positive", audioPreset.durationSec > 0)
    assertTrue("Formatted size should not be empty", videoPreset.formattedSize.isNotBlank())
    assertTrue("Formatted duration should be formatted correctly", videoPreset.formattedDuration.contains("m"))
  }

  @Test
  fun testWebRtcPeerConnectionNegotiationAndSdp() {
    val hostPc = com.example.network.webrtc.WebRtcPeerConnection(peerId = "peer_test_1", isHost = true)
    val offer = hostPc.createOffer()

    assertNotNull("SDP Offer should not be null", offer)
    assertEquals(com.example.network.webrtc.SdpType.OFFER, offer.type)
    assertTrue("SDP Offer should contain audio m-line", offer.sdp.contains("m=audio"))
    assertTrue("SDP Offer should contain video m-line", offer.sdp.contains("m=video"))
    assertTrue("SDP Offer should contain datachannel m-line", offer.sdp.contains("m=application"))

    val participantPc = com.example.network.webrtc.WebRtcPeerConnection(peerId = "host_1", isHost = false)
    val answer = participantPc.setRemoteDescriptionAndCreateAnswer(offer)

    assertNotNull("SDP Answer should not be null", answer)
    assertEquals(com.example.network.webrtc.SdpType.ANSWER, answer.type)
    assertTrue("SDP Answer should contain audio m-line", answer.sdp.contains("m=audio"))
    assertTrue("SDP Answer should contain video m-line", answer.sdp.contains("m=video"))

    hostPc.setRemoteAnswer(answer)
    assertEquals(com.example.network.webrtc.WebRtcSignalingState.STABLE, hostPc.signalingState.value)
    assertEquals(com.example.network.webrtc.WebRtcSignalingState.STABLE, participantPc.signalingState.value)

    hostPc.close()
    participantPc.close()
  }

  @Test
  fun testWebRtcDataChannelAndNtpSync() {
    val hostDc = com.example.network.webrtc.WebRtcDataChannel(
      label = "watch-party-sync",
      remotePeerId = "peer_remote",
      isHost = true
    )
    val peerDc = com.example.network.webrtc.WebRtcDataChannel(
      label = "watch-party-sync",
      remotePeerId = "host_peer",
      isHost = false
    )

    hostDc.open()
    peerDc.open()
    assertEquals(com.example.network.webrtc.WebRtcDataChannelState.OPEN, hostDc.state.value)
    assertEquals(com.example.network.webrtc.WebRtcDataChannelState.OPEN, peerDc.state.value)

    // Send chat packet over DataChannel
    val packet = com.example.network.webrtc.RtcDataChannelPacket(
      type = com.example.network.webrtc.RtcMessageType.CHAT_MESSAGE,
      senderId = "host",
      payloadJson = """{"text":"Hello WebRTC!"}"""
    )
    val sent = hostDc.sendPacket(packet)
    assertTrue("Packet should be sent over open DataChannel", sent)
    assertTrue("Bytes sent counter should be positive", hostDc.bytesSent.get() > 0)

    // Simulate incoming sync frame on peer
    val syncJson = """{"type":"SYNC_FRAME","senderId":"host","timestamp":${System.currentTimeMillis()},"payload":"{\"positionSec\":45.0,\"isPlaying\":true}"}"""
    peerDc.onMessageReceived(syncJson)

    assertTrue("Peer should register received bytes", peerDc.bytesReceived.get() > 0)
    assertNotNull("Sync metrics should be calculated", peerDc.syncMetrics.value)
    assertTrue("Drift status message should exist", peerDc.syncMetrics.value.syncStatusMessage.isNotBlank())

    hostDc.close()
    peerDc.close()
  }

  @Test
  fun testWebRtcPeerConnectionManagerHostingAndJoining() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val manager = com.example.network.webrtc.WebRtcPeerConnectionManager(app) { "TEST_HOST_ID" }

    val media = com.example.data.model.LocalMediaItem(
      title = "Test WebRTC Movie",
      mimeType = "video/mp4",
      durationSec = 3600,
      fileSizeBytes = 1024 * 1024 * 500,
      mediaType = StreamMediaType.VIDEO,
      isLocalFile = true
    )

    manager.startHosting("WP-TEST", media, com.example.data.model.StreamQuality.HIGH_ORIGINAL)
    assertEquals(com.example.network.webrtc.WebRtcPeerConnectionState.CONNECTED, manager.connectionState.value)
    assertEquals(com.example.network.webrtc.WebRtcDataChannelState.OPEN, manager.dataChannelState.value)

    // Test broadcasts
    manager.broadcastSeek(120f)
    assertEquals(120f, manager.currentPlaybackPosSec, 0.01f)

    manager.broadcastPlayPause(isPlaying = false, positionSec = 120f)
    assertFalse(manager.isCurrentlyPlaying)

    manager.broadcastReaction("🔥", "Host Tester")
    manager.broadcastChatMessage("msg_1", "Host Tester", "WebRTC live stream active")

    manager.leave()
    assertEquals(com.example.network.webrtc.WebRtcPeerConnectionState.CLOSED, manager.connectionState.value)
  }
}
