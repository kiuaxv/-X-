plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}

android {
 namespace = "com.x.launcher"
 compileSdk = 34

 defaultConfig {
 applicationId = "com.x.launcher"
 minSdk = 26
 targetSdk = 34
 versionCode = 1
 versionName = "1.0.0-alpha"

 testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

 ndk {
 abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
 }

 externalNativeBuild {
 cmake {
 cppFlags += listOf("-std=c++17", "-fexceptions", "-frtti")
 arguments += "-DANDROID_STL=c++_shared"
 }
 }
 }

 buildTypes {
 release {
 isMinifyEnabled = true
 proguardFiles(
 getDefaultProguardFile("proguard-android-optimize.txt"),
 "proguard-rules.pro"
 )
 signingConfig = signingConfigs.getByName("debug")
 }
 debug {
 isMinifyEnabled = false
 applicationIdSuffix = ".debug"
 versionNameSuffix = "-debug"
 }
 }

 compileOptions {
 sourceCompatibility = JavaVersion.VERSION_17
 targetCompatibility = JavaVersion.VERSION_17
 }

 kotlinOptions {
 jvmTarget = "17"
 }

 buildFeatures {
 compose = true
 buildConfig = true
 }

 packaging {
 resources {
 excludes += "/META-INF/{AL2.0,LGPL2.1}"
 excludes += "/META-INF/DEPENDENCIES"
 excludes += "/META-INF/LICENSE"
 excludes += "/META-INF/LICENSE.txt"
 excludes += "/META-INF/NOTICE"
 }

 jniLibs {
 useLegacyPackaging = true
 }
 }

 externalNativeBuild {
 cmake {
 path = file("src/main/cpp/CMakeLists.txt")
 version = "3.22.1"
 }
 }
}

dependencies {
 // Compose BOM
 val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
 implementation(composeBom)

 // Compose
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-graphics")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")

 // Activity & Lifecycle
 implementation("androidx.activity:activity-compose:1.9.2")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

 // Coroutines
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

 // Navigation
 implementation("androidx.navigation:navigation-compose:2.8.2")

 // DataStore for persistence
 implementation("androidx.datastore:datastore-preferences:1.1.1")

 // JSON parsing
 implementation("org.json:json:20240303")

 // Coil for image loading
 implementation("io.coil-kt:coil-compose:2.7.0")

 // Debugging
 debugImplementation("androidx.compose.ui:ui-tooling")
 debugImplementation("androidx.compose.ui:ui-test-manifest")

 // Testing
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
 androidTestImplementation(composeBom)
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
