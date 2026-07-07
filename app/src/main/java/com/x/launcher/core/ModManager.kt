package com.x.launcher.core

import java.io.File

data class ModItem(
 val fileName: String,
 val filePath: String,
 val sizeBytes: Long,
 val enabled: Boolean = true
)

object ModManager {

 fun getModsDirectory(gameDir: File): File {
 val modsDir = File(gameDir, "mods")
 if (!modsDir.exists()) {
 modsDir.mkdirs()
 }
 return modsDir
 }

 fun listMods(gameDir: File): List<ModItem> {
 val modsDir = getModsDirectory(gameDir)

 return modsDir.listFiles()
 ?.filter { it.isFile && (it.extension == "jar" || it.extension == "zip") }
 ?.map {
 ModItem(
 fileName = it.name,
 filePath = it.absolutePath,
 sizeBytes = it.length(),
 enabled = !it.name.endsWith(".disabled.jar")
 )
 }
 ?: emptyList()
 }

 fun importMod(sourceFile: File, gameDir: File): Result<File> {
 return runCatching {
 val modsDir = getModsDirectory(gameDir)
 val targetFile = File(modsDir, sourceFile.name)

 sourceFile.copyTo(targetFile, overwrite = true)
 targetFile
 }
 }

 fun removeMod(modFileName: String, gameDir: File): Boolean {
 val modsDir = getModsDirectory(gameDir)
 val targetFile = File(modsDir, modFileName)
 return targetFile.exists() && targetFile.delete()
 }

 fun enableMod(modFileName: String, gameDir: File): Boolean {
 val modsDir = getModsDirectory(gameDir)
 val disabledFile = File(modsDir, modFileName)

 if (!disabledFile.exists()) return false
 if (!disabledFile.name.endsWith(".disabled.jar")) return true

 val enabledName = disabledFile.name.removeSuffix(".disabled.jar") + ".jar"
 val enabledFile = File(modsDir, enabledName)

 return disabledFile.renameTo(enabledFile)
 }

 fun disableMod(modFileName: String, gameDir: File): Boolean {
 val modsDir = getModsDirectory(gameDir)
 val modFile = File(modsDir, modFileName)

 if (!modFile.exists()) return false
 if (modFile.name.endsWith(".disabled.jar")) return true
 if (modFile.extension != "jar") return false

 val baseName = modFile.name.removeSuffix(".jar")
 val disabledFile = File(modsDir, "$baseName.disabled.jar")

 return modFile.renameTo(disabledFile)
 }

 fun isModInstalled(modFileName: String, gameDir: File): Boolean {
 val modsDir = getModsDirectory(gameDir)
 return File(modsDir, modFileName).exists()
 }
}
