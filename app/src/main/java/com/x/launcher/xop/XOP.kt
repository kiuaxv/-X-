package com.x.launcher.xop
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import com.x.launcher.runtime.Gl4esManager
import com.x.launcher.runtime.LibraryDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
object XOP {
enum class Level {
 SAFE,
 BALANCED,
 EXTREME,
 HEAVY_MOD,
 SHADER_OPTIMIZED
 }
data class Config(
 val enabled: Boolean = true,
 val level: Level = Level.BALANCED,
 val autoDetect: Boolean = true,
 val safeMode: Boolean = false,
 val fastLaunch: Boolean = true,
 val heavyModSupport: Boolean = false,
 val shaderCompatibility: Boolean = true,
 val smartMemoryTuning: Boolean = true,
 val renderOptimization: Boolean = true,
 val libraryPreload: Boolean = true,
 val textureStreaming: Boolean = true,
 val modCompatScan: Boolean = true,
 val customMemoryMb: Int = 0,
 val customThreads: Int = 0,
 val debugMode: Boolean = false
 )
data class DeviceInfo(
 val ramMb: Int,
 val cpuCores: Int,
 val abi: String,
 val androidVersion: String,
 val sdkLevel: Int,
 val availableStorageMb: Int,
 val manufacturer: String,
 val model: String,
 val isLowRam: Boolean,
 val isHighPerf: Boolean,
 val recommendedLevel: Level
 )
data class Profile(
 val name: String,
 val level: Level,
 val minMemoryMb: Int,
 val maxMemoryMb: Int,
 val xms: Int,
 val xmx: Int,
 val threads: Int,
 val gcFlags: List<String>,
 val glEnv: Map<String, String>,
 val featureArgs: List<String>,
 val preloadLibs: List<String>,
 val cacheSizeMb: Int,
 val description: String
 ) {
 fun toJavaArgs(): List<String> {
 val args = mutableListOf<String>()
 args += "-Xms${xms}m"
 args += "-Xmx${xmx}m"
 args += gcFlags
 args += featureArgs
 if (preloadLibs.isNotEmpty()) {
 args += "-Dxop.preload=${preloadLibs.joinToString(",")}"
 }
 return args
 }
fun toEnv(): Map<String, String> {
 return glEnv.toMutableMap().apply {
 put("XOP_LEVEL", level.name)
 put("XOP_CACHE_MB", cacheSizeMb.toString())
 }
 }
 }
data class State(
 val isPreparing: Boolean = false,
 val isReady: Boolean = false,
 val fallbackMode: Boolean = false,
 val activeLevel: Level = Level.BALANCED,
 val deviceInfo: DeviceInfo? = null,
 val profile: Profile? = null,
 val warnings: List<String> = emptyList(),
 val errors: List<String> = emptyList(),
 val applied: List<String> = emptyList()
 )
data class RuntimeBundle(
 val success: Boolean,
 val javaArgs: List<String>,
 val envVars: Map<String, String>,
 val minMemoryMb: Int,
 val maxMemoryMb: Int,
 val nativesPath: String,
 val heavyModMode: Boolean,
 val shaderCompatEnabled: Boolean,
 val clientOptionsJson: JSONObject,
 val message: String
 )
data class ClientOption(
 val key: String,
 val title: String,
 val description: String,
 val type: String,
 val defaultValue: String
 )
data class ModScanResult(
 val heavyModsDetected: Boolean = false,
 val shaderModsDetected: Boolean = false,
 val warnings: List<String> = emptyList(),
 val extraArgs: List<String> = emptyList(),
 val recommendedLevel: Level? = null
 )
private val _state = MutableStateFlow(State())
 val state: StateFlow<State> = _state
private val digestCache = ConcurrentHashMap<String, String>()
 private val classpathCache = ConcurrentHashMap<String, List<String>>()
suspend fun prepareForRuntime(
 context: Context,
 gameDir: File,
 config: Config = Config()
 ): RuntimeBundle = withContext(Dispatchers.IO) {
 _state.value = State(isPreparing = true)
if (!config.enabled) {
 val env = Gl4esManager.setupEnvironment(gameDir)
 val bundle = RuntimeBundle(
 success = true,
 javaArgs = emptyList(),
 envVars = env,
 minMemoryMb = 512,
 maxMemoryMb = 1024,
 nativesPath = LibraryDownloader.getNativesPath(gameDir),
 heavyModMode = false,
 shaderCompatEnabled = false,
 clientOptionsJson = buildClientOptionsJson(config),
 message = "XOP disabled"
 )
 _state.value = State(
 isPreparing = false,
 isReady = true,
 activeLevel = config.level,
 warnings = emptyList(),
 applied = listOf("XOP disabled")
 )
 return@withContext bundle
 }
try {
 val device = analyzeDevice(context)
if (!isCompatible(device)) {
 _state.value = State(
 isPreparing = false,
 isReady = false,
 fallbackMode = true,
 deviceInfo = device,
 errors = listOf("Device not compatible enough for XOP")
 )
 return@withContext buildSafeFallback(gameDir, "Device not compatible enough for XOP")
 }
val modScan = if (config.modCompatScan) scanMods(gameDir) else ModScanResult()
val chosenLevel = chooseLevel(config, device, modScan)
 val baseProfile = buildProfile(chosenLevel, device.ramMb, device.cpuCores, config.customThreads)
 val finalProfile = applyMemoryOverride(baseProfile, config, device)
val javaArgs = mutableListOf<String>()
 val envVars = mutableMapOf<String, String>()
 val warnings = mutableListOf<String>()
 val applied = mutableListOf<String>()
javaArgs += finalProfile.toJavaArgs()
 envVars.putAll(finalProfile.toEnv())
val glEnv = Gl4esManager.setupEnvironment(gameDir)
 envVars.putAll(glEnv)
 applied += "GL environment prepared"
if (config.fastLaunch) {
 javaArgs += listOf(
 "-Dxop.fastlaunch=true",
 "-Dxop.parallel.init=true",
 "-Dxop.skip.redundant.scan=true"
 )
 applied += "Fast launch"
 }
if (config.libraryPreload) {
 javaArgs += "-Dxop.preload.enabled=true"
 applied += "Library preload"
 }
if (config.textureStreaming) {
 javaArgs += "-Dxop.texture.streaming=true"
 envVars["XOP_TEXTURE_STREAM"] = "1"
 applied += "Texture streaming"
 }
if (config.smartMemoryTuning) {
 javaArgs += listOf(
 "-XX:+UseStringDeduplication",
 "-XX:+DisableExplicitGC",
 "-XX:InitiatingHeapOccupancyPercent=35"
 )
 applied += "Smart memory tuning"
 }
if (config.renderOptimization) {
 javaArgs += listOf(
 "-Dxop.render.optimize=true",
 "-Dxop.render.pipeline=gl4es",
 "-Dxop.frame.pacing=stable"
 )
 envVars["LIBGL_VSYNC"] = "1"
 envVars["LIBGL_MIPMAP"] = "3"
 envVars["LIBGL_NORMALIZE"] = "1"
 applied += "Render optimization"
 }
if (config.shaderCompatibility || modScan.shaderModsDetected || chosenLevel == Level.SHADER_OPTIMIZED) {
 val shaderDir = File(gameDir, "shaderpacks").also { it.mkdirs() }
 javaArgs += listOf(
 "-Dxop.shader.compat=true",
 "-Dxop.shader.pipeline=enabled",
 "-Dxop.shader.dir=${shaderDir.absolutePath}",
 "-Dxop.option.shaders=true"
 )
 envVars["LIBGL_SHADERS"] = "1"
 envVars["XOP_SHADER_COMPAT"] = "1"
 envVars["XOP_SHADERPACK_DIR"] = shaderDir.absolutePath
 applied += "Shader compatibility"
 }
if (config.heavyModSupport || modScan.heavyModsDetected || chosenLevel == Level.HEAVY_MOD) {
 javaArgs += listOf(
 "-Dxop.modcompat.heavy=true",
 "-Dxop.modcompat.fallback=true",
 "-Dxop.memory.heavymod=true"
 )
 applied += "Heavy mod compatibility"
 }
javaArgs += modScan.extraArgs
 warnings += modScan.warnings
val cacheDir = File(gameDir, "xop/cache").also { it.mkdirs() }
 envVars["XOP_CACHE_DIR"] = cacheDir.absolutePath
 envVars["XOP_CACHE_MB"] = finalProfile.cacheSizeMb.toString()
 applied += "XOP cache prepared"
if (config.debugMode) {
 javaArgs += listOf(
 "-Dxop.debug=true",
 "-XX:+PrintGC"
 )
 applied += "Debug mode"
 }
val clientOptions = buildClientOptionsJson(config)
_state.value = State(
 isPreparing = false,
 isReady = true,
 fallbackMode = false,
 activeLevel = chosenLevel,
 deviceInfo = device,
 profile = finalProfile,
 warnings = warnings.distinct(),
 errors = emptyList(),
 applied = applied.distinct()
 )
RuntimeBundle(
 success = true,
 javaArgs = javaArgs.distinct(),
 envVars = envVars,
 minMemoryMb = finalProfile.minMemoryMb,
 maxMemoryMb = finalProfile.maxMemoryMb,
 nativesPath = LibraryDownloader.getNativesPath(gameDir),
 heavyModMode = chosenLevel == Level.HEAVY_MOD || modScan.heavyModsDetected,
 shaderCompatEnabled = chosenLevel == Level.SHADER_OPTIMIZED || config.shaderCompatibility || modScan.shaderModsDetected,
 clientOptionsJson = clientOptions,
 message = "XOP ready: ${finalProfile.name}"
 )
 } catch (e: Exception) {
 _state.value = State(
 isPreparing = false,
 isReady = false,
 fallbackMode = true,
 errors = listOf(e.message ?: "XOP failed")
 )
 buildSafeFallback(gameDir, e.message ?: "XOP failed")
 }
 }
fun reset() {
 _state.value = State()
 }
fun buildClasspathCache(librariesDir: File): List<String> {
 val key = librariesDir.absolutePath
 classpathCache[key]?.let { return it }
val jars = librariesDir.walkTopDown()
 .filter { it.isFile && it.extension == "jar" }
 .map { it.absolutePath }
 .sorted()
 .toList()
classpathCache[key] = jars
 return jars
 }
fun fingerprint(file: File): String {
 val key = "${file.absolutePath}:${file.lastModified()}:${file.length()}"
 digestCache[key]?.let { return it }
val digest = MessageDigest.getInstance("SHA-1")
 val hash = file.inputStream().use { input ->
 val buffer = ByteArray(8192)
 var read: Int
 while (input.read(buffer).also { read = it } != -1) {
 digest.update(buffer, 0, read)
 }
 digest.digest().joinToString("") { "%02x".format(it) }
 }
digestCache[key] = hash
 return hash
 }
fun buildClientOptions(config: Config): List<ClientOption> {
 return listOf(
 ClientOption(
 key = "client_section",
 title = "Client",
 description = "Built-in client settings",
 type = "section",
 defaultValue = "true"
 ),
 ClientOption(
 key = "client_shaders",
 title = "Shaders",
 description = "Enable shader compatibility and shaderpack support",
 type = "toggle",
 defaultValue = config.shaderCompatibility.toString()
 ),
 ClientOption(
 key = "client_heavy_mod_support",
 title = "Heavy Mod Support",
 description = "Improve stability for desktop-style heavy mods",
 type = "toggle",
 defaultValue = config.heavyModSupport.toString()
 ),
 ClientOption(
 key = "client_render_optimization",
 title = "Render Optimization",
 description = "Smoother rendering and frame pacing",
 type = "toggle",
 defaultValue = config.renderOptimization.toString()
 ),
 ClientOption(
 key = "client_fast_launch",
 title = "Fast Launch",
 description = "Reduce startup time by preparing core resources early",
 type = "toggle",
 defaultValue = config.fastLaunch.toString()
 )
 )
 }
fun buildClientOptionsJson(config: Config): JSONObject {
 val array = JSONArray()
 buildClientOptions(config).forEach { option ->
 array.put(
 JSONObject().apply {
 put("key", option.key)
 put("title", option.title)
 put("description", option.description)
 put("type", option.type)
 put("defaultValue", option.defaultValue)
 }
 )
 }
return JSONObject().apply {
 put("section", "client")
 put("items", array)
 }
 }
private fun chooseLevel(
 config: Config,
 device: DeviceInfo,
 modScan: ModScanResult
 ): Level {
 if (config.safeMode) return Level.SAFE
 if (config.heavyModSupport && canRunHeavyMods(device)) return Level.HEAVY_MOD
 if (config.shaderCompatibility && canRunShaders(device)) return Level.SHADER_OPTIMIZED
 if (modScan.recommendedLevel != null) return modScan.recommendedLevel
 return if (config.autoDetect) device.recommendedLevel else config.level
 }
private fun applyMemoryOverride(
 profile: Profile,
 config: Config,
 device: DeviceInfo
 ): Profile {
 if (config.customMemoryMb <= 0) return profile
val xmx = minOf(config.customMemoryMb, maxOf(512, device.ramMb - 512))
 val xms = minOf(maxOf(256, xmx / 2), xmx)
return profile.copy(
 xmx = xmx,
 xms = xms
 )
 }
private fun buildSafeFallback(gameDir: File, reason: String): RuntimeBundle {
 val safe = buildProfile(Level.SAFE, 2048, 4, 0)
 val env = safe.toEnv().toMutableMap()
 env.putAll(Gl4esManager.setupEnvironment(gameDir))
return RuntimeBundle(
 success = true,
 javaArgs = safe.toJavaArgs(),
 envVars = env,
 minMemoryMb = safe.minMemoryMb,
 maxMemoryMb = safe.maxMemoryMb,
 nativesPath = LibraryDownloader.getNativesPath(gameDir),
 heavyModMode = false,
 shaderCompatEnabled = false,
 clientOptionsJson = buildClientOptionsJson(Config(level = Level.SAFE)),
 message = "XOP fallback: $reason"
 )
 }
private fun buildProfile(
 level: Level,
 ramMb: Int,
 cpuCores: Int,
 customThreads: Int
 ): Profile {
 val threads = if (customThreads > 0) customThreads else cpuCores
return when (level) {
 Level.SAFE -> Profile(
 name = "Safe",
 level = level,
 minMemoryMb = 384,
 maxMemoryMb = 512,
 xms = 128,
 xmx = minOf(512, (ramMb * 0.25f).toInt()),
 threads = minOf(2, threads),
 gcFlags = listOf(
 "-XX:+UseSerialGC",
 "-XX:MaxGCPauseMillis=50"
 ),
 glEnv = mapOf(
 "LIBGL_FB" to "3",
 "LIBGL_ES" to "2",
 "LIBGL_BATCH" to "0"
 ),
 featureArgs = emptyList(),
 preloadLibs = emptyList(),
 cacheSizeMb = 32,
 description = "Stability-first profile"
 )
Level.BALANCED -> Profile(
 name = "Balanced",
 level = level,
 minMemoryMb = 512,
 maxMemoryMb = 1024,
 xms = 256,
 xmx = minOf(1024, (ramMb * 0.40f).toInt()),
 threads = minOf(4, threads),
 gcFlags = listOf(
 "-XX:+UseG1GC",
 "-XX:MaxGCPauseMillis=50",
 "-XX:G1HeapRegionSize=16m",
 "-XX:+OptimizeStringConcat"
 ),
 glEnv = mapOf(
 "LIBGL_FB" to "3",
 "LIBGL_ES" to "3",
 "LIBGL_BATCH" to "1",
 "LIBGL_RECYCLE" to "1",
 "LIBGL_SILENTSTUB" to "1"
 ),
 featureArgs = listOf(
 "-Dxop.render.smooth=true",
 "-Dxop.shader.compat=true"
 ),
 preloadLibs = listOf("lwjgl", "lwjgl_opengl", "lwjgl_glfw", "gson"),
 cacheSizeMb = 64,
 description = "Balanced performance and stability"
 )
Level.EXTREME -> Profile(
 name = "Extreme",
 level = level,
 minMemoryMb = 768,
 maxMemoryMb = 1536,
 xms = 512,
 xmx = minOf(1536, (ramMb * 0.50f).toInt()),
 threads = minOf(6, threads),
 gcFlags = listOf(
 "-XX:+UseG1GC",
 "-XX:MaxGCPauseMillis=20",
 "-XX:G1HeapRegionSize=32m",
 "-XX:+OptimizeStringConcat",
 "-XX:ParallelGCThreads=${minOf(4, threads)}",
 "-XX:ConcGCThreads=${minOf(2, threads)}"
 ),
 glEnv = mapOf(
 "LIBGL_FB" to "3",
 "LIBGL_ES" to "3",
 "LIBGL_BATCH" to "1",
 "LIBGL_RECYCLE" to "1",
 "LIBGL_SILENTSTUB" to "1",
 "LIBGL_FASTMATH" to "1"
 ),
 featureArgs = listOf(
 "-Dxop.render.smooth=true",
 "-Dxop.render.batch=true",
 "-Dxop.render.cull=true",
 "-Dxop.shader.cache=true"
 ),
 preloadLibs = listOf("lwjgl", "lwjgl_opengl", "lwjgl_glfw", "lwjgl_stb", "gson", "guava", "netty"),
 cacheSizeMb = 128,
 description = "Maximum performance profile"
 )
Level.HEAVY_MOD -> Profile(
 name = "Heavy Mod",
 level = level,
 minMemoryMb = 1024,
 maxMemoryMb = 2048,
 xms = 512,
 xmx = minOf(2048, (ramMb * 0.60f).toInt()),
 threads = minOf(6, threads),
 gcFlags = listOf(
 "-XX:+UseG1GC",
 "-XX:MaxGCPauseMillis=30",
 "-XX:G1HeapRegionSize=32m",
 "-XX:+UseStringDeduplication",
 "-XX:ParallelGCThreads=${minOf(4, threads)}",
 "-XX:ConcGCThreads=${minOf(2, threads)}"
 ),
 glEnv = mapOf(
 "LIBGL_FB" to "3",
 "LIBGL_ES" to "3",
 "LIBGL_BATCH" to "1",
 "LIBGL_RECYCLE" to "1",
 "LIBGL_SILENTSTUB" to "1"
 ),
 featureArgs = listOf(
 "-Dxop.render.smooth=true",
 "-Dxop.render.batch=true",
 "-Dxop.modcompat.heavy=true",
 "-Dxop.modcompat.scan=true",
 "-Dxop.modcompat.fallback=true"
 ),
 preloadLibs = listOf("lwjgl", "lwjgl_opengl", "lwjgl_glfw", "lwjgl_stb", "gson", "guava", "netty", "asm", "log4j"),
 cacheSizeMb = 128,
 description = "Optimized for heavy mods"
 )
Level.SHADER_OPTIMIZED -> Profile(
 name = "Shader Optimized",
 level = level,
 minMemoryMb = 1024,
 maxMemoryMb = 2048,
 xms = 512,
 xmx = minOf(2048, (ramMb * 0.55f).toInt()),
 threads = minOf(6, threads),
 gcFlags = listOf(
 "-XX:+UseG1GC",
 "-XX:MaxGCPauseMillis=25",
 "-XX:G1HeapRegionSize=32m",
 "-XX:ParallelGCThreads=${minOf(4, threads)}"
 ),
 glEnv = mapOf(
 "LIBGL_FB" to "3",
 "LIBGL_ES" to "3",
 "LIBGL_BATCH" to "1",
 "LIBGL_RECYCLE" to "1",
 "LIBGL_SILENTSTUB" to "1",
 "LIBGL_SHADERS" to "1"
 ),
 featureArgs = listOf(
 "-Dxop.render.smooth=true",
 "-Dxop.render.shader=true",
 "-Dxop.shader.compat=true",
 "-Dxop.shader.cache=true",
 "-Dxop.shader.stream=true"
 ),
 preloadLibs = listOf("lwjgl", "lwjgl_opengl", "lwjgl_glfw", "lwjgl_stb", "gson", "guava"),
 cacheSizeMb = 128,
 description = "Optimized for shaders"
 )
 }
 }
private fun analyzeDevice(context: Context): DeviceInfo {
 val ramMb = try {
 val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
 val info = ActivityManager.MemoryInfo()
 am.getMemoryInfo(info)
 (info.totalMem / 1048576L).toInt()
 } catch (_: Exception) {
 2048
 }
val cpuCores = try {
 val cpuInfo = File("/proc/cpuinfo")
 if (cpuInfo.exists()) {
 cpuInfo.useLines { lines ->
 lines.count { it.startsWith("processor") }.coerceAtLeast(1)
 }
 } else {
 Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
 }
 } catch (_: Exception) {
 Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
 }
val abi = try {
 val abis = Build.SUPPORTED_ABIS
 when {
 abis.contains("arm64-v8a") -> "arm64-v8a"
 abis.contains("armeabi-v7a") -> "armeabi-v7a"
 abis.contains("x86_64") -> "x86_64"
 else -> "arm64-v8a"
 }
 } catch (_: Exception) {
 "arm64-v8a"
 }
val storageMb = try {
 val stat = StatFs(context.filesDir.absolutePath)
 (stat.availableBytes / 1048576L).toInt()
 } catch (_: Exception) {
 1024
 }
val isLowRam = ramMb <= 2048
 val isHighPerf = ramMb >= 6144 && cpuCores >= 6
val recommended = when {
 isLowRam -> Level.SAFE
 ramMb >= 6144 && cpuCores >= 6 -> Level.EXTREME
 ramMb >= 4096 && cpuCores >= 4 -> Level.BALANCED
 else -> Level.SAFE
 }
return DeviceInfo(
 ramMb = ramMb,
 cpuCores = cpuCores,
 abi = abi,
 androidVersion = Build.VERSION.RELEASE ?: "unknown",
 sdkLevel = Build.VERSION.SDK_INT,
 availableStorageMb = storageMb,
 manufacturer = Build.MANUFACTURER ?: "unknown",
 model = Build.MODEL ?: "unknown",
 isLowRam = isLowRam,
 isHighPerf = isHighPerf,
 recommendedLevel = recommended
 )
 }
private fun isCompatible(info: DeviceInfo): Boolean {
 if (info.sdkLevel < 26) return false
 if (info.ramMb < 1500) return false
 if (!info.abi.contains("arm") && !info.abi.contains("x86")) return false
 return true
 }
private fun canRunHeavyMods(info: DeviceInfo): Boolean {
 return info.ramMb >= 4096 && info.cpuCores >= 4
 }
private fun canRunShaders(info: DeviceInfo): Boolean {
 return info.ramMb >= 3072 && info.cpuCores >= 4
 }
private fun scanMods(gameDir: File): ModScanResult {
 val modsDir = File(gameDir, "mods")
 if (!modsDir.exists() || !modsDir.isDirectory) return ModScanResult()
val names = modsDir.listFiles()
 ?.filter { it.isFile && (it.extension == "jar" || it.extension == "zip") }
 ?.map { it.name.lowercase() }
 .orEmpty()
val heavyHints = listOf(
 "sodium",
 "iris",
 "optifine",
 "create",
 "immersiveengineering",
 "mekanism",
 "biomesoplenty",
 "forge",
 "fabric-api",
 "allthemods"
 )
val shaderHints = listOf(
 "shader",
 "iris",
 "oculus",
 "optifine"
 )
val riskyHints = listOf(
 "vivecraft",
 "raytracing",
 "distant_horizons"
 )
val heavy = names.any { name -> heavyHints.any { hint -> name.contains(hint) } }
 val shader = names.any { name -> shaderHints.any { hint -> name.contains(hint) } }
 val risky = names.any { name -> riskyHints.any { hint -> name.contains(hint) } }
val warnings = mutableListOf<String>()
 val args = mutableListOf<String>()
 var recommended: Level? = null
if (heavy) {
 recommended = Level.HEAVY_MOD
 warnings += "Heavy mods detected"
 args += "-Dxop.modcompat.heavy=true"
 }
if (shader) {
 recommended = Level.SHADER_OPTIMIZED
 warnings += "Shader-related mods detected"
 args += "-Dxop.modcompat.shader=true"
 }
if (risky) {
 warnings += "Some mods may need compatibility fallback"
 args += "-Dxop.modcompat.fallback=true"
 }
return ModScanResult(
 heavyModsDetected = heavy,
 shaderModsDetected = shader,
 warnings = warnings,
 extraArgs = args,
 recommendedLevel = recommended
 )
 }
}
