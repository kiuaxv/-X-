package com.x.launcher.game

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ComposeView
import com.x.launcher.core.LaunchConfig
import com.x.launcher.core.RuntimeLauncher
import com.x.launcher.runtime.InputBridge
import com.x.launcher.runtime.JVMRuntime
import com.x.launcher.runtime.RendererBridge
import com.x.launcher.runtime.RendererConfig
import kotlinx.coroutines.delay
import java.io.File

class GameActivity : Activity() {

 private var glSurfaceView: GLSurfaceView? = null
 private var composeView: ComposeView? = null

 override fun onCreate(savedInstanceState: Bundle?) {
 super.onCreate(savedInstanceState)

 window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
 window.setFlags(
 WindowManager.LayoutParams.FLAG_FULLSCREEN,
 WindowManager.LayoutParams.FLAG_FULLSCREEN
 )

 val versionId = intent.getStringExtra("versionId") ?: "unknown"
 val username = intent.getStringExtra("username") ?: "Player"
 val javaCommand = intent.getStringExtra("javaCommand") ?: ""
 val gameDirPath = intent.getStringExtra("gameDir") ?: ""

 setupGameView(versionId, username, gameDirPath)
 }

 private fun setupGameView(versionId: String, username: String, gameDirPath: String) {
 glSurfaceView = GLSurfaceView(this).apply {
 setEGLContextClientVersion(3)
 }

 RendererBridge.attach(
 surfaceView = glSurfaceView!!,
 rendererConfig = RendererConfig(
 maxFps = 60,
 vsync = true,
 renderDistance = 8
 )
 )

 // Start JVM runtime
 val jvmConfig = JVMRuntime.JVMConfig(
 javaHome = File(gameDirPath, "jvm-runtime"),
 classpath = listOf(File(gameDirPath, "versions/$versionId/$versionId.jar")),
 mainClass = "net.minecraft.client.main.Main",
 minMemoryMb = intent.getIntExtra("minMemoryMb", 512),
 maxMemoryMb = intent.getIntExtra("maxMemoryMb", 1024),
 extraArgs = listOf(
 "--username", username,
 "--version", versionId,
 "--gameDir", gameDirPath,
 "--accessToken", "offline",
 "--userType", "legacy"
 )
 )

 val result = JVMRuntime.start(jvmConfig)

 if (result.success) {
 // Show game view
 setContentView(glSurfaceView)
 } else {
 // Show error screen
 showErrorScreen(result.message)
 }
 }

 override fun onTouchEvent(event: MotionEvent): Boolean {
 return InputBridge.handleMotionEvent(event) || super.onTouchEvent(event)
 }

 override fun onPause() {
 super.onPause()
 RendererBridge.pauseRendering()
 JVMRuntime.stop()
 }

 override fun onResume() {
 super.onResume()
 RendererBridge.resumeRendering()
 }

 override fun onDestroy() {
 super.onDestroy()
 RendererBridge.detach()
 InputBridge.reset()
 JVMRuntime.stop()
 }

 private fun showErrorScreen(message: String) {
 composeView = ComposeView(this).apply {
 setContent {
 ErrorScreen(message)
 }
 }
 setContentView(composeView)
 }
}

@Composable
private fun ErrorScreen(message: String) {
 Box(
 modifier = Modifier
 .fillMaxSize()
 .background(Color(0xFF0A0A0A)),
 contentAlignment = Alignment.Center
 ) {
 Text(
 text = message,
 color = Color(0xFFFF4444),
 fontSize = 16.sp,
 fontWeight = FontWeight.Medium
 )
 }
}
