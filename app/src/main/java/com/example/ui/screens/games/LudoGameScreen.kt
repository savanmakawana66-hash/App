package com.example.ui.screens.games

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.games.*
import com.example.ui.components.ProfileFrameAvatar
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoGameScreen(
  session: GameSession,
  currentUserId: String,
  onRollDice: () -> Unit,
  onMoveToken: (Int) -> Unit,
  onRequestRematch: () -> Unit,
  onLeaveGame: () -> Unit
) {
  val ludo = session.ludoState ?: return
  val myPlayer = ludo.players.firstOrNull { it.userId == currentUserId }
  val isMyTurn = ludo.currentTurnPlayerId == currentUserId && session.status == GameStatus.ACTIVE

  val infiniteTransition = rememberInfiniteTransition()
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    )
  )

  Scaffold(
    containerColor = DarkBackground,
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎲 Ludo", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = DarkSurfaceElevated,
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = if (session.gameMode == GameMode.PRIVATE) "Code: ${session.privateCode}" else "Multiplayer",
                fontSize = 11.sp,
                color = IndigoLight,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
        },
        actions = {
          IconButton(onClick = onLeaveGame, modifier = Modifier.testTag("btn_leave_ludo")) {
            Icon(Icons.Default.Close, contentDescription = "Leave Game", tint = TextSecondary)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
      )
    },
    bottomBar = {
      Surface(
        color = DarkSurface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          if (session.status == GameStatus.ACTIVE) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              // Action description text
              Text(
                text = ludo.lastActionText,
                fontSize = 12.sp,
                color = if (isMyTurn) IndigoLight else TextSecondary,
                fontWeight = if (isMyTurn) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
              )

              Spacer(modifier = Modifier.width(12.dp))

              // Interactive Dice Roller
              DiceButton(
                diceValue = ludo.diceValue,
                isMyTurn = isMyTurn,
                canRoll = ludo.canRollDice && isMyTurn,
                onRoll = onRollDice
              )
            }
          } else {
            // Game Finished
            val hasRequestedRematch = ludo.rematchRequestedBy.contains(currentUserId)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = onRequestRematch,
                enabled = !hasRequestedRematch,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_ludo_rematch")
              ) {
                Icon(Icons.Default.Refresh, contentDescription = "Rematch", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (hasRequestedRematch) "Rematch Requested..." else "Request Rematch 🎲",
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
      // PLAYERS LIST (WITH REAL-TIME PRESENCE)
      // -------------------------------------------------------------
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        items(ludo.players, key = { it.userId }) { player ->
          val isTurn = player.color == ludo.currentTurnColor && session.status == GameStatus.ACTIVE
          val playerColor = Color(player.color.hex)
          val finishedTokens = player.tokens.count { it.state == LudoTokenState.HOME }
          val sessionPlayer = session.players.firstOrNull { it.userId == player.userId }

          val statusBadge = when (sessionPlayer?.connectionStatus) {
            PlayerConnectionStatus.ONLINE -> "🟢"
            PlayerConnectionStatus.CONNECTING -> "🟡"
            PlayerConnectionStatus.OFFLINE -> "⚫"
            null -> "🟢"
          }

          Surface(
            color = if (isTurn) DarkSurfaceElevated else DarkSurface,
            shape = RoundedCornerShape(10.dp),
            border = if (isTurn) androidx.compose.foundation.BorderStroke(2.dp, playerColor) else null,
            modifier = Modifier.width(125.dp)
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(playerColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (player.userId == currentUserId) "${player.username} (You)" else player.username,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(text = "$statusBadge Online", fontSize = 9.sp, color = TextSecondary)
                Text(
                  text = "🏁 $finishedTokens/4",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (finishedTokens == 4) AmberXp else TextSecondary
                )
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // WINNER BANNER
      // -------------------------------------------------------------
      if (session.status == GameStatus.FINISHED && ludo.winnerName != null) {
        Surface(
          color = Color(0xFF1B4332),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
          Text(
            text = "🏆 Winner: ${ludo.winnerName} finished all 4 tokens!",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldOnline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
          )
        }
      }

      // -------------------------------------------------------------
      // LUDO BOARD
      // -------------------------------------------------------------
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1f)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF1A1E29))
          .border(2.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
      ) {
        val boardWidth = maxWidth
        val cellSize = boardWidth / 15

        // Render 15x15 Ludo Grid
        LudoBoardGrid(
          ludo = ludo,
          currentUserId = currentUserId,
          isMyTurn = isMyTurn,
          pulseScale = pulseScale,
          onTokenClick = { tokenId ->
            if (isMyTurn && tokenId in ludo.movableTokenIds) {
              onMoveToken(tokenId)
            }
          }
        )
      }

      // -------------------------------------------------------------
      // ACTIVE PLAYER TOKEN CONTROLS (IF MULTIPLE TOKENS MOVABLE)
      // -------------------------------------------------------------
      if (isMyTurn && ludo.hasRolledDice && myPlayer != null) {
        Surface(
          color = DarkSurfaceElevated,
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "Choose Token to Move (Rolled ${ludo.diceValue}):",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              myPlayer.tokens.forEach { token ->
                val canMove = token.id in ludo.movableTokenIds
                val stateLabel = when (token.state) {
                  LudoTokenState.BASE -> "Base"
                  LudoTokenState.TRACK -> "Square ${token.trackPosition}"
                  LudoTokenState.HOME_PATH -> "Straight ${token.homeSteps}"
                  LudoTokenState.HOME -> "Finished"
                }

                Button(
                  onClick = { onMoveToken(token.id) },
                  enabled = canMove,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (canMove) Color(myPlayer.color.hex) else DarkSurfaceBorder
                  ),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                      text = "Token ${token.id + 1}",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                    Text(text = stateLabel, fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                  }
                }
              }
            }
          }
        }
      } else {
        Spacer(modifier = Modifier.height(10.dp))
      }
    }
  }
}

@Composable
private fun DiceButton(
  diceValue: Int,
  isMyTurn: Boolean,
  canRoll: Boolean,
  onRoll: () -> Unit
) {
  Button(
    onClick = onRoll,
    enabled = canRoll,
    colors = ButtonDefaults.buttonColors(
      containerColor = if (canRoll) AmberXp else DarkSurfaceElevated
    ),
    shape = RoundedCornerShape(12.dp),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    modifier = Modifier.testTag("btn_ludo_dice")
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = when (diceValue) {
          1 -> "⚀ 1"
          2 -> "⚁ 2"
          3 -> "⚂ 3"
          4 -> "⚃ 4"
          5 -> "⚄ 5"
          6 -> "⚅ 6"
          else -> "🎲 Roll"
        },
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = if (canRoll) Color.Black else Color.White
      )
      if (canRoll) {
        Spacer(modifier = Modifier.width(6.dp))
        Text("(Tap)", fontSize = 11.sp, color = Color.Black.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun BoxWithConstraintsScope.LudoBoardGrid(
  ludo: LudoState,
  currentUserId: String,
  isMyTurn: Boolean,
  pulseScale: Float,
  onTokenClick: (Int) -> Unit
) {
  val cellSize = maxWidth / 15

  // 15x15 Canvas-based layout
  Column(modifier = Modifier.fillMaxSize()) {
    // Row 0..5: Top bases & column
    Row(modifier = Modifier.fillMaxWidth().weight(6f)) {
      // Red Base (Top-Left)
      BaseBox(
        color = Color(LudoColor.RED.hex),
        name = "Red",
        tokens = ludo.players.firstOrNull { it.color == LudoColor.RED }?.tokens ?: emptyList(),
        currentUserId = currentUserId,
        isMyTurn = isMyTurn && ludo.currentTurnColor == LudoColor.RED,
        movableIds = ludo.movableTokenIds,
        pulseScale = pulseScale,
        onTokenClick = onTokenClick,
        modifier = Modifier.weight(6f).fillMaxHeight()
      )

      // Top Path (Green entry)
      Column(modifier = Modifier.weight(3f).fillMaxHeight()) {
        for (r in 0..5) {
          Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            for (c in 0..2) {
              val isGreenHomePath = c == 1 && r > 0
              val isGreenStart = r == 1 && c == 2
              TrackCell(
                bg = if (isGreenHomePath || isGreenStart) Color(LudoColor.GREEN.hex).copy(alpha = 0.85f) else Color(0xFF232A3B),
                isSafe = isGreenStart,
                label = if (isGreenStart) "★" else null,
                modifier = Modifier.weight(1f).fillMaxHeight()
              )
            }
          }
        }
      }

      // Green Base (Top-Right)
      BaseBox(
        color = Color(LudoColor.GREEN.hex),
        name = "Green",
        tokens = ludo.players.firstOrNull { it.color == LudoColor.GREEN }?.tokens ?: emptyList(),
        currentUserId = currentUserId,
        isMyTurn = isMyTurn && ludo.currentTurnColor == LudoColor.GREEN,
        movableIds = ludo.movableTokenIds,
        pulseScale = pulseScale,
        onTokenClick = onTokenClick,
        modifier = Modifier.weight(6f).fillMaxHeight()
      )
    }

    // Row 6..8: Middle Cross (Left Red path, Center Triangle, Right Yellow path)
    Row(modifier = Modifier.fillMaxWidth().weight(3f)) {
      // Left Path (Red)
      Column(modifier = Modifier.weight(6f).fillMaxHeight()) {
        for (r in 0..2) {
          Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            for (c in 0..5) {
              val isRedHomePath = r == 1 && c > 0
              val isRedStart = r == 0 && c == 1
              TrackCell(
                bg = if (isRedHomePath || isRedStart) Color(LudoColor.RED.hex).copy(alpha = 0.85f) else Color(0xFF232A3B),
                isSafe = isRedStart,
                label = if (isRedStart) "★" else null,
                modifier = Modifier.weight(1f).fillMaxHeight()
              )
            }
          }
        }
      }

      // Center Home Triangle
      Box(
        modifier = Modifier
          .weight(3f)
          .fillMaxHeight()
          .background(Color(0xFF131722)),
        contentAlignment = Alignment.Center
      ) {
        Text("HOME\n🏁", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberXp, textAlign = TextAlign.Center)
      }

      // Right Path (Yellow)
      Column(modifier = Modifier.weight(6f).fillMaxHeight()) {
        for (r in 0..2) {
          Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            for (c in 0..5) {
              val isYellowHomePath = r == 1 && c < 5
              val isYellowStart = r == 2 && c == 4
              TrackCell(
                bg = if (isYellowHomePath || isYellowStart) Color(LudoColor.YELLOW.hex).copy(alpha = 0.85f) else Color(0xFF232A3B),
                isSafe = isYellowStart,
                label = if (isYellowStart) "★" else null,
                modifier = Modifier.weight(1f).fillMaxHeight()
              )
            }
          }
        }
      }
    }

    // Row 9..14: Bottom bases & column
    Row(modifier = Modifier.fillMaxWidth().weight(6f)) {
      // Blue Base (Bottom-Left)
      BaseBox(
        color = Color(LudoColor.BLUE.hex),
        name = "Blue",
        tokens = ludo.players.firstOrNull { it.color == LudoColor.BLUE }?.tokens ?: emptyList(),
        currentUserId = currentUserId,
        isMyTurn = isMyTurn && ludo.currentTurnColor == LudoColor.BLUE,
        movableIds = ludo.movableTokenIds,
        pulseScale = pulseScale,
        onTokenClick = onTokenClick,
        modifier = Modifier.weight(6f).fillMaxHeight()
      )

      // Bottom Path (Blue entry)
      Column(modifier = Modifier.weight(3f).fillMaxHeight()) {
        for (r in 0..5) {
          Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            for (c in 0..2) {
              val isBlueHomePath = c == 1 && r < 5
              val isBlueStart = r == 4 && c == 0
              TrackCell(
                bg = if (isBlueHomePath || isBlueStart) Color(LudoColor.BLUE.hex).copy(alpha = 0.85f) else Color(0xFF232A3B),
                isSafe = isBlueStart,
                label = if (isBlueStart) "★" else null,
                modifier = Modifier.weight(1f).fillMaxHeight()
              )
            }
          }
        }
      }

      // Yellow Base (Bottom-Right)
      BaseBox(
        color = Color(LudoColor.YELLOW.hex),
        name = "Yellow",
        tokens = ludo.players.firstOrNull { it.color == LudoColor.YELLOW }?.tokens ?: emptyList(),
        currentUserId = currentUserId,
        isMyTurn = isMyTurn && ludo.currentTurnColor == LudoColor.YELLOW,
        movableIds = ludo.movableTokenIds,
        pulseScale = pulseScale,
        onTokenClick = onTokenClick,
        modifier = Modifier.weight(6f).fillMaxHeight()
      )
    }
  }
}

@Composable
private fun BaseBox(
  color: Color,
  name: String,
  tokens: List<LudoToken>,
  currentUserId: String,
  isMyTurn: Boolean,
  movableIds: List<Int>,
  pulseScale: Float,
  onTokenClick: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .padding(4.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(color.copy(alpha = 0.2f))
      .border(1.5.dp, color, RoundedCornerShape(8.dp)),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(text = name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
      Spacer(modifier = Modifier.height(4.dp))
      // 4 token circles in base
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tokens.take(2).forEach { t ->
          TokenCircle(
            token = t,
            color = color,
            isMovable = isMyTurn && t.id in movableIds,
            pulseScale = pulseScale,
            onClick = { onTokenClick(t.id) }
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tokens.drop(2).take(2).forEach { t ->
          TokenCircle(
            token = t,
            color = color,
            isMovable = isMyTurn && t.id in movableIds,
            pulseScale = pulseScale,
            onClick = { onTokenClick(t.id) }
          )
        }
      }
    }
  }
}

@Composable
private fun TokenCircle(
  token: LudoToken,
  color: Color,
  isMovable: Boolean,
  pulseScale: Float,
  onClick: () -> Unit
) {
  val inBase = token.state == LudoTokenState.BASE
  val inHome = token.state == LudoTokenState.HOME

  Box(
    modifier = Modifier
      .size(24.dp)
      .scale(if (isMovable) pulseScale else 1f)
      .clip(CircleShape)
      .background(
        when {
          inHome -> AmberXp
          inBase -> color
          else -> color.copy(alpha = 0.5f)
        }
      )
      .border(
        width = if (isMovable) 2.dp else 1.dp,
        color = if (isMovable) Color.White else DarkSurfaceBorder,
        shape = CircleShape
      )
      .clickable(enabled = isMovable, onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = if (inHome) "✓" else "${token.id + 1}",
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      color = Color.White
    )
  }
}

@Composable
private fun TrackCell(
  bg: Color,
  isSafe: Boolean,
  label: String?,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .padding(1.dp)
      .clip(RoundedCornerShape(2.dp))
      .background(bg)
      .border(0.5.dp, DarkSurfaceBorder, RoundedCornerShape(2.dp)),
    contentAlignment = Alignment.Center
  ) {
    if (label != null) {
      Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
    }
  }
}
