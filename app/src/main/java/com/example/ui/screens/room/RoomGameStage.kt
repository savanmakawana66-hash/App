package com.example.ui.screens.room

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalRoomTheme

object RoomGamePrompts {
  val TRUTHS = listOf(
    "What's your most embarrassing movie theater story?",
    "If you could trade lives with anyone in this room for a day, who?",
    "What song do you secretly know all the lyrics to?",
    "What's the weirdest YouTube rabbit hole you've fallen into?",
    "Which movie or show made you cry the hardest?"
  )

  val DARES = listOf(
    "Sing the chorus of your favorite song on mic right now!",
    "Do your best impression of movie movie trailer voice!",
    "Speak in an accent of the room's choice for 2 minutes!",
    "Send a hilarious gif or reaction to the room chat!",
    "Give a 30-second speech hyping up the room host!"
  )

  val TRIVIA = listOf(
    "Trivia: What was the first feature-length animated movie released? (Answer in chat!)",
    "Trivia: Who played Neo in The Matrix? (Answer in chat!)",
    "Trivia: Which film won the very first Academy Award for Best Picture in 1929?",
    "Trivia: What is the highest-grossing film of all time worldwide?"
  )
}

@Composable
fun RoomGameStage(
  activeGame: String?,
  onCloseGame: () -> Unit,
  modifier: Modifier = Modifier
) {
  val theme = LocalRoomTheme.current
  var diceValue by remember { mutableStateOf((1..6).random()) }
  var diceRolling by remember { mutableStateOf(false) }
  var rollRotation by remember { mutableStateOf(0f) }

  val animatedRotation by animateFloatAsState(
    targetValue = rollRotation,
    animationSpec = tween(durationMillis = 600),
    label = "dice_rotation"
  )

  var currentTruthDareType by remember { mutableStateOf("TRUTH") }
  var currentPrompt by remember { mutableStateOf(RoomGamePrompts.TRUTHS.first()) }
  var currentTrivia by remember { mutableStateOf(RoomGamePrompts.TRIVIA.first()) }

  val gameTitle = when (activeGame) {
    "DICE" -> "🎲 Lucky Dice & Turns"
    "TRUTH_DARE" -> "🎭 Truth or Dare Stage"
    "TRIVIA" -> "🧠 Pop Culture Trivia"
    else -> "🎮 Voice Room Party Game"
  }

  Surface(
    color = theme.surfaceGlass,
    shape = RoundedCornerShape(20.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.4f)),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 6.dp)
      .testTag("room_game_stage")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = theme.primaryAccent.copy(alpha = 0.2f),
            shape = CircleShape,
            modifier = Modifier.size(28.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.SportsEsports,
                contentDescription = null,
                tint = theme.primaryAccent,
                modifier = Modifier.size(16.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = gameTitle,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }

        IconButton(
          onClick = onCloseGame,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close Game", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      when (activeGame) {
        "DICE" -> {
          // Lucky Dice Mode
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(60.dp)
                .rotate(animatedRotation)
                .clip(RoundedCornerShape(16.dp))
                .background(
                  Brush.linearGradient(
                    listOf(theme.primaryAccent, theme.secondaryAccent)
                  )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clickable {
                  diceRolling = true
                  rollRotation += 360f
                  diceValue = (1..6).random()
                }
                .testTag("roll_dice_btn")
            ) {
              Text(
                text = when (diceValue) {
                  1 -> "⚀"
                  2 -> "⚁"
                  3 -> "⚂"
                  4 -> "⚃"
                  5 -> "⚄"
                  else -> "⚅"
                },
                fontSize = 42.sp,
                color = Color.White
              )
            }

            Column(modifier = Modifier.padding(start = 12.dp)) {
              Text(
                text = "Rolled a $diceValue!",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Seat #$diceValue's turn on mic!",
                fontSize = 11.sp,
                color = theme.primaryAccent
              )
              Spacer(modifier = Modifier.height(4.dp))
              Button(
                onClick = {
                  rollRotation += 360f
                  diceValue = (1..6).random()
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("Roll Again 🎲", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        "TRUTH_DARE" -> {
          // Truth or Dare Mode
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              FilterChip(
                selected = currentTruthDareType == "TRUTH",
                onClick = {
                  currentTruthDareType = "TRUTH"
                  currentPrompt = RoomGamePrompts.TRUTHS.random()
                },
                label = { Text("Truth 💬", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = theme.primaryAccent,
                  selectedLabelColor = Color.White
                )
              )
              FilterChip(
                selected = currentTruthDareType == "DARE",
                onClick = {
                  currentTruthDareType = "DARE"
                  currentPrompt = RoomGamePrompts.DARES.random()
                },
                label = { Text("Dare 🔥", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = theme.secondaryAccent,
                  selectedLabelColor = Color.White
                )
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
              color = Color.Black.copy(alpha = 0.35f),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.2f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "\"$currentPrompt\"",
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(12.dp)
              )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
              TextButton(
                onClick = {
                  currentPrompt = if (currentTruthDareType == "TRUTH") {
                    RoomGamePrompts.TRUTHS.random()
                  } else {
                    RoomGamePrompts.DARES.random()
                  }
                }
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = theme.primaryAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Draw New Card", color = theme.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        else -> {
          // Pop culture trivia
          Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
              color = Color.Black.copy(alpha = 0.35f),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.2f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = currentTrivia,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(12.dp)
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
              TextButton(
                onClick = { currentTrivia = RoomGamePrompts.TRIVIA.random() }
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = theme.primaryAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Next Question", color = theme.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
