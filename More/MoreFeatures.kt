package com.x.launcher.more

import android.content.Context
import android.os.BatteryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MoreConfig(
 var enableCrashAnalyzer: Boolean = true,
 var enableRepairMode: Boolean = true,
 var enableSmartUpdate: Boolean = true,
 var enableBackupRestore: Boolean = true,
 var enableModConflictDetector: Boolean = true,
 var enableDependencyDetector: Boolean = true,
 var enableResourcePacks: Boolean = true,
 var enableProfiles: Boolean = true,
 var enableBatterySaver: Boolean = true,
 var enableThermalProtection: Boolean = true,
 var enableQuickToggles: Boolean = true,
 var enablePerformanceMonitor: Boolean = true,
 var enableRecentWorlds: Boolean = true,
 var enableFavoriteServers: Boolean = true,
 var enableOneTapOptimize: Boolean = true,
 var enableNewsFeed: Boolean = true,
 var enablePlaytimeStats: Boolean = true,
 var enableThemeSystem: Boolean = true,
 var enableScreenshotTools: Boolean = true,
 var enableCompatibilityPresets: Boolean = true
)

data class MoreState(
 val isReady: Boolean = false,
 val isPreparing: Boolean = false,
 val warnings: List<String> = emptyList(),
 val errors: List<String> = emptyList(),
 val applied: List<String> = emptyList()
)

data class MoreRuntimeBundle(
 val success: Boolean,
 val javaArgs: List<String>,
 val envVars: Map<String, String>,
 val message: String
)

object MoreFeatures {

 private val _state = MutableStateFlow(MoreState())
 val state: StateFlow<MoreState> = _state

 suspend fun prepareForRuntime(
 context: Context,
 gameDir: File,
 moreConfig: MoreConfig = MoreConfig()
 ): MoreRuntimeBundle = withContext(Dispatchers.IO) {
 _state.value = MoreState(isPreparing = true)

 try {
 val javaArgs = mutableListOf<String>()
 val envVars = mutableMapOf<String, String>()
 val applied = mutableListOf<String>()

 // ── Crash Analyzer
 if (moreConfig.enableCrashAnalyzer) {
 javaArgs += listOf(
 "-Dmore.crash.analyzer=true",
 "-Dmore.crash.autoreport=true",
 "-Dmore.crash.dir=${File(gameDir, "crash-reports").absolutePath}"
 )
 envVars["MORE_CRASH_ANALYZER"] = "1"
 applied += "Crash Analyzer"
 }

 // ── Repair Mode
 if (moreConfig.enableRepairMode) {
 javaArgs += "-Dmore.repair.mode=true"
 envVars["MORE_REPAIR_MODE"] = "1"
 applied += "Repair Mode"
 }

 // ── Smart Update
 if (moreConfig.enableSmartUpdate) {
 javaArgs += listOf(
 "-Dmore.smartupdate=true",
 "-Dmore.smartupdate.partial=true",
 "-Dmore.smartupdate.resume=true"
 )
 envVars["MORE_SMART_UPDATE"] = "1"
 applied += "Smart Update"
 }

 // ── Backup / Restore
 if (moreConfig.enableBackupRestore) {
 val backupDir = File(gameDir, "backups").also { it.mkdirs() }
 javaArgs += "-Dmore.backup.dir=${backupDir.absolutePath}"
 envVars["MORE_BACKUP_DIR"] = backupDir.absolutePath
 applied += "Backup/Restore"
 }

 // ── Mod Conflict Detector
 if (moreConfig.enableModConflictDetector) {
 javaArgs += listOf(
 "-Dmore.modconflict.detect=true",
 "-Dmore.modconflict.autodisable=true"
 )
 envVars["MORE_MOD_CONFLICT"] = "1"
 applied += "Mod Conflict Detector"
 }

 // ── Dependency Detector
 if (moreConfig.enableDependencyDetector) {
 javaArgs += listOf(
 "-Dmore.deps.detect=true",
 "-Dmore.deps.autodownload=true"
 )
 envVars["MORE_DEPS_DETECTOR"] = "1"
 applied += "Dependency Detector"
 }

 // ── Resource Packs Manager
 if (moreConfig.enableResourcePacks) {
 val rpDir = File(gameDir, "resourcepacks").also { it.mkdirs() }
 javaArgs += listOf(
 "-Dmore.resourcepacks.enabled=true",
 "-Dmore.resourcepacks.dir=${rpDir.absolutePath}",
 "-Dmore.resourcepacks.auto=true"
 )
 envVars["MORE_RP_DIR"] = rpDir.absolutePath
 envVars["MORE_RP_ENABLED"] = "1"
 applied += "Resource Packs Manager"
 }

 // ── Profiles Manager
 if (moreConfig.enableProfiles) {
 val profilesDir = File(gameDir, "profiles").also { it.mkdirs() }
 javaArgs += "-Dmore.profiles.dir=${profilesDir.absolutePath}"
 envVars["MORE_PROFILES_DIR"] = profilesDir.absolutePath
 applied += "Profiles Manager"
 }

 // ── Battery Saver
 if (moreConfig.enableBatterySaver) {
 val batteryLevel = getBatteryLevel(context)
 val isLowBattery = batteryLevel <= 20

 javaArgs += listOf(
 "-Dmore.battery.saver=true",
 "-Dmore.battery.level=$batteryLevel",
 "-Dmore.battery.low=$isLowBattery"
 )
 envVars["MORE_BATTERY_SAVER"] = "1"
 envVars["MORE_BATTERY_LEVEL"] = batteryLevel.toString()

 if (isLowBattery) {
 javaArgs += listOf(
 "-Dmore.battery.throttle=true",
 "-Dmore.battery.maxfps=30"
 )
 envVars["MORE_BATTERY_THROTTLE"] = "1"
 }
 applied += "Battery Saver (${batteryLevel}%)"
 }

 // ── Thermal Protection
 if (moreConfig.enableThermalProtection) {
 javaArgs += listOf(
 "-Dmore.thermal.protect=true",
 "-Dmore.thermal.autothrottle=true",
 "-Dmore.thermal.maxtemp=75"
 )
 envVars["MORE_THERMAL_PROTECT"] = "1"
 applied += "Thermal Protection"
 }

 // ── Quick Toggles
 if (moreConfig.enableQuickToggles) {
 javaArgs += listOf(
 "-Dmore.quicktoggles.enabled=true",
 "-Dmore.quicktoggles.shader=true",
 "-Dmore.quicktoggles.hud=true",
 "-Dmore.quicktoggles.performance=true",
 "-Dmore.quicktoggles.mods=true"
 )
 envVars["MORE_QUICK_TOGGLES"] = "1"
 applied += "Quick Toggles"
 }

 // ── Performance Monitor
 if (moreConfig.enablePerformanceMonitor) {
 javaArgs += listOf(
 "-Dmore.perfmonitor.enabled=true",
 "-Dmore.perfmonitor.fps=true",
 "-Dmore.perfmonitor.ram=true",
 "-Dmore.perfmonitor.chunks=true",
 "-Dmore.perfmonitor.entities=true",
 "-Dmore.perfmonitor.interval=500"
 )
 envVars["MORE_PERF_MONITOR"] = "1"
 applied += "Performance Monitor"
 }

 // ── Recent Worlds
 if (moreConfig.enableRecentWorlds) {
 val worldsDir = File(gameDir, "saves").also { it.mkdirs() }
 javaArgs += listOf(
 "-Dmore.worlds.recent=true",
 "-Dmore.worlds.dir=${worldsDir.absolutePath}",
 "-Dmore.worlds.max=10"
 )
 envVars["MORE_WORLDS_DIR"] = worldsDir.absolutePath
 applied += "Recent Worlds"
 }

 // ── Favorite Servers
 if (moreConfig.enableFavoriteServers) {
 val serversFile = File(gameDir, "servers.dat")
 javaArgs += listOf(
 "-Dmore.servers.favorites=true",
 "-Dmore.servers.file=${serversFile.absolutePath}",
 "-Dmore.servers.max=20"
 )
 envVars["MORE_SERVERS_FILE"] = serversFile.absolutePath
 applied += "Favorite Servers"
 }

 // ── One Tap Optimize
 if (moreConfig.enableOneTapOptimize) {
 javaArgs += listOf(
 "-Dmore.optimizetap.enabled=true",
 "-Dmore.optimizetap.auto=true",
 "-Dmore.optimizetap.memory=true",
 "-Dmore.optimizetap.render=true",
 "-Dmore.optimizetap.mods=true"
 )
 envVars["MORE_ONE_TAP_OPT"] = "1"
 applied += "One Tap Optimize"
 }

 // ── News Feed
 if (moreConfig.enableNewsFeed) {
 javaArgs += listOf(
 "-Dmore.news.enabled=true",
 "-Dmore.news.autorefresh=true",
 "-Dmore.news.max=5"
 )
 envVars["MORE_NEWS_FEED"] = "1"
 applied += "News Feed"
 }

 // ── Playtime Stats
 if (moreConfig.enablePlaytimeStats) {
 val statsFile = File(gameDir, "playtime_stats.json")
 javaArgs += listOf(
 "-Dmore.playtime.track=true",
 "-Dmore.playtime.file=${statsFile.absolutePath}",
 "-Dmore.playtime.autosave=true"
 )
 envVars["MORE_PLAYTIME_FILE"] = statsFile.absolutePath
 applied += "Playtime Stats"
 }

 // ── Theme System
 if (moreConfig.enableThemeSystem) {
 javaArgs += listOf(
 "-Dmore.theme.enabled=true",
 "-Dmore.theme.options=dark,amoled,glass,gaming,light",
 "-Dmore.theme.default=dark"
 )
 envVars["MORE_THEME_SYSTEM"] = "1"
 applied += "Theme System"
 }

 // ── Screenshot Tools
 if (moreConfig.enableScreenshotTools) {
 val ssDir = File(gameDir, "screenshots").also { it.mkdirs() }
 javaArgs += listOf(
 "-Dmore.screenshot.tools=true",
 "-Dmore.screenshot.hq=true",
 "-Dmore.screenshot.hideui=true",
 "-Dmore.screenshot.dir=${ssDir.absolutePath}",
 "-Dmore.screenshot.format=png"
 )
 envVars["MORE_SS_DIR"] = ssDir.absolutePath
 applied += "Screenshot Tools"
 }

 // ── Compatibility Presets
 if (moreConfig.enableCompatibilityPresets) {
 javaArgs += listOf(
 "-Dmore.presets.enabled=true",
 "-Dmore.presets.list=vanilla,heavymods,shaders,battery,performance"
 )
 envVars["MORE_COMPAT_PRESETS"] = "1"
 applied += "Compatibility Presets"
 }

 _state.value = MoreState(
 isReady = true,
 isPreparing = false,
 applied = applied.distinct()
 )

 MoreRuntimeBundle(
 success = true,
 javaArgs = javaArgs.distinct(),
 envVars = envVars,
 message = "More features ready: ${applied.size} modules"
 )
 } catch (e: Exception) {
 _state.value = MoreState(
 isReady = false,
 isPreparing = false,
 errors = listOf(e.message ?: "More features failed")
 )

 MoreRuntimeBundle(
 success = true,
 javaArgs = emptyList(),
 envVars = emptyMap(),
 message = "More features fallback: ${e.message}"
 )
 }
 }

 fun reset() {
 _state.value = MoreState()
 }

 // ════════════════════════════════════════════════════════════════
 // PLAYTIME STATS
 // ════════════════════════════════════════════════════════════════

 object PlaytimeStats {

 data class Stats(
 val totalPlaytimeMin: Int = 0,
 val sessionsCount: Int = 0,
 val lastSession: String = "",
 val averageSessionMin: Int = 0,
 val perVersion: Map<String, Int> = emptyMap()
 )

 fun recordSession(gameDir: File, versionId: String, durationMin: Int) {
 val file = File(gameDir, "playtime_stats.json")
 val json = if (file.exists()) {
 JSONObject(file.readText())
 } else {
 JSONObject()
 }

 val total = json.optInt("totalPlaytimeMin", 0) + durationMin
 val sessions = json.optInt("sessionsCount", 0) + 1

 json.put("totalPlaytimeMin", total)
 json.put("sessionsCount", sessions)
 json.put("lastSession", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))

 val perVersion = json.optJSONObject("perVersion") ?: JSONObject()
 perVersion.put(versionId, perVersion.optInt(versionId, 0) + durationMin)
 json.put("perVersion", perVersion)

 file.writeText(json.toString(2))
 }

 fun getStats(gameDir: File): Stats {
 val file = File(gameDir, "playtime_stats.json")
 if (!file.exists()) return Stats()

 return try {
 val json = JSONObject(file.readText())
 val total = json.optInt("totalPlaytimeMin", 0)
 val sessions = json.optInt("sessionsCount", 0)

 val perVersion = mutableMapOf<String, Int>()
 json.optJSONObject("perVersion")?.keys()?.forEach { key ->
 perVersion[key] = json.optJSONObject("perVersion").optInt(key, 0)
 }

 Stats(
 totalPlaytimeMin = total,
 sessionsCount = sessions,
 lastSession = json.optString("lastSession", ""),
 averageSessionMin = if (sessions > 0) total / sessions else 0,
 perVersion = perVersion
 )
 } catch (_: Exception) {
 Stats()
 }
 }

 fun formatPlaytime(min: Int): String {
 val hours = min / 60
 val mins = min % 60
 return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
 }
 }

 // ════════════════════════════════════════════════════════════════
 // THEME SYSTEM
 // ════════════════════════════════════════════════════════════════

 object ThemeSystem {

 data class Theme(
 val id: String,
 val name: String,
 val primaryColor: String,
 val secondaryColor: String,
 val backgroundColor: String,
 val textColor: String,
 val accentColor: String,
 val isDark: Boolean
 )

 fun getAllThemes(): List<Theme> {
 return listOf(
 Theme("dark", "Dark", "#1A1A2E", "#16213E", "#0F0F0F", "#E0E0E0", "#E94560", true),
 Theme("amoled", "AMOLED Black", "#000000", "#0D0D0D", "#000000", "#FFFFFF", "#00BCD4", true),
 Theme("glass", "Glass", "#1A1A2E88", "#16213E88", "#0F0F0FAA", "#E0E0E0", "#7B2CBF", true),
 Theme("gaming", "Gaming Neon", "#0F0F0F", "#1A1A2E", "#000000", "#00FF00", "#FF00FF", true),
 Theme("light", "Light", "#FFFFFF", "#F0F0F0", "#FFFFFF", "#1A1A1A", "#0066CC", false)
 )
 }

 fun getTheme(id: String): Theme? {
 return getAllThemes().find { it.id == id }
 }

 fun saveCurrentTheme(gameDir: File, themeId: String) {
 val file = File(gameDir, "theme.txt")
 file.writeText(themeId)
 }

 fun getCurrentTheme(gameDir: File): Theme {
 val file = File(gameDir, "theme.txt")
 val id = if (file.exists()) file.readText().trim() else "dark"
 return getTheme(id) ?: getTheme("dark")!!
 }
 }

 // ════════════════════════════════════════════════════════════════
 // COMPATIBILITY PRESETS
 // ════════════════════════════════════════════════════════════════

 object CompatibilityPresets {

 data class Preset(
 val id: String,
 val name: String,
 val description: String,
 val renderDistance: Int,
 val fov: Int,
 val mipmap: Int,
 val fancyGraphics: Boolean,
 val vsync: Boolean,
 val dynamicFps: Boolean,
 val entityCulling: Boolean,
 val maxFps: Int,
 val shaderQuality: Int,
 val xopLevel: String
 )

 fun getAllPresets(): List<Preset> {
 return listOf(
 Preset(
 id = "vanilla",
 name = "Vanilla Experience",
 description = "Clean vanilla-style gameplay",
 renderDistance = 12,
 fov = 70,
 mipmap = 4,
 fancyGraphics = true,
 vsync = true,
 dynamicFps = true,
 entityCulling = true,
 maxFps = 60,
 shaderQuality = 0,
 xopLevel = "BALANCED"
 ),
 Preset(
 id = "heavymods",
 name = "Heavy Modpack",
 description = "Optimized for large modpacks",
 renderDistance = 10,
 fov = 70,
 mipmap = 3,
 fancyGraphics = true,
 vsync = false,
 dynamicFps = true,
 entityCulling = true,
 maxFps = 60,
 shaderQuality = 0,
 xopLevel = "HEAVY_MOD"
 ),
 Preset(
 id = "shaders",
 name = "Shader Showcase",
 description = "Best visual quality with shaders",
 renderDistance = 14,
 fov = 75,
 mipmap = 4,
 fancyGraphics = true,
 vsync = true,
 dynamicFps = false,
 entityCulling = true,
 maxFps = 60,
 shaderQuality = 3,
 xopLevel = "SHADER_OPTIMIZED"
 ),
 Preset(
 id = "battery",
 name = "Battery Saver",
 description = "Maximum battery life",
 renderDistance = 6,
 fov = 65,
 mipmap = 2,
 fancyGraphics = false,
 vsync = false,
 dynamicFps = true,
 entityCulling = true,
 maxFps = 30,
 shaderQuality = 0,
 xopLevel = "SAFE"
 ),
 Preset(
 id = "performance",
 name = "Maximum Performance",
 description = "Highest possible FPS",
 renderDistance = 8,
 fov = 70,
 mipmap = 2,
 fancyGraphics = false,
 vsync = false,
 dynamicFps = true,
 entityCulling = true,
 maxFps = 120,
 shaderQuality = 0,
 xopLevel = "EXTREME"
 )
 )
 }

 fun getPreset(id: String): Preset? {
 return getAllPresets().find { it.id == id }
 }

 fun applyPreset(gameDir: File, preset: Preset): Boolean {
 return try {
 val file = File(gameDir, "options.txt")
 val options = if (file.exists()) file.readText() else ""

 val updated = StringBuilder(options)
 val replacements = mapOf(
 "renderDistance" to preset.renderDistance.toString(),
 "fov" to preset.fov.toString(),
 "mipmapLevels" to preset.mipmap.toString(),
 "fancyGraphics" to preset.fancyGraphics.toString(),
 "enableVsync" to preset.vsync.toString(),
 "maxFramerate" to preset.maxFps.toString()
 )

 replacements.forEach { (key, value) ->
 val regex = Regex("$key:([^\
]+)")
 if (regex.containsMatchIn(options)) {
 updated.replace(regex.find(options)!!.range, "$key:$value")
 } else {
 updated.append("$key:$value
")
 }
 }

 file.writeText(updated.toString())
 true
 } catch (_: Exception) {
 false
 }
 }
 }

 // ════════════════════════════════════════════════════════════════
 // HELPER
 // ════════════════════════════════════════════════════════════════

 internal fun getBatteryLevel(context: Context): Int {
 return try {
 val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
 bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
 } catch (_: Exception) {
 100
   }
  }
 }
                
