package com.example.ui.screens.watchparty

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.LocalMediaItem
import com.example.data.model.StreamMediaType
import com.example.data.model.StreamQuality
import com.example.network.WatchPartyNetworkEngine
import com.example.ui.theme.*

@Composable
fun HostWatchPartyDialog(
  engine: WatchPartyNetworkEngine,
  onDismiss: () -> Unit,
  onStartParty: (media: LocalMediaItem, quality: StreamQuality, roomName: String, password: String) -> Unit
) {
  val context = LocalContext.current
  val samplePresets = remember { engine.getSampleMediaPresets() }
  val recentSafItems = remember { mutableStateOf(engine.getRecentSafMedia()) }

  var selectedMedia by remember { mutableStateOf(samplePresets[0]) }
  var roomName by remember { mutableStateOf("My Watch Party 🍿") }
  var password by remember { mutableStateOf("") }
  var selectedQuality by remember { mutableStateOf(StreamQuality.HIGH_ORIGINAL) }
  var isCustomFilePicked by remember { mutableStateOf(false) }

  // Storage Access Framework (SAF) File Picker launcher
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      val resolved = engine.resolveLocalMediaFromUri(uri)
      selectedMedia = resolved
      roomName = "${resolved.title} Watch Party"
      isCustomFilePicked = true
      recentSafItems.value = engine.getRecentSafMedia()
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = DarkSurface,
      border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.5f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .testTag("host_watch_party_dialog")
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
                .background(Brush.linearGradient(listOf(IndigoPrimary, PurpleAccent)))
            ) {
              Text("👑", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Host Watch Party",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Stream local files to friends in real time",
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

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.weight(1f, fill = false)
        ) {
          // 1. SAF Storage Access Framework Section
          item {
            Column(
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              // Privacy & Security Guarantee Banner
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceCard,
                border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(10.dp)
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .size(32.dp)
                      .clip(CircleShape)
                      .background(EmeraldOnline.copy(alpha = 0.15f))
                  ) {
                    Icon(
                      Icons.Default.Security,
                      contentDescription = "SAF Secure",
                      tint = EmeraldOnline,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Storage Access Framework (SAF)",
                      color = Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "Sandboxed direct read access • Zero broad storage permissions required",
                      color = TextSecondary,
                      fontSize = 10.sp
                    )
                  }
                }
              }

              // Selected Media Card (if custom file picked)
              if (isCustomFilePicked) {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = EmeraldOnline.copy(alpha = 0.08f),
                  border = BorderStroke(1.5.dp, EmeraldOnline),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                      ) {
                        Box(
                          contentAlignment = Alignment.Center,
                          modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldOnline.copy(alpha = 0.2f))
                        ) {
                          Icon(
                            imageVector = if (selectedMedia.mediaType == StreamMediaType.AUDIO) Icons.Default.MusicNote else Icons.Default.VideoFile,
                            contentDescription = null,
                            tint = EmeraldOnline,
                            modifier = Modifier.size(20.dp)
                          )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = selectedMedia.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                          )
                          Text(
                            text = "${selectedMedia.formattedDuration} • ${selectedMedia.formattedSize} • ${selectedMedia.resolution}",
                            color = EmeraldOnline,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                          )
                        }
                      }
                      OutlinedButton(
                        onClick = { filePickerLauncher.launch(arrayOf("video/*", "audio/*")) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, EmeraldOnline.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                      ) {
                        Text("Change", fontSize = 11.sp)
                      }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier.padding(top = 2.dp)
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldOnline, modifier = Modifier.size(12.dp))
                          Spacer(modifier = Modifier.width(4.dp))
                          Text(
                            text = if (selectedMedia.isPermissionPersisted) "Persistable SAF Permission Granted" else "SAF Sandboxed Read Active",
                            color = Color.White,
                            fontSize = 9.sp
                          )
                        }
                      }
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier.padding(top = 2.dp)
                      ) {
                        Text(
                          text = selectedMedia.codec,
                          color = TextSecondary,
                          fontSize = 9.sp,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }
                }
              }

              // SAF Launch Action Buttons
              Text(
                text = "Select Media via Storage Access Framework:",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                // Pick Video
                Button(
                  onClick = { filePickerLauncher.launch(arrayOf("video/*")) },
                  colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                  border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.5f)),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("pick_device_video_btn")
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = IndigoLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Video", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                  }
                }

                // Pick Audio
                Button(
                  onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                  colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                  border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("pick_device_audio_btn")
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Audio", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                  }
                }

                // Pick All
                Button(
                  onClick = { filePickerLauncher.launch(arrayOf("video/*", "audio/*")) },
                  colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                  border = BorderStroke(1.dp, DarkSurfaceBorder),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("pick_device_storage_file_btn")
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Browse All", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                  }
                }
              }
            }
          }

          // 2. Recent SAF Items (If previously selected with persisted permissions)
          if (recentSafItems.value.isNotEmpty()) {
            item {
              Text(
                text = "Recently Selected Device Files (SAF Persisted):",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                recentSafItems.value.forEach { item ->
                  val isSelected = selectedMedia.uriString == item.uriString && isCustomFilePicked
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) EmeraldOnline.copy(alpha = 0.15f) else DarkSurfaceCard,
                    border = BorderStroke(1.dp, if (isSelected) EmeraldOnline else DarkSurfaceBorder),
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable {
                        selectedMedia = item
                        roomName = "${item.title} Watch Party"
                        isCustomFilePicked = true
                      }
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(8.dp)
                    ) {
                      Text(item.mediaType.icon, fontSize = 16.sp)
                      Spacer(modifier = Modifier.width(8.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = item.title,
                          color = Color.White,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.SemiBold,
                          maxLines = 1
                        )
                        Text(
                          text = "${item.formattedDuration} • ${item.formattedSize} • ${item.resolution}",
                          color = TextSecondary,
                          fontSize = 9.sp
                        )
                      }
                      if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldOnline, modifier = Modifier.size(16.dp))
                      }
                    }
                  }
                }
              }
            }
          }

          // 3. Or Choose Pre-loaded Media Preset (Videos & 4+ Hour Epics)
          item {
            Text(
              text = "Or Choose Sample Media Preset:",
              color = TextSecondary,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              samplePresets.forEach { item ->
                val isSelected = selectedMedia.title == item.title && !isCustomFilePicked
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = if (isSelected) IndigoDark.copy(alpha = 0.4f) else DarkSurfaceCard,
                  border = BorderStroke(1.dp, if (isSelected) IndigoLight else DarkSurfaceBorder),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      selectedMedia = item
                      roomName = "${item.title} Watch Party"
                      isCustomFilePicked = false
                    }
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                  ) {
                    Text(item.mediaType.icon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                      )
                      Text(
                        text = "${item.formattedDuration} • ${item.resolution} • ${item.formattedSize}",
                        color = TextSecondary,
                        fontSize = 10.sp
                      )
                    }
                    if (isSelected) {
                      Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldOnline, modifier = Modifier.size(18.dp))
                    }
                  }
                }
              }
            }
          }

          // 3. Video Quality Options (Low, Medium, High / Original)
          item {
            Text(
              text = "Stream Quality Setting:",
              color = TextSecondary,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              StreamQuality.values().forEach { q ->
                val isSel = selectedQuality == q
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSel) IndigoPrimary else DarkSurfaceElevated,
                  border = BorderStroke(1.dp, if (isSel) IndigoLight else DarkSurfaceBorder),
                  modifier = Modifier
                    .weight(1f)
                    .clickable { selectedQuality = q }
                    .testTag("quality_option_${q.name}")
                ) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                  ) {
                    Text(
                      text = q.label,
                      color = Color.White,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = q.description,
                      color = if (isSel) Color.White.copy(alpha = 0.8f) else TextSecondary,
                      fontSize = 9.sp,
                      maxLines = 1
                    )
                  }
                }
              }
            }
          }

          // 4. Room Details (Name & Optional Password)
          item {
            OutlinedTextField(
              value = roomName,
              onValueChange = { roomName = it },
              label = { Text("Watch Party Name", fontSize = 12.sp) },
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
          }

          item {
            OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              label = { Text("Room Password (Optional)", fontSize = 12.sp) },
              placeholder = { Text("Leave blank for open invite", fontSize = 12.sp) },
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
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start Watch Party Button
        Button(
          onClick = {
            onStartParty(selectedMedia, selectedQuality, roomName, password)
          },
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("start_watch_party_confirm_btn")
        ) {
          Icon(Icons.Default.PlayCircleFilled, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Start Watch Party",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
        }
      }
    }
  }
}
