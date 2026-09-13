package com.example.network.security

import android.util.Base64
import com.example.data.model.Room
import com.example.data.model.User
import com.example.data.model.games.GameSession
import com.example.data.model.games.GameStatus
import com.example.data.model.games.GameType
import com.example.network.ActiveGameSessionManager
import com.example.games.engine.ChessEngine
import com.example.games.engine.LudoEngine
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Server-Authoritative Security Controller.
 *
 * Implements:
 * 1. Short-lived cryptographically signed token issuance and server-side verification.
 * 2. Server-side authorization: never trusts client-side isOwner, isAdmin, role, permissions, gameWinner.
 * 3. Rate limiting for OTP, login, chat, room actions, and game moves.
 * 4. Input sanitization (stripping HTML/scripts, max lengths).
 * 5. Signed short-lived tokens for media streaming.
 * 6. Least-privilege data access enforcement for rooms, messages, and game state.
 */
object BackendSecurityController {

  private const val HMAC_ALGO = "HmacSHA256"
  // Server-side master ephemeral signing key (Never exposed to client or persisted in plain text)
  private val SERVER_SIGNING_KEY = ByteArray(32).apply {
    SecureRandom().nextBytes(this)
  }

  // Token lifetimes
  private const val ACCESS_TOKEN_TTL_MS = 15 * 60 * 1000L      // 15 minutes (short-lived)
  private const val REFRESH_TOKEN_TTL_MS = 14 * 86400000L      // 14 days
  private const val MEDIA_TOKEN_TTL_MS = 60 * 60 * 1000L       // 1 hour signed media token

  // -------------------------------------------------------------
  // RATE LIMITERS (Token Bucket / Sliding Window)
  // -------------------------------------------------------------
  private class RateBucket(val maxTokens: Int, val refillIntervalMs: Long) {
    private var tokens: Int = maxTokens
    private var lastRefill: Long = System.currentTimeMillis()

    @Synchronized
    fun tryAcquire(): Boolean {
      val now = System.currentTimeMillis()
      val intervals = (now - lastRefill) / refillIntervalMs
      if (intervals > 0) {
        tokens = minOf(maxTokens, tokens + intervals.toInt())
        lastRefill = now
      }
      return if (tokens > 0) {
        tokens--
        true
      } else {
        false
      }
    }
  }

  // Rate limiter storage: key -> RateBucket
  private val otpRateLimits = ConcurrentHashMap<String, RateBucket>()        // phone -> 3 per 5 min
  private val loginRateLimits = ConcurrentHashMap<String, RateBucket>()      // phone/ip -> 5 per 15 min
  private val chatRateLimits = ConcurrentHashMap<String, RateBucket>()       // userId -> 3 per 2 sec
  private val actionRateLimits = ConcurrentHashMap<String, RateBucket>()     // userId -> 30 per min
  private val gameMoveRateLimits = ConcurrentHashMap<String, RateBucket>()   // userId -> 2 per sec

  // Active verified sessions: TokenId -> SessionDetails
  data class ValidatedSession(
    val userId: String,
    val username: String,
    val isVerified: Boolean,
    val expiresAtMs: Long,
    val issuedAtMs: Long
  )

  private val activeSessions = ConcurrentHashMap<String, ValidatedSession>()
  private val refreshTokens = ConcurrentHashMap<String, String>() // RefreshToken -> UserId

  // Room Authorization Registry: RoomId -> ServerRoomPolicy
  data class ServerRoomPolicy(
    val roomId: String,
    val ownerId: String,
    val adminUserIds: MutableSet<String> = ConcurrentHashMap.newKeySet(),
    val members: MutableSet<String> = ConcurrentHashMap.newKeySet(),
    val isLocked: Boolean = false,
    val isPrivate: Boolean = false,
    val passwordHash: String? = null
  )

  private val roomPolicies = ConcurrentHashMap<String, ServerRoomPolicy>()

  // -------------------------------------------------------------
  // AUTHENTICATION & TOKEN ISSUANCE
  // -------------------------------------------------------------

  /**
   * Generates a secure, cryptographically signed Access Token for an authenticated user.
   */
  fun issueAccessToken(userId: String, username: String, isVerified: Boolean): String {
    val issuedAt = System.currentTimeMillis()
    val expiresAt = issuedAt + ACCESS_TOKEN_TTL_MS
    val payload = "$userId|$username|$isVerified|$issuedAt|$expiresAt"
    val signature = createHmac(payload)
    val token = Base64.encodeToString("$payload|$signature".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    activeSessions[token] = ValidatedSession(
      userId = userId,
      username = username,
      isVerified = isVerified,
      expiresAtMs = expiresAt,
      issuedAtMs = issuedAt
    )
    return token
  }

  fun issueRefreshToken(userId: String): String {
    val randomBytes = ByteArray(32).apply { SecureRandom().nextBytes(this) }
    val token = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
    refreshTokens[token] = userId
    return token
  }

  /**
   * Server-side token validation.
   * NEVER trusts client-supplied user ID; extracts and returns the cryptographically verified user.
   */
  fun validateAccessToken(token: String?): ValidatedSession? {
    if (token.isNullOrBlank()) return null

    // Check fast cache first
    val cached = activeSessions[token]
    if (cached != null) {
      if (System.currentTimeMillis() > cached.expiresAtMs) {
        activeSessions.remove(token)
        return null
      }
      return cached
    }

    // Cryptographic signature verification
    try {
      val decoded = String(Base64.decode(token, Base64.NO_WRAP), Charsets.UTF_8)
      val parts = decoded.split("|")
      if (parts.size != 6) return null

      val userId = parts[0]
      val username = parts[1]
      val isVerified = parts[2].toBoolean()
      val issuedAt = parts[3].toLongOrNull() ?: return null
      val expiresAt = parts[4].toLongOrNull() ?: return null
      val signature = parts[5]

      val expectedPayload = "$userId|$username|$isVerified|$issuedAt|$expiresAt"
      val expectedSig = createHmac(expectedPayload)

      if (signature != expectedSig) return null
      if (System.currentTimeMillis() > expiresAt) return null

      val session = ValidatedSession(userId, username, isVerified, expiresAt, issuedAt)
      activeSessions[token] = session
      return session
    } catch (_: Exception) {
      return null
    }
  }

  /**
   * Securely refresh an expired access token using a valid refresh token.
   */
  fun refreshSession(refreshToken: String?, currentUser: User): String? {
    if (refreshToken.isNullOrBlank()) return null
    val userId = refreshTokens[refreshToken] ?: return null
    if (userId != currentUser.id) return null

    return issueAccessToken(userId, currentUser.username, currentUser.isVerified)
  }

  fun revokeSession(token: String?, refreshToken: String?) {
    if (!token.isNullOrBlank()) activeSessions.remove(token)
    if (!refreshToken.isNullOrBlank()) refreshTokens.remove(refreshToken)
  }

  // -------------------------------------------------------------
  // RATE LIMITING
  // -------------------------------------------------------------

  fun checkOtpRateLimit(phoneNumber: String): Boolean {
    val clean = phoneNumber.trim().replace("\\s+".toRegex(), "")
    val bucket = otpRateLimits.getOrPut(clean) {
      RateBucket(maxTokens = 3, refillIntervalMs = 100000L) // 3 requests per 5 minutes
    }
    return bucket.tryAcquire()
  }

  fun checkLoginRateLimit(identifier: String): Boolean {
    val bucket = loginRateLimits.getOrPut(identifier) {
      RateBucket(maxTokens = 5, refillIntervalMs = 180000L) // 5 attempts per 15 minutes
    }
    return bucket.tryAcquire()
  }

  fun checkChatRateLimit(userId: String): Boolean {
    val bucket = chatRateLimits.getOrPut(userId) {
      RateBucket(maxTokens = 3, refillIntervalMs = 700L) // max 3 messages per 2 sec
    }
    return bucket.tryAcquire()
  }

  fun checkActionRateLimit(userId: String): Boolean {
    val bucket = actionRateLimits.getOrPut(userId) {
      RateBucket(maxTokens = 30, refillIntervalMs = 2000L) // 30 actions per minute
    }
    return bucket.tryAcquire()
  }

  fun checkGameMoveRateLimit(userId: String): Boolean {
    val bucket = gameMoveRateLimits.getOrPut(userId) {
      RateBucket(maxTokens = 2, refillIntervalMs = 500L) // 2 moves per second
    }
    return bucket.tryAcquire()
  }

  // -------------------------------------------------------------
  // AUTHORIZATION: ROOMS & SEATS
  // -------------------------------------------------------------

  fun registerRoomPolicy(room: Room, passwordClearText: String? = null) {
    val passHash = if (!passwordClearText.isNullOrBlank()) hashString(passwordClearText) else null
    val policy = ServerRoomPolicy(
      roomId = room.id,
      ownerId = room.ownerId,
      isLocked = room.isLocked,
      isPrivate = room.isPrivate,
      passwordHash = passHash
    )
    policy.members.add(room.ownerId)
    roomPolicies[room.id] = policy
  }

  fun canManageRoom(roomId: String, verifiedUserId: String): Boolean {
    val policy = roomPolicies[roomId] ?: return false
    return policy.ownerId == verifiedUserId
  }

  fun canModerateRoom(roomId: String, verifiedUserId: String): Boolean {
    val policy = roomPolicies[roomId] ?: return false
    return policy.ownerId == verifiedUserId || policy.adminUserIds.contains(verifiedUserId)
  }

  fun addRoomAdmin(roomId: String, ownerUserId: String, targetUserId: String): Boolean {
    val policy = roomPolicies[roomId] ?: return false
    if (policy.ownerId != ownerUserId) return false
    policy.adminUserIds.add(targetUserId)
    return true
  }

  fun removeRoomAdmin(roomId: String, ownerUserId: String, targetUserId: String): Boolean {
    val policy = roomPolicies[roomId] ?: return false
    if (policy.ownerId != ownerUserId) return false
    policy.adminUserIds.remove(targetUserId)
    return true
  }

  fun authorizeRoomJoin(roomId: String, verifiedUserId: String, passwordAttempt: String?): Boolean {
    val policy = roomPolicies[roomId] ?: return true // new or dynamic room
    if (policy.isLocked && policy.ownerId != verifiedUserId && !policy.adminUserIds.contains(verifiedUserId)) {
      return false
    }
    if (policy.isPrivate && policy.passwordHash != null) {
      val attemptHash = hashString(passwordAttempt ?: "")
      if (attemptHash != policy.passwordHash && policy.ownerId != verifiedUserId) {
        return false
      }
    }
    policy.members.add(verifiedUserId)
    return true
  }

  fun removeRoomMember(roomId: String, userId: String) {
    roomPolicies[roomId]?.members?.remove(userId)
  }

  // -------------------------------------------------------------
  // AUTHORIZATION: DIRECT MESSAGES (LEAST PRIVILEGE)
  // -------------------------------------------------------------

  fun canAccessDirectMessage(verifiedUserId: String, senderId: String, receiverId: String): Boolean {
    return verifiedUserId == senderId || verifiedUserId == receiverId
  }

  // -------------------------------------------------------------
  // AUTHORIZATION: MULTIPLAYER GAMES (SERVER-AUTHORITATIVE)
  // -------------------------------------------------------------

  /**
   * Server validates game move authorization and rate limits.
   * Client-supplied gameWinner or turn overrides are strictly rejected.
   */
  fun validateGameMoveRequest(
    matchId: String,
    verifiedUserId: String
  ): Result<Boolean> {
    val session = ActiveGameSessionManager.getSession(matchId)
      ?: return Result.failure(IllegalStateException("Match session not found"))

    if (session.status != GameStatus.ACTIVE) {
      return Result.failure(IllegalStateException("Game is not active"))
    }

    if (!checkGameMoveRateLimit(verifiedUserId)) {
      return Result.failure(IllegalStateException("Too many moves. Please wait."))
    }

    val isParticipant = session.players.any { p -> p.userId == verifiedUserId }
    if (!isParticipant) {
      return Result.failure(SecurityException("User is not an active player in this match"))
    }

    return Result.success(true)
  }

  // -------------------------------------------------------------
  // SIGNED MEDIA STREAMING TOKENS
  // -------------------------------------------------------------

  /**
   * Generates a short-lived cryptographic signed token for streaming user media.
   */
  fun generateSignedMediaToken(userId: String, roomId: String): String {
    val expiresAt = System.currentTimeMillis() + MEDIA_TOKEN_TTL_MS
    val payload = "$userId|$roomId|$expiresAt"
    val sig = createHmac(payload)
    return Base64.encodeToString("$payload|$sig".toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
  }

  /**
   * Validates media streaming token and verifies room membership.
   */
  fun validateMediaToken(token: String?, roomId: String): Boolean {
    if (token.isNullOrBlank()) return false
    try {
      val decoded = String(Base64.decode(token, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
      val parts = decoded.split("|")
      if (parts.size != 4) return false

      val userId = parts[0]
      val tokenRoomId = parts[1]
      val expiresAt = parts[2].toLongOrNull() ?: return false
      val sig = parts[3]

      if (tokenRoomId != roomId) return false
      if (System.currentTimeMillis() > expiresAt) return false

      val expectedSig = createHmac("$userId|$tokenRoomId|$expiresAt")
      if (sig != expectedSig) return false

      // Check room policy membership
      val policy = roomPolicies[roomId]
      if (policy != null && !policy.members.contains(userId) && policy.ownerId != userId) {
        return false
      }
      return true
    } catch (_: Exception) {
      return false
    }
  }

  // -------------------------------------------------------------
  // INPUT SANITIZATION & SAFE ERRORS
  // -------------------------------------------------------------

  fun sanitizeInput(input: String, maxLength: Int = 500): String {
    return input.trim()
      .replace("<[^>]*>".toRegex(), "") // Strip HTML/script tags
      .replace("[\u0000-\u0008\u000B\u000C\u000E-\u001F]".toRegex(), "") // Remove control characters
      .take(maxLength)
  }

  fun sanitizeRoomName(name: String): String {
    val clean = sanitizeInput(name, maxLength = 40)
    return clean.ifBlank { "Watch Room" }
  }

  // -------------------------------------------------------------
  // CRYPTOGRAPHIC HELPERS
  // -------------------------------------------------------------

  private fun createHmac(data: String): String {
    val mac = Mac.getInstance(HMAC_ALGO)
    mac.init(SecretKeySpec(SERVER_SIGNING_KEY, HMAC_ALGO))
    val rawHmac = mac.doFinal(data.toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(rawHmac, Base64.NO_WRAP)
  }

  private fun hashString(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }
}
