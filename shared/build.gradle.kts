import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.application) apply false // Not needed if strictly shared, but often used for android target
    id("com.android.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.room)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.datetime)
            api(libs.kotlinx.coroutines.core)
            
            // Room
            api(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled) // KMP SQLite
            
            // DataStore
            implementation(libs.androidx.datastore.preferences)
        }

        
        androidMain.dependencies {
            implementation(libs.play.billing)
            implementation(libs.stripe.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.work.runtime)
            implementation(libs.koin.workmanager)
            implementation(libs.androidx.security.crypto)
            implementation(libs.androidx.biometric)
            implementation(libs.sqlcipher)
            implementation(libs.androidx.exifinterface)
            implementation("com.google.firebase:firebase-auth:23.1.0")
            implementation("com.google.android.gms:play-services-auth:21.3.0")
            implementation(libs.mlkit.text.recognition)
            implementation("com.google.mediapipe:tasks-genai:0.10.14")
            implementation("com.google.android.gms:play-services-location:21.2.0")
        }
        
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.fordham.toolbelt.shared"
    compileSdk = 35
    buildFeatures {
        buildConfig = false
    }
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
}
