package com.x.launcher.xop

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import java.io.File

data class DeviceInfo(
 val ramMb: Int,
 val cpuCores: Int,
 val abi: String,
 val androidVersion: String,
 val sdkLevel: Int,
 val isLowRam: Boolean,
 val isHighPerf: Boolean,
 val availableStorageMb: Int,
 val manufacturer: String,
 val model: String,
 val recommendedLevel: OptimizationLevel
)

object XopDeviceAnalyzer {

 fun analyze(context: Context): DeviceInfo {
 val ramMb = getTotalRamMb(context)
 val cpuCores = getCpuCores()
 val abi = getPreferredABI()
 val androidVersion = Build.VERSION.RELEASE ?: "unknown"
 val sdkLevel = Build.VERSION.SDK_INT
 val availableStorageMb = getAvailableStorageMb(context)
 val manufacturer = Build.MANUFACTURER ?: "unknown"
 val model = Build.MODEL ?: "unknown"

 val isLowRam = ramMb <= 2048
 val isHighPerf = ramMb >= 6144 && cpuCores >= 6

 val recommendedLevel = when {
 isLowRam -> OptimizationLevel.SAFE
 ramMb >= 6144 && cpuCores >= 6 -> OptimizationLevel.EXTREME
 ramMb >= 4096 && cpuCores >= 4 -> OptimizationLevel.BALANCED
 else -> OptimizationLevel.SAFE
 }

 return DeviceInfo(
 ramMb = ramMb,
 cpuCores = cpuCores,
 abi = abi,
 androidVersion = androidVersion,
 sdkLevel = sdkLevel,
 isLowRam = isLowRam,
 isHighPerf = isHighPerf,
 availableStorageMb = availableStorageMb,
 manufacturer = manufacturer,
 model = model,
 recommendedLevel = recommendedLevel
 )
 }

 private fun getTotalRamMb(context: Context): Int {
 return try {
 val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
 val memInfo = ActivityManager.MemoryInfo()
 activityManager.getMemoryInfo(memInfo)
 (memInfo.totalMem / 1048576L).toInt()
 } catch (e: Exception) {
 2048
 }
 }

 private fun getCpuCores(): Int {
 return try {
 val cpuInfo = File("/proc/cpuinfo")
 if (cpuInfo.exists()) {
 cpuInfo.useLines { lines ->
 lines.count { it.startsWith("processor") }
 }
 } else {
 Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
 }
 } catch (e: Exception) {
 Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
 }
 }

 private fun getPreferredABI(): String {
 return try {
 val abis = Build.SUPPORTED_ABIS
 when {
 abis.contains("arm64-v8a") -> "arm64-v8a"
 abis.contains("armeabi-v7a") -> "armeabi-v7a"
 abis.contains("x86_64") -> "x86_64"
 else -> "arm64-v8a"
 }
 } catch (e: Exception) {
 "arm64-v8a"
 }
 }

 private fun getAvailableStorageMb(context: Context): Int {
 return try {
 val stat = StatFs(context.filesDir.absolutePath)
 (stat.availableBytes / 1048576L).toInt()
 } catch (e: Exception) {
 1024
 }
 }

 fun isDeviceCompatible(info: DeviceInfo): Boolean {
 if (info.sdkLevel < 26) return false
 if (info.ramMb < 1500) return false
 if (!info.abi.contains("arm") && !info.abi.contains("x86")) return false
 return true
 }

 fun canRunHeavyMods(info: DeviceInfo): Boolean {
 return info.ramMb >= 4096 && info.cpuCores >= 4
 }

 fun canRunShaders(info: DeviceInfo): Boolean {
 return info.ramMb >= 3072 && info.cpuCores >= 4
 }

 fun getDeviceScore(info: DeviceInfo): Int {
 var score = 0
 score += (info.ramMb / 512) * 10
 score += info.cpuCores * 15
 score += (info.sdkLevel - 26) * 2
 if (info.abi == "arm64-v8a") score += 20
 if (info.isHighPerf) score += 30
 return score
 }
}
