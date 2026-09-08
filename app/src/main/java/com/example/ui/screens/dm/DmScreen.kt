package com.example.ui.screens.dm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DmScreen(
  currentUser: User,
  friends: List<Friend>,
  directMessages: Map<String, List<DirectMessage>>,
  activeChatFriend: Friend?,
  currentActiveRoom: Room?,
  onOpenChat: (Friend) -> Unit,
  onCloseChat: () -> Unit,
  onSendMessage: (receiverId: String, text: String, isInvite: Boolean, roomId: String?, roomName: String?) -> Unit,
  onDeleteMessage: (friendId: String, msgId: String) -> Unit,
  onAcceptFriendRequest: (userId: String) -> Unit,
  onRejectFriendRequest: (userId: String) -> Unit,
  onSendFriendRequest: (userId: String, username: String) -> Unit,
  onInspectUser: (Friend) -> Unit,
  onJoinRoomFromInvite: (roomId: String) -> Unit
) {
  if (activeChatFriend != null) {
    // 1-to-1 Chat Conversation Screen
    DmConversationView(
      friend = activeChatFriend,
      currentUserId = currentUser.id,
      messages = directMessages[activeChatFriend.userId] ?: emptyList(),
      currentActiveRoom = currentActiveRoom,
      onBack = onCloseChat,
      onSendMessage = { text, isInvite, rId, rName ->
        onSendMessage(activeChatFriend.userId, text, isInvite, rId, rName)
      },
      onDeleteMessage = { msgId ->
        onDeleteMessage(activeChatFriend.userId, msgId)
      },
      onInspectUser = { onInspectUser(activeChatFriend) },
      onJoinRoomFromInvite = onJoinRoomFromInvite
    )
  } else {
    // DM Inbox & Friends list
    DmInboxView(
      friends = friends,
      directMessages = directMessages,
      onOpenChat = onOpenChat,
      onAcceptRequest = onAcceptFriendRequest,
      onRejectRequest = onRejectFriendRequest,
      onSendFriendRequest = onSendFriendRequest,
      onInspectUser = onInspectUser
    )
  }
}

@Composable
fun DmInboxView(
  friends: List<Friend>,
  directMessages: Map<String, List<DirectMessage>>,
  onOpenChat: (Friend) -> Unit,
  onAcceptRequest: (String) -> Unit,
  onRejectRequest: (String) -> Unit,
  onSendFriendRequest: (String, String) -> Unit,
  onInspectUser: (Friend) -> Unit
) {
  var showAddFriendDialog by remember { mutableStateOf(false) }
  var searchUserQuery by remember { mutableStateOf("") }

  val acceptedFriends = friends.filter { it.friendshipStatus == FriendshipStatus.ACCEPTED }
  val pendingRequests = friends.filter { it.friendshipStatus == FriendshipStatus.PENDING }

  Scaffold(
    containerColor = DarkBackground,
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurface)
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💬", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Direct Messages",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Chat & share rooms with friends",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }

          IconButton(
            onClick = { showAddFriendDialog = true },
            colors = IconButtonDefaults.iconButtonColors(containerColor = IndigoPrimary),
            modifier = Modifier
              .size(38.dp)
              .testTag("add_friend_btn")
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend", tint = Color.White, modifier = Modifier.size(20.dp))
          }
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      contentPadding = PaddingValues(
        top = paddingValues.calculateTopPadding() + 8.dp,
        bottom = 100.dp,
        start = 16.dp,
        end = 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // 1. Pending Friend Requests Banner (if any)
      if (pendingRequests.isNotEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E38)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "Friend Requests (${pendingRequests.size}) 👥",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AmberXp
              )
              Spacer(modifier = Modifier.height(8.dp))
              pendingRequests.forEach { req ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileFrameAvatar(
                      username = req.username,
                      frame = "Neon Purple",
                      isOnline = req.isOnline,
                      size = 32.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(req.username, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                      Text("User ID: ${req.userId}", color = TextSecondary, fontSize = 10.sp)
                    }
                  }

                  Row {
                    IconButton(
                      onClick = { onAcceptRequest(req.userId) },
                      modifier = Modifier.size(32.dp).testTag("accept_friend_${req.userId}")
                    ) {
                      Icon(Icons.Default.CheckCircle, contentDescription = "Accept", tint = EmeraldOnline)
                    }
                    IconButton(
                      onClick = { onRejectRequest(req.userId) },
                      modifier = Modifier.size(32.dp).testTag("reject_friend_${req.userId}")
                    ) {
                      Icon(Icons.Default.Cancel, contentDescription = "Decline", tint = RoseError)
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 2. Active Friends (Horizontal row)
      if (acceptedFriends.isNotEmpty()) {
        item {
          Column {
            Text(
              text = "Friends Online",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(14.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(acceptedFriends, key = { it.userId }) { friend ->
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier
                    .clickable { onOpenChat(friend) }
                    .width(64.dp)
                ) {
                  ProfileFrameAvatar(
                    username = friend.username,
                    frame = "Neon Purple",
                    isOnline = friend.isOnline,
                    size = 52.dp,
                    onClick = { onOpenChat(friend) }
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = friend.username,
                    fontSize = 11.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = if (friend.isOnline) "${friend.statusEmoji} Active" else "Offline",
                    fontSize = 9.sp,
                    color = if (friend.isOnline) EmeraldOnline else TextSecondary
                  )
                }
              }
            }
          }
        }
      }

      // 3. DM Conversations List
      item {
        Text(
          text = "Conversations",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }

      if (acceptedFriends.isEmpty()) {
        item {
          EmptyStateView(
            icon = Icons.Default.Chat,
            title = "No Friends Yet",
            subtitle = "Add friends using their User ID to start chatting and watching together in permanent rooms!",
            actionText = "Add Friend",
            onAction = { showAddFriendDialog = true }
          )
        }
      } else {
        items(acceptedFriends, key = { it.userId }) { friend ->
          val lastMessage = directMessages[friend.userId]?.lastOrNull()

          Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenChat(friend) }
              .testTag("dm_card_${friend.userId}")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(12.dp)
            ) {
              ProfileFrameAvatar(
                username = friend.username,
                frame = "Neon Purple",
                isOnline = friend.isOnline,
                size = 46.dp,
                onClick = { onInspectUser(friend) }
              )

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = friend.username,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  if (lastMessage != null) {
                    Text(
                      text = formatTimestamp(lastMessage.timestamp),
                      fontSize = 10.sp,
                      color = TextSecondary
                    )
                  }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                  text = if (lastMessage != null) {
                    if (lastMessage.isRoomInvite) "🎟️ Invited you to join a room" else lastMessage.text
                  } else "${friend.statusEmoji} ${friend.statusText}",
                  fontSize = 12.sp,
                  color = if (lastMessage?.isRoomInvite == true) IndigoLight else TextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    }
  }

  // Add Friend Dialog
  if (showAddFriendDialog) {
    var userIdInput by remember { mutableStateOf("") }
    var friendUsernameInput by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddFriendDialog = false },
      containerColor = DarkSurfaceCard,
      title = { Text("Add Friend 👤", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Enter friend's User ID (e.g. USR-10103):", fontSize = 12.sp, color = TextSecondary)
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = userIdInput,
            onValueChange = { userIdInput = it.uppercase() },
            label = { Text("User ID") },
            placeholder = { Text("USR-10103") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = friendUsernameInput,
            onValueChange = { friendUsernameInput = it },
            label = { Text("Friend's Username (optional)") },
            placeholder = { Text("Alex_Rivera") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (userIdInput.isNotBlank()) {
              val uname = if (friendUsernameInput.isNotBlank()) friendUsernameInput else "User_${userIdInput.takeLast(4)}"
              onSendFriendRequest(userIdInput, uname)
              showAddFriendDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
        ) {
          Text("Send Request", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddFriendDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
fun DmConversationView(
  friend: Friend,
  currentUserId: String,
  messages: List<DirectMessage>,
  currentActiveRoom: Room?,
  onBack: () -> Unit,
  onSendMessage: (text: String, isInvite: Boolean, roomId: String?, roomName: String?) -> Unit,
  onDeleteMessage: (msgId: String) -> Unit,
  onInspectUser: () -> Unit,
  onJoinRoomFromInvite: (roomId: String) -> Unit
) {
  var inputMessage by remember { mutableStateOf("") }

  Scaffold(
    containerColor = DarkBackground,
    topBar = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurface)
          .statusBarsPadding()
          .padding(horizontal = 8.dp, vertical = 8.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
          ProfileFrameAvatar(
            username = friend.username,
            frame = "Neon Purple",
            isOnline = friend.isOnline,
            size = 38.dp,
            onClick = onInspectUser
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.clickable { onInspectUser() }) {
            Text(
              text = friend.username,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = if (friend.isOnline) "🟢 Online • ${friend.statusText}" else "Offline",
              fontSize = 11.sp,
              color = if (friend.isOnline) EmeraldOnline else TextSecondary
            )
          }
        }

        IconButton(onClick = onInspectUser) {
          Icon(Icons.Default.Info, contentDescription = "User Info", tint = IndigoLight)
        }
      }
    },
    bottomBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurface)
          .navigationBarsPadding()
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        // Option to invite to current room
        if (currentActiveRoom != null) {
          Surface(
            color = PurpleContainer.copy(alpha = 0.6f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onSendMessage(
                  "Hey, join my room: ${currentActiveRoom.name}!",
                  true,
                  currentActiveRoom.id,
                  currentActiveRoom.name
                )
              }
              .padding(bottom = 6.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text("🎟️", fontSize = 12.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Tap to invite ${friend.username} to ${currentActiveRoom.name}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedTextField(
            value = inputMessage,
            onValueChange = { inputMessage = it },
            placeholder = { Text("Message ${friend.username}...", fontSize = 13.sp) },
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
              .testTag("dm_input_field")
          )
          Spacer(modifier = Modifier.width(6.dp))
          IconButton(
            onClick = {
              if (inputMessage.isNotBlank()) {
                onSendMessage(inputMessage, false, null, null)
                inputMessage = ""
              }
            },
            colors = IconButtonDefaults.iconButtonColors(containerColor = IndigoPrimary),
            modifier = Modifier
              .size(42.dp)
              .testTag("send_dm_btn")
          ) {
            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      contentPadding = PaddingValues(
        top = paddingValues.calculateTopPadding() + 8.dp,
        bottom = paddingValues.calculateBottomPadding() + 8.dp,
        start = 14.dp,
        end = 14.dp
      ),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      items(messages, key = { it.id }) { msg ->
        val isMe = msg.senderId == currentUserId

        Column(
          horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
          modifier = Modifier.fillMaxWidth()
        ) {
          val inviteRoomId = msg.roomId
          if (msg.isRoomInvite && inviteRoomId != null) {
            // Interactive Room Invitation Card in DM thread
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFF241442)),
              shape = RoundedCornerShape(16.dp),
              border = androidx.compose.foundation.BorderStroke(1.5.dp, PurpleAccent),
              modifier = Modifier
                .widthIn(max = 280.dp)
                .testTag("dm_invite_card_$inviteRoomId")
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("🍿", fontSize = 20.sp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Room Invitation",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberXp
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = msg.roomName ?: "Watch Party",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = "Room ID: $inviteRoomId",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                  onClick = { onJoinRoomFromInvite(inviteRoomId) },
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text("Join Room Now 🚀", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                }
              }
            }
          } else {
            // Normal message bubble
            Surface(
              color = if (isMe) IndigoPrimary else DarkSurfaceElevated,
              shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 16.dp
              ),
              modifier = Modifier.widthIn(max = 280.dp)
            ) {
              Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                  text = msg.text,
                  color = Color.White,
                  fontSize = 13.sp
                )
                Text(
                  text = formatTimestamp(msg.timestamp),
                  color = Color.White.copy(alpha = 0.6f),
                  fontSize = 9.sp,
                  modifier = Modifier.align(Alignment.End)
                )
              }
            }
          }

          if (isMe) {
            Text(
              text = "Delete",
              color = TextMuted,
              fontSize = 9.sp,
              modifier = Modifier
                .clickable { onDeleteMessage(msg.id) }
                .padding(top = 2.dp)
            )
          }
        }
      }
    }
  }
}

private fun formatTimestamp(timeMs: Long): String {
  val diff = System.currentTimeMillis() - timeMs
  return when {
    diff < 60_000L -> "Just now"
    diff < 3600_000L -> "${diff / 60_000L}m ago"
    diff < 86400_000L -> "${diff / 3600_000L}h ago"
    else -> "${diff / 86400_000L}d ago"
  }
}
