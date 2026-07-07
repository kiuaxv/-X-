package com.x.launcher.runtime

import android.content.Context
import java.io.File

data class JVMConfig(
 val javaHome: File,
 val classpath: List<File>,
 val mainClass: String,
 val minMemoryMb: Int = 512,
 val maxMemoryMb: Int = 1024,
 val extraArgs: List<String> = emptyList()
)

data class JVMResult(
 val success: Boolean,
 val message: String,
 val pid: Long = -1
)

object JVMRuntime {

 private var isRunning = false
 private var process: Process? = null

 fun extractRuntime(context: Context, targetDir: File): Result<File> {
 return runCatching {
 val runtimeDir = File(targetDir, "jvm-runtime")
 if (!runtimeDir.exists()) {
 runtimeDir.mkdirs()
 }

 // TODO: Extract bundled JRE from assets
 // For now we check if a runtime already exists
 val javaBin = File(runtimeDir, "bin/java")
 if (!javaBin.exists()) {
 throw RuntimeException("JVM runtime not found. Please extract JRE first.")
 }

 runtimeDir
 }
 }

 fun start(config: JVMConfig): JVMResult {
 if (isRunning) {
 return JVMResult(
 success = false,
 message = "JVM is already running"
 )
 }

 return try {
 val command = buildCommand(config)
 val builder = ProcessBuilder(command)
 builder.redirectErrorStream(true)

 // Set JAVA_HOME
 builder.environment()["JAVA_HOME"] = config.javaHome.absolutePath
 builder.environment()["LD_LIBRARY_PATH"] = config.javaHome.absolutePath + "/lib"

 process = builder.start()
 isRunning = true

 JVMResult(
 success = true,
 message = "JVM started successfully",
 pid = process?.pid() ?: -1
 )
 } catch (e: Exception) {
 JVMResult(
 success = false,
 message = e.message ?: "Failed to start JVM"
 )
 }
 }

 fun stop(): JVMResult {
 return try {
 process?.let {
 it.destroy()
 if (it.isAlive) {
 it.destroyForcibly()
 }
 isRunning = false
 JVMResult(success = true, message = "JVM stopped")
 } ?: JVMResult(
 success = false,
 message = "No running JVM to stop"
 )
 } catch (e: Exception) {
 JVMResult(
 success = false,
 message = e.message ?: "Failed to stop JVM"
 )
 }
 }

 fun isAlive(): Boolean {
 return isRunning && process?.isAlive == true
 }

 fun getOutput(callback: (String) -> Unit) {
 process?.inputStream?.bufferedReader()?.use { reader ->
 var line: String?
 while (reader.readLine().also { line = it } != null) {
 callback(line ?: "")
 }
 }
 }

 private fun buildCommand(config: JVMConfig): List<String> {
 val command = mutableListOf<String>()

 // Java binary
 command.add(File(config.javaHome, "bin/java").absolutePath)

 // Memory
 command.add("-Xms${config.minMemoryMb}M")
 command.add("-Xmx${config.maxMemoryMb}M")

 // GC optimization for games
 command.add("-XX:+UseG1GC")
 command.add("-XX:MaxGCPauseMillis=50")
 command.add("-XX:+UnlockExperimentalVMOptions")
 command.add("-XX:G1NewSizePercent=20")
 command.add("-XX:G1ReservePercent=20")

 // Headless mode for Android
 command.add("-Djava.awt.headless=true")

 // OpenGL ES bridge
 command.add("-Dorg.lwjgl.opengl.libName=libgl4es.so")
 command.add("-Dorg.lwjgl.system.allocator=system")

 // Classpath
 val classpathStr = config.classpath
 .joinToString(File.pathSeparator) { it.absolutePath }
 command.add("-cp")
 command.add(classpathStr)

 // Extra args
 command.addAll(config.extraArgs)

 // Main class
 command.add(config.mainClass)

 command
 }
} 
