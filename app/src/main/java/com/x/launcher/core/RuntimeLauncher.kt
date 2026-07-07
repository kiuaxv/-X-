package com.x.launcher.core

import android.content.Context
import android.content.Intent
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
 val versionFile = File(config.gameDir, "versions/${config.versionId}/${config.versionId}.jar")

 if (!versionFile.exists()) {
 return LaunchResult(
 success = false,
 message = "Game version file not found: ${config.versionId}"
 )
 }

 val javaCommand = buildJavaCommand(config, versionFile)

 val intent = Intent(context, GameActivity::class.java).apply {
 putExtra("versionId", config.versionId)
 putExtra("username", config.username)
 putExtra("minMemoryMb", config.minMemoryMb)
 putExtra("maxMemoryMb", config.maxMemoryMb)
 putStringArrayListExtra("javaArgs", ArrayList(config.javaArgs))
 putExtra("javaCommand", javaCommand)
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

 private fun buildJavaCommand(
 config: LaunchConfig,
 versionFile: File
 ): String {
 val classpath = buildClasspath(config, versionFile)

 val baseArgs = listOf(
 "java",
 "-Xms${config.minMemoryMb}M",
 "-Xmx${config.maxMemoryMb}M",
 "-cp",
 classpath,
 "net.minecraft.client.main.Main",
 "--username", config.username,
 "--version", config.versionId,
 "--gameDir", config.gameDir.absolutePath,
 "--assetsDir", config.assetsDir.absolutePath,
 "--assetIndex", config.versionId,
 "--accessToken", "offline",
 "--userType", "legacy",
 "--versionType", "release"
 )

 return (baseArgs + config.javaArgs).joinToString(" ")
 }

 private fun buildClasspath(
 config: LaunchConfig,
 versionFile: File
 ): String {
 val libraries = config.librariesDir
 .walkTopDown()
 .filter { it.isFile && it.extension == "jar" }
 .joinToString(File.pathSeparator) { it.absolutePath }

 return if (libraries.isNotEmpty()) {
 libraries + File.pathSeparator + versionFile.absolutePath
 } else {
 versionFile.absolutePath
 }
 }
}
