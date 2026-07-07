package com.x.launcher.game

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ControlButton(
 val label: String,
 val keyCode: Int,
 val size: Int = 56
)

@Composable
fun TouchControls(
 modifier: Modifier = Modifier
) {
 Box(modifier = modifier.fillMaxSize()) {

 // Movement joystick (left side)
 MovementJoystick(
 modifier = Modifier
 .align(Alignment.BottomStart)
 .padding(start = 24.dp, bottom = 48.dp)
 )

 // Look area (center — swipe to look around)
 LookArea(
 modifier = Modifier
 .align(Alignment.Center)
 .fillMaxWidth()
 .height(320.dp)
 )

 // Action buttons (right side)
 ActionButtons(
 modifier = Modifier
 .align(Alignment.BottomEnd)
 .padding(end = 24.dp, bottom = 48.dp)
 )
 }
}

@Composable
private fun MovementJoystick(modifier: Modifier = Modifier) {
 var isPressed by remember { mutableStateOf(false) }

 Box(
 modifier = modifier
 .size(120.dp)
 .background(
 Color.Black.copy(alpha = 0.35f),
 CircleShape
 )
 .border(
 width = 2.dp,
 color = Color.White.copy(alpha = 0.25f),
 shape = CircleShape
 )
 .pointerInput(Unit) {
 awaitPointerEventScope {
 while (true) {
 val event = awaitPointerEvent()
 isPressed = event.changes.any { it.pressed }
 }
 }
 },
 contentAlignment = Alignment.Center
 ) {
 Box(
 modifier = Modifier
 .size(48.dp)
 .background(
 if (isPressed) Color.White.copy(alpha = 0.4f)
 else Color.White.copy(alpha = 0.2f),
 CircleShape
 )
 )
 }
}

@Composable
private fun LookArea(modifier: Modifier = Modifier) {
 Box(
 modifier = modifier
 .background(Color.Transparent)
 .pointerInput(Unit) {
 awaitPointerEventScope {
 while (true) {
 val event = awaitPointerEvent()
 // TODO: Send look deltas to game runtime
 val change = event.changes.firstOrNull() ?: continue
 val deltaX = change.positionChange().x
 val deltaY = change.positionChange().y
 }
 }
 }
 )
}

@Composable
private fun ActionButtons(modifier: Modifier = Modifier) {
 Column(
 modifier = modifier,
 horizontalAlignment = Alignment.End
 ) {
 // Jump button
 ControlButtonView(
 button = ControlButton("⬆", 57),
 modifier = Modifier.size(56.dp)
 )

 Spacer(modifier = Modifier.height(12.dp))

 Row(
 horizontalArrangement = Arrangement.spacedBy(12.dp)
 ) {
 ControlButtonView(
 button = ControlButton("▣", 31),
 modifier = Modifier.size(52.dp)
 )
 ControlButtonView(
 button = ControlButton("⬇", 42),
 modifier = Modifier.size(52.dp)
 )
 }

 Spacer(modifier = Modifier.height(12.dp))

 Row(
 horizontalArrangement = Arrangement.spacedBy(12.dp)
 ) {
 ControlButtonView(
 button = ControlButton("A", 29),
 modifier = Modifier.size(50.dp)
 )
 ControlButtonView(
 button = ControlButton("B", 30),
 modifier = Modifier.size(50.dp)
 )
 }
 }
}

@Composable
private fun ControlButtonView(
 button: ControlButton,
 modifier: Modifier = Modifier
) {
 var isPressed by remember { mutableStateOf(false) }

 Box(
 modifier = modifier
 .background(
 if (isPressed) Color.White.copy(alpha = 0.35f)
 else Color.Black.copy(alpha = 0.35f),
 RoundedCornerShape(12.dp)
 )
 .border(
 width = 1.dp,
 color = Color.White.copy(alpha = 0.3f),
 shape = RoundedCornerShape(12.dp)
 )
 .pointerInput(button.keyCode) {
 awaitPointerEventScope {
 while (true) {
 val event = awaitPointerEvent()
 isPressed = event.changes.any { it.pressed }
 // TODO: Send keyCode to game runtime
 }
 }
 },
 contentAlignment = Alignment.Center
 ) {
 Text(
 text = button.label,
 color = Color.White,
 fontWeight = FontWeight.Bold,
 fontSize = 18.sp
 )
 }
}
