 
package com.x.launcher.runtime

import android.view.MotionEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

enum class InputAction {
 MOVE_FORWARD,
 MOVE_BACKWARD,
 MOVE_LEFT,
 MOVE_RIGHT,
 JUMP,
 SNEAK,
 SPRINT,
 ATTACK,
 USE,
 PICK_ITEM,
 DROP_ITEM,
 OPEN_INVENTORY,
 OPEN_CHAT,
 SWITCH_HOTBAR_1,
 SWITCH_HOTBAR_2,
 SWITCH_HOTBAR_3,
 SWITCH_HOTBAR_4,
 SWITCH_HOTBAR_5,
 SWITCH_HOTBAR_6,
 SWITCH_HOTBAR_7,
 SWITCH_HOTBAR_8,
 SWITCH_HOTBAR_9
}

data class InputEvent(
 val action: InputAction,
 val pressed: Boolean,
 val deltaX: Float = 0f,
 val deltaY: Float = 0f,
 val timestamp: Long = System.currentTimeMillis()
)

data class JoystickState(
 val x: Float = 0f,
 val y: Float = 0f,
 val isActive: Boolean = false
) {
 val magnitude: Float
 get() = kotlin.math.sqrt(x * x + y * y).coerceIn(0f, 1f)
}

object InputBridge {

 private val _inputEvents = MutableSharedFlow<InputEvent>(extraBufferCapacity = 64)
 val inputEvents: SharedFlow<InputEvent> = _inputEvents

 private val _lookEvents = MutableSharedFlow<Pair<Float, Float>>(extraBufferCapacity = 32)
 val lookEvents: SharedFlow<Pair<Float, Float>> = _lookEvents

 private var movementJoystick = JoystickState()
 private var lookJoystick = JoystickState()

 // Key mapping (configurable)
 private val keyMap = mutableMapOf(
 InputAction.MOVE_FORWARD to 17, // W
 InputAction.MOVE_BACKWARD to 31, // S
 InputAction.MOVE_LEFT to 30, // A
 InputAction.MOVE_RIGHT to 32, // D
 InputAction.JUMP to 57, // Space
 InputAction.SNEAK to 42, // Shift
 InputAction.SPRINT to 42, // Shift (double tap)
 InputAction.ATTACK to 0, // Left click
 InputAction.USE to 1, // Right click
 InputAction.OPEN_INVENTORY to 23, // E
 InputAction.OPEN_CHAT to 33, // T
 InputAction.DROP_ITEM to 25, // Q
 )

 fun setMovementInput(x: Float, y: Float, active: Boolean) {
 movementJoystick = JoystickState(
 x = x.coerceIn(-1f, 1f),
 y = y.coerceIn(-1f, 1f),
 isActive = active
 )

 if (active) {
 // Forward/backward
 emitMovement(InputAction.MOVE_FORWARD, y < -0.3f)
 emitMovement(InputAction.MOVE_BACKWARD, y > 0.3f)
 emitMovement(InputAction.MOVE_LEFT, x < -0.3f)
 emitMovement(InputAction.MOVE_RIGHT, x > 0.3f)
 } else {
 // Release all movement keys
 emitMovement(InputAction.MOVE_FORWARD, false)
 emitMovement(InputAction.MOVE_BACKWARD, false)
 emitMovement(InputAction.MOVE_LEFT, false)
 emitMovement(InputAction.MOVE_RIGHT, false)
 }
 }

 fun setLookInput(deltaX: Float, deltaY: Float) {
 if (deltaX != 0f || deltaY != 0f) {
 lookJoystick = JoystickState(x = deltaX, y = deltaY, isActive = true)
 _lookEvents.tryEmit(Pair(deltaX, deltaY))
 }
 }

 fun pressAction(action: InputAction, pressed: Boolean) {
 _inputEvents.tryEmit(
 InputEvent(action = action, pressed = pressed)
 )
 }

 fun handleMotionEvent(event: MotionEvent): Boolean {
 return when (event.actionMasked) {
 MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
 pressAction(InputAction.ATTACK, true)
 true
 }
 MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
 pressAction(InputAction.ATTACK, false)
 true
 }
 MotionEvent.ACTION_MOVE -> {
 setLookInput(
 deltaX = event.x,
 deltaY = event.y
 )
 true
 }
 else -> false
 }
 }

 fun getMovementJoystick(): JoystickState = movementJoystick
 fun getLookJoystick(): JoystickState = lookJoystick

 fun remapKey(action: InputAction, keyCode: Int) {
 keyMap[action] = keyCode
 }

 fun getKeyCode(action: InputAction): Int? = keyMap[action]

 fun reset() {
 movementJoystick = JoystickState()
 lookJoystick = JoystickState()

 InputAction.values().forEach { action ->
 _inputEvents.tryEmit(InputEvent(action = action, pressed = false))
 }
 }

 // Native JNI — send input to Minecraft's input system
 private external fun nativeSendKey(keyCode: Int, pressed: Boolean)
 private external fun nativeSendMouseDelta(deltaX: Float, deltaY: Float)
 private external fun nativeSendJoystick(x: Float, y: Float)

 private fun emitMovement(action: InputAction, pressed: Boolean) {
 val keyCode = keyMap[action] ?: return
 _inputEvents.tryEmit(InputEvent(action = action, pressed = pressed))
 // nativeSendKey(keyCode, pressed)
 }
}
