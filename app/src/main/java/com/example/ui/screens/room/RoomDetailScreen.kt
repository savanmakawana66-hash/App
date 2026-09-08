package com.example.ui.screens.room

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
  onReportMessage: (msgId: String, senderName: String) -> Unit
) {
  val context = LocalContext.current
  val isOwner = room.ownerId == currentUser.id
  val isAdmin = admins.any { it.userId == currentUser.id }
  val currentSeat = seats.find { it.userId == currentUser.id }
  val isUserSeated = currentSeat != null
  val hasRequestedSeat = audience.find { it.userId == currentUser.id }?.hasRequestedSeat == true

  var chatText by remember { mutableStateOf("") }
  var isFullscreen by remember { mutableStateOf(false) }
  var showVideoSelectMenu by remember { mutableStateOf(false) }
  val chatListState = rememberLazyListState()

  // Auto-scroll chat when new message arrives
  LaunchedEffect(chatMessages.size) {
    if (chatMessages.isNotEmpty()) {
      chatListState.animateScrollToItem(chatMessages.size - 1)
    }
  }

  Scaffold(
    containerColor = DarkBackground,
    topBar = {
      // 1. Header: Room Name, Level, Room ID, Owner 👑, Actions
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurface)
          .statusBarsPadding()
          .padding(horizontal = 14.dp, vertical = 8.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            IconButton(
              onClick = onLeaveRoom,
              modifier = Modifier
                .size(36.dp)
                .testTag("leave_room_btn")
            ) {
              Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Leave Room",
                tint = Color.White
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
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
                Spacer(modifier = Modifier.width(6.dp))
                RoomLevelBadge(level = room.roomLevel)
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                  val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  cm.setPrimaryClip(ClipData.newPlainText("Room ID", room.id))
                }
              ) {
                Text(
                  text = "ID: ${room.id}",
                  fontSize = 11.sp,
                  color = TextSecondary,
                  fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "👑 ${room.ownerName}",
                  fontSize = 11.sp,
                  color = GoldOwner,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onOpenInvite,
              modifier = Modifier.size(36.dp).testTag("room_invite_btn")
            ) {
              Icon(Icons.Default.Share, contentDescription = "Invite", tint = IndigoLight, modifier = Modifier.size(20.dp))
            }

            if (isOwner || isAdmin) {
              IconButton(
                onClick = onOpenRoomSettings,
                modifier = Modifier.size(36.dp).testTag("room_settings_btn")
              ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
              }
            }
          }
        }

        // Room XP bar
        Spacer(modifier = Modifier.height(4.dp))
        XPProgressBar(
          currentXp = room.roomXp,
          maxThreshXp = when (room.roomLevel) {
            1 -> 5000
            2 -> 10000
            3 -> 20000
            else -> 35000
          },
          showLabels = false
        )
      }
    },
    bottomBar = {
      // 6. Bottom Control Bar: 🎙️ Mic, 🔊 Speaker, ✋ Raise Hand / Request Seat, 💬 Chat, ⚙️ Settings
      Surface(
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          // Emoji Quick Reaction Bar
          Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 6.dp)
          ) {
            listOf("❤️", "🔥", "😂", "👏", "🍿", "🎉", "😱").forEach { emoji ->
              Surface(
                color = DarkSurfaceElevated,
                shape = CircleShape,
                modifier = Modifier
                  .clickable {
                    if (chatMessages.isNotEmpty()) {
                      onAddReaction(chatMessages.last().id, emoji)
                    } else {
                      onSendChatMessage(emoji)
                    }
                  }
                  .padding(2.dp)
              ) {
                Text(
                  text = emoji,
                  fontSize = 16.sp,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }
          }

          // Chat Input Row
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            OutlinedTextField(
              value = chatText,
              onValueChange = { chatText = it },
              placeholder = { Text("Say something in room...", fontSize = 12.sp) },
              singleLine = true,
              shape = RoundedCornerShape(20.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated,
                focusedBorderColor = IndigoPrimary,
                unfocusedBorderColor = DarkSurfaceBorder
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("room_chat_input")
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
              onClick = {
                if (chatText.isNotBlank()) {
                  onSendChatMessage(chatText)
                  chatText = ""
                }
              },
              colors = IconButtonDefaults.iconButtonColors(containerColor = IndigoPrimary),
              modifier = Modifier
                .size(42.dp)
                .testTag("send_room_chat_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Control buttons
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
          ) {
            // Microphone toggle (Only if in voice seat)
            if (isUserSeated) {
              val isMuted = currentSeat?.isMuted == true
              IconButton(
                onClick = onToggleMic,
                colors = IconButtonDefaults.iconButtonColors(
                  containerColor = if (isMuted) RoseError else IndigoPrimary
                ),
                modifier = Modifier
                  .size(46.dp)
                  .testTag("toggle_mic_btn")
              ) {
                Icon(
                  imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                  contentDescription = "Microphone",
                  tint = Color.White,
                  modifier = Modifier.size(22.dp)
                )
              }
            } else {
              // Audience: Request Seat button
              Button(
                onClick = onRequestSeat,
                enabled = !hasRequestedSeat,
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (hasRequestedSeat) DarkSurfaceElevated else VioletAudience
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("request_voice_seat_btn")
              ) {
                Icon(Icons.Default.FrontHand, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (hasRequestedSeat) "Request Sent" else "Request Seat",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            // Raise hand
            if (isUserSeated) {
              IconButton(
                onClick = onToggleRaiseHand,
                colors = IconButtonDefaults.iconButtonColors(
                  containerColor = if (currentSeat?.raisedHand == true) AmberXp else DarkSurfaceElevated
                ),
                modifier = Modifier.size(46.dp).testTag("raise_hand_btn")
              ) {
                Text("✋", fontSize = 18.sp)
              }
            }

            // Leave Seat button (if seated)
            if (isUserSeated) {
              OutlinedButton(
                onClick = onLeaveSeat,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.testTag("leave_voice_seat_btn")
              ) {
                Text("Leave Seat", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              }
            }

            // Owner/Admin Approve Requests button
            if ((isOwner || isAdmin) && audience.any { it.hasRequestedSeat }) {
              val pendingCount = audience.count { it.hasRequestedSeat }
              Button(
                onClick = onOpenSeatRequests,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("pending_seat_requests_btn")
              ) {
                Text("✋ Requests ($pendingCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // 2. SYNCHRONIZED VIDEO PLAYER (Top of room)
      SynchronizedVideoPlayer(
        room = room,
        canControlVideo = canControlVideo,
        onTogglePlayPause = onTogglePlayPause,
        onSeek = onSeekVideo,
        onForward10 = onForward10,
        onRewind10 = onRewind10,
        onChangeVideoClick = { showVideoSelectMenu = true },
        isFullscreen = isFullscreen,
        onToggleFullscreen = { isFullscreen = !isFullscreen }
      )

      // Video selector drop-down dialog
      if (showVideoSelectMenu) {
        VideoSelectDialog(
          onDismiss = { showVideoSelectMenu = false },
          onSelect = { title, duration ->
            onChangeVideo(title, duration)
            showVideoSelectMenu = false
          }
        )
      }

      // 3. VOICE SEATS (8-Seat Grid)
      VoiceSeatsSection(
        seats = seats,
        currentUserId = currentUser.id,
        onSeatClick = { index, seat ->
          if (seat.userId == null) {
            onTakeSeat(index)
          } else {
            onInspectUser(
              Friend(
                userId = seat.userId,
                username = seat.username ?: "User",
                avatar = seat.avatar ?: "avatar_1",
                isOnline = true
              )
            )
          }
        }
      )

      // 4. AUDIENCE BAR (Audience Mode)
      AudienceBar(
        audienceCount = room.audienceCount,
        audienceMembers = audience,
        isUserSeated = isUserSeated,
        hasRequestedSeat = hasRequestedSeat,
        canApproveRequests = isOwner || isAdmin,
        onRequestSeat = onRequestSeat,
        onOpenRequests = onOpenSeatRequests
      )

      Divider(color = DarkSurfaceBorder, thickness = 1.dp)

      // 5. ROOM CHAT SECTION
      Box(modifier = Modifier.weight(1f)) {
        LazyColumn(
          state = chatListState,
          contentPadding = PaddingValues(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(chatMessages, key = { it.id }) { message ->
            ChatMessageItem(
              message = message,
              currentUserId = currentUser.id,
              canModerate = isOwner || isAdmin,
              onDelete = { onDeleteMessage(message.id) },
              onReport = { onReportMessage(message.id, message.senderName) },
              onAddReaction = { emoji -> onAddReaction(message.id, emoji) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun SynchronizedVideoPlayer(
  room: Room,
  canControlVideo: Boolean,
  onTogglePlayPause: () -> Unit,
  onSeek: (Float) -> Unit,
  onForward10: () -> Unit,
  onRewind10: () -> Unit,
  onChangeVideoClick: () -> Unit,
  isFullscreen: Boolean,
  onToggleFullscreen: () -> Unit
) {
  val height = if (isFullscreen) 280.dp else 190.dp

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(height)
      .background(Color.Black)
      .testTag("video_player_container")
  ) {
    // Simulated video graphics: animated cyberpunk/cinematic glow background
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            listOf(
              Color(0xFF1E1B4B),
              Color(0xFF0F172A),
              Color.Black
            )
          )
        )
    )

    // Animated film/stream pattern
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxSize()
    ) {
      Text(
        text = "🎬 ${room.activeVideoTitle}",
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 20.dp)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          color = EmeraldOnline.copy(alpha = 0.2f),
          shape = RoundedCornerShape(4.dp)
        ) {
          Text(
            text = "⚡ In Sync (12ms delay)",
            color = EmeraldOnline,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
          color = PurpleContainer,
          shape = RoundedCornerShape(4.dp)
        ) {
          Text(
            text = when (room.videoControlMode) {
              VideoControlMode.EVERYONE -> "🎮 Controls: Everyone"
              VideoControlMode.ADMINS_ONLY -> "🛡️ Controls: Admins"
              VideoControlMode.OWNER_ONLY -> "👑 Controls: Owner"
            },
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }
    }

    // Top video controls: Stream change & Fullscreen
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .align(Alignment.TopCenter)
    ) {
      Surface(
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onChangeVideoClick() }
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = IndigoLight, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Change Stream", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
      }

      IconButton(
        onClick = onToggleFullscreen,
        modifier = Modifier
          .size(32.dp)
          .background(Color.Black.copy(alpha = 0.6f), CircleShape)
      ) {
        Icon(
          imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
          contentDescription = "Fullscreen",
          tint = Color.White,
          modifier = Modifier.size(18.dp)
        )
      }
    }

    // Bottom video player controls & scrubber
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .background(
          Brush.verticalGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
          )
        )
        .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
      // Progress scrubber slider
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = formatSeconds(room.activeVideoPosSec.toInt()),
          color = Color.White,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )
        Slider(
          value = room.activeVideoPosSec,
          onValueChange = { onSeek(it) },
          valueRange = 0f..room.activeVideoDurationSec.toFloat(),
          enabled = canControlVideo,
          colors = SliderDefaults.colors(
            thumbColor = IndigoPrimary,
            activeTrackColor = IndigoPrimary,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
          ),
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp)
            .testTag("video_scrubber_slider")
        )
        Text(
          text = formatSeconds(room.activeVideoDurationSec),
          color = TextSecondary,
          fontSize = 10.sp
        )
      }

      // Play / Pause, Rewind 10s, Forward 10s
      Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        IconButton(
          onClick = onRewind10,
          enabled = canControlVideo,
          modifier = Modifier.size(32.dp).testTag("rewind_10_btn")
        ) {
          Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = if (canControlVideo) Color.White else TextMuted)
        }

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(
          onClick = onTogglePlayPause,
          enabled = canControlVideo,
          modifier = Modifier
            .size(40.dp)
            .background(if (canControlVideo) IndigoPrimary else DarkSurfaceElevated, CircleShape)
            .testTag("video_play_pause_btn")
        ) {
          Icon(
            imageVector = if (room.isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (room.isVideoPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(
          onClick = onForward10,
          enabled = canControlVideo,
          modifier = Modifier.size(32.dp).testTag("forward_10_btn")
        ) {
          Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = if (canControlVideo) Color.White else TextMuted)
        }
      }
    }
  }
}

@Composable
fun VoiceSeatsSection(
  seats: List<RoomSeat>,
  currentUserId: String,
  onSeatClick: (Int, RoomSeat) -> Unit
) {
  val occupiedCount = seats.count { it.userId != null }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(DarkSurface)
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("🎙️", fontSize = 13.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "VOICE SEATS ($occupiedCount/8)",
          fontSize = 12.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          letterSpacing = 1.sp
        )
      }
      if (occupiedCount >= 8) {
        Surface(color = RoseError.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
          Text("FULL (8/8)", color = RoseError, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
      } else {
        Text("${8 - occupiedCount} seats free", color = EmeraldOnline, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 2 rows of 4 seats
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      for (i in 0..3) {
        val seat = seats.getOrNull(i) ?: RoomSeat(seatIndex = i)
        VoiceSeatItem(
          seat = seat,
          isCurrentUser = seat.userId == currentUserId,
          onClick = { onSeatClick(i, seat) },
          modifier = Modifier.weight(1f)
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      for (i in 4..7) {
        val seat = seats.getOrNull(i) ?: RoomSeat(seatIndex = i)
        VoiceSeatItem(
          seat = seat,
          isCurrentUser = seat.userId == currentUserId,
          onClick = { onSeatClick(i, seat) },
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
fun VoiceSeatItem(
  seat: RoomSeat,
  isCurrentUser: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .padding(4.dp)
      .clickable { onClick() }
      .testTag("seat_${seat.seatIndex}")
  ) {
    if (seat.userId != null) {
      SpeakingAvatar(
        avatarName = seat.avatar ?: "avatar_1",
        username = seat.username ?: "User",
        isSpeaking = seat.isSpeaking,
        isMuted = seat.isMuted,
        isOwner = seat.isOwner,
        isAdmin = seat.isAdmin,
        size = 40.dp
      )
    } else {
      // Empty seat box
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, CircleShape)
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Empty Seat",
          tint = TextSecondary,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "Seat ${seat.seatIndex + 1}",
        fontSize = 10.sp,
        color = TextSecondary
      )
    }
  }
}

@Composable
fun AudienceBar(
  audienceCount: Int,
  audienceMembers: List<AudienceMember>,
  isUserSeated: Boolean,
  hasRequestedSeat: Boolean,
  canApproveRequests: Boolean,
  onRequestSeat: () -> Unit,
  onOpenRequests: () -> Unit
) {
  Surface(
    color = Color(0xFF131A29),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("👥", fontSize = 13.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Audience: $audienceCount Watching",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color.White
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (!isUserSeated) {
          Button(
            onClick = onRequestSeat,
            enabled = !hasRequestedSeat,
            colors = ButtonDefaults.buttonColors(
              containerColor = if (hasRequestedSeat) DarkSurfaceElevated else VioletAudience
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier = Modifier.testTag("audience_request_seat_btn")
          ) {
            Text(
              text = if (hasRequestedSeat) "Request Sent ✋" else "Request Seat",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }

        if (canApproveRequests && audienceMembers.any { it.hasRequestedSeat }) {
          Spacer(modifier = Modifier.width(6.dp))
          TextButton(
            onClick = onOpenRequests,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text("Review", color = EmeraldOnline, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun ChatMessageItem(
  message: ChatMessage,
  currentUserId: String,
  canModerate: Boolean,
  onDelete: () -> Unit,
  onReport: () -> Unit,
  onAddReaction: (String) -> Unit
) {
  if (message.isSystem) {
    // System message
    Surface(
      color = DarkSurfaceElevated.copy(alpha = 0.6f),
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)
    ) {
      Text(
        text = "📢 ${message.text}",
        fontSize = 11.sp,
        color = AmberXp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
      )
    }
  } else {
    // User message
    var showMenu by remember { mutableStateOf(false) }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(IndigoDark)
      ) {
        Text(getInitials(message.senderName), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = message.senderName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.width(4.dp))
          if (message.senderRole == "OWNER") {
            OwnerBadge()
          } else if (message.senderRole == "ADMIN") {
            AdminBadge()
          }
        }

        Surface(
          color = if (message.senderId == currentUserId) IndigoDark.copy(alpha = 0.4f) else DarkSurfaceElevated,
          shape = RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp),
          modifier = Modifier
            .padding(top = 2.dp)
            .clickable { showMenu = !showMenu }
        ) {
          Text(
            text = message.text,
            fontSize = 13.sp,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                color = DarkSurfaceBorder,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.clickable { onAddReaction(emoji) }
              ) {
                Text(
                  text = "$emoji $count",
                  fontSize = 10.sp,
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
                color = RoseError,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                  .clickable { onDelete() }
                  .padding(end = 12.dp)
              )
            }
            Text(
              text = "Report",
              color = TextSecondary,
              fontSize = 10.sp,
              modifier = Modifier.clickable { onReport() }
            )
          }
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
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
      shape = RoundedCornerShape(24.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text("Select Synchronized Stream 🎬", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("All room participants will synchronize to this video.", fontSize = 12.sp, color = TextSecondary)

        Spacer(modifier = Modifier.height(14.dp))

        videoStreams.forEach { (title, dur) ->
          Surface(
            color = DarkSurfaceElevated,
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
              Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = IndigoLight)
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(formatSeconds(dur), color = TextSecondary, fontSize = 10.sp)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
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
