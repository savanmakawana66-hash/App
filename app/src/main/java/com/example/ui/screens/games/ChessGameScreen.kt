package com.example.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.games.*
import com.example.games.engine.ChessEngine
import com.example.ui.components.ProfileFrameAvatar
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessGameScreen(
  session: GameSession,
  currentUserId: String,
  onMakeMove: (ChessPosition, ChessPosition) -> Unit,
  onResign: () -> Unit,
  onOfferDraw: () -> Unit,
  onRespondDraw: (Boolean) -> Unit,
  onRequestRematch: () -> Unit,
  onLeaveGame: () -> Unit
) {
  val chess = session.chessState ?: return
  val isWhite = currentUserId == chess.whitePlayerId

  val myColor = if (isWhite) ChessPieceColor.WHITE else ChessPieceColor.BLACK
  val isMyTurn = chess.activeColor == myColor && session.status == GameStatus.ACTIVE

  val myPlayer = session.players.firstOrNull { it.userId == currentUserId }
  val opponentPlayer = session.players.firstOrNull { it.userId != currentUserId }

  var selectedPos by remember { mutableStateOf<ChessPosition?>(null) }
  var showResignDialog by remember { mutableStateOf(false) }

  // Calculate legal moves for selected piece
  val legalTargets = remember(selectedPos, chess.board, chess.activeColor) {
    if (selectedPos != null && isMyTurn) {
      ChessEngine.getLegalMoves(chess.board, selectedPos!!, myColor)
    } else {
      emptyList()
    }
  }

  // Determine opponent connection indicator
  val opponentStatus = opponentPlayer?.connectionStatus ?: PlayerConnectionStatus.ONLINE
  val opponentStatusColor = when (opponentStatus) {
    PlayerConnectionStatus.ONLINE -> EmeraldOnline
    PlayerConnectionStatus.CONNECTING -> AmberXp
    PlayerConnectionStatus.OFFLINE -> TextSecondary
  }
  val opponentStatusText = when (opponentStatus) {
    PlayerConnectionStatus.ONLINE -> "🟢 Online"
    PlayerConnectionStatus.CONNECTING -> "🟡 Connecting"
    PlayerConnectionStatus.OFFLINE -> "⚫ Offline"
  }

  Scaffold(
    containerColor = DarkBackground,
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("♟️ Chess", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = DarkSurfaceElevated,
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = if (session.gameMode == GameMode.PRIVATE) "Code: ${session.privateCode}" else "Online Match",
                fontSize = 11.sp,
                color = IndigoLight,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
        },
        actions = {
          IconButton(onClick = onLeaveGame, modifier = Modifier.testTag("btn_leave_chess")) {
            Icon(Icons.Default.Close, contentDescription = "Leave Game", tint = TextSecondary)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
      )
    },
    bottomBar = {
      // In-game actions bar
      Surface(
        color = DarkSurface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (session.status == GameStatus.ACTIVE) {
            Button(
              onClick = { showResignDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = RoseError.copy(alpha = 0.8f)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("btn_chess_resign")
            ) {
              Icon(Icons.Default.Flag, contentDescription = "Resign", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Resign", fontSize = 12.sp)
            }

            Button(
              onClick = onOfferDraw,
              colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
              border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.5f)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("btn_chess_draw")
            ) {
              Icon(Icons.Default.Handshake, contentDescription = "Offer Draw", modifier = Modifier.size(16.dp), tint = IndigoLight)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Offer Draw", fontSize = 12.sp, color = IndigoLight)
            }
          } else {
            // Game is finished
            val hasRequestedRematch = chess.rematchRequestedBy.contains(currentUserId)
            Button(
              onClick = onRequestRematch,
              enabled = !hasRequestedRematch,
              colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("btn_chess_rematch")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Rematch", modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (hasRequestedRematch) "Rematch Requested..." else "Request Rematch ♟️",
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(
              onClick = onLeaveGame,
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Exit", color = TextSecondary)
            }
          }
        }
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // -------------------------------------------------------------
      // TOP PLAYER CARD (OPPONENT)
      // -------------------------------------------------------------
      val opponentName = if (isWhite) chess.blackPlayerName else chess.whitePlayerName
      val opponentTimeSec = if (isWhite) chess.blackTimeSec else chess.whiteTimeSec
      val isOpponentTurn = chess.activeColor != myColor && session.status == GameStatus.ACTIVE

      PlayerHeaderCard(
        username = opponentName.ifBlank { "Opponent" },
        avatar = opponentPlayer?.avatar ?: "avatar_2",
        colorBadge = if (isWhite) "Black (♚)" else "White (♔)",
        timeSec = opponentTimeSec,
        isActiveTurn = isOpponentTurn,
        statusText = opponentStatusText,
        statusColor = opponentStatusColor,
        isMe = false
      )

      // -------------------------------------------------------------
      // GAME STATUS / ANNOUNCEMENT / DRAW BANNER
      // -------------------------------------------------------------
      when {
        chess.isDrawOfferedBy != null && chess.isDrawOfferedBy != currentUserId -> {
          Surface(
            color = PurpleContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("🤝 Opponent offered a draw", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
              Row {
                Button(
                  onClick = { onRespondDraw(true) },
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                  Text("Accept", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedButton(
                  onClick = { onRespondDraw(false) },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                  Text("Decline", fontSize = 11.sp, color = Color.White)
                }
              }
            }
          }
        }
        session.status == GameStatus.FINISHED -> {
          val isWinner = chess.winnerId == currentUserId
          val isDraw = chess.isDraw
          Surface(
            color = if (isDraw) DarkSurfaceElevated else if (isWinner) Color(0xFF1B4332) else Color(0xFF49111C),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
          ) {
            Text(
              text = if (isDraw) "🤝 Game Draw!" else if (isWinner) "🏆 Victory! Checkmate!" else "💀 Defeat! Checkmate.",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = if (isDraw) IndigoLight else if (isWinner) EmeraldOnline else RoseError,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          }
        }
        chess.isCheck -> {
          Surface(
            color = RoseError.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, RoseError),
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
          ) {
            Text(
              text = "⚠️ Check! King is under attack!",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = RoseError,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(vertical = 6.dp)
            )
          }
        }
      }

      // -------------------------------------------------------------
      // 8x8 CHESSBOARD
      // -------------------------------------------------------------
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1f)
          .clip(RoundedCornerShape(8.dp))
          .border(2.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
      ) {
        val squareSize = maxWidth / 8

        Column(modifier = Modifier.fillMaxSize()) {
          for (displayRow in 0..7) {
            // If playing Black, board can be viewed from Black's perspective
            val row = if (isWhite) displayRow else 7 - displayRow
            Row(modifier = Modifier.fillMaxWidth()) {
              for (displayCol in 0..7) {
                val col = if (isWhite) displayCol else 7 - displayCol
                val pos = ChessPosition(row, col)
                val piece = chess.board[pos]
                val isSelected = selectedPos == pos
                val isLegalTarget = pos in legalTargets
                val isLastMove = chess.lastMove?.from == pos || chess.lastMove?.to == pos
                val isKingInCheck = piece?.type == ChessPieceType.KING &&
                    piece.color == chess.activeColor && chess.isCheck

                // Light & dark squares
                val isLightSquare = (row + col) % 2 == 0
                val squareColor = when {
                  isKingInCheck -> Color(0xFFD32F2F).copy(alpha = 0.8f)
                  isSelected -> Color(0xFFF6E05E)
                  isLastMove -> Color(0xFFBACB43).copy(alpha = 0.6f)
                  isLightSquare -> Color(0xFFEEEED2)
                  else -> Color(0xFF769656)
                }

                Box(
                  modifier = Modifier
                    .size(squareSize)
                    .background(squareColor)
                    .clickable(enabled = session.status == GameStatus.ACTIVE) {
                      if (isMyTurn) {
                        if (selectedPos != null && pos in legalTargets) {
                          onMakeMove(selectedPos!!, pos)
                          selectedPos = null
                        } else if (piece?.color == myColor) {
                          selectedPos = pos
                        } else {
                          selectedPos = null
                        }
                      }
                    },
                  contentAlignment = Alignment.Center
                ) {
                  // Coordinate labels
                  if (col == (if (isWhite) 0 else 7)) {
                    Text(
                      text = "${8 - row}",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isLightSquare) Color(0xFF769656) else Color(0xFFEEEED2),
                      modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(1.dp)
                    )
                  }
                  if (row == (if (isWhite) 7 else 0)) {
                    Text(
                      text = "${('a' + col)}",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isLightSquare) Color(0xFF769656) else Color(0xFFEEEED2),
                      modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(1.dp)
                    )
                  }

                  // Piece rendering
                  if (piece != null) {
                    Text(
                      text = piece.symbol,
                      fontSize = 32.sp,
                      fontWeight = FontWeight.Normal,
                      color = if (piece.color == ChessPieceColor.WHITE) Color.White else Color(0xFF1A1A1A),
                      textAlign = TextAlign.Center
                    )
                  }

                  // Legal move indicator dot / capture ring
                  if (isLegalTarget) {
                    if (piece != null) {
                      // Capture ring
                      Box(
                        modifier = Modifier
                          .size(squareSize * 0.85f)
                          .border(3.dp, Color(0xAA1B5E20), CircleShape)
                      )
                    } else {
                      // Dot
                      Box(
                        modifier = Modifier
                          .size(12.dp)
                          .clip(CircleShape)
                          .background(Color(0x881B5E20))
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // BOTTOM PLAYER CARD (YOU)
      // -------------------------------------------------------------
      val myName = if (isWhite) chess.whitePlayerName else chess.blackPlayerName
      val myTimeSec = if (isWhite) chess.whiteTimeSec else chess.blackTimeSec

      PlayerHeaderCard(
        username = "$myName (You)",
        avatar = myPlayer?.avatar ?: "avatar_1",
        colorBadge = if (isWhite) "White (♔)" else "Black (♚)",
        timeSec = myTimeSec,
        isActiveTurn = isMyTurn,
        statusText = "🟢 You",
        statusColor = EmeraldOnline,
        isMe = true
      )
    }
  }

  // Resign Confirmation Dialog
  if (showResignDialog) {
    AlertDialog(
      onDismissRequest = { showResignDialog = false },
      title = { Text("Resign Game?") },
      text = { Text("Are you sure you want to resign? Your opponent will be awarded the victory.") },
      confirmButton = {
        Button(
          onClick = {
            showResignDialog = false
            onResign()
          },
          colors = ButtonDefaults.buttonColors(containerColor = RoseError)
        ) {
          Text("Resign")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResignDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun PlayerHeaderCard(
  username: String,
  avatar: String,
  colorBadge: String,
  timeSec: Int,
  isActiveTurn: Boolean,
  statusText: String,
  statusColor: Color,
  isMe: Boolean
) {
  Surface(
    color = if (isActiveTurn) DarkSurfaceElevated else DarkSurface,
    shape = RoundedCornerShape(12.dp),
    border = if (isActiveTurn) androidx.compose.foundation.BorderStroke(1.5.dp, IndigoLight) else null,
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        ProfileFrameAvatar(
          username = username,
          frame = "Neon Purple",
          isOnline = true,
          size = 38.dp,
          onClick = {}
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = username,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              color = DarkSurfaceBorder,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = colorBadge,
                fontSize = 10.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
          Text(text = statusText, fontSize = 10.sp, color = statusColor)
        }
      }

      // Chess Clock
      val minutes = timeSec / 60
      val seconds = timeSec % 60
      val formattedTime = "%02d:%02d".format(minutes, seconds)

      Surface(
        color = if (isActiveTurn) IndigoPrimary else Color(0xFF181C26),
        shape = RoundedCornerShape(8.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = "Clock",
            tint = if (timeSec < 60) RoseError else Color.White,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = formattedTime,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (timeSec < 60) RoseError else Color.White
          )
        }
      }
    }
  }
}
