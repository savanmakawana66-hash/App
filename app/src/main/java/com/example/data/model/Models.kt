package com.example.data.model

enum class VideoControlMode {
  EVERYONE,
  ADMINS_ONLY,
  OWNER_ONLY
}

enum class RoomCategory {
  ALL,
  ACTIVE,
  POPULAR,
  NEW,
  VOICE,
  WATCH_TOGETHER,
  MY_ROOMS,
  FAVOURITES,
  RECENT
}

enum class FriendshipStatus {
  ACCEPTED,
  PENDING,
  PENDING_INCOMING,
  PENDING_OUTGOING,
  NONE
}

enum class NotificationType {
  FRIEND_REQUEST,
  FRIEND_ACCEPTED,
  ROOM_INVITE,
  JOIN_REQUEST,
  JOIN_ACCEPTED,
  ADMIN_PROMOTION,
  ADMIN_PERMISSION_CHANGED,
  ROOM_LEVEL_UP,
  ACHIEVEMENT_UNLOCKED,
  FRIEND_ONLINE,
  ROOM_ANNOUNCEMENT,
  SEAT_AVAILABLE
}

data class User(
  val id: String,
  val username: String,
  val phoneNumber: String? = null,
  val isGuest: Boolean = true,
  val guestCreatedAt: Long = System.currentTimeMillis(),
  val permanentLoginUntil: Long = 0L,
  val isVerified: Boolean = false,
  val avatar: String = "avatar_1",
  val coverUrl: String = "cover_gradient_1",
  val bio: String = "Hey there! I love watching movies & chilling with friends.",
  val personalXp: Int = 1250,
  val personalLevel: Int = 2,
  val badges: List<String> = listOf("🏅 First Room", "🎙️ Voice Participant", "🎬 Watch Together"),
  val profileFrame: String = "Neon Purple",
  val statusEmoji: String = "🟢",
  val statusText: String = "Available",
  val currentRoomId: String? = null,
  val isOnline: Boolean = true,
  val watchHours: Float = 14.5f,
  val voiceHours: Float = 8.2f,
  val isBlocked: Boolean = false,
  val isSuspended: Boolean = false
) {
  val xp: Int get() = personalXp
  val level: Int get() = personalLevel
  val frameName: String get() = profileFrame
}

data class Room(
  val id: String, // e.g. "ABC7291"
  val name: String,
  val description: String,
  val roomImage: String = "gradient_1",
  val ownerId: String,
  val ownerName: String,
  val isPrivate: Boolean = false,
  val password: String = "",
  val welcomeMessage: String = "Welcome to the room! Be kind and enjoy the synchronized stream.",
  val rules: String = "1. Respect everyone\n2. No spamming\n3. Keep mic muted when not talking",
  val roomXp: Int = 7850,
  val roomLevel: Int = 2,
  val videoControlMode: VideoControlMode = VideoControlMode.ADMINS_ONLY,
  val isLocked: Boolean = false,
  val createdAt: Long = System.currentTimeMillis() - 86400000L * 3,
  val category: RoomCategory = RoomCategory.WATCH_TOGETHER,
  val audienceCount: Int = 14,
  val isFavourite: Boolean = false,
  val activeVideoTitle: String = "Cyberpunk 2077 - Edgerunners Ep. 1",
  val activeVideoDurationSec: Int = 600,
  val activeVideoPosSec: Float = 145f,
  val isVideoPlaying: Boolean = true
)

data class RoomSeat(
  val seatIndex: Int,
  val userId: String? = null,
  val username: String? = null,
  val avatar: String? = null,
  val isMuted: Boolean = false,
  val isSpeaking: Boolean = false,
  val isOwner: Boolean = false,
  val isAdmin: Boolean = false,
  val raisedHand: Boolean = false
)

data class AudienceMember(
  val userId: String,
  val username: String,
  val avatar: String,
  val hasRequestedSeat: Boolean = false,
  val requestedAt: Long = 0L
)

data class AdminPermission(
  val canRemoveMember: Boolean = true,
  val canMuteMember: Boolean = true,
  val canBanMember: Boolean = true,
  val canManageSeats: Boolean = true,
  val canLockRoom: Boolean = true,
  val canManageChat: Boolean = true,
  val canManageVideo: Boolean = true,
  val canManageSettings: Boolean = true
)

data class RoomAdmin(
  val userId: String,
  val username: String,
  val permissions: AdminPermission = AdminPermission()
)

data class ChatMessage(
  val id: String,
  val roomId: String,
  val senderId: String,
  val senderName: String,
  val senderAvatar: String = "avatar_1",
  val senderRole: String = "MEMBER", // OWNER, ADMIN, MEMBER, SYSTEM
  val text: String,
  val timestamp: Long = System.currentTimeMillis(),
  val reactions: Map<String, Int> = emptyMap(),
  val isSystem: Boolean = false
)

data class DirectMessage(
  val id: String,
  val senderId: String,
  val receiverId: String,
  val text: String,
  val timestamp: Long = System.currentTimeMillis(),
  val isRoomInvite: Boolean = false,
  val invitedRoomId: String? = null,
  val invitedRoomName: String? = null,
  val reactions: Map<String, Int> = emptyMap()
) {
  val roomId: String? get() = invitedRoomId
  val roomName: String? get() = invitedRoomName
}

data class Friend(
  val userId: String,
  val username: String,
  val avatar: String,
  val bio: String = "",
  val isOnline: Boolean = true,
  val statusEmoji: String = "🟢",
  val statusText: String = "Available",
  val inRoomId: String? = null,
  val inRoomName: String? = null,
  val friendshipStatus: FriendshipStatus = FriendshipStatus.ACCEPTED,
  val isBlocked: Boolean = false
)

typealias AppNotification = NotificationItem

data class NotificationItem(
  val id: String,
  val userId: String,
  val type: NotificationType,
  val title: String,
  val message: String,
  val timestamp: Long = System.currentTimeMillis(),
  val isRead: Boolean = false,
  val actionRoomId: String? = null,
  val actionUserId: String? = null
)

data class WatchVideoItem(
  val id: String,
  val title: String,
  val durationSec: Int,
  val category: String,
  val description: String,
  val url: String = ""
)

data class AppSettings(
  val isDarkMode: Boolean = true,
  val micSensitivity: Float = 0.8f,
  val voiceQuality: String = "High Definition (48kHz)",
  val videoQuality: String = "1080p Full HD",
  val pushNotifications: Boolean = true,
  val friendNotifications: Boolean = true,
  val roomNotifications: Boolean = true,
  val messageNotifications: Boolean = true,
  val dmPermissions: String = "Everyone",
  val profileVisibility: String = "Public",
  val language: String = "English",
  val noiseSuppression: Boolean = true,
  val echoCancellation: Boolean = true,
  val autoSyncVideo: Boolean = true,
  val showOnlineStatus: Boolean = true,
  val allowDirectMessages: Boolean = true,
  val allowRoomInvites: Boolean = true,
  val notificationsEnabled: Boolean = true
)

typealias ReportItem = GlobalReport

data class GlobalReport(
  val id: String,
  val reporterId: String,
  val reporterName: String,
  val targetType: String, // "USER", "ROOM", "MESSAGE"
  val targetId: String,
  val targetName: String,
  val reason: String,
  val timestamp: Long = System.currentTimeMillis(),
  val status: String = "PENDING" // "PENDING", "RESOLVED", "DISMISSED"
) {
  val reportedTargetName: String get() = targetName
  val isResolved: Boolean get() = status != "PENDING"
}

data class SystemAnnouncement(
  val id: String,
  val title: String,
  val message: String,
  val timestamp: Long = System.currentTimeMillis(),
  val priority: String = "NORMAL"
)
