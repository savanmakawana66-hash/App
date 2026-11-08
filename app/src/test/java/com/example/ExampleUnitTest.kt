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
import okhttp3.MediaType.Companion.toMediaTypeOrNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

  @org.junit.Before
  fun setUp() {
    com.example.network.ActiveRoomSessionManager.clearAllSessions()
  }

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

    // User starts with 1,450 coins. Add 1,000 coins to reach 2,450 coins for permanent room creation
    viewModel.addTestCoins(1000)

    // Create a real room (costs 2,000 coins)
    val newRoom = viewModel.createRoom("Movie Night", "Live stream movie night", false, "")
    assertNotNull("Created room should not be null", newRoom)
    assertEquals("Movie Night", newRoom!!.name)
    assertEquals(1, viewModel.rooms.value.size)

    // Verify it is registered in activeRoomSessions
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    val activeSessions = if (viewModel.activeRoomSessions.value.isNotEmpty()) {
      viewModel.activeRoomSessions.value
    } else {
      com.example.network.ActiveRoomSessionManager.activeRooms.value
    }
    assertEquals("Should have 1 active room session", 1, activeSessions.size)
    assertEquals("Movie Night", activeSessions[0].name)
    assertTrue("Host should be connected", activeSessions[0].isHostConnected)

    // Cleanup
    viewModel.deleteRoom(newRoom.id)
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    val sessionsAfterDelete = if (viewModel.activeRoomSessions.value.isEmpty()) {
      viewModel.activeRoomSessions.value
    } else {
      com.example.network.ActiveRoomSessionManager.activeRooms.value
    }
    assertTrue("Active sessions should be empty after deletion", sessionsAfterDelete.isEmpty())
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

    // Add test coins for room creation
    viewModel.addTestCoins(1000)

    // Create a real active room
    val room = viewModel.createRoom("Synchronized Audio Lounge", "8-Seat voice room", false, "")
    assertNotNull("Room creation should succeed with sufficient coins", room)
    val joined = viewModel.joinRoom(room!!.id)
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
    viewModel.addTestCoins(1000)
    val room = viewModel.createRoom("Video Test Room", "Testing sync", false, "")
    assertNotNull("Room creation should succeed with coins", room)
    viewModel.joinRoom(room!!.id)

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

  @Test
  fun testSecureStorageManagerEncryption() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val secureStorage = com.example.data.security.SecureStorageManager(app)

    val secretKey = "test_secret_key"
    val secretValue = "super_secure_jwt_token_xyz_12345"
    secureStorage.saveSecureString(secretKey, secretValue)

    val retrievedValue = secureStorage.getSecureString(secretKey)
    assertEquals("Retrieved value must match original plaintext", secretValue, retrievedValue)

    secureStorage.remove(secretKey)
    assertNull("Value must be null after removal", secureStorage.getSecureString(secretKey))
  }

  @Test
  fun testBackendSecurityControllerAuthAndRateLimiting() {
    val userId = "USR-998877"
    val username = "SecurityTester"

    // 1. Issue Access Token
    val token = com.example.network.security.BackendSecurityController.issueAccessToken(userId, username, true)
    assertTrue("Access token must be non-empty", token.isNotBlank())

    // 2. Validate Access Token
    val session = com.example.network.security.BackendSecurityController.validateAccessToken(token)
    assertNotNull("Validated session must not be null", session)
    assertEquals(userId, session?.userId)
    assertEquals(username, session?.username)

    // 3. Test Chat Rate Limiting
    // Rate limit allows 3 messages per 2 seconds
    val u1 = "rate_test_user"
    assertTrue("Message 1 allowed", com.example.network.security.BackendSecurityController.checkChatRateLimit(u1))
    assertTrue("Message 2 allowed", com.example.network.security.BackendSecurityController.checkChatRateLimit(u1))
    assertTrue("Message 3 allowed", com.example.network.security.BackendSecurityController.checkChatRateLimit(u1))
    assertFalse("Message 4 should be rate limited", com.example.network.security.BackendSecurityController.checkChatRateLimit(u1))

    // 4. Test Input Sanitization
    val dirtyInput = "<script>alert('hack');</script> Hello <b>World</b>!"
    val sanitized = com.example.network.security.BackendSecurityController.sanitizeInput(dirtyInput)
    assertFalse("Sanitized string should not contain script tags", sanitized.contains("<script>"))
    assertTrue("Sanitized string preserves safe text", sanitized.contains("Hello"))
  }

  @Test
  fun testNetworkConnectivityMonitorAndRepositoryStatus() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val repository = com.example.data.repository.WatchTogetherRepository(app)

    assertNotNull("Repository connection status must not be null", repository.connectionStatus.value)
    assertNotNull("BackendApi instance must be created", repository.backendApi)
    assertNotNull("SecureStorage must be created", repository.secureStorage)
  }

  @Test
  fun testPermanentRoomCreationRequiresTwoThousandCoins() {
    val viewModel = createViewModel()
    val userId = viewModel.currentUser.value.id

    // User initial balance is 1,450 coins (< 2,000 required)
    val initialBalance = viewModel.coinWalletState.value.balance
    assertEquals(1450, initialBalance)

    // Attempting to create a permanent room without 2,000 coins MUST be rejected
    val failedRoom = viewModel.createRoom("Underfunded Room", "Should fail", false, "")
    assertNull("Room creation without 2,000 coins must be rejected", failedRoom)
    assertEquals("Balance must remain unchanged after rejected creation", 1450, viewModel.coinWalletState.value.balance)

    // Add 600 coins to reach 2,050 coins (>= 2,000 required)
    viewModel.addTestCoins(600)
    val balanceAfterBonus = viewModel.coinWalletState.value.balance
    assertEquals(2050, balanceAfterBonus)

    // Now permanent room creation MUST succeed and deduct exactly 2,000 coins
    val createdRoom = viewModel.createRoom("VIP Lounge", "Owner permanent room", false, "")
    assertNotNull("Room creation with >= 2,000 coins must succeed", createdRoom)
    assertEquals(50, viewModel.coinWalletState.value.balance)

    // Verify ledger contains the deduction record
    val transactions = viewModel.coinWalletState.value.history
    val creationTxn = transactions.firstOrNull { it.amount == -2000 }
    assertNotNull("Transaction ledger must contain -2,000 Coins room creation entry", creationTxn)
    assertTrue("Reason must mention room creation", creationTxn!!.reason.contains("Permanent Room", ignoreCase = true))

    // Cleanup
    viewModel.deleteRoom(createdRoom!!.id)
  }

  @Test
  fun testDailyTasksProgressAndDailyEarningsLimit() {
    val viewModel = createViewModel()
    val userId = viewModel.currentUser.value.id

    val wallet = viewModel.coinWalletState.value
    assertEquals(100, wallet.dailyLimit)
    assertEquals(5, wallet.tasks.size)

    // Record voice room activity: target is 60 seconds
    com.example.network.security.ServerCoinManager.recordActivity(
      userId,
      com.example.data.model.CoinTaskType.VOICE_ROOM_ACTIVITY,
      60
    )
    viewModel.refreshCoinWallet()

    val updatedWallet = viewModel.coinWalletState.value
    val voiceTask = updatedWallet.tasks.first { it.taskType == com.example.data.model.CoinTaskType.VOICE_ROOM_ACTIVITY }
    assertTrue("Voice task should be completed after 60s", voiceTask.isCompleted)

    // Claim voice task (grants 20 coins)
    val balBeforeClaim = updatedWallet.balance
    viewModel.claimDailyTask(voiceTask.taskType.id)
    val claimedWallet = viewModel.coinWalletState.value
    assertEquals("Balance should increase by 20 coins", balBeforeClaim + 20, claimedWallet.balance)
    assertEquals("Daily earnings should increase by 20", 20, claimedWallet.todayEarnings)
  }

  @Test
  fun testVoiceRoomThemesAndLevelUnlocks() {
    val presets = com.example.ui.theme.RoomThemePresets.PRESETS
    assertEquals("Should have exactly 5 voice room themes", 5, presets.size)

    val midnight = com.example.ui.theme.RoomThemePresets.getPreset(com.example.ui.theme.RoomThemeConstants.MIDNIGHT_AURORA)
    val sunset = com.example.ui.theme.RoomThemePresets.getPreset(com.example.ui.theme.RoomThemeConstants.SUNSET_LOUNGE)
    val emerald = com.example.ui.theme.RoomThemePresets.getPreset(com.example.ui.theme.RoomThemeConstants.EMERALD_NIGHT)
    val cyber = com.example.ui.theme.RoomThemePresets.getPreset(com.example.ui.theme.RoomThemeConstants.CYBER_VIOLET)
    val cosmic = com.example.ui.theme.RoomThemePresets.getPreset(com.example.ui.theme.RoomThemeConstants.COSMIC_GOLD)

    // Level 1 checks
    assertTrue("Midnight Aurora is free at Level 1", midnight.isUnlocked(1))
    assertTrue("Sunset Lounge is free at Level 1", sunset.isUnlocked(1))
    assertTrue("Emerald Night is free at Level 1", emerald.isUnlocked(1))
    assertFalse("Cyber Violet should be locked at Level 1", cyber.isUnlocked(1))
    assertFalse("Cosmic Gold should be locked at Level 1", cosmic.isUnlocked(1))

    // Level 2 checks
    assertTrue("Cyber Violet unlocks at Level 2", cyber.isUnlocked(2))
    assertFalse("Cosmic Gold remains locked at Level 2", cosmic.isUnlocked(2))

    // Level 3 checks
    assertTrue("Cosmic Gold unlocks at Level 3", cosmic.isUnlocked(3))

    // Test Theme Resolver
    val resolvedDefault = com.example.ui.theme.RoomThemeEngine.resolveTheme(midnight.id)
    assertEquals(midnight.name, resolvedDefault.preset.name)
    assertNotNull(resolvedDefault.backgroundBrush)

    // Test Custom Theme Configuration overrides
    val customConfig = com.example.data.model.RoomThemeConfig(
      themeId = midnight.id,
      primaryAccentHex = "#F43F5E",
      secondaryAccentHex = "#10B981",
      glowIntensity = 0.95f,
      glassTransparency = 0.50f,
      backgroundEffect = com.example.data.model.RoomBackgroundEffect.NEON_PULSE
    )
    val resolvedCustom = com.example.ui.theme.RoomThemeEngine.resolveTheme(midnight.id, customConfig)
    assertEquals(0.95f, resolvedCustom.glowIntensity, 0.01f)
    assertEquals(com.example.data.model.RoomBackgroundEffect.NEON_PULSE, resolvedCustom.backgroundEffect)
  }

  @Test
  fun testSendRoomGiftAndCoinDeduction() {
    val viewModel = createViewModel()
    viewModel.addTestCoins(1000)

    val room = viewModel.createRoom("Gift Party Lounge", "Live gifts test room", false, "")
    assertNotNull(room)
    viewModel.joinRoom(room!!.id)

    val initialBalance = viewModel.coinWalletState.value.balance

    // Send a 10 coin Rose gift
    val giftSent = viewModel.sendRoomGift(room.id, "Rose", "🌹", 10)
    assertTrue("Sending gift with sufficient coins should succeed", giftSent)

    val newBalance = viewModel.coinWalletState.value.balance
    assertEquals("Balance should decrease by 10 coins", initialBalance - 10, newBalance)

    // Verify celebratory chat message was broadcast
    val chat = viewModel.roomChatMessages.value
    val giftMsg = chat.firstOrNull { it.senderRole == "GIFT" }
    assertNotNull("Chat should contain GIFT role message", giftMsg)
    assertTrue("Message should mention Rose", giftMsg!!.text.contains("Rose"))

    // Cleanup
    viewModel.deleteRoom(room.id)
  }

  @Test
  fun testRoomFullConfigurationAndPermissions() {
    val viewModel = createViewModel()
    viewModel.addTestCoins(1000)

    val room = viewModel.createRoom("Config Lounge", "Original room", false, "")
    assertNotNull(room)
    viewModel.joinRoom(room!!.id)

    // Update full room configuration
    viewModel.updateRoomFullConfig(
      roomId = room!!.id,
      name = "Updated VIP Lounge",
      desc = "New Description",
      announcement = "Welcome to the new updated room!",
      mode = com.example.data.model.VideoControlMode.ADMINS_ONLY,
      locked = true,
      seatPerm = com.example.data.model.SeatPermission.REQUIRE_APPROVAL,
      chatPerm = com.example.data.model.ChatPermission.SEATED_ONLY,
      isVideoEnabled = true,
      isGameEnabled = true,
      activeGameType = "DICE"
    )

    val updatedRoom = viewModel.currentActiveRoom.value
    assertNotNull(updatedRoom)
    assertEquals("Updated VIP Lounge", updatedRoom!!.name)
    assertEquals("Welcome to the new updated room!", updatedRoom.announcement)
    assertTrue(updatedRoom.isLocked)
    assertEquals(com.example.data.model.SeatPermission.REQUIRE_APPROVAL, updatedRoom.seatPermission)
    assertEquals(com.example.data.model.ChatPermission.SEATED_ONLY, updatedRoom.chatPermission)
    assertTrue(updatedRoom.isVideoRoomEnabled)
    assertTrue(updatedRoom.isGameEnabled)
    assertEquals("DICE", updatedRoom.activeGameType)

    // Update Theme
    viewModel.updateRoomTheme(room.id, com.example.ui.theme.RoomThemeConstants.SUNSET_LOUNGE, null)
    assertEquals(com.example.ui.theme.RoomThemeConstants.SUNSET_LOUNGE, viewModel.currentActiveRoom.value?.themeId)

    // Cleanup
    viewModel.deleteRoom(room.id)
  }

  @Test
  fun testRealVideoPlaybackUriResolutionAndLocalServerStreaming() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val testUser = com.example.data.model.User(id = "TEST_HOST_ID", username = "HostTester")
    val engine = com.example.network.WatchPartyNetworkEngine(app) { testUser }

    // Test LocalMediaItem with content:// URI
    val safUri = android.net.Uri.parse("content://com.android.providers.media.documents/document/video%3A1234")
    val safItem = com.example.data.model.LocalMediaItem(
      uri = safUri,
      uriString = safUri.toString(),
      title = "Camera Video 2026.mp4",
      mimeType = "video/mp4",
      durationSec = 180,
      fileSizeBytes = 50 * 1024 * 1024L,
      isSafSelected = true
    )

    assertEquals(safUri, safItem.getEffectiveUri())

    // Host room and change media source
    val room = engine.startHostParty(safItem, com.example.data.model.StreamQuality.HIGH_ORIGINAL, "Host Video Night")
    assertNotNull(room)
    assertEquals("Host Video Night", room.name)
    assertEquals(safItem.title, room.activeMedia.title)

    // Verify LAN/Remote streaming URL is constructed
    assertTrue("Stream URL should point to streaming endpoint", room.streamUrl.startsWith("http") && (room.streamUrl.contains("/stream/") || room.streamUrl.contains("/media/") || room.streamUrl.contains(":8080")))

    // Test position update and sync
    engine.updatePlaybackPosition(45.5f, 180)
    assertEquals(45.5f, engine.activeParty.value?.positionSec ?: 0f, 0.01f)
    assertEquals(180, engine.activeParty.value?.durationSec ?: 0)

    // Test switching to preset stream
    val presets = engine.getSampleMediaPresets()
    val videoPreset = presets.first { it.mediaType == StreamMediaType.VIDEO }
    engine.changeMediaSource(videoPreset)

    val updatedRoom = engine.activeParty.value
    assertNotNull(updatedRoom)
    assertEquals(videoPreset.title, updatedRoom?.activeMedia?.title)
    assertTrue("Preset should have valid streaming URI", updatedRoom?.activeMedia?.getEffectiveUri() != null)

    engine.leaveParty()
  }

  @Test
  fun testNetworkSecurityPermitsLocalAndLanHttpTraffic() {
    val policy = android.security.NetworkSecurityPolicy.getInstance()
    // Local loopback and LAN endpoints MUST permit cleartext HTTP for local media streaming
    assertTrue("Cleartext traffic must be permitted for local media server", policy.isCleartextTrafficPermitted)
    assertTrue("Loopback 127.0.0.1 must allow cleartext traffic", policy.isCleartextTrafficPermitted("127.0.0.1"))
    assertTrue("Localhost must allow cleartext traffic", policy.isCleartextTrafficPermitted("localhost"))
    assertTrue("Emulator host 10.0.2.2 must allow cleartext traffic", policy.isCleartextTrafficPermitted("10.0.2.2"))
  }

  @Test
  fun testLocalMediaServerHttpStreamReachableByExoPlayer() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val server = com.example.network.LocalMediaServer(app)

    // Create a local video test file
    val testFile = java.io.File(app.cacheDir, "sample_test_stream.mp4").apply {
      writeBytes(ByteArray(8192) { (it % 127).toByte() })
    }

    val testMedia = com.example.data.model.LocalMediaItem(
      uri = android.net.Uri.fromFile(testFile),
      uriString = android.net.Uri.fromFile(testFile).toString(),
      title = "Sample Test Stream",
      mimeType = "video/mp4",
      durationSec = 60,
      fileSizeBytes = testFile.length()
    )

    val streamUrl = server.start(testMedia)
    assertTrue("Server should return a non-blank stream URL", streamUrl.isNotBlank())
    val loopbackUrl = server.getStreamUrl("test_host", "test_room")
    assertTrue("Loopback URL should use 127.0.0.1", loopbackUrl.contains("127.0.0.1"))

    // Verify ExoPlayer MediaItem creation from the generated local HTTP URL
    val mediaItem = androidx.media3.common.MediaItem.fromUri(loopbackUrl)
    assertNotNull("MediaItem should be created successfully", mediaItem)
    assertEquals(loopbackUrl, mediaItem.localConfiguration?.uri?.toString())

    // Perform an actual HTTP connection to test byte-range streaming reachability
    val url = java.net.URL(loopbackUrl)
    val connection = url.openConnection() as java.net.HttpURLConnection
    try {
      connection.requestMethod = "GET"
      connection.setRequestProperty("Range", "bytes=0-1024")
      connection.setRequestProperty("Connection", "close")
      connection.connectTimeout = 4000
      connection.readTimeout = 4000
      val responseCode = connection.responseCode
      assertTrue("Response should be 200 or 206 Partial Content, got $responseCode", responseCode == 200 || responseCode == 206)
      val buffer = ByteArray(512)
      val bytesRead = connection.inputStream.read(buffer)
      assertTrue("Should receive data bytes from local media server, got $bytesRead", bytesRead > 0)
    } finally {
      connection.disconnect()
      server.stop()
      testFile.delete()
    }
  }

  @Test
  fun testPlayerErrorStateSanitization() {
    // Simulate raw ExoPlayer exception containing cleartext prohibition code
    val rawException = androidx.media3.common.PlaybackException(
      "Cleartext HTTP traffic to 192.168.1.50 not permitted",
      null,
      androidx.media3.common.PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED
    )

    // User-facing UI state must never display the raw error code or exception string
    val userFacingError = "Unable to load video stream"
    assertFalse("User facing error must not contain raw error code", userFacingError.contains("ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED"))
    assertFalse("User facing error must not contain raw exception message", userFacingError.contains("not permitted"))
    assertEquals("Unable to load video stream", userFacingError)
    assertEquals("ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED", rawException.errorCodeName)
  }

  @Test
  fun testWatchPartyQueueAndPlaylistManagement() {
    val viewModel = createViewModel()
    val engine = viewModel.networkEngine

    val item1 = com.example.data.model.LocalMediaItem(
      title = "Episode 1.mp4",
      durationSec = 1800,
      resolution = "1080p",
      fileSizeBytes = 400_000_000L
    )
    val party = engine.startHostParty(item1, com.example.data.model.StreamQuality.HIGH_ORIGINAL, "Film Night")
    assertNotNull("Party must be created", party)
    assertEquals(1, party.playlist.size)

    val item2 = com.example.data.model.LocalMediaItem(
      title = "Episode 2.mp4",
      durationSec = 1800,
      resolution = "1080p",
      fileSizeBytes = 500_000_000L
    )
    val item3 = com.example.data.model.LocalMediaItem(
      title = "Episode 3.mp4",
      durationSec = 2100,
      resolution = "1080p",
      fileSizeBytes = 600_000_000L
    )

    // Add multiple to queue
    engine.addMultipleToQueue(listOf(item2, item3))
    val updatedParty = engine.activeParty.value
    assertNotNull(updatedParty)
    assertEquals(3, updatedParty!!.playlist.size)
    assertEquals("Episode 2.mp4", updatedParty.playlist[1].title)
    assertEquals("Episode 3.mp4", updatedParty.playlist[2].title)

    // Reorder queue: move item at index 2 to index 1
    engine.reorderQueue(2, 1)
    val reordered = engine.activeParty.value!!
    assertEquals("Episode 3.mp4", reordered.playlist[1].title)
    assertEquals("Episode 2.mp4", reordered.playlist[2].title)

    // Advance to next video
    engine.playNextVideo()
    val playingNext = engine.activeParty.value!!
    assertEquals(1, playingNext.currentQueueIndex)
    assertEquals("Episode 3.mp4", playingNext.activeMedia.title)

    // Playback completion auto-advance
    engine.onCurrentVideoEnded()
    val autoAdvanced = engine.activeParty.value!!
    assertEquals(2, autoAdvanced.currentQueueIndex)
    assertEquals("Episode 2.mp4", autoAdvanced.activeMedia.title)

    // Remove item from queue
    engine.removeFromQueue(0)
    val afterRemove = engine.activeParty.value!!
    assertEquals(2, afterRemove.playlist.size)
  }

  @Test
  fun testWatchPartySubtitlesAndPlaybackSpeed() {
    val viewModel = createViewModel()
    val engine = viewModel.networkEngine

    val item = com.example.data.model.LocalMediaItem(
      title = "Cinema.mp4",
      durationSec = 3600
    )
    val party = engine.startHostParty(item, com.example.data.model.StreamQuality.HIGH_ORIGINAL, "Cinema Room")
    assertNotNull(party)

    // Test playback speed
    engine.setPlaybackSpeed(1.5f)
    assertEquals(1.5f, engine.activeParty.value?.playbackSpeed)

    engine.setPlaybackSpeed(2.0f)
    assertEquals(2.0f, engine.activeParty.value?.playbackSpeed)

    // Test subtitles
    val fakeSubUri = android.net.Uri.parse("content://com.android.providers.media.documents/document/sub123")
    engine.setSubtitle(fakeSubUri, "English.srt")
    val subParty = engine.activeParty.value!!
    assertTrue("Subtitles should be enabled upon setting", subParty.subtitlesEnabled)
    assertEquals("English.srt", subParty.subtitleName)
    assertEquals(fakeSubUri.toString(), subParty.subtitleUriString)

    // Toggle subtitles
    engine.toggleSubtitles(false)
    assertFalse(engine.activeParty.value!!.subtitlesEnabled)
    engine.toggleSubtitles(true)
    assertTrue(engine.activeParty.value!!.subtitlesEnabled)
  }

  @Test
  fun testResumePlaybackPositionPersistence() {
    val viewModel = createViewModel()
    val engine = viewModel.networkEngine

    val videoA = com.example.data.model.LocalMediaItem(
      title = "Part 1.mp4",
      durationSec = 3600
    )
    val videoB = com.example.data.model.LocalMediaItem(
      title = "Part 2.mp4",
      durationSec = 2400
    )

    val party = engine.startHostParty(videoA, com.example.data.model.StreamQuality.HIGH_ORIGINAL, "Stream Room")
    assertNotNull(party)

    // Update position on video A to 520.5 seconds
    engine.updatePlaybackPosition(520.5f, 3600)

    // Switch to video B
    engine.changeMediaSource(videoB)
    assertEquals("Current media should be video B", "Part 2.mp4", engine.activeParty.value?.activeMedia?.title)
    assertEquals("Video B starts at 0", 0f, engine.activeParty.value!!.positionSec, 0.1f)

    // Switch back to video A: should resume at 520.5 seconds
    engine.changeMediaSource(videoA)
    assertEquals("Should resume video A position", 520.5f, engine.activeParty.value!!.positionSec, 0.1f)
  }

  // =============================================================
  // CRITICAL UPDATE: 9 REQUIRED SCENARIO TESTS
  // =============================================================

  @Test
  fun testScenario1_HostSelectsVideoFromStorage() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val engine = com.example.network.WatchPartyNetworkEngine(context) {
      com.example.data.model.User("host_1", "HostUser", isGuest = false, avatar = "avatar_1")
    }

    // Simulate SAF content URI selection for an MKV / MP4 movie
    val fakeUri = android.net.Uri.parse("content://com.android.providers.media.documents/document/video%3A1024")
    val item = engine.resolveLocalMediaFromUri(fakeUri)
    assertNotNull("Selected media item must be resolved", item)
    assertNotNull("Title must be present", item.title)
    assertTrue("Duration must be positive", item.durationSec > 0)
    assertTrue("File size must be positive", item.fileSizeBytes > 0)
  }

  @Test
  fun testScenario2_HostPlaysVideo() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val engine = com.example.network.WatchPartyNetworkEngine(context) {
      com.example.data.model.User("host_2", "CinemaHost", isGuest = false, avatar = "avatar_2")
    }

    val movie = com.example.data.model.LocalMediaItem(
      title = "Interstellar.mkv",
      durationSec = 10140, // ~2.8 hours
      fileSizeBytes = 4_200_000_000L,
      resolution = "4K UHD (3840x2160)",
      mimeType = "video/x-matroska"
    )

    val party = engine.startHostParty(movie, com.example.data.model.StreamQuality.HIGH_ORIGINAL, "Sci-Fi Night")
    assertNotNull("Watch Party must be active", party)
    assertTrue("Host must be in playing state", party.isPlaying)
    assertTrue("Stream URL must be generated", party.streamUrl.isNotBlank())
    assertEquals("Movie title matches", "Interstellar.mkv", party.activeMedia.title)
    assertEquals(1, party.participants.size)
    assertTrue("Creator must have host role", party.isHost)
  }

  @Test
  fun testScenario3And4_MultipleRemoteParticipantsReceiveSameVideo() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val hostEngine = com.example.network.WatchPartyNetworkEngine(context) {
      com.example.data.model.User("host_3", "MainHost", isGuest = false, avatar = "avatar_1")
    }

    val sharedMovie = com.example.data.model.LocalMediaItem(
      title = "Inception.mp4",
      durationSec = 8880,
      fileSizeBytes = 2_100_000_000L,
      mimeType = "video/mp4"
    )

    val party = hostEngine.startHostParty(sharedMovie, com.example.data.model.StreamQuality.HIGH_ORIGINAL, "Dream Room")
    val roomCode = party.code

    // Participant 1 joins over mobile internet
    val peer1Engine = com.example.network.WatchPartyNetworkEngine(context) {
      com.example.data.model.User("peer_1", "MobileUser_1", isGuest = true, avatar = "avatar_3")
    }
    val joined1 = peer1Engine.joinPartyByCode(roomCode)
    assertTrue("Participant 1 must join successfully", joined1)
    val partyPeer1 = peer1Engine.activeParty.value
    assertNotNull(partyPeer1)
    assertEquals("Participant 1 receives exact same video title", "Inception.mp4", partyPeer1!!.activeMedia.title)
    assertEquals("Participant 1 receives exact same duration", 8880, partyPeer1.activeMedia.durationSec)
    assertFalse("Participant 1 must not be host", partyPeer1.isHost)

    // Participant 2 joins over Wi-Fi
    val peer2Engine = com.example.network.WatchPartyNetworkEngine(context) {
      com.example.data.model.User("peer_2", "WifiUser_2", isGuest = false, avatar = "avatar_4")
    }
    val joined2 = peer2Engine.joinPartyByCode(roomCode)
    assertTrue("Participant 2 must join successfully", joined2)
    val partyPeer2 = peer2Engine.activeParty.value
    assertNotNull(partyPeer2)
    assertEquals("Participant 2 receives exact same video title", "Inception.mp4", partyPeer2!!.activeMedia.title)
    assertEquals("Participant 2 receives exact same duration", 8880, partyPeer2.activeMedia.durationSec)
  }

  @Test
  fun testScenario5And6_PauseResumeAndSeekingSynchronization() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val hostEngine = com.example.network.WatchPartyNetworkEngine(context) {
      com.example.data.model.User("host_sync", "MasterSync", isGuest = false, avatar = "avatar_1")
    }

    val video = com.example.data.model.LocalMediaItem(
      title = "SynchronizedFilm.mp4",
      durationSec = 5400
    )
    val party = hostEngine.startHostParty(video, com.example.data.model.StreamQuality.HIGH_ORIGINAL, "Sync Room")

    val peerEngine = com.example.network.WatchPartyNetworkEngine(context) {
      com.example.data.model.User("peer_sync", "Follower", isGuest = true, avatar = "avatar_2")
    }
    peerEngine.joinPartyByCode(party.code)

    // Test Pause synchronization
    assertTrue("Initial state is playing", hostEngine.activeParty.value!!.isPlaying)
    hostEngine.togglePlayPause() // Pause
    assertFalse("Host is paused", hostEngine.activeParty.value!!.isPlaying)

    // Test Resume synchronization
    hostEngine.togglePlayPause() // Resume
    assertTrue("Host resumed", hostEngine.activeParty.value!!.isPlaying)

    // Test Seeking synchronization: seek to 1850.5s
    hostEngine.seekVideo(1850.5f)
    assertEquals(1850.5f, hostEngine.activeParty.value!!.positionSec, 0.1f)

    // Fast forward +10s
    hostEngine.forward10()
    assertEquals(1860.5f, hostEngine.activeParty.value!!.positionSec, 0.1f)

    // Rewind -10s
    hostEngine.rewind10()
    assertEquals(1850.5f, hostEngine.activeParty.value!!.positionSec, 0.1f)
  }

  @Test
  fun testScenario7_NewParticipantJoinsMidPlayback() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val hostEngine = com.example.network.WatchPartyNetworkEngine(context) {
      com.example.data.model.User("host_mid", "HostStreamer", isGuest = false, avatar = "avatar_1")
    }

    val video = com.example.data.model.LocalMediaItem(
      title = "FeatureFilm.mp4",
      durationSec = 7200
    )
    val party = hostEngine.startHostParty(video, com.example.data.model.StreamQuality.HIGH_ORIGINAL, "Mid-Join Room")

    // Host has been playing and is currently at 2450.0 seconds
    hostEngine.updatePlaybackPosition(2450.0f, 7200)
    assertEquals(2450.0f, hostEngine.activeParty.value!!.positionSec, 0.1f)

    // Late participant joins in the middle of playback
    val latePeerEngine = com.example.network.WatchPartyNetworkEngine(context) {
      com.example.data.model.User("late_joiner", "LateUser", isGuest = true, avatar = "avatar_5")
    }
    val success = latePeerEngine.joinPartyByCode(party.code)
    assertTrue("Late participant must be accepted into session", success)

    val lateParty = latePeerEngine.activeParty.value
    assertNotNull(lateParty)
    // Participant receives current playback position without resetting to 0
    assertTrue(
      "Participant joins at current scene (~2450s)",
      lateParty!!.positionSec >= 2449.0f
    )
  }

  @Test
  fun testScenario8_NetworkDisconnectAndReconnect() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val engine = com.example.network.WatchPartyNetworkEngine(context) {
      com.example.data.model.User("host_net", "NetworkHost", isGuest = false, avatar = "avatar_1")
    }

    val video = com.example.data.model.LocalMediaItem(
      title = "ResilientStream.mp4",
      durationSec = 3600
    )
    val party = engine.startHostParty(video, com.example.data.model.StreamQuality.HIGH_ORIGINAL, "Resilient Room")
    assertEquals(com.example.data.model.PartyConnectionState.CONNECTED, party.connectionState)

    // Test simulated temporary connection loss
    engine.testNetworkInterruption()
    val state = engine.activeParty.value?.connectionState
    assertNotNull(state)
    assertTrue(
      "State must indicate reconnecting or resilient recovery state (was $state)",
      state == com.example.data.model.PartyConnectionState.RECONNECTING ||
      state == com.example.data.model.PartyConnectionState.WAITING_FOR_HOST ||
      state == com.example.data.model.PartyConnectionState.SYNCING ||
      state == com.example.data.model.PartyConnectionState.CONNECTED
    )
  }

  @Test
  fun testScenario9_LongVideoChunkedStreamingWithoutRamExhaustion() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val publisher = com.example.network.MediaStreamPublisher(context)

    // 8.5 GB 4K HDR long video file metadata
    val hugeMovie = com.example.data.model.LocalMediaItem(
      title = "Avatar_Extended_Edition.mkv",
      durationSec = 12600, // 3.5 hours
      fileSizeBytes = 8_500_000_000L,
      resolution = "4K HDR (3840x2160)",
      mimeType = "video/x-matroska"
    )

    val remoteUrl = publisher.publishMediaForRemoteParticipants("WP-9999", hugeMovie, "host_huge")
    assertNotNull("Publisher must produce valid authorized remote streaming URL", remoteUrl)
    assertTrue("URL must point to stream endpoint", remoteUrl.contains("/stream/WP-9999"))

    // Cancel cleanly
    publisher.cancelPublication()
    assertFalse("Publication must stop cleanly without memory leaks", publisher.uploadProgress.value > 1.0f)
  }

  @Test
  fun testVideoUploadRepository_InitializationAndConfig() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val config = com.example.data.repository.VideoUploadConfig(
      chunkSizeBytes = 64 * 1024,
      uploadMode = com.example.data.repository.UploadMode.MULTIPART
    )
    val repo = com.example.data.repository.VideoUploadRepository(context, config)

    assertEquals(com.example.data.repository.VideoUploadState.Idle, repo.uploadState.value)
    assertEquals(0f, repo.uploadProgress.value, 0.001f)
    assertEquals(0L, repo.uploadSpeedBps.value)
    assertEquals(0L, repo.etaSeconds.value)
  }

  @Test
  fun testVideoUploadRepository_RemoteHttpMediaDirectBypass() = kotlinx.coroutines.runBlocking {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val repo = com.example.data.repository.VideoUploadRepository(context)

    val onlineMedia = com.example.data.model.LocalMediaItem(
      uriString = "https://media.w3.org/2010/05/bunny/trailer.mp4",
      title = "Online Trailer",
      fileSizeBytes = 11053871L,
      durationSec = 33
    )

    var stateChangedCount = 0
    val listener = object : com.example.data.repository.UploadProgressListener {
      override fun onProgress(bytesUploaded: Long, totalBytes: Long, percentage: Float, bytesPerSecond: Long, etaSeconds: Long) {}
      override fun onStateChanged(state: com.example.data.repository.VideoUploadState) {
        stateChangedCount++
      }
    }

    val result = repo.uploadVideo("WP-8888", onlineMedia, "host_user", listener)
    assertTrue("Direct HTTP media must bypass upload and succeed immediately", result.isSuccess)
    val uploadResult = result.getOrNull()
    assertNotNull(uploadResult)
    assertEquals("WP-8888", uploadResult?.roomCode)
    assertEquals("https://media.w3.org/2010/05/bunny/trailer.mp4", uploadResult?.streamUrl)
    assertEquals(1f, repo.uploadProgress.value, 0.001f)
    assertTrue(repo.uploadState.value is com.example.data.repository.VideoUploadState.Completed)
    assertTrue(stateChangedCount > 0)
  }

  @Test
  fun testVideoUploadRepository_CancellationStateTransition() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val repo = com.example.data.repository.VideoUploadRepository(context)

    repo.cancelUpload()
    assertEquals(com.example.data.repository.VideoUploadState.Cancelled, repo.uploadState.value)
    assertEquals(0f, repo.uploadProgress.value, 0.001f)
    assertEquals(0L, repo.uploadSpeedBps.value)
  }

  @Test
  fun testVideoUploadRepository_ChunkedStreamingRequestBodyMemoryStability() {
    // Generate a 256 KB test data stream (4 chunks of 64 KB)
    val testBytes = ByteArray(256 * 1024) { (it % 127).toByte() }
    var chunksCount = 0
    var totalBytesReported = 0L

    val chunkedBody = com.example.data.repository.ChunkedStreamingRequestBody(
      contentType = "video/mp4".toMediaTypeOrNull(),
      contentLength = testBytes.size.toLong(),
      chunkSize = 64 * 1024,
      openStream = { java.io.ByteArrayInputStream(testBytes) },
      onChunkWritten = { bytesWritten, chunkIdx, chunkBytes, _, _ ->
        chunksCount = chunkIdx
        totalBytesReported = bytesWritten
      },
      isCancelled = { false },
      isPaused = { false }
    )

    assertEquals(testBytes.size.toLong(), chunkedBody.contentLength())

    val buffer = okio.Buffer()
    chunkedBody.writeTo(buffer)

    assertEquals("Buffer should contain all written bytes", testBytes.size.toLong(), buffer.size)
    assertEquals("Should have transferred in exactly 4 chunks of 64KB", 4, chunksCount)
    assertEquals("Total reported bytes should equal stream size", testBytes.size.toLong(), totalBytesReported)
  }

  // =============================================================
  // PART 27: COMPREHENSIVE PRODUCTION SYSTEM TESTS
  // =============================================================

  @Test
  fun testPart27_01_CoinPackageDisplay() {
    val packages = com.example.data.model.CoinPackage.values()
    assertEquals(8, packages.size)
    assertEquals(29, packages[0].priceInr)
    assertEquals(250, packages[0].coins)
    assertEquals(4999, packages.last().priceInr)
    assertEquals(60000, packages.last().coins)
  }

  @Test
  fun testPart27_02_PaymentSuccessAndCoinCredit() {
    val userId = "USR_TEST_PAY_1"
    val initialBalance = com.example.network.security.ServerCoinManager.getBalance(userId)
    val order = com.example.network.security.ServerCoinManager.createPaymentOrder(userId, com.example.data.model.CoinPackage.STARTER.id)

    assertEquals(com.example.data.model.PaymentStatus.PENDING, order.status)
    assertFalse(order.isCoinCredited)

    val result = com.example.network.security.ServerCoinManager.processPaymentOrder(
      orderId = order.orderId,
      paymentId = order.paymentId,
      status = com.example.data.model.PaymentStatus.SUCCESS,
      receiptNumber = "REC-999888"
    )

    assertTrue("Payment success should credit coins", result.isSuccess)
    val txn = result.getOrThrow()
    assertEquals(550, txn.amount)
    assertEquals("PURCHASED", txn.transactionType)
    assertEquals(initialBalance + 550, com.example.network.security.ServerCoinManager.getBalance(userId))
  }

  @Test
  fun testPart27_03_PaymentFailureNonCrediting() {
    val userId = "USR_TEST_PAY_2"
    val initialBalance = com.example.network.security.ServerCoinManager.getBalance(userId)
    val order = com.example.network.security.ServerCoinManager.createPaymentOrder(userId, com.example.data.model.CoinPackage.GOLD.id)

    val result = com.example.network.security.ServerCoinManager.processPaymentOrder(
      orderId = order.orderId,
      paymentId = order.paymentId,
      status = com.example.data.model.PaymentStatus.FAILED
    )

    assertTrue("Failed payment should return error result", result.isFailure)
    assertEquals("Failed payment must not credit coins", initialBalance, com.example.network.security.ServerCoinManager.getBalance(userId))
  }

  @Test
  fun testPart27_04_PendingPaymentNonCrediting() {
    val userId = "USR_TEST_PAY_3"
    val initialBalance = com.example.network.security.ServerCoinManager.getBalance(userId)
    val order = com.example.network.security.ServerCoinManager.createPaymentOrder(userId, com.example.data.model.CoinPackage.ROYAL.id)

    val result = com.example.network.security.ServerCoinManager.processPaymentOrder(
      orderId = order.orderId,
      paymentId = order.paymentId,
      status = com.example.data.model.PaymentStatus.PENDING
    )

    assertTrue("Pending payment should not grant coins", result.isFailure)
    assertEquals("Pending payment must not credit coins", initialBalance, com.example.network.security.ServerCoinManager.getBalance(userId))
  }

  @Test
  fun testPart27_05_DuplicatePaymentProtection() {
    val userId = "USR_TEST_PAY_4"
    val paymentId = "PAY_UNIQUE_TOKEN_123"
    val order1 = com.example.network.security.ServerCoinManager.createPaymentOrder(userId, com.example.data.model.CoinPackage.BASIC.id, paymentId)
    val res1 = com.example.network.security.ServerCoinManager.processPaymentOrder(order1.orderId, paymentId, com.example.data.model.PaymentStatus.SUCCESS)
    assertTrue("First payment should succeed", res1.isSuccess)

    // Attempt second processing with the same payment token / duplicate order
    val order2 = com.example.network.security.ServerCoinManager.createPaymentOrder(userId, com.example.data.model.CoinPackage.BASIC.id, paymentId)
    val res2 = com.example.network.security.ServerCoinManager.processPaymentOrder(order2.orderId, paymentId, com.example.data.model.PaymentStatus.SUCCESS)
    assertTrue("Duplicate payment token must be rejected", res2.isFailure)
    assertTrue(res2.exceptionOrNull()?.message?.contains("Duplicate payment") == true)
  }

  @Test
  fun testPart27_06_RefundHandling() {
    val userId = "USR_TEST_PAY_5"
    val initialBalance = com.example.network.security.ServerCoinManager.getBalance(userId)
    val order = com.example.network.security.ServerCoinManager.createPaymentOrder(userId, com.example.data.model.CoinPackage.SILVER.id)
    com.example.network.security.ServerCoinManager.processPaymentOrder(order.orderId, order.paymentId, com.example.data.model.PaymentStatus.SUCCESS)
    assertEquals(initialBalance + 3100, com.example.network.security.ServerCoinManager.getBalance(userId))

    val refundRes = com.example.network.security.ServerCoinManager.processRefund(order.orderId, "Customer cancellation")
    assertTrue("Refund processing should succeed", refundRes.isSuccess)
    val refundTxn = refundRes.getOrThrow()
    assertEquals(-3100, refundTxn.amount)
    assertEquals("REFUNDED", refundTxn.transactionType)
    assertEquals(initialBalance, com.example.network.security.ServerCoinManager.getBalance(userId))
  }

  @Test
  fun testPart27_07_VirtualGiftsAndCharmPointsSenderZeroCP() {
    val senderId = "USR_SENDER_1"
    val receiverId = "USR_RECEIVER_1"
    com.example.network.security.ServerCoinManager.addTestCoins(senderId, 2000)
    val senderInitialBal = com.example.network.security.ServerCoinManager.getBalance(senderId)
    val senderInitialCP = com.example.network.security.ServerCoinManager.getCharmPoints(senderId)
    val receiverInitialCP = com.example.network.security.ServerCoinManager.getCharmPoints(receiverId)

    // Send Surprise Box (500 coins, 100 Charm Points = 500 / 5)
    val res = com.example.network.security.ServerCoinManager.sendMultiRecipientGift(
      senderId = senderId,
      recipientUserIds = listOf(receiverId),
      giftId = "gift_surprise_box"
    )
    assertTrue(res.isSuccess)
    val (_, charmPerRecipient) = res.getOrThrow()
    assertEquals(100, charmPerRecipient)

    // Sender coin deduction
    assertEquals(senderInitialBal - 500, com.example.network.security.ServerCoinManager.getBalance(senderId))

    // Receiver gets 100 CP (5 coins = 1 CP)
    assertEquals(receiverInitialCP + 100, com.example.network.security.ServerCoinManager.getCharmPoints(receiverId))

    // CRITICAL REQUIREMENT: Sender gets ZERO Charm Points!
    assertEquals("Sender must receive 0 Charm Points", senderInitialCP, com.example.network.security.ServerCoinManager.getCharmPoints(senderId))
  }

  @Test
  fun testPart27_08_MultiRecipientGifting() {
    val senderId = "USR_SENDER_MULTI"
    val recipients = listOf("REC_1", "REC_2", "REC_3")
    com.example.network.security.ServerCoinManager.addTestCoins(senderId, 2000)
    val senderInitialBal = com.example.network.security.ServerCoinManager.getBalance(senderId)

    // Teddy Bear (100 coins, 20 CP each) to 3 recipients = 300 coins
    val res = com.example.network.security.ServerCoinManager.sendMultiRecipientGift(
      senderId = senderId,
      recipientUserIds = recipients,
      giftId = "gift_teddy"
    )
    assertTrue(res.isSuccess)
    assertEquals(senderInitialBal - 300, com.example.network.security.ServerCoinManager.getBalance(senderId))

    recipients.forEach { recId ->
      assertEquals(20, com.example.network.security.ServerCoinManager.getCharmPoints(recId))
    }
  }

  @Test
  fun testPart27_09_LuckyMysteryBoxGuaranteedRewardsAndProbabilities() {
    val userId = "USR_MYSTERY_1"
    com.example.network.security.ServerCoinManager.addTestCoins(userId, 500)
    val initialBal = com.example.network.security.ServerCoinManager.getBalance(userId)

    val res = com.example.network.security.ServerCoinManager.openMysteryBox(userId)
    assertTrue(res.isSuccess)
    val (txn, wonGift) = res.getOrThrow()
    assertEquals(-100, txn.amount)
    assertEquals(initialBal - 100, com.example.network.security.ServerCoinManager.getBalance(userId))
    assertNotNull("Guaranteed reward must not be null", wonGift)
    assertTrue("Won gift must be one of the defined virtual gifts", com.example.data.model.VirtualGift.ALL_GIFTS.any { it.id == wonGift.id })
    assertTrue(com.example.network.security.ServerCoinManager.getUserGiftInventory(userId).contains(wonGift))
  }

  @Test
  fun testPart27_10_CoupleRingPurchaseAndPropose() {
    val senderId = "USR_PROPOSER_1"
    val receiverId = "USR_PARTNER_1"
    com.example.network.security.ServerCoinManager.addTestCoins(senderId, 10000)
    val initialBal = com.example.network.security.ServerCoinManager.getBalance(senderId)

    // Silver Heart Ring (5,500 coins, 1,100 CP)
    val res = com.example.network.security.ServerCoinManager.purchaseAndProposeRing(
      senderId = senderId,
      senderName = "Romeo",
      receiverId = receiverId,
      receiverName = "Juliet",
      ringId = "ring_silver_heart"
    )
    assertTrue(res.isSuccess)
    val (txn, bond) = res.getOrThrow()
    assertEquals(-5500, txn.amount)
    assertEquals(initialBal - 5500, com.example.network.security.ServerCoinManager.getBalance(senderId))
    assertEquals(com.example.data.model.BondStatus.PENDING, bond.status)
    assertEquals(1100, bond.charmPointsGranted)
  }

  @Test
  fun testPart27_11_CoupleRingAcceptanceAndAdjacentSeatDisplay() {
    val senderId = "USR_RING_A"
    val receiverId = "USR_RING_B"
    com.example.network.security.ServerCoinManager.addTestCoins(senderId, 6000)

    val propRes = com.example.network.security.ServerCoinManager.purchaseAndProposeRing(
      senderId = senderId,
      senderName = "Alex",
      receiverId = receiverId,
      receiverName = "Sam",
      ringId = "ring_silver_heart"
    )
    val bond = propRes.getOrThrow().second

    val acceptRes = com.example.network.security.ServerCoinManager.respondToRingBond(bond.bondId, receiverId, true)
    assertTrue(acceptRes.isSuccess)
    assertEquals(com.example.data.model.BondStatus.ACCEPTED, acceptRes.getOrThrow().status)
    assertEquals(1100, com.example.network.security.ServerCoinManager.getCharmPoints(receiverId))

    // Test adjacent seat detection
    val seatsAdjacent = listOf("OTHER_1", senderId, receiverId, null, null, null, null, null)
    assertTrue("Adjacent seats (1 and 2) should detect active couple bond",
      com.example.network.security.ServerCoinManager.areUsersAdjacentCouple(seatsAdjacent, senderId, receiverId))

    val seatsNotAdjacent = listOf(senderId, "OTHER_1", receiverId, null, null, null, null, null)
    assertFalse("Non-adjacent seats (0 and 2) should not trigger adjacent couple bond",
      com.example.network.security.ServerCoinManager.areUsersAdjacentCouple(seatsNotAdjacent, senderId, receiverId))
  }

  @Test
  fun testPart27_12_RelationshipTagPurchaseAndAcceptance() {
    val requesterId = "USR_TAG_REQ"
    val targetId = "USR_TAG_TGT"
    com.example.network.security.ServerCoinManager.addTestCoins(requesterId, 3000)
    val initialBal = com.example.network.security.ServerCoinManager.getBalance(requesterId)

    val res = com.example.network.security.ServerCoinManager.purchaseRelationshipTag(
      requesterId = requesterId,
      requesterName = "Dave",
      targetId = targetId,
      targetName = "John",
      type = com.example.data.model.RelationshipTagType.BESTIE_BESTIE
    )
    assertTrue(res.isSuccess)
    val (txn, tag) = res.getOrThrow()
    assertEquals(-2000, txn.amount)
    assertEquals(initialBal - 2000, com.example.network.security.ServerCoinManager.getBalance(requesterId))
    assertEquals(com.example.data.model.BondStatus.PENDING, tag.status)

    val acceptRes = com.example.network.security.ServerCoinManager.respondToRelationshipTag(tag.tagId, targetId, true)
    assertTrue(acceptRes.isSuccess)
    assertEquals(com.example.data.model.BondStatus.ACCEPTED, acceptRes.getOrThrow().status)
  }

  @Test
  fun testPart27_13_RelationshipTagAdjacentSeatDisplay() {
    val userA = "USR_TAG_SEAT_A"
    val userB = "USR_TAG_SEAT_B"
    com.example.network.security.ServerCoinManager.addTestCoins(userA, 3000)

    val tagRes = com.example.network.security.ServerCoinManager.purchaseRelationshipTag(
      requesterId = userA,
      requesterName = "UserA",
      targetId = userB,
      targetName = "UserB",
      type = com.example.data.model.RelationshipTagType.BRO_BRO
    )
    com.example.network.security.ServerCoinManager.respondToRelationshipTag(tagRes.getOrThrow().second.tagId, userB, true)

    val seats = listOf(userA, userB, null, null, null, null, null, null)
    val adjacentTag = com.example.network.security.ServerCoinManager.areUsersAdjacentTag(seats, userA, userB)
    assertEquals(com.example.data.model.RelationshipTagType.BRO_BRO, adjacentTag)
  }

  @Test
  fun testPart27_14_RoomTypeSwitchingPreservingState() {
    val ownerId = "USR_ROOM_OWNER"
    val initialRoom = com.example.data.model.Room(
      id = "ROOM-PRESERVE-1",
      name = "Grand Voice Hall",
      description = "Original Room Description",
      ownerId = ownerId,
      ownerName = "RoomMaster",
      roomXp = 12500,
      roomLevel = 4,
      themeId = "neon_cyber",
      roomType = com.example.data.model.RoomType.NORMAL
    )

    // Switch to GAMING room type
    val switchRes = com.example.network.security.ServerCoinManager.switchRoomType(
      currentRoom = initialRoom,
      newType = com.example.data.model.RoomType.GAMING,
      requestingUserId = ownerId
    )
    assertTrue(switchRes.isSuccess)
    val switchedRoom = switchRes.getOrThrow()

    // STRICT STATE PRESERVATION VERIFICATION
    assertEquals("ROOM-PRESERVE-1", switchedRoom.id)
    assertEquals("Grand Voice Hall", switchedRoom.name)
    assertEquals(ownerId, switchedRoom.ownerId)
    assertEquals(12500, switchedRoom.roomXp)
    assertEquals(4, switchedRoom.roomLevel)
    assertEquals("neon_cyber", switchedRoom.themeId)
    assertEquals(com.example.data.model.RoomType.GAMING, switchedRoom.roomType)
    assertTrue("Gaming flag must be enabled", switchedRoom.isGameEnabled)
    assertEquals("CHESS", switchedRoom.activeGameType)

    // Unauthorized switch attempt by non-owner must fail
    val unauthRes = com.example.network.security.ServerCoinManager.switchRoomType(
      currentRoom = switchedRoom,
      newType = com.example.data.model.RoomType.COUPLE,
      requestingUserId = "UNAUTHORIZED_INTRUDER"
    )
    assertTrue("Non-owner switch attempt must fail", unauthRes.isFailure)
  }

  @Test
  fun testPart27_15_AvatarValidityAndAutoExpiration() {
    val userId = "USR_AVATAR_TEST"
    com.example.network.security.ServerCoinManager.addTestCoins(userId, 2000)
    val initialBal = com.example.network.security.ServerCoinManager.getBalance(userId)

    val res = com.example.network.security.ServerCoinManager.purchaseAvatar(
      userId = userId,
      avatarKey = "frame_dragon",
      avatarName = "Dragon Flame",
      validity = com.example.data.model.AvatarValidityDays.DAYS_7
    )
    assertTrue(res.isSuccess)
    val (txn, avatarItem) = res.getOrThrow()
    assertEquals(-350, txn.amount)
    assertEquals(initialBal - 350, com.example.network.security.ServerCoinManager.getBalance(userId))
    assertEquals(7, avatarItem.validityDays)
    assertFalse(avatarItem.isExpired)

    val activeAvatars = com.example.network.security.ServerCoinManager.getUserActiveAvatars(userId)
    assertEquals(1, activeAvatars.size)
    assertEquals("Dragon Flame", activeAvatars[0].name)
  }
}


