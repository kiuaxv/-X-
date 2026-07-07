package com.x.launcher.core

import java.io.File

data class SkinItem(
 val fileName: String,
 val filePath: String,
 val isCustom: Boolean = true
)

object SkinManager {

 fun getSkinsDirectory(gameDir: File): File {
 val skinsDir = File(gameDir, "skins")
 if (!skinsDir.exists()) {
 skinsDir.mkdirs()
 }
 return skinsDir
 }

 fun listSkins(gameDir: File): List<SkinItem> {
 val skinsDir = getSkinsDirectory(gameDir)

 return skinsDir.listFiles()
 ?.filter { it.isFile && (it.extension == "png") }
 ?.map {
 SkinItem(
 fileName = it.nameWithoutExtension,
 filePath = it.absolutePath
 )
 }
 ?: emptyList()
 }

 fun importSkin(sourceFile: File, gameDir: File): Result<File> {
 return runCatching {
 if (sourceFile.extension != "png") {
 throw IllegalArgumentException("Skin must be a PNG file")
 }

 val skinsDir = getSkinsDirectory(gameDir)
 val targetFile = File(skinsDir, sourceFile.name)

 sourceFile.copyTo(targetFile, overwrite = true)
 targetFile
 }
 }

 fun removeSkin(skinFileName: String, gameDir: File): Boolean {
 val skinsDir = getSkinsDirectory(gameDir)
 val targetFile = File(skinsDir, "$skinFileName.png")
 return targetFile.exists() && targetFile.delete()
 }

 fun setSkin(skinFilePath: String, username: String): Boolean {
 val skinFile = File(skinFilePath)
 if (!skinFile.exists()) return false
 // TODO: Upload skin to Minecraft account via API
 // For now this just validates the file exists
 return true
 }

 fun getDefaultSkin(): SkinItem {
 return SkinItem(
 fileName = "Steve",
 filePath = "",
 isCustom = false
 )
 }

 fun isSkinInstalled(skinFileName: String, gameDir: File): Boolean {
 val skinsDir = getSkinsDirectory(gameDir)
 return File(skinsDir, "$skinFileName.png").exists()
 }
}
