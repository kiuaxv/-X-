package com.x.launcher.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GameVersion(
 val id: String,
 val type: VersionType,
 val url: String,
 val releaseDate: String,
 val sizeMb: Int,
 var isDownloaded: Boolean = false,
 var downloadProgress: Float = 0f
)

enum class VersionType { RELEASE, SNAPSHOT, BETA, ALPHA }

object VersionManager {

 private val _versions = MutableStateFlow<List<GameVersion>>(emptyList())
 val versions: StateFlow<List<GameVersion>> = _versions

 private val _selectedVersion = MutableStateFlow<GameVersion?>(null)
 val selectedVersion: StateFlow<GameVersion?> = _selectedVersion

 init {
 loadDefaultVersions()
 }

 private fun loadDefaultVersions() {
 _versions.value = listOf(
 GameVersion(
 id = "1.20.4",
 type = VersionType.RELEASE,
 url = "https://piston-data.mojang.com/v1/packages/...",
 releaseDate = "2023-12-07",
 sizeMb = 850
 ),
 GameVersion(
 id = "1.19.2",
 type = VersionType.RELEASE,
 url = "https://piston-data.mojang.com/v1/packages/...",
 releaseDate = "2022-08-05",
 sizeMb = 780
 ),
 GameVersion(
 id = "1.18.2",
 type = VersionType.RELEASE,
 url = "https://piston-data.mojang.com/v1/packages/...",
 releaseDate = "2022-02-28",
 sizeMb = 720
 ),
 GameVersion(
 id = "23w14a",
 type = VersionType.SNAPSHOT,
 url = "https://piston-data.mojang.com/v1/packages/...",
 releaseDate = "2023-04-05",
 sizeMb = 800
 ),
 GameVersion(
 id = "b1.7.3",
 type = VersionType.BETA,
 url = "https://piston-data.mojang.com/v1/packages/...",
 releaseDate = "2011-06-30",
 sizeMb = 320
 )
 )
 }

 fun selectVersion(version: GameVersion) {
 _selectedVersion.value = version
 }

 fun getDownloadedVersions(): List<GameVersion> {
 return _versions.value.filter { it.isDownloaded }
 }

 fun getVersionsByType(type: VersionType): List<GameVersion> {
 return _versions.value.filter { it.type == type }
 }

 fun markAsDownloaded(versionId: String) {
 _versions.value = _versions.value.map {
 if (it.id == versionId) it.copy(
 isDownloaded = true,
 downloadProgress = 1f
 ) else it
 }
 }

 fun updateProgress(versionId: String, progress: Float) {
 _versions.value = _versions.value.map {
 if (it.id == versionId) it.copy(downloadProgress = progress) else it
 }
 }
}
