import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.googleServices)
}

android {
    namespace = "com.fordham.toolbelt"
    compileSdk = 35

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    defaultConfig {
        applicationId = "com.fordham.toolbelt"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        fun quotedProperty(key: String, fallback: String = ""): String {
            val raw = localProperties.getProperty(key, fallback)
            return "\"${raw.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        }
        buildConfigField("String", "GOOGLE_CLIENT_ID", quotedProperty("google.client.id", "716278040823-ngqvn2n3td42nrr6nbe4e3jlki348apa.apps.googleusercontent.com"))
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

    signingConfigs {
        create("release") {
            val storeFilePath = localProperties.getProperty("release.storeFile")
            if (!storeFilePath.isNullOrBlank()) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = localProperties.getProperty("release.storePassword")
                keyAlias = localProperties.getProperty("release.keyAlias")
                keyPassword = localProperties.getProperty("release.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseStore = localProperties.getProperty("release.storeFile")
            signingConfig = if (!releaseStore.isNullOrBlank()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        // Obfuscated beta for testers: debug signing matches Firebase google-services.json today.
        // Use release + beta keystore only after adding that SHA-1 in Firebase (see FIREBASE_GOOGLE_SIGNIN.md).
        create("beta") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}


ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":composeApp"))
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Icons
    implementation("androidx.compose.material:material-icons-extended")
    
    // Image Loading
    implementation(libs.coil.compose)

    // Gemini Cloud Implementation
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.google.ai.client)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.mlkit.text.recognition)

    // Google Drive
    implementation(libs.google.api.client.android) {
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "com.google.guava", module = "guava-jdk5")
    }
    implementation(libs.google.api.services.drive)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.workmanager)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.datetime)
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("app.cash.turbine:turbine:1.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Security
    implementation(libs.sqlcipher)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
}
