package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun OwnerBadge(modifier: Modifier = Modifier) {
  Surface(
    color = GoldOwnerContainer,
    shape = RoundedCornerShape(6.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, GoldOwner),
    modifier = modifier.testTag("owner_badge")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
      Text(
        text = "👑",
        fontSize = 10.sp,
        modifier = Modifier.padding(end = 3.dp)
      )
      Text(
        text = "OWNER",
        color = GoldOwner,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
fun AdminBadge(modifier: Modifier = Modifier) {
  Surface(
    color = Color(0xFF261D3D),
    shape = RoundedCornerShape(6.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, ShieldAdmin),
    modifier = modifier.testTag("admin_badge")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
      Text(
        text = "🛡️",
        fontSize = 10.sp,
        modifier = Modifier.padding(end = 3.dp)
      )
      Text(
        text = "ADMIN",
        color = ShieldAdmin,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
fun RoomLevelBadge(level: Int, modifier: Modifier = Modifier) {
  Surface(
    color = PurpleContainer,
    shape = RoundedCornerShape(8.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent),
    modifier = modifier.testTag("room_level_badge")
  ) {
    Text(
      text = "LVL $level",
      color = Color.White,
      fontSize = 11.sp,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    )
  }
}

@Composable
fun XPProgressBar(
  currentXp: Int,
  maxThreshXp: Int,
  modifier: Modifier = Modifier,
  showLabels: Boolean = true
) {
  val progress = (currentXp.toFloat() / maxThreshXp.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

  Column(modifier = modifier) {
    if (showLabels) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "XP: ${String.format("%,d", currentXp)} / ${String.format("%,d", maxThreshXp)}",
          fontSize = 11.sp,
          color = AmberXp,
          fontWeight = FontWeight.SemiBold
        )
        val needed = (maxThreshXp - currentXp).coerceAtLeast(0)
        Text(
          text = "${String.format("%,d", needed)} needed",
          fontSize = 11.sp,
          color = TextSecondary
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(DarkSurfaceElevated)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(progress)
          .fillMaxHeight()
          .clip(RoundedCornerShape(4.dp))
          .background(
            Brush.horizontalGradient(
              listOf(BrandPrimary, MagentaAccent, AmberXp)
            )
          )
      )
    }
  }
}

@Composable
fun SpeakingAvatar(
  avatarName: String,
  username: String,
  isSpeaking: Boolean,
  isMuted: Boolean,
  isOwner: Boolean,
  isAdmin: Boolean,
  size: Dp = 48.dp,
  onClick: (() -> Unit)? = null
) {
  val infiniteTransition = rememberInfiniteTransition(label = "speak_anim")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = if (isSpeaking) 1.14f else 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = if (isSpeaking) 0.6f else 0.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(500, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow_alpha"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clickable(enabled = onClick != null) { onClick?.invoke() }
      .testTag("avatar_${username}")
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(size + 14.dp)
    ) {
      // Subtle animated Electric Violet / Cyan ring when speaking (only while speaking)
      if (isSpeaking) {
        Box(
          modifier = Modifier
            .size(size + 12.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .border(2.dp, BrandPrimary.copy(alpha = glowAlpha), CircleShape)
            .background(BrandPrimary.copy(alpha = 0.12f))
        )
      }

      // Base Avatar Box
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(size)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              listOf(
                Color(0xFF271C3E),
                Color(0xFF131322)
              )
            )
          )
          .border(
            width = if (isOwner) 2.dp else if (isAdmin) 1.5.dp else 1.dp,
            color = if (isOwner) GoldOwner else if (isAdmin) ShieldAdmin else DarkSurfaceBorder,
            shape = CircleShape
          )
      ) {
        Text(
          text = getInitials(username),
          fontSize = (size.value * 0.35f).sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }

      // Owner or Admin Icon overlay
      if (isOwner) {
        Text(
          text = "👑",
          fontSize = 12.sp,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 2.dp, y = (-2).dp)
        )
      } else if (isAdmin) {
        Text(
          text = "🛡️",
          fontSize = 11.sp,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 2.dp, y = (-2).dp)
        )
      }

      // Clean muted icon (subtle dark badge, avoiding loud red UI)
      if (isMuted) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(17.dp)
            .align(Alignment.BottomEnd)
            .clip(CircleShape)
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, CircleShape)
        ) {
          Icon(
            imageVector = Icons.Default.MicOff,
            contentDescription = "Muted",
            tint = TextMuted,
            modifier = Modifier.size(10.dp)
          )
        }
      }
    }

    Text(
      text = username,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      color = if (isSpeaking) BrandVioletLight else TextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      modifier = Modifier.widthIn(max = size + 20.dp)
    )
  }
}

@Composable
fun ProfileFrameAvatar(
  username: String,
  frame: String,
  isOnline: Boolean,
  size: Dp = 64.dp,
  onClick: (() -> Unit)? = null
) {
  val frameBorderBrush = when (frame) {
    "Neon Purple" -> Brush.sweepGradient(listOf(BrandPrimary, MagentaAccent, BrandPrimary))
    "Cosmic" -> Brush.sweepGradient(listOf(CyanAccent, BrandVioletLight, MagentaAccent, CyanAccent))
    "Gold VIP" -> Brush.sweepGradient(listOf(GoldOwner, Color(0xFFFDE047), GoldOwner, Color(0xFFD97706)))
    else -> Brush.sweepGradient(listOf(BrandPrimary.copy(alpha = 0.5f), CyanAccent.copy(alpha = 0.5f), BrandPrimary.copy(alpha = 0.5f)))
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .size(size + 8.dp)
      .clickable(enabled = onClick != null) { onClick?.invoke() }
      .testTag("profile_avatar_${username}")
  ) {
    // Frame ring
    Box(
      modifier = Modifier
        .size(size + 6.dp)
        .clip(CircleShape)
        .border(2.5.dp, frameBorderBrush, CircleShape)
    )

    // Inner avatar
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            listOf(Color(0xFF261C3D), Color(0xFF141322))
          )
        )
    ) {
      Text(
        text = getInitials(username),
        fontSize = (size.value * 0.38f).sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }

    // Online/Offline status dot
    Box(
      modifier = Modifier
        .size(14.dp)
        .align(Alignment.BottomEnd)
        .offset(x = (-2).dp, y = (-2).dp)
        .clip(CircleShape)
        .background(if (isOnline) EmeraldOnline else TextMuted)
        .border(2.dp, DarkBackground, CircleShape)
    )
  }
}

@Composable
fun GuestVerificationReminderBanner(
  onVerifyClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1738)),
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("guest_verification_banner")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(14.dp)
    ) {
      Text(
        text = "📱",
        fontSize = 28.sp,
        modifier = Modifier.padding(end = 12.dp)
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Protect Your Guest Account",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = "Link your mobile number via OTP. Your User ID, rooms, XP & friends will be permanently preserved!",
          fontSize = 12.sp,
          color = TextSecondary
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Button(
        onClick = onVerifyClick,
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.testTag("verify_now_btn")
      ) {
        Text(
          text = "Verify",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }
  }
}

@Composable
fun EmptyStateView(
  icon: ImageVector,
  title: String,
  subtitle: String,
  actionText: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 4.dp, vertical = 8.dp)
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              listOf(
                BrandPrimary.copy(alpha = 0.22f),
                DarkSurfaceElevated
              )
            )
          )
          .border(1.dp, DarkCardBorderSubtle, CircleShape)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = BrandVioletLight,
          modifier = Modifier.size(32.dp)
        )
      }
      Spacer(modifier = Modifier.height(18.dp))
      Text(
        text = title,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = subtitle,
        fontSize = 13.sp,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 19.sp
      )
      if (actionText != null && onAction != null) {
        Spacer(modifier = Modifier.height(20.dp))
        Button(
          onClick = onAction,
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
          modifier = Modifier.testTag("empty_state_action_btn")
        ) {
          Text(text = actionText, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
        }
      }
    }
  }
}

@Composable
fun LiveStatusPill(modifier: Modifier = Modifier) {
  val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
  val dotAlpha by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(750, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "live_dot_alpha"
  )

  Surface(
    color = EmeraldOnline.copy(alpha = 0.14f),
    shape = RoundedCornerShape(8.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldOnline.copy(alpha = 0.35f)),
    modifier = modifier.testTag("live_status_pill")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .clip(CircleShape)
          .background(EmeraldOnline.copy(alpha = dotAlpha))
      )
      Spacer(modifier = Modifier.width(4.5.dp))
      Text(
        text = "LIVE",
        color = EmeraldOnline,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp
      )
    }
  }
}

fun getInitials(name: String): String {

  if (name.isBlank()) return "?"
  val parts = name.trim().split(" ", "_")
  return if (parts.size >= 2) {
    "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
  } else {
    name.take(2).uppercase()
  }
}
