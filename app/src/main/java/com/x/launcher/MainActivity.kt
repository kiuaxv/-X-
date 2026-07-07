package com.x.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.x.launcher.ui.XLauncherApp
import com.x.launcher.ui.theme.XLauncherTheme

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
 super.onCreate(savedInstanceState)
 enableEdgeToEdge()
 setContent {
 XLauncherTheme {
 XLauncherApp()
 }
 }
 }
}
