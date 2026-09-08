package com.example

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.screens.dm.DmScreen
import com.example.ui.screens.notifications.NotificationsSheet
import com.example.ui.screens.profile.EditProfileDialog
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.room.RoomDetailScreen
import com.example.ui.screens.room.RoomListScreen
import com.example.ui.screens.settings.GlobalAdminScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.BottomNavTab
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        WatchTogetherApp()
      }
    }
  }
}

@Composable
fun WatchTogetherApp(
  viewModel: MainViewModel = viewModel()
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

  // Audio permission launcher
  val audioPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      Toast.makeText(context, "Microphone enabled", Toast.LENGTH_SHORT).show()
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
      // 1. In-Room View (Synchronized 8-Seat Watch Party Room)
      currentActiveRoom != null -> {
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
          onReportMessage = { msgId, senderName -> viewModel.openReport("Message", msgId, senderName) }
        )
      }

      // 2. Global Super-Admin Console
      showGlobalAdminPanel -> {
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
            // STRICT SPECIFICATION: ONLY 3 PRIMARY BOTTOM-NAVIGATION SECTIONS
            NavigationBar(
              containerColor = DarkSurface,
              tonalElevation = 8.dp,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("primary_bottom_navigation")
            ) {
              // 1. 🏠 ROOM
              NavigationBarItem(
                selected = currentTab == BottomNavTab.ROOM,
                onClick = { viewModel.selectTab(BottomNavTab.ROOM) },
                icon = {
                  Icon(
                    imageVector = if (currentTab == BottomNavTab.ROOM) Icons.Default.MeetingRoom else Icons.Outlined.MeetingRoom,
                    contentDescription = "ROOM"
                  )
                },
                label = {
                  Text(
                    text = "🏠 ROOM",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == BottomNavTab.ROOM) FontWeight.Bold else FontWeight.Normal
                  )
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = IndigoLight,
                  selectedTextColor = IndigoLight,
                  indicatorColor = IndigoDark,
                  unselectedIconColor = TextSecondary,
                  unselectedTextColor = TextSecondary
                ),
                modifier = Modifier.testTag("tab_room")
              )

              // 2. 💬 DM
              NavigationBarItem(
                selected = currentTab == BottomNavTab.DM,
                onClick = { viewModel.selectTab(BottomNavTab.DM) },
                icon = {
                  val hasPending = friends.any { it.friendshipStatus == com.example.data.model.FriendshipStatus.PENDING }
                  BadgedBox(
                    badge = {
                      if (hasPending) {
                        Badge(containerColor = AmberXp)
                      }
                    }
                  ) {
                    Icon(
                      imageVector = if (currentTab == BottomNavTab.DM) Icons.Default.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                      contentDescription = "DM"
                    )
                  }
                },
                label = {
                  Text(
                    text = "💬 DM",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == BottomNavTab.DM) FontWeight.Bold else FontWeight.Normal
                  )
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = IndigoLight,
                  selectedTextColor = IndigoLight,
                  indicatorColor = IndigoDark,
                  unselectedIconColor = TextSecondary,
                  unselectedTextColor = TextSecondary
                ),
                modifier = Modifier.testTag("tab_dm")
              )

              // 3. 👤 PROFILE
              NavigationBarItem(
                selected = currentTab == BottomNavTab.PROFILE,
                onClick = { viewModel.selectTab(BottomNavTab.PROFILE) },
                icon = {
                  Icon(
                    imageVector = if (currentTab == BottomNavTab.PROFILE) Icons.Default.Person else Icons.Outlined.Person,
                    contentDescription = "PROFILE"
                  )
                },
                label = {
                  Text(
                    text = "👤 PROFILE",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == BottomNavTab.PROFILE) FontWeight.Bold else FontWeight.Normal
                  )
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = IndigoLight,
                  selectedTextColor = IndigoLight,
                  indicatorColor = IndigoDark,
                  unselectedIconColor = TextSecondary,
                  unselectedTextColor = TextSecondary
                ),
                modifier = Modifier.testTag("tab_profile")
              )
            }
          }
        ) { paddingValues ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues)
          ) {
            when (currentTab) {
              BottomNavTab.ROOM -> {
                RoomListScreen(
                  currentUser = currentUser,
                  rooms = filteredRooms,
                  searchQuery = searchQuery,
                  selectedCategory = selectedCategory,
                  unreadNotifications = unreadNotificationsCount,
                  onSearchChange = { q -> viewModel.setSearchQuery(q) },
                  onSelectCategory = { c -> viewModel.selectCategory(c) },
                  onCreateRoomClick = { viewModel.openCreateRoomDialog() },
                  onJoinRoomClick = { viewModel.openJoinRoomDialog() },
                  onRoomClick = { rId -> viewModel.joinRoom(rId) },
                  onToggleFavourite = { rId -> viewModel.toggleFavourite(rId) },
                  onOpenNotifications = { viewModel.openNotifications() },
                  onVerifyGuestClick = { viewModel.openOtpDialog() }
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
                  onJoinRoomFromInvite = { rId -> viewModel.joinRoom(rId) }
                )
              }

              BottomNavTab.PROFILE -> {
                ProfileScreen(
                  currentUser = currentUser,
                  rooms = filteredRooms,
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
  }
}
