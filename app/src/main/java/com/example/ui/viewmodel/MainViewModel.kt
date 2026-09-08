package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.WatchTogetherRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class BottomNavTab {
  ROOM,
  DM,
  PROFILE
}

class MainViewModel(
  private val repository: WatchTogetherRepository = WatchTogetherRepository()
) : ViewModel() {

  // Primary bottom navigation: EXACTLY 3 sections: 1. ROOM, 2. DM, 3. PROFILE
  private val _currentTab = MutableStateFlow(BottomNavTab.ROOM)
  val currentTab: StateFlow<BottomNavTab> = _currentTab.asStateFlow()

  // Repository states
  val currentUser = repository.currentUser
  val rooms = repository.rooms
  val currentActiveRoom = repository.currentActiveRoom
  val roomSeats = repository.roomSeats
  val audienceMembers = repository.audienceMembers
  val roomAdmins = repository.roomAdmins
  val roomChatMessages = repository.roomChatMessages
  val friends = repository.friends
  val directMessages = repository.directMessages
  val notifications = repository.notifications
  val reports = repository.reports
  val announcements = repository.announcements
  val appSettings = repository.appSettings
  val eventMessages = repository.eventMessages

  // Room search & filtering
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedCategory = MutableStateFlow(RoomCategory.ALL)
  val selectedCategory: StateFlow<RoomCategory> = _selectedCategory.asStateFlow()

  // Filtered rooms
  val filteredRooms: StateFlow<List<Room>> = combine(
    rooms,
    searchQuery,
    selectedCategory,
    currentUser
  ) { allRooms, query, category, user ->
    allRooms.filter { room ->
      // Filter by category
      val matchesCategory = when (category) {
        RoomCategory.ALL -> true
        RoomCategory.ACTIVE -> room.category == RoomCategory.ACTIVE || room.audienceCount > 10
        RoomCategory.POPULAR -> room.category == RoomCategory.POPULAR || room.roomLevel >= 3
        RoomCategory.NEW -> room.category == RoomCategory.NEW || room.roomLevel == 1
        RoomCategory.VOICE -> room.category == RoomCategory.VOICE
        RoomCategory.WATCH_TOGETHER -> room.category == RoomCategory.WATCH_TOGETHER
        RoomCategory.MY_ROOMS -> room.ownerId == user.id
        RoomCategory.FAVOURITES -> room.isFavourite
        RoomCategory.RECENT -> true
      }

      // Search by Room ID, Room Name, User ID, Username
      val matchesQuery = if (query.isBlank()) true else {
        room.id.contains(query, ignoreCase = true) ||
            room.name.contains(query, ignoreCase = true) ||
            room.ownerId.contains(query, ignoreCase = true) ||
            room.ownerName.contains(query, ignoreCase = true)
      }

      matchesCategory && matchesQuery
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Active DM chat thread with a friend
  private val _activeChatFriend = MutableStateFlow<Friend?>(null)
  val activeChatFriend: StateFlow<Friend?> = _activeChatFriend.asStateFlow()

  // Inspection profile modal (inspecting another user)
  private val _inspectedUser = MutableStateFlow<Friend?>(null)
  val inspectedUser: StateFlow<Friend?> = _inspectedUser.asStateFlow()

  // Dialog States
  private val _showOtpDialog = MutableStateFlow(false)
  val showOtpDialog: StateFlow<Boolean> = _showOtpDialog.asStateFlow()

  private val _showCreateRoomDialog = MutableStateFlow(false)
  val showCreateRoomDialog: StateFlow<Boolean> = _showCreateRoomDialog.asStateFlow()

  private val _showJoinRoomDialog = MutableStateFlow(false)
  val showJoinRoomDialog: StateFlow<Boolean> = _showJoinRoomDialog.asStateFlow()

  private val _showInviteFriendDialog = MutableStateFlow(false)
  val showInviteFriendDialog: StateFlow<Boolean> = _showInviteFriendDialog.asStateFlow()

  private val _showRoomSettingsDialog = MutableStateFlow(false)
  val showRoomSettingsDialog: StateFlow<Boolean> = _showRoomSettingsDialog.asStateFlow()

  private val _showSeatRequestsDialog = MutableStateFlow(false)
  val showSeatRequestsDialog: StateFlow<Boolean> = _showSeatRequestsDialog.asStateFlow()

  private val _showNotificationsSheet = MutableStateFlow(false)
  val showNotificationsSheet: StateFlow<Boolean> = _showNotificationsSheet.asStateFlow()

  private val _showSettingsScreen = MutableStateFlow(false)
  val showSettingsScreen: StateFlow<Boolean> = _showSettingsScreen.asStateFlow()

  private val _showGlobalAdminPanel = MutableStateFlow(false)
  val showGlobalAdminPanel: StateFlow<Boolean> = _showGlobalAdminPanel.asStateFlow()

  private val _showEditProfileDialog = MutableStateFlow(false)
  val showEditProfileDialog: StateFlow<Boolean> = _showEditProfileDialog.asStateFlow()

  private val _showQrShareDialog = MutableStateFlow(false)
  val showQrShareDialog: StateFlow<Boolean> = _showQrShareDialog.asStateFlow()

  private val _reportTarget = MutableStateFlow<Pair<String, String>?>(null) // (type, id+name)
  val reportTarget: StateFlow<Pair<String, String>?> = _reportTarget.asStateFlow()

  // Unread notification count
  val unreadNotificationsCount: StateFlow<Int> = notifications.map { list ->
    list.count { !it.isRead }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  // Tab navigation
  fun selectTab(tab: BottomNavTab) {
    _currentTab.value = tab
    _activeChatFriend.value = null
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun selectCategory(category: RoomCategory) {
    _selectedCategory.value = category
  }

  // Auth & OTP
  fun openOtpDialog() { _showOtpDialog.value = true }
  fun closeOtpDialog() { _showOtpDialog.value = false }
  fun verifyOtp(phone: String, otp: String): Boolean {
    val success = repository.verifyMobileAndLinkAccount(phone, otp)
    if (success) _showOtpDialog.value = false
    return success
  }
  fun logout() {
    repository.logout()
    _showSettingsScreen.value = false
  }

  // Room Actions
  fun openCreateRoomDialog() { _showCreateRoomDialog.value = true }
  fun closeCreateRoomDialog() { _showCreateRoomDialog.value = false }
  fun createRoom(
    name: String,
    description: String,
    isPrivate: Boolean,
    password: String,
    welcome: String,
    rules: String,
    category: RoomCategory
  ) {
    val room = repository.createRoom(name, description, isPrivate, password, welcome, rules, category)
    _showCreateRoomDialog.value = false
    // Join newly created room
    joinRoom(room.id)
  }

  fun openJoinRoomDialog() { _showJoinRoomDialog.value = true }
  fun closeJoinRoomDialog() { _showJoinRoomDialog.value = false }
  fun joinRoom(roomId: String, password: String = ""): Boolean {
    val success = repository.joinRoom(roomId, password)
    if (success) {
      _showJoinRoomDialog.value = false
      _currentTab.value = BottomNavTab.ROOM
    }
    return success
  }
  fun leaveRoom() { repository.leaveRoom() }
  fun deleteRoom(roomId: String) { repository.deleteRoom(roomId) }
  fun toggleFavourite(roomId: String) { repository.toggleFavouriteRoom(roomId) }

  // Voice Seat Actions
  fun takeSeat(seatIndex: Int) { repository.takeSeat(seatIndex) }
  fun leaveSeat() { repository.leaveSeat() }
  fun requestSeat() { repository.requestSeat() }
  fun approveSeatRequest(userId: String) { repository.approveSeatRequest(userId) }
  fun toggleMicMute() { repository.toggleMicMute() }
  fun toggleRaiseHand() { repository.toggleRaiseHand() }

  // Synchronized Video Actions
  fun togglePlayPause() { repository.togglePlayPause() }
  fun seekVideo(pos: Float) { repository.seekVideo(pos) }
  fun forward10() { repository.forward10Sec() }
  fun rewind10() { repository.rewind10Sec() }
  fun changeVideo(title: String, durationSec: Int) { repository.changeVideo(title, durationSec) }
  fun canControlVideo(): Boolean = repository.canControlVideo()

  // Chat Actions
  fun sendChatMessage(text: String) { repository.sendChatMessage(text) }
  fun addChatReaction(msgId: String, emoji: String) { repository.addMessageReaction(msgId, emoji) }
  fun deleteChatMessage(msgId: String) { repository.deleteChatMessage(msgId) }

  // DM Actions
  fun openDmChat(friend: Friend) {
    _activeChatFriend.value = friend
    _currentTab.value = BottomNavTab.DM
  }
  fun closeDmChat() { _activeChatFriend.value = null }
  fun sendDirectMessage(receiverId: String, text: String, isRoomInvite: Boolean = false, roomId: String? = null, roomName: String? = null) {
    repository.sendDirectMessage(receiverId, text, isRoomInvite, roomId, roomName)
  }
  fun deleteDirectMessage(friendId: String, msgId: String) {
    repository.deleteDirectMessage(friendId, msgId)
  }

  // Friends
  fun sendFriendRequest(userId: String, username: String) { repository.sendFriendRequest(userId, username) }
  fun acceptFriendRequest(userId: String) { repository.acceptFriendRequest(userId) }
  fun rejectFriendRequest(userId: String) { repository.rejectFriendRequest(userId) }
  fun removeFriend(userId: String) { repository.removeFriend(userId) }
  fun blockUser(userId: String, username: String) { repository.blockUser(userId, username) }

  // User Inspection & Actions
  fun inspectUser(friend: Friend) { _inspectedUser.value = friend }
  fun closeInspectUser() { _inspectedUser.value = null }

  // Room Admin Management
  fun openRoomSettings() { _showRoomSettingsDialog.value = true }
  fun closeRoomSettings() { _showRoomSettingsDialog.value = false }
  fun openSeatRequests() { _showSeatRequestsDialog.value = true }
  fun closeSeatRequests() { _showSeatRequestsDialog.value = false }
  fun addRoomAdmin(roomId: String, userId: String, username: String) { repository.addRoomAdmin(roomId, userId, username) }
  fun removeRoomAdmin(roomId: String, userId: String) { repository.removeRoomAdmin(roomId, userId) }
  fun updateRoomSettings(roomId: String, name: String, desc: String, welcome: String, rules: String, mode: VideoControlMode, locked: Boolean) {
    repository.updateRoomSettings(roomId, name, desc, welcome, rules, mode, locked)
    _showRoomSettingsDialog.value = false
  }

  // Profile & Settings
  fun openEditProfile() { _showEditProfileDialog.value = true }
  fun closeEditProfile() { _showEditProfileDialog.value = false }
  fun updateProfile(name: String, bio: String, avatar: String, frame: String, emoji: String, status: String) {
    repository.updateProfile(name, bio, avatar, frame, emoji, status)
    _showEditProfileDialog.value = false
  }

  fun openSettings() { _showSettingsScreen.value = true }
  fun closeSettings() { _showSettingsScreen.value = false }
  fun updateAppSettings(settings: AppSettings) { repository.updateSettings(settings) }

  // Global Admin Panel
  fun openGlobalAdminPanel() { _showGlobalAdminPanel.value = true }
  fun closeGlobalAdminPanel() { _showGlobalAdminPanel.value = false }
  fun banUserGlobally(userId: String) { repository.banUserGlobally(userId) }
  fun resolveReport(reportId: String, action: String) { repository.resolveReport(reportId, action) }
  fun broadcastAnnouncement(title: String, msg: String) { repository.broadcastAnnouncement(title, msg) }

  // Notifications
  fun openNotifications() { _showNotificationsSheet.value = true }
  fun closeNotifications() { _showNotificationsSheet.value = false }
  fun markNotificationAsRead(id: String) { repository.markNotificationAsRead(id) }
  fun clearAllNotifications() { repository.clearAllNotifications() }

  // Modals & Shares
  fun openInviteFriend() { _showInviteFriendDialog.value = true }
  fun closeInviteFriend() { _showInviteFriendDialog.value = false }
  fun openQrShare() { _showQrShareDialog.value = true }
  fun closeQrShare() { _showQrShareDialog.value = false }

  // Reports
  fun openReport(targetType: String, targetId: String, targetName: String) {
    _reportTarget.value = Pair(targetType, "$targetId: $targetName")
  }
  fun closeReport() { _reportTarget.value = null }
  fun submitReport(reason: String) {
    _reportTarget.value?.let { (type, info) ->
      repository.reportItem(type, info, info, reason)
    }
    _reportTarget.value = null
  }
}
