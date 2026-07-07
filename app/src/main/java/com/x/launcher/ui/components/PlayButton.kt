
package com.x.launcher.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.launcher.core.AccountManager
import com.x.launcher.core.RuntimeLauncher
import com.x.launcher.core.VersionManager
import com.x.launcher.ui.theme.XBlack
import com.x.launcher.ui.theme.XButterYellow
import com.x.launcher.ui.theme.XCardDark
import com.x.launcher.ui.theme.XGrayLight
import com.x.launcher.ui.theme.XGreen
import com.x.launcher.ui.theme.XRed
import com.x.launcher.ui.theme.XWhite
import kotlinx.coroutines.launch

@Composable
fun PlayButton(
 modifier: Modifier = Modifier,
 onLaunch: (String) -> Unit = {}
) {
 val scope = rememberCoroutineScope()
 val selectedVersion by VersionManager.selectedVersion.collectAsState()
 val account by AccountManager.currentAccount.collectAsState()

 var showDialog by remember { mutableStateOf(false) }
 var dialogTitle by remember { mutableStateOf("") }
 var dialogMessage by remember { mutableStateOf("") }
 var isError by remember { mutableStateOf(false) }

 val canPlay = selectedVersion != null && selectedVersion!!.isDownloaded

 Button(
 onClick = {
 val version = selectedVersion
 if (version == null) {
 dialogTitle = "No Version Selected"
 dialogMessage = "Please select a Minecraft version first."
 isError = true
 showDialog = true
 return@Button
 }

 if (!version.isDownloaded) {
 dialogTitle = "Version Not Downloaded"
 dialogMessage = "Download ${version.id} first before playing."
 isError = true
 showDialog = true
 return@Button
 }

 val username = account?.username ?: "Player"

 dialogTitle = "Launching"
 dialogMessage = "Starting Minecraft ${version.id} as $username..."
 isError = false
 showDialog = true

 onLaunch(version.id)
 },
 modifier = modifier
 .width(240.dp)
 .height(58.dp),
 shape = RoundedCornerShape(18.dp),
 colors = ButtonDefaults.buttonColors(
 containerColor = XButterYellow,
 contentColor = XBlack
 )
 ) {
 Text(
 text = if (canPlay) "PLAY" else "SELECT VERSION",
 fontSize = 20.sp,
 fontWeight = FontWeight.Bold
 )
 }

 if (showDialog) {
 AlertDialog(
 onDismissRequest = { showDialog = false },
 confirmButton = {
 TextButton(onClick = { showDialog = false }) {
 Text(
 text = if (isError) "OK" else "Got it",
 color = XButterYellow
 )
 }
 },
 title = {
 Text(
 text = dialogTitle,
 color = XWhite,
 fontWeight = FontWeight.Bold
 )
 },
 text = {
 Text(
 text = dialogMessage,
 color = if (isError) XRed else XGrayLight
 )
 },
 containerColor = XCardDark
 )
 }
}
