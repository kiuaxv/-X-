package com.x.launcher.xop

import android.content.Context
import com.x.launcher.core.LaunchConfig
import com.x.launcher.runtime.Gl4esManager
import com.x.launcher.runtime.LibraryDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class XopLaunchResult(
 val success: Boolean,
 val javaArgs: List<String> = emptyList(),
 val envVars: Map<String, String> = emptyMap(),
 val minMemoryMb: Int = 512,
 val maxMemoryMb: Int = 1024,
 val nativesPath: String = "",
 val shaderCompat: Boolean = false,
 val heavyModMode: Boolean = false,
 val message: String = ""
)

object XopManager {

 private val _state = MutableStateFlow(XopState())
 val state: StateFlow<XopState> = _state

 private var deviceInfo: DeviceInfo? = null
 private var profile: XopProfile? = null

 suspend fun prepare(
 context: Context,
 config: XopConfig,
 gameDir: File
 ): XopLaunchResult = withContext(Dispatchers.IO) {
 _state.value = XopState(isPreparing = true)

 try {
 // 1. Analyze device
 _state.value = _state.value.copy(
 isPreparing = true,
 optimizationsApplied = listOf("Analyzing device...")
 )

 val info = XopDeviceAnalyzer.analyze(context)
 deviceInfo = info

 if (!XopDeviceAnalyzer.isDeviceCompatible(info)) {
 _state.value = _state.value.copy(
 isPreparing = false,
 fallbackMode = true,
 errors = listOf("Device may not be compatible")
 )
 return@withContext XopLaunchResult(
 success = false,
 message = "Device not compatible"
 )
 }

 // 2. Choose optimization level
 val level = if (config.autoDetect) {
 info.recommendedLevel
 } else {
 config.optimizationLevel
 }

 // 3. Apply heavy mod / shader overrides
 val finalLevel = when {
 config.heavyModSupport && XopDeviceAnalyzer.canRunHeavyMods(info) ->
 OptimizationLevel.HEAVY_MOD
 config.shaderCompatibility && XopDeviceAnalyzer.canRunShaders(info) ->
 OptimizationLevel.SHADER_OPTIMIZED
 config.safeMode -> OptimizationLevel.SAFE
 else -> level
 }

 // 4. Build profile
 profile = XopProfile.forLevel(finalLevel, info.ramMb, info.cpuCores)

 // 5. Update state
 _state.value = _state.value.copy(
 isPreparing = true,
 profile = profile,
 activeLevel = finalLevel,
 deviceRamMb = info.ramMb,
 cpuCores = info.cpuCores,
 abi = info.abi,
 androidVersion = info.androidVersion,
 optimizationsApplied = mutableListOf("Device analyzed: ${info.ramMb}MB RAM, ${info.cpuCores} cores")
 )

 // 6. Apply memory overrides
 val effectiveProfile = if (config.customMemoryMb > 0) {
 profile!!.copy(
 xmx = minOf(config.customMemoryMb, info.ramMb - 512),
 xms = minOf(config.customMemoryMb / 2, profile!!.xms)
 )
 } else {
 profile!!
 }

 // 7. Build library preload list
 val preloadList = mutableListOf<String>()
 if (config.libraryPreload) {
 preloadList.addAll(effectiveProfile.preloadLibs)
 }

 // 8. Prepare environment variables
 val gl4esEnv = Gl4esManager.setupEnvironment(gameDir)
 val xopEnv = effectiveProfile.toEnvVars().toMutableMap()
 xopEnv.putAll(gl4esEnv)

 if (config.shaderCompatibility) {
 xopEnv["LIBGL_SHADERS"] = "1"
 xopEnv["XOP_SHADER_COMPAT"] = "1"
 }

 if (config.textureStreaming) {
 xopEnv["XOP_TEXTURE_STREAM"] = "1"
 }

 // 9. Build Java args
 val javaArgs = effectiveProfile.toJavaArgs().toMutableList()

 if (config.fastLaunch) {
 javaArgs.add("-Dxop.fastlaunch=true")
 }

 if (config.renderOptimization) {
 javaArgs.add("-Dxop.render.optimize=true")
 }

 if (config.debugMode) {
 javaArgs.add("-Dxop.debug=true")
 javaArgs.add("-XX:+PrintGC")
 }

 // 10. Get natives path
 val nativesPath = LibraryDownloader.getNativesPath(gameDir)

 // 11. Finalize state
 _state.value = _state.value.copy(
 isPreparing = false,
 isReady = true,
 profile = effectiveProfile,
 heavyModMode = finalLevel == OptimizationLevel.HEAVY_MOD,
 shaderCompatEnabled = config.shaderCompatibility,
 optimizationsApplied = _state.value.optimizationsApplied + listOf(
 "Profile: ${effectiveProfile.name}",
 "Memory: ${effectiveProfile.xms}m - ${effectiveProfile.xmx}m",
 "GC: ${effectiveProfile.gcType}",
 "Threads: ${effectiveProfile.threads}",
 "Shader compat: ${config.shaderCompatibility}",
 "Heavy mod: ${finalLevel == OptimizationLevel.HEAVY_MOD}"
 )
 )

 XopLaunchResult(
 success = true,
 javaArgs = javaArgs,
 envVars = xopEnv,
 minMemoryMb = effectiveProfile.minMemoryMb,
 maxMemoryMb = effectiveProfile.maxMemoryMb,
 nativesPath = nativesPath,
 shaderCompat = config.shaderCompatibility,
 heavyModMode = finalLevel == OptimizationLevel.HEAVY_MOD,
 message = "XOP ready: ${effectiveProfile.name}"
 )
 } catch (e: Exception) {
 _state.value = _state.value.copy(
 isPreparing = false,
 isReady = false,
 fallbackMode = true,
 errors = listOf(e.message ?: "XOP preparation failed")
 )

 // Fallback to safe defaults
 val safeProfile = XopProfile.forLevel(OptimizationLevel.SAFE, 2048, 4)

 XopLaunchResult(
 success = true,
 javaArgs = safeProfile.toJavaArgs(),
 envVars = safeProfile.toEnvVars(),
 minMemoryMb = 384,
 maxMemoryMb = 512,
 nativesPath = LibraryDownloader.getNativesPath(gameDir),
 shaderCompat = false,
 heavyModMode = false,
 message = "XOP fallback: ${e.message}"
 )
 }
 }

 fun getDeviceInfo(): DeviceInfo? = deviceInfo
 fun getProfile(): XopProfile? = profile

 fun reset() {
 _state.value = XopState()
 deviceInfo = null
 profile = null
 }
}
