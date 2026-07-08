package com.x.launcher.xop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class OptimizationLevel {
 SAFE,
 BALANCED,
 EXTREME,
 HEAVY_MOD,
 SHADER_OPTIMIZED
}

data class XopConfig(
 var enabled: Boolean = true,
 var optimizationLevel: OptimizationLevel = OptimizationLevel.BALANCED,
 var fastLaunch: Boolean = true,
 var heavyModSupport: Boolean = false,
 var shaderCompatibility: Boolean = true,
 var smartMemoryTuning: Boolean = true,
 var renderOptimization: Boolean = true,
 var safeMode: Boolean = false,
 var libraryPreload: Boolean = true,
 var textureStreaming: Boolean = true,
 var modCompatScan: Boolean = true,
 var autoDetect: Boolean = true,
 var customMemoryMb: Int = 0,
 var customThreads: Int = 0,
 var debugMode: Boolean = false
) {
 companion object {
 fun fromLevel(level: OptimizationLevel): XopConfig {
 return when (level) {
 OptimizationLevel.SAFE -> XopConfig(
 optimizationLevel = level,
 fastLaunch = false,
 heavyModSupport = false,
 shaderCompatibility = false,
 smartMemoryTuning = true,
 renderOptimization = false,
 safeMode = true,
 libraryPreload = false,
 textureStreaming = false,
 modCompatScan = false
 )
 OptimizationLevel.BALANCED -> XopConfig(
 optimizationLevel = level,
 fastLaunch = true,
 heavyModSupport = false,
 shaderCompatibility = true,
 smartMemoryTuning = true,
 renderOptimization = true,
 libraryPreload = true,
 textureStreaming = true,
 modCompatScan = true
 )
 OptimizationLevel.EXTREME -> XopConfig(
 optimizationLevel = level,
 fastLaunch = true,
 heavyModSupport = false,
 shaderCompatibility = true,
 smartMemoryTuning = true,
 renderOptimization = true,
 libraryPreload = true,
 textureStreaming = true,
 modCompatScan = true,
 customMemoryMb = 0
 )
 OptimizationLevel.HEAVY_MOD -> XopConfig(
 optimizationLevel = level,
 fastLaunch = true,
 heavyModSupport = true,
 shaderCompatibility = true,
 smartMemoryTuning = true,
 renderOptimization = true,
 libraryPreload = true,
 textureStreaming = true,
 modCompatScan = true
 )
 OptimizationLevel.SHADER_OPTIMIZED -> XopConfig(
 optimizationLevel = level,
 fastLaunch = true,
 heavyModSupport = true,
 shaderCompatibility = true,
 smartMemoryTuning = true,
 renderOptimization = true,
 libraryPreload = true,
 textureStreaming = true,
 modCompatScan = true
 )
 }
 }
 }
}

object XopConfigHolder {
 private val _config = MutableStateFlow(XopConfig())
 val config: StateFlow<XopConfig> = _config

 fun update(newConfig: XopConfig) {
 _config.value = newConfig
 }

 fun toggle() {
 _config.value = _config.value.copy(enabled = !_config.value.enabled)
 }

 fun setLevel(level: OptimizationLevel) {
 _config.value = XopConfig.fromLevel(level)
 }
}
