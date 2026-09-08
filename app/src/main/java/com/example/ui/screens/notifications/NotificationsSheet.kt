package com.example.ui.screens.notifications

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*

@Composable
fun NotificationsSheet(
  notifications: List<AppNotification>,
  onDismiss: () -> Unit,
  onMarkRead: (String) -> Unit,
  onClearAll: () -> Unit,
  onActionClick: (roomId: String?) -> Unit
) {
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
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White)
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Notifications 🔔",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }

        if (notifications.isNotEmpty()) {
          TextButton(onClick = onClearAll) {
            Text("Clear All", color = TextSecondary, fontSize = 12.sp)
          }
        }
      }
    }
  ) { paddingValues ->
    if (notifications.isEmpty()) {
      EmptyStateView(
        icon = Icons.Default.NotificationsNone,
        title = "No Notifications",
        subtitle = "You're all caught up! Room invites and alerts will appear here.",
        modifier = Modifier.padding(paddingValues)
      )
    } else {
      LazyColumn(
        contentPadding = PaddingValues(
          top = paddingValues.calculateTopPadding() + 8.dp,
          bottom = 40.dp,
          start = 16.dp,
          end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(notifications, key = { it.id }) { notif ->
          Card(
            colors = CardDefaults.cardColors(
              containerColor = if (!notif.isRead) Color(0xFF1F1E38) else DarkSurfaceCard
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (!notif.isRead) PurpleAccent.copy(alpha = 0.5f) else DarkSurfaceBorder
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onMarkRead(notif.id)
                if (notif.actionRoomId != null) {
                  onActionClick(notif.actionRoomId)
                }
              }
              .testTag("notification_item_${notif.id}")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(14.dp)
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(if (!notif.isRead) IndigoPrimary else DarkSurfaceElevated)
              ) {
                Text(
                  text = when (notif.type) {
                    NotificationType.ROOM_INVITE -> "🎟️"
                    NotificationType.FRIEND_REQUEST -> "👥"
                    NotificationType.ROOM_LEVEL_UP, NotificationType.ACHIEVEMENT_UNLOCKED -> "⭐"
                    else -> "📢"
                  },
                  fontSize = 18.sp
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = notif.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  Text(
                    text = formatNotifTime(notif.timestamp),
                    fontSize = 10.sp,
                    color = TextSecondary
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = notif.message,
                  fontSize = 12.sp,
                  color = TextSecondary
                )
                if (notif.actionRoomId != null) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "Tap to Join Room 🚀",
                    fontSize = 11.sp,
                    color = IndigoLight,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun formatNotifTime(timeMs: Long): String {
  val diff = System.currentTimeMillis() - timeMs
  return when {
    diff < 60_000L -> "Just now"
    diff < 3600_000L -> "${diff / 60_000L}m ago"
    diff < 86400_000L -> "${diff / 3600_000L}h ago"
    else -> "${diff / 86400_000L}d ago"
  }
}
