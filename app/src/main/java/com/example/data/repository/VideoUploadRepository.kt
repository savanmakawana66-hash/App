package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.data.model.LocalMediaItem
import com.example.network.api.BackendServerConfig
import com.example.network.security.BackendSecurityController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Interface to track video upload progress with detailed metrics for the Watch Party feature.
 */
interface UploadProgressListener {
  /**
   * Called periodically as data chunks are written to the network.
   *
   * @param bytesUploaded Number of bytes sent so far.
   * @param totalBytes Total file size in bytes.
   * @param percentage Value between 0.0f and 1.0f.
   * @param bytesPerSecond Instantaneous transfer speed in bytes per second.
   * @param etaSeconds Estimated remaining time in seconds.
   */
  fun onProgress(
    bytesUploaded: Long,
    totalBytes: Long,
    percentage: Float,
    bytesPerSecond: Long,
    etaSeconds: Long
  )

  /**
   * Called when the upload lifecycle state transitions.
   */
  fun onStateChanged(state: VideoUploadState)

  /**
   * Called when a single memory-stable chunk has been written.
   */
  fun onChunkUploaded(chunkIndex: Int, totalChunks: Int, chunkBytes: Long) {}
}

/**
 * High-level state representing the video upload lifecycle.
 */
sealed class VideoUploadState {
  object Idle : VideoUploadState()

  data class Preparing(
    val fileName: String,
    val totalBytes: Long
  ) : VideoUploadState()

  data class Uploading(
    val fileName: String,
    val bytesUploaded: Long,
    val totalBytes: Long,
    val progress: Float,
    val speedBps: Long,
    val etaSeconds: Long,
    val currentChunk: Int,
    val totalChunks: Int
  ) : VideoUploadState()

  data class Paused(
    val fileName: String,
    val bytesUploaded: Long,
    val totalBytes: Long
  ) : VideoUploadState()

  data class Completed(
    val roomCode: String,
    val streamUrl: String,
    val totalBytes: Long,
    val durationMs: Long
  ) : VideoUploadState()

  data class Error(
    val message: String,
    val canRetry: Boolean,
    val cause: Throwable? = null
  ) : VideoUploadState()

  object Cancelled : VideoUploadState()
}

/**
 * Result data returned upon upload completion or failure.
 */
data class VideoUploadResult(
  val success: Boolean,
  val roomCode: String,
  val streamUrl: String = "",
  val totalBytesUploaded: Long = 0L,
  val durationMs: Long = 0L,
  val errorMessage: String? = null
)

/**
 * Upload transmission mode: Multipart form data or raw chunked byte stream.
 */
enum class UploadMode {
  MULTIPART,
  CHUNKED_STREAMING
}

/**
 * Configuration options for memory-stable video uploads.
 */
data class VideoUploadConfig(
  val chunkSizeBytes: Int = 64 * 1024, // 64 KB chunk size keeps heap allocation < 5 MB
  val connectTimeoutSeconds: Long = 30L,
  val writeTimeoutSeconds: Long = 300L,
  val readTimeoutSeconds: Long = 60L,
  val uploadMode: UploadMode = UploadMode.MULTIPART,
  val maxRetries: Int = 3
)

/**
 * Contract for Video Upload Repository.
 */
interface IVideoUploadRepository {
  val uploadState: StateFlow<VideoUploadState>
  val uploadProgress: StateFlow<Float>
  val uploadSpeedBps: StateFlow<Long>
  val etaSeconds: StateFlow<Long>

  suspend fun uploadVideo(
    roomCode: String,
    media: LocalMediaItem,
    hostUserId: String,
    listener: UploadProgressListener? = null
  ): Result<VideoUploadResult>

  suspend fun uploadVideoMultipart(
    roomCode: String,
    videoUri: Uri,
    title: String,
    fileSizeBytes: Long,
    mimeType: String,
    hostUserId: String,
    listener: UploadProgressListener? = null
  ): Result<VideoUploadResult>

  fun cancelUpload()
  fun pauseUpload()
  fun resumeUpload()
}

/**
 * Memory-stable Video Upload Repository for the Watch Party feature.
 *
 * Key guarantees:
 * - Reads data in 64 KB chunks from ContentResolver or FileInputStream directly into OkHttp's sink.
 * - Never loads 100MB+ or multi-gigabyte video files into JVM heap memory.
 * - Supports both Multipart/form-data and direct Chunked streaming modes.
 * - Provides real-time upload progress, speed calculation, and remaining time estimation.
 * - Supports clean pause, resume, and cancellation.
 */
class VideoUploadRepository(
  private val context: Context,
  private val config: VideoUploadConfig = VideoUploadConfig()
) : IVideoUploadRepository {

  companion object {
    private const val TAG = "VideoUploadRepository"
    const val DEFAULT_CHUNK_SIZE = 64 * 1024 // 64 KB
  }

  private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  private val _uploadState = MutableStateFlow<VideoUploadState>(VideoUploadState.Idle)
  override val uploadState: StateFlow<VideoUploadState> = _uploadState.asStateFlow()

  private val _uploadProgress = MutableStateFlow(0f)
  override val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

  private val _uploadSpeedBps = MutableStateFlow(0L)
  override val uploadSpeedBps: StateFlow<Long> = _uploadSpeedBps.asStateFlow()

  private val _etaSeconds = MutableStateFlow(0L)
  override val etaSeconds: StateFlow<Long> = _etaSeconds.asStateFlow()

  private val isUploading = AtomicBoolean(false)
  private val isPaused = AtomicBoolean(false)
  private var activeCall: Call? = null
  private var currentParams: UploadParams? = null

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.RESTRICTED_TLS, ConnectionSpec.CLEARTEXT))
      .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
      .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)
      .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()
  }

  private data class UploadParams(
    val roomCode: String,
    val media: LocalMediaItem,
    val hostUserId: String,
    val listener: UploadProgressListener?
  )

  /**
   * Uploads a LocalMediaItem for the Watch Party room.
   */
  override suspend fun uploadVideo(
    roomCode: String,
    media: LocalMediaItem,
    hostUserId: String,
    listener: UploadProgressListener?
  ): Result<VideoUploadResult> = withContext(Dispatchers.IO) {
    currentParams = UploadParams(roomCode, media, hostUserId, listener)

    // If media is already a remote HTTP/HTTPS URL, skip upload
    val uriStr = media.uriString.trim()
    if (uriStr.startsWith("http://", ignoreCase = true) || uriStr.startsWith("https://", ignoreCase = true)) {
      val completed = VideoUploadState.Completed(
        roomCode = roomCode,
        streamUrl = uriStr,
        totalBytes = media.fileSizeBytes,
        durationMs = 0L
      )
      _uploadState.value = completed
      _uploadProgress.value = 1f
      listener?.onStateChanged(completed)
      return@withContext Result.success(
        VideoUploadResult(
          success = true,
          roomCode = roomCode,
          streamUrl = uriStr,
          totalBytesUploaded = media.fileSizeBytes,
          durationMs = 0L
        )
      )
    }

    val targetUri = media.uri ?: Uri.parse(media.uriString)
    executeChunkedUpload(
      roomCode = roomCode,
      videoUri = targetUri,
      title = media.title.ifBlank { "WatchPartyVideo.mp4" },
      fileSizeBytes = media.fileSizeBytes,
      mimeType = media.mimeType.ifBlank { "video/mp4" },
      durationSec = media.durationSec,
      hostUserId = hostUserId,
      listener = listener
    )
  }

  /**
   * Uploads a video from a Content URI or File URI with multipart headers.
   */
  override suspend fun uploadVideoMultipart(
    roomCode: String,
    videoUri: Uri,
    title: String,
    fileSizeBytes: Long,
    mimeType: String,
    hostUserId: String,
    listener: UploadProgressListener?
  ): Result<VideoUploadResult> = withContext(Dispatchers.IO) {
    val media = LocalMediaItem(
      uri = videoUri,
      uriString = videoUri.toString(),
      title = title,
      fileSizeBytes = fileSizeBytes,
      mimeType = mimeType,
      isSafSelected = true
    )
    currentParams = UploadParams(roomCode, media, hostUserId, listener)

    executeChunkedUpload(
      roomCode = roomCode,
      videoUri = videoUri,
      title = title,
      fileSizeBytes = fileSizeBytes,
      mimeType = mimeType,
      durationSec = 0,
      hostUserId = hostUserId,
      listener = listener
    )
  }

  /**
   * Internal streaming engine that executes chunked transmission without loading files into RAM.
   */
  private suspend fun executeChunkedUpload(
    roomCode: String,
    videoUri: Uri,
    title: String,
    fileSizeBytes: Long,
    mimeType: String,
    durationSec: Int,
    hostUserId: String,
    listener: UploadProgressListener?
  ): Result<VideoUploadResult> {
    cancelUpload() // Cancel previous upload if still active

    isUploading.set(true)
    isPaused.set(false)

    val actualFileSize = if (fileSizeBytes > 0) fileSizeBytes else resolveFileSize(videoUri)
    val totalLength = actualFileSize.coerceAtLeast(1L)
    val totalChunks = ((totalLength + config.chunkSizeBytes - 1) / config.chunkSizeBytes).toInt().coerceAtLeast(1)

    val preparingState = VideoUploadState.Preparing(title, totalLength)
    _uploadState.value = preparingState
    _uploadProgress.value = 0f
    _uploadSpeedBps.value = 0L
    _etaSeconds.value = 0L
    listener?.onStateChanged(preparingState)

    val startTimeMs = System.currentTimeMillis()
    val token = BackendSecurityController.generateSignedMediaToken(hostUserId, roomCode)
    val expectedStreamUrl = BackendServerConfig.getRemoteStreamUrl(roomCode, token)
    val uploadEndpoint = BackendServerConfig.getMediaUploadUrl(roomCode)

    Log.i(TAG, "Starting chunked upload to $uploadEndpoint. Mode=${config.uploadMode}, TotalBytes=$totalLength, Chunks=$totalChunks")

    try {
      val mediaType = mimeType.toMediaTypeOrNull() ?: "video/mp4".toMediaTypeOrNull()

      val chunkedBody = ChunkedStreamingRequestBody(
        contentType = mediaType,
        contentLength = totalLength,
        chunkSize = config.chunkSizeBytes,
        openStream = { openInputStreamForUri(videoUri) },
        onChunkWritten = { bytesWritten, chunkIdx, chunkBytes, speedBps, etaSec ->
          val progress = (bytesWritten.toFloat() / totalLength.toFloat()).coerceIn(0f, 1f)
          _uploadProgress.value = progress
          _uploadSpeedBps.value = speedBps
          _etaSeconds.value = etaSec

          val uploadingState = VideoUploadState.Uploading(
            fileName = title,
            bytesUploaded = bytesWritten,
            totalBytes = totalLength,
            progress = progress,
            speedBps = speedBps,
            etaSeconds = etaSec,
            currentChunk = chunkIdx,
            totalChunks = totalChunks
          )
          _uploadState.value = uploadingState

          listener?.onProgress(bytesWritten, totalLength, progress, speedBps, etaSec)
          listener?.onChunkUploaded(chunkIdx, totalChunks, chunkBytes)
        },
        isCancelled = { !isUploading.get() },
        isPaused = { isPaused.get() }
      )

      val request: Request = if (config.uploadMode == UploadMode.MULTIPART) {
        val multipartBuilder = MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("roomCode", roomCode)
          .addFormDataPart("title", title)
          .addFormDataPart("duration", durationSec.toString())
          .addFormDataPart("fileSize", totalLength.toString())
          .addFormDataPart(
            "file",
            title,
            chunkedBody
          )
        Request.Builder()
          .url(uploadEndpoint)
          .post(multipartBuilder.build())
          .header("X-Media-Room", roomCode)
          .header("X-Media-Token", token)
          .build()
      } else {
        Request.Builder()
          .url(uploadEndpoint)
          .post(chunkedBody)
          .header("X-Media-Title", title)
          .header("X-Media-Duration", durationSec.toString())
          .header("X-Media-Room", roomCode)
          .header("X-Media-Token", token)
          .build()
      }

      val call = okHttpClient.newCall(request)
      activeCall = call

      val response = withContext(Dispatchers.IO) { call.execute() }
      response.use { resp ->
        if (resp.isSuccessful) {
          val durationMs = System.currentTimeMillis() - startTimeMs
          val completedState = VideoUploadState.Completed(
            roomCode = roomCode,
            streamUrl = expectedStreamUrl,
            totalBytes = totalLength,
            durationMs = durationMs
          )
          _uploadState.value = completedState
          _uploadProgress.value = 1f
          _uploadSpeedBps.value = 0L
          _etaSeconds.value = 0L
          listener?.onStateChanged(completedState)

          Log.i(TAG, "Chunked upload completed successfully in ${durationMs}ms for room $roomCode")
          return Result.success(
            VideoUploadResult(
              success = true,
              roomCode = roomCode,
              streamUrl = expectedStreamUrl,
              totalBytesUploaded = totalLength,
              durationMs = durationMs
            )
          )
        } else {
          val errorMsg = "Upload failed with HTTP ${resp.code}: ${resp.message}"
          val errorState = VideoUploadState.Error(errorMsg, canRetry = true)
          _uploadState.value = errorState
          listener?.onStateChanged(errorState)
          Log.e(TAG, errorMsg)
          return Result.failure(IllegalStateException(errorMsg))
        }
      }
    } catch (e: Exception) {
      if (!isUploading.get()) {
        val cancelledState = VideoUploadState.Cancelled
        _uploadState.value = cancelledState
        listener?.onStateChanged(cancelledState)
        return Result.failure(CancellationException("Upload was cancelled"))
      }

      val errorMsg = e.message ?: "Unknown upload failure"
      val errorState = VideoUploadState.Error(errorMsg, canRetry = true, cause = e)
      _uploadState.value = errorState
      listener?.onStateChanged(errorState)
      Log.e(TAG, "Video upload failed: $errorMsg", e)
      return Result.failure(e)
    } finally {
      isUploading.set(false)
      activeCall = null
    }
  }

  override fun pauseUpload() {
    if (isUploading.get()) {
      isPaused.set(true)
      val current = _uploadState.value
      if (current is VideoUploadState.Uploading) {
        val paused = VideoUploadState.Paused(current.fileName, current.bytesUploaded, current.totalBytes)
        _uploadState.value = paused
        currentParams?.listener?.onStateChanged(paused)
      }
    }
  }

  override fun resumeUpload() {
    if (isUploading.get() && isPaused.get()) {
      isPaused.set(false)
    } else if (!isUploading.get() && currentParams != null) {
      repositoryScope.launch {
        currentParams?.let { params ->
          uploadVideo(params.roomCode, params.media, params.hostUserId, params.listener)
        }
      }
    }
  }

  override fun cancelUpload() {
    isUploading.set(false)
    isPaused.set(false)
    try {
      activeCall?.cancel()
    } catch (_: Exception) {}
    activeCall = null
    _uploadState.value = VideoUploadState.Cancelled
    _uploadProgress.value = 0f
    _uploadSpeedBps.value = 0L
    _etaSeconds.value = 0L
  }

  private fun resolveFileSize(uri: Uri): Long {
    var size = 0L
    try {
      if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
          if (sizeIndex != -1 && cursor.moveToFirst()) {
            size = cursor.getLong(sizeIndex)
          }
        }
      } else if (uri.scheme == "file" || uri.scheme == null) {
        uri.path?.let { p ->
          val f = File(p)
          if (f.exists()) size = f.length()
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Unable to resolve file size for $uri: ${e.message}")
    }
    return size
  }

  private fun openInputStreamForUri(uri: Uri): InputStream? {
    // 1. Direct file path check
    if (uri.scheme == "file" || (uri.scheme == null && !uri.path.isNullOrBlank())) {
      val f = File(uri.path ?: "")
      if (f.exists()) return FileInputStream(f)
    }

    // 2. Open via ParcelFileDescriptor for SAF content URIs
    try {
      val pfd = context.contentResolver.openFileDescriptor(uri, "r")
      if (pfd != null) {
        return ParcelFileDescriptor.AutoCloseInputStream(pfd)
      }
    } catch (e: Exception) {
      Log.w(TAG, "openFileDescriptor failed for $uri: ${e.message}")
    }

    // 3. Fallback to openInputStream
    return try {
      context.contentResolver.openInputStream(uri)
    } catch (e: Exception) {
      Log.w(TAG, "openInputStream failed for $uri: ${e.message}")
      null
    }
  }
}

/**
 * Custom OkHttp RequestBody that streams chunks directly to the sink.
 * Memory stability: Only keeps one 64 KB chunk in RAM at a time.
 */
class ChunkedStreamingRequestBody(
  private val contentType: MediaType?,
  private val contentLength: Long,
  private val chunkSize: Int = 64 * 1024,
  private val openStream: () -> InputStream?,
  private val onChunkWritten: (bytesWritten: Long, chunkIdx: Int, chunkBytes: Long, speedBps: Long, etaSec: Long) -> Unit,
  private val isCancelled: () -> Boolean,
  private val isPaused: () -> Boolean
) : RequestBody() {

  override fun contentType(): MediaType? = contentType

  override fun contentLength(): Long = contentLength

  override fun writeTo(sink: BufferedSink) {
    val inputStream = openStream() ?: throw IllegalStateException("Cannot open input stream for video upload")
    val buffer = ByteArray(chunkSize)
    var totalBytesWritten = 0L
    var chunkIndex = 0
    var lastUpdateTimestamp = System.currentTimeMillis()
    var bytesSinceLastSpeedSample = 0L
    var rollingSpeedBps = 0L

    inputStream.use { stream ->
      var readBytes: Int
      while (stream.read(buffer).also { readBytes = it } != -1) {
        if (isCancelled()) {
          throw CancellationException("Upload cancelled by user")
        }

        // Pause loop with light sleep
        while (isPaused() && !isCancelled()) {
          Thread.sleep(150L)
        }

        sink.write(buffer, 0, readBytes)
        totalBytesWritten += readBytes
        bytesSinceLastSpeedSample += readBytes
        chunkIndex++

        val now = System.currentTimeMillis()
        val elapsedSampleMs = now - lastUpdateTimestamp

        // Calculate transfer speed and ETA every 250ms or at completion
        if (elapsedSampleMs >= 250L || totalBytesWritten >= contentLength) {
          val speedBps = if (elapsedSampleMs > 0) {
            (bytesSinceLastSpeedSample * 1000L) / elapsedSampleMs
          } else rollingSpeedBps
          rollingSpeedBps = if (rollingSpeedBps == 0L) speedBps else (rollingSpeedBps * 7 + speedBps * 3) / 10

          val remainingBytes = (contentLength - totalBytesWritten).coerceAtLeast(0L)
          val etaSec = if (rollingSpeedBps > 0) remainingBytes / rollingSpeedBps else 0L

          onChunkWritten(totalBytesWritten, chunkIndex, readBytes.toLong(), rollingSpeedBps, etaSec)
          lastUpdateTimestamp = now
          bytesSinceLastSpeedSample = 0L
        }
      }
      sink.flush()
    }
  }
}
