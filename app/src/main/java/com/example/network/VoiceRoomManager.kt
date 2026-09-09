package com.example.network

import android.content.Context
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
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

  private fun startAudioCapture() {
    isRecording.set(true)
    captureJob = scope.launch(Dispatchers.IO) {
      val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
      val bufferSize = maxOf(minBufSize, 2048)
      val buffer = ShortArray(bufferSize / 2)

      try {
        val record = AudioRecord(
          MediaRecorder.AudioSource.VOICE_COMMUNICATION,
          sampleRate,
          channelConfigIn,
          audioFormat,
          bufferSize
        )
        audioRecord = record

        // Apply hardware acoustic echo cancellation and noise suppression if supported
        if (AcousticEchoCanceler.isAvailable()) {
          try {
            AcousticEchoCanceler.create(record.audioSessionId)?.enabled = true
          } catch (_: Exception) {}
        }
        if (NoiseSuppressor.isAvailable()) {
          try {
            NoiseSuppressor.create(record.audioSessionId)?.enabled = true
          } catch (_: Exception) {}
        }

        record.startRecording()

        var consecutiveSpeakingFrames = 0

        while (isRecording.get() && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
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
        // Normal cancellation when user leaves voice room or stops audio capture
        Log.d(TAG, "Audio capture coroutine cancelled cleanly")
      } catch (e: SecurityException) {
        Log.w(TAG, "AudioRecord permission not granted: ${e.message}")
      } catch (e: Throwable) {
        if (e is CancellationException) {
          Log.d(TAG, "Audio capture coroutine cancelled cleanly")
        } else if (isRecording.get()) {
          Log.w(TAG, "Audio capture warning: ${e.message}")
        }
      } finally {
        try {
          if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord?.stop()
          }
          audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
      }
    }
  }

  private suspend fun simulateVoiceActivity() {
    try {
      while (isRecording.get() && currentCoroutineContext().isActive) {
        if (!_isMuted.value) {
          _isLocalSpeaking.value = false
          _audioAmplitude.value = 0f
        }
        delay(500)
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
      audioRecord?.release()
    } catch (_: Exception) {}
    audioRecord = null
  }

  fun release() {
    leaveVoice()
    scope.cancel()
  }
}
