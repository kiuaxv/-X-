# X Launcher ProGuard Rules

# Keep all classes in the runtime package
-keep class com.x.launcher.runtime.** { *; }

# Keep JNI methods
-keepclasseswithmembernames class * {
 native <methods>;
}

# Keep Minecraft classes loaded dynamically
-keep class net.minecraft.** { *; }
-keep class com.mojang.** { *; }

# Coroutines
-keepclassmembernames class kotlinx.** { *; }

# Compose
-keep class androidx.compose.** { *; }
PATH: gradle/libs.versions.toml
[versions]
agp = "8.6.1"
kotlin = "2.0.20"
composeBom = "2024.09.00"
compileSdk = "34
minSdk = "26
targetSdk = "34

[libraries]
core-ktx = "androidx.core:core-ktx:1.13.1"
activity-compose = "androidx.activity:activity-compose:1.9.2"
lifecycle-runtime = "androidx.lifecycle:lifecycle-runtime-ktx:2.8.6"
compose-bom = "androidx.compose:compose-bom:2024.09.00"
compose-ui = "androidx.compose.ui:ui"
compose-material3 = "androidx.compose.material3:material3"
coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1"
coil = "io.coil-kt:coil-compose:2.7.0"
datastore = "androidx.datastore:datastore-preferences:1.1.1"

[plugins]
android-application = "com.android.application:8.6.1"
kotlin-android = "org.jetbrains.kotlin.android:2.0.20"
kotlin-compose = "org.jetbrains.kotlin.plugin.compose:2.0.20"
PATH: build.gradle.kts (root)
plugins {
 id("com.android.application") version "8.6.1" apply false
 id("org.jetbrains.kotlin.android") version "2.0.20" apply false
 id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}

tasks.register("clean", Delete::class) {
 delete(rootProject.layout.buildDirectory)
}
PATH: settings.gradle.kts
pluginManagement {
 repositories {
 google()
 mavenCentral()
 gradlePluginPortal()
 }
}

dependencyResolutionManagement {
 repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
 repositories {
 google()
 mavenCentral()
 }
}

rootProject.name = "X-Launcher"
include(":app")

