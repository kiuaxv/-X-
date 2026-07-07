package com.x.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.launcher.ui.theme.*

@Composable
fun XLauncherApp() {
 Surface(
 modifier = Modifier.fillMaxSize(),
 color = XBlack
 ) {
 Box(modifier = Modifier.fillMaxSize()) {

 // Top Bar
 TopIconsRow(
 modifier = Modifier
 .align(Alignment.TopCenter)
 .fillMaxWidth()
 .padding(top = 12.dp, start = 16.dp, end = 16.dp)
 )

 // Left Button (Skin Menu)
 LeftIconButton(
 modifier = Modifier
 .align(Alignment.CenterStart)
 .padding(start = 16.dp)
 )

 // Center Content
 CenterContent(
 modifier = Modifier
 .align(Alignment.Center)
 .padding(horizontal = 24.dp)
 )

 // Right Card
 RightCard(
 modifier = Modifier
 .align(Alignment.CenterEnd)
 .padding(end = 16.dp)
 )

 // Bottom Play Button
 PlayButton(
 modifier = Modifier
 .align(Alignment.BottomCenter)
 .padding(bottom = 32.dp)
 )
 }
 }
}

@Composable
private fun TopIconsRow(modifier: Modifier = Modifier) {
 Row(
 modifier = modifier,
 horizontalArrangement = Arrangement.SpaceEvenly,
 verticalAlignment = Alignment.CenterVertically
 ) {
 TopIcon(icon = Icons.Default.Folder, label = "Files")
 Spacer(modifier = Modifier.width(24.dp))
 TopIcon(icon = Icons.Default.Download, label = "Mods")
 Spacer(modifier = Modifier.width(24.dp))
 TopIcon(icon = Icons.Default.Refresh, label = "Refresh")
 Spacer(modifier = Modifier.width(24.dp))
 TopIcon(icon = Icons.Default.Settings, label = "Settings")
 }
}

@Composable
private fun TopIcon(
 icon: androidx.compose.ui.graphics.vector.ImageVector,
 label: String
) {
 var showDialog by remember { mutableStateOf(false) }

 IconButton(
 onClick = { showDialog = true },
 modifier = Modifier
 .size(40.dp)
 .background(XCardDark, CircleShape)
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
 text = { Text("X Launcher 0.1 Alpha
$this section under development", color = XGrayLight) },
 containerColor = XCardDark
 )
 }
}

@Composable
private fun LeftIconButton(modifier: Modifier = Modifier) {
 var showSkinMenu by remember { mutableStateOf(false) }

 IconButton(
 onClick = { showSkinMenu = true },
 modifier = modifier
 .size(36.dp)
 .background(XCardDark, CircleShape)
 ) {
 Icon(
 imageVector = Icons.Default.Folder,
 contentDescription = "Skin Menu",
 tint = XButterYellow,
 modifier = Modifier.size(18.dp)
 )
 }

 if (showSkinMenu) {
 DropdownMenu(
 expanded = true,
 onDismissRequest = { showSkinMenu = false },
 modifier = Modifier.background(XCardDark.copy(alpha = 0.9f))
 ) {
 DropdownMenuItem(
 text = { Text("Change Skin", color = XWhite) },
 onClick = { showSkinMenu = false }
 )
 DropdownMenuItem(
 text = { Text("View Current Skin", color = XWhite) },
 onClick = { showSkinMenu = false
