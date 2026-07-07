package com.x.launcher.runtime

import android.opengl.GLSurfaceView
import android.view.Surface
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

data class RendererConfig(
 val useVbo: Boolean = true,
 val useVao: Boolean = false,
 val maxFps: Int = 60,
 val antiAliasing: Boolean = false,
 val renderDistance: Int = 8,
 val vsync: Boolean = true
)

data class FrameStats(
 var fps: Int = 0,
 var frameTimeMs: Float = 0f,
 var drawCalls: Int = 0,
 var triangles: Int = 0
)

object RendererBridge {

 private var glSurfaceView: GLSurfaceView? = null
 private var renderer: GameRenderer? = null
 private var config: RendererConfig = RendererConfig()
 private var frameStats: FrameStats = FrameStats()
 private var lastFrameTime: Long = 0L
 private var frameCount: Int = 0
 private var fpsUpdateTime: Long = 0L

 fun attach(surfaceView: GLSurfaceView, rendererConfig: RendererConfig) {
 config = rendererConfig
 glSurfaceView = surfaceView

 glSurfaceView?.apply {
 setEGLContextClientVersion(3)
 setEGLConfigChooser(8, 8, 8, 8, 16, 0)
 preserveEGLContextOnPause = true
 renderer = GameRenderer()
 setRenderer(renderer)
 renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
 }
 }

 fun detach() {
 glSurfaceView = null
 renderer = null
 }

 fun pauseRendering() {
 glSurfaceView?.onPause()
 }

 fun resumeRendering() {
 glSurfaceView?.onResume()
 }

 fun updateConfig(newConfig: RendererConfig) {
 config = newConfig
 }

 fun getFrameStats(): FrameStats = frameStats

 fun requestRender() {
 glSurfaceView?.requestRender()
 }

 private fun onFrameRendered() {
 val now = System.nanoTime()
 val delta = now - lastFrameTime
 lastFrameTime = now

 frameStats.frameTimeMs = delta / 1_000_000f

 frameCount++
 if (now - fpsUpdateTime >= 1_000_000_000L) {
 frameStats.fps = frameCount
 frameCount = 0
 fpsUpdateTime = now
 }
 }

 // Native JNI bridge — these call into the C++ renderer layer
 private external fun nativeInit(surface: Surface, width: Int, height: Int)
 private external fun nativeResize(width: Int, height: Int)
 private external fun nativeDrawFrame()
 private external fun nativeShutdown()
 private external fun nativeSetRenderDistance(chunks: Int)
 private external fun nativeSetVsync(enabled: Boolean)
 private external fun nativeGetDrawCalls(): Int
 private external fun nativeGetTriangleCount(): Int

 fun loadNativeLibrary() {
 try {
 System.loadLibrary("xrenderer")
 } catch (e: UnsatisfiedLinkError) {
 // Native library not yet compiled
 // This will be linked when we build the C++ renderer
 }
 }
}

class GameRenderer : GLSurfaceView.Renderer {

 private var width = 0
 private var height = 0

 override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
 // Initialize native renderer with OpenGL ES 3.0 context
 RendererBridge.loadNativeLibrary()

 val surface = null // TODO: Get actual Surface from EGL
 // RendererBridge.nativeInit(surface, width, height)
 }

 override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
 this.width = width
 this.height = height
 // RendererBridge.nativeResize(width, height)
 }

 override fun onDrawFrame(gl: GL10?) {
 // RendererBridge.nativeDrawFrame()
 // frameStats.drawCalls = RendererBridge.nativeGetDrawCalls()
 // frameStats.triangles = RendererBridge.nativeGetTriangleCount()
 // RendererBridge.onFrameRendered()
 }
}
