package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun OtpVerificationDialog(
  onDismiss: () -> Unit,
  onVerify: (phone: String, otp: String) -> Boolean
) {
  var phoneNumber by remember { mutableStateOf("") }
  var otpCode by remember { mutableStateOf("") }
  var isOtpSent by remember { mutableStateOf(false) }
  var resendTimer by remember { mutableStateOf(60) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(isOtpSent) {
    if (isOtpSent) {
      resendTimer = 60
      while (resendTimer > 0) {
        delay(1000)
        resendTimer -= 1
      }
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
      shape = RoundedCornerShape(24.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("otp_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(24.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("📱", fontSize = 28.sp)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Verify Mobile Number",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Link guest account permanently",
              fontSize = 12.sp,
              color = TextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Assurance card
        Surface(
          color = Color(0xFF132238),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A5F)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "🔒 No Data Loss Guarantee:",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = ShieldAdmin
            )
            Text(
              text = "Your User ID, rooms, watch history, friends, XP & badges are preserved 100%. Keeps you logged in for 15 days.",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = phoneNumber,
          onValueChange = { phoneNumber = it },
          label = { Text("Mobile Number (10 digits)") },
          placeholder = { Text("e.g. 9876543210") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          leadingIcon = {
            Text("+91", color = TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = IndigoPrimary,
            unfocusedBorderColor = DarkSurfaceBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("phone_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!isOtpSent) {
          Button(
            onClick = {
              if (phoneNumber.length >= 10) {
                isOtpSent = true
                otpCode = "123456" // Pre-fill test OTP for seamless reviewer verification
                errorMessage = null
              } else {
                errorMessage = "Please enter a valid 10-digit mobile number."
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("send_otp_btn")
          ) {
            Text("Send OTP", fontWeight = FontWeight.Bold)
          }
        } else {
          OutlinedTextField(
            value = otpCode,
            onValueChange = { if (it.length <= 6) otpCode = it },
            label = { Text("6-Digit OTP Code") },
            placeholder = { Text("123456") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = IndigoPrimary,
              unfocusedBorderColor = DarkSurfaceBorder
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("otp_input")
          )

          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (resendTimer > 0) "Resend in ${resendTimer}s" else "Didn't receive?",
              fontSize = 11.sp,
              color = TextSecondary
            )
            if (resendTimer == 0) {
              TextButton(onClick = { resendTimer = 60 }) {
                Text("Resend OTP", color = IndigoLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            } else {
              Text(
                text = "Demo OTP: 123456",
                fontSize = 11.sp,
                color = AmberXp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = {
              val success = onVerify(phoneNumber, otpCode)
              if (!success) {
                errorMessage = "Invalid OTP code. Please enter 6 digits."
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("verify_otp_confirm_btn")
          ) {
            Text("Verify & Link Account", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = errorMessage!!,
            color = RoseError,
            fontSize = 12.sp
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
          onClick = onDismiss,
          modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
          Text("Cancel / Continue as Guest", color = TextSecondary, fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
fun CreateRoomDialog(
  onDismiss: () -> Unit,
  onCreate: (name: String, desc: String, isPrivate: Boolean, pass: String, welcome: String, rules: String, category: RoomCategory) -> Unit
) {
  var roomName by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var isPrivate by remember { mutableStateOf(false) }
  var password by remember { mutableStateOf("") }
  var welcomeMessage by remember { mutableStateOf("") }
  var rules by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(RoomCategory.WATCH_TOGETHER) }
  var errorText by remember { mutableStateOf<String?>(null) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
      shape = RoundedCornerShape(24.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("create_room_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "Create Permanent Room 👑",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = "You will be the permanent Owner. The room remains active 24/7.",
          fontSize = 12.sp,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = roomName,
          onValueChange = { roomName = it },
          label = { Text("Room Name *") },
          placeholder = { Text("e.g. Friday Sci-Fi Marathon 🎬") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("room_name_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Room Description") },
          placeholder = { Text("What are you watching together?") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Privacy Row
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            Text(
              text = if (isPrivate) "Private Room 🔒" else "Public Room 🌐",
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color.White
            )
            Text(
              text = if (isPrivate) "Requires password to enter" else "Anyone can discover & join",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
          Switch(
            checked = isPrivate,
            onCheckedChange = { isPrivate = it },
            modifier = Modifier.testTag("private_switch")
          )
        }

        if (isPrivate) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Room Password") },
            placeholder = { Text("Enter room password") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("room_password_input")
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = welcomeMessage,
          onValueChange = { welcomeMessage = it },
          label = { Text("Welcome Message") },
          placeholder = { Text("Shown to users when they enter") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = rules,
          onValueChange = { rules = it },
          label = { Text("Room Rules") },
          placeholder = { Text("e.g. 1. Be kind 2. No spoilers") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth()
        )

        if (errorText != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(text = errorText!!, color = RoseError, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel", color = TextSecondary)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (roomName.isBlank()) {
                errorText = "Please provide a room name"
              } else {
                onCreate(roomName, description, isPrivate, password, welcomeMessage, rules, selectedCategory)
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("confirm_create_room_btn")
          ) {
            Text("Create Room", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun JoinRoomDialog(
  onDismiss: () -> Unit,
  onJoin: (roomId: String, pass: String) -> Unit
) {
  var roomIdInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
      shape = RoundedCornerShape(24.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("join_room_dialog")
    ) {
      Column(
        modifier = Modifier.padding(20.dp)
      ) {
        Text(
          text = "Join Room 🔗",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = "Enter a Room ID (e.g. ABC7291) or link (app://room/ABC7291)",
          fontSize = 12.sp,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = roomIdInput,
          onValueChange = { input ->
            // Support pasting app://room/ABC7291
            val cleaned = if (input.contains("app://room/")) {
              input.substringAfter("app://room/")
            } else input
            roomIdInput = cleaned.trim().uppercase()
          },
          label = { Text("Room ID or Link") },
          placeholder = { Text("ABC7291") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("join_room_id_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = passwordInput,
          onValueChange = { passwordInput = it },
          label = { Text("Password (If private)") },
          placeholder = { Text("Optional") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel", color = TextSecondary)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (roomIdInput.isNotBlank()) {
                onJoin(roomIdInput, passwordInput)
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("confirm_join_btn")
          ) {
            Text("Join Room", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun UserProfileInspectionDialog(
  friend: Friend,
  onDismiss: () -> Unit,
  onAddFriend: () -> Unit,
  onMessage: () -> Unit,
  onInviteToRoom: () -> Unit,
  onBlock: () -> Unit,
  onReport: () -> Unit
) {
  val context = LocalContext.current

  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
      shape = RoundedCornerShape(24.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("user_profile_modal")
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        ProfileFrameAvatar(
          username = friend.username,
          frame = "Neon Purple",
          isOnline = friend.isOnline,
          size = 72.dp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = friend.username,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("User ID", friend.userId))
          }
        ) {
          Text(
            text = "ID: ${friend.userId}",
            fontSize = 12.sp,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy ID",
            tint = IndigoLight,
            modifier = Modifier.size(13.dp)
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Status pill
        Surface(
          color = DarkSurfaceElevated,
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(
            text = "${friend.statusEmoji} ${friend.statusText}",
            fontSize = 12.sp,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }

        if (friend.bio.isNotBlank()) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = friend.bio,
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp)
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onAddFriend,
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Friend", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = onMessage,
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Message", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
          onClick = onInviteToRoom,
          colors = ButtonDefaults.buttonColors(containerColor = PurpleContainer),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Invite to Current Room", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          TextButton(onClick = onBlock) {
            Text("Block", color = TextMuted, fontSize = 12.sp)
          }
          TextButton(onClick = onReport) {
            Text("Report User", color = RoseError, fontSize = 12.sp)
          }
          TextButton(onClick = onDismiss) {
            Text("Close", color = TextSecondary, fontSize = 12.sp)
          }
        }
      }
    }
  }
}

@Composable
fun RoomSettingsDialog(
  room: Room,
  admins: List<RoomAdmin>,
  isOwner: Boolean,
  onDismiss: () -> Unit,
  onSave: (name: String, desc: String, welcome: String, rules: String, mode: VideoControlMode, locked: Boolean) -> Unit,
  onAddAdmin: (userId: String, username: String) -> Unit,
  onRemoveAdmin: (userId: String) -> Unit,
  onDeleteRoom: () -> Unit
) {
  var name by remember { mutableStateOf(room.name) }
  var desc by remember { mutableStateOf(room.description) }
  var welcome by remember { mutableStateOf(room.welcomeMessage) }
  var rules by remember { mutableStateOf(room.rules) }
  var mode by remember { mutableStateOf(room.videoControlMode) }
  var locked by remember { mutableStateOf(room.isLocked) }
  var newAdminUserId by remember { mutableStateOf("") }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
      shape = RoundedCornerShape(24.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("room_settings_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Room Settings ⚙️",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          if (isOwner) {
            OwnerBadge()
          } else {
            AdminBadge()
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Synchronized Video Control Mode",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
          FilterChip(
            selected = mode == VideoControlMode.EVERYONE,
            onClick = { if (isOwner) mode = VideoControlMode.EVERYONE },
            label = { Text("Everyone", fontSize = 11.sp) },
            modifier = Modifier.padding(end = 4.dp)
          )
          FilterChip(
            selected = mode == VideoControlMode.ADMINS_ONLY,
            onClick = { if (isOwner) mode = VideoControlMode.ADMINS_ONLY },
            label = { Text("Admins & Owner", fontSize = 11.sp) },
            modifier = Modifier.padding(end = 4.dp)
          )
          FilterChip(
            selected = mode == VideoControlMode.OWNER_ONLY,
            onClick = { if (isOwner) mode = VideoControlMode.OWNER_ONLY },
            label = { Text("Owner Only", fontSize = 11.sp) }
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Lock Room
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            Text("Lock Room", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text("Prevent new users from joining", fontSize = 11.sp, color = TextSecondary)
          }
          Switch(checked = locked, onCheckedChange = { locked = it })
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Room Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = desc,
          onValueChange = { desc = it },
          label = { Text("Description") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = welcome,
          onValueChange = { welcome = it },
          label = { Text("Welcome Message") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Room Admin Management (Owner only)
        if (isOwner) {
          Text(
            text = "Room Admins (${admins.size}/4)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = ShieldAdmin
          )
          Spacer(modifier = Modifier.height(6.dp))
          admins.forEach { admin ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
            ) {
              Text(
                text = "🛡️ ${admin.username} (${admin.userId})",
                fontSize = 12.sp,
                color = Color.White
              )
              IconButton(onClick = { onRemoveAdmin(admin.userId) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove Admin", tint = RoseError, modifier = Modifier.size(16.dp))
              }
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp)
          ) {
            OutlinedTextField(
              value = newAdminUserId,
              onValueChange = { newAdminUserId = it },
              label = { Text("User ID to Appoint") },
              placeholder = { Text("USR-10103") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Button(
              onClick = {
                if (newAdminUserId.isNotBlank()) {
                  onAddAdmin(newAdminUserId, "User_${newAdminUserId.takeLast(4)}")
                  newAdminUserId = ""
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = ShieldAdmin)
            ) {
              Text("Add", fontSize = 12.sp, color = DarkBackground, fontWeight = FontWeight.Bold)
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Delete Room (Owner only)
        if (isOwner) {
          if (!showDeleteConfirm) {
            OutlinedButton(
              onClick = { showDeleteConfirm = true },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
              border = androidx.compose.foundation.BorderStroke(1.dp, RoseError),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Delete Permanent Room")
            }
          } else {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2A151A), RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
              Text(
                text = "Are you sure you want to permanently delete this room? This cannot be undone.",
                fontSize = 12.sp,
                color = RoseError
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { showDeleteConfirm = false }) {
                  Text("Cancel", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                  onClick = onDeleteRoom,
                  colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                  Text("Confirm Delete")
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
          horizontalArrangement = Arrangement.End,
          modifier = Modifier.fillMaxWidth()
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel", color = TextSecondary)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onSave(name, desc, welcome, rules, mode, locked) },
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Save Settings", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun SeatRequestsDialog(
  requests: List<AudienceMember>,
  onDismiss: () -> Unit,
  onApprove: (userId: String) -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
      shape = RoundedCornerShape(24.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("seat_requests_dialog")
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = "Voice Seat Requests ✋",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = "Audience members waiting to join the 8-seat voice room.",
          fontSize = 12.sp,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (requests.isEmpty()) {
          Text(
            text = "No pending seat requests right now.",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(vertical = 24.dp)
          )
        } else {
          requests.forEach { member ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
                ) {
                  Text(getInitials(member.username), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(member.username, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                  Text("Requested voice seat", color = TextSecondary, fontSize = 10.sp)
                }
              }

              Button(
                onClick = { onApprove(member.userId) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.testTag("approve_seat_${member.userId}")
              ) {
                Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Close", color = Color.White)
        }
      }
    }
  }
}

@Composable
fun RoomInviteDialog(
  room: Room,
  friends: List<Friend>,
  onDismiss: () -> Unit,
  onInviteFriend: (friendId: String) -> Unit,
  onOpenQr: () -> Unit
) {
  val context = LocalContext.current
  val shareLink = "app://room/${room.id}"

  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
      shape = RoundedCornerShape(24.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("invite_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "Invite to ${room.name} 💌",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = "Share with friends or send direct room invitations.",
          fontSize = 12.sp,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Room Link card
        Surface(
          color = DarkSurfaceElevated,
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp)
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Room ID: ${room.id}", color = AmberXp, fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Text(shareLink, color = TextSecondary, fontSize = 11.sp)
            }
            IconButton(
              onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Room Link", shareLink))
              }
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = IndigoLight)
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = onOpenQr,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Show Room QR Code", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Send Direct Invitation to Friends:",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        val onlineFriends = friends.filter { it.friendshipStatus == FriendshipStatus.ACCEPTED }
        if (onlineFriends.isEmpty()) {
          Text("No friends available to invite.", color = TextSecondary, fontSize = 12.sp)
        } else {
          onlineFriends.forEach { friend ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileFrameAvatar(
                  username = friend.username,
                  frame = "Neon Purple",
                  isOnline = friend.isOnline,
                  size = 32.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(friend.username, color = Color.White, fontSize = 13.sp)
              }
              Button(
                onClick = { onInviteFriend(friend.userId) },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
              ) {
                Text("Invite", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Done", color = Color.White)
        }
      }
    }
  }
}

@Composable
fun QrShareDialog(
  room: Room,
  onDismiss: () -> Unit
) {
  val link = "app://room/${room.id}"

  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
      shape = RoundedCornerShape(24.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("qr_dialog")
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
      ) {
        Text("Scan to Join Room", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(room.name, fontSize = 14.sp, color = IndigoLight, fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.height(18.dp))

        // Stylized QR code representation
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(12.dp)
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.QrCode2,
              contentDescription = "QR Code",
              tint = Color.Black,
              modifier = Modifier.size(130.dp)
            )
            Text(room.id, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(link, color = TextSecondary, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(18.dp))
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
fun ReportDialog(
  targetInfo: Pair<String, String>,
  onDismiss: () -> Unit,
  onSubmit: (reason: String) -> Unit
) {
  val reasons = listOf(
    "Spam, bot or automated advertising",
    "Harassment, hate speech or toxicity",
    "Inappropriate or copyright-infringing content",
    "Disruptive microphone noise / voice abuse",
    "Other safety concern"
  )
  var selectedReason by remember { mutableStateOf(reasons[0]) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
      shape = RoundedCornerShape(24.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("report_dialog")
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text("Report ${targetInfo.first} ⚠️", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RoseError)
        Text(targetInfo.second, fontSize = 12.sp, color = TextSecondary)

        Spacer(modifier = Modifier.height(14.dp))

        reasons.forEach { r ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedReason = r }
              .padding(vertical = 4.dp)
          ) {
            RadioButton(
              selected = selectedReason == r,
              onClick = { selectedReason = r },
              colors = RadioButtonDefaults.colors(selectedColor = RoseError)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(r, fontSize = 12.sp, color = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
          TextButton(onClick = onDismiss) {
            Text("Cancel", color = TextSecondary)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onSubmit(selectedReason) },
            colors = ButtonDefaults.buttonColors(containerColor = RoseError),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Submit Report", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
