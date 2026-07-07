package com.x.launcher.runtime

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class Gl4esState(
 val isInstalling: Boolean = false,
 val progress: Float = 0f,
 val stage: String = "",
 val isComplete: Boolean = false,
 val error: String? = null
)

object Gl4esManager {

 private val _state = MutableStateFlow(Gl4esState())
 val state: StateFlow<Gl4esState> = _state

 // Pre-built gl4es libraries for each ABI
 private val GL4ES_SOURCES = mapOf(
 "arm64-v8a" to "https://github.com/PojavLauncherTeam/gl4es/releases/download/v1.1.4/libgl4es-arm64.so",
 "armeabi-v7a" to "https://github.com/PojavLauncherTeam/gl4es/releases/download/v1.1.4/libgl4es-arm32.so",
 "x86_64" to "https://github.com/PojavLauncherTeam/gl4es/releases/download/v1.1.4/libgl4es-x86_64.so"
 )

 private val ADDITIONAL_LIBS = mapOf(
 "libglfw.so" to "https://github.com/PojavLauncherTeam/glfw-impl-android/releases/download/v3.3/libglfw-arm64.so",
 "libopenal.so" to "https://github.com/PojavLauncherTeam/openal-soft-android/releases/download/v1.21/libopenal-arm64.so"
 )

 fun getLibDirectory(gameDir: File): File {
 return File(gameDir, "natives").also { it.mkdirs() }
 }

 fun isGl4esInstalled(gameDir: File): Boolean {
 val libDir = getLibDirectory(gameDir)
 val abi = getPreferredABI()
 return File(libDir, "libgl4es.so").exists()
 }

 suspend fun install(
 context: Context,
 gameDir: File
 ): Result<File> = withContext(Dispatchers.IO) {
 runCatching {
 val abi = getPreferredABI()
 val libDir = getLibDirectory(gameDir)

 // Download libgl4es.so
 _state.value = Gl4esState(
 isInstalling = true,
 stage = "Downloading gl4es ($abi)..."
 )

 downloadLibrary(
 context = context,
 url = GL4ES_SOURCES[abi] ?: throw RuntimeException("No gl4es for $abi"),
 targetFile = File(libDir, "libgl4es.so")
 )

 // Download additional libraries
 _state.value = Gl4esState(
 isInstalling = true,
 stage = "Downloading supporting libraries..."
 )

 ADDITIONAL_LIBS.forEach { (libName, libUrl) ->
 val abiSpecificUrl = libUrl.replace("arm64", abi.replace("-v8a", "").replace("armeabi-", "arm"))
 downloadLibrary(
 context = context,
 url = abiSpecificUrl,
 targetFile = File(libDir, libName)
 )
 }

 // Set executable permissions
 libDir.walkTopDown()
 .filter { it.isFile && it.name.endsWith(".so") }
 .forEach { it.setExecutable(true) }

 _state.value = Gl4esState(
 isInstalling = false,
 isComplete = true,
 stage = "gl4es installed"
 )

 libDir
 }.recoverCatching { e ->
 _state.value = Gl4esState(
 isInstalling = false,
 error = e.message ?: "gl4es installation failed"
 )
 throw e
 }
 }

 private suspend fun downloadLibrary(
 context: Context,
 url: String,
 targetFile: File
 ) {
 val conn = URL(url).openConnection() as HttpURLConnection
 conn.connect()

 if (conn.responseCode != 200) {
 throw RuntimeException("Failed to download: $url (HTTP ${conn.responseCode})")
 }

 val totalBytes = conn.contentLengthLong.toFloat()
 var downloadedBytes = 0L

 conn.inputStream.use { input ->
 FileOutputStream(targetFile).use { output ->
 val buffer = ByteArray(8192)
 var bytesRead: Int
 while (input.read(buffer).also { bytesRead = it } != -1) {
 output.write(buffer, 0, bytesRead)
 downloadedBytes += bytesRead
 _state.value = _state.value.copy(
 progress = if (totalBytes > 0) downloadedBytes / totalBytes else 0f
 )
 }
 }
 }
 }

 fun getLibPath(gameDir: File): String {
 return getLibDirectory(gameDir).absolutePath
 }

 fun setupEnvironment(gameDir: File): Map<String, String> {
 val libDir = getLibDirectory(gameDir)
 return mapOf(
 "LD_LIBRARY_PATH" to libDir.absolutePath,
 "LIBGL_ALWAYS_SOFTWARE" to "0",
 "LIBGL_FB" to "3",
 "LIBGL_GL" to "35",
 "LIBGL_ES" to "3",
 "LIBGL_RECYCLE" to "1",
 "LIBGL_BATCH" to "0",
 "LIBGL_SILENTSTUB" to "1",
 "PRIMITIVE_RESTART" to "0"
 )
 }

 private fun getPreferredABI(): String {
 return try {
 val abis = android.os.Build.SUPPORTED_ABIS
 when {
 abis.contains("arm64-v8a") -> "arm64-v8a"
 abis.contains("armeabi-v7a") -> "armeabi-v7a"
 abis.contains("x86_64") -> "x86_64"
 else -> "arm64-v8a"
 }
 } catch (e: Exception) {
 "arm64-v8a"
 }
 }
}
