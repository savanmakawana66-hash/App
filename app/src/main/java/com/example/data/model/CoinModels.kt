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
  val transactionType: String = if (amount >= 0) "EARNED" else "SPENT"
)

/**
 * Snapshot of a user's Coin Wallet.
 */
data class CoinWalletState(
  val balance: Int = 1450,
  val todayEarnings: Int = 0,
  val dailyLimit: Int = 100,
  val tasks: List<DailyTaskInfo> = emptyList(),
  val history: List<CoinTransaction> = emptyList()
)
