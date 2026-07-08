package com.x.launcher.more

import android.content.Context
import android.app.ActivityManager
import com.x.launcher.more.MoreFeatures.getBatteryLevel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object MoreToolsData {

 // ════════════════════════════════════════════════════════════════
 // BACKUP / RESTORE
 // ════════════════════════════════════════════════════════════════

 object BackupRestore {

 data class BackupEntry(
 val name: String,
 val file: File,
 val sizeMb: Float,
 val timestamp: String,
 val contents: List<String>
 )

 fun backupWorlds(gameDir: File, worldName: String? = null): Boolean {
 val savesDir = File(gameDir, "saves")
 val backupDir = File(gameDir, "backups").also { it.mkdirs() }
 val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
 val backupName = "backup_$timestamp.zip"
 val backupFile = File(backupDir, backupName)

 try {
 ZipOutputStream(backupFile.outputStream()).use { zos ->
 val worlds = if (worldName != null) {
 listOf(File(savesDir, worldName))
 } else {
 savesDir.listFiles()?.toList().orEmpty()
 }

 worlds.filter { it.exists() && it.isDirectory }.forEach { world ->
 world.walkTopDown().forEach { file ->
 if (file.isFile) {
 val relPath = "saves/${world.name}/${file.relativeTo(world).path}"
 zos.putNextEntry(ZipEntry(relPath))
 file.inputStream().use { it.copyTo(zos) }
 zos.closeEntry()
 }
 }
 }
 }
 return true
 } catch (_: Exception) {
 return false
 }
 }

 fun restoreBackup(backupFile: File, gameDir: File): Boolean {
 if (!backupFile.exists()) return false

 try {
 val savesDir = File(gameDir, "saves").also { it.mkdirs() }
 ZipFile(backupFile).use { zip ->
 zip.entries().toList().forEach { entry ->
 val outputFile = File(savesDir, entry.name.substringAfter("saves/"))
 if (entry.isDirectory) {
 outputFile.mkdirs()
 } else {
 outputFile.parentFile?.mkdirs()
 zip.getInputStream(entry).use { input ->
 outputFile.outputStream().use { input.copyTo(it) }
 }
 }
 }
 }
 return true
 } catch (_: Exception) {
 return false
 }
 }

 fun listBackups(gameDir: File): List<BackupEntry> {
 val backupDir = File(gameDir, "backups")
 if (!backupDir.exists()) return emptyList()

 return backupDir.listFiles()
 ?.filter { it.isFile && it.extension == "zip" }
 ?.sortedByDescending { it.lastModified() }
 ?.map { file ->
 val contents = mutableListOf<String>()
 try {
 ZipFile(file).use { zip ->
 zip.entries().toList().take(50).forEach { entry ->
 contents.add(entry.name)
 }
 }
 } catch (_: Exception) {}

 BackupEntry(
 name = file.nameWithoutExtension,
 file = file,
 sizeMb = file.length() / 1048576f,
 timestamp = file.name.removePrefix("backup_").removeSuffix(".zip"),
 contents = contents
 )
 }
 .orEmpty()
 }

 fun deleteBackup(backupFile: File): Boolean {
 return backupFile.delete()
 }
 }

 // ════════════════════════════════════════════════════════════════
 // RESOURCE PACKS MANAGER
 // ════════════════════════════════════════════════════════════════

 object ResourcePacksManager {

 data class ResourcePack(
 val name: String,
 val file: File,
 val isZip: Boolean,
 val isActive: Boolean = false,
 val description: String = "",
 val packFormat: Int = 0
 )

 fun scanPacks(gameDir: File): List<ResourcePack> {
 val rpDir = File(gameDir, "resourcepacks")
 if (!rpDir.exists() || !rpDir.isDirectory) return emptyList()

 val activeFile = File(gameDir, "options.txt")
 var activeName: String? = null
 if (activeFile.exists()) {
 val options = activeFile.readText()
 val regex = Regex("resourcePacks:\\[\"(.*?)\"\\]")
 activeName = regex.find(options)?.groupValues?.get(1)
 }

 return rpDir.listFiles()
 ?.filter { it.isFile && it.extension == "zip" }
 ?.map { file ->
 var desc = ""
 var format = 0

 try {
 ZipFile(file).use { zip ->
 val mcmeta = zip.getEntry("pack.mcmeta")
 if (mcmeta != null) {
 val json = JSONObject(zip.getInputStream(mcmeta).bufferedReader().readText())
 val packInfo = json.optJSONObject("pack")
 desc = packInfo?.optString("description", "") ?: ""
 format = packInfo?.optInt("pack_format", 0) ?: 0
 }
 }
 } catch (_: Exception) {}

 ResourcePack(
 name = file.nameWithoutExtension,
 file = file,
 isZip = true,
 isActive = activeName == file.nameWithoutExtension,
 description = desc,
 packFormat = format
 )
 }
 .orEmpty()
 }

 fun setActivePack(gameDir: File, packName: String) {
 val file = File(gameDir, "xclient/active_resourcepack.txt")
 file.parentFile?.mkdirs()
 file.writeText(packName)
 }
 }

 // ════════════════════════════════════════════════════════════════
 // PROFILES MANAGER
 // ════════════════════════════════════════════════════════════════

 object ProfilesManager {

 data class Profile(
 val name: String,
 val versionId: String,
 val minMemoryMb: Int = 512,
 val maxMemoryMb: Int = 1024,
 val javaArgs: List<String> = emptyList(),
 val enabledMods: List<String> = emptyList(),
 val shaderPack: String? = null,
 val resourcePack: String? = null,
 val renderDistance: Int = 12,
 val fov: Int = 70,
 val xopLevel: String = "BALANCED",
 val createdAt: String = "",
 val lastUsed: String = ""
 )

 fun saveProfile(gameDir: File, profile: Profile): Boolean {
 val profilesDir = File(gameDir, "profiles").also { it.mkdirs() }
 val file = File(profilesDir, "${profile.name}.json")

 return try {
 val json = JSONObject().apply {
 put("name", profile.name)
 put("versionId", profile.versionId)
 put("minMemoryMb", profile.minMemoryMb)
 put("maxMemoryMb", profile.maxMemoryMb)
 put("javaArgs", JSONArray(profile.javaArgs))
 put("enabledMods", JSONArray(profile.enabledMods))
 put("shaderPack", profile.shaderPack ?: JSONObject.NULL)
 put("resourcePack", profile.resourcePack ?: JSONObject.NULL)
 put("renderDistance", profile.renderDistance)
 put("fov", profile.fov)
 put("xopLevel", profile.xopLevel)
 put("createdAt", profile.createdAt)
 put("lastUsed", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
 }
 file.writeText(json.toString(2))
 true
 } catch (_: Exception) {
 false
 }
 }

 fun loadProfile(gameDir: File, name: String): Profile? {
 val file = File(gameDir, "profiles/$name.json")
 if (!file.exists()) return null

 return try {
 val json = JSONObject(file.readText())
 Profile(
 name = json.getString("name"),
 versionId = json.getString("versionId"),
 minMemoryMb = json.optInt("minMemoryMb", 512),
 maxMemoryMb = json.optInt("maxMemoryMb", 1024),
 javaArgs = json.optJSONArray("javaArgs")?.let { arr ->
 List(arr.length()) { arr.getString(it) }
 } ?: emptyList(),
 enabledMods = json.optJSONArray("enabledMods")?.let { arr ->
 List(arr.length()) { arr.getString(it) }
 } ?: emptyList(),
 shaderPack = json.optString("shaderPack", null),
 resourcePack = json.optString("resourcePack", null),
 renderDistance = json.optInt("renderDistance", 12),
 fov = json.optInt("fov", 70),
 xopLevel = json.optString("xopLevel", "BALANCED"),
 createdAt = json.optString("createdAt", ""),
 lastUsed = json.optString("lastUsed", "")
 )
 } catch (_: Exception) {
 null
 }
 }

 fun listProfiles(gameDir: File): List<Profile> {
 val profilesDir = File(gameDir, "profiles")
 if (!profilesDir.exists()) return emptyList()

 return profilesDir.listFiles()
 ?.filter { it.isFile && it.extension == "json" }
 ?.mapNotNull { loadProfile(gameDir, it.nameWithoutExtension) }
 ?.sortedByDescending { it.lastUsed }
 .orEmpty()
 }

 fun deleteProfile(gameDir: File, name: String): Boolean {
 return File(gameDir, "profiles/$name.json").delete()
 }
 }

 // ════════════════════════════════════════════════════════════════
 // RECENT WORLDS
 // ════════════════════════════════════════════════════════════════

 object RecentWorlds {

 data class WorldEntry(
 val name: String,
 val folder: File,
 val lastPlayed: String,
 val sizeMb: Float,
 val gameMode: String,
 val difficulty: String
 )

 fun listWorlds(gameDir: File, max: Int = 10): List<WorldEntry> {
 val savesDir = File(gameDir, "saves")
 if (!savesDir.exists()) return emptyList()

 return savesDir.listFiles()
 ?.filter { it.isDirectory }
 ?.map { folder ->
 val levelDat = File(folder, "level.dat")
 var lastPlayed = ""

 if (levelDat.exists()) {
 lastPlayed = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
 .format(Date(folder.lastModified()))
 }

 val size = folder.walkTopDown().filter { it.isFile }.sumOf { it.length() }

 WorldEntry(
 name = folder.name,
 folder = folder,
 lastPlayed = lastPlayed,
 sizeMb = size / 1048576f,
 gameMode = "survival",
 difficulty = "normal"
 )
 }
 ?.sortedByDescending { it.folder.lastModified() }
 ?.take(max)
 .orEmpty()
 }

 fun deleteWorld(worldFolder: File): Boolean {
 return worldFolder.deleteRecursively()
 }
 }

 // ════════════════════════════════════════════════════════════════
 // FAVORITE SERVERS
 // ════════════════════════════════════════════════════════════════

 object FavoriteServers {

 data class ServerEntry(
 val name: String,
 val ip: String,
 val port: Int = 25565,
 val isFavorite: Boolean = true,
 val ping: Int = 0,
 val motd: String = ""
 )

 fun saveServers(gameDir: File, servers: List<ServerEntry>): Boolean {
 val file = File(gameDir, "servers.dat")
 val arr = JSONArray()
 servers.forEach { server ->
 arr.put(JSONObject().apply {
 put("name", server.name)
 put("ip", server.ip)
 put("port", server.port)
 put("favorite", server.isFavorite)
 put("ping", server.ping)
 put("motd", server.motd)
 })
 }
 return try {
 file.writeText(JSONObject().apply { put("servers", arr) }.toString(2))
 true
 } catch (_: Exception) {
 false
 }
 }

 fun loadServers(gameDir: File): List<ServerEntry> {
 val file = File(gameDir, "servers.dat")
 if (!file.exists()) return emptyList()

 return try {
 val json = JSONObject(file.readText())
 val arr = json.getJSONArray("servers")
 List(arr.length()) { i ->
 val obj = arr.getJSONObject(i)
 ServerEntry(
 name = obj.getString("name"),
 ip = obj.getString("ip"),
 port = obj.optInt("port", 25565),
 isFavorite = obj.optBoolean("favorite", true),
 ping = obj.optInt("ping", 0),
 motd = obj.optString("motd", "")
 )
 }
 } catch (_: Exception) {
 emptyList()
 }
 }

 fun addServer(gameDir: File, server: ServerEntry): Boolean {
 val current = loadServers(gameDir).toMutableList()
 if (current.none { it.ip == server.ip }) {
 current.add(server)
 return saveServers(gameDir, current)
 }
 return false
 }

 fun removeServer(gameDir: File, ip: String): Boolean {
 val current = loadServers(gameDir).toMutableList()
 current.removeAll { it.ip == ip }
 return saveServers(gameDir, current)
 }
 }

 // ════════════════════════════════════════════════════════════════
 // ONE TAP OPTIMIZE
 // ════════════════════════════════════════════════════════════════

 object OneTapOptimize {

 data class OptimizeResult(
 val actions: List<String>,
 val memoryBeforeMb: Int,
 val memoryAfterMb: Int,
 val modsDisabled: Int,
 val settingsChanged: Int
 )

 fun optimize(
 context: Context,
 gameDir: File,
 currentRamMb: Int
 ): OptimizeResult {
 val actions = mutableListOf<String>()
 var modsDisabled = 0
 var settingsChanged = 0
 var targetMemory = currentRamMb

 // Clear cache
 val cacheDir = File(gameDir, "xop/cache")
 if (cacheDir.exists()) {
 cacheDir.listFiles()?.forEach { it.delete() }
 actions += "Cleared XOP cache"
 }

 // Clear old crash reports
 MoreToolsCore.CrashAnalyzer.clearOldCrashes(gameDir, 3)
 actions += "Cleared old crash reports"

 // Suggest memory based on device
 val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
 val totalRam = try {
 val info = ActivityManager.MemoryInfo()
 am.getMemoryInfo(info)
 (info.totalMem / 1048576L).toInt()
 } catch (_: Exception) {
 4096
 }

 targetMemory = when {
 totalRam >= 8192 -> 2048
 totalRam >= 6144 -> 1536
 totalRam >= 4096 -> 1024
 else -> 512
 }
 actions += "Optimal memory: ${targetMemory}MB"
 settingsChanged++

 // Check for conflicts
 val conflicts = MoreToolsCore.ModConflictDetector.scanMods(gameDir)
 if (conflicts.conflicts.isNotEmpty()) {
 actions += "Found ${conflicts.conflicts.size} mod conflicts"
 modsDisabled = conflicts.conflicts.size
 }

 // Check dependencies
 val deps = MoreToolsCore.DependencyDetector.checkDependencies(gameDir)
 if (deps.missing.isNotEmpty()) {
 actions += "Found ${deps.missing.size} missing dependencies"
 }

 // Repair corrupted files
 val repair = MoreToolsCore.RepairMode.repairVersion(gameDir, "auto")
 if (repair.deletedFiles > 0) {
 actions += "Deleted ${repair.deletedFiles} corrupted files"
 }

 // Suggest render distance based on device
 val suggestedRender = when {
 totalRam >= 8192 -> 16
 totalRam >= 6144 -> 14
 totalRam >= 4096 -> 12
 else -> 8
 }
 actions += "Suggested render distance: $suggestedRender chunks"
 settingsChanged++

 // Check battery
 val batteryLevel = getBatteryLevel(context)
 if (batteryLevel <= 20) {
 actions += "Low battery ($batteryLevel%) — battery saver recommended"
 settingsChanged++
 }

 return OptimizeResult(
 actions = actions,
 memoryBeforeMb = currentRamMb,
 memoryAfterMb = targetMemory,
 modsDisabled = modsDisabled,
 settingsChanged = settingsChanged
 )
 }
 }
}
