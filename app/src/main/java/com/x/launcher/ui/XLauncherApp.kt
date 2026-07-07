package com.x.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.x.launcher.ui.theme.XBlack

@Composable
fun XLauncherApp() {
 Surface(
 modifier = Modifier.fillMaxSize(),
 color = XBlack
 ) {
 Box(modifier = Modifier.fillMaxSize()) {
 TopBar()
 LeftSkinButton()
 CenterContent()
 RightStatusCard()
 PlayButton()
 }
 }
}
