package com.example.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.screens.room.RoomCardItem
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
  currentUser: User,
  rooms: List<Room>,
  onOpenEditProfile: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenOtpVerification: () -> Unit,
  onRoomClick: (String) -> Unit,
  onToggleFavourite: (String) -> Unit
) {
  val context = LocalContext.current
  var selectedSubTab by remember { mutableStateOf(0) } // 0: My Rooms, 1: Favourites, 2: Badges

  val myRooms = rooms.filter { it.ownerId == currentUser.id }
  val favouriteRooms = rooms.filter { it.isFavourite }

  Scaffold(
    containerColor = DarkBackground,
    topBar = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurface)
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Text(
          text = "My Profile 👤",
          fontSize = 19.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onOpenEditProfile,
            modifier = Modifier.testTag("edit_profile_icon_btn")
          ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White)
          }
          IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.testTag("settings_icon_btn")
          ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = IndigoLight)
          }
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      contentPadding = PaddingValues(
        top = paddingValues.calculateTopPadding() + 8.dp,
        bottom = 100.dp,
        start = 16.dp,
        end = 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // 1. User Header Card
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
          shape = RoundedCornerShape(22.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_card")
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
          ) {
            ProfileFrameAvatar(
              username = currentUser.username,
              frame = currentUser.frameName,
              isOnline = true,
              size = 76.dp,
              onClick = onOpenEditProfile
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = currentUser.username,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              if (currentUser.isGuest) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  color = Color(0xFF332A15),
                  shape = RoundedCornerShape(6.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, AmberXp)
                ) {
                  Text(
                    text = "GUEST",
                    fontSize = 10.sp,
                    color = AmberXp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }

            // Permanent User ID + Copy Button
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clickable {
                  val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  cm.setPrimaryClip(ClipData.newPlainText("User ID", currentUser.id))
                }
                .padding(4.dp)
            ) {
              Text(
                text = "Permanent User ID: ${currentUser.id}",
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy ID",
                tint = IndigoLight,
                modifier = Modifier.size(13.dp)
              )
            }

            // Status Badge
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              color = DarkSurfaceElevated,
              shape = RoundedCornerShape(12.dp)
            ) {
              Text(
                text = "${currentUser.statusEmoji} ${currentUser.statusText}",
                fontSize = 12.sp,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
              )
            }

            if (currentUser.bio.isNotBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = currentUser.bio,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // User Level & XP
            XPProgressBar(
              currentXp = currentUser.xp,
              maxThreshXp = when (currentUser.level) {
                1 -> 1000
                2 -> 2000
                3 -> 4000
                else -> 8000
              }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Stats row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              ProfileStatItem(label = "Watch Hours", value = "${currentUser.watchHours}h")
              ProfileStatItem(label = "Voice Hours", value = "${currentUser.voiceHours}h")
              ProfileStatItem(label = "Level", value = "LVL ${currentUser.level}")
            }
          }
        }
      }

      // 2. Guest Link Mobile Banner (if guest)
      if (currentUser.isGuest) {
        item {
          GuestVerificationReminderBanner(
            onVerifyClick = onOpenOtpVerification
          )
        }
      }

      // 3. Sub-tabs: My Rooms, Favourites, Badges
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TabButton(
            title = "👑 My Rooms (${myRooms.size})",
            isSelected = selectedSubTab == 0,
            onClick = { selectedSubTab = 0 },
            modifier = Modifier.weight(1f)
          )
          TabButton(
            title = "❤️ Favourites (${favouriteRooms.size})",
            isSelected = selectedSubTab == 1,
            onClick = { selectedSubTab = 1 },
            modifier = Modifier.weight(1f)
          )
          TabButton(
            title = "🏅 Badges (${currentUser.badges.size})",
            isSelected = selectedSubTab == 2,
            onClick = { selectedSubTab = 2 },
            modifier = Modifier.weight(1f)
          )
        }
      }

      // 4. Content of selected sub-tab
      when (selectedSubTab) {
        0 -> {
          if (myRooms.isEmpty()) {
            item {
              EmptyStateView(
                icon = Icons.Default.MeetingRoom,
                title = "No Created Rooms",
                subtitle = "You haven't created a permanent room yet. Create one from the ROOM tab!"
              )
            }
          } else {
            items(myRooms, key = { it.id }) { room ->
              RoomCardItem(
                room = room,
                onClick = { onRoomClick(room.id) },
                onToggleFavourite = { onToggleFavourite(room.id) }
              )
            }
          }
        }
        1 -> {
          if (favouriteRooms.isEmpty()) {
            item {
              EmptyStateView(
                icon = Icons.Default.FavoriteBorder,
                title = "No Favourites Yet",
                subtitle = "Tap the heart icon on any room card to add it to your favourites list."
              )
            }
          } else {
            items(favouriteRooms, key = { it.id }) { room ->
              RoomCardItem(
                room = room,
                onClick = { onRoomClick(room.id) },
                onToggleFavourite = { onToggleFavourite(room.id) }
              )
            }
          }
        }
        2 -> {
          // Badges Shelf
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
              shape = RoundedCornerShape(18.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "Unlocked Badges & Trophies 🏆",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                currentUser.badges.forEach { badge ->
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 6.dp)
                  ) {
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                    ) {
                      Text("⭐", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                      Text(badge, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                      Text("Earned for community participation", color = TextSecondary, fontSize = 11.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun TabButton(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = if (isSelected) IndigoPrimary else DarkSurfaceCard,
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isSelected) IndigoPrimary else DarkSurfaceBorder
    ),
    modifier = modifier.clickable { onClick() }
  ) {
    Text(
      text = title,
      color = if (isSelected) Color.White else TextSecondary,
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
    )
  }
}

@Composable
fun ProfileStatItem(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      color = Color.White
    )
    Text(
      text = label,
      fontSize = 11.sp,
      color = TextSecondary
    )
  }
}

@Composable
fun EditProfileDialog(
  currentUser: User,
  onDismiss: () -> Unit,
  onSave: (name: String, bio: String, avatar: String, frame: String, emoji: String, status: String) -> Unit
) {
  var name by remember { mutableStateOf(currentUser.username) }
  var bio by remember { mutableStateOf(currentUser.bio) }
  var selectedFrame by remember { mutableStateOf(currentUser.frameName) }
  var selectedStatusEmoji by remember { mutableStateOf(currentUser.statusEmoji) }
  var statusText by remember { mutableStateOf(currentUser.statusText) }

  val frames = listOf("Default", "Neon Purple", "Cosmic", "Gold VIP")
  val statusEmojis = listOf("🎧", "🎬", "🎮", "💤", "🟢", "✨", "🍿")

  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
      shape = RoundedCornerShape(24.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("edit_profile_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(androidx.compose.foundation.rememberScrollState())
      ) {
        Text("Edit Profile 👤", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Customize your display name, status, and profile frame", fontSize = 12.sp, color = TextSecondary)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Display Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("edit_display_name_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = bio,
          onValueChange = { bio = it },
          label = { Text("Bio / About Me") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text("Status Emoji:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          statusEmojis.forEach { emoji ->
            Surface(
              color = if (selectedStatusEmoji == emoji) IndigoPrimary else DarkSurfaceElevated,
              shape = CircleShape,
              modifier = Modifier.clickable { selectedStatusEmoji = emoji }
            ) {
              Text(emoji, fontSize = 18.sp, modifier = Modifier.padding(8.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = statusText,
          onValueChange = { statusText = it },
          label = { Text("Status Message") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text("Avatar Frame:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(frames) { frame ->
            FilterChip(
              selected = selectedFrame == frame,
              onClick = { selectedFrame = frame },
              label = { Text(frame, fontSize = 11.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
          TextButton(onClick = onDismiss) {
            Text("Cancel", color = TextSecondary)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              onSave(name, bio, currentUser.avatar, selectedFrame, selectedStatusEmoji, statusText)
            },
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("save_profile_btn")
          ) {
            Text("Save Profile", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
