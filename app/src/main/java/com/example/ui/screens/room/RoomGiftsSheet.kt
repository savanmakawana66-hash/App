package com.example.ui.screens.room

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalRoomTheme

data class RoomGiftItem(
  val id: String,
  val name: String,
  val emoji: String,
  val cost: Int,
  val description: String
)

val ROOM_GIFTS = listOf(
  RoomGiftItem("gift_rose", "Rose", "🌹", 10, "Sweet & friendly"),
  RoomGiftItem("gift_popcorn", "Popcorn", "🍿", 20, "Movie party mood"),
  RoomGiftItem("gift_music", "Music Beat", "🎵", 30, "For great tunes"),
  RoomGiftItem("gift_fire", "Neon Flame", "🔥", 50, "Stage is on fire"),
  RoomGiftItem("gift_rocket", "Cosmic Rocket", "🚀", 100, "To the moon"),
  RoomGiftItem("gift_diamond", "Diamond", "💎", 200, "Sparkling gem"),
  RoomGiftItem("gift_crown", "Royal Crown", "👑", 500, "Royalty VIP"),
  RoomGiftItem("gift_star", "Super Nova", "🌟", 1000, "Legendary room star")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomGiftsSheet(
  userCoins: Int,
  onDismiss: () -> Unit,
  onSendGift: (name: String, emoji: String, cost: Int) -> Boolean
) {
  val theme = LocalRoomTheme.current
  var selectedGift by remember { mutableStateOf<RoomGiftItem?>(ROOM_GIFTS.first()) }
  var feedbackMessage by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF10131F),
    tonalElevation = 8.dp,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    modifier = Modifier.testTag("room_gifts_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .navigationBarsPadding()
    ) {
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "🎁",
            fontSize = 22.sp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Send Room Gift",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Support the voice room & level up together",
              fontSize = 11.sp,
              color = Color.White.copy(alpha = 0.6f)
            )
          }
        }

        // Wallet Balance Pill
        Surface(
          color = Color(0xFF1E2338),
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
          modifier = Modifier.testTag("gift_wallet_balance_pill")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Text("🪙", fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "$userCoins",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFF59E0B)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 4x2 Gifts Grid
      LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(ROOM_GIFTS, key = { it.id }) { gift ->
          val isSelected = selectedGift?.id == gift.id
          val canAfford = userCoins >= gift.cost

          Surface(
            color = if (isSelected) {
              theme.primaryAccent.copy(alpha = 0.22f)
            } else {
              Color(0xFF161928)
            },
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
              width = if (isSelected) 2.dp else 1.dp,
              color = if (isSelected) theme.primaryAccent else Color.White.copy(alpha = 0.1f)
            ),
            modifier = Modifier
              .clickable {
                selectedGift = gift
                feedbackMessage = null
              }
              .testTag("gift_item_${gift.id}")
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
              modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
              Text(
                text = gift.emoji,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = gift.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(2.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Text(
                  text = "${gift.cost} 🪙",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (canAfford) Color(0xFFFBBF24) else Color(0xFFF43F5E)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      feedbackMessage?.let { msg ->
        Text(
          text = msg,
          fontSize = 12.sp,
          color = Color(0xFFF43F5E),
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(bottom = 8.dp)
        )
      }

      // Send Button
      selectedGift?.let { gift ->
        val canAfford = userCoins >= gift.cost
        Button(
          onClick = {
            if (!canAfford) {
              feedbackMessage = "Not enough coins! Need ${gift.cost} Coins."
            } else {
              val success = onSendGift(gift.name, gift.emoji, gift.cost)
              if (success) {
                onDismiss()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (canAfford) theme.primaryAccent else Color(0xFF2C1A24)
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("send_gift_confirm_btn")
        ) {
          Text(
            text = if (canAfford) "Send ${gift.emoji} ${gift.name} (${gift.cost} 🪙)" else "Need ${gift.cost} Coins (Balance: $userCoins)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (canAfford) Color.White else Color.White.copy(alpha = 0.5f)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
