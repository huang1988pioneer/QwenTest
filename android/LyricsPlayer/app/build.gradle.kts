plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.qwentest.lyricsplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.qwentest.lyricsplayer"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.4.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
}
