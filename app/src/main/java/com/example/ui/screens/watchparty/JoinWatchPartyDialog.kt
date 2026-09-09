package com.example.ui.screens.watchparty

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.network.WatchPartyNetworkEngine
import com.example.ui.theme.*

@Composable
fun JoinWatchPartyDialog(
  engine: WatchPartyNetworkEngine,
  onDismiss: () -> Unit,
  onJoinSuccess: () -> Unit
) {
  var roomCode by remember { mutableStateOf("WP-7842") }
  var password by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val activeParties = remember { WatchPartyNetworkEngine.registeredParties.values.toList() }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = DarkSurface,
      border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.5f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .testTag("join_watch_party_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        // Header
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(EmeraldOnline, IndigoPrimary)))
            ) {
              Text("🍿", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Join Watch Party",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Watch along without downloading the file",
                color = TextSecondary,
                fontSize = 11.sp
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Room Code Input
        OutlinedTextField(
          value = roomCode,
          onValueChange = {
            roomCode = it.uppercase()
            errorMessage = null
          },
          label = { Text("Room Code (e.g. WP-7842)", fontSize = 12.sp) },
          leadingIcon = {
            Icon(Icons.Default.VpnKey, contentDescription = null, tint = IndigoLight)
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated,
            focusedBorderColor = IndigoPrimary,
            unfocusedBorderColor = DarkSurfaceBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_room_code_field")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password Input
        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it
            errorMessage = null
          },
          label = { Text("Password (If room is private)", fontSize = 12.sp) },
          leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary)
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated,
            focusedBorderColor = IndigoPrimary,
            unfocusedBorderColor = DarkSurfaceBorder
          ),
          modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = errorMessage!!,
            color = Color(0xFFFF5252),
            fontSize = 12.sp
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Pick from Active Rooms
        if (activeParties.isNotEmpty()) {
          Text(
            text = "Active Live Rooms Available:",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(6.dp))
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            activeParties.take(3).forEach { party ->
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurfaceCard,
                border = BorderStroke(1.dp, if (roomCode == party.code) IndigoPrimary else DarkSurfaceBorder),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    roomCode = party.code
                    password = party.password
                  }
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(10.dp)
                ) {
                  Text("🎬", fontSize = 16.sp)
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "${party.name} (${party.code})",
                      color = Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                    Text(
                      text = "Host: ${party.hostName} • ${party.participants.size} watching",
                      color = TextSecondary,
                      fontSize = 10.sp
                    )
                  }
                  Text(
                    text = "Tap to Fill",
                    color = IndigoLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Join Button
        Button(
          onClick = {
            if (roomCode.isBlank()) {
              errorMessage = "Please enter a valid Room Code"
              return@Button
            }
            val success = engine.joinPartyByCode(roomCode, password)
            if (success) {
              onJoinSuccess()
            } else {
              errorMessage = "Unable to connect. Verify the Room Code and password."
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("join_watch_party_confirm_btn")
        ) {
          Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Color.Black)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Join & Watch Together",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
        }
      }
    }
  }
}
