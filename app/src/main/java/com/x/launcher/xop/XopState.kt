package com.x.launcher.xop

data class XopState(
 val isReady: Boolean = false,
 val isPreparing: Boolean = false,
 val profile: XopProfile? = null,
 val activeLevel: OptimizationLevel = OptimizationLevel.BALANCED,
 val fallbackMode: Boolean = false,
 val heavyModMode: Boolean = false,
 val shaderCompatEnabled: Boolean = false,
 val deviceRamMb: Int = 0,
 val cpuCores: Int = 0,
 val abi: String = "",
 val androidVersion: String = "",
 val errors: List<String> = emptyList(),
 val warnings: List<String> = emptyList(),
 val optimizationsApplied: List<String> = emptyList()
)
