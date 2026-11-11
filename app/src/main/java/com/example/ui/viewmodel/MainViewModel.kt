package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.model.games.*
import com.example.data.repository.WatchTogetherRepository
import com.example.network.ActiveGameSessionManager
import com.example.network.WatchPartyNetworkEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class BottomNavTab {
  ROOM,
  GAMES,
  STORE,
  DM,
  PROFILE
}

class MainViewModel @JvmOverloads constructor(
  application: Application,
  private val repository: WatchTogetherRepository = WatchTogetherRepository(application)
) : AndroidViewModel(application) {

  // Watch Party Engine (Host file streaming & Voice Room)
  val networkEngine = WatchPartyNetworkEngine(application) { currentUser.value }
  val activeWatchParty = networkEngine.activeParty

  val connectionStatus = repository.connectionStatus

  init {
    viewModelScope.launch {
      networkEngine.eventToast.collect { msg ->
        repository.postEventMessage(msg)
      }
    }

    // Periodic heartbeat to active game session
    viewModelScope.launch {
      while (true) {
        delay(3000L)
        _activeGameSessionId.value?.let { matchId ->
          ActiveGameSessionManager.sendPlayerHeartbeat(matchId, currentUser.value.id)
        }
      }
    }

    // Periodic synchronization of active remote watch party sessions from backend
    viewModelScope.launch {
      while (true) {
        try {
          repository.backendApi?.fetchActiveWatchParties()?.getOrNull()?.forEach { session ->
            com.example.network.ActiveRoomSessionManager.createSession(session)
          }
        } catch (_: Exception) {}
        delay(4000L)
      }
    }
  }

  // Primary bottom navigation: EXACTLY 4 sections: 1. ROOM, 2. DM, 3. GAMES, 4. PROFILE
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
  val coinWalletState = repository.coinWalletState

  private val _showDailyTasksDialog = MutableStateFlow(false)
  val showDailyTasksDialog: StateFlow<Boolean> = _showDailyTasksDialog.asStateFlow()

  private val _showCoinHistoryDialog = MutableStateFlow(false)
  val showCoinHistoryDialog: StateFlow<Boolean> = _showCoinHistoryDialog.asStateFlow()

  fun openDailyTasks() { _showDailyTasksDialog.value = true }
  fun closeDailyTasks() { _showDailyTasksDialog.value = false }
  fun openCoinHistory() { _showCoinHistoryDialog.value = true }
  fun closeCoinHistory() { _showCoinHistoryDialog.value = false }

  fun claimDailyTask(taskId: String) { repository.claimDailyTask(taskId) }
  fun addTestCoins(amount: Int = 600) { repository.addTestCoins(amount) }
  fun refreshCoinWallet() { repository.refreshCoinWallet() }

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
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  // Strictly real-time Active Room Sessions from backend
  val activeRoomSessions: StateFlow<List<ActiveRoomSession>> = combine(
    com.example.network.ActiveRoomSessionManager.activeRooms,
    searchQuery
  ) { list, query ->
    if (query.isBlank()) list
    else list.filter {
      it.name.contains(query, ignoreCase = true) ||
          it.roomId.contains(query, ignoreCase = true) ||
          it.hostName.contains(query, ignoreCase = true)
    }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

  // Watch Party Dialog States
  private val _showHostWatchPartyDialog = MutableStateFlow(false)
  val showHostWatchPartyDialog: StateFlow<Boolean> = _showHostWatchPartyDialog.asStateFlow()

  private val _showJoinWatchPartyDialog = MutableStateFlow(false)
  val showJoinWatchPartyDialog: StateFlow<Boolean> = _showJoinWatchPartyDialog.asStateFlow()

  // -------------------------------------------------------------
  // REAL-TIME GAMES STATE & MATCHMAKING
  // -------------------------------------------------------------
  private val _activeGameSessionId = MutableStateFlow<String?>(null)
  val activeGameSessionId: StateFlow<String?> = _activeGameSessionId.asStateFlow()

  val activeGameSession: StateFlow<GameSession?> = combine(
    ActiveGameSessionManager.activeSessions,
    _activeGameSessionId
  ) { sessionsMap, id ->
    if (id != null) sessionsMap[id] else null
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val pendingGameInvites = ActiveGameSessionManager.pendingInvites

  private val _isMatchmaking = MutableStateFlow(false)
  val isMatchmaking: StateFlow<Boolean> = _isMatchmaking.asStateFlow()

  private val _matchmakingGameType = MutableStateFlow(GameType.CHESS)
  val matchmakingGameType: StateFlow<GameType> = _matchmakingGameType.asStateFlow()

  private val _showInviteFriendForGame = MutableStateFlow<GameType?>(null)
  val showInviteFriendForGame: StateFlow<GameType?> = _showInviteFriendForGame.asStateFlow()

  private val _showJoinGameCodeDialog = MutableStateFlow(false)
  val showJoinGameCodeDialog: StateFlow<Boolean> = _showJoinGameCodeDialog.asStateFlow()

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
    description: String = "",
    isPrivate: Boolean = false,
    password: String = "",
    welcome: String = "Welcome!",
    rules: String = "Be respectful",
    category: RoomCategory = RoomCategory.WATCH_TOGETHER
  ): Room? {
    val room = repository.createRoom(name, description, isPrivate, password, welcome, rules, category)
    if (room != null) {
      _showCreateRoomDialog.value = false
      // Join newly created room
      joinRoom(room.id)
    }
    return room
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

  // Watch Party Actions (Host & Friend Streaming Mode)
  fun openHostWatchParty() { _showHostWatchPartyDialog.value = true }
  fun closeHostWatchParty() { _showHostWatchPartyDialog.value = false }
  fun openJoinWatchParty() { _showJoinWatchPartyDialog.value = true }
  fun closeJoinWatchParty() { _showJoinWatchPartyDialog.value = false }

  fun startHostWatchParty(
    media: LocalMediaItem,
    quality: StreamQuality,
    roomName: String,
    password: String = ""
  ) {
    networkEngine.startHostParty(media, quality, roomName, password)
    _showHostWatchPartyDialog.value = false
  }

  fun joinWatchParty(code: String, password: String = ""): Boolean {
    val success = networkEngine.joinPartyByCode(code, password)
    if (success) {
      _showJoinWatchPartyDialog.value = false
    }
    return success
  }

  fun joinActiveRoomSession(session: ActiveRoomSession, passwordAttempt: String = ""): Boolean {
    val validation = com.example.network.ActiveRoomSessionManager.validateJoin(session.roomId, passwordAttempt)
    if (validation.isFailure) {
      val errorMsg = validation.exceptionOrNull()?.message ?: "Cannot join: Room is no longer active."
      repository.postEventMessage(errorMsg)
      return false
    }

    // Try joining as watch party first if registered or if it's a Watch Party room
    if (session.roomId.startsWith("WP-", ignoreCase = true) || com.example.network.WatchPartyNetworkEngine.registeredParties.containsKey(session.roomId)) {
      val success = networkEngine.joinPartyByCode(session.roomId, passwordAttempt)
      if (success) {
        _showJoinWatchPartyDialog.value = false
      }
      return success
    }

    // Otherwise join in repository
    return repository.joinRoom(session.roomId, passwordAttempt)
  }

  fun leaveWatchParty() {
    networkEngine.leaveParty()
  }

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

  fun updateRoomFullConfig(
    roomId: String,
    name: String,
    desc: String,
    announcement: String,
    mode: VideoControlMode,
    locked: Boolean,
    seatPerm: SeatPermission,
    chatPerm: ChatPermission,
    isVideoEnabled: Boolean,
    isGameEnabled: Boolean,
    activeGameType: String?
  ) {
    repository.updateRoomFullConfig(roomId, name, desc, announcement, mode, locked, seatPerm, chatPerm, isVideoEnabled, isGameEnabled, activeGameType)
    _showRoomSettingsDialog.value = false
  }

  fun updateRoomTheme(roomId: String, themeId: String, customConfig: RoomThemeConfig?) {
    repository.updateRoomTheme(roomId, themeId, customConfig)
  }

  fun updateRoomMediaStream(roomId: String, streamUrl: String, title: String, durationSec: Int, quality: String = "HIGH") {
    repository.updateRoomMediaStream(roomId, streamUrl, title, durationSec, quality)
  }

  fun updateVideoAudioSettings(roomId: String, videoVolume: Float, isMuted: Boolean, micVolume: Float) {
    repository.updateVideoAudioSettings(roomId, videoVolume, isMuted, micVolume)
  }

  fun toggleFollowRoom(roomId: String) {
    repository.toggleFollowRoom(roomId)
  }

  fun sendRoomGift(roomId: String, giftName: String, giftEmoji: String, coinCost: Int): Boolean {
    return repository.sendRoomGift(roomId, giftName, giftEmoji, coinCost)
  }

  fun sendMultiRecipientGift(roomId: String, recipientUserIds: List<String>, giftId: String): Boolean {
    return repository.sendMultiRecipientGift(roomId, recipientUserIds, giftId)
  }

  fun switchRoomType(roomId: String, newType: RoomType): Boolean {
    return repository.switchRoomType(roomId, newType)
  }

  fun openMysteryBox(): VirtualGift? {
    return repository.openMysteryBox()
  }

  fun purchaseAndProposeRing(receiverId: String, receiverName: String, ringId: String): Boolean {
    return repository.purchaseAndProposeRing(receiverId, receiverName, ringId)
  }

  fun respondToRingBond(bondId: String, accept: Boolean): Boolean {
    return repository.respondToRingBond(bondId, accept)
  }

  fun purchaseRelationshipTag(targetId: String, targetName: String, type: RelationshipTagType): Boolean {
    return repository.purchaseRelationshipTag(targetId, targetName, type)
  }

  fun respondToRelationshipTag(tagId: String, accept: Boolean): Boolean {
    return repository.respondToRelationshipTag(tagId, accept)
  }

  fun purchaseAvatar(avatarKey: String, avatarName: String, validity: AvatarValidityDays): Boolean {
    return repository.purchaseAvatar(avatarKey, avatarName, validity)
  }

  fun togglePartnerSeats(roomId: String, enabled: Boolean): Boolean {
    return repository.togglePartnerSeats(roomId, enabled)
  }

  fun setOwnerOnline(roomId: String, isOnline: Boolean) {
    repository.setOwnerOnline(roomId, isOnline)
  }

  fun setRoomGame(roomId: String, gameType: String?, isEnabled: Boolean) {
    repository.setRoomGame(roomId, gameType, isEnabled)
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

  // -------------------------------------------------------------
  // GAMES ACTIONS & LOGIC
  // -------------------------------------------------------------

  fun openPlayWithFriend(gameType: GameType) {
    _showInviteFriendForGame.value = gameType
  }

  fun closePlayWithFriend() {
    _showInviteFriendForGame.value = null
  }

  fun openJoinGameCode() {
    _showJoinGameCodeDialog.value = true
  }

  fun closeJoinGameCode() {
    _showJoinGameCodeDialog.value = false
  }

  fun startMatchmaking(gameType: GameType) {
    val user = currentUser.value
    _matchmakingGameType.value = gameType
    _isMatchmaking.value = true

    val instantSession = ActiveGameSessionManager.enterMatchmaking(
      userId = user.id,
      username = user.username,
      avatar = user.avatar,
      gameType = gameType
    )

    if (instantSession != null) {
      _isMatchmaking.value = false
      _activeGameSessionId.value = instantSession.matchId
      repository.postEventMessage("Opponent matched! Game started.")
    }
  }

  fun startBotGame(gameType: GameType) {
    val user = currentUser.value
    _isMatchmaking.value = false
    val session = ActiveGameSessionManager.startBotGame(
      userId = user.id,
      username = user.username,
      avatar = user.avatar,
      gameType = gameType
    )
    _activeGameSessionId.value = session.matchId
    repository.postEventMessage("Started ${gameType.displayName} match vs 🤖 BOT [AI]!")
  }

  fun purchaseCoinPackage(packageId: String): Boolean = repository.purchaseCoinPackage(packageId)
  fun equipRing(ringId: String?) = repository.equipRing(ringId)
  fun equipNameTag(tagLabel: String?) = repository.equipNameTag(tagLabel)
  fun equipProfileFrame(frameName: String) = repository.equipProfileFrame(frameName)
  fun equipCharmBadge(badge: String?) = repository.equipCharmBadge(badge)

  fun cancelMatchmaking() {
    ActiveGameSessionManager.cancelMatchmaking(currentUser.value.id)
    _isMatchmaking.value = false
  }

  fun createPrivateGame(gameType: GameType) {
    val user = currentUser.value
    closePlayWithFriend()
    val session = ActiveGameSessionManager.createPrivateGame(
      hostId = user.id,
      hostName = user.username,
      hostAvatar = user.avatar,
      gameType = gameType
    )
    _activeGameSessionId.value = session.matchId
    repository.postEventMessage("Private ${gameType.displayName} room created: ${session.privateCode}")
  }

  fun joinGameByCode(code: String) {
    val user = currentUser.value
    closeJoinGameCode()
    val session = ActiveGameSessionManager.joinGameByCode(
      code = code,
      userId = user.id,
      username = user.username,
      avatar = user.avatar
    )
    if (session != null) {
      _activeGameSessionId.value = session.matchId
      repository.postEventMessage("Joined ${session.gameType.displayName} game!")
    } else {
      repository.postEventMessage("Game code not found or already full.")
    }
  }

  fun challengeFriendToGame(friend: Friend, gameType: GameType) {
    val user = currentUser.value
    closePlayWithFriend()
    val (session, invite) = ActiveGameSessionManager.createFriendInvite(
      senderId = user.id,
      senderName = user.username,
      senderAvatar = user.avatar,
      recipientId = friend.userId,
      gameType = gameType
    )
    _activeGameSessionId.value = session.matchId

    // Also send an invitation DM into their chat thread!
    repository.sendDirectMessage(
      receiverId = friend.userId,
      text = "🎮 ${user.username} challenged you to a game of ${gameType.displayName}! Tap to accept.",
      isRoomInvite = false,
      invitedRoomId = null,
      invitedRoomName = null
    )
    repository.postEventMessage("Game challenge sent to ${friend.username}!")
  }

  fun acceptGameInvite(inviteId: String) {
    val user = currentUser.value
    val session = ActiveGameSessionManager.acceptFriendInvite(
      inviteId = inviteId,
      recipientId = user.id,
      recipientName = user.username,
      recipientAvatar = user.avatar
    )
    if (session != null) {
      _activeGameSessionId.value = session.matchId
      repository.postEventMessage("Joined ${session.gameType.displayName} match!")
    } else {
      repository.postEventMessage("Invite expired or game no longer available.")
    }
  }

  fun declineGameInvite(inviteId: String) {
    ActiveGameSessionManager.declineFriendInvite(inviteId)
    repository.postEventMessage("Game invitation declined.")
  }

  fun makeChessMove(from: ChessPosition, to: ChessPosition) {
    val matchId = _activeGameSessionId.value ?: return
    val success = ActiveGameSessionManager.makeChessMove(matchId, from, to)
    if (success) {
      com.example.network.security.ServerCoinManager.recordActivity(
        currentUser.value.id,
        CoinTaskType.PLAY_GAME,
        1
      )
      repository.refreshCoinWallet()
    } else {
      repository.postEventMessage("Illegal move.")
    }
  }

  fun resignChess() {
    val matchId = _activeGameSessionId.value ?: return
    ActiveGameSessionManager.resignChess(matchId, currentUser.value.id)
    repository.postEventMessage("You resigned the game.")
  }

  fun offerChessDraw() {
    val matchId = _activeGameSessionId.value ?: return
    ActiveGameSessionManager.offerChessDraw(matchId, currentUser.value.id)
    repository.postEventMessage("Draw offer sent to opponent.")
  }

  fun respondChessDraw(accepted: Boolean) {
    val matchId = _activeGameSessionId.value ?: return
    ActiveGameSessionManager.respondChessDraw(matchId, accepted)
    if (accepted) {
      repository.postEventMessage("Draw accepted! Game drawn.")
    } else {
      repository.postEventMessage("Draw offer declined.")
    }
  }

  fun rollLudoDice() {
    val matchId = _activeGameSessionId.value ?: return
    val roll = ActiveGameSessionManager.rollLudoDice(matchId, currentUser.value.id)
    if (roll > 0) {
      repository.postEventMessage("Rolled a $roll!")
    }
  }

  fun moveLudoToken(tokenId: Int) {
    val matchId = _activeGameSessionId.value ?: return
    val success = ActiveGameSessionManager.moveLudoToken(matchId, currentUser.value.id, tokenId)
    if (success) {
      com.example.network.security.ServerCoinManager.recordActivity(
        currentUser.value.id,
        CoinTaskType.PLAY_GAME,
        1
      )
      repository.refreshCoinWallet()
    }
  }

  fun requestGameRematch() {
    val matchId = _activeGameSessionId.value ?: return
    ActiveGameSessionManager.requestRematch(matchId, currentUser.value.id)
    repository.postEventMessage("Rematch requested!")
  }

  fun leaveActiveGame() {
    val matchId = _activeGameSessionId.value ?: return
    ActiveGameSessionManager.leaveGame(matchId, currentUser.value.id)
    _activeGameSessionId.value = null
    repository.postEventMessage("Left game.")
  }
}
