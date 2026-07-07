package com.x.launcher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.x.launcher.ui.theme.XButterYellow
import com.x.launcher.ui.theme.XGrayLight
import com.x.launcher.ui.theme.XWhite

@Composable
fun CenterContent(modifier: Modifier = Modifier) {
 Column(
 modifier = modifier,
 horizontalAlignment = Alignment.CenterHorizontally
 ) {
 Text(
 text = "X Launcher",
 style = MaterialTheme.typography.displayMedium,
 color = XWhite,
 fontWeight = FontWeight.Bold
 )

 Spacer(modifier = Modifier.height(10.dp))

 Text(
 text = "0.1 Alpha",
 style = MaterialTheme.typography.titleMedium,
 color = XButterYellow,
 fontWeight = FontWeight.SemiBold
 )

 Spacer(modifier = Modifier.height(18.dp))

 Text(
 text = "Modern Minecraft launcher for Android",
 style = MaterialTheme.typography.bodyLarge,
 color = XGrayLight
 )

 Spacer(modifier = Modifier.height(8.dp))

 Text(
 text = "Fast launch • Clean UI • Better control system",
 style = MaterialTheme.typography.bodyMedium,
 color = XGrayLight.copy(alpha = 0.85f)
 )
 }
}
