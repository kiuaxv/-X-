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
import java.util.zip.ZipInputStream

data class JREInstallState(
 val isInstalling: Boolean = false,
 val progress: Float = 0f,
 val downloadedMb: Float = 0f,
 val totalMb: Float = 0f,
 val stage: String = "",
 val isComplete: Boolean = false,
 val error: String? = null
)

object JREInstaller {

 private val _state = MutableStateFlow(JREInstallState())
 val state: StateFlow<JREInstallState> = _state

 // Pre-built JRE for Android (ARM64 / ARM32 / x86_64)
 private val JRE_SOURCES = mapOf(
 "arm64-v8a" to "https://github.com/PojavLauncherTeam/jre-impl-android/releases/download/v1.0.0/jre8-arm64.tar.gz",
 "armeabi-v7a" to "https://github.com/PojavLauncherTeam/jre-impl-android/releases/download/v1.0.0/jre8-arm32.tar.gz",
 "x86_64" to "https://github.com/PojavLauncherTeam/jre-impl-android/releases/download/v1.0.0/jre8-x86_64.tar.gz"
 )

 fun getJREDirectory(gameDir: File): File {
 return File(gameDir, "jvm-runtime").also { it.mkdirs() }
 }

 fun isJREInstalled(gameDir: File): Boolean {
 val jreDir = getJREDirectory(gameDir)
 val javaBin = File(jreDir, "bin/java")
 val libDir = File(jreDir, "lib")
 return javaBin.exists() && libDir.exists()
 }

 suspend fun installJRE(
 context: Context,
 gameDir: File
 ): Result<File> = withContext(Dispatchers.IO) {
 runCatching {
 // Detect ABI
 val abi = getPreferredABI()
 val downloadUrl = JRE_SOURCES[abi]
 ?: throw RuntimeException("No JRE available for architecture: $abi")

 _state.value = JREInstallState(
 isInstalling = true,
 stage = "Downloading JRE ($abi)..."
 )

 val jreDir = getJREDirectory(gameDir)
 val tempArchive = File(context.cacheDir, "jre-download.tar.gz")

 // Download JRE archive
 val conn = URL(downloadUrl).openConnection() as HttpURLConnection
 conn.connect()
 val totalBytes = conn.contentLengthLong.toFloat()
 var downloadedBytes = 0L

 conn.inputStream.use { input ->
 FileOutputStream(tempArchive).use { output ->
 val buffer = ByteArray(8192)
 var bytesRead: Int
 while (input.read(buffer).also { bytesRead = it } != -1) {
 output.write(buffer, 0, bytesRead)
 downloadedBytes += bytesRead
 _state.value = _state.value.copy(
 progress = if (totalBytes > 0) downloadedBytes / totalBytes else 0f,
 downloadedMb = downloadedBytes / 1_048_576f,
 totalMb = totalBytes / 1_048_576f
 )
 }
 }
 }

 _state.value = _state.value.copy(
 stage = "Extracting JRE...",
 progress = 0f
 )

 // Extract tar.gz
 extractTarGz(tempArchive, jreDir)

 // Clean up
 tempArchive.delete()

 // Find extracted java binary
 val javaBin = findJavaBinary(jreDir)
 if (javaBin == null) {
 throw RuntimeException("java binary not found after extraction")
 }

 // Set executable permissions
 javaBin.setExecutable(true)
 setLibPermissions(jreDir)

 _state.value = JREInstallState(
 isInstalling = false,
 isComplete = true,
 stage = "JRE installed"
 )

 jreDir
 }.recoverCatching { e ->
 _state.value = JREInstallState(
 isInstalling = false,
 error = e.message ?: "JRE installation failed"
 )
 throw e
 }
 }

 private fun extractTarGz(archive: File, targetDir: File) {
 // Use system tar command (faster than pure Kotlin)
 val process = ProcessBuilder("tar", "-xzf", archive.absolutePath, "-C", targetDir.absolutePath)
 .redirectErrorStream(true)
 .start()

 val output = process.inputStream.bufferedReader().readText()
 val exitCode = process.waitFor()

 if (exitCode != 0) {
 throw RuntimeException("tar extraction failed: $output")
 }
 }

 private fun findJavaBinary(jreDir: File): File? {
 // Common patterns after extraction
 val possiblePaths = listOf(
 File(jreDir, "bin/java"),
 File(jreDir, "jre/bin/java"),
 File(jreDir, "lib/jvm/jre/bin/java")
 )

 possiblePaths.firstOrNull { it.exists() }?.let { return it }

 // Search recursively
 return jreDir.walkTopDown()
 .firstOrNull { it.name == "java" && it.isFile }
 }

 private fun setLibPermissions(jreDir: File) {
 jreDir.walkTopDown()
 .filter { it.isFile && (it.name.endsWith(".so") || it.name == "java") }
 .forEach { it.setExecutable(true) }
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

 fun getJavaHome(gameDir: File): File {
 val jreDir = getJREDirectory(gameDir)
 val javaBin = findJavaBinary(jreDir)
 return javaBin?.parentFile?.parentFile ?: jreDir
 }
}
