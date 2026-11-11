package com.example.ui.screens.room

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
  room: Room,
  currentUser: User,
  seats: List<RoomSeat>,
  audience: List<AudienceMember>,
  admins: List<RoomAdmin>,
  chatMessages: List<ChatMessage>,
  onLeaveRoom: () -> Unit,
  onTakeSeat: (Int) -> Unit,
  onLeaveSeat: () -> Unit,
  onRequestSeat: () -> Unit,
  onOpenSeatRequests: () -> Unit,
  onToggleMic: () -> Unit,
  onToggleRaiseHand: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onSeekVideo: (Float) -> Unit,
  onForward10: () -> Unit,
  onRewind10: () -> Unit,
  onChangeVideo: (title: String, durationSec: Int) -> Unit,
  canControlVideo: Boolean,
  onSendChatMessage: (String) -> Unit,
  onAddReaction: (msgId: String, emoji: String) -> Unit,
  onDeleteMessage: (msgId: String) -> Unit,
  onInspectUser: (Friend) -> Unit,
  onOpenRoomSettings: () -> Unit,
  onOpenInvite: () -> Unit,
  onReportMessage: (msgId: String, senderName: String) -> Unit,
  // Enhanced Voice Room actions
  onSendGift: (giftName: String, giftEmoji: String, coinCost: Int) -> Boolean = { _, _, _ -> false },
  onToggleFollowRoom: () -> Unit = {},
  onUpdateVideoAudioSettings: (videoVolume: Float, isMuted: Boolean, micVolume: Float) -> Unit = { _, _, _ -> },
  onUpdateRoomTheme: (themeId: String, customConfig: RoomThemeConfig?) -> Unit = { _, _ -> },
  onUpdateRoomFullConfig: (
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
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
  onSetRoomGame: (gameType: String?, isEnabled: Boolean) -> Unit = { _, _ -> },
  onAddAdmin: (userId: String, username: String) -> Unit = { _, _ -> },
  onRemoveAdmin: (userId: String) -> Unit = {},
  onDeleteRoom: () -> Unit = {},
  onTogglePartnerSeats: (enabled: Boolean) -> Unit = {},
  availableRooms: List<Room> = emptyList(),
  onSwitchRoom: (roomId: String) -> Unit = {}
) {
  val context = LocalContext.current
  val isOwner = room.ownerId == currentUser.id
  val isAdmin = admins.any { it.userId == currentUser.id }
  val canManage = isOwner || isAdmin
  val currentSeat = seats.find { it.userId == currentUser.id }
  val isUserSeated = currentSeat != null
  val hasRequestedSeat = audience.find { it.userId == currentUser.id }?.hasRequestedSeat == true

  var chatText by remember { mutableStateOf("") }
  var showGiftsSheet by remember { mutableStateOf(false) }
  var showThemeEditorDialog by remember { mutableStateOf(false) }
  var showEditRoomDialog by remember { mutableStateOf(false) }
  var showVideoSelectDialog by remember { mutableStateOf(false) }
  var showSwitchRoomSheet by remember { mutableStateOf(false) }
  var isAnnouncementExpanded by remember { mutableStateOf(false) }
  var showAudioSliders by remember { mutableStateOf(false) }

  // Animated gift celebration with auto-removal and queueing
  val giftQueue = remember { mutableStateListOf<Pair<String, String>>() }
  var activeFloatingGift by remember { mutableStateOf<Pair<String, String>?>(null) }
  val giftScale = remember { Animatable(0.2f) }
  val giftAlpha = remember { Animatable(1f) }

  LaunchedEffect(activeFloatingGift) {
    if (activeFloatingGift != null) {
      giftScale.snapTo(0.2f)
      giftAlpha.snapTo(1f)
      launch {
        giftScale.animateTo(
          targetValue = 1.15f,
          animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
        )
      }
      delay(2200L)
      launch {
        giftAlpha.animateTo(0f, animationSpec = tween(350))
      }
      delay(350L)
      activeFloatingGift = null
      if (giftQueue.isNotEmpty()) {
        activeFloatingGift = giftQueue.removeAt(0)
      }
    }
  }

  val chatListState = rememberLazyListState()

  // Video file picker for host streaming
  val mediaPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      val filename = it.lastPathSegment?.substringAfterLast("/") ?: "Selected Media Video"
      onChangeVideo(filename, 1200)
    }
  }

  // Auto-scroll chat
  LaunchedEffect(chatMessages.size) {
    if (chatMessages.isNotEmpty()) {
      chatListState.animateScrollToItem(chatMessages.size - 1)
    }
  }

  ProvideRoomTheme(room.themeId, room.customThemeConfig) {
    val theme = LocalRoomTheme.current

    // Twinkling / Ambient Animation
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_anim")
    val ambientPhase by infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 8000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      ),
      label = "ambient_phase"
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(theme.backgroundBrush)
        .testTag("room_detail_screen")
    ) {
      // 1. Ambient Background Particles / Canvas
      if (theme.ambientParticles) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val starOffsets = listOf(
            Offset(size.width * 0.15f, size.height * 0.12f),
            Offset(size.width * 0.85f, size.height * 0.18f),
            Offset(size.width * 0.50f, size.height * 0.28f),
            Offset(size.width * 0.22f, size.height * 0.45f),
            Offset(size.width * 0.78f, size.height * 0.52f),
            Offset(size.width * 0.12f, size.height * 0.70f),
            Offset(size.width * 0.88f, size.height * 0.78f),
            Offset(size.width * 0.42f, size.height * 0.85f)
          )

          starOffsets.forEachIndexed { index, pos ->
            val phaseOffset = (ambientPhase + (index * 0.15f)) % 1f
            val alpha = (kotlin.math.sin(phaseOffset * Math.PI.toFloat())).coerceIn(0.15f, 0.85f)
            drawCircle(
              color = theme.primaryAccent.copy(alpha = alpha),
              radius = 2.5f + (index % 3),
              center = pos
            )
          }
        }
      }

      // 2. Main Content Column
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
      ) {
        // TOP HEADER
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          // Left: Leave Room & Title Info
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            IconButton(
              onClick = onLeaveRoom,
              modifier = Modifier
                .size(36.dp)
                .testTag("leave_room_btn")
            ) {
              Icon(Icons.Default.ArrowBack, contentDescription = "Exit Room", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = room.name,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Themed Level Badge
                Surface(
                  color = theme.primaryAccent.copy(alpha = 0.25f),
                  shape = RoundedCornerShape(6.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.5f))
                ) {
                  Text(
                    text = "LV.${room.roomLevel}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = theme.primaryAccent,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                  )
                }

                // Room EXP Progress Bar
                val expInLevel = room.roomXp % 1000
                val progress = (expInLevel.toFloat() / 1000f).coerceIn(0f, 1f)
                Spacer(modifier = Modifier.width(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                      .width(44.dp)
                      .height(3.5.dp)
                      .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFFFBBF24),
                    trackColor = Color.White.copy(alpha = 0.15f)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "$expInLevel",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24)
                  )
                }
              }

              // Room ID & Follow Action
              Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                      val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                      cm.setPrimaryClip(ClipData.newPlainText("Room ID", room.id))
                    }
                ) {
                  Text(
                    text = "ID: ${room.id}",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy ID",
                    tint = theme.primaryAccent,
                    modifier = Modifier.size(11.dp)
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Follow Button
                Surface(
                  color = if (room.isFollowed) theme.primaryAccent.copy(alpha = 0.2f) else theme.primaryAccent,
                  shape = RoundedCornerShape(10.dp),
                  border = if (room.isFollowed) androidx.compose.foundation.BorderStroke(1.dp, theme.primaryAccent) else null,
                  modifier = Modifier
                    .clickable { onToggleFollowRoom() }
                    .testTag("follow_room_btn")
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = if (room.isFollowed) "✓ Following" else "+ Follow",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = "${room.followerCount}",
                      fontSize = 9.sp,
                      color = Color.White.copy(alpha = 0.8f)
                    )
                  }
                }
              }
            }
          }

          // Right: Action Buttons (Switch Room, Theme, Edit/Settings, Invite)
          Row(verticalAlignment = Alignment.CenterVertically) {
            // Switch Room Button
            IconButton(
              onClick = { showSwitchRoomSheet = true },
              modifier = Modifier
                .size(34.dp)
                .testTag("btn_switch_room")
            ) {
              Icon(
                Icons.Default.SwapHoriz,
                contentDescription = "Switch Room",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(20.dp)
              )
            }

            // Theme Button (Available to all; editable by room managers)
            IconButton(
              onClick = { showThemeEditorDialog = true },
              modifier = Modifier
                .size(34.dp)
                .testTag("theme_editor_btn")
            ) {
              Icon(
                Icons.Default.Palette,
                contentDescription = if (canManage) "Edit Room Theme" else "View Room Theme",
                tint = theme.primaryAccent,
                modifier = Modifier.size(20.dp)
              )
            }

            if (canManage) {
              // Room Settings / Edit Dialog
              IconButton(
                onClick = { showEditRoomDialog = true },
                modifier = Modifier
                  .size(34.dp)
                  .testTag("room_settings_btn")
              ) {
                Icon(
                  Icons.Default.Settings,
                  contentDescription = "Room Settings",
                  tint = Color.White,
                  modifier = Modifier.size(20.dp)
                )
              }
            }

            // Share / Invite
            IconButton(
              onClick = onOpenInvite,
              modifier = Modifier
                .size(34.dp)
                .testTag("invite_friend_btn")
            ) {
              Icon(
                Icons.Default.PersonAdd,
                contentDescription = "Invite Friends",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        // ROOM ANNOUNCEMENT BANNER
        Surface(
          color = theme.announcementBg,
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, theme.announcementBorder),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clickable { isAnnouncementExpanded = !isAnnouncementExpanded }
            .testTag("room_announcement_banner")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text("📢", fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = room.announcement,
              fontSize = 11.sp,
              color = Color.White,
              maxLines = if (isAnnouncementExpanded) 5 else 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
            Icon(
              if (isAnnouncementExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = null,
              tint = Color.White.copy(alpha = 0.6f),
              modifier = Modifier.size(14.dp)
            )
          }
        }

        // OPTIONAL VIDEO ROOM STAGE
        if (room.isVideoRoomEnabled) {
          Surface(
            color = theme.surfaceGlass,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, theme.surfaceGlassBorder),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 4.dp)
              .testTag("video_room_stage")
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("🎬", fontSize = 14.sp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Column {
                    Text(
                      text = room.activeVideoTitle,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${formatSeconds(room.activeVideoPosSec.toInt())} / ${formatSeconds(room.activeVideoDurationSec)} • ${room.videoQuality}",
                      fontSize = 9.sp,
                      color = theme.primaryAccent
                    )
                  }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  // Audio Sliders Toggle Button
                  IconButton(
                    onClick = { showAudioSliders = !showAudioSliders },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      if (showAudioSliders) Icons.Default.Tune else Icons.Default.VolumeUp,
                      contentDescription = "Audio Controls",
                      tint = if (showAudioSliders) theme.primaryAccent else Color.White,
                      modifier = Modifier.size(16.dp)
                    )
                  }

                  if (canControlVideo) {
                    IconButton(
                      onClick = { showVideoSelectDialog = true },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(Icons.Default.VideoLibrary, contentDescription = "Change Video", tint = theme.primaryAccent, modifier = Modifier.size(16.dp))
                    }
                  }
                }
              }

              // Video Playback Controls (Host / Admin or Everyone based on settings)
              if (canControlVideo) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  IconButton(onClick = onRewind10, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(18.dp))
                  }
                  IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(34.dp)) {
                    Icon(
                      if (room.isVideoPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                      contentDescription = "Play/Pause",
                      tint = theme.primaryAccent,
                      modifier = Modifier.size(26.dp)
                    )
                  }
                  IconButton(onClick = onForward10, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(18.dp))
                  }
                }

                // Scrubber
                val progress = if (room.activeVideoDurationSec > 0) {
                  (room.activeVideoPosSec / room.activeVideoDurationSec).coerceIn(0f, 1f)
                } else 0f
                Slider(
                  value = progress,
                  onValueChange = { posFraction ->
                    onSeekVideo(posFraction * room.activeVideoDurationSec)
                  },
                  colors = SliderDefaults.colors(
                    thumbColor = theme.primaryAccent,
                    activeTrackColor = theme.primaryAccent
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                )
              }

              // SEPARATE AUDIO CONTROLS (Video Volume & Mic Volume)
              if (showAudioSliders) {
                Spacer(modifier = Modifier.height(6.dp))
                Divider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(6.dp))

                // Video Volume Slider
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  IconButton(
                    onClick = {
                      onUpdateVideoAudioSettings(room.videoVolume, !room.isVideoMuted, room.micVolume)
                    },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(
                      if (room.isVideoMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                      contentDescription = "Mute Video",
                      tint = if (room.isVideoMuted) Color(0xFFF43F5E) else Color.White,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                  Text("Video: ${(room.videoVolume * 100).toInt()}%", fontSize = 10.sp, color = Color.White)
                  Slider(
                    value = if (room.isVideoMuted) 0f else room.videoVolume,
                    onValueChange = { vol ->
                      onUpdateVideoAudioSettings(vol, false, room.micVolume)
                    },
                    modifier = Modifier
                      .weight(1f)
                      .height(20.dp),
                    colors = SliderDefaults.colors(thumbColor = theme.secondaryAccent, activeTrackColor = theme.secondaryAccent)
                  )
                }

                // Mic Sensitivity Slider
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Icon(Icons.Default.Mic, contentDescription = null, tint = theme.primaryAccent, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Mic: ${(room.micVolume * 100).toInt()}%", fontSize = 10.sp, color = Color.White)
                  Slider(
                    value = room.micVolume,
                    onValueChange = { vol ->
                      onUpdateVideoAudioSettings(room.videoVolume, room.isVideoMuted, vol)
                    },
                    modifier = Modifier
                      .weight(1f)
                      .height(20.dp),
                    colors = SliderDefaults.colors(thumbColor = theme.primaryAccent, activeTrackColor = theme.primaryAccent)
                  )
                }
              }
            }
          }
        }

        // OPTIONAL IN-ROOM PARTY GAME STAGE
        if (room.isGameEnabled) {
          RoomGameStage(
            activeGame = room.activeGameType ?: "DICE",
            onCloseGame = { onSetRoomGame(null, false) }
          )
        }

        // 8 VOICE SEATS STAGE (2 ROWS OF 4 SEATS)
        Surface(
          color = theme.seatContainerBg,
          shape = RoundedCornerShape(20.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, theme.seatContainerBorder),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("voice_seats_stage")
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 12.dp, horizontal = 8.dp)
          ) {
            // Row 1: Seats 0..3
            Row(
              horizontalArrangement = Arrangement.SpaceAround,
              modifier = Modifier.fillMaxWidth()
            ) {
              for (i in 0..3) {
                val seat = seats.getOrNull(i) ?: RoomSeat(seatIndex = i)
                VoiceSeatItem(
                  seat = seat,
                  isCurrentUser = seat.userId == currentUser.id,
                  theme = theme,
                  onTakeSeat = { onTakeSeat(i) },
                  onInspectUser = {
                    if (seat.userId != null && seat.username != null) {
                      onInspectUser(
                        Friend(
                          userId = seat.userId,
                          username = seat.username,
                          avatar = seat.avatar ?: "avatar_1"
                        )
                      )
                    }
                  }
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 2: Seats 4..7
            Row(
              horizontalArrangement = Arrangement.SpaceAround,
              modifier = Modifier.fillMaxWidth()
            ) {
              for (i in 4..7) {
                val seat = seats.getOrNull(i) ?: RoomSeat(seatIndex = i)
                VoiceSeatItem(
                  seat = seat,
                  isCurrentUser = seat.userId == currentUser.id,
                  theme = theme,
                  onTakeSeat = { onTakeSeat(i) },
                  onInspectUser = {
                    if (seat.userId != null && seat.username != null) {
                      onInspectUser(
                        Friend(
                          userId = seat.userId,
                          username = seat.username,
                          avatar = seat.avatar ?: "avatar_1"
                        )
                      )
                    }
                  }
                )
              }
            }
          }
        }

        // AUDIENCE & SEAT REQUEST STRIP
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👥", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${room.audienceCount} listening",
              fontSize = 11.sp,
              color = Color.White.copy(alpha = 0.7f)
            )
          }

          if (canManage) {
            val pendingRequests = audience.count { it.hasRequestedSeat }
            if (pendingRequests > 0) {
              Surface(
                color = theme.primaryAccent,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .clickable { onOpenSeatRequests() }
                  .testTag("seat_requests_pill")
              ) {
                Text(
                  text = "✋ $pendingRequests Request${if (pendingRequests > 1) "s" else ""}",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          } else if (!isUserSeated) {
            if (room.seatPermission == SeatPermission.REQUIRE_APPROVAL) {
              OutlinedButton(
                onClick = onRequestSeat,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(28.dp)
              ) {
                Text(
                  text = if (hasRequestedSeat) "Request Sent ✋" else "Request to Speak ✋",
                  fontSize = 10.sp,
                  color = theme.primaryAccent
                )
              }
            }
          }
        }

        // ROOM CHAT LIST
        LazyColumn(
          state = chatListState,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .testTag("room_chat_list")
        ) {
          items(chatMessages, key = { it.id }) { msg ->
            ThemedChatMessageItem(
              message = msg,
              currentUserId = currentUser.id,
              canModerate = canManage,
              theme = theme,
              onAddReaction = { emoji -> onAddReaction(msg.id, emoji) },
              onDelete = { onDeleteMessage(msg.id) },
              onReport = { onReportMessage(msg.id, msg.senderName) }
            )
          }
        }

        // QUICK EMOJI BAR & GIFTS BUTTON
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
          // Emoji Pills
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("❤️", "👏", "🔥", "😂", "🎉", "🚀").forEach { emoji ->
              Surface(
                color = Color.Black.copy(alpha = 0.35f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                  .size(30.dp)
                  .clickable {
                    onSendChatMessage(emoji)
                  }
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(emoji, fontSize = 14.sp)
                }
              }
            }
          }

          // Gifts Trigger Button
          Surface(
            color = theme.primaryAccent.copy(alpha = 0.25f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryAccent),
            modifier = Modifier
              .clickable { showGiftsSheet = true }
              .testTag("open_gifts_sheet_btn")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
              Text("🎁", fontSize = 14.sp)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Gifts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primaryAccent)
            }
          }
        }

        // BOTTOM CONTROLS (Chat input, prominent mic, seat leave/request)
        Surface(
          color = Color(0xFF090B14).copy(alpha = 0.9f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 8.dp)
          ) {
            // Chat Input Field
            OutlinedTextField(
              value = chatText,
              onValueChange = { chatText = it },
              placeholder = { Text("Say something...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f)) },
              singleLine = true,
              shape = RoundedCornerShape(20.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("room_chat_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.primaryAccent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              trailingIcon = {
                if (chatText.isNotBlank()) {
                  IconButton(
                    onClick = {
                      onSendChatMessage(chatText.trim())
                      chatText = ""
                    },
                    modifier = Modifier.testTag("send_chat_msg_btn")
                  ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = theme.primaryAccent, modifier = Modifier.size(18.dp))
                  }
                }
              }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // PROMINENT MIC BUTTON
            if (isUserSeated) {
              val isMuted = currentSeat?.isMuted == true
              val isSpeaking = currentSeat?.isSpeaking == true

              val micBg = if (isMuted) Color(0xFFF43F5E) else theme.primaryAccent

              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(micBg)
                  .border(2.dp, if (isSpeaking) Color.White else Color.Transparent, CircleShape)
                  .clickable { onToggleMic() }
                  .testTag("room_mic_toggle_btn")
              ) {
                Icon(
                  imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                  contentDescription = if (isMuted) "Unmute" else "Mute",
                  tint = Color.White,
                  modifier = Modifier.size(24.dp)
                )
              }

              Spacer(modifier = Modifier.width(6.dp))

              // Leave Seat Button
              IconButton(
                onClick = onLeaveSeat,
                modifier = Modifier
                  .size(40.dp)
                  .testTag("leave_seat_btn")
              ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Leave Seat", tint = Color.White.copy(alpha = 0.8f))
              }
            } else {
              // Not seated: Prompt to take seat
              Button(
                onClick = {
                  // Find first empty seat
                  val emptyIdx = seats.indexOfFirst { it.userId == null }
                  if (emptyIdx != -1) {
                    onTakeSeat(emptyIdx)
                  } else {
                    onRequestSeat()
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                  .height(44.dp)
                  .testTag("take_any_seat_btn")
              ) {
                Icon(Icons.Default.Chair, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // FLOATING GIFT CELEBRATION (Auto-removes cleanly with smooth spring animation)
      activeFloatingGift?.let { (name, emoji) ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .scale(giftScale.value)
              .alpha(giftAlpha.value)
              .background(Color(0xE60F172A), RoundedCornerShape(20.dp))
              .border(1.5.dp, Color(0xFFFBBF24).copy(alpha = 0.8f), RoundedCornerShape(20.dp))
              .padding(horizontal = 28.dp, vertical = 20.dp)
          ) {
            Text(text = emoji, fontSize = 68.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = name,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Gift Celebration ✨",
              fontSize = 12.sp,
              color = Color(0xFFFBBF24)
            )
          }
        }
      }
    }

    // DIALOGS & SHEETS
    if (showSwitchRoomSheet) {
      SwitchRoomSheet(
        currentRoomId = room.id,
        availableRooms = availableRooms,
        onDismiss = { showSwitchRoomSheet = false },
        onSelectRoom = { targetRoomId ->
          showSwitchRoomSheet = false
          onSwitchRoom(targetRoomId)
        }
      )
    }

    if (showGiftsSheet) {
      val wallet = com.example.network.security.ServerCoinManager.getWalletState(currentUser.id)
      RoomGiftsSheet(
        userCoins = wallet.balance,
        onDismiss = { showGiftsSheet = false },
        onSendGift = { name, emoji, cost ->
          val success = onSendGift(name, emoji, cost)
          if (success) {
            if (activeFloatingGift == null) {
              activeFloatingGift = name to emoji
            } else {
              giftQueue.add(name to emoji)
            }
          }
          success
        }
      )
    }

    if (showThemeEditorDialog) {
      RoomThemeEditorDialog(
        currentThemeId = room.themeId,
        currentConfig = room.customThemeConfig,
        roomLevel = room.roomLevel,
        canEdit = canManage,
        onDismiss = { showThemeEditorDialog = false },
        onSaveTheme = { themeId, config ->
          onUpdateRoomTheme(themeId, config)
          showThemeEditorDialog = false
        }
      )
    }

    if (showEditRoomDialog) {
      RoomEditFullDialog(
        room = room,
        admins = admins,
        isOwner = isOwner,
        onDismiss = { showEditRoomDialog = false },
        onSaveFullConfig = onUpdateRoomFullConfig,
        onOpenThemeEditor = {
          showEditRoomDialog = false
          showThemeEditorDialog = true
        },
        onSelectThemePreset = { themeId ->
          onUpdateRoomTheme(themeId, null)
        },
        onAddAdmin = onAddAdmin,
        onRemoveAdmin = onRemoveAdmin,
        onDeleteRoom = onDeleteRoom,
        onPickMediaFile = {
          mediaPickerLauncher.launch("video/*")
        }
      )
    }

    if (showVideoSelectDialog) {
      VideoSelectDialog(
        onDismiss = { showVideoSelectDialog = false },
        onSelect = { title, dur ->
          onChangeVideo(title, dur)
          showVideoSelectDialog = false
        }
      )
    }
  }
}

@Composable
fun VoiceSeatItem(
  seat: RoomSeat,
  isCurrentUser: Boolean,
  theme: ResolvedRoomTheme,
  onTakeSeat: () -> Unit,
  onInspectUser: () -> Unit
) {
  val isOccupied = seat.userId != null

  val infiniteTransition = rememberInfiniteTransition(label = "speaking_pulse_${seat.seatIndex}")
  val speakingPulse by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = if (seat.isSpeaking) 1.20f else 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.25f,
    targetValue = if (seat.isSpeaking) 0.85f else 0.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 500, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow_alpha"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .width(76.dp)
      .testTag("voice_seat_${seat.seatIndex}")
  ) {
    if (isOccupied) {
      val isDimmed = seat.isOwnerOffline
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(62.dp)
          .then(if (isDimmed) Modifier.alpha(0.55f) else Modifier)
          .clickable { onInspectUser() }
      ) {
        // Outer Speaking Animated Glow Ring (Double ring)
        if (seat.isSpeaking && !seat.isOwnerOffline) {
          Box(
            modifier = Modifier
              .size(60.dp * speakingPulse)
              .clip(CircleShape)
              .background(theme.speakingRingColor.copy(alpha = glowAlpha * 0.2f))
              .border(2.dp, theme.speakingRingColor.copy(alpha = glowAlpha), CircleShape)
          )
        }

        // Avatar Core Container
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                listOf(
                  theme.surfaceGlass,
                  Color(0xFF0C0E1A)
                )
              )
            )
            .border(
              width = if (seat.isSpeaking && !seat.isOwnerOffline) 2.dp else if (seat.isOwner) 2.dp else 1.5.dp,
              color = if (seat.isSpeaking && !seat.isOwnerOffline) theme.speakingRingColor else if (seat.isOwner) Color(0xFFFBBF24) else theme.primaryAccent.copy(alpha = 0.8f),
              shape = CircleShape
            )
        ) {
          Text(
            text = seat.username?.take(2)?.uppercase() ?: "??",
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
          )
        }

        // Owner / Admin Badge (Top right) with subtle glass background
        if (seat.isOwner) {
          Surface(
            color = Color(0xCC000000),
            shape = CircleShape,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .offset(x = 2.dp, y = (-2).dp)
          ) {
            Text(
              text = "👑",
              fontSize = 11.sp,
              modifier = Modifier.padding(2.dp)
            )
          }
        } else if (seat.isAdmin) {
          Surface(
            color = Color(0xCC000000),
            shape = CircleShape,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .offset(x = 2.dp, y = (-2).dp)
          ) {
            Text(
              text = "🛡️",
              fontSize = 10.sp,
              modifier = Modifier.padding(2.dp)
            )
          }
        }

        // Mute / Speaking / Offline Status (Bottom right)
        if (seat.isOwnerOffline) {
          Box(
            modifier = Modifier
              .size(18.dp)
              .clip(CircleShape)
              .background(Color(0xFFF59E0B))
              .border(1.5.dp, Color(0xFF0F172A), CircleShape)
              .align(Alignment.BottomEnd),
            contentAlignment = Alignment.Center
          ) {
            Text("💤", fontSize = 9.sp)
          }
        } else {
          Box(
            modifier = Modifier
              .size(18.dp)
              .clip(CircleShape)
              .background(if (seat.isMuted) Color(0xFFE11D48) else Color(0xFF10B981))
              .border(1.5.dp, Color(0xFF0F172A), CircleShape)
              .align(Alignment.BottomEnd),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (seat.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(11.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = if (seat.isOwnerOffline) "${seat.username ?: "Owner"}\n(💤 Offline)" else if (isCurrentUser) "You" else (seat.username ?: "User"),
        fontSize = if (seat.isOwnerOffline) 9.5.sp else 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (seat.isOwnerOffline) Color(0xFFFBBF24) else Color.White,
        maxLines = if (seat.isOwnerOffline) 2 else 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center
      )
    } else {
      // Elegant Frosted Empty Seat
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(62.dp)
          .clickable { onTakeSeat() }
          .testTag("empty_seat_${seat.seatIndex}")
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(theme.seatItemEmptyBg)
            .border(
              width = 1.2.dp,
              color = theme.seatItemEmptyBorder,
              shape = CircleShape
            )
        ) {
          Icon(
            Icons.Default.Add,
            contentDescription = "Take Seat",
            tint = theme.primaryAccent.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Seat ${seat.seatIndex + 1}",
        fontSize = 10.sp,
        color = Color.White.copy(alpha = 0.65f),
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun ThemedChatMessageItem(
  message: ChatMessage,
  currentUserId: String,
  canModerate: Boolean,
  theme: ResolvedRoomTheme,
  onAddReaction: (String) -> Unit,
  onDelete: () -> Unit,
  onReport: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }
  val isGift = message.senderRole == "GIFT"
  val isSystem = message.isSystem || message.senderRole == "SYSTEM"

  if (isSystem) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 3.dp)
    ) {
      Surface(
        color = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(
          text = message.text,
          fontSize = 10.sp,
          color = Color.White.copy(alpha = 0.7f),
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
      }
    }
    return
  }

  if (isGift) {
    // Celebratory gift pill
    Surface(
      color = theme.primaryAccent.copy(alpha = 0.18f),
      shape = RoundedCornerShape(12.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.4f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 3.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
      ) {
        Text("🎁", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "${message.senderName} ${message.text}",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFFBBF24)
        )
      }
    }
    return
  }

  // Standard user message
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp)
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = message.senderName,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Spacer(modifier = Modifier.width(4.dp))
        when (message.senderRole) {
          "OWNER" -> {
            Surface(
              color = Color(0xFFF59E0B).copy(alpha = 0.25f),
              shape = RoundedCornerShape(4.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
            ) {
              Text("👑 OWNER", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
            }
          }
          "ADMIN" -> {
            Surface(
              color = Color(0xFF3B82F6).copy(alpha = 0.25f),
              shape = RoundedCornerShape(4.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6))
            ) {
              Text("🛡️ ADMIN", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
            }
          }
        }
      }

      Surface(
        color = if (message.senderId == currentUserId) theme.chatBubbleHighlight else Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier
          .padding(top = 2.dp)
          .clickable { showMenu = !showMenu }
      ) {
        Text(
          text = message.text,
          fontSize = 12.sp,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
      }

      // Reactions
      if (message.reactions.isNotEmpty()) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.padding(top = 2.dp)
        ) {
          message.reactions.forEach { (emoji, count) ->
            Surface(
              color = Color.Black.copy(alpha = 0.4f),
              shape = RoundedCornerShape(6.dp),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
              modifier = Modifier.clickable { onAddReaction(emoji) }
            ) {
              Text(
                text = "$emoji $count",
                fontSize = 9.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }
      }

      if (showMenu) {
        Row(modifier = Modifier.padding(top = 2.dp)) {
          if (message.senderId == currentUserId || canModerate) {
            Text(
              text = "Delete",
              color = Color(0xFFF43F5E),
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier
                .clickable { onDelete() }
                .padding(end = 12.dp)
            )
          }
          Text(
            text = "Report",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 9.sp,
            modifier = Modifier.clickable { onReport() }
          )
        }
      }
    }
  }
}

@Composable
fun VideoSelectDialog(
  onDismiss: () -> Unit,
  onSelect: (title: String, duration: Int) -> Unit
) {
  val videoStreams = listOf(
    Pair("Cyberpunk: Edgerunners Ep. 1", 1420),
    Pair("Lofi Hip Hop Radio - Beats to Relax/Study to", 3600),
    Pair("Tears of Steel (Sci-Fi 4K HDR)", 734),
    Pair("Big Buck Bunny 60FPS Remastered", 596),
    Pair("Cosmos & Deep Space Documentary 4K", 2800),
    Pair("Demon Slayer Season 4 Special Stream", 1800),
    Pair("Kotlin Jetpack Compose Mastery Workshop", 2400)
  )

  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = Color(0xFF131626)),
      shape = RoundedCornerShape(24.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text("Select Synchronized Stream 🎬", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("All room participants synchronize to this video stream.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))

        Spacer(modifier = Modifier.height(14.dp))

        videoStreams.forEach { (title, dur) ->
          Surface(
            color = Color(0xFF1A1E32),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelect(title, dur) }
              .padding(vertical = 4.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(10.dp)
            ) {
              Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = Color(0xFF06B6D4))
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(formatSeconds(dur), color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Cancel", color = Color.White)
        }
      }
    }
  }
}

private fun formatSeconds(sec: Int): String {
  val m = sec / 60
  val s = sec % 60
  return String.format("%02d:%02d", m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchRoomSheet(
  currentRoomId: String,
  availableRooms: List<Room>,
  onDismiss: () -> Unit,
  onSelectRoom: (String) -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = DarkSurfaceElevated,
    scrimColor = Color.Black.copy(alpha = 0.6f)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Switch Room 🔄",
          fontSize = 18.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }
      Text(
        text = "Seamlessly switch to another live room without leaving the app",
        fontSize = 12.sp,
        color = TextSecondary
      )
      Spacer(modifier = Modifier.height(14.dp))

      val otherRooms = availableRooms.filter { it.id != currentRoomId }
      if (otherRooms.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("No other active rooms available right now.", color = TextSecondary, fontSize = 13.sp)
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(otherRooms) { targetRoom ->
            Surface(
              color = DarkSurfaceCard,
              shape = RoundedCornerShape(14.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onDismiss()
                  onSelectRoom(targetRoom.id)
                }
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = if (targetRoom.category == RoomCategory.WATCH_TOGETHER) "🎬" else "🎙️",
                    fontSize = 24.sp
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = targetRoom.name,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                    Text(
                      text = "Host: ${targetRoom.ownerName} • LV.${targetRoom.roomLevel}",
                      fontSize = 11.sp,
                      color = TextSecondary
                    )
                  }
                }
                Button(
                  onClick = {
                    onDismiss()
                    onSelectRoom(targetRoom.id)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                  Text("Join", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
