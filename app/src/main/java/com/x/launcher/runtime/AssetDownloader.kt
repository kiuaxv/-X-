package com.x.launcher.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class AssetDownloadState(
 val isDownloading: Boolean = false,
 val progress: Float = 0f,
 val currentFile: String = "",
 val downloadedFiles: Int = 0,
 val totalFiles: Int = 0,
 val downloadedMb: Float = 0f,
 val totalMb: Float = 0f,
 val isComplete: Boolean = false,
 val error: String? = null
)

data class AssetObject(
 val hash: String,
 val size: Long,
 val path: String
)

object AssetDownloader {

 private const val VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
 private const val ASSET_BASE_URL = "https://resources.download.minecraft.net"

 private val _state = MutableStateFlow(AssetDownloadState())
 val state: StateFlow<AssetDownloadState> = _state

 fun getAssetsDirectory(gameDir: File): File {
 return File(gameDir, "assets").also { it.mkdirs() }
 }

 fun getObjectsDirectory(gameDir: File): File {
 return File(getAssetsDirectory(gameDir), "objects").also { it.mkdirs() }
 }

 fun getIndexesDirectory(gameDir: File): File {
 return File(getAssetsDirectory(gameDir), "indexes").also { it.mkdirs() }
 }

 fun areAssetsInstalled(gameDir: File, versionId: String): Boolean {
 val objectsDir = getObjectsDirectory(gameDir)
 val indexFile = File(getIndexesDirectory(gameDir), "$versionId.json")
 return objectsDir.exists() && indexFile.exists() && countAssetFiles(objectsDir) > 100
 }

 suspend fun downloadAssets(
 gameDir: File,
 versionId: String,
 assetIndexUrl: String? = null
 ): Result<File> = withContext(Dispatchers.IO) {
 runCatching {
 val assetsDir = getAssetsDirectory(gameDir)
 val objectsDir = getObjectsDirectory(gameDir)
 val indexesDir = getIndexesDirectory(gameDir)

 // 1. Fetch asset index
 _state.value = AssetDownloadState(
 isDownloading = true,
 stage = "Fetching asset index..."
 )

 val indexUrl = assetIndexUrl ?: fetchAssetIndexUrl(versionId)
 val indexFile = File(indexesDir, "$versionId.json")
 downloadFile(indexUrl, indexFile)

 // 2. Parse index JSON
 val indexJson = JSONObject(indexFile.readText())
 val objects = indexJson.getJSONObject("objects")
 val totalSize = indexJson.optLong("totalSize", 0)

 val assetList = mutableListOf<AssetObject>()
 val keys = objects.keys()
 while (keys.hasNext()) {
 val key = keys.next()
 val obj = objects.getJSONObject(key)
 val hash = obj.getString("hash")
 val size = obj.getLong("size")

 // Asset path: first 2 chars of hash / full hash
 val subDir = hash.substring(0, 2)
 val assetPath = "$subDir/$hash"

 val targetFile = File(objectsDir, assetPath)
 if (!targetFile.exists() || targetFile.length() != size) {
 assetList.add(AssetObject(hash, size, assetPath))
 }
 }

 if (assetList.isEmpty()) {
 _state.value = AssetDownloadState(
 isDownloading = false,
 isComplete = true,
 stage = "All assets already downloaded"
 )
 return@runCatching assetsDir
 }

 // 3. Download all assets
 _state.value = _state.value.copy(
 totalFiles = assetList.size,
 totalMb = (assetList.sumOf { it.size } / 1_048_576f)
 )

 var downloadedFiles = 0
 var downloadedBytes = 0L

 assetList.forEach { asset ->
 val subDir = asset.hash.substring(0, 2)
 val dir = File(objectsDir, subDir).also { it.mkdirs() }
 val targetFile = File(dir, asset.hash)

 val assetUrl = "$ASSET_BASE_URL/$subDir/${asset.hash}"

 _state.value = _state.value.copy(
 currentFile = asset.hash,
 downloadedFiles = downloadedFiles,
 progress = downloadedFiles.toFloat() / assetList.size,
 downloadedMb = downloadedBytes / 1_048_576f
 )

 downloadFile(assetUrl, targetFile)

 // Verify hash
 if (!verifyHash(targetFile, asset.hash)) {
 targetFile.delete()
 throw RuntimeException("Hash mismatch for asset: ${asset.hash}")
 }

 downloadedFiles++
 downloadedBytes += asset.size
 }

 _state.value = AssetDownloadState(
 isDownloading = false,
 isComplete = true,
 downloadedFiles = downloadedFiles,
 totalFiles = assetList.size,
 progress = 1f,
 downloadedMb = downloadedBytes / 1_048_576f,
 stage = "Assets downloaded"
 )

 assetsDir
 }.recoverCatching { e ->
 _state.value = AssetDownloadState(
 isDownloading = false,
 error = e.message ?: "Asset download failed"
 )
 throw e
 }
 }

 private var stage: String = ""
 private fun setStage(s: String) {
 stage = s
 _state.value = _state.value.copy(stage = s)
 }

 private suspend fun fetchAssetIndexUrl(versionId: String): String {
 return withContext(Dispatchers.IO) {
 // Fetch version manifest
 val manifest = fetchJson(VERSION_MANIFEST_URL)
 val versions = manifest.getJSONArray("versions")

 // Find our version
 var versionUrl: String? = null
 for (i in 0 until versions.length()) {
 val version = versions.getJSONObject(i)
 if (version.getString("id") == versionId) {
 versionUrl = version.getString("url")
 break
 }
 }

 if (versionUrl == null) {
 throw RuntimeException("Version $versionId not found in manifest")
 }

 // Fetch version JSON
 val versionJson = fetchJson(versionUrl!!)
 val assetIndex = versionJson.getJSONObject("assetIndex")
 assetIndex.getString("url")
 }
 }

 private fun fetchJson(url: String): JSONObject {
 val conn = URL(url).openConnection() as HttpURLConnection
 conn.requestMethod = "GET"
 conn.setRequestProperty("Accept", "application/json")

 val response = conn.inputStream.bufferedReader().readText()
 return JSONObject(response)
 }

 private fun downloadFile(url: String, targetFile: File) {
 targetFile.parentFile?.mkdirs()

 val conn = URL(url).openConnection() as HttpURLConnection
 conn.connect()

 if (conn.responseCode != 200) {
 throw RuntimeException("Download failed: $url (HTTP ${conn.responseCode})")
 }

 conn.inputStream.use { input ->
 FileOutputStream(targetFile).use { output ->
 input.copyTo(output)
 }
 }
 }

 private fun verifyHash(file: File, expectedHash: String): Boolean {
 val sha1 = MessageDigest.getInstance("SHA-1")
 val fileBytes = file.readBytes()
 val hashBytes = sha1.digest(fileBytes)
 return hashBytes.joinToString("") { "%02x".format(it) } == expectedHash
 }

 private fun countAssetFiles(dir: File): Int {
 return dir.walkTopDown().filter { it.isFile }.count()
 }

 fun getAssetPath(hash: String, gameDir: File): File {
 val subDir = hash.substring(0, 2)
 return File(getObjectsDirectory(gameDir), "$subDir/$hash")
 }
}
