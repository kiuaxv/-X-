pluginManagement {
 repositories {
 google()
 mavenCentral()
 gradlePluginPortal()
 }
}

dependencyResolutionManagement {
 repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
 repositories {
 google()
 mavenCentral()

 maven("https://maven.fabricmc.net/")
 maven("https://maven.minecraftforge.net/")
 maven("https://maven.neoforged.net/releases/")
 maven("https://api.modrinth.com/maven")
 maven("https://jitpack.io")
 }
}

rootProject.name = "X Launcher"
include(":app")
