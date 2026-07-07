package com.x.launcher.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class DownloadState(
 val versionId: String = "",
 val progress: Float = 0f,
 val downloadedMb: Int = 0,
 val totalMb: Int = 0,
 val isDownloading: Boolean = false,
 val isComplete: Boolean = false,
 val error: String? = null
)

object VersionDownloader {

 private val _state = MutableStateFlow(DownloadState())
 val state: StateFlow<DownloadState> = _state

 private var isCancelled = false

 suspend fun download(
 version: GameVersion,
 targetDir: File
 ): DownloadState = withContext(Dispatchers.IO) {

 isCancelled = false
 _state.value = DownloadState(
 versionId = version.id,
 totalMb = version.sizeMb,
 isDownloading = true
 )

 try {
 val targetFile = File(targetDir, "${version.id}.jar")
 targetDir.mkdirs()

 val connection = URL(version.url).openConnection() as HttpURLConnection
 connection.connectTimeout = 15000
 connection.readTimeout = 15000
 connection.connect()

 val totalBytes = connection.contentLengthLong
 val totalMb = (totalBytes / 1048576).toInt()

 connection.inputStream.use { input ->
 FileOutputStream(targetFile).use { output ->
 val buffer = ByteArray(8192)
 var downloadedBytes = 0L
 var bytesRead: Int

 while (input.read(buffer).also { bytesRead = it } != -1) {
 if (isCancelled) {
 targetFile.delete()
 _state.value = DownloadState(
 versionId = version.id,
 error = "Download cancelled"
 )
 return@withContext _state.value
 }

 output.write(buffer, 0, bytesRead)
 downloadedBytes += bytesRead

 val progress = downloadedBytes.toFloat() / totalBytes
 val downloadedMb = (downloadedBytes / 1048576).toInt()

 _state.value = DownloadState(
 versionId = version.id,
 progress = progress,
 downloadedMb = downloadedMb,
 totalMb = totalMb,
 isDownloading = true
 )

 VersionManager.updateProgress(version.id, progress)
 }
 }
 }

 VersionManager.markAsDownloaded(version.id)

 _state.value = _state.value.copy(
 progress = 1f,
 isDownloading = false,
 isComplete = true
 )

 } catch (e: Exception) {
 _state.value = _state.value.copy(
 isDownloading = false,
 error = e.message
 )
 }

 _state.value
 }

 fun cancel() {
 isCancelled = true
 }
}
