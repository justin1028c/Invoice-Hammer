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
            implementation(libs.ktor.client.core)
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
        commonMain.configure {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
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
        buildConfig = true
    }
    defaultConfig {
        minSdk = 26
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }
        fun quotedProperty(key: String, fallback: String = ""): String {
            val raw = localProperties.getProperty(key, fallback)
            return "\"${raw.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        }
        buildConfigField("String", "POWERPAY_BASE_URL", quotedProperty("powerpay.base.url"))
        buildConfigField("String", "POWERPAY_APP_ID", quotedProperty("powerpay.app.id"))
        buildConfigField("String", "POWERPAY_PUBLIC_KEY", quotedProperty("powerpay.public.key"))
        buildConfigField("String", "POWERPAY_SIGNING_SECRET", quotedProperty("powerpay.signing.secret"))
        buildConfigField("String", "POWERPAY_ENV", quotedProperty("powerpay.env", "sandbox"))
        buildConfigField("String", "POWERPAY_API_VARIANT", quotedProperty("powerpay.api.variant", "sdk"))
        buildConfigField("String", "SUPABASE_URL", quotedProperty("supabase.url"))
        buildConfigField("String", "SUPABASE_ANON_KEY", quotedProperty("supabase.anon.key"))
        buildConfigField("String", "SUPABASE_SCHEMA", quotedProperty("supabase.schema", "public"))
        buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", quotedProperty("stripe.publishable.key"))
        buildConfigField("String", "STRIPE_PAYMENT_BACKEND_URL", quotedProperty("stripe.payment.backend.url"))
        buildConfigField("String", "STRIPE_CONNECT_ONBOARDING_URL", quotedProperty("stripe.connect.onboarding.url"))
        buildConfigField("String", "STRIPE_BACKEND_API_KEY", quotedProperty("stripe.backend.api.key"))
        buildConfigField("String", "STRIPE_APPLICATION_FEE_BPS", quotedProperty("stripe.application.fee.bps", "100"))
        val geminiModelOverride = localProperties.getProperty("gemini.model.name", "gemini-3.5-flash")
        buildConfigField("String", "FOREMAN_GEMINI_BACKEND_URL", quotedProperty("foreman.gemini.backend.url"))
        buildConfigField("String", "FOREMAN_BACKEND_API_KEY", quotedProperty("foreman.backend.api.key"))
        buildConfigField("String", "GEMINI_AGENT_MODEL_NAME", quotedProperty("gemini.agent.model.name", geminiModelOverride))
        buildConfigField("String", "GEMINI_TASK_MODEL_NAME", quotedProperty("gemini.task.model.name", "gemini-3.1-flash-lite"))
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
