plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "br.com.jean.meuponto"
    compileSdk = 36
    val localTestSuffix = providers.gradleProperty("localTestSuffix").orNull ?: ""
    val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
    val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
    val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
    val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
    val hasReleaseSigning = listOf(releaseKeystorePath, releaseKeystorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }
    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    defaultConfig {
        applicationId = "br.com.jean.meuponto$localTestSuffix"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "0.6.3"
    }
    buildFeatures { compose = true; buildConfig = true }
    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATER_ENABLED", "false")
        }
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATER_ENABLED", "true")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
}

kotlin { jvmToolchain(17) }

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
