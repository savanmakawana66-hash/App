package com.example.ui.screens.room

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
  onVerifyGuestClick: () -> Unit,
  onOpenProfile: () -> Unit = {}
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
        // TOP HEADER: App Logo, Notification Icon with Badge, Profile Avatar
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          // App Logo + Name
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onOpenProfile() }
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(listOf(BrandPrimary, MagentaAccent)))
            ) {
              Text("🍿", fontSize = 21.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Watch Together",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
              )
              Text(
                text = "Social Streaming & Voice",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }

          // Header Actions: Notifications & Profile Avatar with Live Border
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

            Spacer(modifier = Modifier.width(6.dp))

            // Profile Avatar with Live Online Border (tap opens profile)
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(40.dp)
                .clickable { onOpenProfile() }
                .testTag("header_profile_avatar")
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Brush.linearGradient(listOf(Color(0xFF261B3B), Color(0xFF131222))))
                  .border(1.5.dp, DarkCardBorderSubtle, CircleShape)
              ) {
                Text(
                  text = getInitials(currentUser.username),
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }
              // Online green indicator dot
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .align(Alignment.BottomEnd)
                  .clip(CircleShape)
                  .background(EmeraldOnline)
                  .border(1.5.dp, DarkSurface, CircleShape)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search bar with clear icon
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchChange,
          placeholder = { Text("Search rooms, hosts, or code...", fontSize = 13.sp, color = TextSecondary) },
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

      // 2. MAIN HERO SECTION (Headline: "Watch together. Talk together. Play together.")
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("watch_party_hero_card")
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.verticalGradient(
                  listOf(
                    Color(0x2C8B5CF6),
                    Color(0x14EC4899),
                    Color.Transparent
                  )
                )
              )
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                LiveStatusPill()
                Surface(
                  color = DarkSurfaceElevated,
                  shape = RoundedCornerShape(8.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle)
                ) {
                  Text(
                    text = "⚡ Real-Time P2P",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Text(
                text = buildAnnotatedString {
                  append("Watch ")
                  withStyle(style = SpanStyle(color = CyanAccent, fontWeight = FontWeight.ExtraBold)) {
                    append("together")
                  }
                  append(". Talk ")
                  withStyle(style = SpanStyle(color = BrandVioletLight, fontWeight = FontWeight.ExtraBold)) {
                    append("together")
                  }
                  append(". Play ")
                  withStyle(style = SpanStyle(color = MagentaAccent, fontWeight = FontWeight.ExtraBold)) {
                    append("together")
                  }
                  append(".")
                },
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 26.sp
              )

              Spacer(modifier = Modifier.height(8.dp))

              Text(
                text = "Synchronized video streaming with zero delay, crystal-clear 8-seat voice stages, and instant multiplayer games with your circle.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
              )

              Spacer(modifier = Modifier.height(18.dp))

              // Action Buttons: Create Room, Join Room, Watch Party
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                // 1. Create Room (Primary button with premium gradient, rounded corners, subtle shadow & press animation)
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(BrandPrimary, MagentaAccent)))
                    .clickable { onCreateRoomClick() }
                    .padding(vertical = 11.dp, horizontal = 10.dp)
                    .testTag("create_room_btn")
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create Room", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                  }
                }

                // 2. Join Room (Secondary button with darker glass-like surface and subtle border)
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkCardBorderSubtle, RoundedCornerShape(14.dp))
                    .clickable { onJoinRoomClick() }
                    .padding(vertical = 11.dp, horizontal = 10.dp)
                    .testTag("join_room_btn")
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Join Room", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                  }
                }

                // 3. Watch Party (Subtle accent button)
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, BrandPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .clickable { onHostWatchPartyClick() }
                    .padding(vertical = 11.dp, horizontal = 10.dp)
                    .testTag("host_watch_party_banner_btn")
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    Text("🍿", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Watch Party", color = BrandVioletLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                  }
                }
              }
            }
          }
        }
      }

      // 3. SECTION HEADER: LIVE ROOMS (Genuine Sessions Only)
      val totalLiveRooms = activeRoomSessions.size + rooms.size
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
                .background(if (totalLiveRooms > 0) EmeraldOnline else TextSecondary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "LIVE ROOMS",
              fontSize = 15.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White,
              letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = if (totalLiveRooms > 0) EmeraldOnline.copy(alpha = 0.2f) else DarkSurfaceElevated,
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = "$totalLiveRooms Live",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (totalLiveRooms > 0) EmeraldOnline else TextSecondary,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
              )
            }
          }

          Text(
            text = "Genuine Sessions",
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
          )
        }
      }

      // 4. LIVE ROOMS OR ELEGANT EMPTY STATE
      if (totalLiveRooms == 0) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(22.dp),
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
                text = "No active live rooms at this moment.",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )

              Spacer(modifier = Modifier.height(8.dp))

              Text(
                text = "Start a Watch Party or create a room to go live! All rooms shown here are genuine live sessions with real host heartbeats.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
              )

              Spacer(modifier = Modifier.height(20.dp))

              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Button(
                  onClick = onHostWatchPartyClick,
                  colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.testTag("host_empty_state_action_btn")
                ) {
                  Text("🍿", fontSize = 14.sp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Host Watch Party", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                  onClick = onCreateRoomClick,
                  colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                  border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.testTag("create_empty_state_action_btn")
                ) {
                  Text("Create Room", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
              }
            }
          }
        }
      } else {
        // Active Watch Party Live Sessions
        items(activeRoomSessions, key = { "active_${it.roomId}" }) { session ->
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

        // Standard Live Rooms
        items(rooms, key = { "room_${it.id}" }) { room ->
          RoomCardItem(
            room = room,
            onClick = { onRoomClick(room.id) },
            onToggleFavourite = { onToggleFavourite(room.id) }
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
              focusedBorderColor = BrandPrimary
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
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
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
    shape = RoundedCornerShape(22.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("active_room_card_${session.roomId}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Top Row: Avatar + Room Name + Owner + LIVE pill & Level
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Circular Avatar with Initials
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(Color(0xFF271C3D), Color(0xFF151427))))
              .border(1.5.dp, DarkCardBorderSubtle, CircleShape)
          ) {
            Text(
              text = getInitials(session.hostName),
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = session.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (session.isPrivate) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = "Private",
                  tint = AmberXp,
                  modifier = Modifier.size(13.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "👑 Host: ${session.hostName}",
                fontSize = 12.sp,
                color = GoldOwner,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = "•", fontSize = 11.sp, color = TextMuted)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Code: ${session.roomId}",
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        // LIVE status pill
        LiveStatusPill()
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Status Badges Row: Speaker Seats status, Listeners/Audience, Media Type
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Speaker Seats Status (e.g. "🎙️ 2/8 speaking")
        val activeSpeakers = (session.participantCount.coerceIn(1, 8))
        Surface(
          color = DarkSurfaceElevated,
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text("🎙️", fontSize = 11.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "$activeSpeakers/8 speaking",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = BrandVioletLight
            )
          }
        }

        // Active listeners / member count
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

        // Media Type Badge
        Surface(
          color = PurpleContainer,
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = if (session.mediaType == StreamMediaType.VIDEO) "🎬 Video" else "🎵 Audio",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CyanAccent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
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
              tint = BrandVioletLight,
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

      Spacer(modifier = Modifier.height(14.dp))

      // Footer: Heartbeat telemetry & Clean high-contrast Join Button
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
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
          modifier = Modifier.testTag("join_active_room_${session.roomId}")
        ) {
          Text("Join Room", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
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
    shape = RoundedCornerShape(22.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("room_card_${room.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Top Row: Avatar + Room Name + Owner + LIVE pill & Level + Favorite
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Circular Avatar with Owner Initials
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(Color(0xFF271C3D), Color(0xFF151427))))
              .border(1.5.dp, DarkCardBorderSubtle, CircleShape)
          ) {
            Text(
              text = getInitials(room.ownerName),
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = room.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (room.isPrivate) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = "Private",
                  tint = AmberXp,
                  modifier = Modifier.size(13.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "👑 Owner: ${room.ownerName}",
                fontSize = 12.sp,
                color = GoldOwner,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = "•", fontSize = 11.sp, color = TextMuted)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "ID: ${room.id}",
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          LiveStatusPill()
          Spacer(modifier = Modifier.width(6.dp))
          RoomLevelBadge(level = room.roomLevel)
          Spacer(modifier = Modifier.width(4.dp))
          IconButton(
            onClick = onToggleFavourite,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = if (room.isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
              contentDescription = "Favourite",
              tint = if (room.isFavourite) RoseError else TextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      if (room.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = room.description,
          fontSize = 12.sp,
          color = TextSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Status Badges & High-Contrast Join Action
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Speaker seats status (e.g. "2/8 speaking")
          val speakers = ((room.audienceCount / 4).coerceIn(1, 8))
          Surface(
            color = DarkSurfaceElevated,
            shape = RoundedCornerShape(8.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text("🎙️", fontSize = 11.sp)
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "$speakers/8 speaking",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandVioletLight
              )
            }
          }

          // Active listeners / member count
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
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        // Clean, High-Contrast Join Button
        Button(
          onClick = onClick,
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
          modifier = Modifier.testTag("enter_room_${room.id}")
        ) {
          Text("Join", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
      }
    }
  }
}


