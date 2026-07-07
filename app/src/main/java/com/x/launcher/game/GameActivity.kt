package com.x.launcher.game

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.x.launcher.core.RuntimeLauncher
import kotlinx.coroutines.delay

class GameActivity : Activity() {

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

 val launchError = try {
 startRuntime(javaCommand, gameDirPath, versionId, username)
 null
 } catch (e: Exception) {
 e.message
 }

 setContentComposable(versionId, launchError)
 }

 private fun startRuntime(
 javaCommand: String,
 gameDirPath: String,
 versionId: String,
 username: String
 ) {
 // TODO: Bridge to PojavLauncher runtime
 // This is where we hand off to the actual Java runtime
 // For now this is a placeholder that simulates the launch
 }

 private fun setContentComposable(versionId: String, error: String?) {
 val content = if (error != null) {
 "Launch failed: $error"
 } else {
 "Launching Minecraft $versionId..."
 }

 setContentView(
 composeView(content, error != null)
 )
 }

 private fun composeView(text: String, isError: Boolean): android.view.View {
 return androidx.compose.ui.platform.ComposeView(this).apply {
 setContent {
 LaunchScreen(text = text, isError = isError)
 }
 }
 }
}

@Composable
private fun LaunchScreen(
 text: String,
 isError: Boolean
) {
 var dots by remember { mutableStateOf("") }

 LaunchedEffect(Unit) {
 while (true) {
 delay(500)
 dots = when (dots) {
 "" -> "."
 "." -> ".."
 ".." -> "..."
 else -> ""
 }
 }
 }

 Box(
 modifier = Modifier
 .fillMaxSize()
 .background(Color(0xFF0A0A0A)),
 contentAlignment = Alignment.Center
 ) {
 Text(
 text = if (isError) text else "$text$dots",
 color = if (isError) Color(0xFFFF4444) else Color.White,
 fontSize = 18.sp,
 fontWeight = FontWeight.Medium
 )
 }
}
