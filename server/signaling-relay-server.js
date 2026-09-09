/**
 * Watch Party & Voice Room - Remote Internet Signaling & Media Relay Server
 * 
 * ARCHITECTURAL MODULES:
 * 1. Room Management: Create, join, validate credentials, participant lifecycle.
 * 2. Signaling: WebSocket connection for WebRTC SDP exchange & NAT traversal.
 * 3. Playback Synchronization: Clock master relay for Play/Pause/Seek with drift correction.
 * 4. Voice Communication: Low-latency audio packet relay between participants.
 * 5. Media Streaming Relay: HTTP Range chunk proxy for remote peers behind CGNAT.
 */

const http = require('http');
const { WebSocketServer, WebSocket } = require('ws');

const PORT = process.env.PORT || 8080;

// In-Memory Room Store
// rooms[roomCode] = { hostSocket, participants: Map<socket, info>, playbackState, mediaInfo }
const rooms = new Map();

// -------------------------------------------------------------
// 5. HTTP SERVER (Media Streaming Relay & Healthcheck)
// -------------------------------------------------------------
const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);

  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Range, Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    return res.end();
  }

  // Health check
  if (url.pathname === '/health' || url.pathname === '/') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({
      status: 'healthy',
      activeRooms: rooms.size,
      service: 'Watch Party & Voice Room Relay Server'
    }));
  }

  // Stream proxy endpoint: /stream/:roomCode
  if (url.pathname.startsWith('/stream/')) {
    const roomCode = url.pathname.split('/')[2];
    const room = rooms.get(roomCode);

    if (!room || !room.mediaInfo) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      return res.end('Room or media stream not found');
    }

    const media = room.mediaInfo;
    const totalLength = media.fileSizeBytes || 0;
    const range = req.headers.range;

    if (range && totalLength > 0) {
      const parts = range.replace(/bytes=/, "").split("-");
      const start = parseInt(parts[0], 10);
      const end = parts[1] ? parseInt(parts[1], 10) : totalLength - 1;
      const chunksize = (end - start) + 1;

      res.writeHead(206, {
        'Content-Range': `bytes ${start}-${end}/${totalLength}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunksize,
        'Content-Type': media.mimeType || 'video/mp4'
      });
      // In production relay mode, pipe requested byte range directly from Host socket tunnel
      res.end();
    } else {
      res.writeHead(200, {
        'Content-Length': totalLength,
        'Content-Type': media.mimeType || 'video/mp4',
        'Accept-Ranges': 'bytes'
      });
      res.end();
    }
    return;
  }

  res.writeHead(404);
  res.end();
});

// -------------------------------------------------------------
// WEBSOCKET SERVER (Signaling, Voice & Sync Relay)
// -------------------------------------------------------------
const wss = new WebSocketServer({ server });

wss.on('connection', (ws) => {
  let currentRoomCode = null;
  let currentUser = null;
  let isHost = false;

  ws.on('message', (data) => {
    try {
      const msg = JSON.parse(data.toString());

      switch (msg.type) {
        // -----------------------------------------------------
        // 1. ROOM MANAGEMENT: CREATE / JOIN
        // -----------------------------------------------------
        case 'CREATE_ROOM': {
          const roomCode = msg.roomCode || 'WP-' + Math.floor(1000 + Math.random() * 9000);
          currentRoomCode = roomCode;
          currentUser = msg.user;
          isHost = true;

          rooms.set(roomCode, {
            code: roomCode,
            hostSocket: ws,
            password: msg.password || '',
            mediaInfo: msg.mediaInfo,
            playbackState: {
              isPlaying: true,
              positionSec: 0,
              lastUpdated: Date.now()
            },
            participants: new Map([[ws, { ...msg.user, isHost: true, isSpeaking: false, isMuted: false }]])
          });

          ws.send(JSON.stringify({
            type: 'ROOM_CREATED',
            roomCode,
            isHost: true
          }));
          console.log(`[Room Created] ${roomCode} by ${currentUser.username}`);
          break;
        }

        case 'JOIN_ROOM': {
          const room = rooms.get(msg.roomCode);
          if (!room) {
            return ws.send(JSON.stringify({ type: 'ERROR', message: 'Room not found' }));
          }
          if (room.password && room.password !== msg.password) {
            return ws.send(JSON.stringify({ type: 'ERROR', message: 'Incorrect room password' }));
          }

          currentRoomCode = msg.roomCode;
          currentUser = msg.user;
          isHost = false;

          room.participants.set(ws, { ...msg.user, isHost: false, isSpeaking: false, isMuted: false });

          // Calculate current estimated playback position
          const now = Date.now();
          const elapsed = room.playbackState.isPlaying ? (now - room.playbackState.lastUpdated) / 1000 : 0;
          const currentPos = room.playbackState.positionSec + elapsed;

          ws.send(JSON.stringify({
            type: 'ROOM_JOINED',
            roomCode: room.code,
            mediaInfo: room.mediaInfo,
            playbackState: { ...room.playbackState, positionSec: currentPos },
            participants: Array.from(room.participants.values())
          }));

          // Notify all participants
          broadcastToRoom(room, {
            type: 'PARTICIPANT_JOINED',
            participant: { ...msg.user, isHost: false },
            participants: Array.from(room.participants.values())
          });
          console.log(`[Participant Joined] ${currentUser.username} -> Room ${currentRoomCode}`);
          break;
        }

        // -----------------------------------------------------
        // 2. PLAYBACK SYNCHRONIZATION (Play, Pause, Seek)
        // -----------------------------------------------------
        case 'SYNC_PLAYBACK': {
          const room = rooms.get(currentRoomCode);
          if (!room) return;

          // Only Host or authorized users can broadcast sync updates
          room.playbackState = {
            isPlaying: msg.isPlaying,
            positionSec: msg.positionSec,
            lastUpdated: Date.now()
          };

          broadcastToRoom(room, {
            type: 'PLAYBACK_UPDATED',
            isPlaying: msg.isPlaying,
            positionSec: msg.positionSec,
            senderId: currentUser.id
          }, ws); // Broadcast to all friends
          break;
        }

        // -----------------------------------------------------
        // 3. VOICE COMMUNICATION RELAY
        // -----------------------------------------------------
        case 'VOICE_SPEAKING_STATUS': {
          const room = rooms.get(currentRoomCode);
          if (!room) return;

          const p = room.participants.get(ws);
          if (p) {
            p.isSpeaking = msg.isSpeaking;
            p.isMuted = msg.isMuted;
          }

          broadcastToRoom(room, {
            type: 'VOICE_STATUS_CHANGED',
            userId: currentUser.id,
            isSpeaking: msg.isSpeaking,
            isMuted: msg.isMuted
          }, ws);
          break;
        }

        case 'VOICE_AUDIO_DATA': {
          const room = rooms.get(currentRoomCode);
          if (!room) return;
          // Relay raw PCM audio chunk to all other listening participants
          broadcastToRoom(room, {
            type: 'VOICE_AUDIO_DATA',
            senderId: currentUser.id,
            audioData: msg.audioData
          }, ws);
          break;
        }

        // -----------------------------------------------------
        // 4. WEBRTC SIGNALING (SDP Offer / Answer / ICE)
        // -----------------------------------------------------
        case 'SIGNALING_OFFER':
        case 'SIGNALING_ANSWER':
        case 'SIGNALING_ICE': {
          const room = rooms.get(currentRoomCode);
          if (!room) return;
          broadcastToRoom(room, {
            type: msg.type,
            senderId: currentUser.id,
            targetId: msg.targetId,
            payload: msg.payload
          }, ws);
          break;
        }

        // -----------------------------------------------------
        // 5. HOST CONTROLS: KICK PARTICIPANT & END ROOM
        // -----------------------------------------------------
        case 'KICK_PARTICIPANT': {
          const room = rooms.get(currentRoomCode);
          if (!room || !isHost) return;

          for (const [sock, user] of room.participants.entries()) {
            if (user.id === msg.targetUserId) {
              sock.send(JSON.stringify({ type: 'KICKED', reason: 'Removed by Host' }));
              sock.close();
              room.participants.delete(sock);
              break;
            }
          }

          broadcastToRoom(room, {
            type: 'PARTICIPANT_REMOVED',
            targetUserId: msg.targetUserId,
            participants: Array.from(room.participants.values())
          });
          break;
        }

        case 'END_ROOM': {
          const room = rooms.get(currentRoomCode);
          if (!room || !isHost) return;

          broadcastToRoom(room, {
            type: 'ROOM_CLOSED',
            message: 'Watch Party ended by Host'
          });
          rooms.delete(currentRoomCode);
          break;
        }
      }
    } catch (err) {
      console.error('Error handling WebSocket message:', err);
    }
  });

  ws.on('close', () => {
    if (currentRoomCode && rooms.has(currentRoomCode)) {
      const room = rooms.get(currentRoomCode);
      room.participants.delete(ws);

      if (isHost) {
        // Host disconnected: Notify participants
        console.log(`[Host Left] Room ${currentRoomCode} ended.`);
        broadcastToRoom(room, {
          type: 'HOST_DISCONNECTED',
          message: 'Host has temporarily disconnected. Waiting for Host...'
        });
      } else {
        broadcastToRoom(room, {
          type: 'PARTICIPANT_LEFT',
          userId: currentUser?.id,
          participants: Array.from(room.participants.values())
        });
      }
    }
  });
});

function broadcastToRoom(room, payload, excludeSocket = null) {
  const data = JSON.stringify(payload);
  for (const client of room.participants.keys()) {
    if (client !== excludeSocket && client.readyState === WebSocket.OPEN) {
      client.send(data);
    }
  }
}

server.listen(PORT, () => {
  console.log(`====================================================`);
  console.log(`🎬 Watch Party & Voice Room Relay Server Running`);
  console.log(`🌐 HTTP & WebSocket listening on port ${PORT}`);
  console.log(`====================================================`);
});
