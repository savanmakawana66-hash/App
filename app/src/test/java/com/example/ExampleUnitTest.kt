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
}

