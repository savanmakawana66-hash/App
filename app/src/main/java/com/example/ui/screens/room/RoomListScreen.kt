package com.example.ui.screens.room

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
import com.example.data.model.Room
import com.example.data.model.RoomCategory
import com.example.data.model.User
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun RoomListScreen(
  currentUser: User,
  rooms: List<Room>,
  searchQuery: String,
  selectedCategory: RoomCategory,
  unreadNotifications: Int,
  onSearchChange: (String) -> Unit,
  onSelectCategory: (RoomCategory) -> Unit,
  onCreateRoomClick: () -> Unit,
  onJoinRoomClick: () -> Unit,
  onRoomClick: (String) -> Unit,
  onToggleFavourite: (String) -> Unit,
  onOpenNotifications: () -> Unit,
  onVerifyGuestClick: () -> Unit
) {
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
                .size(36.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(IndigoPrimary, PurpleAccent)))
            ) {
              Text("🍿", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Watch Together",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
              )
              Text(
                text = "Synchronized Voice & Video Rooms",
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
              onClick = onJoinRoomClick,
              modifier = Modifier.testTag("join_by_id_icon_btn")
            ) {
              Icon(
                imageVector = Icons.Default.MeetingRoom,
                contentDescription = "Join by ID",
                tint = IndigoLight
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search bar (Room ID, Room Name, User ID, Username)
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchChange,
          placeholder = { Text("Search by Room ID, Name, or User...", fontSize = 13.sp) },
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

      // 2. Quick Action Buttons: Create Room & Join Room
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = onCreateRoomClick,
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("create_room_main_btn")
          ) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create Room", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }

          Button(
            onClick = onJoinRoomClick,
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("join_room_main_btn")
          ) {
            Icon(Icons.Default.Login, contentDescription = null, tint = IndigoLight, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Join by ID", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }

      // 3. Category Filter Chips
      item {
        val categories = listOf(
          RoomCategory.ALL to "All Rooms",
          RoomCategory.ACTIVE to "🔥 Active",
          RoomCategory.POPULAR to "⭐ Popular",
          RoomCategory.NEW to "✨ New",
          RoomCategory.VOICE to "🎙️ Voice Rooms",
          RoomCategory.WATCH_TOGETHER to "🎬 Watch Together",
          RoomCategory.MY_ROOMS to "👑 My Rooms",
          RoomCategory.FAVOURITES to "❤️ Favourites"
        )
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(categories) { (category, label) ->
            val isSelected = selectedCategory == category
            Surface(
              color = if (isSelected) IndigoPrimary else DarkSurfaceCard,
              shape = RoundedCornerShape(10.dp),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) IndigoPrimary else DarkSurfaceBorder
              ),
              modifier = Modifier
                .clickable { onSelectCategory(category) }
                .testTag("filter_chip_${category.name}")
            ) {
              Text(
                text = label,
                color = if (isSelected) Color.White else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
              )
            }
          }
        }
      }

      // 4. Room List Header & Count
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Permanent Rooms (${rooms.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Text(
            text = "Always Available 24/7",
            fontSize = 11.sp,
            color = EmeraldOnline,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      // 5. Room Cards
      if (rooms.isEmpty()) {
        item {
          EmptyStateView(
            icon = Icons.Default.MeetingRoom,
            title = "No Rooms Found",
            subtitle = "No permanent rooms match your search or filter. You can create your own permanent room anytime!",
            actionText = "Create New Room",
            onAction = onCreateRoomClick
          )
        }
      } else {
        items(rooms, key = { it.id }) { room ->
          RoomCardItem(
            room = room,
            onClick = { onRoomClick(room.id) },
            onToggleFavourite = { onToggleFavourite(room.id) }
          )
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
      // Header: Room Name, Level, Favourite Icon
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

      // Owner & Room ID
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
        Text(
          text = "•",
          fontSize = 11.sp,
          color = TextSecondary
        )
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

      // Now Playing Synced Stream
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
            text = "Now Playing: ${room.activeVideoTitle}",
            fontSize = 11.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
          Text(
            text = "⚡ Sync",
            fontSize = 10.sp,
            color = EmeraldOnline,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Room XP progress bar
      XPProgressBar(
        currentXp = room.roomXp,
        maxThreshXp = when (room.roomLevel) {
          1 -> 5000
          2 -> 10000
          3 -> 20000
          else -> 35000
        }
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Footer: 8-Seats status, Audience status, Join Button
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = PurpleContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text("🎙️", fontSize = 11.sp)
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (room.id == "ABC7291") "8/8 Seats (Full)" else "2/8 Seats",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (room.id == "ABC7291") RoseError else Color.White
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

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
                text = "${room.audienceCount} Watching",
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
              )
            }
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
