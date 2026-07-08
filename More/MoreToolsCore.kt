package com.x.launcher.more

import android.content.Context
import android.app.ActivityManager
import com.x.launcher.more.MoreFeatures.getBatteryLevel
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object MoreToolsCore {

 // ════════════════════════════════════════════════════════════════
 // CRASH ANALYZER
 // ════════════════════════════════════════════════════════════════

 object CrashAnalyzer {

 data class CrashReport(
 val file: File,
 val timestamp: String,
 val exception: String,
 val stackTrace: String,
 val possibleCause: String,
 val suggestedFix: String
 )

 private val knownPatterns = mapOf(
 "OutOfMemoryError" to Pair(
 "Not enough RAM allocated to JVM",
 "Increase max memory in XOP settings or reduce render distance"
 ),
 "NoSuchMethodError" to Pair(
 "Mod version mismatch or missing dependency",
 "Update the mod or install the required dependency library"
 ),
 "NoSuchFieldError" to Pair(
 "Mod compiled against different Minecraft version",
 "Use a mod version matching your Minecraft version"
 ),
 "ClassNotFoundException" to Pair(
 "Missing library or mod",
 "Check missing dependencies or reinstall libraries"
 ),
 "NullPointerException" to Pair(
 "Bug in mod or game code",
 "Try removing recently added mods one by one"
 ),
 "ConcurrentModificationException" to Pair(
 "Mod thread safety issue",
 "Update the mod or report to the mod author"
 ),
 "UnsatisfiedLinkError" to Pair(
 "Missing native library (.so)",
 "Reinstall natives or check ABI compatibility"
 ),
 "ShaderCompilationException" to Pair(
 "Shader pack failed to compile",
 "Try a different shader pack or update gl4es"
 ),
 "GL_OUT_OF_MEMORY" to Pair(
 "GPU memory exhausted",
 "Reduce render distance, shader quality, or texture resolution"
 ),
 "java.lang.OutOfMemoryError: Metaspace" to Pair(
 "Too many mods loaded",
 "Increase Metaspace size or reduce mod count"
 )
 )

 fun analyzeCrashDir(gameDir: File): List<CrashReport> {
 val crashDir = File(gameDir, "crash-reports")
 if (!crashDir.exists() || !crashDir.isDirectory) return emptyList()

 return crashDir.listFiles()
 ?.filter { it.isFile && it.name.startsWith("crash-") && it.extension == "txt" }
 ?.sortedByDescending { it.lastModified() }
 ?.take(10)
 ?.map { parseCrashFile(it) }
 .orEmpty()
 }

 private fun parseCrashFile(file: File): CrashReport {
 val content = file.readText()
 val timestamp = file.name.removePrefix("crash-").removeSuffix(".txt")

 val exceptionLine = content.lines().find { it.contains("Exception") || it.contains("Error") }
 val exception = exceptionLine?.substringAfter("// ")?.trim() ?: "Unknown"

 val stackTrace = content.substringAfter("Stacktrace:")
 .take(2000)

 val (cause, fix) = knownPatterns.entries.find { (pattern, _) ->
 content.contains(pattern, ignoreCase = true) ||
 exception.contains(pattern, ignoreCase = true)
 }?.value ?: ("Unknown" to "Check the full crash log for details")

 return CrashReport(
 file = file,
 timestamp = timestamp,
 exception = exception,
 stackTrace = stackTrace,
 possibleCause = cause,
 suggestedFix = fix
 )
 }

 fun clearOldCrashes(gameDir: File, keepCount: Int = 5) {
 val crashDir = File(gameDir, "crash-reports")
 if (!crashDir.exists()) return

 crashDir.listFiles()
 ?.filter { it.isFile && it.name.startsWith("crash-") }
 ?.sortedByDescending { it.lastModified() }
 ?.drop(keepCount)
 ?.forEach { it.delete() }
 }
 }

 // ════════════════════════════════════════════════════════════════
 // REPAIR MODE
 // ════════════════════════════════════════════════════════════════

 object RepairMode {

 data class RepairResult(
 val checkedFiles: Int = 0,
 val repairedFiles: Int = 0,
 val deletedFiles: Int = 0,
 val errors: List<String> = emptyList()
 )

 fun repairVersion(gameDir: File, versionId: String): RepairResult {
 var checked = 0
 var repaired = 0
 var deleted = 0
 val errors = mutableListOf<String>()

 val versionJar = File(gameDir, "versions/$versionId/$versionId.jar")
 checked++
 if (versionJar.exists() && versionJar.length() < 1000) {
 versionJar.delete()
 deleted++
 errors += "Version jar was corrupted, deleted for re-download"
 }

 val libDir = File(gameDir, "libraries")
 if (libDir.exists()) {
 libDir.walkTopDown()
 .filter { it.isFile && it.extension == "jar" }
 .forEach { jar ->
 checked++
 if (jar.length() < 100) {
 jar.delete()
 deleted++
 repaired++
 }
 }
 }

 val nativesDir = File(gameDir, "natives")
 if (nativesDir.exists()) {
 nativesDir.listFiles()?.forEach { so ->
 checked++
 if (so.length() < 1000) {
 so.delete()
 deleted++
 }
 }
 }

 val assetsIndex = File(gameDir, "assets/indexes/$versionId.json")
 checked++
 if (assetsIndex.exists() && assetsIndex.length() < 10) {
 assetsIndex.delete()
 deleted++
 }

 val modsDir = File(gameDir, "mods")
 if (modsDir.exists()) {
 modsDir.listFiles()?.forEach { mod ->
 checked++
 if (mod.length() < 100) {
 mod.delete()
 deleted++
 errors += "Corrupted mod deleted: ${mod.name}"
 }
 }
 }

 return RepairResult(
 checkedFiles = checked,
 repairedFiles = repaired,
 deletedFiles = deleted,
 errors = errors
 )
 }
 }

 // ════════════════════════════════════════════════════════════════
 // MOD CONFLICT DETECTOR
 // ════════════════════════════════════════════════════════════════

 object ModConflictDetector {

 data class ConflictResult(
 val conflicts: List<ModConflict>,
 val duplicates: List<ModDuplicate>,
 val riskLevel: RiskLevel
 )

 data class ModConflict(
 val mod1: String,
 val mod2: String,
 val reason: String,
 val severity: String
 )

 data class ModDuplicate(
 val modId: String,
 val files: List<String>,
 val versions: List<String>
 )

 enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

 private val knownConflicts = listOf(
 Triple("optifine", "iris", "Both are shader mods, will conflict"),
 Triple("optifine", "sodium", "OptiFine replaces rendering engine"),
 Triple("rubidium", "embeddium", "Both replace rendering engine"),
 Triple("optifine", "oculus", "Duplicate shader compatibility layer"),
 Triple("indium", "sodium", "Indium requires specific Sodium version"),
 Triple("lithium", "cardboard", "Incompatible optimization mods")
 )

 fun scanMods(gameDir: File): ConflictResult {
 val modsDir = File(gameDir, "mods")
 if (!modsDir.exists() || !modsDir.isDirectory) {
 return ConflictResult(emptyList(), emptyList(), RiskLevel.LOW)
 }

 val modFiles = modsDir.listFiles()
 ?.filter { it.isFile && (it.extension == "jar" || it.extension == "zip") }
 .orEmpty()

 val modNames = modFiles.map { it.name.lowercase() }
 val conflicts = mutableListOf<ModConflict>()

 for ((mod1, mod2, reason) in knownConflicts) {
 val has1 = modNames.any { it.contains(mod1) }
 val has2 = modNames.any { it.contains(mod2) }
 if (has1 && has2) {
 conflicts += ModConflict(mod1, mod2, reason, "high")
 }
 }

 val duplicates = mutableListOf<ModDuplicate>()
 val modIds = mutableMapOf<String, MutableList<Pair<String, String>>>()

 modFiles.forEach { file ->
 val id = file.nameWithoutExtension.lowercase()
 .replace(Regex("-\\d.*"), "")
 .replace(Regex("_\\d.*"), "")
 modIds.getOrPut(id) { mutableListOf() }.add(file.name to id)
 }

 modIds.forEach { (id, files) ->
 if (files.size > 1) {
 duplicates += ModDuplicate(
 modId = id,
 files = files.map { it.first },
 versions = files.map { it.second }
 )
 }
 }

 val riskLevel = when {
 conflicts.size >= 3 -> RiskLevel.CRITICAL
 conflicts.size >= 1 -> RiskLevel.HIGH
 duplicates.size >= 2 -> RiskLevel.MEDIUM
 duplicates.size >= 1 -> RiskLevel.LOW
 else -> RiskLevel.LOW
 }

 return ConflictResult(conflicts, duplicates, riskLevel)
 }
 }

 // ════════════════════════════════════════════════════════════════
 // DEPENDENCY DETECTOR
 // ════════════════════════════════════════════════════════════════

 object DependencyDetector {

 data class DependencyResult(
 val missing: List<MissingDependency>,
 val satisfied: Int,
 val total: Int
 )

 data class MissingDependency(
 val modId: String,
 val requiredDependency: String,
 val requiredVersion: String?,
 val downloadHint: String
 )

 private val knownDependencies = mapOf(
 "iris" to listOf("sodium"),
 "oculus" to listOf("rubidium", "embeddium"),
 "indium" to listOf("sodium"),
 "create" to listOf("flywheel"),
 "jei" to emptyList(),
 "cloth-config" to emptyList(),
 "architectury" to emptyList(),
 "cloth-api" to listOf("cloth-config"),
 "rei" to emptyList(),
 "jeed" to listOf("jei")
 )

 fun checkDependencies(gameDir: File): DependencyResult {
 val modsDir = File(gameDir, "mods")
 if (!modsDir.exists()) return DependencyResult(emptyList(), 0, 0)

 val modFiles = modsDir.listFiles()
 ?.filter { it.isFile && it.extension == "jar" }
 .orEmpty()

 val installedMods = modFiles.map { file ->
 file.nameWithoutExtension.lowercase()
 .replace(Regex("-\\d.*"), "")
 .replace(Regex("_\\d.*"), "")
 }.toSet()

 val missing = mutableListOf<MissingDependency>()
 var total = 0

 installedMods.forEach { modId ->
 val deps = knownDependencies[modId]
 if (deps != null) {
 total += deps.size
 deps.forEach { dep ->
 if (!installedMods.contains(dep)) {
 missing += MissingDependency(
 modId = modId,
 requiredDependency = dep,
 requiredVersion = null,
 downloadHint = "Download $dep from Modrinth or CurseForge"
 )
 }
 }
 }
 }

 return DependencyResult(
 missing = missing,
 satisfied = total - missing.size,
 total = total
 )
 }
 }

 // ════════════════════════════════════════════════════════════════
 // SMART UPDATE
 // ════════════════════════════════════════════════════════════════

 object SmartUpdate {

 data class UpdateResult(
 val checkedItems: Int = 0,
 val updatedItems: Int = 0,
 val skippedItems: Int = 0,
 val errors: List<String> = emptyList()
 )

 private val fileHashes = ConcurrentHashMap<String, String>()

 fun checkForUpdates(gameDir: File): List<File> {
 val outdated = mutableListOf<File>()

 val libDir = File(gameDir, "libraries")
 if (libDir.exists()) {
 libDir.walkTopDown()
 .filter { it.isFile && it.extension == "jar" }
 .forEach { jar ->
 val hash = hashFile(jar)
 val cached = fileHashes[jar.absolutePath]

 if (cached != null && cached != hash) {
 outdated.add(jar)
 }

 fileHashes[jar.absolutePath] = hash
 }
 }

 return outdated
 }

 private fun hashFile(file: File): String {
 return try {
 val digest = java.security.MessageDigest.getInstance("SHA-1")
 file.inputStream().use { input ->
 val buffer = ByteArray(8192)
 var read: Int
 while (input.read(buffer).also { read = it } != -1) {
 digest.update(buffer, 0, read)
 }
 }
 digest.digest().joinToString("") { "%02x".format(it) }
 } catch (_: Exception) {
 ""
 }
 }
 }
}
