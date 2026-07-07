package com.x.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.x.launcher.ui.components.CenterContent
import com.x.launcher.ui.components.LeftSkinButton
import com.x.launcher.ui.components.PlayButton
import com.x.launcher.ui.components.RightStatusCard
import com.x.launcher.ui.components.TopBar
import com.x.launcher.ui.theme.XBlack

@Composable
fun XLauncherApp() {
 Surface(
 modifier = Modifier.fillMaxSize(),
 color = XBlack
 ) {
 Box(
 modifier = Modifier
 .fillMaxSize()
 .padding(16.dp)
 ) {
 TopBar(
 modifier = Modifier.align(Alignment.TopCenter)
 )

 LeftSkinButton(
 modifier = Modifier.align(Alignment.CenterStart)
 )

 CenterContent(
 modifier = Modifier.align(Alignment.Center)
 )

 RightStatusCard(
 modifier = Modifier.align(Alignment.CenterEnd)
 )

 PlayButton(
 modifier = Modifier.align(Alignment.BottomCenter)
 )
 }
 }
}
