package com.example.data.model

import android.net.Uri

enum class StreamQuality(val label: String, val description: String, val bitrateKbps: Int) {
  LOW("Low (480p)", "Data Saver • Lower bandwidth", 450),
  MEDIUM("Medium (720p)", "Balanced quality & speed", 1200),
  HIGH_ORIGINAL("High / Original", "Preserves full file bitrate", 4000)
}

enum class StreamMediaType(val label: String, val icon: String) {
  VIDEO("Video", "🎬"),
  AUDIO("Music / Audio", "🎵")
}

enum class PartyConnectionState(val label: String, val emoji: String) {
  CONNECTED("Connected", "🟢"),
  RECONNECTING("Reconnecting…", "🟡"),
  WAITING_FOR_HOST("Waiting for Host…", "🟠"),
  SYNCING("Connection restored — syncing video…", "🔵"),
  DISCONNECTED("Disconnected", "🔴")
}

data class LocalMediaItem(
  val uri: Uri? = null,
  val uriString: String = "",
  val title: String,
  val mimeType: String = "video/mp4",
  val durationSec: Int = 1800,
  val fileSizeBytes: Long = 104857600L, // 100 MB default
  val mediaType: StreamMediaType = StreamMediaType.VIDEO,
  val resolution: String = "1080p (60fps)",
  val codec: String = "H.264 / AAC",
  val isLocalFile: Boolean = true,
  val isSafSelected: Boolean = false,
  val isPermissionPersisted: Boolean = false,
  val artist: String? = null,
  val album: String? = null
) {
  val formattedSize: String
    get() {
      val mb = fileSizeBytes.toDouble() / (1024 * 1024)
      return if (mb >= 1024) {
        String.format("%.2f GB", mb / 1024)
      } else {
        String.format("%.1f MB", mb)
      }
    }

  val formattedDuration: String
    get() {
      val h = durationSec / 3600
      val m = (durationSec % 3600) / 60
      val s = durationSec % 60
      return if (h > 0) {
        String.format("%dh %02dm %02ds", h, m, s)
      } else {
        String.format("%02dm %02ds", m, s)
      }
    }
}

data class WatchPartyParticipant(
  val id: String,
  val username: String,
  val avatar: String = "avatar_1",
  val isHost: Boolean = false,
  val isSpeaking: Boolean = false,
  val isMuted: Boolean = false,
  val inVoice: Boolean = true,
  val connectionState: PartyConnectionState = PartyConnectionState.CONNECTED,
  val pingMs: Int = 18,
  val dataUsageBytes: Long = 0L
) {
  val formattedDataUsage: String
    get() {
      val mb = dataUsageBytes.toDouble() / (1024 * 1024)
      return String.format("%.1f MB", mb)
    }
}

data class WatchPartySyncPacket(
  val roomCode: String,
  val hostId: String,
  val isPlaying: Boolean,
  val positionSec: Float,
  val timestampMs: Long = System.currentTimeMillis(),
  val mediaTitle: String,
  val durationSec: Int,
  val mediaType: StreamMediaType,
  val quality: StreamQuality = StreamQuality.HIGH_ORIGINAL,
  val streamUrl: String = ""
)

data class WatchPartyRoom(
  val code: String, // e.g. "WP-8392"
  val name: String,
  val hostId: String,
  val hostName: String,
  val isPrivate: Boolean = true,
  val password: String = "",
  val activeMedia: LocalMediaItem,
  val currentQuality: StreamQuality = StreamQuality.HIGH_ORIGINAL,
  val isPlaying: Boolean = true,
  val positionSec: Float = 0f,
  val durationSec: Int = 1800,
  val connectionState: PartyConnectionState = PartyConnectionState.CONNECTED,
  val participants: List<WatchPartyParticipant> = emptyList(),
  val streamBitrateKbps: Float = 1450f,
  val totalStreamedBytes: Long = 0L,
  val isHost: Boolean = false,
  val streamUrl: String = "",
  val errorBanner: String? = null
)
