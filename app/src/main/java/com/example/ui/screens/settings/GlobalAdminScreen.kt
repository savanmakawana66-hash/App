package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReportItem
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*

@Composable
fun GlobalAdminScreen(
  reports: List<ReportItem>,
  onClose: () -> Unit,
  onResolveReport: (reportId: String, action: String) -> Unit,
  onBanUser: (userId: String) -> Unit,
  onBroadcastAnnouncement: (title: String, message: String) -> Unit
) {
  var banUserIdInput by remember { mutableStateOf("") }
  var announcementTitle by remember { mutableStateOf("") }
  var announcementMessage by remember { mutableStateOf("") }
  var statusMessage by remember { mutableStateOf<String?>(null) }

  Scaffold(
    containerColor = DarkBackground,
    topBar = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurface)
          .statusBarsPadding()
          .padding(horizontal = 12.dp, vertical = 10.dp)
      ) {
        IconButton(onClick = onClose, modifier = Modifier.testTag("admin_back_btn")) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Super-Admin Console 🛡️",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }
  ) { paddingValues ->
    LazyColumn(
      contentPadding = PaddingValues(
        top = paddingValues.calculateTopPadding() + 8.dp,
        bottom = 40.dp,
        start = 16.dp,
        end = 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // 1. Health & Server Metrics
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("System Overview 📊", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              MetricItem("Permanent Rooms", "1,248")
              MetricItem("Active Voice Streams", "382")
              MetricItem("Avg Latency", "14ms")
              MetricItem("Status", "🟢 Healthy")
            }
          }
        }
      }

      // 2. Broadcast System Announcement
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("Broadcast Announcement 📢", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Deliver a high-priority banner to all online users.", fontSize = 11.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
              value = announcementTitle,
              onValueChange = { announcementTitle = it },
              label = { Text("Title") },
              placeholder = { Text("Scheduled Maintenance") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
              value = announcementMessage,
              onValueChange = { announcementMessage = it },
              label = { Text("Message Body") },
              placeholder = { Text("Servers will perform a 2-minute rolling update.") },
              maxLines = 3,
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
              onClick = {
                if (announcementTitle.isNotBlank() && announcementMessage.isNotBlank()) {
                  onBroadcastAnnouncement(announcementTitle, announcementMessage)
                  announcementTitle = ""
                  announcementMessage = ""
                  statusMessage = "Announcement broadcasted successfully!"
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Broadcast to All Rooms", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // 3. User Lookup & Global Ban
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("Global User Moderation 🚫", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RoseError)
            Text("Ban offending users across all rooms and direct messages.", fontSize = 11.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
              OutlinedTextField(
                value = banUserIdInput,
                onValueChange = { banUserIdInput = it.uppercase() },
                label = { Text("Offender User ID") },
                placeholder = { Text("USR-9999") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = {
                  if (banUserIdInput.isNotBlank()) {
                    onBanUser(banUserIdInput)
                    statusMessage = "User $banUserIdInput has been globally banned."
                    banUserIdInput = ""
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("Ban User", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      if (statusMessage != null) {
        item {
          Surface(
            color = EmeraldOnline.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = statusMessage!!,
              color = EmeraldOnline,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(12.dp)
            )
          }
        }
      }

      // 4. Moderation Reports Queue
      item {
        Text(
          text = "Pending Moderation Reports (${reports.count { !it.isResolved }}) ⚠️",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }

      val pendingReports = reports.filter { !it.isResolved }
      if (pendingReports.isEmpty()) {
        item {
          EmptyStateView(
            icon = Icons.Default.CheckCircleOutline,
            title = "All Reports Resolved",
            subtitle = "Great job! The moderation queue is completely clean."
          )
        }
      } else {
        items(pendingReports, key = { it.id }) { rep ->
          Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Target: ${rep.reportedTargetName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Surface(color = RoseError.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                  Text(rep.targetType, color = RoseError, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text("Reason: ${rep.reason}", color = AmberXp, fontSize = 12.sp)
              Text("Reported by ${rep.reporterName} • ${rep.timestamp}", color = TextSecondary, fontSize = 10.sp)

              Spacer(modifier = Modifier.height(10.dp))

              Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                  onClick = { onResolveReport(rep.id, "DISMISSED") },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                  Text("Dismiss", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                  onClick = { onResolveReport(rep.id, "BANNED") },
                  colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                  Text("Action & Ban", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun MetricItem(label: String, value: String) {
  Column {
    Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    Text(label, color = TextSecondary, fontSize = 10.sp)
  }
}
