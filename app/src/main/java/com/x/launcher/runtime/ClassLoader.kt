package com.x.launcher.runtime

import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile

data class LoadedClass(
 val name: String,
 val source: String,
 val isMainClass: Boolean = false
)

data class ClassLoadResult(
 val success: Boolean,
 val message: String,
 val classes: List<LoadedClass> = emptyList(),
 val mainClass: String? = null
)

object DynamicClassLoader {

 private var classLoader: URLClassLoader? = null
 private var loadedClasses = mutableMapOf<String, Class<*>>()

 fun loadGameJar(
 jarFile: File,
 librariesDir: File
 ): ClassLoadResult {
 return try {
 if (!jarFile.exists()) {
 return ClassLoadResult(
 success = false,
 message = "JAR file not found: ${jarFile.absolutePath}"
 )
 }

 // Collect all library JARs
 val libraryJars = librariesDir.walkTopDown()
 .filter { it.isFile && it.extension == "jar" }
 .toList()

 // Build URL classpath
 val urls = mutableListOf<URL>()
 urls.add(jarFile.toURI().toURL())
 libraryJars.forEach { urls.add(it.toURI().toURL()) }

 // Create classloader
 classLoader = URLClassLoader(
 urls.toTypedArray(),
 ClassLoader.getSystemClassLoader()
 )

 // Scan JAR for classes
 val classes = mutableListOf<LoadedClass>()
 var mainClass: String? = null

 JarFile(jarFile).use { jar ->
 jar.entries().toList().forEach { entry ->
 if (entry.name.endsWith(".class") && !entry.name.contains("$")) {
 val className = entry.name
 .removeSuffix(".class")
 .replace("/", ".")

 val isMain = isMainClass(className)

 classes.add(
 LoadedClass(
 name = className,
 source = jarFile.name,
 isMainClass = isMain
 )
 )

 if (isMain && mainClass == null) {
 mainClass = className
 }
 }
 }
 }

 ClassLoadResult(
 success = true,
 message = "Loaded ${classes.size} classes from ${jarFile.name}",
 classes = classes,
 mainClass = mainClass
 )
 } catch (e: Exception) {
 ClassLoadResult(
 success = false,
 message = e.message ?: "Failed to load JAR"
 )
 }
 }

 fun loadClass(className: String): Class<*>? {
 loadedClasses[className]?.let { return it }

 return try {
 val clazz = classLoader?.loadClass(className)
 if (clazz != null) {
 loadedClasses[className] = clazz
 }
 clazz
 } catch (e: Exception) {
 null
 }
 }

 fun invokeMain(className: String, args: Array<String>): Result<*> {
 return runCatching {
 val clazz = loadClass(className)
 ?: throw RuntimeException("Class not found: $className")

 val mainMethod = clazz.getMethod("main", Array<String>::class.java)
 mainMethod.invoke(null, args as Any)
 }
 }

 fun isLoaded(): Boolean = classLoader != null

 fun unload() {
 classLoader?.close()
 classLoader = null
 loadedClasses.clear()
 }

 private fun isMainClass(className: String): Boolean {
 return className.endsWith(".Main") ||
 className.endsWith(".client.main.Main") ||
 className == "net.minecraft.client.main.Main" ||
 className == "net.minecraft.client.Minecraft"
 }

 fun getLoadedClassCount(): Int = loadedClasses.size

 fun listLoadedClasses(): List<String> = loadedClasses.keys.toList()
}
