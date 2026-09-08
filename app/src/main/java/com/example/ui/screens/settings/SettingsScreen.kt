package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.User
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
  currentUser: User,
  settings: AppSettings,
  onClose: () -> Unit,
  onUpdateSettings: (AppSettings) -> Unit,
  onOpenOtpVerification: () -> Unit,
  onOpenGlobalAdmin: () -> Unit,
  onLogout: () -> Unit
) {
  var noiseSuppression by remember { mutableStateOf(settings.noiseSuppression) }
  var echoCancellation by remember { mutableStateOf(settings.echoCancellation) }
  var allowDirectMessages by remember { mutableStateOf(settings.allowDirectMessages) }
  var allowRoomInvites by remember { mutableStateOf(settings.allowRoomInvites) }
  var notificationsEnabled by remember { mutableStateOf(settings.notificationsEnabled) }
  var showOnlineStatus by remember { mutableStateOf(settings.showOnlineStatus) }
  var autoSyncVideo by remember { mutableStateOf(settings.autoSyncVideo) }

  var showCacheClearedMessage by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = DarkBackground,
    topBar = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurface)
          .statusBarsPadding()
          .padding(horizontal = 12.dp, vertical = 10.dp)
      ) {
        IconButton(onClick = onClose, modifier = Modifier.testTag("settings_back_btn")) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Settings & Preferences ⚙️",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }
  ) { paddingValues ->
    LazyColumn(
      contentPadding = PaddingValues(
        top = paddingValues.calculateTopPadding() + 8.dp,
        bottom = 40.dp,
        start = 16.dp,
        end = 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // 1. Account Section
      item {
        SettingsSectionHeader("Account & Security 🔒")
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column {
                Text(
                  text = if (currentUser.isGuest) "Guest Account" else "Verified Mobile Account",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = if (currentUser.isGuest) "Temporary ID • 7-10 day grace period" else "Linked to +91 ${currentUser.phoneNumber} (15-day session active)",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
              if (currentUser.isGuest) {
                Button(
                  onClick = onOpenOtpVerification,
                  colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.testTag("settings_verify_btn")
                ) {
                  Text("Verify", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              } else {
                Text("🟢 Active", color = EmeraldOnline, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 2. Audio & Voice Settings
      item {
        SettingsSectionHeader("Audio & Voice Chat 🎙️")
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            SettingsToggleItem(
              title = "AI Noise Suppression",
              subtitle = "Filters background noise and keyboard clicks",
              checked = noiseSuppression,
              onCheckedChange = {
                noiseSuppression = it
                onUpdateSettings(settings.copy(noiseSuppression = it))
              }
            )
            Divider(color = DarkSurfaceBorder, thickness = 0.5.dp)
            SettingsToggleItem(
              title = "Acoustic Echo Cancellation",
              subtitle = "Prevents audio feedback during speaker playback",
              checked = echoCancellation,
              onCheckedChange = {
                echoCancellation = it
                onUpdateSettings(settings.copy(echoCancellation = it))
              }
            )
          }
        }
      }

      // 3. Video & Sync Settings
      item {
        SettingsSectionHeader("Synchronized Video 🎬")
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            SettingsToggleItem(
              title = "Automatic Room Sync",
              subtitle = "Re-synchronizes video stream if delay exceeds 250ms",
              checked = autoSyncVideo,
              onCheckedChange = {
                autoSyncVideo = it
                onUpdateSettings(settings.copy(autoSyncVideo = it))
              }
            )
          }
        }
      }

      // 4. Privacy & Notifications
      item {
        SettingsSectionHeader("Privacy & Notifications 🔔")
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            SettingsToggleItem(
              title = "Show Online Status",
              subtitle = "Allow friends to see when you're online or in a room",
              checked = showOnlineStatus,
              onCheckedChange = {
                showOnlineStatus = it
                onUpdateSettings(settings.copy(showOnlineStatus = it))
              }
            )
            Divider(color = DarkSurfaceBorder, thickness = 0.5.dp)
            SettingsToggleItem(
              title = "Allow Direct Messages",
              subtitle = "Receive direct messages from friends",
              checked = allowDirectMessages,
              onCheckedChange = {
                allowDirectMessages = it
                onUpdateSettings(settings.copy(allowDirectMessages = it))
              }
            )
            Divider(color = DarkSurfaceBorder, thickness = 0.5.dp)
            SettingsToggleItem(
              title = "Allow Room Invitations",
              subtitle = "Receive 1-tap room join invites from friends",
              checked = allowRoomInvites,
              onCheckedChange = {
                allowRoomInvites = it
                onUpdateSettings(settings.copy(allowRoomInvites = it))
              }
            )
            Divider(color = DarkSurfaceBorder, thickness = 0.5.dp)
            SettingsToggleItem(
              title = "Push Notifications",
              subtitle = "Receive alerts for room activities and friend requests",
              checked = notificationsEnabled,
              onCheckedChange = {
                notificationsEnabled = it
                onUpdateSettings(settings.copy(notificationsEnabled = it))
              }
            )
          }
        }
      }

      // 5. Storage & Cache
      item {
        SettingsSectionHeader("Storage & Diagnostics 💾")
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column {
                Text("Cached Media & Thumbnails", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(if (showCacheClearedMessage) "Cache cleared! (0 MB)" else "12.4 MB temporary storage", color = if (showCacheClearedMessage) EmeraldOnline else TextSecondary, fontSize = 11.sp)
              }
              OutlinedButton(
                onClick = { showCacheClearedMessage = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
              ) {
                Text("Clear Cache", fontSize = 11.sp)
              }
            }
          }
        }
      }

      // 6. Global Super-Admin Console
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1738)),
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(16.dp)
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Global Super-Admin Console 🛡️", color = AmberXp, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("User bans, abuse reports moderation queue & system announcements", color = TextSecondary, fontSize = 11.sp)
            }
            Button(
              onClick = onOpenGlobalAdmin,
              colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.testTag("open_admin_panel_btn")
            ) {
              Text("Open", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
        }
      }

      // 7. Session Logout / Reset
      item {
        OutlinedButton(
          onClick = onLogout,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
          border = androidx.compose.foundation.BorderStroke(1.dp, RoseError),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("logout_btn")
        ) {
          Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Reset Guest Session / Logout", fontWeight = FontWeight.Bold)
        }
      }

      // 8. Version footer
      item {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
        ) {
          Text("Watch Together v2.4.0 (Build 2026.09)", color = TextMuted, fontSize = 11.sp)
          Text("Designed with 3-tab modern architecture", color = TextMuted, fontSize = 10.sp)
        }
      }
    }
  }
}

@Composable
fun SettingsSectionHeader(title: String) {
  Text(
    text = title,
    fontSize = 13.sp,
    fontWeight = FontWeight.Bold,
    color = TextSecondary,
    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
  )
}

@Composable
fun SettingsToggleItem(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 10.dp)
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
      Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
    }
    Spacer(modifier = Modifier.width(12.dp))
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoPrimary)
    )
  }
}
