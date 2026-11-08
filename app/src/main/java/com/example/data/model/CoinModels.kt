package com.example.data.model

/**
 * Virtual Coin Task definitions.
 * Coins are virtual in-app rewards with no real-money value.
 * Daily limit: maximum 100 Coins per user per day.
 */
enum class CoinTaskType(
  val id: String,
  val title: String,
  val description: String,
  val rewardCoins: Int,
  val icon: String,
  val targetValue: Int,
  val unit: String
) {
  PLAY_GAME(
    id = "task_play_game",
    title = "Play Game",
    description = "Complete a genuine game session in Chess or Ludo",
    rewardCoins = 20,
    icon = "♟️",
    targetValue = 1,
    unit = "game"
  ),
  VOICE_ROOM_ACTIVITY(
    id = "task_voice_activity",
    title = "Voice Room Activity",
    description = "Spend meaningful time participating in a Voice Room",
    rewardCoins = 20,
    icon = "🎙️",
    targetValue = 60, // 60 seconds of verified voice stage presence
    unit = "s"
  ),
  WATCH_PARTY(
    id = "task_watch_party",
    title = "Watch Party",
    description = "Participate in a synchronized Watch Party stream",
    rewardCoins = 20,
    icon = "🎬",
    targetValue = 60, // 60 seconds of verified stream watch-time
    unit = "s"
  ),
  DAILY_APP_ACTIVITY(
    id = "task_daily_activity",
    title = "Daily App Activity",
    description = "Browse rooms and complete normal app activity",
    rewardCoins = 20,
    icon = "⚡",
    targetValue = 3, // Browse 3 rooms or stay active
    unit = "rooms"
  ),
  EXPLORE_SOCIAL(
    id = "task_explore_social",
    title = "Explore / Social Task",
    description = "Complete an eligible social activity (Chat, DM, Reaction)",
    rewardCoins = 20,
    icon = "💬",
    targetValue = 2, // 2 social interactions
    unit = "actions"
  )
}

/**
 * Daily task status for a user on the current server day.
 */
data class DailyTaskInfo(
  val taskType: CoinTaskType,
  val currentProgress: Int,
  val targetProgress: Int,
  val isCompleted: Boolean,
  val isClaimed: Boolean,
  val rewardCoins: Int = taskType.rewardCoins,
  val isDailyLimitReached: Boolean
)

/**
 * Server-authoritative Coin Transaction ledger entry.
 */
data class CoinTransaction(
  val id: String,
  val userId: String,
  val amount: Int, // Positive for earnings (+20), negative for deductions (-2000)
  val reason: String,
  val timestamp: Long,
  val resultingBalance: Int,
  val transactionType: String = if (amount >= 0) "EARNED" else "SPENT",
  val orderId: String? = null,
  val receiptId: String? = null
)

/**
 * Snapshot of a user's Coin Wallet.
 */
data class CoinWalletState(
  val balance: Int = 1450,
  val todayEarnings: Int = 0,
  val dailyLimit: Int = 100,
  val tasks: List<DailyTaskInfo> = emptyList(),
  val history: List<CoinTransaction> = emptyList(),
  val charmPoints: Int = 0,
  val lifetimeCharmPoints: Int = 0,
  val charmLevel: Int = 1
)

// -------------------------------------------------------------
// COIN STORE PACKAGES (Part 3)
// -------------------------------------------------------------

enum class CoinPackage(
  val id: String,
  val title: String,
  val priceInr: Int,
  val coins: Int,
  val tag: String = ""
) {
  MINI("pkg_mini", "Mini", 29, 250),
  STARTER("pkg_starter", "Starter", 59, 550),
  BASIC("pkg_basic", "Basic", 119, 1200),
  SILVER("pkg_silver", "Silver", 299, 3100, "POPULAR"),
  GOLD("pkg_gold", "Gold", 599, 6300, "VALUE"),
  DIAMOND("pkg_diamond", "Diamond", 1199, 13000),
  ROYAL("pkg_royal", "Royal", 2499, 28000, "ELITE"),
  MEGA("pkg_mega", "Mega", 4999, 60000, "BEST VALUE")
}

enum class PaymentStatus {
  PENDING,
  SUCCESS,
  FAILED,
  REFUNDED,
  CANCELLED
}

data class PaymentOrder(
  val orderId: String,
  val paymentId: String,
  val userId: String,
  val packageId: String,
  val priceInr: Int,
  val coins: Int,
  val status: PaymentStatus,
  val createdAtMs: Long,
  val processedAtMs: Long? = null,
  val isCoinCredited: Boolean = false,
  val receiptNumber: String = ""
)

// -------------------------------------------------------------
// VIRTUAL GIFTS & CHARM POINTS (Part 12)
// -------------------------------------------------------------

enum class GiftTier {
  BASIC,
  SPECIAL,
  PREMIUM,
  LEGENDARY
}

data class VirtualGift(
  val id: String,
  val name: String,
  val emoji: String,
  val priceCoins: Int,
  val charmPoints: Int = priceCoins / 5,
  val tier: GiftTier,
  val animationDurationMs: Long = if (tier == GiftTier.BASIC) 1800L else 3200L,
  val animationKey: String = ""
) {
  companion object {
    val ALL_GIFTS = listOf(
      // Basic Gifts
      VirtualGift("gift_rose", "Rose", "🌹", 5, 1, GiftTier.BASIC),
      VirtualGift("gift_heart", "Heart", "❤️", 10, 2, GiftTier.BASIC),
      VirtualGift("gift_star", "Star", "⭐", 15, 3, GiftTier.BASIC),
      VirtualGift("gift_coffee", "Coffee", "☕", 20, 4, GiftTier.BASIC),
      VirtualGift("gift_chocolate", "Chocolate", "🍫", 30, 6, GiftTier.BASIC),
      VirtualGift("gift_balloon", "Balloon", "🎈", 50, 10, GiftTier.BASIC),
      // Special Gifts
      VirtualGift("gift_teddy", "Teddy Bear", "🧸", 100, 20, GiftTier.SPECIAL),
      VirtualGift("gift_cake", "Birthday Cake", "🎂", 150, 30, GiftTier.SPECIAL),
      VirtualGift("gift_diamond", "Diamond", "💎", 200, 40, GiftTier.SPECIAL),
      VirtualGift("gift_crown", "Crown", "👑", 300, 60, GiftTier.SPECIAL),
      VirtualGift("gift_music_box", "Music Box", "🎶", 400, 80, GiftTier.SPECIAL),
      VirtualGift("gift_surprise_box", "Surprise Box", "🎁", 500, 100, GiftTier.SPECIAL)
    )

    fun findById(id: String): VirtualGift? = ALL_GIFTS.find { it.id == id }
  }
}

// -------------------------------------------------------------
// LUCKY MYSTERY REWARD SYSTEM (Part 13)
// -------------------------------------------------------------

data class MysteryBoxReward(
  val gift: VirtualGift,
  val probabilityPercent: Float,
  val isGuaranteed: Boolean = true
) {
  companion object {
    const val MYSTERY_BOX_PRICE = 100

    val REWARDS = listOf(
      MysteryBoxReward(VirtualGift("gift_rose", "Rose", "🌹", 5, 1, GiftTier.BASIC), 30.0f),
      MysteryBoxReward(VirtualGift("gift_heart", "Heart", "❤️", 10, 2, GiftTier.BASIC), 25.0f),
      MysteryBoxReward(VirtualGift("gift_chocolate", "Chocolate", "🍫", 30, 6, GiftTier.BASIC), 18.0f),
      MysteryBoxReward(VirtualGift("gift_teddy", "Teddy Bear", "🧸", 100, 20, GiftTier.SPECIAL), 12.0f),
      MysteryBoxReward(VirtualGift("gift_cake", "Birthday Cake", "🎂", 150, 30, GiftTier.SPECIAL), 7.0f),
      MysteryBoxReward(VirtualGift("gift_diamond", "Diamond", "💎", 200, 40, GiftTier.SPECIAL), 4.0f),
      MysteryBoxReward(VirtualGift("gift_crown", "Crown", "👑", 300, 60, GiftTier.SPECIAL), 2.5f),
      MysteryBoxReward(VirtualGift("gift_music_box", "Music Box", "🎶", 400, 80, GiftTier.SPECIAL), 1.0f),
      MysteryBoxReward(VirtualGift("gift_surprise_box", "Surprise Box", "🎁", 500, 100, GiftTier.SPECIAL), 0.5f)
    )
  }
}

// -------------------------------------------------------------
// CHARM LEVEL SYSTEM (Part 14)
// -------------------------------------------------------------

data class CharmLevelInfo(
  val level: Int,
  val minCharmPoints: Int,
  val tierBadge: String,
  val tierName: String
) {
  companion object {
    fun calculateLevel(points: Int): CharmLevelInfo {
      val lvl = (points / 50 + 1).coerceIn(1, 30)
      val (badge, tier) = when (lvl) {
        in 1..5 -> "🥉" to "Bronze"
        in 6..10 -> "🥈" to "Silver"
        in 11..15 -> "🥇" to "Gold"
        in 16..20 -> "💎" to "Platinum"
        in 21..25 -> "💠" to "Diamond"
        else -> "👑" to "Legendary"
      }
      return CharmLevelInfo(lvl, (lvl - 1) * 50, badge, tier)
    }
  }
}

// -------------------------------------------------------------
// RELATIONSHIP TAG SYSTEM (Part 15)
// -------------------------------------------------------------

enum class RelationshipTagType(val label: String, val emoji: String, val badgeColor: String) {
  BRO_BRO("BRO-BRO", "🤜🤛", "#3B82F6"),
  SIS_SIS("SIS-SIS", "👭", "#EC4899"),
  BRO_SIS("BRO-SIS", "👫", "#8B5CF6"),
  FRIEND_FRIEND("FRIEND-FRIEND", "🤝", "#10B981"),
  BESTIE_BESTIE("BESTIE-BESTIE", "✨", "#F59E0B")
}

data class UserRelationshipTag(
  val tagId: String,
  val type: RelationshipTagType,
  val requesterId: String,
  val requesterName: String,
  val targetId: String,
  val targetName: String,
  val status: BondStatus,
  val createdAtMs: Long,
  val acceptedAtMs: Long? = null
) {
  companion object {
    const val TAG_COST = 2000
  }
}

// -------------------------------------------------------------
// COUPLE RING SYSTEM (Part 16)
// -------------------------------------------------------------

data class VirtualRing(
  val id: String,
  val name: String,
  val emoji: String,
  val priceCoins: Int,
  val charmPoints: Int = priceCoins / 5,
  val animationDescription: String,
  val effectColor: String
) {
  companion object {
    val ALL_RINGS = listOf(
      VirtualRing("ring_silver_heart", "Silver Heart", "💍🤍", 5500, 1100, "Simple heart glow", "#C0C0C0"),
      VirtualRing("ring_rose_gold", "Rose Gold", "💍🌸", 11000, 2200, "Rose-gold shine", "#B76E79"),
      VirtualRing("ring_golden_heart", "Golden Heart", "💍💛", 22000, 4400, "Golden sparkle", "#FFD700"),
      VirtualRing("ring_ruby_diamond", "Ruby Diamond", "💍❤️", 44000, 8800, "Ruby and diamond sparkle", "#E0115F"),
      VirtualRing("ring_royal_diamond", "Royal Diamond", "💍👑", 88000, 17600, "Royal light and 3D ring effect", "#4169E1"),
      VirtualRing("ring_eternal_galaxy", "Eternal Galaxy", "💍🌌", 110000, 22000, "Galaxy particles and cinematic animation", "#9400D3")
    )

    fun findById(id: String): VirtualRing? = ALL_RINGS.find { it.id == id }
  }
}

enum class BondStatus {
  PENDING,
  ACCEPTED,
  REJECTED,
  TERMINATED
}

data class RingBond(
  val bondId: String,
  val ringId: String,
  val ringName: String,
  val senderUserId: String,
  val senderUsername: String,
  val receiverUserId: String,
  val receiverUsername: String,
  val status: BondStatus,
  val createdAtMs: Long,
  val acceptedAtMs: Long? = null,
  val charmPointsGranted: Int
)

// -------------------------------------------------------------
// ROOM TYPE SWITCHING (Part 9)
// -------------------------------------------------------------

enum class RoomType(val displayName: String, val description: String, val seatCount: Int) {
  NORMAL("Normal Room", "Standard voice chat & social room", 8),
  VIDEO("Video Room", "Synchronized Watch Party streaming", 8),
  COUPLE("Couple Room", "Private intimate room for 2 members", 2),
  GAMING("Gaming Room", "Chess & Ludo multiplayer room with spectators", 8)
}

// -------------------------------------------------------------
// AVATAR VALIDITY SYSTEM (Part 5)
// -------------------------------------------------------------

enum class AvatarValidityDays(val days: Int, val coinCost: Int) {
  DAYS_7(7, 350),
  DAYS_15(15, 650),
  DAYS_30(30, 1100)
}

data class UserAvatarItem(
  val id: String,
  val avatarKey: String,
  val name: String,
  val purchasedAtMs: Long,
  val validityDays: Int,
  val expiresAtMs: Long,
  val isActive: Boolean = false
) {
  val isExpired: Boolean get() = System.currentTimeMillis() > expiresAtMs
  val remainingDays: Int get() = ((expiresAtMs - System.currentTimeMillis()).coerceAtLeast(0L) / 86400000L).toInt()
}
