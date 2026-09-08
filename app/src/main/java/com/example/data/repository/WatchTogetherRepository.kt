package com.example.data.repository

import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class WatchTogetherRepository {

  private val scope = CoroutineScope(Dispatchers.Default)

  // -------------------------------------------------------------
  // CURRENT USER STATE (Guest -> Mobile OTP Verified Account)
  // -------------------------------------------------------------
  private val initialUserId = "USR-" + (10000..99999).random()
  private val _currentUser = MutableStateFlow(
    User(
      id = initialUserId,
      username = "Guest_${initialUserId.takeLast(4)}",
      isGuest = true,
      guestCreatedAt = System.currentTimeMillis() - 86400000L * 8, // Seeded 8 days ago so friendly verification prompt is eligible!
      avatar = "avatar_1",
      bio = "Movie enthusiast & night owl. Exploring watch rooms!",
      personalXp = 1450,
      personalLevel = 2,
      profileFrame = "Neon Purple",
      statusEmoji = "🎧",
      statusText = "Listening",
      isOnline = true
    )
  )
  val currentUser: StateFlow<User> = _currentUser.asStateFlow()

  // -------------------------------------------------------------
  // ROOMS REPOSITORY
  // -------------------------------------------------------------
  private val _rooms = MutableStateFlow<List<Room>>(emptyList())
  val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

  // -------------------------------------------------------------
  // ACTIVE ROOM SESSION
  // -------------------------------------------------------------
  private val _currentActiveRoom = MutableStateFlow<Room?>(null)
  val currentActiveRoom: StateFlow<Room?> = _currentActiveRoom.asStateFlow()

  private val _roomSeats = MutableStateFlow<List<RoomSeat>>(List(8) { index -> RoomSeat(seatIndex = index) })
  val roomSeats: StateFlow<List<RoomSeat>> = _roomSeats.asStateFlow()

  private val _audienceMembers = MutableStateFlow<List<AudienceMember>>(emptyList())
  val audienceMembers: StateFlow<List<AudienceMember>> = _audienceMembers.asStateFlow()

  private val _roomAdmins = MutableStateFlow<Map<String, List<RoomAdmin>>>(emptyMap())
  val roomAdmins: StateFlow<Map<String, List<RoomAdmin>>> = _roomAdmins.asStateFlow()

  private val _roomChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val roomChatMessages: StateFlow<List<ChatMessage>> = _roomChatMessages.asStateFlow()

  // -------------------------------------------------------------
  // DIRECT MESSAGING & FRIENDS
  // -------------------------------------------------------------
  private val _friends = MutableStateFlow<List<Friend>>(emptyList())
  val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

  private val _directMessages = MutableStateFlow<Map<String, List<DirectMessage>>>(emptyMap())
  val directMessages: StateFlow<Map<String, List<DirectMessage>>> = _directMessages.asStateFlow()

  // -------------------------------------------------------------
  // NOTIFICATIONS
  // -------------------------------------------------------------
  private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
  val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

  // -------------------------------------------------------------
  // GLOBAL ADMIN DATA
  // -------------------------------------------------------------
  private val _reports = MutableStateFlow<List<GlobalReport>>(emptyList())
  val reports: StateFlow<List<GlobalReport>> = _reports.asStateFlow()

  private val _announcements = MutableStateFlow<List<SystemAnnouncement>>(emptyList())
  val announcements: StateFlow<List<SystemAnnouncement>> = _announcements.asStateFlow()

  private val _bannedUserIds = MutableStateFlow<Set<String>>(emptySet())
  val bannedUserIds: StateFlow<Set<String>> = _bannedUserIds.asStateFlow()

  // -------------------------------------------------------------
  // SETTINGS
  // -------------------------------------------------------------
  private val _appSettings = MutableStateFlow(AppSettings())
  val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

  // Toast / Snackbar Events
  private val _eventMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
  val eventMessages: SharedFlow<String> = _eventMessages.asSharedFlow()

  // Anti-farming trackers
  private var lastChatMessageTime = 0L
  private var lastVoiceXpAwardTime = 0L

  init {
    seedInitialData()
    startSynchronizedClockLoop()
  }

  private fun seedInitialData() {
    val seedRooms = listOf(
      Room(
        id = "ABC7291",
        name = "Movie Night 🍿",
        description = "Streaming sci-fi & anime blockbusters with high-def surround sound. Grab your popcorn!",
        roomImage = "gradient_1",
        ownerId = "USR-10101",
        ownerName = "Alex Rivera",
        isPrivate = false,
        roomXp = 7850,
        roomLevel = 2,
        videoControlMode = VideoControlMode.ADMINS_ONLY,
        category = RoomCategory.WATCH_TOGETHER,
        audienceCount = 14,
        isFavourite = true,
        activeVideoTitle = "Cyberpunk: Edgerunners Ep. 1",
        activeVideoDurationSec = 1420,
        activeVideoPosSec = 345f,
        isVideoPlaying = true
      ),
      Room(
        id = "LOFI882",
        name = "EDM Chill & Lo-Fi Lounge 🎧",
        description = "24/7 synchronized synthwave, lo-fi beats, and study chill room. Chill voice chat on.",
        roomImage = "gradient_2",
        ownerId = "USR-20202",
        ownerName = "DJ Spark",
        isPrivate = false,
        roomXp = 18400,
        roomLevel = 3,
        videoControlMode = VideoControlMode.EVERYONE,
        category = RoomCategory.VOICE,
        audienceCount = 28,
        isFavourite = false,
        activeVideoTitle = "Lofi Hip Hop Radio - Beats to Relax/Study to",
        activeVideoDurationSec = 3600,
        activeVideoPosSec = 820f,
        isVideoPlaying = true
      ),
      Room(
        id = "ANM5510",
        name = "Anime Watch Party ⚔️",
        description = "Private squad for new season weekly drops. Password protected room.",
        roomImage = "gradient_3",
        ownerId = "USR-30303",
        ownerName = "SakuraKun",
        isPrivate = true,
        password = "anime",
        roomXp = 2100,
        roomLevel = 1,
        videoControlMode = VideoControlMode.OWNER_ONLY,
        category = RoomCategory.POPULAR,
        audienceCount = 6,
        isFavourite = true,
        activeVideoTitle = "Demon Slayer Season 4 Special",
        activeVideoDurationSec = 1800,
        activeVideoPosSec = 120f,
        isVideoPlaying = false
      ),
      Room(
        id = "DEV4040",
        name = "Coding & Tech Docs Stream 💻",
        description = "Collaborative learning, Android Jetpack Compose tutorials, and tech documentaries.",
        roomImage = "gradient_4",
        ownerId = "USR-40404",
        ownerName = "CodeMaster",
        isPrivate = false,
        roomXp = 6200,
        roomLevel = 2,
        videoControlMode = VideoControlMode.ADMINS_ONLY,
        category = RoomCategory.NEW,
        audienceCount = 9,
        activeVideoTitle = "Kotlin Coroutines & Flow Deep Dive",
        activeVideoDurationSec = 2400,
        activeVideoPosSec = 450f,
        isVideoPlaying = true
      ),
      Room(
        id = "CLS9901",
        name = "Classic Cinema Club 🎬",
        description = "Restored 4K masterpieces, noir classics, and director commentaries.",
        roomImage = "gradient_5",
        ownerId = "USR-50505",
        ownerName = "CinemaBuff",
        isPrivate = false,
        roomXp = 32000,
        roomLevel = 4,
        videoControlMode = VideoControlMode.ADMINS_ONLY,
        category = RoomCategory.ACTIVE,
        audienceCount = 35,
        activeVideoTitle = "Metropolis (1927) Restored Edition",
        activeVideoDurationSec = 9000,
        activeVideoPosSec = 1200f,
        isVideoPlaying = true
      )
    )
    _rooms.value = seedRooms

    // Seed Admins for ABC7291
    _roomAdmins.value = mapOf(
      "ABC7291" to listOf(
        RoomAdmin(
          userId = "USR-10102",
          username = "Elena_V",
          permissions = AdminPermission()
        ),
        RoomAdmin(
          userId = "USR-10103",
          username = "MarcusK",
          permissions = AdminPermission(canLockRoom = false, canBanMember = false)
        )
      )
    )

    // Seed Friends
    val seedFriends = listOf(
      Friend(
        userId = "USR-10102",
        username = "Elena_V",
        avatar = "avatar_2",
        bio = "Sci-fi nerd, animator & synth lover 🚀",
        isOnline = true,
        statusEmoji = "🎬",
        statusText = "Watching Movie Night",
        inRoomId = "ABC7291",
        inRoomName = "Movie Night 🍿",
        friendshipStatus = FriendshipStatus.ACCEPTED
      ),
      Friend(
        userId = "USR-10103",
        username = "MarcusK",
        avatar = "avatar_3",
        bio = "Film student & cinephile 📽️",
        isOnline = true,
        statusEmoji = "🎧",
        statusText = "Listening",
        inRoomId = "ABC7291",
        inRoomName = "Movie Night 🍿",
        friendshipStatus = FriendshipStatus.ACCEPTED
      ),
      Friend(
        userId = "USR-20202",
        username = "DJ Spark",
        avatar = "avatar_4",
        bio = "Making beats & streaming 24/7 🎵",
        isOnline = true,
        statusEmoji = "🎮",
        statusText = "Gaming",
        inRoomId = "LOFI882",
        inRoomName = "EDM Chill & Lo-Fi Lounge 🎧",
        friendshipStatus = FriendshipStatus.ACCEPTED
      ),
      Friend(
        userId = "USR-77777",
        username = "Aria_Star",
        avatar = "avatar_5",
        bio = "Looking for cool watch rooms!",
        isOnline = false,
        statusEmoji = "💤",
        statusText = "Away",
        friendshipStatus = FriendshipStatus.ACCEPTED
      ),
      Friend(
        userId = "USR-88888",
        username = "Vikram_99",
        avatar = "avatar_6",
        bio = "Bollywood & Cricket watch parties 🏏",
        isOnline = true,
        statusEmoji = "🟢",
        statusText = "Available",
        friendshipStatus = FriendshipStatus.PENDING_INCOMING
      )
    )
    _friends.value = seedFriends

    // Seed Direct Messages
    val seedDms = mapOf(
      "USR-10102" to listOf(
        DirectMessage(
          id = UUID.randomUUID().toString(),
          senderId = "USR-10102",
          receiverId = _currentUser.value.id,
          text = "Hey! We are watching the new Edgerunners episode in Movie Night!",
          timestamp = System.currentTimeMillis() - 1000 * 60 * 15
        ),
        DirectMessage(
          id = UUID.randomUUID().toString(),
          senderId = "USR-10102",
          receiverId = _currentUser.value.id,
          text = "Join us right here:",
          timestamp = System.currentTimeMillis() - 1000 * 60 * 14,
          isRoomInvite = true,
          invitedRoomId = "ABC7291",
          invitedRoomName = "Movie Night 🍿"
        )
      ),
      "USR-20202" to listOf(
        DirectMessage(
          id = UUID.randomUUID().toString(),
          senderId = "USR-20202",
          receiverId = _currentUser.value.id,
          text = "New lo-fi playlist just dropped! Jump in when you have time 🎶",
          timestamp = System.currentTimeMillis() - 1000 * 60 * 120
        )
      )
    )
    _directMessages.value = seedDms

    // Seed Notifications
    _notifications.value = listOf(
      NotificationItem(
        id = UUID.randomUUID().toString(),
        userId = _currentUser.value.id,
        type = NotificationType.ROOM_INVITE,
        title = "Room Invitation",
        message = "Elena_V invited you to join 'Movie Night 🍿' (ABC7291)",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 14,
        actionRoomId = "ABC7291"
      ),
      NotificationItem(
        id = UUID.randomUUID().toString(),
        userId = _currentUser.value.id,
        type = NotificationType.FRIEND_REQUEST,
        title = "New Friend Request",
        message = "Vikram_99 sent you a friend request",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 60,
        actionUserId = "USR-88888"
      ),
      NotificationItem(
        id = UUID.randomUUID().toString(),
        userId = _currentUser.value.id,
        type = NotificationType.ROOM_LEVEL_UP,
        title = "Room Leveled Up!",
        message = "Room 'Movie Night 🍿' reached Level 2! 4 Admin slots now unlocked.",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 5,
        actionRoomId = "ABC7291"
      ),
      NotificationItem(
        id = UUID.randomUUID().toString(),
        userId = _currentUser.value.id,
        type = NotificationType.ACHIEVEMENT_UNLOCKED,
        title = "Achievement Unlocked 🏅",
        message = "Unlocked badge 'First Room' (+250 XP earned)!",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24
      )
    )

    // Seed Global Reports
    _reports.value = listOf(
      GlobalReport(
        id = "REP-101",
        reporterId = "USR-10103",
        reporterName = "MarcusK",
        targetType = "USER",
        targetId = "USR-99999",
        targetName = "SpamBot_42",
        reason = "Repeated join-leave spam and unsolicited advertising links in voice chat",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 90,
        status = "PENDING"
      )
    )

    _announcements.value = listOf(
      SystemAnnouncement(
        id = "ANN-1",
        title = "🎉 Watch Together v1.0 Launch!",
        message = "Welcome to Watch Together! Permanent 8-seat voice rooms, audience mode, and synchronized video playback are now fully operational.",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48
      )
    )
  }

  // -------------------------------------------------------------
  // PERIODIC SYNCHRONIZATION & XP ACCRUAL
  // -------------------------------------------------------------
  private fun startSynchronizedClockLoop() {
    scope.launch {
      while (true) {
        delay(1000)
        // Advance current active video time if playing
        _currentActiveRoom.value?.let { room ->
          if (room.isVideoPlaying) {
            val newPos = if (room.activeVideoPosSec + 1f >= room.activeVideoDurationSec) {
              0f // Loop
            } else {
              room.activeVideoPosSec + 1f
            }
            _currentActiveRoom.value = room.copy(activeVideoPosSec = newPos)
          }
        }

        // Voice chat simulation: toggle speaking simulation for lively feel
        if ((1..5).random() == 1) {
          _roomSeats.update { seats ->
            seats.map { seat ->
              if (seat.userId != null && !seat.isMuted) {
                seat.copy(isSpeaking = (1..3).random() == 1)
              } else {
                seat.copy(isSpeaking = false)
              }
            }
          }
        }
      }
    }
  }

  // -------------------------------------------------------------
  // AUTH / GUEST LINKING & OTP SYSTEM
  // -------------------------------------------------------------
  fun verifyMobileAndLinkAccount(phoneNumber: String, otpCode: String): Boolean {
    // Validates 6 digit OTP (accepts 123456 or any 6-digit input)
    if (otpCode.length == 6 && phoneNumber.length >= 10) {
      val user = _currentUser.value
      // Preserve: User ID, Username, Profile, Friends, Rooms, History, Achievements, XP, Settings!
      val updatedUser = user.copy(
        phoneNumber = phoneNumber,
        isGuest = false,
        isVerified = true,
        permanentLoginUntil = System.currentTimeMillis() + (15L * 86400000L) // 15-day session
      )
      _currentUser.value = updatedUser
      addPersonalXp(500, "Verified Mobile Number")
      sendNotification(
        NotificationType.ACHIEVEMENT_UNLOCKED,
        "Account Verified! 🛡️",
        "Your account is now permanent. All data, XP, and rooms are secured."
      )
      _eventMessages.tryEmit("Mobile number verified! Account linked permanently.")
      return true
    }
    return false
  }

  fun logout() {
    val user = _currentUser.value
    _currentUser.value = user.copy(
      permanentLoginUntil = 0L,
      isVerified = false
    )
    _eventMessages.tryEmit("Logged out successfully. Please re-verify via Mobile OTP.")
  }

  fun updateProfile(
    newUsername: String,
    newBio: String,
    newAvatar: String,
    newProfileFrame: String,
    newStatusEmoji: String,
    newStatusText: String
  ) {
    _currentUser.update {
      it.copy(
        username = newUsername.ifBlank { it.username },
        bio = newBio,
        avatar = newAvatar,
        profileFrame = newProfileFrame,
        statusEmoji = newStatusEmoji,
        statusText = newStatusText
      )
    }
    _eventMessages.tryEmit("Profile updated successfully!")
  }

  fun addPersonalXp(amount: Int, reason: String = "") {
    _currentUser.update { user ->
      val newXp = user.personalXp + amount
      val nextLevelThreshold = user.personalLevel * 1000
      val newLevel = if (newXp >= nextLevelThreshold) user.personalLevel + 1 else user.personalLevel
      if (newLevel > user.personalLevel) {
        sendNotification(
          NotificationType.ACHIEVEMENT_UNLOCKED,
          "Personal Level Up! ⭐",
          "You reached Level $newLevel! +$amount XP from $reason"
        )
      }
      user.copy(personalXp = newXp, personalLevel = newLevel)
    }
  }

  // -------------------------------------------------------------
  // ROOM JOINING, 8-SEATS & AUDIENCE MODE
  // -------------------------------------------------------------
  fun joinRoom(roomId: String, passwordAttempt: String = ""): Boolean {
    val targetRoom = _rooms.value.find { it.id.equals(roomId.trim(), ignoreCase = true) }
    if (targetRoom == null) {
      _eventMessages.tryEmit("Room not found with ID $roomId")
      return false
    }

    if (targetRoom.isPrivate && targetRoom.password.isNotBlank() && targetRoom.password != passwordAttempt) {
      _eventMessages.tryEmit("Incorrect room password")
      return false
    }

    if (targetRoom.isLocked && targetRoom.ownerId != _currentUser.value.id) {
      _eventMessages.tryEmit("This room is currently locked by the Owner/Admin")
      return false
    }

    // Set as active room
    _currentActiveRoom.value = targetRoom
    _currentUser.update { it.copy(currentRoomId = targetRoom.id) }

    // Populate initial seats
    val admins = _roomAdmins.value[targetRoom.id] ?: emptyList()
    val isUserOwner = targetRoom.ownerId == _currentUser.value.id
    val isUserAdmin = admins.any { it.userId == _currentUser.value.id }

    // Seed 8 voice seats: Seat 0 has Owner or user
    val initialSeats = mutableListOf<RoomSeat>()

    if (isUserOwner) {
      initialSeats.add(
        RoomSeat(
          seatIndex = 0,
          userId = _currentUser.value.id,
          username = _currentUser.value.username,
          avatar = _currentUser.value.avatar,
          isOwner = true,
          isAdmin = false
        )
      )
      // Fill some other seats
      initialSeats.add(
        RoomSeat(
          seatIndex = 1,
          userId = "USR-10102",
          username = "Elena_V",
          avatar = "avatar_2",
          isAdmin = true
        )
      )
      initialSeats.add(
        RoomSeat(
          seatIndex = 2,
          userId = "USR-10103",
          username = "MarcusK",
          avatar = "avatar_3",
          isAdmin = true
        )
      )
      for (i in 3..7) {
        initialSeats.add(RoomSeat(seatIndex = i))
      }
    } else {
      // Owner is in Seat 0
      initialSeats.add(
        RoomSeat(
          seatIndex = 0,
          userId = targetRoom.ownerId,
          username = targetRoom.ownerName,
          avatar = "avatar_1",
          isOwner = true,
          isAdmin = false
        )
      )
      // Pre-fill seats 1-7. If room is ABC7291, let's prefill all 8 seats to demonstrate Audience Mode!
      if (targetRoom.id == "ABC7291") {
        for (i in 1..7) {
          initialSeats.add(
            RoomSeat(
              seatIndex = i,
              userId = "USR-BOT-$i",
              username = listOf("Kai_88", "Elena_V", "MarcusK", "Zoe_Moon", "Aarav_P", "NeoX", "Zara_K")[i - 1],
              avatar = "avatar_${(i % 5) + 1}",
              isMuted = i % 2 == 0,
              isSpeaking = i == 1
            )
          )
        }
      } else {
        // Seat user in seat 1 if available
        initialSeats.add(
          RoomSeat(
            seatIndex = 1,
            userId = _currentUser.value.id,
            username = _currentUser.value.username,
            avatar = _currentUser.value.avatar,
            isAdmin = isUserAdmin
          )
        )
        for (i in 2..7) {
          initialSeats.add(RoomSeat(seatIndex = i))
        }
      }
    }
    _roomSeats.value = initialSeats

    // Check if current user is in a seat or audience
    val userInSeat = initialSeats.any { it.userId == _currentUser.value.id }
    if (!userInSeat) {
      // User is in audience
      _audienceMembers.update { list ->
        if (list.none { it.userId == _currentUser.value.id }) {
          list + AudienceMember(
            userId = _currentUser.value.id,
            username = _currentUser.value.username,
            avatar = _currentUser.value.avatar
          )
        } else list
      }
      _eventMessages.tryEmit("All 8 voice seats are full. You entered as Audience/Spectator!")
    } else {
      _eventMessages.tryEmit("Joined room: ${targetRoom.name}")
    }

    // Seed chat
    _roomChatMessages.value = listOf(
      ChatMessage(
        id = UUID.randomUUID().toString(),
        roomId = targetRoom.id,
        senderId = "SYSTEM",
        senderName = "System",
        senderRole = "SYSTEM",
        text = "Welcome to ${targetRoom.name}! ${targetRoom.welcomeMessage}",
        isSystem = true
      ),
      ChatMessage(
        id = UUID.randomUUID().toString(),
        roomId = targetRoom.id,
        senderId = targetRoom.ownerId,
        senderName = targetRoom.ownerName,
        senderRole = "OWNER",
        text = "Hey everyone! Sound check: can you hear the synchronized audio clearly?",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 3
      ),
      ChatMessage(
        id = UUID.randomUUID().toString(),
        roomId = targetRoom.id,
        senderId = "USR-10102",
        senderName = "Elena_V",
        senderRole = "ADMIN",
        text = "Audio is crystal clear! Sync delay is only 14ms.",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 2
      )
    )

    // Award Room XP for joining
    awardRoomXp(targetRoom.id, 50, "New member joined")
    return true
  }

  fun leaveRoom() {
    _currentActiveRoom.value?.let { room ->
      // Remove from seat
      _roomSeats.update { seats ->
        seats.map {
          if (it.userId == _currentUser.value.id) it.copy(userId = null, username = null, avatar = null, isSpeaking = false)
          else it
        }
      }
      // Remove from audience
      _audienceMembers.update { list -> list.filterNot { it.userId == _currentUser.value.id } }
    }
    _currentActiveRoom.value = null
    _currentUser.update { it.copy(currentRoomId = null) }
    _eventMessages.tryEmit("Left room")
  }

  // -------------------------------------------------------------
  // SEAT & AUDIENCE INTERACTIONS
  // -------------------------------------------------------------
  fun takeSeat(seatIndex: Int): Boolean {
    val seats = _roomSeats.value
    if (seatIndex !in 0..7) return false
    val targetSeat = seats[seatIndex]
    if (targetSeat.userId != null) {
      _eventMessages.tryEmit("Seat ${seatIndex + 1} is already occupied")
      return false
    }

    // Leave any existing seat
    val updatedSeats = seats.mapIndexed { i, seat ->
      if (seat.userId == _currentUser.value.id) {
        seat.copy(userId = null, username = null, avatar = null, isSpeaking = false)
      } else if (i == seatIndex) {
        val isOwner = _currentActiveRoom.value?.ownerId == _currentUser.value.id
        val admins = _roomAdmins.value[_currentActiveRoom.value?.id] ?: emptyList()
        val isAdmin = admins.any { it.userId == _currentUser.value.id }
        seat.copy(
          userId = _currentUser.value.id,
          username = _currentUser.value.username,
          avatar = _currentUser.value.avatar,
          isOwner = isOwner,
          isAdmin = isAdmin,
          isMuted = false
        )
      } else {
        seat
      }
    }
    _roomSeats.value = updatedSeats
    // Remove from audience
    _audienceMembers.update { it.filterNot { m -> m.userId == _currentUser.value.id } }
    _eventMessages.tryEmit("You took Voice Seat ${seatIndex + 1} 🎙️")
    return true
  }

  fun leaveSeat(): Boolean {
    var removed = false
    _roomSeats.update { seats ->
      seats.map {
        if (it.userId == _currentUser.value.id) {
          removed = true
          it.copy(userId = null, username = null, avatar = null, isSpeaking = false, isMuted = false)
        } else it
      }
    }
    if (removed) {
      // Add to audience
      _audienceMembers.update { list ->
        if (list.none { it.userId == _currentUser.value.id }) {
          list + AudienceMember(
            userId = _currentUser.value.id,
            username = _currentUser.value.username,
            avatar = _currentUser.value.avatar
          )
        } else list
      }
      _eventMessages.tryEmit("Moved to Audience/Spectator mode")
      // Broadcast system message
      postSystemMessage("Seat is now available")
    }
    return removed
  }

  fun requestSeat() {
    _audienceMembers.update { list ->
      list.map {
        if (it.userId == _currentUser.value.id) it.copy(hasRequestedSeat = true, requestedAt = System.currentTimeMillis())
        else it
      }
    }
    _eventMessages.tryEmit("Seat request sent to Room Owner & Admins ✋")
  }

  fun approveSeatRequest(userId: String) {
    val requester = _audienceMembers.value.find { it.userId == userId } ?: return
    val seats = _roomSeats.value
    val firstEmptySeat = seats.indexOfFirst { it.userId == null }
    if (firstEmptySeat == -1) {
      _eventMessages.tryEmit("Cannot approve: All 8 seats are full")
      return
    }

    _roomSeats.update { currentSeats ->
      currentSeats.mapIndexed { i, seat ->
        if (i == firstEmptySeat) {
          seat.copy(
            userId = requester.userId,
            username = requester.username,
            avatar = requester.avatar,
            isMuted = false
          )
        } else seat
      }
    }
    _audienceMembers.update { it.filterNot { m -> m.userId == userId } }
    postSystemMessage("${requester.username} took Voice Seat ${firstEmptySeat + 1}")
    _eventMessages.tryEmit("Approved ${requester.username} to Seat ${firstEmptySeat + 1}")
  }

  fun toggleMicMute() {
    _roomSeats.update { seats ->
      seats.map {
        if (it.userId == _currentUser.value.id) {
          val newMute = !it.isMuted
          _eventMessages.tryEmit(if (newMute) "Microphone muted 🔇" else "Microphone unmuted 🎙️")
          it.copy(isMuted = newMute, isSpeaking = if (newMute) false else it.isSpeaking)
        } else it
      }
    }
  }

  fun toggleRaiseHand() {
    _roomSeats.update { seats ->
      seats.map {
        if (it.userId == _currentUser.value.id) {
          val newHand = !it.raisedHand
          _eventMessages.tryEmit(if (newHand) "Hand raised ✋" else "Hand lowered")
          it.copy(raisedHand = newHand)
        } else it
      }
    }
  }

  // -------------------------------------------------------------
  // SYNCHRONIZED WATCH TOGETHER VIDEO CONTROLS
  // -------------------------------------------------------------
  fun canControlVideo(): Boolean {
    val room = _currentActiveRoom.value ?: return false
    val userId = _currentUser.value.id
    if (room.ownerId == userId) return true
    val admins = _roomAdmins.value[room.id] ?: emptyList()
    val isAdmin = admins.any { it.userId == userId }

    return when (room.videoControlMode) {
      VideoControlMode.EVERYONE -> true
      VideoControlMode.ADMINS_ONLY -> isAdmin
      VideoControlMode.OWNER_ONLY -> false
    }
  }

  fun togglePlayPause(): Boolean {
    if (!canControlVideo()) {
      _eventMessages.tryEmit("Video controls restricted to ${getCurrentControlModeLabel()}")
      return false
    }
    _currentActiveRoom.update { room ->
      room?.let {
        val newPlaying = !it.isVideoPlaying
        _eventMessages.tryEmit(if (newPlaying) "Synchronized: Playing ▶️" else "Synchronized: Paused ⏸️")
        it.copy(isVideoPlaying = newPlaying)
      }
    }
    awardRoomXp(_currentActiveRoom.value?.id ?: "", 20, "Watch together activity")
    return true
  }

  fun seekVideo(newPosSec: Float): Boolean {
    if (!canControlVideo()) {
      _eventMessages.tryEmit("Video controls restricted to ${getCurrentControlModeLabel()}")
      return false
    }
    _currentActiveRoom.update { room ->
      room?.copy(activeVideoPosSec = newPosSec.coerceIn(0f, room.activeVideoDurationSec.toFloat()))
    }
    _eventMessages.tryEmit("Synchronized to ${formatSeconds(newPosSec.toInt())}")
    return true
  }

  fun forward10Sec() {
    _currentActiveRoom.value?.let { room ->
      seekVideo(room.activeVideoPosSec + 10f)
    }
  }

  fun rewind10Sec() {
    _currentActiveRoom.value?.let { room ->
      seekVideo(room.activeVideoPosSec - 10f)
    }
  }

  fun changeVideo(title: String, durationSec: Int = 1200) {
    if (!canControlVideo()) {
      _eventMessages.tryEmit("Video controls restricted to ${getCurrentControlModeLabel()}")
      return
    }
    _currentActiveRoom.update {
      it?.copy(
        activeVideoTitle = title,
        activeVideoDurationSec = durationSec,
        activeVideoPosSec = 0f,
        isVideoPlaying = true
      )
    }
    postSystemMessage("Now playing: $title")
    _eventMessages.tryEmit("Now synchronized playing: $title")
  }

  private fun getCurrentControlModeLabel(): String {
    return when (_currentActiveRoom.value?.videoControlMode) {
      VideoControlMode.EVERYONE -> "Everyone"
      VideoControlMode.ADMINS_ONLY -> "Admins & Owner"
      VideoControlMode.OWNER_ONLY -> "Room Owner Only"
      null -> "Authorized Users"
    }
  }

  // -------------------------------------------------------------
  // ROOM CHAT & MODERATION
  // -------------------------------------------------------------
  fun sendChatMessage(text: String): Boolean {
    val room = _currentActiveRoom.value ?: return false
    val now = System.currentTimeMillis()

    // Anti-farming check (500ms cooldown)
    if (now - lastChatMessageTime < 500) {
      return false
    }
    lastChatMessageTime = now

    val user = _currentUser.value
    val isOwner = room.ownerId == user.id
    val admins = _roomAdmins.value[room.id] ?: emptyList()
    val isAdmin = admins.any { it.userId == user.id }
    val role = if (isOwner) "OWNER" else if (isAdmin) "ADMIN" else "MEMBER"

    val newMessage = ChatMessage(
      id = UUID.randomUUID().toString(),
      roomId = room.id,
      senderId = user.id,
      senderName = user.username,
      senderAvatar = user.avatar,
      senderRole = role,
      text = text.trim(),
      timestamp = now
    )

    _roomChatMessages.update { it + newMessage }

    // Award Room XP & Personal XP with rate limit
    awardRoomXp(room.id, 5, "Chat activity")
    addPersonalXp(3, "Room participation")
    return true
  }

  fun addMessageReaction(messageId: String, emoji: String) {
    _roomChatMessages.update { list ->
      list.map { msg ->
        if (msg.id == messageId) {
          val currCount = msg.reactions[emoji] ?: 0
          val updatedMap = msg.reactions.toMutableMap()
          updatedMap[emoji] = currCount + 1
          msg.copy(reactions = updatedMap)
        } else msg
      }
    }
  }

  fun deleteChatMessage(messageId: String) {
    val user = _currentUser.value
    val room = _currentActiveRoom.value ?: return
    val admins = _roomAdmins.value[room.id] ?: emptyList()
    val isOwner = room.ownerId == user.id
    val isAdmin = admins.any { it.userId == user.id }

    _roomChatMessages.update { list ->
      list.filterNot { msg ->
        msg.id == messageId && (msg.senderId == user.id || isOwner || isAdmin)
      }
    }
    _eventMessages.tryEmit("Message deleted")
  }

  private fun postSystemMessage(text: String) {
    _currentActiveRoom.value?.let { room ->
      val sysMsg = ChatMessage(
        id = UUID.randomUUID().toString(),
        roomId = room.id,
        senderId = "SYSTEM",
        senderName = "System",
        senderRole = "SYSTEM",
        text = text,
        isSystem = true
      )
      _roomChatMessages.update { it + sysMsg }
    }
  }

  // -------------------------------------------------------------
  // ROOM CREATION & PERMANENT OWNER RULES
  // -------------------------------------------------------------
  fun createRoom(
    name: String,
    description: String,
    isPrivate: Boolean,
    password: String,
    welcomeMessage: String,
    rules: String,
    category: RoomCategory = RoomCategory.WATCH_TOGETHER
  ): Room {
    val newId = "RM" + (1000..9999).random()
    val user = _currentUser.value
    val newRoom = Room(
      id = newId,
      name = name.trim(),
      description = description.trim(),
      ownerId = user.id, // Permanent Owner
      ownerName = user.username,
      isPrivate = isPrivate,
      password = password,
      welcomeMessage = welcomeMessage.ifBlank { "Welcome to $name! Enjoy synchronized watch parties." },
      rules = rules.ifBlank { "1. Be respectful\n2. Have fun watching" },
      roomXp = 0,
      roomLevel = 1,
      videoControlMode = VideoControlMode.ADMINS_ONLY,
      category = category,
      audienceCount = 1
    )

    _rooms.update { listOf(newRoom) + it }
    addPersonalXp(300, "Created Permanent Room")
    _eventMessages.tryEmit("Permanent Room '$name' (ID: $newId) created successfully! 👑")
    return newRoom
  }

  fun deleteRoom(roomId: String): Boolean {
    val room = _rooms.value.find { it.id == roomId } ?: return false
    // Only permanent owner can delete
    if (room.ownerId != _currentUser.value.id) {
      _eventMessages.tryEmit("Only the permanent Owner can delete this room.")
      return false
    }
    _rooms.update { it.filterNot { r -> r.id == roomId } }
    if (_currentActiveRoom.value?.id == roomId) {
      leaveRoom()
    }
    _eventMessages.tryEmit("Room deleted successfully.")
    return true
  }

  fun updateRoomSettings(
    roomId: String,
    name: String,
    description: String,
    welcomeMessage: String,
    rules: String,
    videoControlMode: VideoControlMode,
    isLocked: Boolean
  ) {
    _rooms.update { list ->
      list.map { r ->
        if (r.id == roomId) {
          r.copy(
            name = name,
            description = description,
            welcomeMessage = welcomeMessage,
            rules = rules,
            videoControlMode = videoControlMode,
            isLocked = isLocked
          )
        } else r
      }
    }
    if (_currentActiveRoom.value?.id == roomId) {
      _currentActiveRoom.update {
        it?.copy(
          name = name,
          description = description,
          welcomeMessage = welcomeMessage,
          rules = rules,
          videoControlMode = videoControlMode,
          isLocked = isLocked
        )
      }
    }
    _eventMessages.tryEmit("Room settings updated.")
  }

  // -------------------------------------------------------------
  // ADMIN SYSTEM & PERMISSIONS
  // -------------------------------------------------------------
  fun addRoomAdmin(roomId: String, userId: String, username: String): Boolean {
    val room = _rooms.value.find { it.id == roomId } ?: return false
    if (room.ownerId != _currentUser.value.id) {
      _eventMessages.tryEmit("Only the permanent Owner can appoint Admins.")
      return false
    }

    // Check level-based admin limits
    // Level 1 -> max 3, Level 2 -> max 4, Level 3 -> max 5
    val currentAdmins = _roomAdmins.value[roomId] ?: emptyList()
    val maxAdmins = when (room.roomLevel) {
      1 -> 3
      2 -> 4
      else -> 5
    }

    if (currentAdmins.size >= maxAdmins) {
      _eventMessages.tryEmit("Admin limit reached (Max $maxAdmins for Level ${room.roomLevel}). Level up room for more slots!")
      return false
    }

    if (currentAdmins.any { it.userId == userId }) {
      _eventMessages.tryEmit("$username is already an Admin.")
      return false
    }

    val updatedAdmins = currentAdmins + RoomAdmin(userId = userId, username = username)
    _roomAdmins.update { it + (roomId to updatedAdmins) }

    postSystemMessage("$username became Admin 🛡️")
    sendNotification(
      NotificationType.ADMIN_PROMOTION,
      "Promoted to Room Admin 🛡️",
      "You were promoted to Admin in room '${room.name}'!",
      actionRoomId = roomId
    )
    _eventMessages.tryEmit("$username is now an Admin of this room.")
    return true
  }

  fun removeRoomAdmin(roomId: String, userId: String): Boolean {
    val room = _rooms.value.find { it.id == roomId } ?: return false
    if (room.ownerId != _currentUser.value.id) {
      _eventMessages.tryEmit("Only the permanent Owner can remove Admins.")
      return false
    }
    val currentAdmins = _roomAdmins.value[roomId] ?: emptyList()
    val updated = currentAdmins.filterNot { it.userId == userId }
    _roomAdmins.update { it + (roomId to updated) }
    _eventMessages.tryEmit("Admin removed.")
    return true
  }

  // -------------------------------------------------------------
  // ROOM XP & LEVEL UP (Anti-Farming)
  // -------------------------------------------------------------
  fun awardRoomXp(roomId: String, xpDelta: Int, reason: String) {
    _rooms.update { list ->
      list.map { room ->
        if (room.id == roomId) {
          val newXp = room.roomXp + xpDelta
          val threshold = getXpThresholdForRoomLevel(room.roomLevel)
          var newLevel = room.roomLevel
          if (newXp >= threshold) {
            newLevel += 1
            sendNotification(
              NotificationType.ROOM_LEVEL_UP,
              "Room Leveled Up! 🚀",
              "Room '${room.name}' reached Level $newLevel! +1 Admin slot unlocked.",
              actionRoomId = roomId
            )
            postSystemMessage("🎉 Room reached Level $newLevel!")
            _eventMessages.tryEmit("🎉 Room reached Level $newLevel!")
          }
          val currentActive = _currentActiveRoom.value
          val updated = if (currentActive?.id == roomId) {
            currentActive.copy(roomXp = newXp, roomLevel = newLevel)
          } else {
            room.copy(roomXp = newXp, roomLevel = newLevel)
          }
          if (_currentActiveRoom.value?.id == roomId) {
            _currentActiveRoom.value = updated
          }
          updated
        } else room
      }
    }
  }

  fun getXpThresholdForRoomLevel(level: Int): Int {
    return when (level) {
      1 -> 5000
      2 -> 10000
      3 -> 20000
      4 -> 35000
      else -> level * 10000
    }
  }

  // -------------------------------------------------------------
  // FAVOURITE ROOMS
  // -------------------------------------------------------------
  fun toggleFavouriteRoom(roomId: String) {
    _rooms.update { list ->
      list.map {
        if (it.id == roomId) {
          val newFav = !it.isFavourite
          _eventMessages.tryEmit(if (newFav) "Added to Favourites ❤️" else "Removed from Favourites")
          it.copy(isFavourite = newFav)
        } else it
      }
    }
  }

  // -------------------------------------------------------------
  // FRIEND SYSTEM
  // -------------------------------------------------------------
  fun sendFriendRequest(userId: String, username: String) {
    val existing = _friends.value.find { it.userId == userId }
    if (existing != null) {
      _eventMessages.tryEmit("Already connected or requested")
      return
    }
    val newFriend = Friend(
      userId = userId,
      username = username,
      avatar = "avatar_1",
      friendshipStatus = FriendshipStatus.PENDING_OUTGOING
    )
    _friends.update { it + newFriend }
    _eventMessages.tryEmit("Friend request sent to $username")
  }

  fun acceptFriendRequest(userId: String) {
    _friends.update { list ->
      list.map {
        if (it.userId == userId) it.copy(friendshipStatus = FriendshipStatus.ACCEPTED)
        else it
      }
    }
    sendNotification(
      NotificationType.FRIEND_ACCEPTED,
      "Friend Request Accepted",
      "You and your friend are now connected!"
    )
    _eventMessages.tryEmit("Friend request accepted! 🤝")
  }

  fun rejectFriendRequest(userId: String) {
    _friends.update { it.filterNot { f -> f.userId == userId } }
    _eventMessages.tryEmit("Friend request declined.")
  }

  fun removeFriend(userId: String) {
    _friends.update { it.filterNot { f -> f.userId == userId } }
    _eventMessages.tryEmit("Friend removed.")
  }

  fun blockUser(userId: String, username: String) {
    _friends.update { list ->
      list.map { if (it.userId == userId) it.copy(isBlocked = true) else it }
    }
    _eventMessages.tryEmit("$username has been blocked.")
  }

  // -------------------------------------------------------------
  // DIRECT MESSAGING
  // -------------------------------------------------------------
  fun sendDirectMessage(receiverId: String, text: String, isRoomInvite: Boolean = false, invitedRoomId: String? = null, invitedRoomName: String? = null) {
    val user = _currentUser.value
    val msg = DirectMessage(
      id = UUID.randomUUID().toString(),
      senderId = user.id,
      receiverId = receiverId,
      text = text.trim(),
      timestamp = System.currentTimeMillis(),
      isRoomInvite = isRoomInvite,
      invitedRoomId = invitedRoomId,
      invitedRoomName = invitedRoomName
    )
    val currentThread = _directMessages.value[receiverId] ?: emptyList()
    _directMessages.update { it + (receiverId to (currentThread + msg)) }
  }

  fun deleteDirectMessage(friendId: String, messageId: String) {
    val currentThread = _directMessages.value[friendId] ?: return
    val updated = currentThread.filterNot { it.id == messageId && it.senderId == _currentUser.value.id }
    _directMessages.update { it + (friendId to updated) }
    _eventMessages.tryEmit("Message deleted")
  }

  // -------------------------------------------------------------
  // NOTIFICATIONS
  // -------------------------------------------------------------
  fun sendNotification(type: NotificationType, title: String, message: String, actionRoomId: String? = null, actionUserId: String? = null) {
    val item = NotificationItem(
      id = UUID.randomUUID().toString(),
      userId = _currentUser.value.id,
      type = type,
      title = title,
      message = message,
      timestamp = System.currentTimeMillis(),
      actionRoomId = actionRoomId,
      actionUserId = actionUserId
    )
    _notifications.update { listOf(item) + it }
  }

  fun markNotificationAsRead(id: String) {
    _notifications.update { list ->
      list.map { if (it.id == id) it.copy(isRead = true) else it }
    }
  }

  fun clearAllNotifications() {
    _notifications.value = emptyList()
  }

  // -------------------------------------------------------------
  // SAFETY & MODERATION (Reports, Bans)
  // -------------------------------------------------------------
  fun reportItem(targetType: String, targetId: String, targetName: String, reason: String) {
    val report = GlobalReport(
      id = "REP-" + (1000..9999).random(),
      reporterId = _currentUser.value.id,
      reporterName = _currentUser.value.username,
      targetType = targetType,
      targetId = targetId,
      targetName = targetName,
      reason = reason
    )
    _reports.update { listOf(report) + it }
    _eventMessages.tryEmit("Report submitted. Our moderation team will review this shortly.")
  }

  fun resolveReport(reportId: String, actionTaken: String) {
    _reports.update { list ->
      list.map { if (it.id == reportId) it.copy(status = "RESOLVED: $actionTaken") else it }
    }
    _eventMessages.tryEmit("Report marked as resolved: $actionTaken")
  }

  fun banUserGlobally(userId: String) {
    _bannedUserIds.update { it + userId }
    _eventMessages.tryEmit("User $userId has been banned globally.")
  }

  fun broadcastAnnouncement(title: String, message: String) {
    val ann = SystemAnnouncement(
      id = UUID.randomUUID().toString(),
      title = title,
      message = message
    )
    _announcements.update { listOf(ann) + it }
    sendNotification(NotificationType.ROOM_ANNOUNCEMENT, title, message)
    _eventMessages.tryEmit("Global Announcement Broadcasted!")
  }

  fun updateSettings(settings: AppSettings) {
    _appSettings.value = settings
    _eventMessages.tryEmit("Settings saved.")
  }

  private fun formatSeconds(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d:%02d", m, s)
  }
}
