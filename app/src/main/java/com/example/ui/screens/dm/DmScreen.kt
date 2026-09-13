package com.example.ui.screens.dm

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.model.games.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

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
  onJoinRoomFromInvite: (roomId: String) -> Unit,
  onChallengeGame: ((Friend, GameType) -> Unit)? = null,
  onAcceptGameInvite: ((inviteId: String) -> Unit)? = null,
  pendingGameInvites: List<GameInvite> = emptyList()
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
      onJoinRoomFromInvite = onJoinRoomFromInvite,
      onChallengeGame = { gType -> onChallengeGame?.invoke(activeChatFriend, gType) },
      onAcceptGameInvite = { inviteId -> onAcceptGameInvite?.invoke(inviteId) },
      pendingGameInvites = pendingGameInvites
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
          val friendMessages = directMessages[friend.userId] ?: emptyList()
          val lastMessage = friendMessages.lastOrNull()
          val isLastFromMe = lastMessage?.senderId != friend.userId
          val unreadCount = friendMessages.count { it.senderId == friend.userId && !it.isRead }

          Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenChat(friend) }
              .testTag("dm_card_${friend.userId}")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
              // Avatar with Online Green Dot Indicator
              Box {
                ProfileFrameAvatar(
                  username = friend.username,
                  frame = "Neon Purple",
                  isOnline = friend.isOnline,
                  size = 50.dp,
                  onClick = { onInspectUser(friend) }
                )
                if (friend.isOnline) {
                  Box(
                    modifier = Modifier
                      .size(13.dp)
                      .clip(CircleShape)
                      .background(EmeraldOnline)
                      .border(2.dp, DarkSurfaceCard, CircleShape)
                      .align(Alignment.BottomEnd)
                  )
                }
              }

              Spacer(modifier = Modifier.width(14.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = friend.username,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  if (lastMessage != null) {
                    Text(
                      text = formatModernTimestamp(lastMessage.timestamp),
                      fontSize = 11.sp,
                      color = TextSecondary
                    )
                  }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                  ) {
                    if (lastMessage != null && isLastFromMe) {
                      MessageStatusCheckmarks(lastMessage.status)
                      Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                      text = if (lastMessage != null) {
                        if (lastMessage.isRoomInvite) "🎟️ Invited you to a room"
                        else if (lastMessage.isVoiceNote) "🎤 Voice message"
                        else lastMessage.text
                      } else "${friend.statusEmoji} ${friend.statusText}",
                      fontSize = 12.sp,
                      color = if (lastMessage?.isRoomInvite == true) IndigoLight else TextSecondary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }

                  // Unread Badge (Electric violet pill)
                  if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                      color = IndigoPrimary,
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.height(20.dp)
                    ) {
                      Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 7.dp)
                      ) {
                        Text(
                          text = "$unreadCount",
                          color = Color.White,
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold
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
  onJoinRoomFromInvite: (roomId: String) -> Unit,
  onChallengeGame: (GameType) -> Unit = {},
  onAcceptGameInvite: (String) -> Unit = {},
  pendingGameInvites: List<GameInvite> = emptyList()
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

        // Quick Game Challenge bar (Chess / Ludo)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            color = DarkSurfaceElevated,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f)),
            modifier = Modifier
              .weight(1f)
              .clickable { onChallengeGame(GameType.CHESS) }
          ) {
            Row(
              modifier = Modifier.padding(vertical = 6.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("♟️ Challenge Chess", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoLight)
            }
          }

          Surface(
            color = DarkSurfaceElevated,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f)),
            modifier = Modifier
              .weight(1f)
              .clickable { onChallengeGame(GameType.LUDO) }
          ) {
            Row(
              modifier = Modifier.padding(vertical = 6.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("🎲 Challenge Ludo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberXp)
            }
          }
        }

        // Modern Input Bar: Attachment, Rounded Pill Input, Voice Note Mic, Send Button
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          // Attachment Button
          IconButton(
            onClick = {
              // Quick photo/video attachment simulation
              onSendMessage("📷 Photo Attachment", false, null, null)
            },
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(DarkSurfaceElevated)
              .testTag("attachment_btn")
          ) {
            Icon(Icons.Default.AttachFile, contentDescription = "Attachment", tint = IndigoLight, modifier = Modifier.size(20.dp))
          }

          Spacer(modifier = Modifier.width(8.dp))

          OutlinedTextField(
            value = inputMessage,
            onValueChange = { inputMessage = it },
            placeholder = { Text("Message ${friend.username}...", fontSize = 13.sp, color = TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated,
              focusedBorderColor = IndigoPrimary,
              unfocusedBorderColor = DarkSurfaceBorder,
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("dm_input_field")
          )

          Spacer(modifier = Modifier.width(8.dp))

          // Dynamic Action: Send Button when typing, Voice Note Mic button when empty
          if (inputMessage.isNotBlank()) {
            IconButton(
              onClick = {
                onSendMessage(inputMessage, false, null, null)
                inputMessage = ""
              },
              colors = IconButtonDefaults.iconButtonColors(containerColor = IndigoPrimary),
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .testTag("send_dm_btn")
            ) {
              Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
            }
          } else {
            IconButton(
              onClick = {
                onSendMessage("🎤 Voice Note (0:08)", false, null, null)
              },
              colors = IconButtonDefaults.iconButtonColors(containerColor = DarkSurfaceElevated),
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .border(1.dp, IndigoPrimary.copy(alpha = 0.5f), CircleShape)
                .testTag("voice_note_mic_btn")
            ) {
              Icon(Icons.Default.Mic, contentDescription = "Voice Note", tint = IndigoLight, modifier = Modifier.size(20.dp))
            }
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
          val isGameInvite = msg.text.contains("challenged you to a game", ignoreCase = true) || msg.text.startsWith("🎮")
          val isChess = msg.text.contains("Chess", ignoreCase = true)
          val isLudo = msg.text.contains("Ludo", ignoreCase = true)

          if (msg.isRoomInvite && inviteRoomId != null) {
            // Interactive Room Invitation Card in DM thread
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1435)),
              shape = RoundedCornerShape(18.dp),
              border = androidx.compose.foundation.BorderStroke(1.5.dp, PurpleAccent),
              modifier = Modifier
                .widthIn(max = 290.dp)
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
          } else if (isGameInvite) {
            // Interactive Game Challenge Card in DM thread
            val gameIcon = if (isChess) "♟️" else if (isLudo) "🎲" else "🎮"
            val gameTitle = if (isChess) "Chess Match" else if (isLudo) "Ludo Match" else "Live Game Match"
            val gameType = if (isChess) GameType.CHESS else GameType.LUDO

            Card(
              colors = CardDefaults.cardColors(containerColor = if (isChess) Color(0xFF192338) else Color(0xFF2A1C3E)),
              shape = RoundedCornerShape(18.dp),
              border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isChess) IndigoLight else AmberXp),
              modifier = Modifier
                .widthIn(max = 290.dp)
                .testTag("dm_game_card_${msg.id}")
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(gameIcon, fontSize = 22.sp)
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = "Real-Time Game",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isChess) IndigoLight else AmberXp
                    )
                    Text(
                      text = gameTitle,
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                  }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = if (isMe) "You challenged ${friend.username}" else "${friend.username} challenged you to a live game!",
                  fontSize = 12.sp,
                  color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (!isMe) {
                  val matchedInvite = pendingGameInvites.firstOrNull {
                    (it.senderId == friend.userId && it.recipientId == currentUserId) ||
                    (it.gameType == gameType)
                  }
                  Button(
                    onClick = {
                      if (matchedInvite != null) {
                        onAcceptGameInvite(matchedInvite.inviteId)
                      } else {
                        onChallengeGame(gameType)
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("btn_accept_dm_game_${msg.id}")
                  ) {
                    Text("Accept & Play Now 🎮", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                  }
                } else {
                  Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = "Waiting for ${friend.username} to accept...",
                      fontSize = 11.sp,
                      color = TextSecondary,
                      textAlign = TextAlign.Center,
                      modifier = Modifier.padding(vertical = 6.dp)
                    )
                  }
                }
              }
            }
          } else {
            // Distinct Message Bubbles: Sent = IndigoPrimary, Received = DarkSurfaceCard
            Surface(
              color = if (isMe) IndigoPrimary else DarkSurfaceCard,
              border = if (!isMe) androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder) else null,
              shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
              ),
              modifier = Modifier.widthIn(max = 290.dp)
            ) {
              Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text(
                  text = msg.text,
                  color = Color.White,
                  fontSize = 14.sp,
                  lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.End,
                  modifier = Modifier.align(Alignment.End)
                ) {
                  Text(
                    text = formatModernTimestamp(msg.timestamp),
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 10.sp
                  )
                  if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    MessageStatusCheckmarks(msg.status)
                  }
                }
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
                .padding(top = 2.dp, end = 4.dp)
            )
          }
        }
      }

      // Typing indicator when friend is online
      if (friend.isOnline) {
        item {
          TypingIndicatorBubble(friend.username)
        }
      }
    }
  }
}

@Composable
fun MessageStatusCheckmarks(status: MessageDeliveryStatus) {
  val checkmarkColor = when (status) {
    MessageDeliveryStatus.READ -> Color(0xFF00E5FF) // Electric cyan/blue for read
    MessageDeliveryStatus.DELIVERED, MessageDeliveryStatus.SENT -> Color.White.copy(alpha = 0.7f)
  }
  Row(verticalAlignment = Alignment.CenterVertically) {
    when (status) {
      MessageDeliveryStatus.SENT -> {
        Text("✓", fontSize = 11.sp, color = checkmarkColor, fontWeight = FontWeight.Bold)
      }
      MessageDeliveryStatus.DELIVERED, MessageDeliveryStatus.READ -> {
        Text("✓✓", fontSize = 11.sp, color = checkmarkColor, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun TypingIndicatorBubble(username: String) {
  val transition = rememberInfiniteTransition(label = "typing_dots")
  val dot1Alpha by transition.animateFloat(
    initialValue = 0.25f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(500, delayMillis = 0), RepeatMode.Reverse),
    label = "dot1"
  )
  val dot2Alpha by transition.animateFloat(
    initialValue = 0.25f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(500, delayMillis = 180), RepeatMode.Reverse),
    label = "dot2"
  )
  val dot3Alpha by transition.animateFloat(
    initialValue = 0.25f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(500, delayMillis = 360), RepeatMode.Reverse),
    label = "dot3"
  )

  Surface(
    color = DarkSurfaceCard,
    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
    modifier = Modifier.padding(vertical = 4.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
      Text("$username is typing", fontSize = 11.sp, color = TextSecondary)
      Spacer(modifier = Modifier.width(6.dp))
      Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(IndigoLight.copy(alpha = dot1Alpha)))
      Spacer(modifier = Modifier.width(3.dp))
      Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(IndigoLight.copy(alpha = dot2Alpha)))
      Spacer(modifier = Modifier.width(3.dp))
      Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(IndigoLight.copy(alpha = dot3Alpha)))
    }
  }
}

private fun formatModernTimestamp(timeMs: Long): String {
  val now = System.currentTimeMillis()
  val diff = now - timeMs
  val calendar = Calendar.getInstance().apply { timeInMillis = timeMs }
  val today = Calendar.getInstance()
  return when {
    diff < 60_000L -> "Just now"
    today.get(Calendar.DATE) == calendar.get(Calendar.DATE) &&
    today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) -> {
      val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
      timeFormat.format(Date(timeMs))
    }
    diff < 2 * 86400_000L -> "Yesterday"
    diff < 7 * 86400_000L -> {
      val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
      dayFormat.format(Date(timeMs))
    }
    else -> {
      val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
      dateFormat.format(Date(timeMs))
    }
  }
}

private fun formatTimestamp(timeMs: Long): String {
  return formatModernTimestamp(timeMs)
}
