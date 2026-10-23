plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.personal.carvideo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.personal.carvideo"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // NewPipeExtractor needs Java 8+ APIs
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.car.app:app:1.4.0")
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.3.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.24.2")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.0.4")
}
