package com.example.ui.components

import androidx.compose.animation.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Premium Virtual Coin Wallet Card for Profile screen.
 * Follows the midnight dark theme with subtle gold highlights for coin elements.
 */
@Composable
fun CoinWalletCard(
  walletState: CoinWalletState,
  onOpenDailyTasks: () -> Unit,
  onOpenCoinHistory: () -> Unit,
  onAddTestBonus: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val progress = (walletState.todayEarnings.toFloat() / walletState.dailyLimit.toFloat()).coerceIn(0f, 1f)
  val percent = (progress * 100).toInt()

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
    modifier = modifier
      .fillMaxWidth()
      .testTag("coin_wallet_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      // Header: 🪙 COINS Tag
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(Color(0xFF2A2004))
              .border(1.dp, AmberXp.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text("🪙", fontSize = 14.sp)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "COINS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD54F),
            letterSpacing = 1.sp
          )
        }

        // Daily limit badge
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (walletState.todayEarnings >= walletState.dailyLimit) Color(0xFF1E3A2F) else DarkSurfaceElevated,
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (walletState.todayEarnings >= walletState.dailyLimit) EmeraldOnline.copy(alpha = 0.5f) else DarkCardBorderSubtle
          )
        ) {
          Text(
            text = if (walletState.todayEarnings >= walletState.dailyLimit) "Daily Limit Reached ✓" else "Max 100 / Day",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (walletState.todayEarnings >= walletState.dailyLimit) EmeraldOnline else TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Balance Display
      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = String.format(Locale.US, "%,d", walletState.balance),
          fontSize = 32.sp,
          fontWeight = FontWeight.Black,
          color = Color.White,
          letterSpacing = (-0.5).sp,
          modifier = Modifier.testTag("coin_balance_text")
        )
        Text(
          text = "Coins",
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = TextSecondary,
          modifier = Modifier.padding(bottom = 4.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Today's Earning Progress
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Today's earning",
          fontSize = 12.sp,
          color = TextSecondary
        )
        Text(
          text = "${walletState.todayEarnings} / ${walletState.dailyLimit} Coins ($percent%)",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFFFD54F)
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Progress bar (styled as ████████████░░░░)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(DarkSurfaceElevated)
      ) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(progress)
            .clip(RoundedCornerShape(4.dp))
            .background(
              Brush.horizontalGradient(
                listOf(AmberXp, Color(0xFFFFD54F))
              )
            )
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Quick Actions: [ Daily Tasks ] [ Coin History ]
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Daily Tasks Button
        Button(
          onClick = onOpenDailyTasks,
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("daily_tasks_btn")
        ) {
          Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Daily Tasks", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Coin History Button
        OutlinedButton(
          onClick = onOpenCoinHistory,
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("coin_history_btn")
        ) {
          Icon(Icons.Default.History, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Coin History", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      // Optional test top-up helper
      if (onAddTestBonus != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          Text(
            text = "+600 Test Coins (for Room Creation)",
            fontSize = 11.sp,
            color = CyanAccent,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
              .clickable { onAddTestBonus() }
              .padding(4.dp)
              .testTag("add_test_coins_btn")
          )
        }
      }
    }
  }
}

/**
 * Daily Tasks Dialog showing the 5 eligible coin tasks, progress, and claim buttons.
 */
@Composable
fun DailyTasksDialog(
  walletState: CoinWalletState,
  onClaimTask: (String) -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 16.dp)
        .testTag("daily_tasks_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        // Dialog Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📋", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Daily Tasks",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Earn up to 100 Coins per day",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Pill Header
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = DarkSurfaceElevated,
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("🪙", fontSize = 16.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Today's Limit: ${walletState.todayEarnings} / ${walletState.dailyLimit}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
            Text(
              text = "Resets at 00:00 UTC",
              fontSize = 10.sp,
              color = TextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // List of 5 Tasks
        LazyColumn(
          modifier = Modifier.weight(1f, fill = false),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(walletState.tasks, key = { it.taskType.id }) { taskInfo ->
            DailyTaskRow(
              taskInfo = taskInfo,
              isDailyLimitReached = walletState.todayEarnings >= walletState.dailyLimit,
              onClaim = { onClaimTask(taskInfo.taskType.id) }
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Done", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun DailyTaskRow(
  taskInfo: DailyTaskInfo,
  isDailyLimitReached: Boolean,
  onClaim: () -> Unit
) {
  val task = taskInfo.taskType
  val progressFraction = (taskInfo.currentProgress.toFloat() / taskInfo.targetProgress.toFloat()).coerceIn(0f, 1f)

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = DarkSurface,
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Icon
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(DarkSurfaceElevated),
        contentAlignment = Alignment.Center
      ) {
        Text(task.icon, fontSize = 18.sp)
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Title, Description & Progress
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = task.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "+${task.rewardCoins} 🪙",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD54F)
          )
        }

        Text(
          text = task.description,
          fontSize = 11.sp,
          color = TextSecondary,
          maxLines = 1
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(DarkSurfaceElevated)
          ) {
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progressFraction)
                .clip(RoundedCornerShape(2.dp))
                .background(if (taskInfo.isCompleted) EmeraldOnline else IndigoPrimary)
            )
          }
          Text(
            text = "${taskInfo.currentProgress}/${taskInfo.targetProgress} ${task.unit}",
            fontSize = 10.sp,
            color = TextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      // Action / Status Pill
      when {
        taskInfo.isClaimed -> {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkSurfaceElevated
          ) {
            Text(
              text = "Claimed ✓",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = EmeraldOnline,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            )
          }
        }
        taskInfo.isCompleted -> {
          if (isDailyLimitReached) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = DarkSurfaceElevated
            ) {
              Text(
                text = "Limit reached",
                fontSize = 10.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
              )
            }
          } else {
            Button(
              onClick = onClaim,
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AmberXp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.testTag("claim_${task.id}")
            ) {
              Text("Claim", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
          }
        }
        else -> {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkSurfaceElevated
          ) {
            Text(
              text = "In Progress",
              fontSize = 10.sp,
              color = TextSecondary,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
          }
        }
      }
    }
  }
}

/**
 * Coin History Dialog displaying immutable transaction ledger records.
 */
@Composable
fun CoinHistoryDialog(
  walletState: CoinWalletState,
  onDismiss: () -> Unit
) {
  val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 16.dp)
        .testTag("coin_history_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📜", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Coin History",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Authoritative transaction ledger",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Balance reminder banner
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = DarkSurfaceElevated,
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Current Wallet Balance", fontSize = 12.sp, color = TextSecondary)
            Text(
              text = "🪙 ${String.format(Locale.US, "%,d", walletState.balance)}",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFFD54F)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Transactions list
        if (walletState.history.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp),
            contentAlignment = Alignment.Center
          ) {
            Text("No transactions recorded yet.", color = TextSecondary, fontSize = 13.sp)
          }
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(walletState.history, key = { it.id }) { txn ->
              TransactionRow(txn = txn, dateFormat = dateFormat)
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Close", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun TransactionRow(
  txn: CoinTransaction,
  dateFormat: SimpleDateFormat
) {
  val isPositive = txn.amount > 0
  val formattedAmount = if (isPositive) "+${txn.amount}" else "${txn.amount}"
  val amountColor = if (isPositive) Color(0xFFFFD54F) else RoseError

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkSurface,
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = txn.reason,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color.White
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = txn.id,
            fontSize = 10.sp,
            color = TextMuted
          )
          Text(
            text = dateFormat.format(Date(txn.timestamp)),
            fontSize = 10.sp,
            color = TextSecondary
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "$formattedAmount 🪙",
          fontSize = 14.sp,
          fontWeight = FontWeight.Black,
          color = amountColor
        )
        Text(
          text = "Bal: ${txn.resultingBalance}",
          fontSize = 10.sp,
          color = TextSecondary
        )
      }
    }
  }
}
