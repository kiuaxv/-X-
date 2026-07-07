package com.x.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.launcher.ui.theme.XBlack
import com.x.launcher.ui.theme.XBlue
import com.x.launcher.ui.theme.XButterDeep
import com.x.launcher.ui.theme.XButterYellow
import com.x.launcher.ui.theme.XCardDark
import com.x.launcher.ui.theme.XGrayLight
import com.x.launcher.ui.theme.XGreen
import androidx.compose.ui.graphics.Color
import com.x.launcher.ui.theme.XRed
import com.x.launcher.ui.theme.XWhite
import com.x.launcher.ui.theme.XYellow

@Composable
fun XLauncherApp() {
 Surface(
 modifier = Modifier.fillMaxSize(),
 color = XBlack
 ) {
 Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {

 TopIconsRow(
 modifier = Modifier
 .align(Alignment.TopCenter)
 .fillMaxWidth()
 )

 LeftSkinButton(
 modifier = Modifier
 .align(Alignment.CenterStart)
 .offset(y = (-12).dp)
 )

 CenterContent(
 modifier = Modifier.align(Alignment.Center)
 )

 RightStatusCard(
 modifier = Modifier.align(Alignment.CenterEnd)
 )

 PlayButton(
 modifier = Modifier
 .align(Alignment.BottomCenter)
 .padding(bottom = 16.dp)
 )
 }
 }
}

@Composable
private fun TopIconsRow(modifier: Modifier = Modifier) {
 Row(
 modifier = modifier,
 horizontalArrangement = Arrangement.Center,
 verticalAlignment = Alignment.CenterVertically
 ) {
 TopIcon(Icons.Default.Folder, "Files")
 Spacer(Modifier.width(18.dp))
 TopIcon(Icons.Default.Download, "Mods")
 Spacer(Modifier.width(18.dp))
 TopIcon(Icons.Default.Refresh, "Refresh")
 Spacer(Modifier.width(18.dp))
 TopIcon(Icons.Default.Settings, "Settings")
 }
}

@Composable
private fun TopIcon(icon: ImageVector, label: String) {
 var showDialog by remember { mutableStateOf(false) }

 IconButton(
 onClick = { showDialog = true },
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

 if (showDialog) {
 AlertDialog(
 onDismissRequest = { showDialog = false },
 confirmButton = {
 TextButton(onClick = { showDialog = false }) {
 Text("Close", color = XButterYellow)
 }
 },
 title = { Text(label, color = XWhite, fontWeight = FontWeight.Bold) },
 text = {
 Text(
 "X Launcher 0.1 Alpha
This section is under development.",
 color = XGrayLight
 )
 },
 containerColor = XCardDark
 )
 }
}

@Composable
private fun LeftSkinButton(modifier: Modifier = Modifier) {
 var expanded by remember { mutableStateOf(false) }

 Box(modifier = modifier) {
 IconButton(
 onClick = { expanded = true },
 modifier = Modifier
 .size(46.dp)
 .background(XCardDark, CircleShape)
 .border(1.dp, XButterDeep.copy(alpha = 0.22f), CircleShape)
 ) {
 Icon(
 imageVector = Icons.Default.Folder,
 contentDescription = "Skin Menu",
 tint = XButterYellow,
 modifier = Modifier.size(20.dp)
 )
 }

 DropdownMenu(
 expanded = expanded,
 onDismissRequest = { expanded = false },
 modifier = Modifier.background(XCardDark)
 ) {
 DropdownMenuItem(
 text = { Text("Change Skin", color = XWhite) },
 onClick = { expanded = false }
 )
 DropdownMenuItem(
 text = { Text("View Current Skin", color = XWhite) },
 onClick = { expanded = false }
 )
 DropdownMenuItem(
 text = { Text("Import Skin", color = XWhite) },
 onClick = { expanded = false }
 )
 }
 }
}

@Composable
private fun CenterContent(modifier: Modifier = Modifier) {
 Column(
 modifier = modifier,
 horizontalAlignment = Alignment.CenterHorizontally
 ) {
 Text(
 "X Launcher",
 style = MaterialTheme.typography.displayMedium,
 color = XWhite,
 fontWeight = FontWeight.Bold
 )
 Spacer(Modifier.height(10.dp))
 Text(
 "0.1 Alpha",
 style = MaterialTheme.typography.titleMedium,
 color = XButterYellow,
 fontWeight = FontWeight.SemiBold
 )
 Spacer(Modifier.height(18.dp))
 Text(
 "Modern Minecraft launcher for Android",
 style = MaterialTheme.typography.bodyLarge,
 color = XGrayLight
 )
 Spacer(Modifier.height(8.dp))
 Text(
 "Fast launch • Clean UI • Better control system",
 style = MaterialTheme.typography.bodyMedium,
 color = XGrayLight.copy(alpha = 0.85f)
 )
 }
}

@Composable
private fun RightStatusCard(modifier: Modifier = Modifier) {
 Surface(
 modifier = modifier.width(220.dp),
 shape = RoundedCornerShape(20.dp),
 color = XCardDark
 ) {
 Column(Modifier.fillMaxWidth().padding(18.dp)) {
 Text(
 "System",
 color = XButterYellow,
 style = MaterialTheme.typography.titleMedium,
 fontWeight = FontWeight.Bold
 )
 Spacer(Modifier.height(14.dp))
 StatusRow("Launcher", "Ready", XGreen)
 Spacer(Modifier.height(10.dp))
 StatusRow("Mods", "Idle", XYellow)
 Spacer(Modifier.height(10.dp))
 StatusRow("Renderer", "Stable", XBlue)
 Spacer(Modifier.height(10.dp))
 StatusRow("Account", "Offline", XRed)
 Spacer(Modifier.height(14.dp))
 HorizontalDivider(color = XWhite.copy(alpha = 0.08f))
 Spacer(Modifier.height(14.dp))
 Text(
 "Build channel: Alpha",
 color = XGrayLight,
 style = MaterialTheme.typography.bodySmall
 )
 }
 }
}

@Composable
private fun StatusRow(title: String, value: String, dotColor: Color) {
 Row(
 modifier = Modifier.fillMaxWidth(),
 verticalAlignment = Alignment.CenterVertically
 ) {
 Text(
 title,
 color = XWhite,
 style = MaterialTheme.typography.bodyMedium,
