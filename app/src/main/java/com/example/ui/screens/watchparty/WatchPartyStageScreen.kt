package com.example.ui.screens.watchparty

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.*
import com.example.network.WatchPartyNetworkEngine
import com.example.network.webrtc.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchPartyStageScreen(
  engine: WatchPartyNetworkEngine,
  party: WatchPartyRoom,
  isInPipMode: Boolean = false,
  onEnterPip: () -> Unit = {},
  onLeave: () -> Unit
) {
  val context = LocalContext.current
  val isVoiceMuted by engine.voiceRoomManager.isMuted.collectAsState()
  val isInVoice by engine.voiceRoomManager.isInVoice.collectAsState()
  val isLocalSpeaking by engine.voiceRoomManager.isLocalSpeaking.collectAsState()
  val audioAmplitude by engine.voiceRoomManager.audioAmplitude.collectAsState()

  var showParticipantsSheet by remember { mutableStateOf(false) }
  var showQueueSheet by remember { mutableStateOf(false) }
  var showQualityMenu by remember { mutableStateOf(false) }
  var isFullscreen by remember { mutableStateOf(false) }
  var showFloatingChat by remember { mutableStateOf(true) }

  // Ephemeral Floating Reaction Emojis
  var floatingReactions by remember { mutableStateOf(listOf<String>()) }
  val liveChatMessages = remember { mutableStateListOf<String>() }

  // Non-host control tap warning ("Host controls playback")
  var hostControlWarning by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(hostControlWarning) {
    if (hostControlWarning != null) {
      delay(2200)
      hostControlWarning = null
    }
  }

  // SAF File Picker: Add one or more videos to Watch Party Queue
  val addToQueuePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
      val resolvedItems = uris.map { engine.resolveLocalMediaFromUri(it) }
      engine.addMultipleToQueue(resolvedItems)
      Toast.makeText(context, "Added ${resolvedItems.size} video(s) to Queue 🎬", Toast.LENGTH_SHORT).show()
    }
  }

  // SAF File Picker: External Subtitle (.srt) Selection
  val subtitlePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        val displayName = cursor?.use {
          if (it.moveToFirst()) {
            val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIdx >= 0) it.getString(nameIdx) else null
          } else null
        } ?: "Subtitles.srt"
        engine.setSubtitle(uri, displayName)
        Toast.makeText(context, "Subtitles loaded: $displayName", Toast.LENGTH_SHORT).show()
      } catch (e: Exception) {
        Log.e("WatchPartyStage", "Failed to load subtitle URI", e)
      }
    }
  }

  // Listen to remote WebRTC reaction bursts and WebSocket relay
  LaunchedEffect(Unit) {
    engine.webRtcManager.incomingPackets.collect { packet ->
      if (packet.type == RtcMessageType.REACTION_BURST) {
        try {
          val payload = JSONObject(packet.payloadJson)
          val emoji = payload.optString("emoji", "")
          if (emoji.isNotBlank()) {
            floatingReactions = floatingReactions + emoji
          }
        } catch (_: Exception) {}
      }
    }
  }

  LaunchedEffect(Unit) {
    engine.incomingReactions.collect { emoji ->
      if (emoji.isNotBlank()) {
        floatingReactions = floatingReactions + emoji
      }
    }
  }

  LaunchedEffect(Unit) {
    engine.incomingChatMessages.collect { obj ->
      val msg = obj.optString("message")
      val sender = obj.optString("username", "Member")
      if (msg.isNotBlank()) {
        liveChatMessages.add("$sender: $msg")
      }
    }
  }

  val uploadProgress by engine.mediaPublisher.uploadProgress.collectAsState()
  val isPublished by engine.mediaPublisher.isPublished.collectAsState()

  fun triggerReaction(emoji: String) {
    floatingReactions = floatingReactions + emoji
    engine.sendReactionBurst(emoji)
    engine.webRtcManager.broadcastReaction(emoji, party.hostName)
  }

  // PICTURE-IN-PICTURE (PiP) SPECIAL LAYOUT: Video Only
  if (isInPipMode) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
      VideoPlayerStage(
        party = party,
        isPlaying = party.isPlaying,
        isFullscreen = true,
        isInPip = true,
        engine = engine,
        onNonHostAction = {},
        onEnterPip = {},
        onPickSubtitles = {},
        onOpenQueue = {},
        onFullscreenToggle = {}
      )
    }
    return
  }

  Scaffold(
    containerColor = DarkBackground,
    topBar = {
      if (!isFullscreen) {
        // TOP APP BAR: Room Title, Host Badge, Room Code (tap-to-copy), Share, and Leave
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
          title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = party.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  if (party.isHost) {
                    Surface(
                      color = AmberXp.copy(alpha = 0.2f),
                      shape = RoundedCornerShape(6.dp),
                      border = BorderStroke(1.dp, AmberXp)
                    ) {
                      Text(
                        text = "HOST",
                        color = AmberXp,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                      )
                    }
                  }
                }

                // Room Code with Tap to Copy
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .clickable {
                      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                      clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", party.code))
                      Toast.makeText(context, "Room Code ${party.code} copied! 📋", Toast.LENGTH_SHORT).show()
                    }
                    .testTag("room_code_header_badge")
                ) {
                  Text(
                    text = "Code: ${party.code}",
                    color = IndigoLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = IndigoLight, modifier = Modifier.size(11.dp))
                }
              }
            }
          },
          actions = {
            // Share Room Code button
            IconButton(
              onClick = {
                val sendIntent = Intent().apply {
                  action = Intent.ACTION_SEND
                  putExtra(Intent.EXTRA_TEXT, "Join my Watch Party '${party.name}'! Room Code: ${party.code}")
                  type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Invite friends to Watch Party"))
              },
              modifier = Modifier.testTag("share_room_code_btn")
            ) {
              Icon(Icons.Default.Share, contentDescription = "Share Code", tint = TextSecondary)
            }

            // Playlist / Queue Button
            IconButton(
              onClick = { showQueueSheet = true },
              modifier = Modifier.testTag("queue_sheet_toggle_btn")
            ) {
              BadgedBox(
                badge = {
                  if (party.playlist.size > 1) {
                    Badge(containerColor = IndigoPrimary) {
                      Text("${party.playlist.size}", color = Color.White, fontSize = 9.sp)
                    }
                  }
                }
              ) {
                Icon(Icons.Default.QueueMusic, contentDescription = "Queue", tint = IndigoLight)
              }
            }

            // Leave / End Party Button
            Button(
              onClick = {
                engine.leaveParty()
                onLeave()
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (party.isHost) Color(0xFFD32F2F) else DarkSurfaceElevated
              ),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.testTag("leave_party_btn")
            ) {
              Text(
                text = if (party.isHost) "End Party" else "Leave",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        )
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(if (isFullscreen) PaddingValues(0.dp) else paddingValues)
    ) {
      if (!isFullscreen) {
        // STATUS STRIP: Watching count, Connection pill & Stream Quality
        Surface(
          color = DarkSurfaceElevated,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 6.dp)
          ) {
            // Live Watching Count
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(EmeraldOnline)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "👥 ${party.participants.size} watching",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }

            // Quality Dropdown Selector
            Box {
              Surface(
                color = DarkSurfaceCard,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                  .clickable { showQualityMenu = true }
                  .testTag("quality_selector_trigger")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = party.currentQuality.label,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = IndigoLight, modifier = Modifier.size(16.dp))
                }
              }

              DropdownMenu(
                expanded = showQualityMenu,
                onDismissRequest = { showQualityMenu = false }
              ) {
                StreamQuality.values().forEach { q ->
                  DropdownMenuItem(
                    text = {
                      Column {
                        Text(q.label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(q.description, fontSize = 10.sp, color = Color.Gray)
                      }
                    },
                    onClick = {
                      engine.setQuality(q)
                      showQualityMenu = false
                    }
                  )
                }
              }
            }
          }
        }
      }

      // CINEMATIC 16:9 MEDIA STAGE
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .then(
            if (isFullscreen) Modifier.fillMaxSize()
            else Modifier.aspectRatio(16f / 9f)
          )
          .background(Color.Black)
      ) {
        if (party.activeMedia.mediaType == StreamMediaType.AUDIO) {
          MusicVisualizerStage(
            party = party,
            isPlaying = party.isPlaying,
            audioAmplitude = if (isLocalSpeaking) audioAmplitude else 0.4f
          )
        } else {
          VideoPlayerStage(
            party = party,
            isPlaying = party.isPlaying,
            isFullscreen = isFullscreen,
            isInPip = false,
            engine = engine,
            onNonHostAction = {
              hostControlWarning = "Host controls playback"
            },
            onEnterPip = onEnterPip,
            onPickSubtitles = {
              subtitlePickerLauncher.launch(arrayOf("application/x-subrip", "text/plain", "*/*"))
            },
            onOpenQueue = { showQueueSheet = true },
            onFullscreenToggle = { isFullscreen = !isFullscreen }
          )
        }

        // Non-host Action Toast Pill ("Host controls playback")
        if (hostControlWarning != null) {
          Surface(
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, AmberXp),
            modifier = Modifier
              .align(Alignment.Center)
              .padding(16.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Text("🔒", fontSize = 12.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = hostControlWarning ?: "",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        // Host Cloud Publishing Progress Overlay
        if (party.isHost && !isPublished && uploadProgress > 0f && uploadProgress < 1f) {
          Surface(
            color = Color.Black.copy(alpha = 0.8f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.6f)),
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(12.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              CircularProgressIndicator(
                progress = { uploadProgress },
                color = IndigoPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Cloud Publishing: ${(uploadProgress * 100).toInt()}%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        // Floating Live Chat Overlay
        if (showFloatingChat) {
          Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(start = 12.dp, bottom = if (isFullscreen) 90.dp else 12.dp)
              .fillMaxWidth(0.65f)
          ) {
            liveChatMessages.takeLast(3).forEach { msg ->
              Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DarkSurfaceBorder.copy(alpha = 0.5f))
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text("💬", fontSize = 10.sp)
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(
                    text = msg,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }

        // Floating reaction animations
        Row(
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(bottom = if (isFullscreen) 95.dp else 20.dp)
        ) {
          floatingReactions.takeLast(4).forEach { emoji ->
            Text(
              text = emoji,
              fontSize = 32.sp,
              modifier = Modifier.padding(horizontal = 6.dp)
            )
          }
        }
      }

      if (!isFullscreen) {
        // SYNCHRONIZED PLAYBACK CONTROLS
        Surface(
          color = DarkSurface,
          border = BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            // Time Progress Scrubber
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = formatTime(party.positionSec.toInt()),
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.width(44.dp)
              )

              Slider(
                value = party.positionSec,
                onValueChange = { newPos ->
                  if (party.isHost) {
                    engine.seekVideo(newPos)
                  } else {
                    hostControlWarning = "Host controls playback"
                  }
                },
                valueRange = 0f..party.durationSec.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                  thumbColor = IndigoLight,
                  activeTrackColor = IndigoPrimary,
                  inactiveTrackColor = DarkSurfaceElevated
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("synchronized_scrubber_slider")
              )

              Text(
                text = formatTime(party.durationSec),
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.width(44.dp)
              )
            }

            // Playback Buttons (Rewind, Play/Pause, Forward, Next, Fullscreen)
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceEvenly,
              modifier = Modifier.fillMaxWidth()
            ) {
              // Toggle Chat Overlay
              IconButton(
                onClick = { showFloatingChat = !showFloatingChat },
                modifier = Modifier.testTag("toggle_floating_chat_btn")
              ) {
                Icon(
                  imageVector = if (showFloatingChat) Icons.Default.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                  contentDescription = "Toggle Chat Overlay",
                  tint = if (showFloatingChat) IndigoLight else TextSecondary
                )
              }

              // -10s Rewind
              IconButton(
                onClick = {
                  if (party.isHost) engine.rewind10()
                  else hostControlWarning = "Host controls playback"
                },
                modifier = Modifier.testTag("rewind_10_btn")
              ) {
                Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White)
              }

              // Play / Pause Master Toggle
              FilledIconButton(
                onClick = {
                  if (party.isHost) engine.togglePlayPause()
                  else hostControlWarning = "Host controls playback"
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = IndigoPrimary),
                modifier = Modifier
                  .size(52.dp)
                  .testTag("toggle_play_pause_btn")
              ) {
                Icon(
                  imageVector = if (party.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                  contentDescription = if (party.isPlaying) "Pause" else "Play",
                  tint = Color.White,
                  modifier = Modifier.size(28.dp)
                )
              }

              // +10s Forward
              IconButton(
                onClick = {
                  if (party.isHost) engine.forward10()
                  else hostControlWarning = "Host controls playback"
                },
                modifier = Modifier.testTag("forward_10_btn")
              ) {
                Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White)
              }

              // Skip to Next in Playlist (if available)
              if (party.playlist.size > 1) {
                IconButton(
                  onClick = {
                    if (party.isHost) {
                      engine.playNextVideo()
                    } else {
                      hostControlWarning = "Host controls playback"
                    }
                  },
                  modifier = Modifier.testTag("skip_next_video_btn")
                ) {
                  Icon(Icons.Default.SkipNext, contentDescription = "Next Video", tint = Color.White)
                }
              }

              // Fullscreen Toggle
              IconButton(
                onClick = { isFullscreen = !isFullscreen },
                modifier = Modifier.testTag("fullscreen_toggle_btn")
              ) {
                Icon(
                  imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                  contentDescription = "Toggle Fullscreen",
                  tint = Color.White
                )
              }
            }
          }
        }

        // VOICE ROOM STRIP & PARTICIPANT AVATARS
        Surface(
          color = DarkSurfaceCard,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "🎙️ Voice Room (${party.participants.size})",
                  color = Color.White,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                  color = if (isInVoice) EmeraldOnline.copy(alpha = 0.2f) else DarkSurfaceElevated,
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = if (isInVoice) "Live In Voice" else "Listening Only",
                    color = if (isInVoice) EmeraldOnline else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              // Voice Action Controls (Mic Mute + View Participants)
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { engine.voiceRoomManager.toggleMute() },
                  modifier = Modifier
                    .size(36.dp)
                    .testTag("mic_toggle_btn")
                ) {
                  Icon(
                    imageVector = if (isVoiceMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Toggle Mic",
                    tint = if (isVoiceMuted) Color(0xFFFF5252) else EmeraldOnline
                  )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                  onClick = { showParticipantsSheet = true },
                  modifier = Modifier
                    .size(36.dp)
                    .testTag("view_participants_btn")
                ) {
                  Icon(Icons.Default.People, contentDescription = "Participants", tint = TextSecondary)
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row of Participant Avatars with Speaking Pulse
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(party.participants) { p ->
                ParticipantAvatarPill(participant = p)
              }
            }
          }
        }

        // FOOTER: REACTION BURST BAR
        Surface(
          color = DarkSurface,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 8.dp)
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("🍿", "❤️", "🔥", "😂", "🎬").forEach { emoji ->
                Surface(
                  shape = CircleShape,
                  color = DarkSurfaceElevated,
                  modifier = Modifier
                    .size(36.dp)
                    .clickable { triggerReaction(emoji) }
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 18.sp)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // WATCH PARTY PLAYLIST / QUEUE SHEET
  if (showQueueSheet) {
    WatchPartyQueueSheet(
      party = party,
      isHost = party.isHost,
      onDismiss = { showQueueSheet = false },
      onPlayItem = { index ->
        engine.playQueueIndex(index)
        showQueueSheet = false
      },
      onRemoveItem = { index -> engine.removeFromQueue(index) },
      onMoveUp = { index -> if (index > 0) engine.reorderQueue(index, index - 1) },
      onMoveDown = { index -> if (index < party.playlist.lastIndex) engine.reorderQueue(index, index + 1) },
      onAddVideos = {
        try {
          addToQueuePickerLauncher.launch(arrayOf("video/*", "audio/*"))
        } catch (_: Exception) {
          addToQueuePickerLauncher.launch(arrayOf("*/*"))
        }
      }
    )
  }

  // PARTICIPANTS SHEET MODAL
  if (showParticipantsSheet) {
    ModalBottomSheet(
      onDismissRequest = { showParticipantsSheet = false },
      containerColor = DarkSurface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Watch Party Members (${party.participants.size})",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Stream: ${party.currentQuality.label}",
            color = IndigoLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          items(party.participants) { p ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = DarkSurfaceElevated,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (p.isHost) AmberXp else IndigoPrimary)
                ) {
                  Text(if (p.isHost) "👑" else "👤", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = p.username,
                      color = Color.White,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold
                    )
                    if (p.isHost) {
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "HOST",
                        color = AmberXp,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                  Text(
                    text = "Ping: ${p.pingMs}ms • Connected",
                    color = TextSecondary,
                    fontSize = 11.sp
                  )
                }

                Icon(
                  imageVector = if (p.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                  contentDescription = null,
                  tint = if (p.isMuted) Color(0xFFFF5252) else EmeraldOnline,
                  modifier = Modifier.size(18.dp)
                )

                if (party.isHost && !p.isHost) {
                  Spacer(modifier = Modifier.width(10.dp))
                  IconButton(
                    onClick = { engine.kickParticipant(p.id) },
                    modifier = Modifier.size(32.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.RemoveCircleOutline,
                      contentDescription = "Kick",
                      tint = Color(0xFFFF5252)
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
}

// -------------------------------------------------------------
// PARTICIPANT AVATAR PILL WITH SPEAKING PULSE
// -------------------------------------------------------------
@Composable
fun ParticipantAvatarPill(participant: WatchPartyParticipant) {
  val infiniteTransition = rememberInfiniteTransition(label = "speaking_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(400, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.width(52.dp)
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(44.dp)
        .scale(if (participant.isSpeaking) pulseScale else 1f)
        .clip(CircleShape)
        .background(
          if (participant.isSpeaking) EmeraldOnline.copy(alpha = 0.3f)
          else if (participant.isHost) AmberXp.copy(alpha = 0.2f)
          else IndigoPrimary.copy(alpha = 0.2f)
        )
        .border(
          width = if (participant.isSpeaking) 2.dp else 1.dp,
          color = if (participant.isSpeaking) EmeraldOnline else if (participant.isHost) AmberXp else DarkSurfaceBorder,
          shape = CircleShape
        )
    ) {
      Text(if (participant.isHost) "👑" else "👤", fontSize = 18.sp)

      if (participant.isMuted) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(14.dp)
            .align(Alignment.BottomEnd)
            .clip(CircleShape)
            .background(Color(0xFFFF5252))
        ) {
          Icon(Icons.Default.MicOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = participant.username,
      color = if (participant.isSpeaking) EmeraldOnline else Color.White,
      fontSize = 10.sp,
      fontWeight = if (participant.isSpeaking) FontWeight.Bold else FontWeight.Normal,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

// -------------------------------------------------------------
// AUDIO TRACK DATA HELPER
// -------------------------------------------------------------
data class AudioTrackItem(
  val groupIndex: Int,
  val trackIndex: Int,
  val label: String,
  val isSelected: Boolean
)

// -------------------------------------------------------------
// PRODUCTION-GRADE VIDEO PLAYER STAGE (AndroidX Media3 ExoPlayer)
// -------------------------------------------------------------
@Composable
fun VideoPlayerStage(
  party: WatchPartyRoom,
  isPlaying: Boolean,
  isFullscreen: Boolean = false,
  isInPip: Boolean = false,
  engine: WatchPartyNetworkEngine,
  onNonHostAction: () -> Unit,
  onEnterPip: () -> Unit,
  onPickSubtitles: () -> Unit,
  onOpenQueue: () -> Unit,
  onFullscreenToggle: () -> Unit
) {
  val context = LocalContext.current
  val media = party.activeMedia

  // Resolve playback URI:
  // For Host: SAF content:// URI or local file:// directly.
  // For Remote Participant: LAN streaming URL.
  val streamUri = remember(party.isHost, party.streamUrl, media.uriString, media.uri, media.isSafSelected, media.isLocalFile) {
    val effectiveUri = media.getEffectiveUri()
    if (party.isHost && (media.isSafSelected || media.isLocalFile || effectiveUri?.scheme == "content" || effectiveUri?.scheme == "file")) {
      effectiveUri
    } else if (effectiveUri?.scheme?.startsWith("http") == true && !media.isSafSelected) {
      effectiveUri
    } else if (!party.streamUrl.isNullOrBlank()) {
      val raw = party.streamUrl!!
      val fullUrl = if (raw.startsWith("/")) {
        com.example.network.api.BackendServerConfig.activeBaseUrl + raw
      } else raw
      Uri.parse(fullUrl)
    } else {
      effectiveUri
    }
  }

  var isBuffering by remember { mutableStateOf(false) }
  var playbackError by remember { mutableStateOf<String?>(null) }
  var retryTrigger by remember { mutableStateOf(0) }

  // Auto-hiding controls state
  var controlsVisible by remember { mutableStateOf(!isInPip) }
  var lastUserInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

  // Audio track and speed states
  var availableAudioTracks by remember { mutableStateOf<List<AudioTrackItem>>(emptyList()) }
  var showAudioTrackDialog by remember { mutableStateOf(false) }
  var showSpeedDialog by remember { mutableStateOf(false) }
  var isLocalMuted by remember { mutableStateOf(false) }

  // Auto-hide controls after 3.5 seconds of inactivity
  LaunchedEffect(controlsVisible, lastUserInteractionTime, party.isPlaying, isInPip) {
    if (!isInPip && controlsVisible && party.isPlaying) {
      delay(3500)
      controlsVisible = false
    }
  }

  fun notifyUserInteraction() {
    lastUserInteractionTime = System.currentTimeMillis()
    controlsVisible = true
  }

  // Build ExoPlayer instance with standard browser User-Agent and redirect handling to prevent CDN 403s
  val exoPlayer = remember(context) {
    val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
      .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 ExoPlayer/2.19.1")
      .setAllowCrossProtocolRedirects(true)
      .setConnectTimeoutMs(15000)
      .setReadTimeoutMs(30000)

    val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)

    ExoPlayer.Builder(context)
      .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory))
      .setSeekBackIncrementMs(10_000)
      .setSeekForwardIncrementMs(10_000)
      .build().apply {
        playWhenReady = party.isPlaying
        repeatMode = Player.REPEAT_MODE_OFF
      }
  }

  // Lifecycle Release
  DisposableEffect(exoPlayer) {
    onDispose {
      try {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.release()
      } catch (e: Exception) {
        Log.e("VideoPlayerStage", "Error releasing ExoPlayer", e)
      }
    }
  }

  // Playback listener for state, tracks, duration and completion
  DisposableEffect(exoPlayer) {
    val listener = object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        isBuffering = (playbackState == Player.STATE_BUFFERING)
        if (playbackState == Player.STATE_READY) {
          playbackError = null
          val durMs = exoPlayer.duration
          if (durMs > 0) {
            val durSec = (durMs / 1000L).toInt()
            val currentPosSec = (exoPlayer.currentPosition / 1000f)
            engine.updatePlaybackPosition(currentPosSec, durSec)
          }
        } else if (playbackState == Player.STATE_ENDED) {
          // Video finished: Advance to next in playlist automatically
          if (party.isHost) {
            engine.onCurrentVideoEnded()
          }
        }
      }

      override fun onTracksChanged(tracks: Tracks) {
        val audioList = mutableListOf<AudioTrackItem>()
        var audioCount = 0
        tracks.groups.forEachIndexed { groupIndex, group ->
          if (group.type == C.TRACK_TYPE_AUDIO) {
            for (trackIndex in 0 until group.length) {
              audioCount++
              val format = group.getTrackFormat(trackIndex)
              val lang = format.language ?: "Audio $audioCount"
              val label = if (format.label != null) "${format.label} ($lang)" else lang
              val isSelected = group.isTrackSelected(trackIndex)
              audioList.add(AudioTrackItem(groupIndex, trackIndex, label, isSelected))
            }
          }
        }
        availableAudioTracks = audioList
      }

      override fun onPlayerError(error: PlaybackException) {
        // Log technical detail to Logcat only
        Log.e("VideoPlayerStage", "ExoPlayer playback error [${error.errorCodeName}]: ${error.message}", error)
        // Clean user-friendly message without raw error codes
        playbackError = "Couldn't play this video"
        isBuffering = false
      }
    }
    exoPlayer.addListener(listener)
    onDispose {
      exoPlayer.removeListener(listener)
    }
  }

  // Load MediaItem with optional SubRip (.srt) configuration
  LaunchedEffect(streamUri, party.subtitlesEnabled, party.subtitleUriString, retryTrigger) {
    if (streamUri != null) {
      try {
        playbackError = null
        isBuffering = true

        val mediaItemBuilder = MediaItem.Builder().setUri(streamUri)

        // Configure SubRip subtitles if enabled
        if (party.subtitlesEnabled && !party.subtitleUriString.isNullOrBlank()) {
          try {
            val subUri = Uri.parse(party.subtitleUriString)
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subUri)
              .setMimeType(MimeTypes.APPLICATION_SUBRIP)
              .setLanguage("en")
              .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
              .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
          } catch (e: Exception) {
            Log.w("VideoPlayerStage", "Failed to configure subtitle", e)
          }
        }

        val mediaItem = mediaItemBuilder.build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        if (party.positionSec > 0f) {
          exoPlayer.seekTo((party.positionSec * 1000L).toLong())
        }
        exoPlayer.setPlaybackSpeed(party.playbackSpeed)
        exoPlayer.playWhenReady = party.isPlaying
      } catch (e: Exception) {
        Log.e("VideoPlayerStage", "Failed to load media item: $streamUri", e)
        playbackError = "Couldn't play this video"
        isBuffering = false
      }
    } else {
      exoPlayer.clearMediaItems()
      isBuffering = false
    }
  }

  // Sync Play/Pause with party state
  LaunchedEffect(isPlaying) {
    if (exoPlayer.isPlaying != isPlaying) {
      if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }
  }

  // Sync Playback Speed
  LaunchedEffect(party.playbackSpeed) {
    exoPlayer.setPlaybackSpeed(party.playbackSpeed)
  }

  // Local Volume Mute Toggle
  LaunchedEffect(isLocalMuted) {
    exoPlayer.volume = if (isLocalMuted) 0f else 1f
  }

  // Remote seek position drift sync
  LaunchedEffect(party.positionSec) {
    val playerPosSec = exoPlayer.currentPosition / 1000f
    val drift = kotlin.math.abs(playerPosSec - party.positionSec)
    if (!party.isHost && drift > 2.0f) {
      exoPlayer.seekTo((party.positionSec * 1000L).toLong())
    }
  }

  // Host continuous playback position reporting
  LaunchedEffect(isPlaying, party.isHost) {
    while (isPlaying && party.isHost) {
      delay(400)
      if (exoPlayer.playbackState == Player.STATE_READY) {
        val currentSec = exoPlayer.currentPosition / 1000f
        val durMs = exoPlayer.duration
        val durSec = if (durMs > 0) (durMs / 1000L).toInt() else party.durationSec
        engine.updatePlaybackPosition(currentSec, durSec)
      }
    }
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxSize()
      .testTag("video_player_stage")
      .clickable {
        if (!isInPip) {
          controlsVisible = !controlsVisible
          if (controlsVisible) notifyUserInteraction()
        }
      }
  ) {
    if (streamUri != null) {
      AndroidView(
        factory = { ctx ->
          PlayerView(ctx).apply {
            player = exoPlayer
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            layoutParams = FrameLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
          }
        },
        update = { playerView ->
          if (playerView.player != exoPlayer) {
            playerView.player = exoPlayer
          }
        },
        modifier = Modifier.fillMaxSize()
      )
    } else {
      // Empty playlist or waiting for host selection
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Movie,
          contentDescription = null,
          tint = TextSecondary,
          modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = if (party.isHost) "Select a video to start Watch Party" else "Waiting for host to start video...",
          color = Color.White,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.Center
        )
        if (party.isHost) {
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = onOpenQueue,
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Open Video Queue", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Centered Buffering Spinner
    if (isBuffering && playbackError == null) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.35f))
      ) {
        CircularProgressIndicator(
          color = IndigoLight,
          modifier = Modifier.size(42.dp),
          strokeWidth = 3.dp
        )
      }
    }

    // Clean user-friendly error pill (without raw ExoPlayer codes)
    if (playbackError != null) {
      Surface(
        color = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier
          .align(Alignment.Center)
          .padding(16.dp)
          .testTag("player_error_state")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          Text(
            text = playbackError ?: "Couldn't play this video",
            color = Color.White,
            fontSize = 12.sp
          )
          Spacer(modifier = Modifier.width(10.dp))
          TextButton(
            onClick = {
              playbackError = null
              retryTrigger++
            },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(28.dp)
          ) {
            Text(
              text = "Retry",
              color = IndigoLight,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // OVERLAY CONTROLS (Fade in / out, disabled in PiP)
    AnimatedVisibility(
      visible = controlsVisible && !isInPip && streamUri != null,
      enter = fadeIn(animationSpec = tween(200)),
      exit = fadeOut(animationSpec = tween(200)),
      modifier = Modifier.fillMaxSize()
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                Color.Black.copy(alpha = 0.65f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.8f)
              )
            )
          )
      ) {
        // TOP OVERLAY BAR: Title, Subtitles, Audio, Speed, PiP, Fullscreen
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          // Media Title & Queue Position
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = party.activeMedia.title,
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (party.playlist.size > 1) {
              Text(
                text = "${party.currentQueueIndex + 1} of ${party.playlist.size} in queue",
                color = TextSecondary,
                fontSize = 10.sp
              )
            }
          }

          // Controls Action Row
          Row(verticalAlignment = Alignment.CenterVertically) {
            // Subtitle CC Toggle & Picker Button
            IconButton(
              onClick = {
                notifyUserInteraction()
                if (party.isHost) {
                  if (party.subtitleUriString == null) {
                    onPickSubtitles()
                  } else {
                    engine.toggleSubtitles(!party.subtitlesEnabled)
                  }
                } else {
                  engine.toggleSubtitles(!party.subtitlesEnabled)
                }
              },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = if (party.subtitlesEnabled) Icons.Default.ClosedCaption else Icons.Outlined.ClosedCaption,
                contentDescription = "Subtitles",
                tint = if (party.subtitlesEnabled) IndigoLight else Color.White,
                modifier = Modifier.size(20.dp)
              )
            }

            // Audio Track Selector (if multi-track available)
            if (availableAudioTracks.size > 1) {
              IconButton(
                onClick = {
                  notifyUserInteraction()
                  showAudioTrackDialog = true
                },
                modifier = Modifier.size(36.dp)
              ) {
                Icon(Icons.Default.Audiotrack, contentDescription = "Audio Track", tint = Color.White, modifier = Modifier.size(19.dp))
              }
            }

            // Playback Speed Pill Button
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = DarkSurfaceElevated,
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
              modifier = Modifier
                .clickable {
                  notifyUserInteraction()
                  showSpeedDialog = true
                }
                .padding(horizontal = 4.dp)
            ) {
              Text(
                text = "${party.playbackSpeed}x",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }

            // Picture-in-Picture Button
            IconButton(
              onClick = {
                notifyUserInteraction()
                onEnterPip()
              },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Picture-in-Picture", tint = Color.White, modifier = Modifier.size(19.dp))
            }

            // Fullscreen Button
            IconButton(
              onClick = {
                notifyUserInteraction()
                onFullscreenToggle()
              },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = "Fullscreen",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        // CENTER OVERLAY CONTROLS: -10s, Play/Pause, +10s
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(24.dp),
          modifier = Modifier.align(Alignment.Center)
        ) {
          // -10s Rewind
          IconButton(
            onClick = {
              notifyUserInteraction()
              if (party.isHost) engine.rewind10()
              else onNonHostAction()
            },
            modifier = Modifier
              .size(44.dp)
              .background(Color.Black.copy(alpha = 0.45f), CircleShape)
          ) {
            Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White)
          }

          // Play / Pause Toggle
          FilledIconButton(
            onClick = {
              notifyUserInteraction()
              if (party.isHost) engine.togglePlayPause()
              else onNonHostAction()
            },
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = IndigoPrimary),
            modifier = Modifier.size(56.dp)
          ) {
            Icon(
              imageVector = if (party.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (party.isPlaying) "Pause" else "Play",
              tint = Color.White,
              modifier = Modifier.size(30.dp)
            )
          }

          // +10s Forward
          IconButton(
            onClick = {
              notifyUserInteraction()
              if (party.isHost) engine.forward10()
              else onNonHostAction()
            },
            modifier = Modifier
              .size(44.dp)
              .background(Color.Black.copy(alpha = 0.45f), CircleShape)
          ) {
            Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White)
          }
        }

        // BOTTOM OVERLAY CONTROLS (Scrubber & Actions)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = formatTime(party.positionSec.toInt()),
              color = Color.White,
              fontSize = 10.sp,
              modifier = Modifier.width(40.dp)
            )

            Slider(
              value = party.positionSec,
              onValueChange = { newPos ->
                notifyUserInteraction()
                if (party.isHost) {
                  engine.seekVideo(newPos)
                } else {
                  onNonHostAction()
                }
              },
              valueRange = 0f..party.durationSec.toFloat().coerceAtLeast(1f),
              colors = SliderDefaults.colors(
                thumbColor = IndigoLight,
                activeTrackColor = IndigoPrimary,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
              ),
              modifier = Modifier.weight(1f)
            )

            Text(
              text = formatTime(party.durationSec),
              color = Color.White,
              fontSize = 10.sp,
              textAlign = TextAlign.End,
              modifier = Modifier.width(40.dp)
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            // Local Mute Toggle
            IconButton(
              onClick = {
                notifyUserInteraction()
                isLocalMuted = !isLocalMuted
              },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                imageVector = if (isLocalMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = "Volume",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }

            // Queue & Skip to next
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (party.playlist.size > 1) {
                IconButton(
                  onClick = {
                    notifyUserInteraction()
                    if (party.isHost) engine.playNextVideo()
                    else onNonHostAction()
                  },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.SkipNext, contentDescription = "Next Video", tint = Color.White, modifier = Modifier.size(20.dp))
                }
              }

              IconButton(
                onClick = {
                  notifyUserInteraction()
                  onOpenQueue()
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.QueueMusic, contentDescription = "Queue", tint = IndigoLight, modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }
  }

  // PLAYBACK SPEED SELECTION DIALOG
  if (showSpeedDialog) {
    AlertDialog(
      onDismissRequest = { showSpeedDialog = false },
      containerColor = DarkSurfaceCard,
      title = { Text("Playback Speed", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (party.playbackSpeed == speed) IndigoPrimary.copy(alpha = 0.3f) else DarkSurfaceElevated,
              border = BorderStroke(1.dp, if (party.playbackSpeed == speed) IndigoPrimary else Color.Transparent),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  engine.setPlaybackSpeed(speed)
                  showSpeedDialog = false
                }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
              ) {
                Text(
                  text = "${speed}x" + if (speed == 1.0f) " (Normal)" else "",
                  color = Color.White,
                  fontSize = 13.sp,
                  fontWeight = if (party.playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                )
                if (party.playbackSpeed == speed) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldOnline, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSpeedDialog = false }) {
          Text("Close", color = TextSecondary)
        }
      }
    )
  }

  // AUDIO TRACK SELECTION DIALOG
  if (showAudioTrackDialog) {
    AlertDialog(
      onDismissRequest = { showAudioTrackDialog = false },
      containerColor = DarkSurfaceCard,
      title = { Text("Select Audio Track", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          availableAudioTracks.forEach { item ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (item.isSelected) IndigoPrimary.copy(alpha = 0.3f) else DarkSurfaceElevated,
              border = BorderStroke(1.dp, if (item.isSelected) IndigoPrimary else Color.Transparent),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  val groups = exoPlayer.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                  if (item.groupIndex in groups.indices) {
                    val targetGroup = groups[item.groupIndex]
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                      .buildUpon()
                      .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                      .setOverrideForType(TrackSelectionOverride(targetGroup.mediaTrackGroup, listOf(item.trackIndex)))
                      .build()
                  }
                  showAudioTrackDialog = false
                }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
              ) {
                Text(
                  text = item.label,
                  color = Color.White,
                  fontSize = 13.sp,
                  fontWeight = if (item.isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (item.isSelected) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldOnline, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showAudioTrackDialog = false }) {
          Text("Close", color = TextSecondary)
        }
      }
    )
  }
}

// -------------------------------------------------------------
// WATCH PARTY QUEUE & PLAYLIST BOTTOM SHEET
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchPartyQueueSheet(
  party: WatchPartyRoom,
  isHost: Boolean,
  onDismiss: () -> Unit,
  onPlayItem: (Int) -> Unit,
  onRemoveItem: (Int) -> Unit,
  onMoveUp: (Int) -> Unit,
  onMoveDown: (Int) -> Unit,
  onAddVideos: () -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = DarkSurface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      // Header & Add Videos Action
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Text(
            text = "Watch Party Queue",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${party.playlist.size} video(s) in playlist",
            color = TextSecondary,
            fontSize = 11.sp
          )
        }

        if (isHost) {
          Button(
            onClick = onAddVideos,
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.testTag("add_videos_to_queue_btn")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Videos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Playlist Items
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        itemsIndexed(party.playlist) { index, item ->
          val isCurrent = index == party.currentQueueIndex
          val isNext = index == party.currentQueueIndex + 1

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isCurrent) IndigoPrimary.copy(alpha = 0.25f) else DarkSurfaceElevated,
            border = BorderStroke(
              1.dp,
              if (isCurrent) IndigoPrimary else DarkSurfaceBorder
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable(enabled = isHost) { onPlayItem(index) }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(10.dp)
            ) {
              // Thumbnail / Icon
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(38.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isCurrent) IndigoPrimary else DarkSurfaceCard)
              ) {
                Text(if (item.mediaType == StreamMediaType.VIDEO) "🎬" else "🎵", fontSize = 16.sp)
              }

              Spacer(modifier = Modifier.width(10.dp))

              // Title and Info
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  if (isCurrent) {
                    Surface(
                      color = IndigoLight.copy(alpha = 0.2f),
                      shape = RoundedCornerShape(4.dp)
                    ) {
                      Text(
                        text = "NOW PLAYING",
                        color = IndigoLight,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                      )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                  } else if (isNext) {
                    Surface(
                      color = EmeraldOnline.copy(alpha = 0.2f),
                      shape = RoundedCornerShape(4.dp)
                    ) {
                      Text(
                        text = "UP NEXT",
                        color = EmeraldOnline,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                      )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                  }
                  Text(
                    text = "${item.resolution} • ${formatTime(item.durationSec)}",
                    color = TextSecondary,
                    fontSize = 10.sp
                  )
                }
              }

              // Host Queue Controls: Move Up, Move Down, Delete
              if (isHost) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  if (index > 0) {
                    IconButton(
                      onClick = { onMoveUp(index) },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }
                  }
                  if (index < party.playlist.lastIndex) {
                    IconButton(
                      onClick = { onMoveDown(index) },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }
                  }
                  if (party.playlist.size > 1) {
                    IconButton(
                      onClick = { onRemoveItem(index) },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Color(0xFFFF5252), modifier = Modifier.size(15.dp))
                    }
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

// -------------------------------------------------------------
// MUSIC / AUDIO VISUALIZER STAGE
// -------------------------------------------------------------
@Composable
fun MusicVisualizerStage(
  party: WatchPartyRoom,
  isPlaying: Boolean,
  audioAmplitude: Float
) {
  val media = party.activeMedia

  val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(if (isPlaying) 6000 else Int.MAX_VALUE, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "spin_angle"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp)
      .testTag("music_visualizer_stage")
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(130.dp)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            listOf(Color(0xFF2A2A2A), Color(0xFF111111), Color.Black)
          )
        )
        .border(3.dp, PurpleAccent, CircleShape)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .background(Brush.linearGradient(listOf(IndigoPrimary, PurpleAccent)))
      ) {
        Text("🎵", fontSize = 24.sp)
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = media.title,
      color = Color.White,
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center
    )
    Text(
      text = "${media.resolution} • ${media.codec} • ${media.formattedSize}",
      color = TextSecondary,
      fontSize = 12.sp
    )

    Spacer(modifier = Modifier.height(20.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.Bottom,
      modifier = Modifier.height(44.dp)
    ) {
      val barHeights = if (isPlaying) listOf(14, 28, 42, 35, 20, 38, 44, 24, 30, 16, 32, 22) else listOf(6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6)
      barHeights.forEach { h ->
        Box(
          modifier = Modifier
            .width(6.dp)
            .height(h.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Brush.verticalGradient(listOf(PurpleAccent, IndigoLight)))
        )
      }
    }
  }
}

private fun formatTime(sec: Int): String {
  val h = sec / 3600
  val m = (sec % 3600) / 60
  val s = sec % 60
  return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
