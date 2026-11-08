package com.example.network

import android.content.Context
import android.util.Log
import com.example.data.model.LocalMediaItem
import com.example.data.repository.IVideoUploadRepository
import com.example.data.repository.VideoUploadRepository
import com.example.data.repository.VideoUploadState
import com.example.network.api.BackendServerConfig
import com.example.network.security.BackendSecurityController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Publishes host media to the remote streaming relay server.
 * Delegates chunked multipart upload and progress tracking to [VideoUploadRepository]
 * without loading large video files into device RAM.
 */
class MediaStreamPublisher(
  private val context: Context,
  val uploadRepository: IVideoUploadRepository = VideoUploadRepository(context)
) {
  private val TAG = "MediaStreamPublisher"
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  val uploadProgress: StateFlow<Float> = uploadRepository.uploadProgress
  val uploadState: StateFlow<VideoUploadState> = uploadRepository.uploadState

  private val _isPublished = MutableStateFlow(false)
  val isPublished: StateFlow<Boolean> = _isPublished.asStateFlow()

  /**
   * Asynchronously publishes the media file to the remote backend relay server.
   * Returns the authorized remote streaming URL immediately so participants can connect.
   */
  fun publishMediaForRemoteParticipants(
    roomCode: String,
    media: LocalMediaItem,
    hostUserId: String
  ): String {
    val token = BackendSecurityController.generateSignedMediaToken(hostUserId, roomCode)
    val remoteStreamUrl = BackendServerConfig.getRemoteStreamUrl(roomCode, token)

    // If media is already an external internet URL, no need to upload
    if (media.uriString.startsWith("http://", ignoreCase = true) || media.uriString.startsWith("https://", ignoreCase = true)) {
      _isPublished.value = true
      return media.uriString
    }

    // Cancel any previous in-flight publication
    cancelPublication()
    _isPublished.value = false

    scope.launch {
      try {
        Log.i(TAG, "Initiating chunked publication for room $roomCode (File: ${media.title}, size: ${media.fileSizeBytes} bytes)")
        val result = uploadRepository.uploadVideo(roomCode, media, hostUserId)
        if (result.isSuccess) {
          _isPublished.value = true
          Log.i(TAG, "Media successfully published to remote server for room $roomCode")
        } else {
          Log.w(TAG, "Media upload did not succeed: ${result.exceptionOrNull()?.message}")
        }
      } catch (e: Exception) {
        Log.w(TAG, "Publication coroutine exception: ${e.message}")
      }
    }

    return remoteStreamUrl
  }

  fun cancelPublication() {
    uploadRepository.cancelUpload()
    _isPublished.value = false
  }
}
