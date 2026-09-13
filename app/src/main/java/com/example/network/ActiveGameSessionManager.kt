package com.example.network

import android.util.Log
import com.example.data.model.games.*
import com.example.games.engine.ChessEngine
import com.example.games.engine.LudoEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object ActiveGameSessionManager {
  private const val TAG = "GameSessionManager"
  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  // Thread-safe game sessions map
  private val sessions = ConcurrentHashMap<String, GameSession>()

  // Active game sessions flow
  private val _activeSessions = MutableStateFlow<Map<String, GameSession>>(emptyMap())
  val activeSessions: StateFlow<Map<String, GameSession>> = _activeSessions.asStateFlow()

  // Matchmaking Queue: Map<UserId, MatchmakingTicket>
  data class MatchmakingTicket(
    val userId: String,
    val username: String,
    val avatar: String,
    val gameType: GameType,
    val queuedAtMs: Long = System.currentTimeMillis()
  )

  private val matchmakingQueue = ConcurrentHashMap<String, MatchmakingTicket>()

  // Real-time invites: Map<InviteId, GameInvite>
  private val _pendingInvites = MutableStateFlow<List<GameInvite>>(emptyList())
  val pendingInvites: StateFlow<List<GameInvite>> = _pendingInvites.asStateFlow()

  // User Stats: Map<UserId, GamePlayerStats>
  private val _playerStats = MutableStateFlow<Map<String, GamePlayerStats>>(emptyMap())
  val playerStats: StateFlow<Map<String, GamePlayerStats>> = _playerStats.asStateFlow()

  init {
    startGameHeartbeatReaper()
  }

  fun getPlayerStats(userId: String): GamePlayerStats {
    return _playerStats.value[userId] ?: GamePlayerStats()
  }

  fun getSession(matchId: String): GameSession? = sessions[matchId]

  private fun updateStats(userId: String, gameType: GameType, won: Boolean) {
    val current = getPlayerStats(userId)
    val updated = when (gameType) {
      GameType.CHESS -> current.copy(
        chessGamesPlayed = current.chessGamesPlayed + 1,
        chessWins = if (won) current.chessWins + 1 else current.chessWins
      )
      GameType.LUDO -> current.copy(
        ludoGamesPlayed = current.ludoGamesPlayed + 1,
        ludoWins = if (won) current.ludoWins + 1 else current.ludoWins
      )
    }
    _playerStats.value = _playerStats.value + (userId to updated)
  }

  /**
   * Generates a 6-character alphanumeric game code (e.g. "X7K92P").
   */
  fun generatePrivateCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
  }

  // -------------------------------------------------------------
  // MATCHMAKING (STRICTLY REAL ACTIVE PLAYERS)
  // -------------------------------------------------------------

  /**
   * Enqueues a real user into matchmaking.
   * If a real opponent of the same gameType is already waiting, pairs them up into an active GameSession!
   * Returns: GameSession if instantly paired, null if waiting for another real player.
   */
  fun enterMatchmaking(
    userId: String,
    username: String,
    avatar: String,
    gameType: GameType
  ): GameSession? {
    // Check if there is already a real active user in the queue
    val now = System.currentTimeMillis()
    val opponentTicket = matchmakingQueue.values.firstOrNull { ticket ->
      ticket.userId != userId &&
          ticket.gameType == gameType &&
          (now - ticket.queuedAtMs < 30000L) // only active within last 30s
    }

    if (opponentTicket != null) {
      // Real opponent found! Remove both from queue
      matchmakingQueue.remove(opponentTicket.userId)
      matchmakingQueue.remove(userId)

      val player1 = GamePlayer(
        userId = opponentTicket.userId,
        username = opponentTicket.username,
        avatar = opponentTicket.avatar,
        isHost = true,
        colorTag = if (gameType == GameType.CHESS) "WHITE" else "RED"
      )
      val player2 = GamePlayer(
        userId = userId,
        username = username,
        avatar = avatar,
        isHost = false,
        colorTag = if (gameType == GameType.CHESS) "BLACK" else "GREEN"
      )

      val session = createGameSession(
        gameType = gameType,
        gameMode = GameMode.RANDOM,
        privateCode = "",
        players = listOf(player1, player2)
      )
      return session
    } else {
      // Put user into real matchmaking queue
      matchmakingQueue[userId] = MatchmakingTicket(
        userId = userId,
        username = username,
        avatar = avatar,
        gameType = gameType
      )
      return null
    }
  }

  fun cancelMatchmaking(userId: String) {
    matchmakingQueue.remove(userId)
  }

  fun isUserInMatchmaking(userId: String): Boolean {
    return matchmakingQueue.containsKey(userId)
  }

  // -------------------------------------------------------------
  // PRIVATE & FRIEND SESSIONS
  // -------------------------------------------------------------

  fun createPrivateGame(
    hostId: String,
    hostName: String,
    hostAvatar: String,
    gameType: GameType
  ): GameSession {
    val code = generatePrivateCode()
    val hostPlayer = GamePlayer(
      userId = hostId,
      username = hostName,
      avatar = hostAvatar,
      isHost = true,
      colorTag = if (gameType == GameType.CHESS) "WHITE" else "RED"
    )
    val session = GameSession(
      gameType = gameType,
      gameMode = GameMode.PRIVATE,
      privateCode = code,
      status = GameStatus.WAITING_FOR_PLAYERS,
      players = listOf(hostPlayer)
    )
    sessions[session.matchId] = session
    publishSessions()
    return session
  }

  fun joinGameByCode(
    code: String,
    userId: String,
    username: String,
    avatar: String
  ): GameSession? {
    val cleanCode = code.trim().uppercase()
    val session = sessions.values.firstOrNull {
      it.privateCode.equals(cleanCode, ignoreCase = true) &&
          it.status == GameStatus.WAITING_FOR_PLAYERS &&
          it.players.none { p -> p.userId == userId }
    } ?: return null

    val newPlayer = GamePlayer(
      userId = userId,
      username = username,
      avatar = avatar,
      isHost = false,
      colorTag = if (session.gameType == GameType.CHESS) "BLACK" else "GREEN"
    )

    val updatedPlayers = session.players + newPlayer
    val startedSession = initGameBoards(session.copy(players = updatedPlayers))
    sessions[session.matchId] = startedSession
    publishSessions()
    return startedSession
  }

  fun createFriendInvite(
    senderId: String,
    senderName: String,
    senderAvatar: String,
    recipientId: String,
    gameType: GameType
  ): Pair<GameSession, GameInvite> {
    val hostPlayer = GamePlayer(
      userId = senderId,
      username = senderName,
      avatar = senderAvatar,
      isHost = true,
      colorTag = if (gameType == GameType.CHESS) "WHITE" else "RED"
    )
    val code = generatePrivateCode()
    val session = GameSession(
      gameType = gameType,
      gameMode = GameMode.FRIEND,
      privateCode = code,
      status = GameStatus.WAITING_FOR_PLAYERS,
      players = listOf(hostPlayer)
    )
    sessions[session.matchId] = session
    publishSessions()

    val invite = GameInvite(
      senderId = senderId,
      senderName = senderName,
      recipientId = recipientId,
      gameType = gameType,
      matchId = session.matchId,
      privateCode = code
    )
    _pendingInvites.value = _pendingInvites.value + invite
    return Pair(session, invite)
  }

  fun acceptFriendInvite(
    inviteId: String,
    recipientId: String,
    recipientName: String,
    recipientAvatar: String
  ): GameSession? {
    val invite = _pendingInvites.value.firstOrNull { it.inviteId == inviteId } ?: return null
    val session = sessions[invite.matchId] ?: return null

    _pendingInvites.value = _pendingInvites.value.filter { it.inviteId != inviteId }

    val joinPlayer = GamePlayer(
      userId = recipientId,
      username = recipientName,
      avatar = recipientAvatar,
      isHost = false,
      colorTag = if (session.gameType == GameType.CHESS) "BLACK" else "GREEN"
    )

    val startedSession = initGameBoards(session.copy(players = session.players + joinPlayer))
    sessions[session.matchId] = startedSession
    publishSessions()
    return startedSession
  }

  fun declineFriendInvite(inviteId: String) {
    val invite = _pendingInvites.value.firstOrNull { it.inviteId == inviteId }
    if (invite != null) {
      sessions.remove(invite.matchId)
      _pendingInvites.value = _pendingInvites.value.filter { it.inviteId != inviteId }
      publishSessions()
    }
  }

  // -------------------------------------------------------------
  // GAME INITIALIZATION & BOARD LOGIC
  // -------------------------------------------------------------

  private fun createGameSession(
    gameType: GameType,
    gameMode: GameMode,
    privateCode: String,
    players: List<GamePlayer>
  ): GameSession {
    val base = GameSession(
      gameType = gameType,
      gameMode = gameMode,
      privateCode = privateCode,
      players = players
    )
    val started = initGameBoards(base)
    sessions[started.matchId] = started
    publishSessions()
    return started
  }

  private fun initGameBoards(session: GameSession): GameSession {
    val startedAt = System.currentTimeMillis()
    return when (session.gameType) {
      GameType.CHESS -> {
        val white = session.players.getOrNull(0)
        val black = session.players.getOrNull(1)
        val chessState = ChessEngine.createInitialState(
          whiteId = white?.userId ?: "",
          whiteName = white?.username ?: "Player 1",
          blackId = black?.userId ?: "",
          blackName = black?.username ?: "Player 2"
        )
        session.copy(
          status = GameStatus.ACTIVE,
          startedAtMs = startedAt,
          chessState = chessState
        )
      }
      GameType.LUDO -> {
        val ludoState = LudoEngine.createInitialState(session.players)
        session.copy(
          status = GameStatus.ACTIVE,
          startedAtMs = startedAt,
          ludoState = ludoState
        )
      }
    }
  }

  // -------------------------------------------------------------
  // IN-GAME REAL-TIME ACTIONS
  // -------------------------------------------------------------

  fun makeChessMove(matchId: String, from: ChessPosition, to: ChessPosition): Boolean {
    val session = sessions[matchId] ?: return false
    val chess = session.chessState ?: return false
    if (session.status != GameStatus.ACTIVE) return false

    val updatedState = ChessEngine.applyMove(chess, from, to)
    if (updatedState == chess) return false // invalid move

    val isFinished = updatedState.isCheckmate || updatedState.isStalemate || updatedState.isDraw
    val newStatus = if (isFinished) GameStatus.FINISHED else GameStatus.ACTIVE
    val endedAt = if (isFinished) System.currentTimeMillis() else null

    if (isFinished) {
      updatedState.winnerId?.let { winner ->
        val loser = if (winner == updatedState.whitePlayerId) updatedState.blackPlayerId else updatedState.whitePlayerId
        updateStats(winner, GameType.CHESS, won = true)
        updateStats(loser, GameType.CHESS, won = false)
      }
    }

    sessions[matchId] = session.copy(
      chessState = updatedState,
      status = newStatus,
      endedAtMs = endedAt,
      winnerId = updatedState.winnerId,
      lastHeartbeatMs = System.currentTimeMillis()
    )
    publishSessions()
    return true
  }

  fun resignChess(matchId: String, resigningUserId: String) {
    val session = sessions[matchId] ?: return
    val chess = session.chessState ?: return
    if (session.status != GameStatus.ACTIVE) return

    val winnerId = if (resigningUserId == chess.whitePlayerId) chess.blackPlayerId else chess.whitePlayerId
    val updatedChess = chess.copy(
      winnerId = winnerId,
      resignedPlayerId = resigningUserId,
      isCheckmate = true
    )

    updateStats(winnerId, GameType.CHESS, won = true)
    updateStats(resigningUserId, GameType.CHESS, won = false)

    sessions[matchId] = session.copy(
      chessState = updatedChess,
      status = GameStatus.FINISHED,
      winnerId = winnerId,
      endedAtMs = System.currentTimeMillis()
    )
    publishSessions()
  }

  fun offerChessDraw(matchId: String, userId: String) {
    val session = sessions[matchId] ?: return
    val chess = session.chessState ?: return
    sessions[matchId] = session.copy(
      chessState = chess.copy(isDrawOfferedBy = userId)
    )
    publishSessions()
  }

  fun respondChessDraw(matchId: String, accepted: Boolean) {
    val session = sessions[matchId] ?: return
    val chess = session.chessState ?: return
    if (accepted) {
      val updatedChess = chess.copy(isDraw = true, isDrawOfferedBy = null)
      updateStats(chess.whitePlayerId, GameType.CHESS, won = false)
      updateStats(chess.blackPlayerId, GameType.CHESS, won = false)
      sessions[matchId] = session.copy(
        chessState = updatedChess,
        status = GameStatus.FINISHED,
        endedAtMs = System.currentTimeMillis()
      )
    } else {
      sessions[matchId] = session.copy(
        chessState = chess.copy(isDrawOfferedBy = null)
      )
    }
    publishSessions()
  }

  fun rollLudoDice(matchId: String, userId: String): Int {
    val session = sessions[matchId] ?: return 0
    val ludo = session.ludoState ?: return 0
    if (session.status != GameStatus.ACTIVE || ludo.currentTurnPlayerId != userId) return 0

    val (updatedLudo, roll) = LudoEngine.rollDice(ludo)
    sessions[matchId] = session.copy(
      ludoState = updatedLudo,
      lastHeartbeatMs = System.currentTimeMillis()
    )
    publishSessions()
    return roll
  }

  fun moveLudoToken(matchId: String, userId: String, tokenId: Int): Boolean {
    val session = sessions[matchId] ?: return false
    val ludo = session.ludoState ?: return false
    if (session.status != GameStatus.ACTIVE || ludo.currentTurnPlayerId != userId) return false

    val updatedLudo = LudoEngine.moveToken(ludo, tokenId)
    val isFinished = updatedLudo.winnerId != null
    val newStatus = if (isFinished) GameStatus.FINISHED else GameStatus.ACTIVE
    val endedAt = if (isFinished) System.currentTimeMillis() else null

    if (isFinished && updatedLudo.winnerId != null) {
      for (p in updatedLudo.players) {
        updateStats(p.userId, GameType.LUDO, won = p.userId == updatedLudo.winnerId)
      }
    }

    sessions[matchId] = session.copy(
      ludoState = updatedLudo,
      status = newStatus,
      endedAtMs = endedAt,
      winnerId = updatedLudo.winnerId,
      lastHeartbeatMs = System.currentTimeMillis()
    )
    publishSessions()
    return true
  }

  fun requestRematch(matchId: String, userId: String) {
    val session = sessions[matchId] ?: return
    when (session.gameType) {
      GameType.CHESS -> {
        val chess = session.chessState ?: return
        val updatedRequests = chess.rematchRequestedBy + userId
        if (updatedRequests.size >= 2) {
          // Switch colors for rematch!
          val newChess = ChessEngine.createInitialState(
            whiteId = chess.blackPlayerId,
            whiteName = chess.blackPlayerName,
            blackId = chess.whitePlayerId,
            blackName = chess.whitePlayerName
          )
          sessions[matchId] = session.copy(
            chessState = newChess,
            status = GameStatus.ACTIVE,
            winnerId = null,
            endedAtMs = null,
            startedAtMs = System.currentTimeMillis()
          )
        } else {
          sessions[matchId] = session.copy(
            chessState = chess.copy(rematchRequestedBy = updatedRequests)
          )
        }
      }
      GameType.LUDO -> {
        val ludo = session.ludoState ?: return
        val updatedRequests = ludo.rematchRequestedBy + userId
        if (updatedRequests.size >= session.players.size) {
          val newLudo = LudoEngine.createInitialState(session.players)
          sessions[matchId] = session.copy(
            ludoState = newLudo,
            status = GameStatus.ACTIVE,
            winnerId = null,
            endedAtMs = null,
            startedAtMs = System.currentTimeMillis()
          )
        } else {
          sessions[matchId] = session.copy(
            ludoState = ludo.copy(rematchRequestedBy = updatedRequests)
          )
        }
      }
    }
    publishSessions()
  }

  fun leaveGame(matchId: String, userId: String) {
    val session = sessions[matchId] ?: return
    if (session.status == GameStatus.ACTIVE) {
      // Resign or forfeit
      when (session.gameType) {
        GameType.CHESS -> resignChess(matchId, userId)
        GameType.LUDO -> {
          // Pass win to next player or end
          val otherPlayer = session.players.firstOrNull { it.userId != userId }
          sessions[matchId] = session.copy(
            status = GameStatus.FINISHED,
            winnerId = otherPlayer?.userId,
            endedAtMs = System.currentTimeMillis()
          )
        }
      }
    } else {
      sessions.remove(matchId)
    }
    publishSessions()
  }

  // -------------------------------------------------------------
  // HEARTBEAT & CONNECTION PRESENCE
  // -------------------------------------------------------------

  fun sendPlayerHeartbeat(matchId: String, userId: String) {
    val session = sessions[matchId] ?: return
    val now = System.currentTimeMillis()
    val updatedPlayers = session.players.map { p ->
      if (p.userId == userId) {
        p.copy(
          connectionStatus = PlayerConnectionStatus.ONLINE,
          lastHeartbeatMs = now
        )
      } else {
        // Calculate status of opponent based on time since heartbeat
        val diff = now - p.lastHeartbeatMs
        val status = when {
          diff < 10000L -> PlayerConnectionStatus.ONLINE
          diff < 20000L -> PlayerConnectionStatus.CONNECTING
          else -> PlayerConnectionStatus.OFFLINE
        }
        p.copy(connectionStatus = status)
      }
    }
    sessions[matchId] = session.copy(
      players = updatedPlayers,
      lastHeartbeatMs = now
    )
    publishSessions()
  }

  private fun publishSessions() {
    _activeSessions.value = sessions.toMap()
  }

  private fun startGameHeartbeatReaper() {
    scope.launch {
      while (true) {
        delay(1000L)
        val now = System.currentTimeMillis()

        // 1. Update Chess clocks for active games
        for ((id, session) in sessions) {
          if (session.status == GameStatus.ACTIVE && session.gameType == GameType.CHESS) {
            session.chessState?.let { chess ->
              if (!chess.isCheckmate && !chess.isDraw) {
                var wTime = chess.whiteTimeSec
                var bTime = chess.blackTimeSec
                var timeFlagWinner: String? = null

                if (chess.activeColor == ChessPieceColor.WHITE) {
                  wTime = (wTime - 1).coerceAtLeast(0)
                  if (wTime == 0) timeFlagWinner = chess.blackPlayerId
                } else {
                  bTime = (bTime - 1).coerceAtLeast(0)
                  if (bTime == 0) timeFlagWinner = chess.whitePlayerId
                }

                if (timeFlagWinner != null) {
                  sessions[id] = session.copy(
                    status = GameStatus.FINISHED,
                    winnerId = timeFlagWinner,
                    chessState = chess.copy(
                      whiteTimeSec = wTime,
                      blackTimeSec = bTime,
                      winnerId = timeFlagWinner,
                      isCheckmate = true
                    ),
                    endedAtMs = now
                  )
                  updateStats(timeFlagWinner, GameType.CHESS, won = true)
                  val loser = if (timeFlagWinner == chess.whitePlayerId) chess.blackPlayerId else chess.whitePlayerId
                  updateStats(loser, GameType.CHESS, won = false)
                } else {
                  sessions[id] = session.copy(
                    chessState = chess.copy(
                      whiteTimeSec = wTime,
                      blackTimeSec = bTime
                    )
                  )
                }
              }
            }
          }
        }

        // 2. Prune matchmaking entries older than 30s
        matchmakingQueue.entries.removeIf { (_, ticket) ->
          now - ticket.queuedAtMs > 30000L
        }

        // 3. Prune closed/expired game sessions
        sessions.entries.removeIf { (_, session) ->
          session.isExpired
        }

        publishSessions()
      }
    }
  }
}
