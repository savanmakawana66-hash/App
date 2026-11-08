package com.example.network.api

import android.os.Build

/**
 * Centralized Backend Connectivity & Server Configuration.
 *
 * Supports:
 * - Production Cloud Run remote internet endpoint (HTTPS / WSS).
 * - Android Emulator loopback routing (10.0.2.2).
 * - Localhost / test runner routing (127.0.0.1).
 * - Scoped media upload & streaming URLs for remote Watch Party playback.
 */
object BackendServerConfig {

  const val DEFAULT_CLOUD_BASE_URL = "https://ais-dev-c555ii3jx626zxd5sixz54-555154314704.asia-southeast1.run.app"
  const val DEFAULT_CLOUD_WS_URL = "wss://ais-dev-c555ii3jx626zxd5sixz54-555154314704.asia-southeast1.run.app/ws/signaling"

  const val EMULATOR_BASE_URL = "http://10.0.2.2:8080"
  const val EMULATOR_WS_URL = "ws://10.0.2.2:8080/ws/signaling"

  const val LOCALHOST_BASE_URL = "http://127.0.0.1:8080"
  const val LOCALHOST_WS_URL = "ws://127.0.0.1:8080/ws/signaling"

  val isEmulator: Boolean by lazy {
    if (Build.FINGERPRINT.contains("robolectric", ignoreCase = true)) {
      false
    } else {
      Build.FINGERPRINT.startsWith("generic")
        || Build.FINGERPRINT.startsWith("unknown")
        || Build.MODEL.contains("google_sdk")
        || Build.MODEL.contains("Emulator")
        || Build.MODEL.contains("Android SDK built for x86")
        || Build.MANUFACTURER.contains("Genymotion")
        || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
        || "google_sdk" == Build.PRODUCT
        || Build.HARDWARE.contains("goldfish")
        || Build.HARDWARE.contains("ranchu")
    }
  }

  // Primary active base URL
  @Volatile
  var activeBaseUrl: String = DEFAULT_CLOUD_BASE_URL
    private set

  // Primary active WebSocket URL
  @Volatile
  var activeWsUrl: String = DEFAULT_CLOUD_WS_URL
    private set

  init {
    if (isEmulator) {
      activeBaseUrl = EMULATOR_BASE_URL
      activeWsUrl = EMULATOR_WS_URL
    }
  }

  fun setBaseUrl(url: String) {
    val clean = url.trimEnd('/')
    activeBaseUrl = clean
    activeWsUrl = if (clean.startsWith("https://", ignoreCase = true)) {
      clean.replaceFirst("https://", "wss://", ignoreCase = true) + "/ws/signaling"
    } else {
      clean.replaceFirst("http://", "ws://", ignoreCase = true) + "/ws/signaling"
    }
  }

  /**
   * Resets to default cloud URL
   */
  fun resetToCloud() {
    activeBaseUrl = DEFAULT_CLOUD_BASE_URL
    activeWsUrl = DEFAULT_CLOUD_WS_URL
  }

  /**
   * Sets to local emulator endpoint (10.0.2.2:8080)
   */
  fun useEmulatorEndpoint() {
    activeBaseUrl = EMULATOR_BASE_URL
    activeWsUrl = EMULATOR_WS_URL
  }

  /**
   * Sets to localhost endpoint (127.0.0.1:8080)
   */
  fun useLocalhostEndpoint() {
    activeBaseUrl = LOCALHOST_BASE_URL
    activeWsUrl = LOCALHOST_WS_URL
  }

  fun getHealthUrl(): String = "$activeBaseUrl/health"

  fun getActiveRoomsUrl(): String = "$activeBaseUrl/api/v1/rooms/active"

  fun getRoomDetailsUrl(roomCode: String): String = "$activeBaseUrl/api/v1/rooms/$roomCode"

  fun getMediaUploadUrl(roomCode: String): String = "$activeBaseUrl/api/v1/media/upload/$roomCode"

  fun getRemoteStreamUrl(roomCode: String, token: String = ""): String {
    val base = "$activeBaseUrl/stream/$roomCode"
    return if (token.isNotBlank()) "$base?token=$token&room=$roomCode" else base
  }
}
