package com.example.games.engine

import com.example.data.model.games.*
import kotlin.math.abs

object ChessEngine {

  fun initialBoard(): Map<ChessPosition, ChessPiece> {
    val board = mutableMapOf<ChessPosition, ChessPiece>()

    // Black major pieces (Row 0)
    board[ChessPosition(0, 0)] = ChessPiece(ChessPieceType.ROOK, ChessPieceColor.BLACK)
    board[ChessPosition(0, 1)] = ChessPiece(ChessPieceType.KNIGHT, ChessPieceColor.BLACK)
    board[ChessPosition(0, 2)] = ChessPiece(ChessPieceType.BISHOP, ChessPieceColor.BLACK)
    board[ChessPosition(0, 3)] = ChessPiece(ChessPieceType.QUEEN, ChessPieceColor.BLACK)
    board[ChessPosition(0, 4)] = ChessPiece(ChessPieceType.KING, ChessPieceColor.BLACK)
    board[ChessPosition(0, 5)] = ChessPiece(ChessPieceType.BISHOP, ChessPieceColor.BLACK)
    board[ChessPosition(0, 6)] = ChessPiece(ChessPieceType.KNIGHT, ChessPieceColor.BLACK)
    board[ChessPosition(0, 7)] = ChessPiece(ChessPieceType.ROOK, ChessPieceColor.BLACK)

    // Black pawns (Row 1)
    for (c in 0..7) {
      board[ChessPosition(1, c)] = ChessPiece(ChessPieceType.PAWN, ChessPieceColor.BLACK)
    }

    // White pawns (Row 6)
    for (c in 0..7) {
      board[ChessPosition(6, c)] = ChessPiece(ChessPieceType.PAWN, ChessPieceColor.WHITE)
    }

    // White major pieces (Row 7)
    board[ChessPosition(7, 0)] = ChessPiece(ChessPieceType.ROOK, ChessPieceColor.WHITE)
    board[ChessPosition(7, 1)] = ChessPiece(ChessPieceType.KNIGHT, ChessPieceColor.WHITE)
    board[ChessPosition(7, 2)] = ChessPiece(ChessPieceType.BISHOP, ChessPieceColor.WHITE)
    board[ChessPosition(7, 3)] = ChessPiece(ChessPieceType.QUEEN, ChessPieceColor.WHITE)
    board[ChessPosition(7, 4)] = ChessPiece(ChessPieceType.KING, ChessPieceColor.WHITE)
    board[ChessPosition(7, 5)] = ChessPiece(ChessPieceType.BISHOP, ChessPieceColor.WHITE)
    board[ChessPosition(7, 6)] = ChessPiece(ChessPieceType.KNIGHT, ChessPieceColor.WHITE)
    board[ChessPosition(7, 7)] = ChessPiece(ChessPieceType.ROOK, ChessPieceColor.WHITE)

    return board
  }

  fun createInitialState(
    whiteId: String,
    whiteName: String,
    blackId: String,
    blackName: String
  ): ChessState {
    return ChessState(
      board = initialBoard(),
      activeColor = ChessPieceColor.WHITE,
      whitePlayerId = whiteId,
      whitePlayerName = whiteName,
      blackPlayerId = blackId,
      blackPlayerName = blackName,
      whiteTimeSec = 600,
      blackTimeSec = 600,
      isCheck = false,
      isCheckmate = false,
      isStalemate = false,
      isDraw = false
    )
  }

  /**
   * Returns pseudo-legal destination positions without checking if king is left in check.
   */
  private fun getPseudoLegalMoves(
    board: Map<ChessPosition, ChessPiece>,
    from: ChessPosition
  ): List<ChessPosition> {
    val piece = board[from] ?: return emptyList()
    val moves = mutableListOf<ChessPosition>()
    val color = piece.color

    fun isValid(r: Int, c: Int): Boolean = r in 0..7 && c in 0..7
    fun isOccupied(r: Int, c: Int): Boolean = board.containsKey(ChessPosition(r, c))
    fun isEnemy(r: Int, c: Int): Boolean = board[ChessPosition(r, c)]?.color != color

    when (piece.type) {
      ChessPieceType.PAWN -> {
        val forwardDir = if (color == ChessPieceColor.WHITE) -1 else 1
        val startRow = if (color == ChessPieceColor.WHITE) 6 else 1

        // 1 square forward
        val oneFwd = from.row + forwardDir
        if (isValid(oneFwd, from.col) && !isOccupied(oneFwd, from.col)) {
          moves.add(ChessPosition(oneFwd, from.col))
          // 2 squares forward from start
          val twoFwd = from.row + 2 * forwardDir
          if (from.row == startRow && isValid(twoFwd, from.col) && !isOccupied(twoFwd, from.col)) {
            moves.add(ChessPosition(twoFwd, from.col))
          }
        }

        // Diagonal captures
        for (dc in listOf(-1, 1)) {
          val capC = from.col + dc
          val capR = from.row + forwardDir
          if (isValid(capR, capC) && isOccupied(capR, capC) && isEnemy(capR, capC)) {
            moves.add(ChessPosition(capR, capC))
          }
        }
      }

      ChessPieceType.KNIGHT -> {
        val knightOffsets = listOf(
          Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
          Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        for ((dr, dc) in knightOffsets) {
          val nr = from.row + dr
          val nc = from.col + dc
          if (isValid(nr, nc) && (!isOccupied(nr, nc) || isEnemy(nr, nc))) {
            moves.add(ChessPosition(nr, nc))
          }
        }
      }

      ChessPieceType.BISHOP -> {
        val dirs = listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
        for ((dr, dc) in dirs) {
          var nr = from.row + dr
          var nc = from.col + dc
          while (isValid(nr, nc)) {
            if (!isOccupied(nr, nc)) {
              moves.add(ChessPosition(nr, nc))
            } else {
              if (isEnemy(nr, nc)) moves.add(ChessPosition(nr, nc))
              break
            }
            nr += dr
            nc += dc
          }
        }
      }

      ChessPieceType.ROOK -> {
        val dirs = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
        for ((dr, dc) in dirs) {
          var nr = from.row + dr
          var nc = from.col + dc
          while (isValid(nr, nc)) {
            if (!isOccupied(nr, nc)) {
              moves.add(ChessPosition(nr, nc))
            } else {
              if (isEnemy(nr, nc)) moves.add(ChessPosition(nr, nc))
              break
            }
            nr += dr
            nc += dc
          }
        }
      }

      ChessPieceType.QUEEN -> {
        val dirs = listOf(
          Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1),
          Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
        )
        for ((dr, dc) in dirs) {
          var nr = from.row + dr
          var nc = from.col + dc
          while (isValid(nr, nc)) {
            if (!isOccupied(nr, nc)) {
              moves.add(ChessPosition(nr, nc))
            } else {
              if (isEnemy(nr, nc)) moves.add(ChessPosition(nr, nc))
              break
            }
            nr += dr
            nc += dc
          }
        }
      }

      ChessPieceType.KING -> {
        for (dr in -1..1) {
          for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = from.row + dr
            val nc = from.col + dc
            if (isValid(nr, nc) && (!isOccupied(nr, nc) || isEnemy(nr, nc))) {
              moves.add(ChessPosition(nr, nc))
            }
          }
        }
      }
    }

    return moves
  }

  fun isKingInCheck(board: Map<ChessPosition, ChessPiece>, color: ChessPieceColor): Boolean {
    // Find king position
    val kingPos = board.entries.firstOrNull {
      it.value.type == ChessPieceType.KING && it.value.color == color
    }?.key ?: return false

    val enemyColor = if (color == ChessPieceColor.WHITE) ChessPieceColor.BLACK else ChessPieceColor.WHITE

    // Check if any enemy piece can attack kingPos
    for ((pos, piece) in board) {
      if (piece.color == enemyColor) {
        val attacks = getPseudoLegalMoves(board, pos)
        if (kingPos in attacks) {
          return true
        }
      }
    }
    return false
  }

  /**
   * Filters pseudo-legal moves to only those that do NOT leave the player's king in check.
   */
  fun getLegalMoves(
    board: Map<ChessPosition, ChessPiece>,
    from: ChessPosition,
    activeColor: ChessPieceColor
  ): List<ChessPosition> {
    val piece = board[from] ?: return emptyList()
    if (piece.color != activeColor) return emptyList()

    val pseudoMoves = getPseudoLegalMoves(board, from)
    val legalMoves = mutableListOf<ChessPosition>()

    for (target in pseudoMoves) {
      // Simulate move
      val simBoard = board.toMutableMap()
      simBoard.remove(from)
      simBoard[target] = piece

      // If king not in check, move is legal
      if (!isKingInCheck(simBoard, activeColor)) {
        legalMoves.add(target)
      }
    }

    return legalMoves
  }

  fun getAllLegalMoves(
    board: Map<ChessPosition, ChessPiece>,
    activeColor: ChessPieceColor
  ): List<Pair<ChessPosition, ChessPosition>> {
    val allMoves = mutableListOf<Pair<ChessPosition, ChessPosition>>()
    for ((pos, piece) in board) {
      if (piece.color == activeColor) {
        val targets = getLegalMoves(board, pos, activeColor)
        for (target in targets) {
          allMoves.add(Pair(pos, target))
        }
      }
    }
    return allMoves
  }

  /**
   * Applies move, promotes pawn to Queen if reaching end rank,
   * calculates check, checkmate, stalemate, and switches active turn.
   */
  fun applyMove(
    currentState: ChessState,
    from: ChessPosition,
    to: ChessPosition
  ): ChessState {
    val movingPiece = currentState.board[from] ?: return currentState
    if (movingPiece.color != currentState.activeColor) return currentState

    val legalTargets = getLegalMoves(currentState.board, from, currentState.activeColor)
    if (to !in legalTargets) return currentState

    val newBoard = currentState.board.toMutableMap()
    val capturedPiece = newBoard[to]
    newBoard.remove(from)

    // Check pawn promotion
    val isPromotion = movingPiece.type == ChessPieceType.PAWN &&
        (to.row == 0 || to.row == 7)
    val finalPiece = if (isPromotion) {
      ChessPiece(ChessPieceType.QUEEN, movingPiece.color)
    } else {
      movingPiece
    }

    newBoard[to] = finalPiece

    val nextColor = if (currentState.activeColor == ChessPieceColor.WHITE) {
      ChessPieceColor.BLACK
    } else {
      ChessPieceColor.WHITE
    }

    val isCheck = isKingInCheck(newBoard, nextColor)
    val nextLegalMoves = getAllLegalMoves(newBoard, nextColor)
    val hasNoMoves = nextLegalMoves.isEmpty()

    val isCheckmate = isCheck && hasNoMoves
    val isStalemate = !isCheck && hasNoMoves

    // Insufficient material check (Only 2 kings left)
    val isInsufficientMaterial = newBoard.size == 2 &&
        newBoard.values.all { it.type == ChessPieceType.KING }

    val isDraw = isStalemate || isInsufficientMaterial
    val winnerId = if (isCheckmate) {
      if (currentState.activeColor == ChessPieceColor.WHITE) currentState.whitePlayerId else currentState.blackPlayerId
    } else null

    val moveNotation = "${movingPiece.type.name.first()}${from.algebraic}->${to.algebraic}" +
        if (isCheckmate) "#" else if (isCheck) "+" else ""

    val move = ChessMove(
      from = from,
      to = to,
      piece = finalPiece,
      capturedPiece = capturedPiece,
      isPromotion = isPromotion,
      notation = moveNotation
    )

    return currentState.copy(
      board = newBoard,
      activeColor = nextColor,
      isCheck = isCheck,
      isCheckmate = isCheckmate,
      isStalemate = isStalemate,
      isDraw = isDraw,
      winnerId = winnerId,
      lastMove = move,
      moveHistory = currentState.moveHistory + move
    )
  }
}
