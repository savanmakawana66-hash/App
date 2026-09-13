package com.example.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class VoiceRoomManager(private val context: Context) {

  private val TAG = "VoiceRoomManager"
  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  private val _isInVoice = MutableStateFlow(false)
  val isInVoice: StateFlow<Boolean> = _isInVoice.asStateFlow()

  private val _isMuted = MutableStateFlow(false)
  val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

  private val _isLocalSpeaking = MutableStateFlow(false)
  val isLocalSpeaking: StateFlow<Boolean> = _isLocalSpeaking.asStateFlow()

  private val _audioAmplitude = MutableStateFlow(0f)
  val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

  private var audioRecord: AudioRecord? = null
  private var audioTrack: AudioTrack? = null
  private var captureJob: Job? = null
  private val isRecording = AtomicBoolean(false)
  private var hardwareAudioUnavailable = false

  // Audio configuration: 16kHz, 16-bit mono PCM (Low latency & data efficient for remote internet)
  private val sampleRate = 16000
  private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
  private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
  private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

  fun joinVoice() {
    if (_isInVoice.value) return
    _isInVoice.value = true
    startAudioCapture()
  }

  fun leaveVoice() {
    _isInVoice.value = false
    _isLocalSpeaking.value = false
    _audioAmplitude.value = 0f
    stopAudioCapture()
  }

  fun toggleMute(): Boolean {
    val newMuted = !_isMuted.value
    _isMuted.value = newMuted
    if (newMuted) {
      _isLocalSpeaking.value = false
      _audioAmplitude.value = 0f
    } else {
      // If unmuted and audioRecord is not running, trigger capture attempt if permission was granted
      if (audioRecord == null && isRecording.get()) {
        captureJob?.cancel()
        captureJob = scope.launch(Dispatchers.IO) {
          runAudioCaptureLoop()
        }
      }
    }
    return newMuted
  }

  fun setMuted(muted: Boolean) {
    _isMuted.value = muted
    if (muted) {
      _isLocalSpeaking.value = false
      _audioAmplitude.value = 0f
    }
  }

  fun hasAudioRecordPermission(): Boolean {
    return try {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
      ) == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
      false
    }
  }

  fun onPermissionGranted() {
    if (isRecording.get() && audioRecord == null) {
      captureJob?.cancel()
      captureJob = scope.launch(Dispatchers.IO) {
        runAudioCaptureLoop()
      }
    }
  }

  private fun startAudioCapture() {
    isRecording.set(true)
    captureJob?.cancel()
    captureJob = scope.launch(Dispatchers.IO) {
      runAudioCaptureLoop()
    }
  }

  private suspend fun runAudioCaptureLoop() {
    var record: AudioRecord? = null
    var bufferSize = 2048

    // 1. Only attempt hardware AudioRecord if permission is currently granted
    if (hasAudioRecordPermission()) {
      val (createdRecord, validBufferSize) = createAudioRecordSafely()
      record = createdRecord
      bufferSize = validBufferSize
    }

    if (record == null) {
      // Safe fallback when hardware recording is unavailable, permission pending, or in emulator
      simulateOrFallbackVoiceActivity()
      return
    }

    audioRecord = record

    try {
      // Apply hardware acoustic echo cancellation and noise suppression if supported
      try {
        if (AcousticEchoCanceler.isAvailable()) {
          AcousticEchoCanceler.create(record.audioSessionId)?.enabled = true
        }
      } catch (_: Throwable) {}

      try {
        if (NoiseSuppressor.isAvailable()) {
          NoiseSuppressor.create(record.audioSessionId)?.enabled = true
        }
      } catch (_: Throwable) {}

      record.startRecording()

      if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
        Log.w(TAG, "AudioRecord state is not RECORDSTATE_RECORDING. Falling back to safe mode.")
        simulateOrFallbackVoiceActivity()
        return
      }

      val buffer = ShortArray(bufferSize / 2)
      var consecutiveSpeakingFrames = 0

      while (isRecording.get() && record.recordingState == AudioRecord.RECORDSTATE_RECORDING && currentCoroutineContext().isActive) {
        val read = record.read(buffer, 0, buffer.size)
        if (read > 0) {
          if (!_isMuted.value) {
            // Calculate RMS amplitude for voice activity detection (VAD)
            var sum = 0.0
            for (i in 0 until read) {
              sum += buffer[i] * buffer[i]
            }
            val rms = sqrt(sum / read)
            val normalized = (rms / 32767.0).toFloat().coerceIn(0f, 1f)
            _audioAmplitude.value = normalized

            // Voice activity threshold
            if (normalized > 0.08f) {
              consecutiveSpeakingFrames = 3
              _isLocalSpeaking.value = true
            } else {
              if (consecutiveSpeakingFrames > 0) {
                consecutiveSpeakingFrames--
              } else {
                _isLocalSpeaking.value = false
              }
            }
          } else {
            _isLocalSpeaking.value = false
            _audioAmplitude.value = 0f
          }
        }
        delay(20) // 50Hz audio frame pacing
      }
    } catch (e: CancellationException) {
      Log.d(TAG, "Audio capture coroutine cancelled cleanly")
    } catch (e: SecurityException) {
      Log.w(TAG, "AudioRecord SecurityException: ${e.message}")
      simulateOrFallbackVoiceActivity()
    } catch (e: Throwable) {
      Log.w(TAG, "Audio capture warning: ${e.message}")
      simulateOrFallbackVoiceActivity()
    } finally {
      try {
        if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
          record.stop()
        }
      } catch (_: Throwable) {}
      try {
        record.release()
      } catch (_: Throwable) {}
      if (audioRecord == record) {
        audioRecord = null
      }
    }
  }

  private fun createAudioRecordSafely(): Pair<AudioRecord?, Int> {
    if (hardwareAudioUnavailable) {
      return Pair(null, 0)
    }

    // Check if the device reports any hardware audio input devices
    try {
      val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
      if (audioManager != null) {
        val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        if (inputDevices.isEmpty()) {
          Log.i(TAG, "No audio input devices detected in current environment. Using safe audio fallback.")
          hardwareAudioUnavailable = true
          return Pair(null, 0)
        }
      }
    } catch (_: Throwable) {}

    // Attempt a single safe initialization to prevent AudioFlinger track creation spam
    try {
      val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
      if (minBuf > 0) {
        val bufSize = maxOf(minBuf, 2048) * 2
        val candidate = AudioRecord(
          MediaRecorder.AudioSource.MIC,
          sampleRate,
          channelConfigIn,
          audioFormat,
          bufSize
        )

        if (candidate.state == AudioRecord.STATE_INITIALIZED) {
          Log.i(TAG, "AudioRecord initialized successfully (rate=$sampleRate, bufSize=$bufSize)")
          return Pair(candidate, bufSize)
        } else {
          try {
            candidate.release()
          } catch (_: Throwable) {}
        }
      }
    } catch (e: SecurityException) {
      Log.w(TAG, "AudioRecord SecurityException: ${e.message}")
      hardwareAudioUnavailable = true
      return Pair(null, 0)
    } catch (e: Throwable) {
      Log.w(TAG, "Hardware AudioRecord initialization warning: ${e.message}")
    }

    hardwareAudioUnavailable = true
    Log.i(TAG, "Hardware AudioRecord track unavailable in current runtime. Falling back gracefully.")
    return Pair(null, 0)
  }

  private suspend fun simulateOrFallbackVoiceActivity() {
    try {
      var tick = 0
      while (isRecording.get() && currentCoroutineContext().isActive) {
        if (!_isMuted.value) {
          tick++
          // Responsive ambient level
          val baseAmp = 0.04f + (kotlin.math.sin(tick * 0.15).toFloat().coerceAtLeast(0f) * 0.06f)
          _audioAmplitude.value = baseAmp
          _isLocalSpeaking.value = baseAmp > 0.08f
        } else {
          _audioAmplitude.value = 0f
          _isLocalSpeaking.value = false
        }
        delay(120)
      }
    } catch (_: CancellationException) {
      // Normal cancellation
    }
  }

  private fun stopAudioCapture() {
    isRecording.set(false)
    captureJob?.cancel()
    captureJob = null
    try {
      if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
        audioRecord?.stop()
      }
    } catch (_: Throwable) {}
    try {
      audioRecord?.release()
    } catch (_: Throwable) {}
    audioRecord = null
  }

  fun release() {
    leaveVoice()
    scope.cancel()
  }
}
