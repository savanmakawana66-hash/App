package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmojiCategory
import com.example.data.model.PlayRoomEmoji
import com.example.data.model.PlayRoomEmojiRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayRoomEmojiPickerSheet(
  userLevel: Int = 1,
  onEmojiSelected: (PlayRoomEmoji) -> Unit,
  onDismiss: () -> Unit
) {
  var selectedCategory by remember { mutableStateOf(EmojiCategory.HAPPY) }
  val emojis = remember(selectedCategory) {
    PlayRoomEmojiRegistry.getByCategory(selectedCategory)
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF111422),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(vertical = 10.dp)
          .width(42.dp)
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(Color.White.copy(alpha = 0.3f))
      )
    },
    modifier = Modifier.testTag("play_room_emoji_picker")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 24.dp)
    ) {
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("✨", fontSize = 18.sp)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Play Room Emojis",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            color = Color(0xFF8B5CF6).copy(alpha = 0.25f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF8B5CF6))
          ) {
            Text(
              text = "ORIGINAL",
              color = Color(0xFFA78BFA),
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Category Tabs
      ScrollableTabRow(
        selectedTabIndex = selectedCategory.ordinal,
        containerColor = Color.Transparent,
        contentColor = Color(0xFF8B5CF6),
        edgePadding = 0.dp,
        divider = {}
      ) {
        EmojiCategory.values().forEach { cat ->
          val isSelected = cat == selectedCategory
          Tab(
            selected = isSelected,
            onClick = { selectedCategory = cat },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cat.icon, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = cat.title,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.6f)
                )
              }
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Emoji Grid (4 columns)
      LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(260.dp)
      ) {
        items(emojis, key = { it.code }) { emoji ->
          val isLocked = emoji.isExclusive && userLevel < emoji.unlockLevel

          // Pulsing scale for animated emojis
          val infiniteTransition = rememberInfiniteTransition(label = "pulse")
          val scaleAnim by infiniteTransition.animateFloat(
            initialValue = if (emoji.isAnimated) 0.95f else 1.0f,
            targetValue = if (emoji.isAnimated) 1.08f else 1.0f,
            animationSpec = infiniteRepeatable(
              animation = tween(800, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse
            ),
            label = "emoji_scale"
          )

          Surface(
            color = if (isLocked) Color(0xFF1E2235).copy(alpha = 0.4f) else Color(0xFF1A1E30),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
              1.dp,
              if (emoji.isExclusive) Color(0xFFF59E0B).copy(alpha = 0.6f)
              else Color.White.copy(alpha = 0.12f)
            ),
            modifier = Modifier
              .aspectRatio(1f)
              .clip(RoundedCornerShape(14.dp))
              .clickable(enabled = !isLocked) {
                onEmojiSelected(emoji)
              }
              .testTag("emoji_item_${emoji.code.replace(":", "")}")
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.fillMaxSize().padding(4.dp)
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Text(
                  text = emoji.visualSymbol,
                  fontSize = 24.sp,
                  modifier = Modifier.scale(if (emoji.isAnimated) scaleAnim else 1.0f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = emoji.name,
                  color = if (isLocked) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.85f),
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Medium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = TextAlign.Center
                )
              }

              // Badges: Exclusive VIP / Level Lock or Animated Pulse
              if (isLocked) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Lvl ${emoji.unlockLevel}", color = Color(0xFFF59E0B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  }
                }
              } else if (emoji.isAnimated) {
                Box(
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Text renderer that seamlessly parses Play Room custom emoji codes (e.g. :pr_heart_flame:)
 * and presents them with custom styled badge pills.
 */
@Composable
fun RenderMessageWithCustomEmojis(
  text: String,
  textColor: Color = Color.White,
  fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
  modifier: Modifier = Modifier
) {
  val hasCustomEmoji = remember(text) { PlayRoomEmojiRegistry.containsCustomEmoji(text) }

  if (!hasCustomEmoji) {
    Text(
      text = text,
      color = textColor,
      fontSize = fontSize,
      modifier = modifier
    )
  } else {
    // Break up into tokens
    val codes = remember(text) { PlayRoomEmojiRegistry.extractEmojiCodes(text) }
    var current = text
    codes.forEach { code ->
      val emoji = PlayRoomEmojiRegistry.findByCode(code)
      if (emoji != null) {
        current = current.replace(code, " ${emoji.visualSymbol} ")
      }
    }

    Text(
      text = current.trim(),
      color = textColor,
      fontSize = fontSize,
      modifier = modifier
    )
  }
}
