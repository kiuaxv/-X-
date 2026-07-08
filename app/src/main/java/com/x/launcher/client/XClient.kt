
package com.x.launcher.client

import android.content.Context
import com.x.launcher.xop.XOP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

object XClient {

 data class Config(
 val enabled: Boolean = true,
 val autoUpdate: Boolean = true,
 val enableShadersMenu: Boolean = true,
 val enableRenderOptions: Boolean = true,
 val enablePerformanceOverlay: Boolean = false,
 val enableModMenu: Boolean = true,
 val enableHUD: Boolean = true,
 val enableChatEnhancements: Boolean = true,
 val enableCustomControls: Boolean = true,
 val enableDynamicFPS: Boolean = true,
 val enableEntityCulling: Boolean = true,
 val heavyModCompat: Boolean = true,
 val shaderSupport: Boolean = true,
 val debugOverlay: Boolean = false,
 val customTheme: String = "dark",
 val language: String = "en"
 )

 data class Feature(
 val id: String,
 val title: String,
 val description: String,
 val enabled: Boolean,
 val category: FeatureCategory,
 val requiresResources: Boolean = false
 )

 enum class FeatureCategory {
 RENDERING,
 SHADERS,
 PERFORMANCE,
 UI,
 COMPATIBILITY,
 ACCESSIBILITY,
 DEBUG
 }

 data class ShaderPack(
 val name: String,
 val path: File,
 val isZip: Boolean,
 val isActive: Boolean = false
 )

 data class ClientMod(
 val fileName: String,
 val path: File,
 val isLoaded: Boolean = false,
 val isClientMod: Boolean = false,
 val modId: String? = null,
 val version: String? = null
 )

 data class InjectedOption(
 val id: String,
 val title: String,
 val description: String,
 val type: String,
 val defaultValue: String,
 val category: String
 )

 data class State(
 val isReady: Boolean = false,
 val isPreparing: Boolean = false,
 val config: Config = Config(),
 val features: List<Feature> = emptyList(),
 val shaderPacks: List<ShaderPack> = emptyList(),
 val activeShader: String? = null,
 val clientMods: List<ClientMod> = emptyList(),
 val injectedOptions: List<InjectedOption> = emptyList(),
 val warnings: List<String> = emptyList(),
 val errors: List<String> = emptyList()
 )

 data class RuntimeBundle(
 val success: Boolean,
 val javaArgs: List<String>,
 val envVars: Map<String, String>,
 val injectedOptionsJson: JSONObject,
 val shaderPacksJson: JSONObject,
 val featuresJson: JSONObject,
 val message: String
 )

 private val _state = MutableStateFlow(State())
 val state: StateFlow<State> = _state

 suspend fun prepareForRuntime(
 context: Context,
 gameDir: File,
 config: Config = Config()
 ): RuntimeBundle = withContext(Dispatchers.IO) {
 _state.value = State(isPreparing = true, config = config)

 if (!config.enabled) {
 _state.value = State(
 isPreparing = false,
 isReady = true,
 config = config,
 features = emptyList(),
 injectedOptions = emptyList()
 )
 return@withContext RuntimeBundle(
 success = true,
 javaArgs = emptyList(),
 envVars = emptyMap(),
 injectedOptionsJson = JSONObject(),
 shaderPacksJson = JSONObject(),
 featuresJson = JSONObject(),
 message = "X Client disabled"
 )
 }

 try {
 val features = buildFeatures(config)
 val shaderPacks = scanShaderPacks(gameDir)
 val clientMods = scanClientMods(gameDir)
 val injectedOptions = buildInjectedOptions(config)

 val javaArgs = mutableListOf<String>()
 val envVars = mutableMapOf<String, String>()
 val warnings = mutableListOf<String>()

 // Core client args
 javaArgs += listOf(
 "-Dxclient.enabled=true",
 "-Dxclient.version=1.0",
 "-Dxclient.theme=${config.customTheme}",
 "-Dxclient.language=${config.language}"
 )
 envVars["XCLIENT_ENABLED"] = "1"

 // Feature flags
 if (config.enableShadersMenu) {
 javaArgs += "-Dxclient.shaders.menu=true"
 envVars["XCLIENT_SHADERS_MENU"] = "1"
 }

 if (config.enableRenderOptions) {
 javaArgs += "-Dxclient.render.options=true"
 envVars["XCLIENT_RENDER_OPTIONS"] = "1"
 }

 if (config.enablePerformanceOverlay) {
 javaArgs += "-Dxclient.perf.overlay=true"
 envVars["XCLIENT_PERF_OVERLAY"] = "1"
 }

 if (config.enableModMenu) {
 javaArgs += "-Dxclient.modmenu=true"
 envVars["XCLIENT_MOD_MENU"] = "1"
 }

 if (config.enableHUD) {
 javaArgs += "-Dxclient.hud=true"
 envVars["XCLIENT_HUD"] = "1"
 }

 if (config.enableChatEnhancements) {
 javaArgs += "-Dxclient.chat.enhanced=true"
 envVars["XCLIENT_CHAT"] = "1"
 }

 if (config.enableCustomControls) {
 javaArgs += "-Dxclient.controls.custom=true"
 envVars["XCLIENT_CONTROLS"] = "1"
 }

 if (config.enableDynamicFPS) {
 javaArgs += listOf(
 "-Dxclient.dynamicfps=true",
 "-Dxclient.dynamicfps.unfocused=30",
 "-Dxclient.dynamicfps.background=15"
 )
 envVars["XCLIENT_DYNAMIC_FPS"] = "1"
 }

 if (config.enableEntityCulling) {
 javaArgs += "-Dxclient.entityculling=true"
 envVars["XCLIENT_ENTITY_CULLING"] = "1"
 }

 if (config.heavyModCompat) {
 javaArgs += listOf(
 "-Dxclient.modcompat.heavy=true",
 "-Dxclient.modcompat.auto=true",
 "-Dxclient.modcompat.fallback=true"
 )
 envVars["XCLIENT_HEAVY_MOD_COMPAT"] = "1"
 }

 if (config.shaderSupport) {
 val shaderDir = File(gameDir, "shaderpacks").also { it.mkdirs() }
 javaArgs += listOf(
 "-Dxclient.shader.support=true",
 "-Dxclient.shader.dir=${shaderDir.absolutePath}"
 )
 envVars["XCLIENT_SHADER_SUPPORT"] = "1"
 envVars["XCLIENT_SHADER_DIR"] = shaderDir.absolutePath

 if (shaderPacks.isNotEmpty()) {
 val active = shaderPacks.find { it.isActive } ?: shaderPacks.first()
 javaArgs += "-Dxclient.shader.active=${active.name}"
 envVars["XCLIENT_SHADER_ACTIVE"] = active.name
 }
 }

 if (config.debugOverlay) {
 javaArgs += listOf(
 "-Dxclient.debug.overlay=true",
 "-Dxclient.debug.fps=true",
 "-Dxclient.debug.memory=true",
 "-Dxclient.debug.chunk=true"
 )
 envVars["XCLIENT_DEBUG"] = "1"
 }

 // Client mods integration
 if (clientMods.isNotEmpty()) {
 javaArgs += "-Dxclient.mods.count=${clientMods.size}"
 envVars["XCLIENT_MOD_COUNT"] = clientMods.size.toString()

 val modsDir = File(gameDir, "xclient/mods").also { it.mkdirs() }
 javaArgs += "-Dxclient.mods.dir=${modsDir.absolutePath}"
 envVars["XCLIENT_MODS_DIR"] = modsDir.absolutePath

 if (config.autoUpdate) {
 javaArgs += "-Dxclient.mods.autoupdate=true"
 }
 }

 // Build JSON outputs
 val optionsJson = buildInjectedOptionsJson(injectedOptions)
 val shadersJson = buildShaderPacksJson(shaderPacks)
 val featuresJson = buildFeaturesJson(features)

 if (shaderPacks.isEmpty() && config.shaderSupport) {
 warnings += "No shader packs found in shaderpacks directory"
 }

 _state.value = State(
 isPreparing = false,
 isReady = true,
 config = config,
 features = features,
 shaderPacks = shaderPacks,
 activeShader = shaderPacks.find { it.isActive }?.name,
 clientMods = clientMods,
 injectedOptions = injectedOptions,
 warnings = warnings
 )

 RuntimeBundle(
 success = true,
 javaArgs = javaArgs.distinct(),
 envVars = envVars,
 injectedOptionsJson = optionsJson,
 shaderPacksJson = shadersJson,
 featuresJson = featuresJson,
 message = "X Client ready: ${features.size} features, ${shaderPacks.size} shaders, ${clientMods.size} mods"
 )
 } catch (e: Exception) {
 _state.value = State(
 isPreparing = false,
 isReady = false,
 config = config,
 errors = listOf(e.message ?: "X Client failed")
 )

 RuntimeBundle(
 success = true,
 javaArgs = listOf("-Dxclient.enabled=false"),
 envVars = emptyMap(),
 injectedOptionsJson = JSONObject(),
 shaderPacksJson = JSONObject(),
 featuresJson = JSONObject(),
 message = "X Client fallback: ${e.message}"
 )
 }
 }

 fun buildFeatures(config: Config): List<Feature> {
 return listOf(
 Feature(
 id = "shaders_menu",
 title = "Shaders Menu",
 description = "Built-in shader pack selector in game options",
 enabled = config.enableShadersMenu,
 category = FeatureCategory.SHADERS
 ),
 Feature(
 id = "render_options",
 title = "Render Options",
 description = "Extended rendering settings (fov, distance, mipmap)",
 enabled = config.enableRenderOptions,
 category = FeatureCategory.RENDERING
 ),
 Feature(
 id = "performance_overlay",
 title = "Performance Overlay",
 description = "FPS, memory and chunk stats overlay",
 enabled = config.enablePerformanceOverlay,
 category = FeatureCategory.PERFORMANCE
 ),
 Feature(
 id = "mod_menu",
 title = "Mod Menu",
 description = "In-game mod list and configuration",
 enabled = config.enableModMenu,
 category = FeatureCategory.UI
 ),
 Feature(
 id = "custom_hud",
 title = "Custom HUD",
 description = "Enhanced HUD with customizable elements",
 enabled = config.enableHUD,
 category = FeatureCategory.UI
 ),
 Feature(
 id = "chat_enhancements",
 title = "Chat Enhancements",
 description = "Chat history, search and formatting",
 enabled = config.enableChatEnhancements,
 category = FeatureCategory.UI
 ),
 Feature(
 id = "custom_controls",
 title = "Custom Controls",
 description = "Advanced keybinding and control schemes",
 enabled = config.enableCustomControls,
 category = FeatureCategory.UI
 ),
 Feature(
 id = "dynamic_fps",
 title = "Dynamic FPS",
 description = "Reduce FPS when unfocused to save battery",
 enabled = config.enableDynamicFPS,
 category = FeatureCategory.PERFORMANCE
 ),
 Feature(
 id = "entity_culling",
 title = "Entity Culling",
 description = "Hide entities not visible to improve performance",
 enabled = config.enableEntityCulling,
 category = FeatureCategory.PERFORMANCE
 ),
 Feature(
 id = "heavy_mod_compat",
 title = "Heavy Mod Compatibility",
 description = "Run desktop-grade mods smoothly on mobile",
 enabled = config.heavyModCompat,
 category = FeatureCategory.COMPATIBILITY
 ),
 Feature(
 id = "shader_support",
 title = "Shader Support",
 description = "Full shader pack compatibility",
 enabled = config.shaderSupport,
 category = FeatureCategory.SHADERS
 ),
 Feature(
 id = "debug_overlay",
 title = "Debug Overlay",
 description = "Detailed debug information overlay",
 enabled = config.debugOverlay,
 category = FeatureCategory.DEBUG
 )
 )
 }

 fun buildInjectedOptions(config: Config): List<InjectedOption> {
 val options = mutableListOf<InjectedOption>()

 // Client section header
 options += InjectedOption(
 id = "client_section",
 title = "Client",
 description = "X Client built-in features",
 type = "section",
 defaultValue = "true",
 category = "client"
 )

 // Shaders section
 if (config.enableShadersMenu) {
 options += InjectedOption(
 id = "client_shaders",
 title = "Shaders",
 description = "Select and configure shader packs",
 type = "shaderselector",
 defaultValue = "none",
 category = "shaders"
 )
 options += InjectedOption(
 id = "client_shader_quality",
 title = "Shader Quality",
 description = "Adjust shader rendering quality",
 type = "slider",
 defaultValue = "2",
 category = "shaders"
 )
 options += InjectedOption(
 id = "client_shader_shadow",
 title = "Shadow Quality",
 description = "Shadow map resolution",
 type = "slider",
 defaultValue = "1024",
 category = "shaders"
 )
 }

 // Render section
 if (config.enableRenderOptions) {
 options += InjectedOption(
 id = "client_render_distance",
 title = "Render Distance",
 description = "Maximum visible chunks",
 type = "slider",
 defaultValue = "12",
 category = "render"
 )
 options += InjectedOption(
 id = "client_fov",
 title = "Field of View",
 description = "Camera field of view",
 type = "slider",
 defaultValue = "70",
 category = "render"
 )
 options += InjectedOption(
 id = "client_mipmap",
 title = "Mipmap Level",
 description = "Texture mipmap detail",
 type = "slider",
 defaultValue = "4",
 category = "render"
 )
 options += InjectedOption(
 id = "client_vsync",
 title = "VSync",
 description = "Sync frames to display refresh rate",
 type = "toggle",
 defaultValue = "true",
 category = "render"
 )
 options += InjectedOption(
 id = "client_fancy_graphics",
 title = "Fancy Graphics",
 description = "Enable advanced graphics effects",
 type = "toggle",
 defaultValue = "true",
 category = "render"
 )
 }

 // Performance section
 options += InjectedOption(
 id = "client_dynamic_fps",
 title = "Dynamic FPS",
 description = "Reduce FPS when window is unfocused",
 type = "toggle",
 defaultValue = config.enableDynamicFPS.toString(),
 category = "performance"
 )
 options += InjectedOption(
 id = "client_entity_culling",
 title = "Entity Culling",
 description = "Skip rendering hidden entities",
 type = "toggle",
 defaultValue = config.enableEntityCulling.toString(),
 category = "performance"
 )
 options += InjectedOption(
 id = "client_chunk_updates",
 title = "Chunk Update Limit",
 description = "Max chunk updates per frame",
 type = "slider",
 defaultValue = "3",
 category = "performance"
 )

 // Compatibility section
 if (config.heavyModCompat) {
 options += InjectedOption(
 id = "client_heavy_mod",
 title = "Heavy Mod Mode",
 description = "Enable compatibility for heavy desktop mods",
 type = "toggle",
 defaultValue = config.heavyModCompat.toString(),
 category = "compatibility"
 )
 options += InjectedOption(
 id = "client_mod_fallback",
 title = "Mod Fallback",
 description = "Auto-disable incompatible mods",
 type = "toggle",
 defaultValue = "true",
 category = "compatibility"
 )
 }

 return options
 }

 private fun buildInjectedOptionsJson(options: List<InjectedOption>): JSONObject {
 val categories = mutableMapOf<String, JSONArray>()

 options.forEach { option ->
 val categoryArray = categories.getOrPut(option.category) { JSONArray() }
 categoryArray.put(
 JSONObject().apply {
 put("id", option.id)
 put("title", option.title)
 put("description", option.description)
 put("type", option.type)
 put("defaultValue", option.defaultValue)
 }
 )
 }

 val result = JSONObject()
 categories.forEach { (category, items) ->
 result.put(category, items)
 }

 return result
 }

 private fun buildShaderPacksJson(packs: List<ShaderPack>): JSONObject {
 val array = JSONArray()
 packs.forEach { pack ->
 array.put(
 JSONObject().apply {
 put("name", pack.name)
 put("path", pack.path.absolutePath)
 put("isZip", pack.isZip)
 put("isActive", pack.isActive)
 }
 )
 }
 return JSONObject().apply {
 put("shaderPacks", array)
 }
 }

 private fun buildFeaturesJson(features: List<Feature>): JSONObject {
 val array = JSONArray()
 features.forEach { feature ->
 array.put(
 JSONObject().apply {
 put("id", feature.id)
 put("title", feature.title)
 put("description", feature.description)
 put("enabled", feature.enabled)
 put("category", feature.category.name)
 }
 )
 }
 return JSONObject().apply {
 put("features", array)
 }
 }

 private fun scanShaderPacks(gameDir: File): List<ShaderPack> {
 val shaderDir = File(gameDir, "shaderpacks")
 if (!shaderDir.exists() || !shaderDir.isDirectory) return emptyList()

 val activeFile = File(gameDir, "xclient/active_shader.txt")
 val activeName = if (activeFile.exists()) activeFile.readText().trim() else null

 return shaderDir.listFiles()
 ?.filter { it.isFile && (it.extension == "zip" || it.isDirectory) }
 ?.map { file ->
 ShaderPack(
 name = file.nameWithoutExtension,
 path = file,
 isZip = file.extension == "zip",
 isActive = activeName == file.nameWithoutExtension
 )
 }
 .orEmpty()
 }

 private fun scanClientMods(gameDir: File): List<ClientMod> {
 val modsDir = File(gameDir, "xclient/mods")
 if (!modsDir.exists() || !modsDir.isDirectory) return emptyList()

 return modsDir.listFiles()
 ?.filter { it.isFile && it.extension == "jar" }
 ?.map { file ->
 val (modId, version) = try {
 parseModMetadata(file)
 } catch (_: Exception) {
 null to null
 }

 ClientMod(
 fileName = file.name,
 path = file,
 isLoaded = false,
 isClientMod = true,
 modId = modId,
 version = version
 )
 }
 .orEmpty()
 }

 private fun parseModMetadata(jarFile: File): Pair<String?, String?> {
 try {
 ZipFile(jarFile).use { zip ->
 // Try fabric.mod.json
 val fabricEntry = zip.getEntry("fabric.mod.json")
 if (fabricEntry != null) {
 val json = zip.getInputStream(fabricEntry).bufferedReader().readText()
 val obj = JSONObject(json)
 return obj.optString("id", null) to obj.optString("version", null)
 }

 // Try META-INF/MANIFEST.MF
 val manifestEntry = zip.getEntry("META-INF/MANIFEST.MF")
 if (manifestEntry != null) {
 val manifest = zip.getInputStream(manifestEntry).bufferedReader().readText()
 val id = manifest.lines().find { it.startsWith("Implementation-Title:") }?.substringAfter(":")?.trim()
 val ver = manifest.lines().find { it.startsWith("Implementation-Version:") }?.substringAfter(":")?.trim()
 return id to ver
 }
 }
 } catch (_: Exception) {
 // Ignore
 }

 return null to null
 }

 fun setActiveShader(gameDir: File, shaderName: String) {
 val file = File(gameDir, "xclient/active_shader.txt")
 file.parentFile?.mkdirs()
 file.writeText(shaderName)

 _state.value = _state.value.copy(
 activeShader = shaderName,
 shaderPacks = _state.value.shaderPacks.map {
 it.copy(isActive = it.name == shaderName)
 }
 )
 }

 fun updateConfig(newConfig: Config) {
 _state.value = _state.value.copy(config = newConfig)
 }

 fun reset() {
 _state.value = State()
 }
}
