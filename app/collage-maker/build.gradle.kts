plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // Agar Dagger/Hilt use kar rahe hain toh ye line add karein:
    // id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.editor.collage"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    // ImageToolbox Compose use karta hai, isliye ye zaroori hai
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {
    implementation(project(":lib:collages"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
}