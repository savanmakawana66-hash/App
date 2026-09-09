package com.example.ui.screens.watchparty

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import com.example.data.model.*
import com.example.network.WatchPartyNetworkEngine
import com.example.network.webrtc.*
import com.example.ui.theme.*
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchPartyStageScreen(
  engine: WatchPartyNetworkEngine,
  party: WatchPartyRoom,
  onLeave: () -> Unit
) {
  val context = LocalContext.current
  val isVoiceMuted by engine.voiceRoomManager.isMuted.collectAsState()
  val isInVoice by engine.voiceRoomManager.isInVoice.collectAsState()
  val isLocalSpeaking by engine.voiceRoomManager.isLocalSpeaking.collectAsState()
  val audioAmplitude by engine.voiceRoomManager.audioAmplitude.collectAsState()

  // WebRTC Reactive State
  val webRtcStats by engine.webRtcManager.aggregateStats.collectAsState()
  val webRtcSyncMetrics by engine.webRtcManager.syncMetrics.collectAsState()
  val webRtcConnState by engine.webRtcManager.connectionState.collectAsState()
  val webRtcDataChannelState by engine.webRtcManager.dataChannelState.collectAsState()
  var showWebRtcHud by remember { mutableStateOf(false) }

  var showParticipantsSheet by remember { mutableStateOf(false) }
  var showQualityMenu by remember { mutableStateOf(false) }
  var floatingReactions by remember { mutableStateOf(listOf<String>()) }

  // Listen to remote WebRTC reaction bursts
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

  // Reaction burst trigger: local visual + WebRTC broadcast
  fun triggerReaction(emoji: String) {
    floatingReactions = floatingReactions + emoji
    engine.webRtcManager.broadcastReaction(emoji, party.hostName)
  }

  Scaffold(
    containerColor = DarkBackground,
    topBar = {
      // -------------------------------------------------------------
      // TOP BAR: ROOM CODE, COPY/SHARE, SYNC PILL & LEAVE
      // -------------------------------------------------------------
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
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = IndigoLight, modifier = Modifier.size(12.dp))
              }
            }
          }
        },
        actions = {
          // Share Room Code button (System Share Sheet)
          IconButton(
            onClick = {
              val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                  Intent.EXTRA_TEXT,
                  "Join my Watch Party '${party.name}'! Use Room Code: ${party.code}"
                )
                type = "text/plain"
              }
              val shareIntent = Intent.createChooser(sendIntent, "Invite friends to Watch Party")
              context.startActivity(shareIntent)
            },
            modifier = Modifier.testTag("share_room_code_btn")
          ) {
            Icon(Icons.Default.Share, contentDescription = "Share Code", tint = TextSecondary)
          }

          // WebRTC Diagnostics HUD Button
          IconButton(
            onClick = { showWebRtcHud = true },
            modifier = Modifier.testTag("webrtc_diagnostics_hud_btn")
          ) {
            Icon(Icons.Default.Bolt, contentDescription = "WebRTC Diagnostics", tint = Color(0xFF00E5FF))
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
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // -------------------------------------------------------------
      // STATUS STRIP: CONNECTION STATE, QUALITY & METRICS
      // -------------------------------------------------------------
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
          // Connection State Pill (🟢 Connected, 🟡 Reconnecting, etc.)
          Row(verticalAlignment = Alignment.CenterVertically) {
            val statusColor = when (party.connectionState) {
              PartyConnectionState.CONNECTED -> EmeraldOnline
              PartyConnectionState.RECONNECTING -> AmberXp
              PartyConnectionState.WAITING_FOR_HOST -> Color(0xFFFF9800)
              PartyConnectionState.SYNCING -> IndigoLight
              PartyConnectionState.DISCONNECTED -> Color(0xFFFF5252)
            }
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = party.connectionState.label,
              color = statusColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // WebRTC P2P Status Pill
          Surface(
            color = DarkSurfaceCard,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
            modifier = Modifier
              .clickable { showWebRtcHud = true }
              .testTag("status_strip_webrtc_pill")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${webRtcStats.formattedRtt} • ${webRtcSyncMetrics.driftMs}ms drift",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Quality Dropdown & Live Bitrate Pill
          Row(verticalAlignment = Alignment.CenterVertically) {
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

            Spacer(modifier = Modifier.width(10.dp))

            // Data Rate & Total Streamed MB
            Text(
              text = "📶 ${(party.streamBitrateKbps / 8000f).let { String.format("%.1f", it) }} MB/s",
              color = TextSecondary,
              fontSize = 10.sp
            )
          }
        }
      }

      // Reconnection or Interruption Warning Banner
      if (party.connectionState != PartyConnectionState.CONNECTED) {
        Surface(
          color = AmberXp.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, AmberXp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
          ) {
            Text(party.connectionState.emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = party.connectionState.label,
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      // -------------------------------------------------------------
      // MEDIA STAGE: VIDEO PLAYER OR MUSIC VISUALIZER
      // -------------------------------------------------------------
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .background(Color.Black)
      ) {
        if (party.activeMedia.mediaType == StreamMediaType.AUDIO) {
          // MUSIC / AUDIO VISUALIZER STAGE
          MusicVisualizerStage(
            party = party,
            isPlaying = party.isPlaying,
            audioAmplitude = if (isLocalSpeaking) audioAmplitude else 0.4f
          )
        } else {
          // VIDEO PLAYER STAGE
          VideoPlayerStage(
            party = party,
            isPlaying = party.isPlaying
          )
        }

        // WebRTC Live Stream Indicator Overlay
        Surface(
          color = Color.Black.copy(alpha = 0.65f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
          modifier = Modifier
            .padding(12.dp)
            .align(Alignment.TopStart)
            .clickable { showWebRtcHud = true }
            .testTag("webrtc_live_badge")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(EmeraldOnline)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "⚡ WebRTC P2P • ${webRtcStats.formattedRtt} • ${webRtcStats.resolution}",
              color = Color(0xFF00E5FF),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Floating reaction animations
        Row(
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(bottom = 60.dp)
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

      // -------------------------------------------------------------
      // SYNCHRONIZED PLAYBACK CONTROLS (Play/Pause, Scrub, Skip)
      // -------------------------------------------------------------
      Surface(
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          // Progress Slider
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
                engine.seekVideo(newPos)
              },
              valueRange = 0f..party.durationSec.toFloat(),
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

          // Main Playback Buttons
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
          ) {
            // -10s Rewind
            IconButton(
              onClick = { engine.rewind10() },
              modifier = Modifier.testTag("rewind_10_btn")
            ) {
              Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White)
            }

            // Play / Pause Master Toggle
            FilledIconButton(
              onClick = { engine.togglePlayPause() },
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
              onClick = { engine.forward10() },
              modifier = Modifier.testTag("forward_10_btn")
            ) {
              Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White)
            }
          }
        }
      }

      // -------------------------------------------------------------
      // VOICE ROOM STRIP & PARTICIPANT AVATARS
      // -------------------------------------------------------------
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
              // Mic Mute Button
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

              // Participants List button
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

      // -------------------------------------------------------------
      // FOOTER: REACTION BURST BAR & TEST NETWORK SIMULATION BUTTON
      // -------------------------------------------------------------
      Surface(
        color = DarkSurface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          // Floating Emoji Reactions
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

          // Test Network Interruption & Recovery Button (Requirement 7 & 12)
          Button(
            onClick = { engine.testNetworkInterruption() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.testTag("test_network_interruption_btn")
          ) {
            Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = AmberXp, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Test Disconnect / Sync",
              color = TextSecondary,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }
  }

  // -------------------------------------------------------------
  // PARTICIPANTS SHEET MODAL
  // -------------------------------------------------------------
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
            text = "Total Streamed: ${(party.totalStreamedBytes / (1024 * 1024))} MB",
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
                // Avatar
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
                    text = "Ping: ${p.pingMs}ms • Streamed: ${p.formattedDataUsage}",
                    color = TextSecondary,
                    fontSize = 11.sp
                  )
                }

                // Mic State
                Icon(
                  imageVector = if (p.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                  contentDescription = null,
                  tint = if (p.isMuted) Color(0xFFFF5252) else EmeraldOnline,
                  modifier = Modifier.size(18.dp)
                )

                // Host Kick Button (Host can remove participants)
                if (party.isHost && !p.isHost) {
                  Spacer(modifier = Modifier.width(10.dp))
                  IconButton(
                    onClick = {
                      engine.kickParticipant(p.id)
                    },
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

  // -------------------------------------------------------------
  // WEBRTC PEERCONNECTION DIAGNOSTICS & METRICS HUD
  // -------------------------------------------------------------
  if (showWebRtcHud) {
    WebRtcDiagnosticsModalSheet(
      stats = webRtcStats,
      syncMetrics = webRtcSyncMetrics,
      connectionState = webRtcConnState,
      dataChannelState = webRtcDataChannelState,
      isHost = party.isHost,
      onDismiss = { showWebRtcHud = false },
      onTriggerNtpSync = {
        engine.webRtcManager.broadcastSeek(party.positionSec)
      }
    )
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

      // Mic muted badge
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
// VIDEO PLAYER STAGE
// -------------------------------------------------------------
@Composable
fun VideoPlayerStage(party: WatchPartyRoom, isPlaying: Boolean) {
  val media = party.activeMedia

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxSize()
      .testTag("video_player_stage")
  ) {
    // Cinematic background gradient
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            listOf(IndigoDark.copy(alpha = 0.4f), Color.Black)
          )
        )
    )

    // Center Video Stage representation
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(24.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(88.dp)
          .clip(CircleShape)
          .background(IndigoPrimary.copy(alpha = 0.25f))
          .border(2.dp, IndigoLight, CircleShape)
      ) {
        Icon(
          imageVector = if (isPlaying) Icons.Default.Movie else Icons.Default.Pause,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(44.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = media.title,
        color = Color.White,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(6.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
          color = DarkSurfaceCard,
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
          Text(
            text = media.resolution,
            color = IndigoLight,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }

        Surface(
          color = DarkSurfaceCard,
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
          Text(
            text = media.codec,
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }

        Surface(
          color = DarkSurfaceCard,
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
          Text(
            text = media.formattedSize,
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }
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

  // Vinyl rotation animation
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
    // Vinyl Record Artwork
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

    // Track Title & Codec
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

    // Animated EQ Frequency Bars
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

// -------------------------------------------------------------
// WEBRTC PEERCONNECTION DIAGNOSTICS & METRICS MODAL SHEET
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebRtcDiagnosticsModalSheet(
  stats: WebRtcStats,
  syncMetrics: WebRtcSyncMetrics,
  connectionState: WebRtcPeerConnectionState,
  dataChannelState: WebRtcDataChannelState,
  isHost: Boolean,
  onDismiss: () -> Unit,
  onTriggerNtpSync: () -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = DarkSurface,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .verticalScroll(rememberScrollState())
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
              .size(36.dp)
              .clip(CircleShape)
              .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
          ) {
            Icon(
              imageVector = Icons.Default.Bolt,
              contentDescription = null,
              tint = Color(0xFF00E5FF),
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "WebRTC Diagnostics & Stats",
              color = Color.White,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = if (isHost) "Host Broadcaster • Low-Latency Stream" else "Remote Participant • Sub-Frame Sync",
              color = TextSecondary,
              fontSize = 12.sp
            )
          }
        }

        Surface(
          color = EmeraldOnline.copy(alpha = 0.2f),
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, EmeraldOnline)
        ) {
          Text(
            text = connectionState.label,
            color = EmeraldOnline,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Section 1: Real-time Latency & Jitter
      Text(
        text = "NETWORK & LATENCY (SRTP)",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        DiagnosticMetricCard(
          label = "RTT / Ping",
          value = stats.formattedRtt,
          subtext = "Sub-20ms ultra low latency",
          tint = Color(0xFF00E5FF),
          modifier = Modifier.weight(1f)
        )
        DiagnosticMetricCard(
          label = "Jitter",
          value = "${stats.jitterMs} ms",
          subtext = "Stable packet arrival",
          tint = EmeraldOnline,
          modifier = Modifier.weight(1f)
        )
        DiagnosticMetricCard(
          label = "Packet Loss",
          value = "${stats.packetLossPercent}%",
          subtext = "SRTP error recovery active",
          tint = IndigoLight,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Section 2: Sub-frame Synchronization & NTP Clock
      Text(
        text = "SUB-FRAME CLOCK SYNCHRONIZATION",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column {
              Text(
                text = "Sync Drift",
                color = TextSecondary,
                fontSize = 12.sp
              )
              Text(
                text = stats.formattedDrift,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Surface(
              color = IndigoPrimary.copy(alpha = 0.25f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = syncMetrics.syncStatusMessage,
                color = IndigoLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = DarkSurfaceBorder)
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "NTP Clock Offset: ${syncMetrics.clockOffsetMs} ms",
              color = TextSecondary,
              fontSize = 11.sp
            )
            Text(
              text = "Micro-rate compensation: Active (0.98x - 1.02x)",
              color = Color(0xFF00E5FF),
              fontSize = 11.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Section 3: Media Transceivers (Video / Audio)
      Text(
        text = "MEDIA TRANSCEIVERS & STREAM QUALITY",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Video Track", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("${stats.resolution} • ${stats.frameRateFps} FPS", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
          Text(text = "Codec: ${stats.videoCodec} • Bitrate: ${stats.formattedBitrate}", color = TextSecondary, fontSize = 11.sp)

          Spacer(modifier = Modifier.height(8.dp))
          HorizontalDivider(color = DarkSurfaceBorder)
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Audio Track", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("48 kHz Stereo", color = EmeraldOnline, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
          Text(text = "Codec: ${stats.audioCodec} • Transceiver: SendRecv", color = TextSecondary, fontSize = 11.sp)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Section 4: SCTP DataChannel & ICE Topology
      Text(
        text = "SCTP DATACHANNEL & ICE TOPOLOGY",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("DataChannel: watch-party-sync-v1", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(dataChannelState.name, color = EmeraldOnline, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Active Pair: ${stats.iceCandidatePair}",
            color = TextSecondary,
            fontSize = 11.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Traffic: ${stats.bytesSent} B sent / ${stats.bytesReceived} B received (10Hz Beacon)",
            color = TextSecondary,
            fontSize = 11.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Action Buttons
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedButton(
          onClick = onTriggerNtpSync,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, Color(0xFF00E5FF)),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Trigger NTP Resync", color = Color(0xFF00E5FF), fontSize = 12.sp)
        }

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Text("Done", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun DiagnosticMetricCard(
  label: String,
  value: String,
  subtext: String,
  tint: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = DarkSurfaceCard,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, DarkSurfaceBorder),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(text = label, color = TextSecondary, fontSize = 11.sp)
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = value, color = tint, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = subtext, color = TextSecondary.copy(alpha = 0.8f), fontSize = 9.sp, maxLines = 1)
    }
  }
}

