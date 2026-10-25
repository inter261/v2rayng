plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.v2rang"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.v2rang"
        minSdk = 28   // setShadowLayer di hardware canvas (bayangan clay) butuh API 28+
        targetSdk = 34
        versionCode = 1
        versionName = "0.2"
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    signingConfigs {
        create("aospTest") {
            storeFile = file("testkey.keystore")
            storePassword = "android"
            keyAlias = "testkey"
            keyPassword = "android"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }
    buildTypes {
        // debug juga ditandatangani kunci yang sama, karena workflow GitHub Actions membangun debug
        debug {
            signingConfig = signingConfigs.getByName("aospTest")
        }
        // Build release jauh lebih ringan dari debug: R8 mengecilkan & mengoptimalkan kode, dan Compose hanya benar-benar
        // cepat di build release. Pakai APK release untuk pemakaian sehari-hari; debug hanya untuk mencari bug.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("aospTest")
        }
    }
    packaging { jniLibs { useLegacyPackaging = true } }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation(files("libs/libv2ray.aar")) // Xray-core (2dust/AndroidLibXrayLite v26.10.21)
}
