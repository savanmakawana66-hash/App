package com.example

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.*
import com.example.data.model.games.*
import com.example.ui.components.*
import com.example.ui.screens.dm.DmScreen
import com.example.ui.screens.games.*
import com.example.ui.screens.notifications.NotificationsSheet
import com.example.ui.screens.profile.EditProfileDialog
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.room.RoomDetailScreen
import com.example.ui.screens.room.RoomListScreen
import com.example.ui.screens.settings.GlobalAdminScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.store.StoreScreen
import com.example.ui.screens.watchparty.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BottomNavTab
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
  private val _isInPipMode = mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        WatchTogetherApp(
          isInPipMode = _isInPipMode.value,
          onEnterPip = {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
              try {
                val params = android.app.PictureInPictureParams.Builder()
                  .setAspectRatio(android.util.Rational(16, 9))
                  .build()
                enterPictureInPictureMode(params)
              } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to enter PiP mode", e)
              }
            }
          }
        )
      }
    }
  }

  override fun onPictureInPictureModeChanged(
    isInPictureInPictureMode: Boolean,
    newConfig: android.content.res.Configuration
  ) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    _isInPipMode.value = isInPictureInPictureMode
  }
}

@Composable
fun WatchTogetherApp(
  viewModel: MainViewModel = viewModel(),
  isInPipMode: Boolean = false,
  onEnterPip: () -> Unit = {}
) {
  val context = LocalContext.current

  // State collectors
  val currentTab by viewModel.currentTab.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val filteredRooms by viewModel.filteredRooms.collectAsState()
  val currentActiveRoom by viewModel.currentActiveRoom.collectAsState()
  val roomSeats by viewModel.roomSeats.collectAsState()
  val audienceMembers by viewModel.audienceMembers.collectAsState()
  val roomAdmins by viewModel.roomAdmins.collectAsState()
  val roomChatMessages by viewModel.roomChatMessages.collectAsState()
  val friends by viewModel.friends.collectAsState()
  val directMessages by viewModel.directMessages.collectAsState()
  val activeChatFriend by viewModel.activeChatFriend.collectAsState()
  val notifications by viewModel.notifications.collectAsState()
  val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsState()
  val reports by viewModel.reports.collectAsState()
  val appSettings by viewModel.appSettings.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val selectedCategory by viewModel.selectedCategory.collectAsState()

  // Virtual Coin Wallet & Daily Tasks states
  val coinWalletState by viewModel.coinWalletState.collectAsState()
  val showDailyTasksDialog by viewModel.showDailyTasksDialog.collectAsState()
  val showCoinHistoryDialog by viewModel.showCoinHistoryDialog.collectAsState()

  // Dialog & Modal state collectors
  val inspectedUser by viewModel.inspectedUser.collectAsState()
  val showOtpDialog by viewModel.showOtpDialog.collectAsState()
  val showCreateRoomDialog by viewModel.showCreateRoomDialog.collectAsState()
  val showJoinRoomDialog by viewModel.showJoinRoomDialog.collectAsState()
  val showInviteFriendDialog by viewModel.showInviteFriendDialog.collectAsState()
  val showRoomSettingsDialog by viewModel.showRoomSettingsDialog.collectAsState()
  val showSeatRequestsDialog by viewModel.showSeatRequestsDialog.collectAsState()
  val showNotificationsSheet by viewModel.showNotificationsSheet.collectAsState()
  val showSettingsScreen by viewModel.showSettingsScreen.collectAsState()
  val showGlobalAdminPanel by viewModel.showGlobalAdminPanel.collectAsState()
  val showEditProfileDialog by viewModel.showEditProfileDialog.collectAsState()
  val showQrShareDialog by viewModel.showQrShareDialog.collectAsState()
  val reportTarget by viewModel.reportTarget.collectAsState()

  // Watch Party States
  val activeWatchParty by viewModel.activeWatchParty.collectAsState()
  val activeRoomSessions by viewModel.activeRoomSessions.collectAsState()
  val showHostWatchPartyDialog by viewModel.showHostWatchPartyDialog.collectAsState()
  val showJoinWatchPartyDialog by viewModel.showJoinWatchPartyDialog.collectAsState()

  // Real-Time Games States
  val activeGameSession by viewModel.activeGameSession.collectAsState()
  val isMatchmaking by viewModel.isMatchmaking.collectAsState()
  val matchmakingGameType by viewModel.matchmakingGameType.collectAsState()
  val showInviteFriendForGame by viewModel.showInviteFriendForGame.collectAsState()
  val showJoinGameCodeDialog by viewModel.showJoinGameCodeDialog.collectAsState()
  val pendingGameInvites by viewModel.pendingGameInvites.collectAsState()
  val connectionStatus by viewModel.connectionStatus.collectAsState()

  // Audio permission launcher
  val audioPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      Toast.makeText(context, "Microphone enabled", Toast.LENGTH_SHORT).show()
      viewModel.networkEngine.voiceRoomManager.onPermissionGranted()
    } else {
      Toast.makeText(context, "Microphone permission required for voice chat", Toast.LENGTH_SHORT).show()
    }
  }

  // Handle Event snackbars / toasts
  LaunchedEffect(Unit) {
    viewModel.eventMessages.collect { msg ->
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // Top-Level Screen Routing
    when {
      // 0. Active Real-Time Game Session (Chess or Ludo)
      activeGameSession != null -> {
        BackHandler { viewModel.leaveActiveGame() }
        when (activeGameSession!!.gameType) {
          GameType.CHESS -> {
            ChessGameScreen(
              session = activeGameSession!!,
              currentUserId = currentUser.id,
              onMakeMove = { from, to -> viewModel.makeChessMove(from, to) },
              onResign = { viewModel.resignChess() },
              onOfferDraw = { viewModel.offerChessDraw() },
              onRespondDraw = { accept -> viewModel.respondChessDraw(accept) },
              onRequestRematch = { viewModel.requestGameRematch() },
              onLeaveGame = { viewModel.leaveActiveGame() }
            )
          }
          GameType.LUDO -> {
            LudoGameScreen(
              session = activeGameSession!!,
              currentUserId = currentUser.id,
              onRollDice = { viewModel.rollLudoDice() },
              onMoveToken = { tokenId -> viewModel.moveLudoToken(tokenId) },
              onRequestRematch = { viewModel.requestGameRematch() },
              onLeaveGame = { viewModel.leaveActiveGame() }
            )
          }
        }
      }

      // 1. Active Watch Party & Voice Room (Local Media Streaming to Remote Friends)
      activeWatchParty != null -> {
        BackHandler { viewModel.leaveWatchParty() }
        WatchPartyStageScreen(
          engine = viewModel.networkEngine,
          party = activeWatchParty!!,
          isInPipMode = isInPipMode,
          onEnterPip = onEnterPip,
          onLeave = { viewModel.leaveWatchParty() }
        )
      }

      // 1. In-Room View (Synchronized 8-Seat Watch Party Room)
      currentActiveRoom != null -> {
        BackHandler { viewModel.leaveRoom() }
        RoomDetailScreen(
          room = currentActiveRoom!!,
          currentUser = currentUser,
          seats = roomSeats,
          audience = audienceMembers,
          admins = roomAdmins[currentActiveRoom!!.id] ?: emptyList(),
          chatMessages = roomChatMessages,
          onLeaveRoom = { viewModel.leaveRoom() },
          onTakeSeat = { seatIdx ->
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            viewModel.takeSeat(seatIdx)
          },
          onLeaveSeat = { viewModel.leaveSeat() },
          onRequestSeat = { viewModel.requestSeat() },
          onOpenSeatRequests = { viewModel.openSeatRequests() },
          onToggleMic = {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            viewModel.toggleMicMute()
          },
          onToggleRaiseHand = { viewModel.toggleRaiseHand() },
          onTogglePlayPause = { viewModel.togglePlayPause() },
          onSeekVideo = { pos -> viewModel.seekVideo(pos) },
          onForward10 = { viewModel.forward10() },
          onRewind10 = { viewModel.rewind10() },
          onChangeVideo = { title, dur -> viewModel.changeVideo(title, dur) },
          canControlVideo = viewModel.canControlVideo(),
          onSendChatMessage = { text -> viewModel.sendChatMessage(text) },
          onAddReaction = { msgId, emoji -> viewModel.addChatReaction(msgId, emoji) },
          onDeleteMessage = { msgId -> viewModel.deleteChatMessage(msgId) },
          onInspectUser = { friend -> viewModel.inspectUser(friend) },
          onOpenRoomSettings = { viewModel.openRoomSettings() },
          onOpenInvite = { viewModel.openInviteFriend() },
          onReportMessage = { msgId, senderName -> viewModel.openReport("Message", msgId, senderName) },
          onSendGift = { giftName, emoji, cost -> viewModel.sendRoomGift(currentActiveRoom!!.id, giftName, emoji, cost) },
          onToggleFollowRoom = { viewModel.toggleFollowRoom(currentActiveRoom!!.id) },
          onUpdateVideoAudioSettings = { videoVol, isMuted, micVol -> viewModel.updateVideoAudioSettings(currentActiveRoom!!.id, videoVol, isMuted, micVol) },
          onUpdateRoomTheme = { themeId, config -> viewModel.updateRoomTheme(currentActiveRoom!!.id, themeId, config) },
          onUpdateRoomFullConfig = { name, desc, ann, mode, locked, seatPerm, chatPerm, isVid, isGame, gameType ->
            viewModel.updateRoomFullConfig(currentActiveRoom!!.id, name, desc, ann, mode, locked, seatPerm, chatPerm, isVid, isGame, gameType)
          },
          onSetRoomGame = { gameType, isEnabled -> viewModel.setRoomGame(currentActiveRoom!!.id, gameType, isEnabled) },
          onAddAdmin = { userId, username -> viewModel.addRoomAdmin(currentActiveRoom!!.id, userId, username) },
          onRemoveAdmin = { userId -> viewModel.removeRoomAdmin(currentActiveRoom!!.id, userId) },
          onDeleteRoom = { viewModel.deleteRoom(currentActiveRoom!!.id) },
          onTogglePartnerSeats = { enabled -> viewModel.togglePartnerSeats(currentActiveRoom!!.id, enabled) },
          availableRooms = filteredRooms,
          onSwitchRoom = { targetRoomId ->
            viewModel.leaveRoom()
            viewModel.joinRoom(targetRoomId)
          }
        )
      }

      // 2. Global Super-Admin Console
      showGlobalAdminPanel -> {
        BackHandler { viewModel.closeGlobalAdminPanel() }
        GlobalAdminScreen(
          reports = reports,
          onClose = { viewModel.closeGlobalAdminPanel() },
          onResolveReport = { repId, action -> viewModel.resolveReport(repId, action) },
          onBanUser = { uId -> viewModel.banUserGlobally(uId) },
          onBroadcastAnnouncement = { title, msg -> viewModel.broadcastAnnouncement(title, msg) }
        )
      }

      // 3. App Settings Screen
      showSettingsScreen -> {
        BackHandler { viewModel.closeSettings() }
        SettingsScreen(
          currentUser = currentUser,
          settings = appSettings,
          onClose = { viewModel.closeSettings() },
          onUpdateSettings = { s -> viewModel.updateAppSettings(s) },
          onOpenOtpVerification = { viewModel.openOtpDialog() },
          onOpenGlobalAdmin = { viewModel.openGlobalAdminPanel() },
          onLogout = { viewModel.logout() }
        )
      }

      // 4. In-App Notifications Sheet
      showNotificationsSheet -> {
        BackHandler { viewModel.closeNotifications() }
        NotificationsSheet(
          notifications = notifications,
          onDismiss = { viewModel.closeNotifications() },
          onMarkRead = { id -> viewModel.markNotificationAsRead(id) },
          onClearAll = { viewModel.clearAllNotifications() },
          onActionClick = { roomId ->
            viewModel.closeNotifications()
            if (roomId != null) {
              viewModel.joinRoom(roomId)
            }
          }
        )
      }

      // 5. PRIMARY 3-TAB BOTTOM NAVIGATION SECTIONS: 1. ROOM, 2. DM, 3. PROFILE
      else -> {
        Scaffold(
          containerColor = DarkBackground,
          bottomBar = {
            // PREMIUM BOTTOM NAVIGATION
            Surface(
              color = DarkSurface,
              border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
              tonalElevation = 6.dp,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("primary_bottom_navigation")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .navigationBarsPadding()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // 1. ROOM
                PremiumBottomNavItem(
                  selected = currentTab == BottomNavTab.ROOM,
                  onClick = { viewModel.selectTab(BottomNavTab.ROOM) },
                  icon = if (currentTab == BottomNavTab.ROOM) Icons.Default.MeetingRoom else Icons.Outlined.MeetingRoom,
                  label = "ROOM",
                  testTag = "tab_room"
                )

                // 2. GAMES
                PremiumBottomNavItem(
                  selected = currentTab == BottomNavTab.GAMES,
                  onClick = { viewModel.selectTab(BottomNavTab.GAMES) },
                  icon = if (currentTab == BottomNavTab.GAMES) Icons.Default.SportsEsports else Icons.Outlined.SportsEsports,
                  label = "GAMES",
                  testTag = "tab_games"
                )

                // 3. STORE
                PremiumBottomNavItem(
                  selected = currentTab == BottomNavTab.STORE,
                  onClick = { viewModel.selectTab(BottomNavTab.STORE) },
                  icon = if (currentTab == BottomNavTab.STORE) Icons.Default.Storefront else Icons.Outlined.Storefront,
                  label = "STORE",
                  testTag = "tab_store"
                )

                // 4. DM
                val hasPending = friends.any { it.friendshipStatus == com.example.data.model.FriendshipStatus.PENDING }
                PremiumBottomNavItem(
                  selected = currentTab == BottomNavTab.DM,
                  onClick = { viewModel.selectTab(BottomNavTab.DM) },
                  icon = if (currentTab == BottomNavTab.DM) Icons.Default.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                  label = "DM",
                  testTag = "tab_dm",
                  badgeCount = if (hasPending) 1 else 0
                )

                // 5. PROFILE
                PremiumBottomNavItem(
                  selected = currentTab == BottomNavTab.PROFILE,
                  onClick = { viewModel.selectTab(BottomNavTab.PROFILE) },
                  icon = if (currentTab == BottomNavTab.PROFILE) Icons.Default.Person else Icons.Outlined.Person,
                  label = "PROFILE",
                  testTag = "tab_profile"
                )
              }
            }
          }
        ) { paddingValues ->
          if (currentTab != BottomNavTab.ROOM) {
            BackHandler { viewModel.selectTab(BottomNavTab.ROOM) }
          }
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues)
          ) {
            when (currentTab) {
              BottomNavTab.ROOM -> {
                RoomListScreen(
                  currentUser = currentUser,
                  activeRoomSessions = activeRoomSessions,
                  rooms = filteredRooms,
                  searchQuery = searchQuery,
                  selectedCategory = selectedCategory,
                  unreadNotifications = unreadNotificationsCount,
                  onSearchChange = { q -> viewModel.setSearchQuery(q) },
                  onSelectCategory = { c -> viewModel.selectCategory(c) },
                  onCreateRoomClick = { viewModel.openCreateRoomDialog() },
                  onJoinRoomClick = { viewModel.openJoinRoomDialog() },
                  onHostWatchPartyClick = { viewModel.openHostWatchParty() },
                  onJoinWatchPartyClick = { viewModel.openJoinWatchParty() },
                  onRoomClick = { rId -> viewModel.joinRoom(rId) },
                  onActiveRoomSessionClick = { session -> viewModel.joinActiveRoomSession(session) },
                  onToggleFavourite = { rId -> viewModel.toggleFavourite(rId) },
                  onOpenNotifications = { viewModel.openNotifications() },
                  onVerifyGuestClick = { viewModel.openOtpDialog() },
                  onOpenProfile = { viewModel.selectTab(BottomNavTab.PROFILE) }
                )
              }

              BottomNavTab.GAMES -> {
                GamesHomeScreen(
                  currentUser = currentUser,
                  onPlayRandom = { gType -> viewModel.startMatchmaking(gType) },
                  onPlayWithFriend = { gType -> viewModel.openPlayWithFriend(gType) },
                  onPlayVsBot = { gType -> viewModel.startBotGame(gType) },
                  onJoinByCode = { viewModel.openJoinGameCode() }
                )
              }

              BottomNavTab.STORE -> {
                StoreScreen(
                  currentUser = currentUser,
                  onPurchaseCoinPackage = { pkgId -> viewModel.purchaseCoinPackage(pkgId) },
                  onOpenMysteryBox = { viewModel.openMysteryBox() },
                  onEquipRing = { ringId -> viewModel.equipRing(ringId) },
                  onEquipNameTag = { tag -> viewModel.equipNameTag(tag) },
                  onEquipProfileFrame = { frame -> viewModel.equipProfileFrame(frame) },
                  onEquipCharmBadge = { badge -> viewModel.equipCharmBadge(badge) }
                )
              }

              BottomNavTab.DM -> {
                DmScreen(
                  currentUser = currentUser,
                  friends = friends,
                  directMessages = directMessages,
                  activeChatFriend = activeChatFriend,
                  currentActiveRoom = currentActiveRoom,
                  onOpenChat = { f -> viewModel.openDmChat(f) },
                  onCloseChat = { viewModel.closeDmChat() },
                  onSendMessage = { rId, text, isInvite, roomId, roomName ->
                    viewModel.sendDirectMessage(rId, text, isInvite, roomId, roomName)
                  },
                  onDeleteMessage = { fId, mId -> viewModel.deleteDirectMessage(fId, mId) },
                  onAcceptFriendRequest = { uId -> viewModel.acceptFriendRequest(uId) },
                  onRejectFriendRequest = { uId -> viewModel.rejectFriendRequest(uId) },
                  onSendFriendRequest = { uId, uname -> viewModel.sendFriendRequest(uId, uname) },
                  onInspectUser = { f -> viewModel.inspectUser(f) },
                  onJoinRoomFromInvite = { rId -> viewModel.joinRoom(rId) },
                  onChallengeGame = { friend, gType -> viewModel.challengeFriendToGame(friend, gType) },
                  onAcceptGameInvite = { inviteId -> viewModel.acceptGameInvite(inviteId) },
                  pendingGameInvites = pendingGameInvites
                )
              }

              BottomNavTab.PROFILE -> {
                ProfileScreen(
                  currentUser = currentUser,
                  rooms = filteredRooms,
                  coinWalletState = coinWalletState,
                  onOpenDailyTasks = { viewModel.openDailyTasks() },
                  onOpenCoinHistory = { viewModel.openCoinHistory() },
                  onAddTestBonus = { viewModel.addTestCoins(600) },
                  onOpenEditProfile = { viewModel.openEditProfile() },
                  onOpenSettings = { viewModel.openSettings() },
                  onOpenOtpVerification = { viewModel.openOtpDialog() },
                  onRoomClick = { rId -> viewModel.joinRoom(rId) },
                  onToggleFavourite = { rId -> viewModel.toggleFavourite(rId) }
                )
              }
            }
          }
        }
      }
    }

    // Modal Dialogs Layer

    // Daily Tasks Modal
    if (showDailyTasksDialog) {
      DailyTasksDialog(
        walletState = coinWalletState,
        onClaimTask = { taskId -> viewModel.claimDailyTask(taskId) },
        onDismiss = { viewModel.closeDailyTasks() }
      )
    }

    // Authoritative Coin History Modal
    if (showCoinHistoryDialog) {
      CoinHistoryDialog(
        walletState = coinWalletState,
        onDismiss = { viewModel.closeCoinHistory() }
      )
    }

    // OTP Verification Modal
    if (showOtpDialog) {
      OtpVerificationDialog(
        onDismiss = { viewModel.closeOtpDialog() },
        onVerify = { phone, otp -> viewModel.verifyOtp(phone, otp) }
      )
    }

    // Create Room Modal
    if (showCreateRoomDialog) {
      CreateRoomDialog(
        onDismiss = { viewModel.closeCreateRoomDialog() },
        userCoins = coinWalletState.balance,
        onCreate = { name, desc, isPrivate, pass, welcome, rules, cat ->
          viewModel.createRoom(name, desc, isPrivate, pass, welcome, rules, cat)
        }
      )
    }

    // Join Room Modal
    if (showJoinRoomDialog) {
      JoinRoomDialog(
        onDismiss = { viewModel.closeJoinRoomDialog() },
        onJoin = { rId, pass ->
          val success = viewModel.joinRoom(rId, pass)
          if (!success) {
            Toast.makeText(context, "Room not found or incorrect password", Toast.LENGTH_SHORT).show()
          }
        }
      )
    }

    // Host Watch Party Modal (Pick local video/music & stream over Internet)
    if (showHostWatchPartyDialog) {
      HostWatchPartyDialog(
        engine = viewModel.networkEngine,
        onDismiss = { viewModel.closeHostWatchParty() },
        onStartParty = { media, quality, roomName, password ->
          audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
          viewModel.startHostWatchParty(media, quality, roomName, password)
        }
      )
    }

    // Join Watch Party Modal (Enter Room Code to watch Host's stream)
    if (showJoinWatchPartyDialog) {
      JoinWatchPartyDialog(
        engine = viewModel.networkEngine,
        onDismiss = { viewModel.closeJoinWatchParty() },
        onJoinSuccess = {
          audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
          viewModel.closeJoinWatchParty()
        }
      )
    }

    // Inspect User Profile Modal
    if (inspectedUser != null) {
      val friend = inspectedUser!!
      UserProfileInspectionDialog(
        friend = friend,
        onDismiss = { viewModel.closeInspectUser() },
        onAddFriend = {
          viewModel.sendFriendRequest(friend.userId, friend.username)
          viewModel.closeInspectUser()
        },
        onMessage = {
          viewModel.closeInspectUser()
          viewModel.openDmChat(friend)
        },
        onInviteToRoom = {
          if (currentActiveRoom != null) {
            viewModel.sendDirectMessage(
              receiverId = friend.userId,
              text = "Hey! Join my room ${currentActiveRoom!!.name}",
              isRoomInvite = true,
              roomId = currentActiveRoom!!.id,
              roomName = currentActiveRoom!!.name
            )
            Toast.makeText(context, "Room invite sent to ${friend.username}!", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(context, "Join or create a room first to invite friends!", Toast.LENGTH_SHORT).show()
          }
          viewModel.closeInspectUser()
        },
        onBlock = {
          viewModel.blockUser(friend.userId, friend.username)
          viewModel.closeInspectUser()
        },
        onReport = {
          viewModel.openReport("User", friend.userId, friend.username)
          viewModel.closeInspectUser()
        }
      )
    }

    // Room Settings Modal
    if (showRoomSettingsDialog && currentActiveRoom != null) {
      RoomSettingsDialog(
        room = currentActiveRoom!!,
        admins = roomAdmins[currentActiveRoom!!.id] ?: emptyList(),
        isOwner = currentActiveRoom!!.ownerId == currentUser.id,
        onDismiss = { viewModel.closeRoomSettings() },
        onSave = { name, desc, welcome, rules, mode, locked ->
          viewModel.updateRoomSettings(currentActiveRoom!!.id, name, desc, welcome, rules, mode, locked)
        },
        onAddAdmin = { uId, uname -> viewModel.addRoomAdmin(currentActiveRoom!!.id, uId, uname) },
        onRemoveAdmin = { uId -> viewModel.removeRoomAdmin(currentActiveRoom!!.id, uId) },
        onDeleteRoom = {
          viewModel.deleteRoom(currentActiveRoom!!.id)
          viewModel.closeRoomSettings()
        }
      )
    }

    // Seat Requests Modal
    if (showSeatRequestsDialog) {
      SeatRequestsDialog(
        requests = audienceMembers.filter { it.hasRequestedSeat },
        onDismiss = { viewModel.closeSeatRequests() },
        onApprove = { uId -> viewModel.approveSeatRequest(uId) }
      )
    }

    // Room Invite Modal
    if (showInviteFriendDialog && currentActiveRoom != null) {
      RoomInviteDialog(
        room = currentActiveRoom!!,
        friends = friends,
        onDismiss = { viewModel.closeInviteFriend() },
        onInviteFriend = { fId ->
          viewModel.sendDirectMessage(
            receiverId = fId,
            text = "Join my watch room: ${currentActiveRoom!!.name}",
            isRoomInvite = true,
            roomId = currentActiveRoom!!.id,
            roomName = currentActiveRoom!!.name
          )
          Toast.makeText(context, "Invite sent!", Toast.LENGTH_SHORT).show()
        },
        onOpenQr = {
          viewModel.closeInviteFriend()
          viewModel.openQrShare()
        }
      )
    }

    // QR Share Modal
    if (showQrShareDialog && currentActiveRoom != null) {
      QrShareDialog(
        room = currentActiveRoom!!,
        onDismiss = { viewModel.closeQrShare() }
      )
    }

    // Report Item Modal
    if (reportTarget != null) {
      ReportDialog(
        targetInfo = reportTarget!!,
        onDismiss = { viewModel.closeReport() },
        onSubmit = { reason ->
          viewModel.submitReport(reason)
          Toast.makeText(context, "Report submitted for moderation review.", Toast.LENGTH_SHORT).show()
        }
      )
    }

    // Edit Profile Modal
    if (showEditProfileDialog) {
      EditProfileDialog(
        currentUser = currentUser,
        onDismiss = { viewModel.closeEditProfile() },
        onSave = { name, bio, avatar, frame, emoji, status ->
          viewModel.updateProfile(name, bio, avatar, frame, emoji, status)
        }
      )
    }

    // Matchmaking Searching Dialog (Strictly Real Players Only)
    if (isMatchmaking) {
      MatchmakingSearchingDialog(
        gameType = matchmakingGameType,
        isSearching = isMatchmaking,
        onCancel = { viewModel.cancelMatchmaking() },
        onRetry = { viewModel.startMatchmaking(matchmakingGameType) }
      )
    }

    // Play with Friend Dialog
    showInviteFriendForGame?.let { gType ->
      GameInviteFriendDialog(
        gameType = gType,
        friends = friends,
        onInviteFriend = { friend -> viewModel.challengeFriendToGame(friend, gType) },
        onCreatePrivateCode = { viewModel.createPrivateGame(gType) },
        onDismiss = { viewModel.closePlayWithFriend() }
      )
    }

    // Join Game Code Dialog
    if (showJoinGameCodeDialog) {
      JoinGameCodeDialog(
        onJoinCode = { code -> viewModel.joinGameByCode(code) },
        onDismiss = { viewModel.closeJoinGameCode() }
      )
    }

    // Incoming Game Challenge Alert (Real-time invite reception)
    val incomingGameInvite = pendingGameInvites.firstOrNull { it.recipientId == currentUser.id }
    if (incomingGameInvite != null && activeGameSession == null) {
      AlertDialog(
        onDismissRequest = { viewModel.declineGameInvite(incomingGameInvite.inviteId) },
        containerColor = DarkSurface,
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(incomingGameInvite.gameType.icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Game Challenge!", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
          }
        },
        text = {
          Column {
            Text(
              text = "${incomingGameInvite.senderName} challenged you to play ${incomingGameInvite.gameType.displayName}!",
              fontSize = 14.sp,
              color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Real-time match is waiting for you.", fontSize = 12.sp, color = TextSecondary)
          }
        },
        confirmButton = {
          Button(
            onClick = { viewModel.acceptGameInvite(incomingGameInvite.inviteId) },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline)
          ) {
            Text("Accept & Play", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { viewModel.declineGameInvite(incomingGameInvite.inviteId) }) {
            Text("Decline", color = TextSecondary)
          }
        }
      )
    }

    // Internet Connectivity Status Banner (Offline / Reconnecting)
    if (connectionStatus != com.example.network.ConnectionStatus.ONLINE) {
      Surface(
        color = if (connectionStatus == com.example.network.ConnectionStatus.OFFLINE) Color(0xFFC62828) else Color(0xFFEF6C00),
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopCenter)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = if (connectionStatus == com.example.network.ConnectionStatus.OFFLINE) Icons.Default.WifiOff else Icons.Default.Sync,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (connectionStatus == com.example.network.ConnectionStatus.OFFLINE)
              "You are offline. Reconnecting automatically when online."
            else
              "Reconnecting to secure server…",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }
}

@Composable
private fun PremiumBottomNavItem(
  selected: Boolean,
  onClick: () -> Unit,
  icon: ImageVector,
  label: String,
  testTag: String,
  badgeCount: Int = 0
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(22.dp))
      .then(
        if (selected) {
          Modifier.background(Brush.horizontalGradient(listOf(BrandPrimary, MagentaAccent)))
        } else {
          Modifier
        }
      )
      .clickable(onClick = onClick)
      .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 9.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      BadgedBox(
        badge = {
          if (badgeCount > 0) {
            Badge(containerColor = AmberXp)
          }
        }
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (selected) Color.White else TextSecondary,
          modifier = Modifier.size(20.dp)
        )
      }
      if (selected) {
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = label,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }
  }
}

