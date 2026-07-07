package com.x.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.x.launcher.ui.theme.XButterDeep
import com.x.launcher.ui.theme.XButterYellow
import com.x.launcher.ui.theme.XCardDark

@Composable
fun TopBar(modifier: Modifier = Modifier) {
 Row(
 modifier = modifier
 .fillMaxWidth()
 .padding(top = 12.dp),
 horizontalArrangement = Arrangement.Center,
 verticalAlignment = Alignment.CenterVertically
 ) {
 TopIcon(icon = Icons.Default.Folder, label = "Files")
 Spacer(modifier = Modifier.width(18.dp))
 TopIcon(icon = Icons.Default.Download, label = "Mods")
 Spacer(modifier = Modifier.width(18.dp))
 TopIcon(icon = Icons.Default.Refresh, label = "Refresh")
 Spacer(modifier = Modifier.width(18.dp))
 TopIcon(icon = Icons.Default.Settings, label = "Settings")
 }
}

@Composable
private fun TopIcon(icon: ImageVector, label: String) {
 IconButton(
 onClick = { },
 modifier = Modifier
 .size(46.dp)
 .background(XCardDark, CircleShape)
 .border(1.dp, XButterDeep.copy(alpha = 0.22f), CircleShape)
 ) {
 Icon(
 imageVector = icon,
 contentDescription = label,
 tint = XButterYellow,
 modifier = Modifier.size(22.dp)
 )
 }
}
