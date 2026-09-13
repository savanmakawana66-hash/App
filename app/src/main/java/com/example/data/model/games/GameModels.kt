package com.example.data.model.games

import java.util.UUID

enum class GameType(val displayName: String, val icon: String) {
  CHESS("Chess", "♟️"),
  LUDO("Ludo", "🎲")
}

enum class GameMode {
  FRIEND,
  RANDOM,
  PRIVATE
}

enum class GameStatus {
  WAITING_FOR_PLAYERS,
  ACTIVE,
  FINISHED,
  CANCELLED
}

enum class PlayerConnectionStatus {
  ONLINE,
  CONNECTING,
  OFFLINE
}

data class GamePlayer(
  val userId: String,
  val username: String,
  val avatar: String = "avatar_1",
  val connectionStatus: PlayerConnectionStatus = PlayerConnectionStatus.ONLINE,
  val lastHeartbeatMs: Long = System.currentTimeMillis(),
  val isHost: Boolean = false,
  val colorTag: String = "" // e.g. "WHITE", "BLACK", "RED", "GREEN"
) {
  val isConnected: Boolean
    get() = connectionStatus == PlayerConnectionStatus.ONLINE &&
        (System.currentTimeMillis() - lastHeartbeatMs < 12000L)
}

data class GamePlayerStats(
  val chessGamesPlayed: Int = 0,
  val chessWins: Int = 0,
  val ludoGamesPlayed: Int = 0,
  val ludoWins: Int = 0
)

// -------------------------------------------------------------
// CHESS DATA STRUCTURES
// -------------------------------------------------------------
enum class ChessPieceColor {
  WHITE,
  BLACK
}

enum class ChessPieceType {
  PAWN,
  KNIGHT,
  BISHOP,
  ROOK,
  QUEEN,
  KING
}

data class ChessPiece(
  val type: ChessPieceType,
  val color: ChessPieceColor
) {
  val symbol: String
    get() = when (color) {
      ChessPieceColor.WHITE -> when (type) {
        ChessPieceType.KING -> "♔"
        ChessPieceType.QUEEN -> "♕"
        ChessPieceType.ROOK -> "♖"
        ChessPieceType.BISHOP -> "♗"
        ChessPieceType.KNIGHT -> "♘"
        ChessPieceType.PAWN -> "♙"
      }
      ChessPieceColor.BLACK -> when (type) {
        ChessPieceType.KING -> "♚"
        ChessPieceType.QUEEN -> "♛"
        ChessPieceType.ROOK -> "♜"
        ChessPieceType.BISHOP -> "♝"
        ChessPieceType.KNIGHT -> "♞"
        ChessPieceType.PAWN -> "♟"
      }
    }
}

data class ChessPosition(val row: Int, val col: Int) {
  val algebraic: String
    get() = "${('a' + col)}${8 - row}"
}

data class ChessMove(
  val from: ChessPosition,
  val to: ChessPosition,
  val piece: ChessPiece,
  val capturedPiece: ChessPiece? = null,
  val isPromotion: Boolean = false,
  val isCastling: Boolean = false,
  val notation: String = ""
)

data class ChessState(
  val board: Map<ChessPosition, ChessPiece> = emptyMap(),
  val activeColor: ChessPieceColor = ChessPieceColor.WHITE,
  val whitePlayerId: String = "",
  val whitePlayerName: String = "",
  val blackPlayerId: String = "",
  val blackPlayerName: String = "",
  val whiteTimeSec: Int = 600, // 10 min vs 10 min
  val blackTimeSec: Int = 600,
  val isCheck: Boolean = false,
  val isCheckmate: Boolean = false,
  val isStalemate: Boolean = false,
  val isDrawOfferedBy: String? = null,
  val isDraw: Boolean = false,
  val winnerId: String? = null,
  val resignedPlayerId: String? = null,
  val lastMove: ChessMove? = null,
  val moveHistory: List<ChessMove> = emptyList(),
  val rematchRequestedBy: Set<String> = emptySet()
)

// -------------------------------------------------------------
// LUDO DATA STRUCTURES
// -------------------------------------------------------------
enum class LudoColor(val hex: Long, val displayName: String, val startTrackPos: Int) {
  RED(0xFFE53935, "Red", 0),
  GREEN(0xFF43A047, "Green", 13),
  YELLOW(0xFFFDD835, "Yellow", 26),
  BLUE(0xFF1E88E5, "Blue", 39)
}

enum class LudoTokenState {
  BASE,
  TRACK,
  HOME_PATH,
  HOME
}

data class LudoToken(
  val id: Int, // 0..3
  val color: LudoColor,
  val state: LudoTokenState = LudoTokenState.BASE,
  val trackPosition: Int = -1, // 0..51
  val homeSteps: Int = 0 // 1..6 (6 is HOME)
)

data class LudoPlayer(
  val userId: String,
  val username: String,
  val color: LudoColor,
  val tokens: List<LudoToken> = List(4) { idx -> LudoToken(id = idx, color = color) }
) {
  val hasWon: Boolean
    get() = tokens.all { it.state == LudoTokenState.HOME }
}

data class LudoState(
  val players: List<LudoPlayer> = emptyList(),
  val currentTurnColor: LudoColor = LudoColor.RED,
  val currentTurnPlayerId: String = "",
  val diceValue: Int = 0,
  val hasRolledDice: Boolean = false,
  val consecutiveSixes: Int = 0,
  val canRollDice: Boolean = true,
  val movableTokenIds: List<Int> = emptyList(),
  val winnerId: String? = null,
  val winnerName: String? = null,
  val lastActionText: String = "Game started. Roll dice!",
  val rematchRequestedBy: Set<String> = emptySet()
)

// -------------------------------------------------------------
// GENERAL GAME SESSION
// -------------------------------------------------------------
data class GameSession(
  val matchId: String = UUID.randomUUID().toString(),
  val gameType: GameType,
  val gameMode: GameMode,
  val privateCode: String = "",
  val status: GameStatus = GameStatus.WAITING_FOR_PLAYERS,
  val createdAtMs: Long = System.currentTimeMillis(),
  val startedAtMs: Long? = null,
  val endedAtMs: Long? = null,
  val players: List<GamePlayer> = emptyList(),
  val winnerId: String? = null,
  val chessState: ChessState? = null,
  val ludoState: LudoState? = null,
  val lastHeartbeatMs: Long = System.currentTimeMillis()
) {
  val isExpired: Boolean
    get() = status == GameStatus.CANCELLED ||
        (status == GameStatus.FINISHED && (System.currentTimeMillis() - (endedAtMs ?: createdAtMs)) > 300000L)
}

data class GameInvite(
  val inviteId: String = UUID.randomUUID().toString(),
  val senderId: String,
  val senderName: String,
  val recipientId: String,
  val gameType: GameType,
  val matchId: String,
  val privateCode: String,
  val timestamp: Long = System.currentTimeMillis()
)
