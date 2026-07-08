package com.x.launcher.xop

data class XopProfile(
 val name: String,
 val level: OptimizationLevel,
 val minMemoryMb: Int,
 val maxMemoryMb: Int,
 val xms: Int,
 val xmx: Int,
 val threads: Int,
 val gcType: String,
 val gcFlags: List<String>,
 val gl4esFlags: Map<String, String>,
 val renderFlags: List<String>,
 val modCompatFlags: List<String>,
 val shaderFlags: List<String>,
 val preloadLibs: List<String>,
 val cacheSizeMb: Int,
 val description: String
) {
 companion object {
 fun forLevel(level: OptimizationLevel, deviceRamMb: Int, cpuCores: Int): XopProfile {
 return when (level) {
 OptimizationLevel.SAFE -> XopProfile(
 name = "Safe",
 level = level,
 minMemoryMb = 384,
 maxMemoryMb = 512,
 xms = 128,
 xmx = minOf(512, (deviceRamMb * 0.25).toInt()),
 threads = minOf(2, cpuCores),
 gcType = "-XX:+UseSerialGC",
 gcFlags = listOf("-XX:+UseSerialGC", "-XX:MaxGCPauseMillis=50"),
 gl4esFlags = mapOf(
 "LIBGL_FB" to "3",
 "LIBGL_ES" to "2",
 "LIBGL_BATCH" to "0"
 ),
 renderFlags = listOf(),
 modCompatFlags = listOf(),
 shaderFlags = listOf(),
 preloadLibs = listOf(),
 cacheSizeMb = 32,
 description = "Stability-first, minimal optimizations"
 )

 OptimizationLevel.BALANCED -> XopProfile(
 name = "Balanced",
 level = level,
 minMemoryMb = 512,
 maxMemoryMb = 1024,
 xms = 256,
 xmx = minOf(1024, (deviceRamMb * 0.4).toInt()),
 threads = minOf(4, cpuCores),
 gcType = "-XX:+UseG1GC",
 gcFlags = listOf(
 "-XX:+UseG1GC",
 "-XX:MaxGCPauseMillis=50",
 "-XX:G1HeapRegionSize=16m",
 "-XX:+OptimizeStringConcat"
 ),
 gl4esFlags = mapOf(
 "LIBGL_FB" to "3",
 "LIBGL_ES" to "3",
 "LIBGL_BATCH" to "1",
 "LIBGL_RECYCLE" to "1",
 "LIBGL_SILENTSTUB" to "1"
 ),
 renderFlags = listOf("-Dxop.render.smooth=true"),
 modCompatFlags = listOf(),
 shaderFlags = listOf("-Dxop.shader.compat=true"),
 preloadLibs = listOf(
 "lwjgl",
 "lwjgl_opengl",
 "lwjgl_glfw",
 "gson"
 ),
 cacheSizeMb = 64,
 description = "Best balance of performance and stability"
 )

 OptimizationLevel.EXTREME -> XopProfile(
 name = "Extreme",
 level = level,
 minMemoryMb = 768,
 maxMemoryMb = 1536,
 xms = 512,
 xmx = minOf(1536, (deviceRamMb * 0.5).toInt()),
 threads = minOf(6, cpuCores),
 gcType = "-XX:+UseG1GC",
 gcFlags = listOf(
 "-XX:+UseG1GC",
 "-XX:MaxGCPauseMillis=20",
 "-XX:G1HeapRegionSize=32m",
 "-XX:+OptimizeStringConcat",
 "-XX:+AggressiveOpts",
 "-XX:ParallelGCThreads=${minOf(4, cpuCores)}",
 "-XX:ConcGCThreads=${minOf(2, cpuCores)}"
 ),
 gl4esFlags = mapOf(
 "LIBGL_FB" to "3",
 "LIBGL_ES" to "3",
 "LIBGL_BATCH" to "1",
 "LIBGL_RECYCLE" to "1",
 "LIBGL_SILENTSTUB" to "1",
 "LIBGL_FASTMATH" to "1"
 ),
 renderFlags = listOf(
 "-Dxop.render.smooth=true",
 "-Dxop.render.batch=true",
 "-Dxop.render.cull=true"
 ),
 modCompatFlags = listOf(),
 shaderFlags = listOf(
 "-Dxop.shader.compat=true",
 "-Dxop.shader.cache=true"
 ),
 preloadLibs = listOf(
 "lwjgl",
 "lwjgl_opengl",
 "lwjgl_glfw",
 "lwjgl_stb",
 "gson",
 "guava",
 "netty"
 ),
 cacheSizeMb = 128,
 description = "Maximum performance for capable devices"
 )

 OptimizationLevel.HEAVY_MOD -> XopProfile(
 name = "Heavy Mod",
 level = level,
 minMemoryMb = 1024,
 maxMemoryMb = 2048,
 xms = 512,
 xmx = minOf(2048, (deviceRamMb * 0.6).toInt()),
 threads = minOf(6, cpuCores),
 gcType = "-XX:+UseG1GC",
 gcFlags = listOf(
 "-XX:+UseG1GC",
 "-XX:MaxGCPauseMillis=30",
 "-XX:G1HeapRegionSize=32m",
 "-XX:+OptimizeStringConcat",
 "-XX:+UseCompressedOops",
 "-XX:ParallelGCThreads=${minOf(4, cpuCores)}",
 "-XX:ConcGCThreads=${minOf(2, cpuCores)}",
 "-XX:+UseStringDeduplication"
 ),
 gl4esFlags = mapOf(
 "LIBGL_FB" to "3",
 "LIBGL_ES" to "3",
 "LIBGL_BATCH" to "1",
 "LIBGL_RECYCLE" to "1",
 "LIBGL_SILENTSTUB" to "1"
 ),
 renderFlags = listOf(
 "-Dxop.render.smooth=true",
 "-Dxop.render.batch=true"
 ),
 modCompatFlags = listOf(
 "-Dxop.modcompat.heavy=true",
 "-Dxop.modcompat.scan=true",
 "-Dxop.modcompat.fallback=true"
 ),
 shaderFlags = listOf("-Dxop.shader.compat=true"),
 preloadLibs = listOf(
 "lwjgl",
 "lwjgl_opengl",
 "lwjgl_glfw",
 "lwjgl_stb",
 "gson",
 "guava",
 "netty",
 "asm",
 "log4j"
 ),
 cacheSizeMb = 128,
 description = "Optimized for heavy modpacks"
 )

 OptimizationLevel.SHADER_OPTIMIZED -> XopProfile(
 name = "Shader Optimized",
 level = level,
 minMemoryMb = 1024,
 maxMemoryMb = 2048,
 xms = 512,
 xmx = minOf(2048, (deviceRamMb * 0.55).toInt()),
 threads = minOf(6, cpuCores),
 gcType = "-XX:+UseG1GC",
 gcFlags = listOf(
 "-XX:+UseG1GC",
 "-XX:MaxGCPauseMillis=25",
 "-XX:G1HeapRegionSize=32m",
 "-XX:+OptimizeStringConcat",
 "-XX:ParallelGCThreads=${minOf(4, cpuCores)}"
 ),
 gl4esFlags = mapOf(
 "LIBGL_FB" to "3",
 "LIBGL_ES" to "3",
 "LIBGL_BATCH" to "1",
 "LIBGL_RECYCLE" to "1",
 "LIBGL_SILENTSTUB" to "1",
 "LIBGL_SHADERS" to "1"
 ),
 renderFlags = listOf(
 "-Dxop.render.smooth=true",
 "-Dxop.render.shader=true"
 ),
 modCompatFlags = listOf(),
 shaderFlags = listOf(
 "-Dxop.shader.compat=true",
 "-Dxop.shader.cache=true",
 "-Dxop.shader.stream=true"
 ),
 preloadLibs = listOf(
 "lwjgl",
 "lwjgl_opengl",
 "lwjgl_glfw",
 "lwjgl_stb",
 "gson",
 "guava"
 ),
 cacheSizeMb = 128,
 description = "Best for shader packs"
 )
 }
 }
 }

 fun toJavaArgs(): List<String> {
 val args = mutableListOf<String>()
 args.add("-Xms${xms}m")
 args.add("-Xmx${xmx}m")
 args.addAll(gcFlags)
 args.addAll(renderFlags)
 args.addAll(modCompatFlags)
 args.addAll(shaderFlags)
 if (preloadLibs.isNotEmpty()) {
 args.add("-Dxop.preload=${preloadLibs.joinToString(",")}")
 }
 return args
 }

 fun toEnvVars(): Map<String, String> {
 return gl4esFlags.toMutableMap().apply {
 put("XOP_LEVEL", level.name)
 put("XOP_CACHE_MB", cacheSizeMb.toString())
 }
 }
}
