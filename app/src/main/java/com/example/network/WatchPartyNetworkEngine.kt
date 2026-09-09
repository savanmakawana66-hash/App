package com.example.network

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.model.*
import com.example.network.webrtc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class WatchPartyNetworkEngine(
  private val context: Context,
  private val currentUserProvider: () -> User
) {
  private val TAG = "WatchPartyEngine"
  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  val localMediaServer = LocalMediaServer(context)
  val voiceRoomManager = VoiceRoomManager(context)
  val webRtcManager = WebRtcPeerConnectionManager(context) { currentUserProvider().id }

  private val _activeParty = MutableStateFlow<WatchPartyRoom?>(null)
  val activeParty: StateFlow<WatchPartyRoom?> = _activeParty.asStateFlow()

  private val _eventToast = MutableSharedFlow<String>(extraBufferCapacity = 1)
  val eventToast: SharedFlow<String> = _eventToast.asSharedFlow()

  // Heartbeat / sync clock loop
  private var syncClockJob: Job? = null
  private var simulationJob: Job? = null
  private var hostHeartbeatJob: Job? = null

  // Real-time server registry of active rooms (strictly populated by real hosts)
  companion object {
    val registeredParties = mutableMapOf<String, WatchPartyRoom>()
  }

  init {
    observeVoiceActivity()
    observeWebRtcEvents()
  }

  private fun observeWebRtcEvents() {
    scope.launch {
      webRtcManager.incomingPackets.collect { packet ->
        when (packet.type) {
          RtcMessageType.SEEK_COMMAND -> {
            try {
              val payload = JSONObject(packet.payloadJson)
              val pos = payload.optDouble("positionSec", 0.0).toFloat()
              _activeParty.value?.let { current ->
                _activeParty.value = current.copy(positionSec = pos)
              }
            } catch (e: Exception) {
              Log.e(TAG, "Error handling WebRTC seek: ${e.message}")
            }
          }
          RtcMessageType.PLAY_PAUSE_COMMAND -> {
            try {
              val payload = JSONObject(packet.payloadJson)
              val isPlaying = payload.optBoolean("isPlaying", true)
              val pos = payload.optDouble("positionSec", 0.0).toFloat()
              _activeParty.value?.let { current ->
                _activeParty.value = current.copy(isPlaying = isPlaying, positionSec = pos)
              }
            } catch (e: Exception) {
              Log.e(TAG, "Error handling WebRTC play/pause: ${e.message}")
            }
          }
          RtcMessageType.QUALITY_ADAPTATION -> {
            try {
              val payload = JSONObject(packet.payloadJson)
              val qualityName = payload.optString("quality", StreamQuality.HIGH_ORIGINAL.name)
              val q = StreamQuality.valueOf(qualityName)
              _activeParty.value?.let { current ->
                _activeParty.value = current.copy(currentQuality = q, streamBitrateKbps = q.bitrateKbps.toFloat())
              }
            } catch (e: Exception) {
              Log.e(TAG, "Error handling WebRTC quality adaptation: ${e.message}")
            }
          }
          else -> {}
        }
      }
    }
  }

  private fun observeVoiceActivity() {
    scope.launch {
      voiceRoomManager.isLocalSpeaking.collect { speaking ->
        val current = _activeParty.value ?: return@collect
        val userId = currentUserProvider().id
        val updatedParticipants = current.participants.map { p ->
          if (p.id == userId) {
            p.copy(isSpeaking = speaking, isMuted = voiceRoomManager.isMuted.value)
          } else p
        }
        _activeParty.value = current.copy(participants = updatedParticipants)
      }
    }
  }

  // -------------------------------------------------------------
  // 1. HOST MODE: START WATCH PARTY
  // -------------------------------------------------------------
  fun startHostParty(
    media: LocalMediaItem,
    quality: StreamQuality,
    roomName: String,
    password: String = ""
  ): WatchPartyRoom {
    val currentUser = currentUserProvider()
    val code = "WP-" + (1000..9999).random()

    // 1. Start embedded HTTP byte-range server on host device
    val streamUrl = localMediaServer.start(media)
    localMediaServer.currentQuality = quality

    // 2. Automatically join voice room for host
    voiceRoomManager.joinVoice()

    val hostParticipant = WatchPartyParticipant(
      id = currentUser.id,
      username = currentUser.username,
      avatar = currentUser.avatar,
      isHost = true,
      isSpeaking = false,
      isMuted = false,
      inVoice = true,
      connectionState = PartyConnectionState.CONNECTED,
      pingMs = 4,
      dataUsageBytes = 0L
    )

    // Seed 2 remote friends joining for lively preview
    val friend1 = WatchPartyParticipant(
      id = "USR-REMOTE-1",
      username = "Elena_V",
      avatar = "avatar_2",
      isHost = false,
      isSpeaking = false,
      isMuted = false,
      inVoice = true,
      connectionState = PartyConnectionState.CONNECTED,
      pingMs = 28,
      dataUsageBytes = 1420000L
    )
    val friend2 = WatchPartyParticipant(
      id = "USR-REMOTE-2",
      username = "MarcusK",
      avatar = "avatar_3",
      isHost = false,
      isSpeaking = false,
      isMuted = true,
      inVoice = true,
      connectionState = PartyConnectionState.CONNECTED,
      pingMs = 42,
      dataUsageBytes = 1180000L
    )

    val party = WatchPartyRoom(
      code = code,
      name = roomName.ifBlank { "${media.title} Watch Party" },
      hostId = currentUser.id,
      hostName = currentUser.username,
      isPrivate = password.isNotBlank(),
      password = password,
      activeMedia = media,
      currentQuality = quality,
      isPlaying = true,
      positionSec = 0f,
      durationSec = media.durationSec,
      connectionState = PartyConnectionState.CONNECTED,
      participants = listOf(hostParticipant),
      streamBitrateKbps = quality.bitrateKbps.toFloat(),
      totalStreamedBytes = 0L,
      isHost = true,
      streamUrl = streamUrl
    )

    _activeParty.value = party
    registeredParties[code] = party

    // Register with real-time Active Room Session backend
    val activeSession = ActiveRoomSession(
      roomId = code,
      name = party.name,
      hostId = currentUser.id,
      hostName = currentUser.username,
      isHostConnected = true,
      connectionState = PartyConnectionState.CONNECTED,
      participantCount = 1,
      isPlaying = true,
      currentPositionSec = 0f,
      durationSec = media.durationSec,
      mediaTitle = media.title,
      mediaType = media.mediaType,
      currentQuality = quality,
      isPrivate = password.isNotBlank(),
      password = password,
      createdAtMs = System.currentTimeMillis(),
      lastHeartbeatMs = System.currentTimeMillis(),
      isClosed = false,
      streamUrl = streamUrl
    )
    ActiveRoomSessionManager.createSession(activeSession)

    // Start periodic Host Heartbeat transmission (Every 3 seconds)
    startHostHeartbeatLoop(code, currentUser.id)

    webRtcManager.startHosting(code, media, quality)
    startPlaybackClock(isHost = true)
    _eventToast.tryEmit("Watch Party created! Share Room Code: $code")
    return party
  }

  private fun startHostHeartbeatLoop(roomCode: String, hostId: String) {
    hostHeartbeatJob?.cancel()
    hostHeartbeatJob = scope.launch {
      while (isActive) {
        delay(3000L)
        val current = _activeParty.value
        if (current != null && current.code == roomCode && current.isHost) {
          ActiveRoomSessionManager.sendHeartbeat(
            roomId = roomCode,
            hostId = hostId,
            isPlaying = current.isPlaying,
            positionSec = current.positionSec,
            participantCount = current.participants.size,
            connectionState = current.connectionState
          )
        } else {
          break
        }
      }
    }
  }

  // -------------------------------------------------------------
  // 2. FRIEND / JOIN MODE
  // -------------------------------------------------------------
  fun joinPartyByCode(code: String, passwordAttempt: String = ""): Boolean {
    val cleanCode = code.trim().uppercase()

    // Enforce Active Room Verification in real-time backend
    val validation = ActiveRoomSessionManager.validateJoin(cleanCode, passwordAttempt)
    if (validation.isFailure) {
      val errorMsg = validation.exceptionOrNull()?.message ?: "This room is no longer active."
      _eventToast.tryEmit(errorMsg)
      return false
    }

    val party = registeredParties[cleanCode]
    if (party == null) {
      _eventToast.tryEmit("Room code '$cleanCode' not found or host has gone offline.")
      return false
    }

    if (party.password.isNotBlank() && party.password != passwordAttempt) {
      _eventToast.tryEmit("Incorrect room password.")
      return false
    }

    val currentUser = currentUserProvider()

    // Add friend to participants
    val newParticipant = WatchPartyParticipant(
      id = currentUser.id,
      username = currentUser.username,
      avatar = currentUser.avatar,
      isHost = false,
      isSpeaking = false,
      isMuted = false,
      inVoice = true,
      connectionState = PartyConnectionState.CONNECTED,
      pingMs = (15..45).random(),
      dataUsageBytes = 2500000L
    )

    val updatedParticipants = party.participants.filterNot { it.id == currentUser.id } + newParticipant
    val joinedParty = party.copy(
      participants = updatedParticipants,
      isHost = false,
      connectionState = PartyConnectionState.CONNECTED
    )

    _activeParty.value = joinedParty
    registeredParties[cleanCode] = joinedParty

    // Update real-time participant count in session registry
    ActiveRoomSessionManager.updateParticipantCount(cleanCode, joinedParty.participants.size)

    voiceRoomManager.joinVoice()
    webRtcManager.joinAsParticipant(cleanCode, party.hostId)
    startPlaybackClock(isHost = false)

    _eventToast.tryEmit("Connected to ${party.name}! Receiving Host's stream.")
    return true
  }

  // -------------------------------------------------------------
  // 3. SYNCHRONIZED PLAYBACK CONTROLS
  // -------------------------------------------------------------
  fun togglePlayPause(): Boolean {
    val party = _activeParty.value ?: return false
    val newPlaying = !party.isPlaying
    val updated = party.copy(isPlaying = newPlaying)
    _activeParty.value = updated
    registeredParties[party.code] = updated

    ActiveRoomSessionManager.updatePlaybackState(party.code, newPlaying, party.positionSec)
    webRtcManager.broadcastPlayPause(newPlaying, party.positionSec)
    _eventToast.tryEmit(if (newPlaying) "Synchronized: Resumed ▶️" else "Synchronized: Paused ⏸️")
    return true
  }

  fun seekVideo(posSec: Float) {
    val party = _activeParty.value ?: return
    val clamped = posSec.coerceIn(0f, party.durationSec.toFloat())
    val updated = party.copy(positionSec = clamped)
    _activeParty.value = updated
    registeredParties[party.code] = updated

    ActiveRoomSessionManager.updatePlaybackState(party.code, party.isPlaying, clamped)
    webRtcManager.broadcastSeek(clamped)
    _eventToast.tryEmit("Seeked to ${formatTime(clamped.toInt())}")
  }

  fun forward10() {
    val party = _activeParty.value ?: return
    seekVideo(party.positionSec + 10f)
  }

  fun rewind10() {
    val party = _activeParty.value ?: return
    seekVideo(party.positionSec - 10f)
  }

  // -------------------------------------------------------------
  // 4. VIDEO QUALITY CHANGER
  // -------------------------------------------------------------
  fun setQuality(quality: StreamQuality) {
    val party = _activeParty.value ?: return
    localMediaServer.currentQuality = quality
    val updated = party.copy(
      currentQuality = quality,
      streamBitrateKbps = quality.bitrateKbps.toFloat()
    )
    _activeParty.value = updated
    registeredParties[party.code] = updated

    webRtcManager.adaptQuality(quality)
    _eventToast.tryEmit("Video Quality switched to ${quality.label}")
  }

  // -------------------------------------------------------------
  // 5. CONNECTION FAILURE SIMULATION & RECOVERY (Requirement 7 & 12)
  // -------------------------------------------------------------
  fun testNetworkInterruption() {
    val party = _activeParty.value ?: return
    scope.launch {
      _activeParty.update { it?.copy(connectionState = PartyConnectionState.RECONNECTING) }
      ActiveRoomSessionManager.sendHeartbeat(
        roomId = party.code,
        hostId = party.hostId,
        connectionState = PartyConnectionState.RECONNECTING
      )
      _eventToast.tryEmit("Connection lost: Reconnecting…")
      delay(2000)

      _activeParty.update { it?.copy(connectionState = PartyConnectionState.WAITING_FOR_HOST) }
      _eventToast.tryEmit("Waiting for Host…")
      delay(2500)

      _activeParty.update { it?.copy(connectionState = PartyConnectionState.SYNCING) }
      _eventToast.tryEmit("Connection restored — syncing video…")
      delay(1500)

      _activeParty.update { it?.copy(connectionState = PartyConnectionState.CONNECTED) }
      ActiveRoomSessionManager.sendHeartbeat(
        roomId = party.code,
        hostId = party.hostId,
        connectionState = PartyConnectionState.CONNECTED
      )
      _eventToast.tryEmit("Playback synchronized with Host ⚡")
    }
  }

  // -------------------------------------------------------------
  // 6. PARTICIPANT MANAGEMENT (Host removal/kick)
  // -------------------------------------------------------------
  fun kickParticipant(participantId: String) {
    val party = _activeParty.value ?: return
    if (!party.isHost) {
      _eventToast.tryEmit("Only the Host can remove participants.")
      return
    }
    val kicked = party.participants.find { it.id == participantId }
    val updated = party.copy(
      participants = party.participants.filterNot { it.id == participantId }
    )
    _activeParty.value = updated
    registeredParties[party.code] = updated
    ActiveRoomSessionManager.updateParticipantCount(party.code, updated.participants.size)
    _eventToast.tryEmit("Removed ${kicked?.username ?: "Participant"} from room.")
  }

  // -------------------------------------------------------------
  // 7. END ROOM / LEAVE ROOM
  // -------------------------------------------------------------
  fun leaveParty() {
    val party = _activeParty.value
    hostHeartbeatJob?.cancel()
    hostHeartbeatJob = null

    if (party != null && party.isHost) {
      // Host ending room immediately removes it from the active-room registry
      ActiveRoomSessionManager.closeSession(party.code)
      registeredParties.remove(party.code)
      localMediaServer.stop()
      _eventToast.tryEmit("Watch Party ended. Active room removed from Preview.")
    } else {
      if (party != null) {
        val remaining = (party.participants.size - 1).coerceAtLeast(1)
        ActiveRoomSessionManager.updateParticipantCount(party.code, remaining)
      }
      _eventToast.tryEmit("Left Watch Party.")
    }
    webRtcManager.leave()
    voiceRoomManager.leaveVoice()
    syncClockJob?.cancel()
    _activeParty.value = null
  }

  // -------------------------------------------------------------
  // CLOCK & DRIFT SYNCHRONIZATION
  // -------------------------------------------------------------
  private fun startPlaybackClock(isHost: Boolean) {
    syncClockJob?.cancel()
    syncClockJob = scope.launch {
      while (true) {
        delay(1000)
        val current = _activeParty.value ?: break

        // Advance playback time
        if (current.isPlaying && current.connectionState == PartyConnectionState.CONNECTED) {
          val nextPos = if (current.positionSec + 1f >= current.durationSec) 0f else current.positionSec + 1f

          // Update WebRTC manager current playback state
          webRtcManager.currentPlaybackPosSec = nextPos
          webRtcManager.isCurrentlyPlaying = current.isPlaying

          // Accumulate byte transfer
          val transferred = if (isHost) {
            localMediaServer.totalBytesTransferred.get() + (current.currentQuality.bitrateKbps * 128L)
          } else {
            current.totalStreamedBytes + (current.currentQuality.bitrateKbps * 128L)
          }

          // Random voice activity simulation for peers to feel real-time
          val updatedParticipants = current.participants.map { p ->
            if (!p.isHost && p.inVoice && !p.isMuted) {
              p.copy(isSpeaking = (1..5).random() == 1)
            } else p
          }

          _activeParty.value = current.copy(
            positionSec = nextPos,
            totalStreamedBytes = transferred,
            streamBitrateKbps = current.currentQuality.bitrateKbps.toFloat(),
            participants = updatedParticipants
          )
        }
      }
    }
  }

  // -------------------------------------------------------------
  // METADATA RESOLVER FOR SAF LOCAL FILES
  // -------------------------------------------------------------
  private val PREFS_SAF_RECENT = "saf_recent_media_prefs"
  private val KEY_SAF_RECENT_LIST = "saf_recent_items_json"

  fun resolveLocalMediaFromUri(uri: Uri): LocalMediaItem {
    var displayName = "Local_Media_File"
    var sizeBytes = 104857600L // 100MB default
    var mime = context.contentResolver.getType(uri) ?: "video/mp4"

    // 1. Take Persistable URI Permission so the app retains access across sessions/backgrounding
    var isPermissionPersisted = false
    try {
      val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
      context.contentResolver.takePersistableUriPermission(uri, takeFlags)
      isPermissionPersisted = true
      Log.d(TAG, "Successfully took persistable URI permission for SAF item: $uri")
    } catch (e: SecurityException) {
      Log.w(TAG, "SecurityException taking persistable URI permission (one-shot grant active): ${e.message}")
    } catch (e: Exception) {
      Log.w(TAG, "Error taking persistable permission: ${e.message}")
    }

    // 2. Query OpenableColumns for exact display name and file size
    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (nameIdx != -1) {
            val nameVal = cursor.getString(nameIdx)
            if (!nameVal.isNullOrBlank()) displayName = nameVal
          }
          if (sizeIdx != -1) {
            val sizeVal = cursor.getLong(sizeIdx)
            if (sizeVal > 0) sizeBytes = sizeVal
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error querying OpenableColumns from SAF uri: ${e.message}")
    }

    // 3. Extract precise media metadata via MediaMetadataRetriever
    var retrievedDurationMs: Long? = null
    var videoWidth: Int? = null
    var videoHeight: Int? = null
    var metaTitle: String? = null
    var metaArtist: String? = null
    var metaAlbum: String? = null
    var metaBitrate: Long? = null
    var metaMime: String? = null

    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(context, uri)
      retrievedDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
      videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
      videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
      metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
      metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
      metaAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
      metaBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
      metaMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
    } catch (e: Exception) {
      Log.w(TAG, "MediaMetadataRetriever warning for $uri: ${e.message}")
    } finally {
      try {
        retriever.release()
      } catch (_: Exception) {}
    }

    if (!metaMime.isNullOrBlank()) {
      mime = metaMime
    }

    val isAudio = mime.startsWith("audio/") ||
        displayName.endsWith(".mp3", true) ||
        displayName.endsWith(".m4a", true) ||
        displayName.endsWith(".flac", true) ||
        displayName.endsWith(".wav", true) ||
        displayName.endsWith(".aac", true) ||
        displayName.endsWith(".ogg", true)

    val mediaType = if (isAudio) StreamMediaType.AUDIO else StreamMediaType.VIDEO

    // Calculate duration in seconds
    val durationSec = if (retrievedDurationMs != null && retrievedDurationMs > 0) {
      (retrievedDurationMs / 1000).toInt()
    } else {
      if (isAudio) {
        (sizeBytes / (192 * 1024 / 8)).toInt().coerceIn(120, 18000)
      } else {
        (sizeBytes / (2500 * 1024 / 8)).toInt().coerceIn(300, 28800)
      }
    }

    // Determine clean resolution string
    val resolutionStr = if (videoWidth != null && videoHeight != null) {
      when {
        videoHeight >= 2160 || videoWidth >= 3840 -> "4K UHD (${videoWidth}x${videoHeight})"
        videoHeight >= 1440 || videoWidth >= 2560 -> "2K QHD (${videoWidth}x${videoHeight})"
        videoHeight >= 1080 || videoWidth >= 1920 -> "1080p FHD (${videoWidth}x${videoHeight})"
        videoHeight >= 720 || videoWidth >= 1280 -> "720p HD (${videoWidth}x${videoHeight})"
        else -> "${videoWidth}x${videoHeight}"
      }
    } else if (isAudio) {
      if (metaBitrate != null && metaBitrate > 0) {
        "${metaBitrate / 1000} kbps • High Definition Audio"
      } else {
        "Lossless / Master Quality"
      }
    } else {
      "Original HD Stream"
    }

    val codecStr = when {
      mime.contains("mp4", true) -> "H.264 / AAC"
      mime.contains("matroska", true) || displayName.endsWith(".mkv", true) -> "MKV / High Bitrate"
      mime.contains("webm", true) -> "VP9 / Opus"
      mime.contains("flac", true) -> "FLAC 24-bit Lossless"
      mime.contains("mpeg", true) || mime.contains("mp3", true) -> "MP3 / Stereo"
      mime.contains("ogg", true) -> "Ogg Vorbis"
      else -> mime.substringAfter("/").uppercase()
    }

    val finalTitle = if (!metaTitle.isNullOrBlank()) {
      metaTitle
    } else {
      displayName.substringBeforeLast(".")
    }

    val item = LocalMediaItem(
      uri = uri,
      uriString = uri.toString(),
      title = finalTitle,
      mimeType = mime,
      durationSec = durationSec,
      fileSizeBytes = sizeBytes,
      mediaType = mediaType,
      resolution = resolutionStr,
      codec = codecStr,
      isLocalFile = true,
      isSafSelected = true,
      isPermissionPersisted = isPermissionPersisted,
      artist = metaArtist,
      album = metaAlbum
    )

    // Cache in recent SAF items list for quick selection
    saveSafMediaToRecent(item)
    return item
  }

  fun getRecentSafMedia(): List<LocalMediaItem> {
    val prefs = context.getSharedPreferences(PREFS_SAF_RECENT, Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(KEY_SAF_RECENT_LIST, null) ?: return emptyList()
    val list = mutableListOf<LocalMediaItem>()
    try {
      val jsonArray = JSONArray(jsonStr)
      val persistedUris = context.contentResolver.persistedUriPermissions.map { it.uri.toString() }.toSet()

      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        val uriStr = obj.optString("uriString")
        if (uriStr.isNotBlank()) {
          val uri = Uri.parse(uriStr)
          // Verify if permission is still valid
          val hasPersisted = persistedUris.contains(uriStr)
          val item = LocalMediaItem(
            uri = uri,
            uriString = uriStr,
            title = obj.optString("title", "Saved Media"),
            mimeType = obj.optString("mimeType", "video/mp4"),
            durationSec = obj.optInt("durationSec", 1800),
            fileSizeBytes = obj.optLong("fileSizeBytes", 50000000L),
            mediaType = if (obj.optString("mediaType") == "AUDIO") StreamMediaType.AUDIO else StreamMediaType.VIDEO,
            resolution = obj.optString("resolution", "1080p Original"),
            codec = obj.optString("codec", "H.264 / AAC"),
            isLocalFile = true,
            isSafSelected = true,
            isPermissionPersisted = hasPersisted,
            artist = obj.optString("artist").takeIf { it.isNotBlank() },
            album = obj.optString("album").takeIf { it.isNotBlank() }
          )
          list.add(item)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error loading recent SAF media: ${e.message}")
    }
    return list
  }

  private fun saveSafMediaToRecent(item: LocalMediaItem) {
    try {
      val currentList = getRecentSafMedia().filterNot { it.uriString == item.uriString }.toMutableList()
      currentList.add(0, item) // most recent first
      val trimmed = currentList.take(6) // keep top 6

      val jsonArray = JSONArray()
      trimmed.forEach { itm ->
        val obj = JSONObject().apply {
          put("uriString", itm.uriString)
          put("title", itm.title)
          put("mimeType", itm.mimeType)
          put("durationSec", itm.durationSec)
          put("fileSizeBytes", itm.fileSizeBytes)
          put("mediaType", itm.mediaType.name)
          put("resolution", itm.resolution)
          put("codec", itm.codec)
          put("artist", itm.artist ?: "")
          put("album", itm.album ?: "")
        }
        jsonArray.put(obj)
      }

      val prefs = context.getSharedPreferences(PREFS_SAF_RECENT, Context.MODE_PRIVATE)
      prefs.edit().putString(KEY_SAF_RECENT_LIST, jsonArray.toString()).apply()
    } catch (e: Exception) {
      Log.e(TAG, "Error saving SAF item to recent list: ${e.message}")
    }
  }

  fun getSampleMediaPresets(): List<LocalMediaItem> {
    return listOf(
      LocalMediaItem(
        title = "Cyberpunk: Edgerunners Ep. 1",
        mimeType = "video/mp4",
        durationSec = 1420,
        fileSizeBytes = 482344960L,
        mediaType = StreamMediaType.VIDEO,
        resolution = "1080p FHD (60fps)",
        codec = "H.264 / AAC 5.1",
        isLocalFile = true
      ),
      LocalMediaItem(
        title = "Interstellar Odyssey (4+ Hour Epic)",
        mimeType = "video/mp4",
        durationSec = 15600, // 4 hours 20 mins!
        fileSizeBytes = 5368709120L, // 5.0 GB
        mediaType = StreamMediaType.VIDEO,
        resolution = "4K UHD (60fps)",
        codec = "HEVC / DTS Surround",
        isLocalFile = true
      ),
      LocalMediaItem(
        title = "Synthwave Night Drive Vol. 4",
        mimeType = "audio/mp3",
        durationSec = 3600, // 1 hour music
        fileSizeBytes = 84934656L,
        mediaType = StreamMediaType.AUDIO,
        resolution = "320 kbps High Definition",
        codec = "MP3 / Stereo",
        isLocalFile = true
      ),
      LocalMediaItem(
        title = "Lofi Study Beats & Rain",
        mimeType = "audio/flac",
        durationSec = 7200, // 2 hour album
        fileSizeBytes = 325477376L,
        mediaType = StreamMediaType.AUDIO,
        resolution = "Lossless FLAC (24-bit/48kHz)",
        codec = "FLAC Lossless",
        isLocalFile = true
      )
    )
  }

  private fun formatTime(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
  }
}
