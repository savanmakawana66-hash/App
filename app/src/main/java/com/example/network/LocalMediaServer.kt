package com.example.network

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.data.model.LocalMediaItem
import com.example.data.model.StreamQuality
import kotlinx.coroutines.*
import java.io.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class LocalMediaServer(private val context: Context) {

  private val TAG = "LocalMediaServer"
  private var serverSocket: ServerSocket? = null
  private var serverJob: Job? = null
  private val isRunning = AtomicBoolean(false)
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  var currentMedia: LocalMediaItem? = null
  var currentQuality: StreamQuality = StreamQuality.HIGH_ORIGINAL

  val totalBytesTransferred = AtomicLong(0L)
  var currentBitrateKbps: Float = 0f
  private var lastBitrateCalcTime = System.currentTimeMillis()
  private var bytesSinceLastCalc = AtomicLong(0L)

  var serverPort: Int = 0
    private set

  fun start(media: LocalMediaItem): String {
    currentMedia = media
    if (isRunning.get()) {
      return getStreamUrl()
    }

    try {
      val socket = ServerSocket(0)
      serverSocket = socket
      serverPort = socket.localPort
      isRunning.set(true)

      serverJob = scope.launch {
        while (isRunning.get() && !socket.isClosed) {
          try {
            val clientSocket = socket.accept()
            launch(Dispatchers.IO) {
              handleClientConnection(clientSocket)
            }
          } catch (e: Exception) {
            if (isRunning.get()) {
              Log.e(TAG, "Server socket accept error: ${e.message}")
            }
          }
        }
      }

      // Bitrate tracking ticker
      scope.launch {
        while (isRunning.get()) {
          delay(1000)
          val now = System.currentTimeMillis()
          val elapsedSec = (now - lastBitrateCalcTime) / 1000f
          if (elapsedSec >= 1f) {
            val bytes = bytesSinceLastCalc.getAndSet(0L)
            currentBitrateKbps = (bytes * 8f) / (elapsedSec * 1024f)
            lastBitrateCalcTime = now
          }
        }
      }

      Log.i(TAG, "LocalMediaServer started on port $serverPort")
      return getStreamUrl()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start LocalMediaServer: ${e.message}", e)
      return ""
    }
  }

  fun getStreamUrl(): String {
    return "http://127.0.0.1:$serverPort/stream"
  }

  fun getLanStreamUrl(): String {
    val ip = getLocalIpAddress() ?: "127.0.0.1"
    return "http://$ip:$serverPort/stream"
  }

  private fun handleClientConnection(client: Socket) {
    try {
      client.soTimeout = 30000
      val inputStream = BufferedReader(InputStreamReader(client.getInputStream()))
      val outputStream = BufferedOutputStream(client.getOutputStream())

      val requestLine = inputStream.readLine() ?: return
      val parts = requestLine.split(" ")
      if (parts.size < 2) return

      val method = parts[0]
      val path = parts[1]

      var rangeStart: Long = 0
      var rangeEnd: Long = -1

      // Read headers
      var headerLine: String? = inputStream.readLine()
      while (!headerLine.isNullOrBlank()) {
        if (headerLine.startsWith("Range:", ignoreCase = true)) {
          val rangeHeader = headerLine.substringAfter(":").trim()
          if (rangeHeader.startsWith("bytes=")) {
            val rangeVal = rangeHeader.substringAfter("bytes=")
            val rangeTokens = rangeVal.split("-")
            rangeStart = rangeTokens[0].toLongOrNull() ?: 0L
            if (rangeTokens.size > 1 && rangeTokens[1].isNotBlank()) {
              rangeEnd = rangeTokens[1].toLongOrNull() ?: -1L
            }
          }
        }
        headerLine = inputStream.readLine()
      }

      serveMediaStream(client, outputStream, method == "HEAD", rangeStart, rangeEnd)
    } catch (e: SocketException) {
      // Normal when player cancels or seeks
    } catch (e: Exception) {
      Log.w(TAG, "Client stream handling exception: ${e.message}")
    } finally {
      try {
        client.close()
      } catch (_: Exception) {}
    }
  }

  private fun serveMediaStream(
    client: Socket,
    out: OutputStream,
    isHead: Boolean,
    requestedStart: Long,
    requestedEnd: Long
  ) {
    val media = currentMedia ?: return
    val totalLength = media.fileSizeBytes

    val start = requestedStart.coerceIn(0L, totalLength - 1)
    val end = if (requestedEnd in start until totalLength) requestedEnd else totalLength - 1
    val contentLength = end - start + 1

    val isPartial = requestedStart > 0 || requestedEnd > 0
    val statusLine = if (isPartial) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n"

    val headers = StringBuilder()
      .append(statusLine)
      .append("Content-Type: ${media.mimeType}\r\n")
      .append("Accept-Ranges: bytes\r\n")
      .append("Content-Length: $contentLength\r\n")
      .append("Access-Control-Allow-Origin: *\r\n")

    if (isPartial) {
      headers.append("Content-Range: bytes $start-$end/$totalLength\r\n")
    }
    headers.append("Connection: keep-alive\r\n\r\n")

    out.write(headers.toString().toByteArray(Charsets.UTF_8))
    out.flush()

    if (isHead) return

    // Stream the content chunk by chunk (64KB buffer)
    val chunkDelayMs = when (currentQuality) {
      StreamQuality.LOW -> 80L
      StreamQuality.MEDIUM -> 25L
      StreamQuality.HIGH_ORIGINAL -> 0L
    }

    var source: MediaStreamSource? = null
    try {
      source = openMediaSource(media)
      if (source != null) {
        val bufferSize = 65536 // 64 KB chunk
        val buffer = ByteArray(bufferSize)
        var remaining = contentLength

        if (source.channel != null) {
          // Instantaneous seek via FileChannel position (Supports 4+ hour large movie files)
          if (start > 0) {
            source.channel.position(start)
          }
          val byteBuffer = ByteBuffer.wrap(buffer)
          while (remaining > 0 && !client.isClosed && isRunning.get()) {
            val toRead = minOf(buffer.size.toLong(), remaining).toInt()
            byteBuffer.clear()
            byteBuffer.limit(toRead)
            val bytesRead = source.channel.read(byteBuffer)
            if (bytesRead <= 0) break

            out.write(buffer, 0, bytesRead)
            out.flush()

            remaining -= bytesRead
            totalBytesTransferred.addAndGet(bytesRead.toLong())
            bytesSinceLastCalc.addAndGet(bytesRead.toLong())

            if (chunkDelayMs > 0) {
              Thread.sleep(chunkDelayMs)
            }
          }
        } else if (source.stream != null) {
          val stream = source.stream
          if (start > 0) {
            var skipped = 0L
            while (skipped < start) {
              val skipAmt = stream.skip(start - skipped)
              if (skipAmt <= 0) break
              skipped += skipAmt
            }
          }

          while (remaining > 0 && !client.isClosed && isRunning.get()) {
            val toRead = minOf(buffer.size.toLong(), remaining).toInt()
            val bytesRead = stream.read(buffer, 0, toRead)
            if (bytesRead == -1) break

            out.write(buffer, 0, bytesRead)
            out.flush()

            remaining -= bytesRead
            totalBytesTransferred.addAndGet(bytesRead.toLong())
            bytesSinceLastCalc.addAndGet(bytesRead.toLong())

            if (chunkDelayMs > 0) {
              Thread.sleep(chunkDelayMs)
            }
          }
        }
      } else {
        // Fallback for demo synthetic media (e.g. test video buffer)
        generateSyntheticStream(out, contentLength, chunkDelayMs)
      }
    } finally {
      try {
        source?.close()
      } catch (_: Exception) {}
    }
  }

  private class MediaStreamSource(
    val pfd: ParcelFileDescriptor? = null,
    val channel: FileChannel? = null,
    val stream: InputStream? = null
  ) : Closeable {
    override fun close() {
      try { channel?.close() } catch (_: Exception) {}
      try { stream?.close() } catch (_: Exception) {}
      try { pfd?.close() } catch (_: Exception) {}
    }
  }

  private fun openMediaSource(media: LocalMediaItem): MediaStreamSource? {
    val uri = media.uri ?: if (media.uriString.isNotBlank()) Uri.parse(media.uriString) else null
    if (uri == null) return null

    // 1. SAF direct ParcelFileDescriptor & FileChannel (Zero overhead O(1) seek)
    try {
      val pfd = context.contentResolver.openFileDescriptor(uri, "r")
      if (pfd != null) {
        val fis = FileInputStream(pfd.fileDescriptor)
        val channel = fis.channel
        return MediaStreamSource(pfd = pfd, channel = channel, stream = fis)
      }
    } catch (e: Exception) {
      Log.d(TAG, "FileDescriptor not available for URI ($uri): ${e.message}. Falling back to openInputStream.")
    }

    // 2. Standard ContentResolver InputStream fallback
    try {
      val stream = context.contentResolver.openInputStream(uri)
      if (stream != null) {
        return MediaStreamSource(stream = stream)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to open ContentResolver stream for SAF uri: ${e.message}")
    }

    return null
  }

  private fun generateSyntheticStream(out: OutputStream, length: Long, delayMs: Long) {
    val buffer = ByteArray(32768)
    var remaining = length
    while (remaining > 0 && isRunning.get()) {
      val toWrite = minOf(buffer.size.toLong(), remaining).toInt()
      out.write(buffer, 0, toWrite)
      out.flush()
      remaining -= toWrite
      totalBytesTransferred.addAndGet(toWrite.toLong())
      bytesSinceLastCalc.addAndGet(toWrite.toLong())
      if (delayMs > 0) {
        Thread.sleep(delayMs)
      }
    }
  }

  fun stop() {
    isRunning.set(false)
    try {
      serverSocket?.close()
    } catch (_: Exception) {}
    serverJob?.cancel()
    serverSocket = null
    serverPort = 0
  }

  private fun getLocalIpAddress(): String? {
    try {
      val interfaces = NetworkInterface.getNetworkInterfaces()
      while (interfaces.hasMoreElements()) {
        val intf = interfaces.nextElement()
        val addresses = intf.inetAddresses
        while (addresses.hasMoreElements()) {
          val addr = addresses.nextElement()
          if (!addr.isLoopbackAddress && addr is Inet4Address) {
            return addr.hostAddress
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error obtaining local IP: ${e.message}")
    }
    return null
  }
}
