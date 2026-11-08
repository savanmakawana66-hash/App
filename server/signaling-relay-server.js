/**
 * Watch Party & Voice Room - Remote Internet Signaling & Media Relay Server
 * 
 * ARCHITECTURAL MODULES:
 * 1. Room Management: Create, join, validate credentials, participant lifecycle over Internet.
 * 2. Media Delivery System: HTTP Range Request chunk streamer (206 Partial Content) & upload publisher.
 * 3. Playback Synchronization: Server-clock authoritative master relay for Play/Pause/Seek with drift correction.
 * 4. Voice Communication: Low-latency audio packet relay between participants.
 * 5. WebRTC Signaling: SDP Offer/Answer/ICE candidate relay for peer mesh NAT traversal.
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const { WebSocketServer, WebSocket } = require('ws');

// Listen on port 3000 (proxied by Nginx at port 8080)
const PORT = process.env.SERVER_PORT || 3000;

// Temporary media cache directory for published local files
const MEDIA_STORAGE_DIR = path.join('/tmp', 'watchparty_media');
try {
  if (!fs.existsSync(MEDIA_STORAGE_DIR)) {
    fs.mkdirSync(MEDIA_STORAGE_DIR, { recursive: true });
  }
} catch (e) {
  console.error('Failed to create media directory:', e);
}

// In-Memory Room Store
// rooms[roomCode] = { code, hostSocket, hostId, hostName, password, mediaInfo, playbackState, participants, streamUrl, lastHeartbeat }
const rooms = new Map();

// -------------------------------------------------------------
// 1. HTTP REST & STREAMING RELAY SERVER
// -------------------------------------------------------------
const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);

  // Permissive CORS Headers for all remote internet clients
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, HEAD, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Range, Content-Type, Authorization, X-Client-Timestamp');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    return res.end();
  }

  // Health check & stats
  if (url.pathname === '/health' || url.pathname === '/') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({
      status: 'healthy',
      activeRooms: rooms.size,
      service: 'Watch Party Remote Internet & Media Relay Server',
      timestamp: Date.now()
    }));
  }

  // Active rooms list: GET /api/v1/rooms/active
  if (url.pathname === '/api/v1/rooms/active') {
    const activeList = [];
    for (const room of rooms.values()) {
      const now = Date.now();
      const elapsed = room.playbackState.isPlaying ? (now - room.playbackState.lastUpdated) / 1000 : 0;
      const currentPos = room.playbackState.positionSec + elapsed;

      activeList.push({
        id: room.code,
        name: room.name || `${room.mediaInfo?.title || 'Video'} Watch Party`,
        ownerId: room.hostId,
        ownerName: room.hostName || 'Host',
        isPrivate: !!room.password,
        participantCount: room.participants.size,
        mediaTitle: room.mediaInfo?.title || '',
        mediaType: room.mediaInfo?.mediaType || 'VIDEO',
        isPlaying: room.playbackState.isPlaying,
        positionSec: currentPos,
        durationSec: room.mediaInfo?.durationSec || 1800,
        streamUrl: room.streamUrl || `/stream/${room.code}`,
        createdAtMs: room.createdAtMs || Date.now()
      });
    }

    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(activeList));
  }

  // Room details: GET /api/v1/rooms/:roomCode
  if (url.pathname.startsWith('/api/v1/rooms/')) {
    const roomCode = url.pathname.split('/')[4]?.toUpperCase();
    const room = rooms.get(roomCode);
    if (!room) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'Room not found or offline' }));
    }

    const now = Date.now();
    const elapsed = room.playbackState.isPlaying ? (now - room.playbackState.lastUpdated) / 1000 : 0;
    const currentPos = room.playbackState.positionSec + elapsed;

    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({
      roomCode: room.code,
      name: room.name,
      hostId: room.hostId,
      hostName: room.hostName,
      isPrivate: !!room.password,
      mediaInfo: room.mediaInfo,
      playbackState: {
        isPlaying: room.playbackState.isPlaying,
        positionSec: currentPos,
        lastUpdated: room.playbackState.lastUpdated,
        serverTimestamp: now
      },
      participantCount: room.participants.size,
      streamUrl: room.streamUrl || `/stream/${room.code}`
    }));
  }

  // Media publish / upload endpoint: POST /api/v1/media/upload/:roomCode
  if ((req.method === 'POST' || req.method === 'PUT') && url.pathname.startsWith('/api/v1/media/upload/')) {
    const roomCode = url.pathname.split('/')[5]?.toUpperCase();
    const room = rooms.get(roomCode);
    const contentType = req.headers['content-type'] || '';

    const targetFile = path.join(MEDIA_STORAGE_DIR, `${roomCode}.bin`);
    const fileWriteStream = fs.createWriteStream(targetFile);

    let bytesReceived = 0;
    const isMultipart = contentType.includes('multipart/form-data');
    let boundary = null;
    if (isMultipart) {
      const match = contentType.match(/boundary=(?:"([^"]+)"|([^;]+))/i);
      if (match) {
        boundary = match[1] || match[2];
      }
    }

    if (!isMultipart || !boundary) {
      req.on('data', (chunk) => {
        bytesReceived += chunk.length;
        fileWriteStream.write(chunk);
      });
    } else {
      let headerEnded = false;
      let buffer = Buffer.alloc(0);

      req.on('data', (chunk) => {
        if (!headerEnded) {
          buffer = Buffer.concat([buffer, chunk]);
          const headerEndIndex = buffer.indexOf('\r\n\r\n');
          if (headerEndIndex !== -1) {
            headerEnded = true;
            const fileData = buffer.slice(headerEndIndex + 4);
            bytesReceived += fileData.length;
            fileWriteStream.write(fileData);
            buffer = Buffer.alloc(0);
          }
        } else {
          bytesReceived += chunk.length;
          fileWriteStream.write(chunk);
        }
      });
    }

    req.on('end', () => {
      fileWriteStream.end();
      // If multipart, trim trailing boundary delimiter
      if (isMultipart && boundary && bytesReceived > 0) {
        try {
          const stats = fs.statSync(targetFile);
          if (stats.size > 100) {
            const tailSize = Math.min(stats.size, 512);
            const fd = fs.openSync(targetFile, 'r+');
            const tailBuf = Buffer.alloc(tailSize);
            fs.readSync(fd, tailBuf, 0, tailSize, stats.size - tailSize);
            const boundaryIdx = tailBuf.indexOf(`\r\n--${boundary}`);
            if (boundaryIdx !== -1) {
              const newSize = (stats.size - tailSize) + boundaryIdx;
              fs.ftruncateSync(fd, newSize);
              bytesReceived = newSize;
            }
            fs.closeSync(fd);
          }
        } catch (_) {}
      }

      if (room) {
        room.hasUploadedMedia = true;
        room.mediaFilePath = targetFile;
        room.mediaFileSize = bytesReceived;
        if (room.mediaInfo) {
          room.mediaInfo.fileSizeBytes = bytesReceived;
        }
      }

      console.log(`[Media Uploaded] Room ${roomCode} (${isMultipart ? 'multipart' : 'raw chunked'}): ${bytesReceived} bytes saved to ${targetFile}`);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({
        status: 'success',
        roomCode,
        fileSizeBytes: bytesReceived,
        streamUrl: `/stream/${roomCode}`
      }));
    });

    req.on('error', (err) => {
      console.error(`[Upload Error] Room ${roomCode}:`, err);
      fileWriteStream.end();
      res.writeHead(500, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: err.message }));
    });
    return;
  }

  // Media Streaming Proxy & HTTP Range Request server: GET or HEAD /stream/:roomCode
  if (url.pathname.startsWith('/stream/')) {
    const roomCode = url.pathname.split('/')[2]?.toUpperCase();
    const room = rooms.get(roomCode);

    const filePath = path.join(MEDIA_STORAGE_DIR, `${roomCode}.bin`);
    const hasLocalFile = fs.existsSync(filePath);

    // If room has an external direct internet URL (e.g. Big Buck Bunny, CDN MP4)
    if (room && room.mediaInfo && room.mediaInfo.uriString && room.mediaInfo.uriString.startsWith('http')) {
      res.writeHead(302, { 'Location': room.mediaInfo.uriString });
      return res.end();
    }

    if (!hasLocalFile && (!room || !room.mediaInfo)) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      return res.end('Room media not published yet. Please wait for Host.');
    }

    let totalLength = 0;
    let mimeType = room?.mediaInfo?.mimeType || 'video/mp4';

    if (hasLocalFile) {
      const stat = fs.statSync(filePath);
      totalLength = stat.size;
    } else if (room?.mediaInfo?.fileSizeBytes) {
      totalLength = room.mediaInfo.fileSizeBytes;
    }

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
        'Content-Type': mimeType
      });

      if (req.method === 'HEAD') {
        return res.end();
      }

      if (hasLocalFile) {
        const fileStream = fs.createReadStream(filePath, { start, end });
        fileStream.pipe(res);
        fileStream.on('error', (err) => {
          console.error('[Stream Error]', err);
          res.end();
        });
      } else {
        res.end();
      }
    } else {
      res.writeHead(200, {
        'Content-Length': totalLength,
        'Content-Type': mimeType,
        'Accept-Ranges': 'bytes'
      });

      if (req.method === 'HEAD') {
        return res.end();
      }

      if (hasLocalFile) {
        const fileStream = fs.createReadStream(filePath);
        fileStream.pipe(res);
      } else {
        res.end();
      }
    }
    return;
  }

  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

// -------------------------------------------------------------
// 2. WEBSOCKET SERVER (Signaling, Voice, Chat & Playback Sync)
// -------------------------------------------------------------
const wss = new WebSocketServer({ server });

wss.on('connection', (ws, req) => {
  let currentRoomCode = null;
  let currentUser = null;
  let isHost = false;

  ws.isAlive = true;
  ws.on('pong', () => { ws.isAlive = true; });

  ws.on('message', (data) => {
    try {
      const msg = JSON.parse(data.toString());

      switch (msg.type || msg.action) {
        // -----------------------------------------------------
        // ROOM REGISTRATION & LIFECYCLE
        // -----------------------------------------------------
        case 'CREATE_ROOM': {
          const roomCode = (msg.roomCode || 'WP-' + Math.floor(1000 + Math.random() * 9000)).toUpperCase();
          currentRoomCode = roomCode;
          currentUser = msg.user || { id: 'host_' + Date.now(), username: 'Host' };
          isHost = true;

          const streamUrl = msg.streamUrl || `/stream/${roomCode}`;

          rooms.set(roomCode, {
            code: roomCode,
            name: msg.name || `${msg.mediaInfo?.title || 'Video'} Watch Party`,
            hostSocket: ws,
            hostId: currentUser.id,
            hostName: currentUser.username,
            password: msg.password || '',
            mediaInfo: msg.mediaInfo,
            streamUrl: streamUrl,
            createdAtMs: Date.now(),
            lastHeartbeat: Date.now(),
            playbackState: {
              isPlaying: msg.isPlaying !== undefined ? msg.isPlaying : true,
              positionSec: msg.positionSec || 0,
              lastUpdated: Date.now()
            },
            participants: new Map([[ws, { ...currentUser, isHost: true, isSpeaking: false, isMuted: false }]])
          });

          ws.send(JSON.stringify({
            type: 'ROOM_CREATED',
            roomCode,
            isHost: true,
            streamUrl,
            serverTimestamp: Date.now()
          }));
          console.log(`[Room Created] ${roomCode} by ${currentUser.username}`);
          break;
        }

        case 'JOIN_ROOM': {
          const cleanCode = (msg.roomCode || '').trim().toUpperCase();
          const room = rooms.get(cleanCode);
          if (!room) {
            return ws.send(JSON.stringify({ type: 'ERROR', message: `Room '${cleanCode}' not found or host has gone offline.` }));
          }
          if (room.password && room.password !== msg.password) {
            return ws.send(JSON.stringify({ type: 'ERROR', message: 'Incorrect room password.' }));
          }

          currentRoomCode = cleanCode;
          currentUser = msg.user || { id: 'peer_' + Date.now(), username: 'Guest' };
          isHost = false;

          room.participants.set(ws, { ...currentUser, isHost: false, isSpeaking: false, isMuted: false });

          // Calculate current estimated playback position using server clock
          const now = Date.now();
          const elapsed = room.playbackState.isPlaying ? (now - room.playbackState.lastUpdated) / 1000 : 0;
          const currentPos = room.playbackState.positionSec + elapsed;

          ws.send(JSON.stringify({
            type: 'ROOM_JOINED',
            roomCode: room.code,
            mediaInfo: room.mediaInfo,
            playbackState: { ...room.playbackState, positionSec: currentPos },
            streamUrl: room.streamUrl || `/stream/${room.code}`,
            serverTimestamp: now,
            participants: Array.from(room.participants.values())
          }));

          // Notify existing participants
          broadcastToRoom(room, {
            type: 'PARTICIPANT_JOINED',
            participant: { ...currentUser, isHost: false },
            participants: Array.from(room.participants.values())
          }, ws);

          console.log(`[Participant Joined] ${currentUser.username} -> Room ${currentRoomCode}`);
          break;
        }

        // -----------------------------------------------------
        // PLAYBACK SYNCHRONIZATION (Play, Pause, Seek, Speed)
        // -----------------------------------------------------
        case 'SYNC_PLAYBACK': {
          const room = rooms.get(currentRoomCode);
          if (!room) return;

          const now = Date.now();
          room.playbackState = {
            isPlaying: msg.isPlaying,
            positionSec: msg.positionSec,
            lastUpdated: now
          };

          broadcastToRoom(room, {
            type: 'PLAYBACK_UPDATED',
            isPlaying: msg.isPlaying,
            positionSec: msg.positionSec,
            playbackSpeed: msg.playbackSpeed || 1.0,
            serverTimestamp: now,
            senderId: currentUser?.id
          }, ws);
          break;
        }

        case 'MEDIA_CHANGED': {
          const room = rooms.get(currentRoomCode);
          if (!room || !isHost) return;

          room.mediaInfo = msg.mediaInfo;
          room.streamUrl = msg.streamUrl || `/stream/${room.code}`;
          room.playbackState = {
            isPlaying: true,
            positionSec: msg.positionSec || 0,
            lastUpdated: Date.now()
          };

          broadcastToRoom(room, {
            type: 'MEDIA_CHANGED',
            mediaInfo: room.mediaInfo,
            streamUrl: room.streamUrl,
            positionSec: msg.positionSec || 0,
            serverTimestamp: Date.now()
          });
          break;
        }

        // -----------------------------------------------------
        // HEARTBEAT
        // -----------------------------------------------------
        case 'HEARTBEAT': {
          const room = rooms.get(currentRoomCode);
          if (room) {
            room.lastHeartbeat = Date.now();
            if (isHost && msg.positionSec !== undefined) {
              room.playbackState.positionSec = msg.positionSec;
              room.playbackState.isPlaying = msg.isPlaying !== undefined ? msg.isPlaying : room.playbackState.isPlaying;
              room.playbackState.lastUpdated = Date.now();
            }
          }
          ws.send(JSON.stringify({ type: 'HEARTBEAT_ACK', timestamp: Date.now() }));
          break;
        }

        // -----------------------------------------------------
        // CHAT & REACTION BURSTS
        // -----------------------------------------------------
        case 'CHAT_MESSAGE': {
          const room = rooms.get(currentRoomCode);
          if (!room) return;
          broadcastToRoom(room, {
            type: 'CHAT_MESSAGE',
            senderId: currentUser?.id,
            username: currentUser?.username || 'Member',
            message: msg.message,
            timestamp: Date.now()
          });
          break;
        }

        case 'REACTION_BURST': {
          const room = rooms.get(currentRoomCode);
          if (!room) return;
          broadcastToRoom(room, {
            type: 'REACTION_BURST',
            senderId: currentUser?.id,
            emoji: msg.emoji,
            timestamp: Date.now()
          }, ws);
          break;
        }

        // -----------------------------------------------------
        // VOICE COMMUNICATION RELAY
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
            userId: currentUser?.id,
            isSpeaking: msg.isSpeaking,
            isMuted: msg.isMuted
          }, ws);
          break;
        }

        case 'VOICE_AUDIO_DATA': {
          const room = rooms.get(currentRoomCode);
          if (!room) return;
          broadcastToRoom(room, {
            type: 'VOICE_AUDIO_DATA',
            senderId: currentUser?.id,
            audioData: msg.audioData
          }, ws);
          break;
        }

        // -----------------------------------------------------
        // WEBRTC SIGNALING (SDP Offer / Answer / ICE)
        // -----------------------------------------------------
        case 'SIGNALING_OFFER':
        case 'SIGNALING_ANSWER':
        case 'SIGNALING_ICE':
        case 'SIGNAL': {
          const room = rooms.get(currentRoomCode);
          if (!room) return;
          broadcastToRoom(room, {
            type: msg.type || msg.action,
            senderId: currentUser?.id,
            targetId: msg.targetId,
            payload: msg.payload || msg
          }, ws);
          break;
        }

        // -----------------------------------------------------
        // HOST ACTIONS
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
          cleanupRoomFiles(currentRoomCode);
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
        console.log(`[Host Disconnected] Room ${currentRoomCode}`);
        broadcastToRoom(room, {
          type: 'HOST_DISCONNECTED',
          message: 'Host has temporarily disconnected. Reconnecting…'
        });
        // Give host 30s to reconnect before pruning
        setTimeout(() => {
          const r = rooms.get(currentRoomCode);
          if (r && !Array.from(r.participants.values()).some(p => p.isHost)) {
            console.log(`[Room Pruned] Room ${currentRoomCode} expired after host left`);
            cleanupRoomFiles(currentRoomCode);
            rooms.delete(currentRoomCode);
          }
        }, 30000);
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

function cleanupRoomFiles(roomCode) {
  try {
    const f = path.join(MEDIA_STORAGE_DIR, `${roomCode}.bin`);
    if (fs.existsSync(f)) {
      fs.unlinkSync(f);
    }
  } catch (e) {}
}

// Watchdog: Ping alive sockets & reap stale rooms every 10s
setInterval(() => {
  const now = Date.now();
  for (const [code, room] of rooms.entries()) {
    if (now - room.lastHeartbeat > 45000) {
      console.log(`[Watchdog] Pruning inactive room: ${code}`);
      cleanupRoomFiles(code);
      rooms.delete(code);
    }
  }

  wss.clients.forEach((ws) => {
    if (!ws.isAlive) return ws.terminate();
    ws.isAlive = false;
    ws.ping();
  });
}, 10000);

server.listen(PORT, () => {
  console.log(`====================================================`);
  console.log(`🎬 Watch Party & Media Relay Server Running`);
  console.log(`🌐 HTTP & WebSocket listening on port ${PORT}`);
  console.log(`📁 Media Cache: ${MEDIA_STORAGE_DIR}`);
  console.log(`====================================================`);
});
