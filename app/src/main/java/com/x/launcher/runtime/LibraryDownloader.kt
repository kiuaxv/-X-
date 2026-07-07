package com.x.launcher.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class LibraryDownloadState(
 val isDownloading: Boolean = false,
 val progress: Float = 0f,
 val currentLibrary: String = "",
 val downloadedLibs: Int = 0,
 val totalLibs: Int = 0,
 val downloadedMb: Float = 0f,
 val totalMb: Float = 0f,
 val isComplete: Boolean = false,
 val error: String? = null
)

data class LibraryArtifact(
 val group: String,
 val name: String,
 val version: String,
 val classifier: String?,
 val path: String,
 val url: String,
 val sha1: String?,
 val size: Long
)

data class NativeArtifact(
 val name: String,
 val path: String,
 val url: String,
 val sha1: String?,
 val size: Long,
 val extractRules: List<ExtractRule> = emptyList()
)

data class ExtractRule(
 val action: String, // "include" or "exclude"
 val pattern: String
)

object LibraryDownloader {

 private const val LIBRARIES_BASE_URL = "https://libraries.minecraft.net"
 private const val FALLBACK_URL = "https://repo1.maven.org/maven2"

 private val _state = MutableStateFlow(LibraryDownloadState())
 val state: StateFlow<LibraryDownloadState> = _state

 fun getLibrariesDirectory(gameDir: File): File {
 return File(gameDir, "libraries").also { it.mkdirs() }
 }

 fun getNativesDirectory(gameDir: File): File {
 return File(gameDir, "natives").also { it.mkdirs() }
 }

 fun areLibrariesInstalled(gameDir: File, versionId: String): Boolean {
 val libDir = getLibrariesDirectory(gameDir)
 return libDir.exists() && libDir.walkTopDown().filter { it.isFile && it.extension == "jar" }.count() > 20
 }

 suspend fun downloadLibraries(
 gameDir: File,
 versionJson: JSONObject
 ): Result<File> = withContext(Dispatchers.IO) {
 runCatching {
 val libDir = getLibrariesDirectory(gameDir)
 val nativesDir = getNativesDirectory(gameDir)

 // Parse libraries from version JSON
 val libraries = versionJson.getJSONArray("libraries")
 val libraryArtifacts = mutableListOf<LibraryArtifact>()
 val nativeArtifacts = mutableListOf<NativeArtifact>()

 for (i in 0 until libraries.length()) {
 val lib = libraries.getJSONObject(i)

 // Check rules (OS filtering)
 if (!shouldLoadLibrary(lib)) continue

 val name = lib.getString("name")
 val downloads = lib.optJSONObject("downloads")

 if (downloads != null) {
 // Regular library
 val artifact = downloads.optJSONObject("artifact")
 if (artifact != null) {
 val libArtifact = parseArtifact(name, artifact)
 val targetFile = File(libDir, libArtifact.path)

 if (!targetFile.exists() || targetFile.length() != libArtifact.size) {
 libraryArtifacts.add(libArtifact)
 }
 }

 // Native library
 val classifiers = downloads.optJSONObject("classifiers")
 if (classifiers != null) {
 val abi = getPreferredABI()
 val classifierKey = mapAbiToClassifier(abi)

 val nativeArtifact = classifiers.optJSONObject(classifierKey)
 if (nativeArtifact != null) {
 val nativePath = nativeArtifact.getString("path")
 val nativeUrl = nativeArtifact.getString("url")
 val nativeSha1 = nativeArtifact.optString("sha1", null)
 val nativeSize = nativeArtifact.getLong("size")

 val extractRules = parseExtractRules(lib.optJSONObject("extract"))

 nativeArtifacts.add(
 NativeArtifact(
 name = name,
 path = nativePath,
 url = nativeUrl,
 sha1 = nativeSha1,
 size = nativeSize,
 extractRules = extractRules
 )
 )
 }
 }
 } else {
 // Fallback: construct URL from name
 val libArtifact = parseLibraryName(name, lib.optString("url", LIBRARIES_BASE_URL))
 val targetFile = File(libDir, libArtifact.path)

 if (!targetFile.exists()) {
 libraryArtifacts.add(libArtifact)
 }
 }
 }

 // Download all libraries
 val totalCount = libraryArtifacts.size + nativeArtifacts.size
 val totalSize = libraryArtifacts.sumOf { it.size } + nativeArtifacts.sumOf { it.size }

 _state.value = LibraryDownloadState(
 isDownloading = true,
 totalLibs = totalCount,
 totalMb = totalSize / 1_048_576f
 )

 var downloadedCount = 0
 var downloadedBytes = 0L

 // Download regular libraries
 for (artifact in libraryArtifacts) {
 _state.value = _state.value.copy(
 currentLibrary = artifact.name,
 downloadedLibs = downloadedCount,
 progress = downloadedCount.toFloat() / totalCount,
 downloadedMb = downloadedBytes / 1_048_576f
 )

 val targetFile = File(libDir, artifact.path)
 targetFile.parentFile?.mkdirs()

 downloadFile(artifact.url, targetFile)

 if (artifact.sha1 != null) {
 if (!verifySha1(targetFile, artifact.sha1)) {
 targetFile.delete()
 throw RuntimeException("SHA-1 mismatch: ${artifact.name}")
 }
 }

 downloadedCount++
 downloadedBytes += artifact.size
 }

 // Download and extract native libraries
 for (native in nativeArtifacts) {
 _state.value = _state.value.copy(
 currentLibrary = "native: ${native.name}",
 downloadedLibs = downloadedCount,
 progress = downloadedCount.toFloat() / totalCount,
 downloadedMb = downloadedBytes / 1_048_576f
 )

 val tempFile = File(nativesDir, "temp_${native.name.replace(":", "_")}.jar")
 downloadFile(native.url, tempFile)

 if (native.sha1 != null) {
 if (!verifySha1(tempFile, native.sha1)) {
 tempFile.delete()
 throw RuntimeException("SHA-1 mismatch: native ${native.name}")
 }
 }

 // Extract native .so files
 extractNatives(tempFile, nativesDir, native.extractRules)
 tempFile.delete()

 downloadedCount++
 downloadedBytes += native.size
 }

 _state.value = LibraryDownloadState(
 isDownloading = false,
 isComplete = true,
 downloadedLibs = downloadedCount,
 totalLibs = totalCount,
 progress = 1f,
 downloadedMb = downloadedBytes / 1_048_576f,
 totalMb = totalSize / 1_048_576f
 )

 libDir
 }.recoverCatching { e ->
 _state.value = LibraryDownloadState(
 isDownloading = false,
 error = e.message ?: "Library download failed"
 )
 throw e
 }
 }

 private fun parseArtifact(name: String, artifact: JSONObject): LibraryArtifact {
 val parts = name.split(":")
 val group = parts[0]
 val artifactName = parts[1]
 val version = parts[2]
 val classifier = if (parts.size > 3) parts[3] else null

 return LibraryArtifact(
 group = group,
 name = artifactName,
 version = version,
 classifier = classifier,
 path = artifact.getString("path"),
 url = artifact.getString("url"),
 sha1 = artifact.optString("sha1", null),
 size = artifact.getLong("size")
 )
 }

 private fun parseLibraryName(name: String, baseUrl: String): LibraryArtifact {
 val parts = name.split(":")
 val group = parts[0].replace(".", "/")
 val artifactName = parts[1]
 val version = parts[2]
 val classifier = if (parts.size > 3) "-${parts[3]}" else ""

 val path = "$group/$artifactName/$version/$artifactName-$version$classifier.jar"
 val url = "${baseUrl.trimEnd('/')}/$path"

 return LibraryArtifact(
 group = parts[0],
 name = artifactName,
 version = version,
 classifier = classifier.removePrefix("-").ifEmpty { null },
 path = path,
 url = url,
 sha1 = null,
 size = 0
 )
 }

 private fun parseExtractRules(extract: JSONObject?): List<ExtractRule> {
 if (extract == null) return emptyList()

 val rules = mutableListOf<ExtractRule>()
 val exclude = extract.optJSONArray("exclude")

 exclude?.let {
 for (i in 0 until it.length()) {
 rules.add(ExtractRule("exclude", it.getString(i)))
 }
 }

 return rules
 }

 private fun extractNatives(jarFile: File, targetDir: File, rules: List<ExtractRule>) {
 targetDir.mkdirs()

 java.util.jar.JarFile(jarFile).use { jar ->
 jar.entries().toList()
 .filter { it.name.endsWith(".so") }
 .filter { entry -> shouldExtract(entry.name, rules) }
 .forEach { entry ->
 val outputFile = File(targetDir, File(entry.name).name)
 jar.getInputStream(entry).use { input ->
 FileOutputStream(outputFile).use { output ->
 input.copyTo(output)
 }
 }
 outputFile.setExecutable(true)
 }
 }
 }

 private fun shouldExtract(name: String, rules: List<ExtractRule>): Boolean {
 rules.forEach { rule ->
 if (rule.action == "exclude" && name.contains(rule.pattern)) {
 return false
 }
 }
 return true
 }

 private fun shouldLoadLibrary(lib: JSONObject): Boolean {
 val rules = lib.optJSONArray("rules")
 if (rules == null) return true

 var allowed = false

 for (i in 0 until rules.length()) {
 val rule = rules.getJSONObject(i)
 val os = rule.optJSONObject("os")
 val action = rule.getString("action")

 if (os == null) {
 allowed = action == "allow"
 } else {
 val osName = os.optString("name", "")
 if (osName == "linux" || osName == "") {
 allowed = action == "allow"
 }
 }
 }

 return allowed
 }

 private fun mapAbiToClassifier(abi: String): String {
 return when (abi) {
 "arm64-v8a" -> "natives-linux-arm64"
 "armeabi-v7a" -> "natives-linux-arm32"
 "x86_64" -> "natives-linux"
 else -> "natives-linux-arm64"
 }
 }

 private fun downloadFile(url: String, targetFile: File) {
 targetFile.parentFile?.mkdirs()

 val conn = URL(url).openConnection() as HttpURLConnection
 conn.connect()

 if (conn.responseCode == 200) {
 conn.inputStream.use { input ->
 FileOutputStream(targetFile).use { output ->
 input.copyTo(output)
 }
 }
 } else {
 // Try fallback URL
 val fallbackUrl = url.replace(LIBRARIES_BASE_URL, FALLBACK_URL)
 val fallbackConn = URL(fallbackUrl).openConnection() as HttpURLConnection
 fallbackConn.connect()

 if (fallbackConn.responseCode == 200) {
 fallbackConn.inputStream.use { input ->
 FileOutputStream(targetFile).use { output ->
 input.copyTo(output)
 }
 }
 } else {
 throw RuntimeException("Download failed: $url (HTTP ${conn.responseCode})")
 }
 }
 }

 private fun verifySha1(file: File, expectedHash: String): Boolean {
 val sha1 = MessageDigest.getInstance("SHA-1")
 val hashBytes = sha1.digest(file.readBytes())
 return hashBytes.joinToString("") { "%02x".format(it) } == expectedHash
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

 fun getClasspath(gameDir: File): List<File> {
 val libDir = getLibrariesDirectory(gameDir)
 return libDir.walkTopDown()
 .filter { it.isFile && it.extension == "jar" }
 .toList()
 }

 fun getNativesPath(gameDir: File): String {
 return getNativesDirectory(gameDir).absolutePath
 }
}
