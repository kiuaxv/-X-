#include <jni.h>
#include <android/log.h>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <cstring>
#include <chrono>
#include <mutex>
#include <vector>

#define LOG_TAG "XRenderer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

static EGLDisplay eglDisplay = EGL_NO_DISPLAY;
static EGLSurface eglSurface = EGL_NO_SURFACE;
static EGLContext eglContext = EGL_NO_CONTEXT;
static EGLConfig eglConfig = nullptr;
static ANativeWindow* nativeWindow = nullptr;
static int surfaceWidth = 0;
static int surfaceHeight = 0;
static bool isInitialized = false;
static bool isRunning = false;
static std::mutex renderMutex;

static int drawCalls = 0;
static int triangleCount = 0;
static auto lastFrameTime = std::chrono::high_resolution_clock::now();

// Frame stats
extern "C" {

bool initEGL(ANativeWindow* window, int width, int height) {
 nativeWindow = window;
 surfaceWidth = width;
 surfaceHeight = height;

 eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
 if (eglDisplay == EGL_NO_DISPLAY) {
 LOGE("Failed to get EGL display");
 return false;
 }

 EGLint major, minor;
 if (!eglInitialize(eglDisplay, &major, &minor)) {
 LOGE("Failed to initialize EGL");
 return false;
 }

 LOGI("EGL initialized: version %d.%d", major, minor);

 // Configure EGL
 const EGLint configAttribs[] = {
 EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
 EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
 EGL_RED_SIZE, 8,
 EGL_GREEN_SIZE, 8,
 EGL_BLUE_SIZE, 8,
 EGL_ALPHA_SIZE, 8,
 EGL_DEPTH_SIZE, 16,
 EGL_STENCIL_SIZE, 0,
 EGL_NONE
 };

 EGLint numConfigs;
 if (!eglChooseConfig(eglDisplay, configAttribs, &eglConfig, 1, &numConfigs) || numConfigs == 0) {
 LOGE("Failed to choose EGL config");
 return false;
 }

 // Create surface
 eglSurface = eglCreateWindowSurface(eglDisplay, eglConfig, nativeWindow, nullptr);
 if (eglSurface == EGL_NO_SURFACE) {
 LOGE("Failed to create EGL surface");
 return false;
 }

 // Create context
 const EGLint contextAttribs[] = {
 EGL_CONTEXT_CLIENT_VERSION, 3,
 EGL_NONE
 };

 eglContext = eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT, contextAttribs);
 if (eglContext == EGL_NO_CONTEXT) {
 LOGE("Failed to create EGL context");
 return false;
 }

 if (!eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
 LOGE("Failed to make EGL context current");
 return false;
 }

 eglSwapInterval(eglDisplay, 1); // Enable vsync

 glViewport(0, 0, surfaceWidth, surfaceHeight);

 LOGI("EGL context ready: %dx%d", surfaceWidth, surfaceHeight);

 isInitialized = true;
 return true;
}

void destroyEGL() {
 std::lock_guard<std::mutex> lock(renderMutex);

 if (eglDisplay != EGL_NO_DISPLAY) {
 eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);

 if (eglContext != EGL_NO_CONTEXT) {
 eglDestroyContext(eglDisplay, eglContext);
 eglContext = EGL_NO_CONTEXT;
 }

 if (eglSurface != EGL_NO_SURFACE) {
 eglDestroySurface(eglDisplay, eglSurface);
 eglSurface = EGL_NO_SURFACE;
 }

 eglTerminate(eglDisplay);
 eglDisplay = EGL_NO_DISPLAY;
 }

 if (nativeWindow != nullptr) {
 ANativeWindow_release(nativeWindow);
 nativeWindow = nullptr;
 }

 isInitialized = false;
 isRunning = false;
 LOGI("EGL destroyed");
}

void resizeSurface(int width, int height) {
 std::lock_guard<std::mutex> lock(renderMutex);
 surfaceWidth = width;
 surfaceHeight = height;

 if (isInitialized) {
 glViewport(0, 0, surfaceWidth, surfaceHeight);
 LOGI("Surface resized: %dx%d", width, height);
 }
}

void renderFrame() {
 std::lock_guard<std::mutex> lock(renderMutex);

 if (!isInitialized || !isRunning) return;

 glClearColor(0.02f, 0.02f, 0.04f, 1.0f);
 glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

 // TODO: Draw Minecraft scene here
 // This is where the actual game rendering happens
 // The game's draw calls are translated from OpenGL to OpenGL ES

 eglSwapBuffers(eglDisplay, eglSurface);

 // Update frame stats
 auto now = std::chrono::high_resolution_clock::now();
 lastFrameTime = now;
}

void setRenderDistance(int chunks) {
 std::lock_guard<std::mutex> lock(renderMutex);
 LOGI("Render distance set to %d chunks", chunks);
}

void setVsync(bool enabled) {
 if (eglDisplay != EGL_NO_DISPLAY) {
 eglSwapInterval(eglDisplay, enabled ? 1 : 0);
 }
}

int getDrawCalls() {
 return drawCalls;
}

int getTriangleCount() {
 return triangleCount;
}

JNIEXPORT void JNICALL
Java_com_x_launcher_runtime_RendererBridge_nativeInit(
 JNIEnv* env, jobject thiz, jobject surface, jint width, jint height
) {
 ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
 if (window == nullptr) {
 LOGE("Failed to get native window from surface");
 return;
 }

 if (initEGL(window, width, height)) {
 isRunning = true;
 }
}

JNIEXPORT void JNICALL
Java_com_x_launcher_runtime_RendererBridge_nativeResize(
 JNIEnv* env, jobject thiz, jint width, jint height
) {
 resizeSurface(width, height);
}

JNIEXPORT void JNICALL
Java_com_x_launcher_runtime_RendererBridge_nativeDrawFrame(
 JNIEnv* env, jobject thiz
) {
 renderFrame();
}

JNIEXPORT void JNICALL
Java_com_x_launcher_runtime_RendererBridge_nativeShutdown(
 JNIEnv* env, jobject thiz
) {
 destroyEGL();
}

JNIEXPORT void JNICALL
Java_com_x_launcher_runtime_RendererBridge_nativeSetRenderDistance(
 JNIEnv* env, jobject thiz, jint chunks
) {
 setRenderDistance(chunks);
}

JNIEXPORT void JNICALL
Java_com_x_launcher_runtime_RendererBridge_nativeSetVsync(
 JNIEnv* env, jobject thiz, jboolean enabled
) {
 setVsync(enabled);
}

JNIEXPORT jint JNICALL
Java_com_x_launcher_runtime_RendererBridge_nativeGetDrawCalls(
 JNIEnv* env, jobject thiz
) {
 return getDrawCalls();
}

JNIEXPORT jint JNICALL
Java_com_x_launcher_runtime_RendererBridge_nativeGetTriangleCount(
 JNIEnv* env, jobject thiz
) {
 return getTriangleCount();
}

JNIEXPORT void JNICALL
Java_com_x_launcher_runtime_InputBridge_nativeSendKey(
 JNIEnv* env, jobject thiz, jint keyCode, jboolean pressed
) {
 // TODO: Forward key event to Minecraft's input system
 LOGI("Key: %d %s", keyCode, pressed ? "DOWN" : "UP");
}

JNIEXPORT void JNICALL
Java_com_x_launcher_runtime_InputBridge_nativeSendMouseDelta(
 JNIEnv* env, jobject thiz, jfloat deltaX, jfloat deltaY
) {
 // TODO: Forward mouse delta to Minecraft's camera
}

JNIEXPORT void JNICALL
Java_com_x_launcher_runtime_InputBridge_nativeSendJoystick(
 JNIEnv* env, jobject thiz, jfloat x, jfloat y
) {
 // TODO: Forward joystick state to Minecraft's movement
}

} // extern "C"
