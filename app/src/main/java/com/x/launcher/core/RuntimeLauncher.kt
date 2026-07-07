package com.x.launcher.core

import android.content.Context
import android.content.Intent
import com.x.launcher.game.GameActivity
import com.x.launcher.runtime.AssetDownloader
import com.x.launcher.runtime.DynamicClassLoader
import com.x.launcher.runtime.Gl4esManager
import com.x.launcher.runtime.JREInstaller
import com.x.launcher.runtime.JVMRuntime
import com.x.launcher.runtime.LibraryDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
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

data class LaunchStage(
 val step: LaunchStep,
 val label: String,
 val progress: Float = 0f
)

enum class LaunchStep {
 IDLE,
 CHECKING_VERSION,
 INSTALLING_JRE,
 INSTALLING_GL4ES,
 DOWNLOADING_LIBRARIES,
 DOWNLOADING_ASSETS,
 LOADING_CLASSES,
 STARTING_JVM,
 LAUNCHING_GAME,
 DONE,
 ERROR
}

object RuntimeLauncher {

 private val _launchStage = MutableStateFlow(
 LaunchStage(LaunchStep.IDLE, "")
 )
 val launchStage: StateFlow<LaunchStage> = _launchStage

 suspend fun launch(
 context: Context,
 config: LaunchConfig
 ): LaunchResult = withContext(Dispatchers.IO) {
 try {
 // 1. Verify version exists
 _launchStage.value = LaunchStage(
 LaunchStep.CHECKING_VERSION,
 "Checking version ${config.versionId}..."
 )

 val versionFile = File(
 config.gameDir,
 "versions/${config.versionId}/${config.versionId}.jar"
 )
 val versionJsonFile = File(
 config.gameDir,
 "versions/${config.versionId}/${config.versionId}.json"
 )

 if (!versionFile.exists() || !versionJsonFile.exists()) {
 _launchStage.value = LaunchStage(
 LaunchStep.ERROR,
 "Version ${config.versionId} not installed"
 )
 return@withContext LaunchResult(
 false,
 "Version ${config.versionId} not installed"
 )
 }

 // 2. Install JRE if needed
 if (!JREInstaller.isJREInstalled(config.gameDir)) {
 _launchStage.value = LaunchStage(
 LaunchStep.INSTALLING_JRE,
 "Installing Java Runtime..."
 )

 val jreResult = JREInstaller.installJRE(context, config.gameDir)
 if (jreResult.isFailure) {
 _launchStage.value = LaunchStage(
 LaunchStep.ERROR,
 "JRE installation failed: ${jreResult.exceptionOrNull()?.message}"
 )
 return@withContext LaunchResult(
 false,
 "JRE installation failed"
 )
 }
 }

 // 3. Install gl4es if needed
 if (!Gl4esManager.isGl4esInstalled(config.gameDir)) {
 _launchStage.value = LaunchStage(
 LaunchStep.INSTALLING_GL4ES,
 "Installing OpenGL ES bridge..."
 )

 val gl4esResult = Gl4esManager.install(context, config.gameDir)
 if (gl4esResult.isFailure) {
 _launchStage.value = LaunchStage(
 LaunchStep.ERROR,
 "gl4es installation failed: ${gl4esResult.exceptionOrNull()?.message}"
 )
 return@withContext LaunchResult(
 false,
 "gl4es installation failed"
 )
 }
 }

 // 4. Download libraries if needed
 if (!LibraryDownloader.areLibrariesInstalled(config.gameDir, config.versionId)) {
 _launchStage.value = LaunchStage(
 LaunchStep.DOWNLOADING_LIBRARIES,
 "Downloading libraries (LWJGL, Gson, Netty...)..."
 )

 val versionJson = JSONObject(versionJsonFile.readText())
 val libResult = LibraryDownloader.downloadLibraries(
 config.gameDir,
 versionJson
 )
 if (libResult.isFailure) {
 _launchStage.value = LaunchStage(
 LaunchStep.ERROR,
 "Library download failed: ${libResult.exceptionOrNull()?.message}"
 )
 return@withContext LaunchResult(
 false,
 "Library download failed"
 )
 }
 }

 // 5. Download assets if needed
 if (!AssetDownloader.areAssetsInstalled(config.gameDir, config.versionId)) {
 _launchStage.value = LaunchStage(
 LaunchStep.DOWNLOADING_ASSETS,
 "Downloading Minecraft assets..."
 )

 val versionJson = JSONObject(versionJsonFile.readText())
 val assetIndexUrl = versionJson.optJSONObject("assetIndex")?.optString("url")

 val assetResult = AssetDownloader.downloadAssets(
 config.gameDir,
 config.versionId,
 assetIndexUrl
 )
 if (assetResult.isFailure) {
 _launchStage.value = LaunchStage(
 LaunchStep.ERROR,
 "Asset download failed: ${assetResult.exceptionOrNull()?.message}"
 )
 return@withContext LaunchResult(
 false,
 "Asset download failed"
 )
 }
 }

 // 6. Load game classes
 _launchStage.value = LaunchStage(
 LaunchStep.LOADING_CLASSES,
 "Loading game classes..."
 )

 val loadResult = DynamicClassLoader.loadGameJar(
 jarFile = versionFile,
 librariesDir = config.librariesDir
 )

 if (!loadResult.success) {
 _launchStage.value = LaunchStage(
 LaunchStep.ERROR,
 "Class loading failed: ${loadResult.message}"
 )
 return@withContext LaunchResult(
 false,
 "Class loading failed: ${loadResult.message}"
 )
 }

 val mainClass = loadResult.mainClass
 ?: "net.minecraft.client.main.Main"

 // 7. Start JVM
 _launchStage.value = LaunchStage(
 LaunchStep.STARTING_JVM,
 "Starting JVM..."
 )

 val javaHome = JREInstaller.getJavaHome(config.gameDir)
 val classpath = LibraryDownloader.getClasspath(config.gameDir) + versionFile
 val nativesPath = LibraryDownloader.getNativesPath(config.gameDir)
 val gl4esEnv = Gl4esManager.setupEnvironment(config.gameDir)

 val jvmConfig = JVMRuntime.JVMConfig(
 javaHome = javaHome,
 classpath = classpath,
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
 "--versionType", "release",
 "-- nativesDir", nativesPath
 ) + config.javaArgs
 )

 val jvmResult = JVMRuntime.start(jvmConfig)

 if (!jvmResult.success) {
 _launchStage.value = LaunchStage(
 LaunchStep.ERROR,
 "JVM failed: ${jvmResult.message}"
 )
 return@withContext LaunchResult(
 false,
 "JVM failed: ${jvmResult.message}"
 )
 }

 // 8. Launch GameActivity
 _launchStage.value = LaunchStage(
 LaunchStep.LAUNCHING_GAME,
 "Launching game..."
 )

 val intent = Intent(context, GameActivity::class.java).apply {
 putExtra("versionId", config.versionId)
 putExtra("username", config.username)
 putExtra("minMemoryMb", config.minMemoryMb)
 putExtra("maxMemoryMb", config.maxMemoryMb)
 putExtra("mainClass", mainClass)
 putExtra("gameDir", config.gameDir.absolutePath)
 putExtra("assetsDir", config.assetsDir.absolutePath)
 putExtra("librariesDir", config.librariesDir.absolutePath)
 putExtra("nativesPath", nativesPath)
 addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
 }

 context.startActivity(intent)

 _launchStage.value = LaunchStage(
 LaunchStep.DONE,
 "Game launched"
 )

 LaunchResult(true, "Launching Minecraft ${config.versionId}")
 } catch (e: Exception) {
 _launchStage.value = LaunchStage(
 LaunchStep.ERROR,
 e.message ?: "Unknown error"
 )
 LaunchResult(false, e.message ?: "Unknown launch error")
 }
 }

 fun resetStage() {
 _launchStage.value = LaunchStage(LaunchStep.IDLE, "")
 }
}
