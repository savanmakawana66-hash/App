package com.example.ui.screens.room

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.*
import com.example.ui.theme.LocalRoomTheme
import com.example.ui.theme.RoomThemeConstants
import com.example.ui.theme.RoomThemePresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomEditFullDialog(
  room: Room,
  admins: List<RoomAdmin>,
  isOwner: Boolean,
  onDismiss: () -> Unit,
  onSaveFullConfig: (
    name: String,
    desc: String,
    announcement: String,
    mode: VideoControlMode,
    locked: Boolean,
    seatPerm: SeatPermission,
    chatPerm: ChatPermission,
    isVideoEnabled: Boolean,
    isGameEnabled: Boolean,
    activeGameType: String?
  ) -> Unit,
  onOpenThemeEditor: () -> Unit,
  onSelectThemePreset: (themeId: String) -> Unit,
  onAddAdmin: (userId: String, username: String) -> Unit,
  onRemoveAdmin: (userId: String) -> Unit,
  onDeleteRoom: () -> Unit,
  onPickMediaFile: () -> Unit
) {
  val theme = LocalRoomTheme.current
  var selectedTab by remember { mutableStateOf(0) }
  val tabs = listOf("Info 📋", "Features 🎛️", "Theme 🎨", "Permissions 🛡️")

  var name by remember { mutableStateOf(room.name) }
  var description by remember { mutableStateOf(room.description) }
  var announcement by remember { mutableStateOf(room.announcement) }
  var isLocked by remember { mutableStateOf(room.isLocked) }
  var videoControlMode by remember { mutableStateOf(room.videoControlMode) }
  var seatPermission by remember { mutableStateOf(room.seatPermission) }
  var chatPermission by remember { mutableStateOf(room.chatPermission) }

  var isVideoEnabled by remember { mutableStateOf(room.isVideoRoomEnabled) }
  var isGameEnabled by remember { mutableStateOf(room.isGameEnabled) }
  var activeGameType by remember { mutableStateOf(room.activeGameType ?: "DICE") }

  var newAdminId by remember { mutableStateOf("") }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F111E)),
      border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.4f)),
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.92f)
        .testTag("room_edit_full_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        // Title Bar
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚙️", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Room Management & Editing",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "ID: ${room.id} • Room Level ${room.roomLevel}",
                fontSize = 11.sp,
                color = theme.primaryAccent
              )
            }
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = Color(0xFF151829),
          contentColor = theme.primaryAccent,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
        ) {
          tabs.forEachIndexed { index, title ->
            Tab(
              selected = selectedTab == index,
              onClick = { selectedTab = index },
              text = {
                Text(
                  text = title,
                  fontSize = 11.sp,
                  fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                  maxLines = 1
                )
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab Content
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          when (selectedTab) {
            0 -> {
              // TAB 1: ROOM INFO
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState())
              ) {
                OutlinedTextField(
                  value = name,
                  onValueChange = { name = it },
                  label = { Text("Room Name") },
                  singleLine = true,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_room_name_input"),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.primaryAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                  )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                  value = description,
                  onValueChange = { description = it },
                  label = { Text("Room Description / Topic") },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .testTag("edit_room_desc_input"),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.primaryAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                  )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                  value = announcement,
                  onValueChange = { announcement = it },
                  label = { Text("Pinned Room Announcement") },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("edit_room_announcement_input"),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.primaryAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                  )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                  color = Color(0xFF151829),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(14.dp)
                  ) {
                    Column {
                      Text("Lock Room (Password / Private)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                      Text("Prevent new audience members from joining", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                    Switch(checked = isLocked, onCheckedChange = { isLocked = it })
                  }
                }
              }
            }

            1 -> {
              // TAB 2: ROOM FEATURES
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState())
              ) {
                // Voice Room (Always Active)
                Surface(
                  color = Color(0xFF151829),
                  shape = RoundedCornerShape(14.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.3f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(14.dp)
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text("🎙️", fontSize = 20.sp)
                      Spacer(modifier = Modifier.width(10.dp))
                      Column {
                        Text("8 Voice Seats Stage", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("High-fidelity synchronized voice chat", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                      }
                    }
                    Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = theme.primaryAccent)
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Video Room Option
                Surface(
                  color = Color(0xFF151829),
                  shape = RoundedCornerShape(14.dp),
                  border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isVideoEnabled) theme.primaryAccent else Color.White.copy(alpha = 0.1f)
                  ),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎬", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                          Text("Video Room Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                          Text("Synchronized host-controlled video/music stream", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                      }
                      Switch(checked = isVideoEnabled, onCheckedChange = { isVideoEnabled = it })
                    }

                    if (isVideoEnabled) {
                      Spacer(modifier = Modifier.height(10.dp))
                      Divider(color = Color.White.copy(alpha = 0.1f))
                      Spacer(modifier = Modifier.height(10.dp))

                      Text(
                        text = "Current Stream: ${room.activeVideoTitle}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                      )
                      Text(
                        text = "Quality: ${room.videoQuality} • Sync: WebRTC / HTTP Chunk Server",
                        fontSize = 10.sp,
                        color = theme.primaryAccent
                      )

                      Spacer(modifier = Modifier.height(8.dp))

                      Button(
                        onClick = onPickMediaFile,
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Video / Audio from Device", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      }
                      Text(
                        text = "Supports MP4, WebM, MKV, MP3, AAC. Streamed directly to participants without them needing the file.",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 4.dp)
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Party Game Option
                Surface(
                  color = Color(0xFF151829),
                  shape = RoundedCornerShape(14.dp),
                  border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isGameEnabled) theme.primaryAccent else Color.White.copy(alpha = 0.1f)
                  ),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎮", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                          Text("In-Room Party Game", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                          Text("Interactive stage games for speakers & room", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                      }
                      Switch(checked = isGameEnabled, onCheckedChange = { isGameEnabled = it })
                    }

                    if (isGameEnabled) {
                      Spacer(modifier = Modifier.height(8.dp))
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                          selected = activeGameType == "DICE",
                          onClick = { activeGameType = "DICE" },
                          label = { Text("🎲 Lucky Dice", fontSize = 10.sp) },
                          colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.primaryAccent,
                            selectedLabelColor = Color.White
                          )
                        )
                        FilterChip(
                          selected = activeGameType == "TRUTH_DARE",
                          onClick = { activeGameType = "TRUTH_DARE" },
                          label = { Text("🎭 Truth / Dare", fontSize = 10.sp) },
                          colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.primaryAccent,
                            selectedLabelColor = Color.White
                          )
                        )
                        FilterChip(
                          selected = activeGameType == "TRIVIA",
                          onClick = { activeGameType = "TRIVIA" },
                          label = { Text("🧠 Trivia Blitz", fontSize = 10.sp) },
                          colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.primaryAccent,
                            selectedLabelColor = Color.White
                          )
                        )
                      }
                    }
                  }
                }
              }
            }

            2 -> {
              // TAB 3: ROOM THEME
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState())
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column {
                    Text("Voice Room Theme", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Global app theme is fixed; this theme applies to this voice room.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                  }

                  Button(
                    onClick = onOpenThemeEditor,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Live Editor 🎨", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets List
                RoomThemePresets.PRESETS.forEach { preset ->
                  val isUnlocked = preset.isUnlocked(room.roomLevel)
                  val isCurrent = room.themeId == preset.id

                  Surface(
                    color = if (isCurrent) preset.primaryAccent.copy(alpha = 0.18f) else Color(0xFF151829),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                      width = if (isCurrent) 2.dp else 1.dp,
                      color = if (isCurrent) preset.primaryAccent else Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 4.dp)
                      .clickable(enabled = isUnlocked) {
                        onSelectThemePreset(preset.id)
                      }
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.padding(12.dp)
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                          modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                              Brush.linearGradient(listOf(preset.primaryAccent, preset.secondaryAccent))
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                          Text(
                            text = preset.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.4f)
                          )
                          Text(
                            text = preset.subtitle,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f)
                          )
                        }
                      }

                      if (!isUnlocked) {
                        Surface(
                          color = Color(0xFF231810),
                          shape = RoundedCornerShape(8.dp),
                          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                          Text(
                            text = "🔒 Unlocks at LV.${preset.requiredLevel}",
                            fontSize = 10.sp,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                          )
                        }
                      } else if (isCurrent) {
                        Surface(
                          color = preset.primaryAccent,
                          shape = RoundedCornerShape(8.dp)
                        ) {
                          Text(
                            text = "ACTIVE",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                          )
                        }
                      } else {
                        OutlinedButton(
                          onClick = { onSelectThemePreset(preset.id) },
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                          Text("Apply", fontSize = 10.sp)
                        }
                      }
                    }
                  }
                }
              }
            }

            3 -> {
              // TAB 4: PERMISSIONS & ADMINS
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState())
              ) {
                // Seat Permission
                Text("Seat Take Permission", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  FilterChip(
                    selected = seatPermission == SeatPermission.FREE_TO_SIT,
                    onClick = { seatPermission = SeatPermission.FREE_TO_SIT },
                    label = { Text("Free to Sit (Open)", fontSize = 10.sp) }
                  )
                  FilterChip(
                    selected = seatPermission == SeatPermission.REQUIRE_APPROVAL,
                    onClick = { seatPermission = SeatPermission.REQUIRE_APPROVAL },
                    label = { Text("Require Approval", fontSize = 10.sp) }
                  )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chat Permission
                Text("Room Chat Permission", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  FilterChip(
                    selected = chatPermission == ChatPermission.EVERYONE,
                    onClick = { chatPermission = ChatPermission.EVERYONE },
                    label = { Text("Everyone", fontSize = 10.sp) }
                  )
                  FilterChip(
                    selected = chatPermission == ChatPermission.SEATED_ONLY,
                    onClick = { chatPermission = ChatPermission.SEATED_ONLY },
                    label = { Text("Seated Members Only", fontSize = 10.sp) }
                  )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Admin Management
                Text("Room Admins (${admins.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))

                admins.forEach { admin ->
                  Surface(
                    color = Color(0xFF151829),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 2.dp)
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.padding(8.dp)
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(admin.username, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                      }
                      if (isOwner) {
                        IconButton(onClick = { onRemoveAdmin(admin.userId) }, modifier = Modifier.size(24.dp)) {
                          Icon(Icons.Default.Delete, contentDescription = "Revoke Admin", tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
                        }
                      }
                    }
                  }
                }

                if (isOwner) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    OutlinedTextField(
                      value = newAdminId,
                      onValueChange = { newAdminId = it },
                      placeholder = { Text("User ID to appoint as Admin", fontSize = 11.sp) },
                      singleLine = true,
                      modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                      onClick = {
                        if (newAdminId.isNotBlank()) {
                          onAddAdmin(newAdminId.trim(), "Admin_${newAdminId.take(4)}")
                          newAdminId = ""
                        }
                      },
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Text("Add", fontSize = 11.sp)
                    }
                  }
                }

                if (isOwner) {
                  Spacer(modifier = Modifier.height(20.dp))
                  Divider(color = Color.White.copy(alpha = 0.1f))
                  Spacer(modifier = Modifier.height(12.dp))

                  Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Room (Permanent)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Save & Dismiss Bar
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.End,
          modifier = Modifier.fillMaxWidth()
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = {
              onSaveFullConfig(
                name,
                description,
                announcement,
                videoControlMode,
                isLocked,
                seatPermission,
                chatPermission,
                isVideoEnabled,
                isGameEnabled,
                if (isGameEnabled) activeGameType else null
              )
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("save_room_config_btn")
          ) {
            Text("Save Changes ✨", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }
    }
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Delete Permanent Room?") },
      text = { Text("This will permanently remove the room '${room.name}' and all associated history. This action cannot be undone.") },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            onDeleteRoom()
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
        ) {
          Text("Yes, Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
