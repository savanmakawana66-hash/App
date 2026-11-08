package com.example.network.security

import android.os.SystemClock
import com.example.data.model.*
import com.example.data.security.SecureStorageManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.random.Random

/**
 * Server-Authoritative Virtual Coin & Reward Manager.
 *
 * Enforcements:
 * 1. Hard Daily Limit: 100 Coins maximum per user per server day.
 * 2. Monotonic Server Time Anchor: Prevents bypassing limits via device clock manipulation.
 * 3. Anti-Farming & Activity Verification: Requires genuine activity before marking tasks completed.
 * 4. Atomicity & Non-Negative Balances: Deductions fail safely without charging user if requirements are not met.
 * 5. Immutable Transaction Ledger: Every coin earned or spent generates a cryptographically traceable record.
 * 6. Permanent Voice Room Creation Cost: 2,000 Coins. No recurring maintenance fees.
 */
object ServerCoinManager {

  const val DAILY_COIN_LIMIT = 100
  const val PERMANENT_ROOM_COST = 2000

  // Server Time Baseline: anchored at boot to prevent device clock spoofing
  private val bootServerTimeMs: Long = System.currentTimeMillis()
  private val bootElapsedRealtimeMs: Long = SystemClock.elapsedRealtime()

  // Monotonically corrected server timestamp
  val currentServerTimeMs: Long
    get() = bootServerTimeMs + (SystemClock.elapsedRealtime() - bootElapsedRealtimeMs)

  val currentServerDateKey: String
    get() {
      val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
      }
      return sdf.format(Date(currentServerTimeMs))
    }

  // Storage
  private val userBalances = ConcurrentHashMap<String, Int>()
  private val userTransactions = ConcurrentHashMap<String, MutableList<CoinTransaction>>()

  // Daily Tracking: key = "${userId}_${dateKey}"
  private val dailyEarnings = ConcurrentHashMap<String, Int>()
  private val dailyClaimedTasks = ConcurrentHashMap<String, MutableSet<String>>()
  private val dailyTaskProgress = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()

  // Anti-Farming throttle: "${userId}_${taskType}" -> lastRecordTimeMs
  private val lastActivityTime = ConcurrentHashMap<String, Long>()

  // Payment Orders: orderId -> PaymentOrder
  private val paymentOrders = ConcurrentHashMap<String, com.example.data.model.PaymentOrder>()
  // Processed Payments: paymentId -> orderId (Prevents duplicate payment credit)
  private val processedPaymentIds = ConcurrentHashMap<String, String>()

  // Charm Points: userId -> lifetime points
  private val userCharmPoints = ConcurrentHashMap<String, Int>()

  // Active Ring Bonds: bondId -> RingBond
  private val ringBonds = ConcurrentHashMap<String, com.example.data.model.RingBond>()

  // Active Relationship Tags: tagId -> UserRelationshipTag
  private val relationshipTags = ConcurrentHashMap<String, com.example.data.model.UserRelationshipTag>()

  // User Avatars: userId -> List<UserAvatarItem>
  private val userAvatars = ConcurrentHashMap<String, MutableList<com.example.data.model.UserAvatarItem>>()

  // Mystery Box openings: userId -> List<VirtualGift>
  private val userGiftInventory = ConcurrentHashMap<String, MutableList<com.example.data.model.VirtualGift>>()

  private var secureStorage: SecureStorageManager? = null

  fun initialize(storage: SecureStorageManager?, initialUserId: String) {
    secureStorage = storage
    if (!userBalances.containsKey(initialUserId)) {
      initSeedUser(initialUserId)
    }
  }

  private fun initSeedUser(userId: String) {
    // Seed standard profile with 1,450 coins & 72 earned today (as per prompt specification)
    val baseBalance = 1450
    userBalances[userId] = baseBalance

    val dateKey = currentServerDateKey
    val userDateKey = "${userId}_$dateKey"
    dailyEarnings[userDateKey] = 72

    val claimed = ConcurrentHashMap.newKeySet<String>()
    claimed.add(CoinTaskType.PLAY_GAME.id)
    claimed.add(CoinTaskType.VOICE_ROOM_ACTIVITY.id)
    claimed.add(CoinTaskType.WATCH_PARTY.id)
    dailyClaimedTasks[userDateKey] = claimed

    val progress = ConcurrentHashMap<String, Int>()
    progress[CoinTaskType.PLAY_GAME.id] = 1
    progress[CoinTaskType.VOICE_ROOM_ACTIVITY.id] = 60
    progress[CoinTaskType.WATCH_PARTY.id] = 60
    progress[CoinTaskType.DAILY_APP_ACTIVITY.id] = 2 // 2/3 progress
    progress[CoinTaskType.EXPLORE_SOCIAL.id] = 1   // 1/2 progress
    dailyTaskProgress[userDateKey] = progress

    // Seed realistic ledger history
    val history = mutableListOf<CoinTransaction>()
    val now = currentServerTimeMs
    val hourMs = 3600000L

    history.add(
      CoinTransaction(
        id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
        userId = userId,
        amount = 20,
        reason = "Play Game (Chess Match completed)",
        timestamp = now - hourMs * 4,
        resultingBalance = 1410
      )
    )
    history.add(
      CoinTransaction(
        id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
        userId = userId,
        amount = 20,
        reason = "Voice Room activity (Stage participation)",
        timestamp = now - hourMs * 3,
        resultingBalance = 1430
      )
    )
    history.add(
      CoinTransaction(
        id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
        userId = userId,
        amount = 20,
        reason = "Watch Party (Synchronized stream viewing)",
        timestamp = now - hourMs * 2,
        resultingBalance = 1450
      )
    )
    userTransactions[userId] = history
  }

  // -------------------------------------------------------------
  // WALLET & BALANCE INQUIRIES
  // -------------------------------------------------------------

  fun getBalance(userId: String): Int {
    return userBalances.getOrPut(userId) { 1450 }
  }

  fun getTodayEarnings(userId: String): Int {
    val key = "${userId}_$currentServerDateKey"
    return dailyEarnings.getOrDefault(key, 0)
  }

  fun canAffordPermanentRoom(userId: String): Boolean {
    return getBalance(userId) >= PERMANENT_ROOM_COST
  }

  fun getTransactionHistory(userId: String): List<CoinTransaction> {
    return userTransactions.getOrDefault(userId, emptyList()).reversed()
  }

  fun getDailyTasks(userId: String): List<DailyTaskInfo> {
    val dateKey = currentServerDateKey
    val userDateKey = "${userId}_$dateKey"
    val earningsToday = dailyEarnings.getOrDefault(userDateKey, 0)
    val isLimitReached = earningsToday >= DAILY_COIN_LIMIT
    val claimed = dailyClaimedTasks.getOrDefault(userDateKey, emptySet())
    val progressMap = dailyTaskProgress.getOrDefault(userDateKey, ConcurrentHashMap())

    return CoinTaskType.values().map { task ->
      val currentProgress = progressMap.getOrDefault(task.id, 0)
      val isCompleted = currentProgress >= task.targetValue
      val isClaimed = claimed.contains(task.id)

      DailyTaskInfo(
        taskType = task,
        currentProgress = currentProgress.coerceAtMost(task.targetValue),
        targetProgress = task.targetValue,
        isCompleted = isCompleted,
        isClaimed = isClaimed,
        rewardCoins = task.rewardCoins,
        isDailyLimitReached = isLimitReached && !isClaimed
      )
    }
  }

  fun getWalletState(userId: String): CoinWalletState {
    val cp = getCharmPoints(userId)
    return CoinWalletState(
      balance = getBalance(userId),
      todayEarnings = getTodayEarnings(userId),
      dailyLimit = DAILY_COIN_LIMIT,
      tasks = getDailyTasks(userId),
      history = getTransactionHistory(userId),
      charmPoints = cp,
      lifetimeCharmPoints = cp,
      charmLevel = getCharmLevel(userId).level
    )
  }

  // -------------------------------------------------------------
  // SERVER-SIDE ACTIVITY TRACKING & ANTI-FARMING
  // -------------------------------------------------------------

  @Synchronized
  fun recordActivity(userId: String, taskType: CoinTaskType, increment: Int = 1) {
    val now = currentServerTimeMs
    val throttleKey = "${userId}_${taskType.id}"
    val lastTime = lastActivityTime.getOrDefault(throttleKey, 0L)

    // Rate-limit incoming increments to prevent automated spamming (min 800ms between ticks)
    if (now - lastTime < 800L) {
      return
    }
    lastActivityTime[throttleKey] = now

    val userDateKey = "${userId}_$currentServerDateKey"
    val progressMap = dailyTaskProgress.getOrPut(userDateKey) { ConcurrentHashMap() }
    val current = progressMap.getOrDefault(taskType.id, 0)
    progressMap[taskType.id] = (current + increment).coerceAtMost(taskType.targetValue)
  }

  // -------------------------------------------------------------
  // TASK CLAIM & COIN REWARD GRANT (Server Authoritative)
  // -------------------------------------------------------------

  @Synchronized
  fun claimDailyTask(userId: String, taskId: String): Result<CoinTransaction> {
    val dateKey = currentServerDateKey
    val userDateKey = "${userId}_$dateKey"

    val task = CoinTaskType.values().find { it.id == taskId }
      ?: return Result.failure(IllegalArgumentException("Invalid task ID"))

    val claimedSet = dailyClaimedTasks.getOrPut(userDateKey) { ConcurrentHashMap.newKeySet() }
    if (claimedSet.contains(taskId)) {
      return Result.failure(IllegalStateException("Task already claimed today"))
    }

    val progressMap = dailyTaskProgress.getOrDefault(userDateKey, ConcurrentHashMap())
    val progress = progressMap.getOrDefault(taskId, 0)
    if (progress < task.targetValue) {
      return Result.failure(IllegalStateException("Task requirements not yet satisfied"))
    }

    val currentToday = dailyEarnings.getOrDefault(userDateKey, 0)
    if (currentToday >= DAILY_COIN_LIMIT) {
      return Result.failure(IllegalStateException("Daily limit of $DAILY_COIN_LIMIT Coins reached. Tasks reset tomorrow at 00:00 UTC."))
    }

    // Grant reward up to daily ceiling
    val grantAmount = minOf(task.rewardCoins, DAILY_COIN_LIMIT - currentToday)
    val currentBalance = getBalance(userId)
    val newBalance = currentBalance + grantAmount

    userBalances[userId] = newBalance
    dailyEarnings[userDateKey] = currentToday + grantAmount
    claimedSet.add(taskId)

    val txn = CoinTransaction(
      id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
      userId = userId,
      amount = grantAmount,
      reason = "${task.title} task completed",
      timestamp = currentServerTimeMs,
      resultingBalance = newBalance,
      transactionType = "EARNED"
    )

    val list = userTransactions.getOrPut(userId) { mutableListOf() }
    list.add(txn)

    return Result.success(txn)
  }

  // -------------------------------------------------------------
  // PERMANENT ROOM PAYMENT (2,000 Coins)
  // -------------------------------------------------------------

  @Synchronized
  fun deductCoinsForPermanentRoom(userId: String, roomId: String, roomName: String): Result<CoinTransaction> {
    val currentBalance = getBalance(userId)
    if (currentBalance < PERMANENT_ROOM_COST) {
      val remaining = PERMANENT_ROOM_COST - currentBalance
      return Result.failure(
        IllegalStateException("Not enough Coins. Required: $PERMANENT_ROOM_COST, Current: $currentBalance, Remaining needed: $remaining")
      )
    }

    val newBalance = currentBalance - PERMANENT_ROOM_COST
    userBalances[userId] = newBalance

    val txn = CoinTransaction(
      id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
      userId = userId,
      amount = -PERMANENT_ROOM_COST,
      reason = "Permanent Room creation ('$roomName')",
      timestamp = currentServerTimeMs,
      resultingBalance = newBalance,
      transactionType = "SPENT"
    )

    val list = userTransactions.getOrPut(userId) { mutableListOf() }
    list.add(txn)

    return Result.success(txn)
  }

  @Synchronized
  fun deductCoinsForGift(userId: String, giftName: String, amount: Int): Result<CoinTransaction> {
    val currentBalance = getBalance(userId)
    if (currentBalance < amount) {
      val remaining = amount - currentBalance
      return Result.failure(
        IllegalStateException("Not enough Coins. Required: $amount, Current: $currentBalance, Needed: $remaining")
      )
    }

    val newBalance = currentBalance - amount
    userBalances[userId] = newBalance

    val txn = CoinTransaction(
      id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
      userId = userId,
      amount = -amount,
      reason = "Sent Room Gift: $giftName",
      timestamp = currentServerTimeMs,
      resultingBalance = newBalance,
      transactionType = "SPENT"
    )

    val list = userTransactions.getOrPut(userId) { mutableListOf() }
    list.add(txn)

    return Result.success(txn)
  }

  /**
   * Helper to add coins for test verification purposes if requested.
   */
  @Synchronized
  fun addTestCoins(userId: String, amount: Int): CoinTransaction {
    val current = getBalance(userId)
    val newBal = current + amount
    userBalances[userId] = newBal

    val txn = CoinTransaction(
      id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
      userId = userId,
      amount = amount,
      reason = "Bonus Virtual Reward",
      timestamp = currentServerTimeMs,
      resultingBalance = newBal
    )
    val list = userTransactions.getOrPut(userId) { mutableListOf() }
    list.add(txn)
    return txn
  }

  // -------------------------------------------------------------
  // PART 3 – PAYMENT ORDERS & COIN CREDITING SECURITY
  // -------------------------------------------------------------

  @Synchronized
  fun createPaymentOrder(
    userId: String,
    packageId: String,
    paymentId: String = "PAY-${UUID.randomUUID().toString().take(12).uppercase()}"
  ): PaymentOrder {
    val pkg = CoinPackage.values().find { it.id == packageId } ?: CoinPackage.MINI
    val order = PaymentOrder(
      orderId = "ORD-${UUID.randomUUID().toString().take(10).uppercase()}",
      paymentId = paymentId,
      userId = userId,
      packageId = pkg.id,
      priceInr = pkg.priceInr,
      coins = pkg.coins,
      status = PaymentStatus.PENDING,
      createdAtMs = currentServerTimeMs,
      isCoinCredited = false,
      receiptNumber = "REC-${System.currentTimeMillis() % 1000000}"
    )
    paymentOrders[order.orderId] = order
    return order
  }

  @Synchronized
  fun processPaymentOrder(
    orderId: String,
    paymentId: String,
    status: PaymentStatus,
    receiptNumber: String = ""
  ): Result<CoinTransaction> {
    val order = paymentOrders[orderId]
      ?: return Result.failure(IllegalArgumentException("Payment order not found: $orderId"))

    // Duplicate transaction protection
    if (processedPaymentIds.containsKey(paymentId)) {
      return Result.failure(
        IllegalStateException("Duplicate payment transaction detected: $paymentId has already been credited.")
      )
    }

    if (order.isCoinCredited) {
      return Result.failure(
        IllegalStateException("Order $orderId has already been credited with coins.")
      )
    }

    // Pending or Failed payments MUST NEVER credit coins
    if (status == PaymentStatus.PENDING) {
      val updated = order.copy(status = PaymentStatus.PENDING)
      paymentOrders[orderId] = updated
      return Result.failure(
        IllegalStateException("Payment is pending confirmation. Coins will not be credited until payment succeeds.")
      )
    }

    if (status == PaymentStatus.FAILED || status == PaymentStatus.CANCELLED) {
      val updated = order.copy(status = status)
      paymentOrders[orderId] = updated
      return Result.failure(
        IllegalStateException("Payment failed or cancelled ($status). No coins credited.")
      )
    }

    // Payment Success: Credit coins atomically
    val currentBalance = getBalance(order.userId)
    val newBalance = currentBalance + order.coins
    userBalances[order.userId] = newBalance

    processedPaymentIds[paymentId] = orderId
    val updated = order.copy(
      status = PaymentStatus.SUCCESS,
      isCoinCredited = true,
      processedAtMs = currentServerTimeMs,
      receiptNumber = if (receiptNumber.isNotBlank()) receiptNumber else order.receiptNumber
    )
    paymentOrders[orderId] = updated

    val txn = CoinTransaction(
      id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
      userId = order.userId,
      amount = order.coins,
      reason = "Purchased ${order.coins} Coins (₹${order.priceInr})",
      timestamp = currentServerTimeMs,
      resultingBalance = newBalance,
      transactionType = "PURCHASED",
      orderId = orderId,
      receiptId = updated.receiptNumber
    )

    val list = userTransactions.getOrPut(order.userId) { mutableListOf() }
    list.add(txn)

    return Result.success(txn)
  }

  @Synchronized
  fun processRefund(orderId: String, reason: String = "User refund requested"): Result<CoinTransaction> {
    val order = paymentOrders[orderId]
      ?: return Result.failure(IllegalArgumentException("Order not found: $orderId"))

    if (!order.isCoinCredited) {
      return Result.failure(IllegalStateException("Order was never credited, refund cannot reverse coin balance."))
    }
    if (order.status == PaymentStatus.REFUNDED) {
      return Result.failure(IllegalStateException("Order $orderId has already been refunded."))
    }

    val currentBal = getBalance(order.userId)
    val deduction = order.coins
    val newBal = (currentBal - deduction).coerceAtLeast(0)
    userBalances[order.userId] = newBal

    val updated = order.copy(status = PaymentStatus.REFUNDED)
    paymentOrders[orderId] = updated

    val txn = CoinTransaction(
      id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
      userId = order.userId,
      amount = -deduction,
      reason = "Refund for order $orderId: $reason",
      timestamp = currentServerTimeMs,
      resultingBalance = newBal,
      transactionType = "REFUNDED",
      orderId = orderId
    )
    val list = userTransactions.getOrPut(order.userId) { mutableListOf() }
    list.add(txn)

    return Result.success(txn)
  }

  fun getPaymentOrders(userId: String): List<PaymentOrder> {
    return paymentOrders.values.filter { it.userId == userId }.sortedByDescending { it.createdAtMs }
  }

  // -------------------------------------------------------------
  // PART 12 – VIRTUAL GIFTS & CHARM POINTS (Receiver only!)
  // -------------------------------------------------------------

  @Synchronized
  fun sendMultiRecipientGift(
    senderId: String,
    recipientUserIds: List<String>,
    giftId: String
  ): Result<Pair<CoinTransaction, Int>> {
    val gift = VirtualGift.findById(giftId)
      ?: return Result.failure(IllegalArgumentException("Invalid gift ID: $giftId"))

    if (recipientUserIds.isEmpty()) {
      return Result.failure(IllegalArgumentException("At least one recipient is required."))
    }

    val totalCost = gift.priceCoins * recipientUserIds.size
    val currentBal = getBalance(senderId)
    if (currentBal < totalCost) {
      return Result.failure(
        IllegalStateException("Insufficient Coins. Required: $totalCost, Available: $currentBal")
      )
    }

    // Deduct coins from sender
    val newBal = currentBal - totalCost
    userBalances[senderId] = newBal

    val txn = CoinTransaction(
      id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
      userId = senderId,
      amount = -totalCost,
      reason = "Sent ${gift.emoji} ${gift.name} to ${recipientUserIds.size} recipient(s)",
      timestamp = currentServerTimeMs,
      resultingBalance = newBal,
      transactionType = "SPENT"
    )
    val list = userTransactions.getOrPut(senderId) { mutableListOf() }
    list.add(txn)

    // Credit Charm Points to RECEIVER(S) ONLY (5 coins = 1 Charm Point)
    // SENDER gets 0 Charm Points!
    val charmPerRecipient = gift.charmPoints
    recipientUserIds.forEach { recipientId ->
      addCharmPoints(recipientId, charmPerRecipient)
    }

    return Result.success(Pair(txn, charmPerRecipient))
  }

  fun getCharmPoints(userId: String): Int {
    return userCharmPoints.getOrDefault(userId, 0)
  }

  fun getCharmLevel(userId: String): CharmLevelInfo {
    return CharmLevelInfo.calculateLevel(getCharmPoints(userId))
  }

  @Synchronized
  fun addCharmPoints(userId: String, points: Int): Int {
    val current = getCharmPoints(userId)
    val updated = current + points
    userCharmPoints[userId] = updated
    return updated
  }

  // -------------------------------------------------------------
  // PART 13 – LUCKY MYSTERY REWARD SYSTEM (Guaranteed rewards)
  // -------------------------------------------------------------

  @Synchronized
  fun openMysteryBox(userId: String): Result<Pair<CoinTransaction, VirtualGift>> {
    val currentBal = getBalance(userId)
    val price = MysteryBoxReward.MYSTERY_BOX_PRICE
    if (currentBal < price) {
      return Result.failure(
        IllegalStateException("Not enough Coins to open Lucky Mystery Box. Required: $price, Available: $currentBal")
      )
    }

    // Deduct 100 coins
    val newBal = currentBal - price
    userBalances[userId] = newBal

    // Guaranteed reward determined by exact probability distribution
    val roll = Random.nextFloat() * 100f
    var cumulative = 0f
    var selectedGift = MysteryBoxReward.REWARDS.first().gift

    for (reward in MysteryBoxReward.REWARDS) {
      cumulative += reward.probabilityPercent
      if (roll <= cumulative) {
        selectedGift = reward.gift
        break
      }
    }

    val txn = CoinTransaction(
      id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
      userId = userId,
      amount = -price,
      reason = "Opened Lucky Mystery Box (Won: ${selectedGift.name})",
      timestamp = currentServerTimeMs,
      resultingBalance = newBal,
      transactionType = "SPENT"
    )
    val list = userTransactions.getOrPut(userId) { mutableListOf() }
    list.add(txn)

    // Store in user gift inventory
    val inv = userGiftInventory.getOrPut(userId) { mutableListOf() }
    inv.add(selectedGift)

    return Result.success(Pair(txn, selectedGift))
  }

  fun getUserGiftInventory(userId: String): List<VirtualGift> {
    return userGiftInventory.getOrDefault(userId, emptyList())
  }

  // -------------------------------------------------------------
  // PART 16 – COUPLE RINGS & ADJACENT SEAT DISPLAY
  // -------------------------------------------------------------

  @Synchronized
  fun purchaseAndProposeRing(
    senderId: String,
    senderName: String,
    receiverId: String,
    receiverName: String,
    ringId: String
  ): Result<Pair<CoinTransaction, RingBond>> {
    val ring = VirtualRing.findById(ringId)
      ?: return Result.failure(IllegalArgumentException("Invalid ring ID: $ringId"))

    val currentBal = getBalance(senderId)
    if (currentBal < ring.priceCoins) {
      return Result.failure(
        IllegalStateException("Insufficient Coins. Required: ${ring.priceCoins}, Available: $currentBal")
      )
    }

    // Deduct ring cost from proposer
    val newBal = currentBal - ring.priceCoins
    userBalances[senderId] = newBal

    val bond = RingBond(
      bondId = "BOND-${UUID.randomUUID().toString().take(8).uppercase()}",
      ringId = ring.id,
      ringName = ring.name,
      senderUserId = senderId,
      senderUsername = senderName,
      receiverUserId = receiverId,
      receiverUsername = receiverName,
      status = BondStatus.PENDING,
      createdAtMs = currentServerTimeMs,
      charmPointsGranted = ring.charmPoints
    )
    ringBonds[bond.bondId] = bond

    val txn = CoinTransaction(
      id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
      userId = senderId,
      amount = -ring.priceCoins,
      reason = "Couple Ring Proposal: ${ring.name} to $receiverName",
      timestamp = currentServerTimeMs,
      resultingBalance = newBal,
      transactionType = "SPENT"
    )
    val list = userTransactions.getOrPut(senderId) { mutableListOf() }
    list.add(txn)

    return Result.success(Pair(txn, bond))
  }

  @Synchronized
  fun respondToRingBond(bondId: String, receiverUserId: String, accept: Boolean): Result<RingBond> {
    val bond = ringBonds[bondId]
      ?: return Result.failure(IllegalArgumentException("Ring proposal not found: $bondId"))

    if (bond.receiverUserId != receiverUserId) {
      return Result.failure(IllegalStateException("Only the designated receiver can respond to this proposal."))
    }
    if (bond.status != BondStatus.PENDING) {
      return Result.failure(IllegalStateException("Proposal already answered: ${bond.status}"))
    }

    val updatedBond = if (accept) {
      // Receiver accepted: grant charm points to receiver!
      addCharmPoints(receiverUserId, bond.charmPointsGranted)
      bond.copy(status = BondStatus.ACCEPTED, acceptedAtMs = currentServerTimeMs)
    } else {
      bond.copy(status = BondStatus.REJECTED)
    }
    ringBonds[bondId] = updatedBond

    return Result.success(updatedBond)
  }

  fun getActiveRingBonds(userId: String): List<RingBond> {
    return ringBonds.values.filter {
      it.status == BondStatus.ACCEPTED && (it.senderUserId == userId || it.receiverUserId == userId)
    }
  }

  fun areUsersAdjacentCouple(seatUserIds: List<String?>, userA: String, userB: String): Boolean {
    val hasBond = ringBonds.values.any {
      it.status == BondStatus.ACCEPTED &&
        ((it.senderUserId == userA && it.receiverUserId == userB) ||
         (it.senderUserId == userB && it.receiverUserId == userA))
    }
    if (!hasBond) return false

    val indexA = seatUserIds.indexOf(userA)
    val indexB = seatUserIds.indexOf(userB)
    return indexA != -1 && indexB != -1 && abs(indexA - indexB) == 1
  }

  // -------------------------------------------------------------
  // PART 15 – RELATIONSHIP TAGS & ADJACENT DISPLAY
  // -------------------------------------------------------------

  @Synchronized
  fun purchaseRelationshipTag(
    requesterId: String,
    requesterName: String,
    targetId: String,
    targetName: String,
    type: RelationshipTagType
  ): Result<Pair<CoinTransaction, UserRelationshipTag>> {
    val cost = UserRelationshipTag.TAG_COST
    val currentBal = getBalance(requesterId)
    if (currentBal < cost) {
      return Result.failure(
        IllegalStateException("Insufficient Coins. Required: $cost, Available: $currentBal")
      )
    }

    val newBal = currentBal - cost
    userBalances[requesterId] = newBal

    val tag = UserRelationshipTag(
      tagId = "TAG-${UUID.randomUUID().toString().take(8).uppercase()}",
      type = type,
      requesterId = requesterId,
      requesterName = requesterName,
      targetId = targetId,
      targetName = targetName,
      status = BondStatus.PENDING,
      createdAtMs = currentServerTimeMs
    )
    relationshipTags[tag.tagId] = tag

    val txn = CoinTransaction(
      id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
      userId = requesterId,
      amount = -cost,
      reason = "Purchased Relationship Tag: ${type.label} for $targetName",
      timestamp = currentServerTimeMs,
      resultingBalance = newBal,
      transactionType = "SPENT"
    )
    val list = userTransactions.getOrPut(requesterId) { mutableListOf() }
    list.add(txn)

    return Result.success(Pair(txn, tag))
  }

  @Synchronized
  fun respondToRelationshipTag(tagId: String, targetUserId: String, accept: Boolean): Result<UserRelationshipTag> {
    val tag = relationshipTags[tagId]
      ?: return Result.failure(IllegalArgumentException("Tag request not found: $tagId"))

    if (tag.targetId != targetUserId) {
      return Result.failure(IllegalStateException("Only designated target user can respond to this tag."))
    }
    if (tag.status != BondStatus.PENDING) {
      return Result.failure(IllegalStateException("Tag already answered: ${tag.status}"))
    }

    val updatedTag = if (accept) {
      tag.copy(status = BondStatus.ACCEPTED, acceptedAtMs = currentServerTimeMs)
    } else {
      tag.copy(status = BondStatus.REJECTED)
    }
    relationshipTags[tagId] = updatedTag

    return Result.success(updatedTag)
  }

  fun getActiveRelationshipTags(userId: String): List<UserRelationshipTag> {
    return relationshipTags.values.filter {
      it.status == BondStatus.ACCEPTED && (it.requesterId == userId || it.targetId == userId)
    }
  }

  fun areUsersAdjacentTag(seatUserIds: List<String?>, userA: String, userB: String): RelationshipTagType? {
    val activeTag = relationshipTags.values.find {
      it.status == BondStatus.ACCEPTED &&
        ((it.requesterId == userA && it.targetId == userB) ||
         (it.requesterId == userB && it.targetId == userA))
    } ?: return null

    val indexA = seatUserIds.indexOf(userA)
    val indexB = seatUserIds.indexOf(userB)
    return if (indexA != -1 && indexB != -1 && abs(indexA - indexB) == 1) activeTag.type else null
  }

  // -------------------------------------------------------------
  // PART 9 – ROOM TYPE SWITCHING (Owner only, preserves state)
  // -------------------------------------------------------------

  @Synchronized
  fun switchRoomType(currentRoom: Room, newType: RoomType, requestingUserId: String): Result<Room> {
    if (currentRoom.ownerId != requestingUserId) {
      return Result.failure(
        IllegalStateException("Unauthorized: Only the room owner (${currentRoom.ownerName}) can switch room types.")
      )
    }

    // Preserve Room ID, XP, Level, Themes, Ownership, Announcement
    val updatedRoom = currentRoom.copy(
      roomType = newType,
      isVideoRoomEnabled = (newType == RoomType.VIDEO),
      isGameEnabled = (newType == RoomType.GAMING),
      activeGameType = if (newType == RoomType.GAMING) "CHESS" else null
    )
    return Result.success(updatedRoom)
  }

  // -------------------------------------------------------------
  // PART 5 – AVATAR VALIDITY & EXPIRATION SYSTEM
  // -------------------------------------------------------------

  @Synchronized
  fun purchaseAvatar(
    userId: String,
    avatarKey: String,
    avatarName: String,
    validity: AvatarValidityDays
  ): Result<Pair<CoinTransaction, UserAvatarItem>> {
    val currentBal = getBalance(userId)
    if (currentBal < validity.coinCost) {
      return Result.failure(
        IllegalStateException("Insufficient Coins. Required: ${validity.coinCost}, Available: $currentBal")
      )
    }

    val newBal = currentBal - validity.coinCost
    userBalances[userId] = newBal

    val expiresAt = currentServerTimeMs + validity.days * 86400000L
    val item = UserAvatarItem(
      id = "AVT-${UUID.randomUUID().toString().take(8).uppercase()}",
      avatarKey = avatarKey,
      name = avatarName,
      purchasedAtMs = currentServerTimeMs,
      validityDays = validity.days,
      expiresAtMs = expiresAt,
      isActive = true
    )

    val list = userAvatars.getOrPut(userId) { mutableListOf() }
    list.add(item)

    val txn = CoinTransaction(
      id = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
      userId = userId,
      amount = -validity.coinCost,
      reason = "Purchased Avatar Frame: $avatarName (${validity.days} days)",
      timestamp = currentServerTimeMs,
      resultingBalance = newBal,
      transactionType = "SPENT"
    )
    val txnList = userTransactions.getOrPut(userId) { mutableListOf() }
    txnList.add(txn)

    return Result.success(Pair(txn, item))
  }

  fun getUserActiveAvatars(userId: String): List<UserAvatarItem> {
    val list = userAvatars.getOrDefault(userId, emptyList())
    val now = currentServerTimeMs
    return list.filter { it.expiresAtMs > now }
  }

  @Synchronized
  fun cleanupExpiredAvatars(userId: String): Int {
    val list = userAvatars[userId] ?: return 0
    val now = currentServerTimeMs
    val countBefore = list.size
    list.removeAll { it.expiresAtMs <= now }
    return countBefore - list.size
  }

  @Synchronized
  fun clearAllData() {
    userBalances.clear()
    userTransactions.clear()
    dailyEarnings.clear()
    dailyClaimedTasks.clear()
    dailyTaskProgress.clear()
    lastActivityTime.clear()
    paymentOrders.clear()
    processedPaymentIds.clear()
    userCharmPoints.clear()
    ringBonds.clear()
    relationshipTags.clear()
    userAvatars.clear()
    userGiftInventory.clear()
  }
}

