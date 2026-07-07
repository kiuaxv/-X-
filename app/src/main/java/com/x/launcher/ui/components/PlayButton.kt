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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.launcher.ui.theme.XBlack
import com.x.launcher.ui.theme.XButterYellow
import com.x.launcher.ui.theme.XCardDark
import com.x.launcher.ui.theme.XGrayLight
import com.x.launcher.ui.theme.XWhite

@Composable
fun PlayButton(modifier: Modifier = Modifier) {
 var showDialog by remember { mutableStateOf(false) }

 Button(
 onClick = { showDialog = true },
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
 text = "PLAY",
 fontSize = 20.sp,
 fontWeight = FontWeight.Bold
 )
 }

 if (showDialog) {
 AlertDialog(
 onDismissRequest = { showDialog = false },
 confirmButton = {
 TextButton(onClick = { showDialog = false }) {
 Text("OK", color = XButterYellow)
 }
 },
 title = {
 Text(
 text = "Launch",
 color = XWhite,
 fontWeight = FontWeight.Bold
 )
 },
 text = {
 Text(
 text = "Launch flow will be connected in the next step.",
 color = XGrayLight
 )
 },
 containerColor = XCardDark
 )
 }
}
