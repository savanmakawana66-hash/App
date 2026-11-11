package com.example.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.model.games.GameType
import com.example.ui.theme.*

@Composable
fun GamesHomeScreen(
  currentUser: User,
  onPlayRandom: (GameType) -> Unit,
  onPlayWithFriend: (GameType) -> Unit,
  onPlayVsBot: (GameType) -> Unit = {},
  onJoinByCode: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top App Bar / Title
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "🎮 Games",
              fontSize = 24.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = IndigoPrimary.copy(alpha = 0.15f),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.3f))
            ) {
              Text(
                text = "REAL PLAYERS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Play real-time online multiplayer with friends or active players",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }

        // Enter Code Action
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkCardBorderSubtle, RoundedCornerShape(12.dp))
            .clickable { onJoinByCode() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("btn_enter_game_code")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VpnKey, contentDescription = "Code", tint = CyanAccent, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Join Code", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
          }
        }
      }
    }

    // Player Quick Stats Card
    item {
      Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("♟️ Chess", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text("${currentUser.chessGames} Played • ${currentUser.chessWins} Won", fontSize = 11.sp, color = TextSecondary)
          }

          Box(
            modifier = Modifier
              .width(1.dp)
              .height(32.dp)
              .background(DarkCardBorderSubtle)
          )

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎲 Ludo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text("${currentUser.ludoGames} Played • ${currentUser.ludoWins} Won", fontSize = 11.sp, color = TextSecondary)
          }
        }
      }
    }

    // -------------------------------------------------------------
    // 1. ♟️ CHESS CARD (Subtle Gold/Amber Ambient Glow)
    // -------------------------------------------------------------
    item {
      GameCard(
        title = "♟️ Chess",
        subtitle = "Classic strategy game with 10-min clocks, legal move validation, checks, checkmate & draw offers.",
        gradientColors = listOf(Color(0x28D4AF37), Color(0x106C63FF), DarkSurfaceCard),
        borderColor = Color(0x40D4AF37),
        badge = "Real-Time 1v1",
        playerCountTag = "👥 2 Players",
        onPlayFriend = { onPlayWithFriend(GameType.CHESS) },
        onPlayRandom = { onPlayRandom(GameType.CHESS) },
        onPlayVsBot = { onPlayVsBot(GameType.CHESS) },
        testTagPrefix = "chess"
      )
    }

    // -------------------------------------------------------------
    // 2. 🎲 LUDO CARD (Subtle Purple/Magenta Ambient Glow)
    // -------------------------------------------------------------
    item {
      GameCard(
        title = "🎲 Ludo",
        subtitle = "Online board game with 4 colored tokens, authentic track steps, safe zones, bonus rolls & captures.",
        gradientColors = listOf(Color(0x28C65CFF), Color(0x1035C9FF), DarkSurfaceCard),
        borderColor = Color(0x40C65CFF),
        badge = "Multiplayer",
        playerCountTag = "👥 2-4 Players",
        onPlayFriend = { onPlayWithFriend(GameType.LUDO) },
        onPlayRandom = { onPlayRandom(GameType.LUDO) },
        onPlayVsBot = { onPlayVsBot(GameType.LUDO) },
        testTagPrefix = "ludo"
      )
    }

    // -------------------------------------------------------------
    // COMING SOON SECTION
    // -------------------------------------------------------------
    item {
      Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(DarkSurfaceElevated),
            contentAlignment = Alignment.Center
          ) {
            Text("🚀", fontSize = 18.sp)
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = "More Games Coming Soon",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Carrom, Uno & 8 Ball Pool are in active development.",
              fontSize = 12.sp,
              color = TextSecondary
            )
          }
        }
      }
    }
  }
}

@Composable
private fun GameCard(
  title: String,
  subtitle: String,
  gradientColors: List<Color>,
  borderColor: Color,
  badge: String,
  playerCountTag: String,
  onPlayFriend: () -> Unit,
  onPlayRandom: () -> Unit,
  onPlayVsBot: () -> Unit = {},
  testTagPrefix: String
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("game_card_${testTagPrefix}")
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(Brush.verticalGradient(gradientColors))
        .padding(18.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Player count tag
            Surface(
              color = DarkSurfaceElevated,
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle)
            ) {
              Text(
                text = playerCountTag,
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }

            // Mode badge
            Surface(
              color = IndigoPrimary.copy(alpha = 0.2f),
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f))
            ) {
              Text(
                text = badge,
                fontSize = 10.sp,
                color = CyanAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = subtitle,
          fontSize = 12.sp,
          color = TextSecondary,
          lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Play with Friend
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkCardBorderSubtle, RoundedCornerShape(12.dp))
              .clickable { onPlayFriend() }
              .padding(vertical = 11.dp)
              .testTag("btn_${testTagPrefix}_friend")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Group, contentDescription = "Friend", modifier = Modifier.size(15.dp), tint = CyanAccent)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Friend", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
          }

          // Play Now / Random (Accent Gradient)
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(Brush.horizontalGradient(listOf(IndigoPrimary, IndigoViolet)))
              .clickable { onPlayRandom() }
              .padding(vertical = 11.dp)
              .testTag("btn_${testTagPrefix}_random")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Quick Match", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Play vs Bot (AI)
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x22F59E0B))
            .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { onPlayVsBot() }
            .padding(vertical = 10.dp)
            .testTag("btn_${testTagPrefix}_vs_bot")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🤖", fontSize = 15.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Play vs Bot [AI]", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
          }
        }
      }
    }
  }
}

