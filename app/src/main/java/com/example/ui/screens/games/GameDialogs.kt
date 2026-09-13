package com.example.ui.screens.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Friend
import com.example.data.model.games.GameType
import com.example.ui.components.ProfileFrameAvatar
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MatchmakingSearchingDialog(
  gameType: GameType,
  isSearching: Boolean,
  onCancel: () -> Unit,
  onRetry: () -> Unit
) {
  var searchTimeoutReached by remember { mutableStateOf(false) }

  LaunchedEffect(isSearching) {
    if (isSearching) {
      searchTimeoutReached = false
      // Strict rule: Match with real active players only.
      // Search for up to 8 seconds. If no other real player has queued, notify truthfully!
      delay(8000L)
      searchTimeoutReached = true
    }
  }

  AlertDialog(
    onDismissRequest = onCancel,
    containerColor = DarkSurface,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(gameType.icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (searchTimeoutReached) "Matchmaking" else "Searching for Opponent...",
          fontWeight = FontWeight.Bold,
          color = Color.White,
          fontSize = 16.sp
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        if (!searchTimeoutReached) {
          CircularProgressIndicator(
            color = IndigoPrimary,
            modifier = Modifier.size(44.dp)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Finding real active player for ${gameType.displayName}...",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
          )
        } else {
          Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = "No opponent",
            tint = AmberXp,
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "No opponent available right now. Please try again.",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "You can also challenge a friend directly or create a private game code!",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
          )
        }
      }
    },
    confirmButton = {
      if (searchTimeoutReached) {
        Button(
          onClick = {
            searchTimeoutReached = false
            onRetry()
          },
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
        ) {
          Text("Try Again")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onCancel) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}

@Composable
fun GameInviteFriendDialog(
  gameType: GameType,
  friends: List<Friend>,
  onInviteFriend: (Friend) -> Unit,
  onCreatePrivateCode: () -> Unit,
  onDismiss: () -> Unit
) {
  val onlineFriends = friends.filter { it.isOnline }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurface,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(gameType.icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Play ${gameType.displayName} with Friend", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Option 1: Create Private Code
        Surface(
          color = DarkSurfaceElevated,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onCreatePrivateCode() }
            .padding(bottom = 12.dp)
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.VpnKey, contentDescription = "Private Code", tint = IndigoLight)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Generate Private Game Code", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
              Text("Share a 6-letter room code with anyone", fontSize = 11.sp, color = TextSecondary)
            }
          }
        }

        Text("Or challenge an active friend:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))

        if (onlineFriends.isEmpty()) {
          Text(
            text = "No friends are currently online. You can still generate a private game code above!",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(vertical = 12.dp)
          )
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 240.dp)
          ) {
            items(onlineFriends, key = { it.userId }) { friend ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  ProfileFrameAvatar(
                    username = friend.username,
                    frame = "Neon Purple",
                    isOnline = friend.isOnline,
                    size = 36.dp,
                    onClick = {}
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(friend.username, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Text("🟢 Online", fontSize = 10.sp, color = EmeraldOnline)
                  }
                }

                Button(
                  onClick = { onInviteFriend(friend) },
                  colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text("Challenge", fontSize = 11.sp)
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = TextSecondary)
      }
    }
  )
}

@Composable
fun JoinGameCodeDialog(
  onJoinCode: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var code by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurface,
    title = {
      Text("Join Private Game", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Enter the 6-character game code shared by your friend:",
          fontSize = 13.sp,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = code,
          onValueChange = {
            if (it.length <= 6) {
              code = it.uppercase()
              errorText = null
            }
          },
          placeholder = { Text("e.g. X7K92P") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated,
            focusedBorderColor = IndigoPrimary,
            unfocusedBorderColor = DarkSurfaceBorder
          ),
          modifier = Modifier.fillMaxWidth().testTag("input_game_code")
        )

        if (errorText != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(errorText!!, fontSize = 11.sp, color = RoseError)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (code.trim().length == 6) {
            onJoinCode(code.trim())
          } else {
            errorText = "Please enter a valid 6-character code"
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Join Game")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
