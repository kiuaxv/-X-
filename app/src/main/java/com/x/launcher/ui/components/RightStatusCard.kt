package com.x.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x.launcher.ui.theme.XBlue
import com.x.launcher.ui.theme.XButterYellow
import com.x.launcher.ui.theme.XCardDark
import com.x.launcher.ui.theme.XGrayLight
import com.x.launcher.ui.theme.XGreen
import com.x.launcher.ui.theme.XRed
import com.x.launcher.ui.theme.XWhite
import com.x.launcher.ui.theme.XYellow

@Composable
fun RightStatusCard(modifier: Modifier = Modifier) {
 Surface(
 modifier = modifier.width(220.dp),
 shape = RoundedCornerShape(20.dp),
 color = XCardDark
 ) {
 Column(
 modifier = Modifier
 .fillMaxWidth()
 .padding(18.dp)
 ) {
 Text(
 text = "System",
 color = XButterYellow,
 style = MaterialTheme.typography.titleMedium,
 fontWeight = FontWeight.Bold
 )

 Spacer(modifier = Modifier.height(14.dp))

 StatusRow("Launcher", "Ready", XGreen)
 Spacer(modifier = Modifier.height(10.dp))
 StatusRow("Mods", "Idle", XYellow)
 Spacer(modifier = Modifier.height(10.dp))
 StatusRow("Renderer", "Stable", XBlue)
 Spacer(modifier = Modifier.height(10.dp))
 StatusRow("Account", "Offline", XRed)

 Spacer(modifier = Modifier.height(14.dp))
 HorizontalDivider(color = XWhite.copy(alpha = 0.08f))
 Spacer(modifier = Modifier.height(14.dp))

 Text(
 text = "Build channel: Alpha",
 color = XGrayLight,
 style = MaterialTheme.typography.bodySmall
 )
 }
 }
}

@Composable
private fun StatusRow(
 title: String,
 value: String,
 dotColor: Color
) {
 Row(
 modifier = Modifier.fillMaxWidth(),
 verticalAlignment = Alignment.CenterVertically
 ) {
 Text(
 text = title,
 color = XWhite,
 style = MaterialTheme.typography.bodyMedium,
 modifier = Modifier.weight(1f)
 )

 Box(
 modifier = Modifier
 .size(8.dp)
 .background(dotColor, CircleShape)
 )

 Spacer(modifier = Modifier.width(8.dp))

 Text(
 text = value,
 color = XGrayLight,
 style = MaterialTheme.typography.bodySmall
 )
 }
}
