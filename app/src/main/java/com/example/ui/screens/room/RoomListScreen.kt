package com.example.ui.screens.room

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun RoomListScreen(
  currentUser: User,
  activeRoomSessions: List<ActiveRoomSession> = emptyList(),
  rooms: List<Room> = emptyList(),
  searchQuery: String,
  selectedCategory: RoomCategory,
  unreadNotifications: Int,
  onSearchChange: (String) -> Unit,
  onSelectCategory: (RoomCategory) -> Unit,
  onCreateRoomClick: () -> Unit,
  onJoinRoomClick: () -> Unit,
  onHostWatchPartyClick: () -> Unit = {},
  onJoinWatchPartyClick: () -> Unit = {},
  onRoomClick: (String) -> Unit,
  onActiveRoomSessionClick: (ActiveRoomSession) -> Unit = {},
  onToggleFavourite: (String) -> Unit,
  onOpenNotifications: () -> Unit,
  onVerifyGuestClick: () -> Unit
) {
  var passwordTargetSession by remember { mutableStateOf<ActiveRoomSession?>(null) }
  var passwordInput by remember { mutableStateOf("") }
  var passwordError by remember { mutableStateOf<String?>(null) }

  Scaffold(
    containerColor = DarkBackground,
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurface)
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(IndigoPrimary, PurpleAccent)))
            ) {
              Text("🍿", fontSize = 19.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Watch Party",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
              )
              Text(
                text = "Real-Time Synchronized Streaming",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }

          // Top action icons
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onOpenNotifications,
              modifier = Modifier.testTag("notification_btn")
            ) {
              BadgedBox(
                badge = {
                  if (unreadNotifications > 0) {
                    Badge(containerColor = RoseError) {
                      Text("$unreadNotifications", fontSize = 10.sp, color = Color.White)
                    }
                  }
                }
              ) {
                Icon(
                  imageVector = Icons.Default.Notifications,
                  contentDescription = "Notifications",
                  tint = Color.White
                )
              }
            }

            IconButton(
              onClick = onJoinWatchPartyClick,
              modifier = Modifier.testTag("join_by_id_icon_btn")
            ) {
              Icon(
                imageVector = Icons.Default.MeetingRoom,
                contentDescription = "Join by Room Code",
                tint = IndigoLight
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchChange,
          placeholder = { Text("Search active rooms, hosts, or code...", fontSize = 13.sp) },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
          },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { onSearchChange("") }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated,
            focusedBorderColor = IndigoPrimary,
            unfocusedBorderColor = DarkSurfaceBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("room_search_input")
        )
      }
    }
  ) { paddingValues ->
    LazyColumn(
      contentPadding = PaddingValues(
        top = paddingValues.calculateTopPadding() + 10.dp,
        bottom = 100.dp,
        start = 16.dp,
        end = 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // 1. Friendly Guest Verification Banner (If guest)
      if (currentUser.isGuest) {
        item {
          GuestVerificationReminderBanner(
            onVerifyClick = onVerifyGuestClick
          )
        }
      }

      // 2. WATCH PARTY HOST HERO CARD
      item {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = DarkSurfaceCard,
          border = androidx.compose.foundation.BorderStroke(1.dp, Brush.linearGradient(listOf(IndigoPrimary, PurpleAccent))),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("watch_party_hero_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(IndigoPrimary, PurpleAccent)))
                ) {
                  Text("🎬", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = "Host a Watch Party Room",
                      color = Color.White,
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      color = EmeraldOnline.copy(alpha = 0.2f),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = "LIVE",
                        color = EmeraldOnline,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                      )
                    }
                  }
                  Text(
                    text = "Stream local media to remote friends with zero delay",
                    color = TextSecondary,
                    fontSize = 11.sp
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Become a real Host. Pick video/audio from your phone and stream it in real-time. Your room instantly appears in the live directory with your verified heartbeat.",
              color = Color.White.copy(alpha = 0.85f),
              fontSize = 12.sp,
              lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              // Host Button
              Button(
                onClick = onHostWatchPartyClick,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
                  .testTag("host_watch_party_banner_btn")
              ) {
                Text("👑", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Host Party", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }

              // Join Button
              Button(
                onClick = onJoinWatchPartyClick,
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndigoLight.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
                  .testTag("join_watch_party_banner_btn")
              ) {
                Text("🍿", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Join by Code", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }
          }
        }
      }

      // 3. SECTION HEADER: ACTIVE ROOMS (Live Sessions Only)
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (activeRoomSessions.isNotEmpty()) EmeraldOnline else TextSecondary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "ACTIVE ROOMS",
              fontSize = 15.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White,
              letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = if (activeRoomSessions.isNotEmpty()) EmeraldOnline.copy(alpha = 0.2f) else DarkSurfaceElevated,
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = "${activeRoomSessions.size} Live",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (activeRoomSessions.isNotEmpty()) EmeraldOnline else TextSecondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Text(
            text = "Live Host Heartbeat",
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
          )
        }
      }

      // 4. ACTIVE ROOMS LIST OR EMPTY STATE
      if (activeRoomSessions.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("empty_active_rooms_card")
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp, horizontal = 24.dp)
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(64.dp)
                  .clip(CircleShape)
                  .background(DarkSurfaceElevated)
              ) {
                Icon(
                  imageVector = Icons.Default.SensorsOff,
                  contentDescription = null,
                  tint = TextSecondary,
                  modifier = Modifier.size(32.dp)
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                text = "No active rooms right now.",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )

              Spacer(modifier = Modifier.height(8.dp))

              Text(
                text = "A room appears here ONLY when a real Host creates it and maintains an active heartbeat. No fake or placeholder rooms are displayed.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 17.sp
              )

              Spacer(modifier = Modifier.height(20.dp))

              Button(
                onClick = onHostWatchPartyClick,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("host_empty_state_action_btn")
              ) {
                Text("👑", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Host a Watch Party Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
            }
          }
        }
      } else {
        items(activeRoomSessions, key = { it.roomId }) { session ->
          ActiveRoomCardItem(
            session = session,
            onClick = {
              if (session.isPrivate && session.password.isNotBlank()) {
                passwordTargetSession = session
                passwordInput = ""
                passwordError = null
              } else {
                onActiveRoomSessionClick(session)
              }
            }
          )
        }
      }

      // 5. Verified Security Note
      item {
        Surface(
          color = DarkSurfaceElevated.copy(alpha = 0.5f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = EmeraldOnline,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Real-Time Verification: Disconnected or expired rooms are automatically pruned within 10s of lost heartbeat.",
              fontSize = 11.sp,
              color = TextSecondary,
              lineHeight = 15.sp
            )
          }
        }
      }
    }
  }

  // Password Dialog for Private Active Rooms
  if (passwordTargetSession != null) {
    val session = passwordTargetSession!!
    AlertDialog(
      onDismissRequest = { passwordTargetSession = null },
      containerColor = DarkSurfaceCard,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = AmberXp)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Private Room", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column {
          Text(
            text = "'${session.name}' hosted by ${session.hostName} requires a password to join.",
            color = TextSecondary,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = passwordInput,
            onValueChange = {
              passwordInput = it
              passwordError = null
            },
            placeholder = { Text("Enter room password") },
            singleLine = true,
            isError = passwordError != null,
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated,
              focusedBorderColor = IndigoPrimary
            ),
            modifier = Modifier.fillMaxWidth()
          )
          if (passwordError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(passwordError!!, color = RoseError, fontSize = 11.sp)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (passwordInput != session.password) {
              passwordError = "Incorrect room password"
            } else {
              val target = passwordTargetSession
              passwordTargetSession = null
              target?.let { onActiveRoomSessionClick(it) }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
        ) {
          Text("Join")
        }
      },
      dismissButton = {
        TextButton(onClick = { passwordTargetSession = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
fun ActiveRoomCardItem(
  session: ActiveRoomSession,
  onClick: () -> Unit
) {
  val isHostConnected = session.isHostConnected && session.connectionState == PartyConnectionState.CONNECTED
  val isReconnecting = session.connectionState == PartyConnectionState.RECONNECTING

  Card(
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
    shape = RoundedCornerShape(20.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isHostConnected) EmeraldOnline.copy(alpha = 0.4f) else AmberXp.copy(alpha = 0.4f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("active_room_card_${session.roomId}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Top Row: Status badge, Room Name, Privacy tag
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Status indicator dot
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(
                when {
                  isHostConnected -> EmeraldOnline
                  isReconnecting -> AmberXp
                  else -> RoseError
                }
              )
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = session.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        // Privacy indicator
        Surface(
          color = if (session.isPrivate) AmberXp.copy(alpha = 0.15f) else DarkSurfaceElevated,
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
          ) {
            Icon(
              imageVector = if (session.isPrivate) Icons.Default.Lock else Icons.Default.Public,
              contentDescription = null,
              tint = if (session.isPrivate) AmberXp else TextSecondary,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (session.isPrivate) "Private" else "Public",
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (session.isPrivate) AmberXp else TextSecondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Host Name & Room ID
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "👑 Host: ${session.hostName}",
          fontSize = 12.sp,
          color = GoldOwner,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "•", fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Code: ${session.roomId}",
          fontSize = 11.sp,
          color = TextSecondary,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Status Badges Row: Media Type, Participants, Playback State
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Media Type Badge
        Surface(
          color = IndigoDark.copy(alpha = 0.5f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = if (session.mediaType == StreamMediaType.VIDEO) "🎬 Video" else "🎵 Audio",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = IndigoLight
            )
          }
        }

        // Watching Count Badge
        Surface(
          color = DarkSurfaceElevated,
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text("👥", fontSize = 11.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${session.participantCount} watching",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color.White
            )
          }
        }

        // Playback State Badge
        Surface(
          color = if (session.isPlaying) EmeraldOnline.copy(alpha = 0.15f) else AmberXp.copy(alpha = 0.15f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = if (session.isPlaying) "▶ Playing" else "⏸ Paused",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (session.isPlaying) EmeraldOnline else AmberXp
            )
          }
        }
      }

      // Now Playing Title (if present)
      if (session.mediaTitle.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          color = DarkSurfaceElevated,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.PlayCircleFilled,
              contentDescription = null,
              tint = IndigoLight,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = session.mediaTitle,
              fontSize = 11.sp,
              color = TextPrimary,
              fontWeight = FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
            Text(
              text = session.currentQuality.label,
              fontSize = 10.sp,
              color = EmeraldOnline,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Footer: Heartbeat telemetry & Join Button
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Bolt,
              contentDescription = null,
              tint = if (isHostConnected) EmeraldOnline else AmberXp,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isHostConnected) "Verified Active" else "Reconnecting...",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (isHostConnected) EmeraldOnline else AmberXp
            )
          }
          Text(
            text = "Heartbeat: ${session.secondsSinceLastHeartbeat}s ago",
            fontSize = 10.sp,
            color = TextSecondary
          )
        }

        Button(
          onClick = onClick,
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          modifier = Modifier.testTag("join_active_room_${session.roomId}")
        ) {
          Text("Join Room", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun RoomCardItem(
  room: Room,
  onClick: () -> Unit,
  onToggleFavourite: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
    shape = RoundedCornerShape(20.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("room_card_${room.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = room.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.width(6.dp))
          if (room.isPrivate) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Private Room",
              tint = AmberXp,
              modifier = Modifier.size(14.dp)
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          RoomLevelBadge(level = room.roomLevel)
          Spacer(modifier = Modifier.width(6.dp))
          IconButton(
            onClick = onToggleFavourite,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = if (room.isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
              contentDescription = "Favourite",
              tint = if (room.isFavourite) RoseError else TextSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp)
      ) {
        Text(
          text = "👑 Owner: ${room.ownerName}",
          fontSize = 11.sp,
          color = GoldOwner,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "•", fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "ID: ${room.id}",
          fontSize = 11.sp,
          color = TextSecondary,
          fontWeight = FontWeight.Medium
        )
      }

      if (room.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = room.description,
          fontSize = 12.sp,
          color = TextSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Surface(
          color = DarkSurfaceElevated,
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text("👥", fontSize = 11.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${room.audienceCount} members",
              fontSize = 11.sp,
              color = TextSecondary,
              fontWeight = FontWeight.Medium
            )
          }
        }

        Button(
          onClick = onClick,
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          modifier = Modifier.testTag("enter_room_${room.id}")
        ) {
          Text("Enter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

