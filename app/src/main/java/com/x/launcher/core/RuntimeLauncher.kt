 package com.x.launcher.core

import android.content.Context
import android.content.Intent
import com.x.launcher.runtime.DynamicClassLoader
import com.x.launcher.runtime.JVMRuntime
import java.io.File

data class LaunchConfig(
 val versionId: String,
 val username: String = "Player",
 val minMemoryMb: Int = 512,
 val maxMemoryMb: Int = 1024,
 val javaArgs: List<String> = emptyList(),
 val gameDir: File,
 val assetsDir: File,
 val librariesDir: File
)

data class LaunchResult(
 val success: Boolean,
 val message: String
)

object RuntimeLauncher {

 fun launch(
 context: Context,
 config: LaunchConfig
 ): LaunchResult {
 return try {
 // 1. Verify version file exists
 val versionFile = File(
 config.gameDir,
 "versions/${config.versionId}/${config.versionId}.jar"
 )

 if (!versionFile.exists()) {
 return LaunchResult(
 success = false,
 message = "Game version file not found: ${config.versionId}"
 )
 }

 // 2. Load game classes dynamically
 val loadResult = DynamicClassLoader.loadGameJar(
 jarFile = versionFile,
 librariesDir = config.librariesDir
 )

 if (!loadResult.success) {
 return LaunchResult(
 success = false,
 message = "Failed to load classes: ${loadResult.message}"
 )
 }

 val mainClass = loadResult.mainClass
 ?: "net.minecraft.client.main.Main"

 // 3. Build JVM config
 val jvmConfig = JVMRuntime.JVMConfig(
 javaHome = File(config.gameDir, "jvm-runtime"),
 classpath = buildClasspath(config, versionFile),
 mainClass = mainClass,
 minMemoryMb = config.minMemoryMb,
 maxMemoryMb = config.maxMemoryMb,
 extraArgs = listOf(
 "--username", config.username,
 "--version", config.versionId,
 "--gameDir", config.gameDir.absolutePath,
 "--assetsDir", config.assetsDir.absolutePath,
 "--assetIndex", config.versionId,
 "--accessToken", "offline",
 "--userType", "legacy",
 "--versionType", "release"
 ) + config.javaArgs
 )

 // 4. Start JVM
 val jvmResult = JVMRuntime.start(jvmConfig)

 if (!jvmResult.success) {
 return LaunchResult(
 success = false,
 message = "JVM failed: ${jvmResult.message}"
 )
 }

 // 5. Launch GameActivity for rendering + input
 val intent = Intent(context, GameActivity::class.java).apply {
 putExtra("versionId", config.versionId)
 putExtra("username", config.username)
 putExtra("minMemoryMb", config.minMemoryMb)
 putExtra("maxMemoryMb", config.maxMemoryMb)
 putExtra("mainClass", mainClass)
 putExtra("gameDir", config.gameDir.absolutePath)
 putExtra("assetsDir", config.assetsDir.absolutePath)
 putExtra("librariesDir", config.librariesDir.absolutePath)
 addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
 }

 context.startActivity(intent)

 LaunchResult(
 success = true,
 message = "Launching Minecraft ${config.versionId}"
 )
 } catch (e: Exception) {
 LaunchResult(
 success = false,
 message = e.message ?: "Unknown launch error"
 )
 }
 }

 private fun buildClasspath(
 config: LaunchConfig,
 versionFile: File
 ): List<File> {
 val libraries = config.librariesDir
 .walkTopDown()
 .filter { it.isFile && it.extension == "jar" }
 .toList()

 return if (libraries.isNotEmpty()) {
 libraries + versionFile
 } else {
 listOf(versionFile)
 }
 }
}
