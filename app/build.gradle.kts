plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.spoyt"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.spoyt"
        minSdk = 26
        targetSdk = 34
        versionCode = 9
        versionName = "1.4.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 shrinking: the APK gets much smaller and NewPipe/Rhino need the keep rules
            // in proguard-rules.pro (reflection-heavy JS signature deciphering).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Compose compiler compatible with Kotlin 2.0.20 / Compose BOM 2024.10.00.
        kotlinCompilerExtensionVersion = "1.6.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LGPL3.0,MPL-2.0}.txt"
            excludes += "/META-INF/{LICENSE,NOTICE}.txt"
        }
    }
}

dependencies {
    // Compose BOM keeps all Compose libraries in lockstep.
    implementation(platform(libs.androidx.compose.bom))

    // AndroidX core & lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Media3 (ExoPlayer + MediaSession for background playback + notification)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

    // Room (local database)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager (downloads)
    implementation(libs.androidx.work.runtime.ktx)

    // Coil (image loading for Compose)
    implementation(libs.coil.compose)

    // Palette (extract a dark accent color from the artwork for the player background)
    implementation(libs.androidx.palette)

    // OkHttp (used by the NewPipe Downloader implementation)
    implementation(libs.okhttp)

    // NewPipe Extractor (YouTube search + stream resolution) via JitPack
    implementation(libs.newpipe.extractor)

    // Plain JVM diagnostics for the extractor (run manually, not part of the app).
    testImplementation(libs.junit)
}
