# Invoice Hammer Aggregate

Generated: 2026-05-17T17:47:39.5825815-04:00

Workspace: C:\Users\Justin\AndroidStudioProjects\InvoiceApp


---

## .\settings.gradle.kts

```kts
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "InvoiceHammer"
include(":shared")
include(":composeApp")
include(":androidApp")
project(":androidApp").projectDir = file("app")

```


---

## .\build.gradle.kts

```kts
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
}
```


---

## .\gradle.properties

```properties
# Project-wide Gradle settings.
# IDE (e.g. Android Studio) users:
# Gradle settings configured through the IDE *will override*
# any settings specified in this file.
# For more details on how to configure your build environment visit
# http://www.gradle.org/docs/current/userguide/build_environment.html
# Specifies the JVM arguments used for the daemon process.
# The setting is particularly useful for tweaking memory settings.
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
# When configured, Gradle will run in incubating parallel mode.
# This option should only be used with decoupled projects. For more details, visit
# https://developer.android.com/r/tools/gradle-multi-project-decoupled-projects
# org.gradle.parallel=true
# Kotlin code style for this project: "official" or "obsolete":
kotlin.code.style=official
android.disallowKotlinSourceSets=false
android.builtInKotlin=false
android.newDsl=false

# Use the Java version bundled with Android Studio
org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr

android.useAndroidX=true
android.enableJetifier=true
```


---

## .\shared\build.gradle.kts

```kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.application) apply false // Not needed if strictly shared, but often used for android target
    id("com.android.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
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
            
            // Room
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled) // KMP SQLite
            
            // DataStore
            implementation(libs.androidx.datastore.preferences)
        }
        
        androidMain.dependencies {
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
        }
        
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation("androidx.sqlite:sqlite:2.5.0-alpha13") // For NativeSQLiteDriver
        }
    }
}

android {
    namespace = "com.fordham.toolbelt.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
}

```


---

## .\composeApp\build.gradle.kts

```kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.application) apply false
    id("com.android.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.compose)
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
            baseName = "ComposeApp"
            isStatic = true
            export(project(":shared"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            
            // ViewModel & Lifecycle
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
            implementation(compose.materialIconsExtended)
            
            // Image Loading
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            
            // Navigation
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")
        }
        
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
        
        iosMain.dependencies {
        }
    }
}

android {
    namespace = "com.fordham.toolbelt.composeapp"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

```


---

## .\app\build.gradle.kts

```kts
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
        buildConfigField("String", "GEMINI_MODEL", "\"gemini-3.1-flash-lite-preview\"")

        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
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
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
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

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\App.kt

```kt
package com.fordham.toolbelt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.ui.MainScreen
import com.fordham.toolbelt.ui.theme.ToolbeltTheme
import com.fordham.toolbelt.ui.viewmodel.*
import com.fordham.toolbelt.util.PlatformActions
import com.fordham.toolbelt.util.VoiceAssistant
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val platformActions: PlatformActions = koinInject()
    var isAuthenticated by remember { mutableStateOf(false) }
    var isCheckingBiometrics by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (platformActions.isBiometricAvailable()) {
            platformActions.authenticateBiometric(
                title = "Vault Locked",
                subtitle = "Authenticate to access Invoice Hammer",
                onSuccess = { isAuthenticated = true; isCheckingBiometrics = false },
                onError = { isCheckingBiometrics = false }
            )
        } else {
            isAuthenticated = true
            isCheckingBiometrics = false
        }
    }

    ToolbeltTheme {
        if (isAuthenticated) {
            val newInvoiceViewModel: NewInvoiceViewModel = koinViewModel()
            val historyViewModel: HistoryViewModel = koinViewModel()
            val receiptsViewModel: ReceiptsViewModel = koinViewModel()
            val statsViewModel: StatsViewModel = koinViewModel()
            val clientsViewModel: ClientsViewModel = koinViewModel()
            val suppliersViewModel: SuppliersViewModel = koinViewModel()
            val agentViewModel: AgentViewModel = koinViewModel()
            val authViewModel: AuthViewModel = koinViewModel()
            val paymentViewModel: PaymentViewModel = koinViewModel()
            val sharedViewModel: SharedViewModel = koinViewModel()
            val voiceAssistant: VoiceAssistant = koinInject()

            MainScreen(
                newInvoiceViewModel = newInvoiceViewModel,
                historyViewModel = historyViewModel,
                receiptsViewModel = receiptsViewModel,
                statsViewModel = statsViewModel,
                clientsViewModel = clientsViewModel,
                suppliersViewModel = suppliersViewModel,
                agentViewModel = agentViewModel,
                authViewModel = authViewModel,
                paymentViewModel = paymentViewModel,
                sharedViewModel = sharedViewModel,
                voiceAssistant = voiceAssistant,
                platformActions = platformActions
            )
        } else if (!isCheckingBiometrics) {
            // Biometric Lock Screen (Common UI)
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Lock, 
                        contentDescription = null, 
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Vault Locked", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { 
                            platformActions.authenticateBiometric(
                                title = "Vault Locked", 
                                subtitle = "Authenticate to access",
                                onSuccess = { isAuthenticated = true },
                                onError = {}
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Unlock Vault")
                    }
                }
            }
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\di\ViewModelModule.kt

```kt
package com.fordham.toolbelt.di

import com.fordham.toolbelt.ui.viewmodel.*
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { SuppliersViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ClientsViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { PaymentViewModel(get(), get()) }
    viewModel { StatsViewModel(get(), get(), get(), get()) }
    viewModel { AgentViewModel(get(), get()) }
    viewModel { ReceiptsViewModel(get(), get(), get(), get()) }
    viewModel { SharedViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { NewInvoiceViewModel(get(), get(), get(), get(), get()) }
    viewModel { AuthViewModel(get(), get()) }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\components\AgentOverlay.kt

```kt
package com.fordham.toolbelt.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.AgentMode

@Composable
fun AgentOverlay(
    isActive: Boolean,
    isProcessing: Boolean,
    currentMode: AgentMode,
    transcript: String,
    lastResponse: String?,
    isListening: Boolean,
    onDismiss: () -> Unit,
    onMicClick: () -> Unit,
    onApprove: () -> Unit,
    pendingApproval: com.fordham.toolbelt.domain.model.ForemanToolCall? = null,
    modifier: Modifier = Modifier
) {
    if (!isProcessing && !isListening && pendingApproval == null && lastResponse == null) return

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .padding(start = 12.dp, end = 96.dp, bottom = 8.dp)
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .then(
                if (isListening) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    RoundedCornerShape(12.dp)
                ) else if (pendingApproval != null) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.error,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (pendingApproval != null) 
                MaterialTheme.colorScheme.errorContainer 
            else if (currentMode == AgentMode.ACTION)
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isListening) MaterialTheme.colorScheme.primary.copy(alpha = alpha) 
                                else if (pendingApproval != null) MaterialTheme.colorScheme.error
                                else Color.Transparent, 
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            pendingApproval != null -> "APPROVAL NEEDED"
                            isListening -> "LISTENING"
                            isProcessing -> "WORKING"
                            currentMode == AgentMode.ACTION -> "AI ACTION"
                            else -> "AI RESPONSE"
                        },
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pendingApproval != null) MaterialTheme.colorScheme.error
                        else if (currentMode == AgentMode.ACTION) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.secondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isProcessing && pendingApproval == null) {
                        IconButton(onClick = onMicClick, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Speak",
                                tint = if (isListening) MaterialTheme.colorScheme.primary.copy(alpha = alpha) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (isListening) 24.dp else 20.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            if (isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Checking: \"${transcript}\"", style = MaterialTheme.typography.bodySmall)
            } else {
                if (pendingApproval != null) {
                    Text("The agent wants to: ${pendingApproval.type.name}", fontWeight = FontWeight.Bold)
                    Text(pendingApproval.reasoning, style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("REJECT") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("APPROVE ACTION")
                        }
                    }
                } else {
                    Text(
                        lastResponse ?: if (isListening) "Speak now..." else "Ready.",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\components\CircleImage.kt

```kt
package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun CircleImage(
    url: String,
    modifier: Modifier = Modifier.size(40.dp)
) {
    AsyncImage(
        model = url,
        contentDescription = "Profile Image",
        modifier = modifier.clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\components\HistoryItemCard.kt

```kt
package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.ui.theme.*
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.Invoice

@Composable
fun HistoryItemCard(
    invoice: Invoice,
    paymentRequest: InvoicePaymentRequest? = null,
    onDelete: (Invoice) -> Unit,
    onTogglePaid: (Invoice) -> Unit,
    onView: (Invoice) -> Unit,
    onShare: (Invoice) -> Unit,
    onRequestDeposit: (Invoice) -> Unit,
    onRequestFullPayment: (Invoice) -> Unit,
    onConvert: ((Invoice) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (invoice.isPaid) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
                    else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        invoice.clientName.uppercase(), 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )
                    if (invoice.isEstimate) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                " ESTIMATE ", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.primary, 
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    paymentRequest?.let { request ->
                        Surface(
                            color = BrandOrange.copy(alpha = 0.16f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 4.dp),
                            border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.4f))
                        ) {
                            Text(
                                " ${request.statusLabel}: ${request.formattedAmount} ",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandOrange,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                Text(
                    invoice.formattedTotal, 
                    style = MaterialTheme.typography.headlineSmall, 
                    color = if (invoice.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, 
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                invoice.date.uppercase(), 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                invoice.itemsSummary, 
                style = MaterialTheme.typography.bodyMedium, 
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
            
            if (invoice.depositAmount > 0) {
                Text(
                    "DEPOSIT: ${invoice.formattedDeposit}", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = BrandOrange, 
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TacticalButton(
                    onClick = { onRequestDeposit(invoice) },
                    text = "REQUEST DEPOSIT",
                    modifier = Modifier.height(40.dp).weight(1f),
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = { Icon(Icons.Default.Link, null) }
                )
                TacticalButton(
                    onClick = { onRequestFullPayment(invoice) },
                    text = "PAY LINK",
                    modifier = Modifier.height(40.dp).weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!invoice.isEstimate) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Checkbox(
                            checked = invoice.isPaid, 
                            onCheckedChange = { onTogglePaid(invoice) }, 
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            if (invoice.isPaid) "PAID" else "UNPAID", 
                            style = MaterialTheme.typography.labelLarge, 
                            fontWeight = FontWeight.Black, 
                            color = if (invoice.isPaid) BrandOrange else MaterialTheme.colorScheme.error
                        )
                    }
                } else if (onConvert != null) {
                    TacticalButton(
                        onClick = { onConvert(invoice) }, 
                        text = "FINALIZE INVOICE", 
                        modifier = Modifier.height(40.dp).weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row {
                    IconButton(onClick = { onView(invoice) }) { 
                        Icon(Icons.Default.Visibility, "View", tint = MaterialTheme.colorScheme.onSurface) 
                    }
                    IconButton(onClick = { onShare(invoice) }) { 
                        Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.primary) 
                    }
                    IconButton(onClick = { onDelete(invoice) }) { 
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) 
                    }
                }
            }
        }
    }

}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\components\MainAgentFab.kt

```kt
package com.fordham.toolbelt.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainAgentFab(
    isListening: Boolean,
    isPremium: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onPremiumRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "geminiFabGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier.padding(bottom = 22.dp, end = 2.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(999.dp),
            tonalElevation = 3.dp,
            shadowElevation = 2.dp
        ) {
            Text(
                text = if (isListening) "LISTENING" else "ASK AI",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        FloatingActionButton(
            onClick = {
                if (!isPremium) {
                    onPremiumRequired()
                } else if (isListening) {
                    onStopListening()
                } else {
                    onStartListening()
                }
            },
            containerColor = if (isListening) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
            contentColor = if (isListening) Color.Black else MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    if (isListening) {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.AutoAwesome,
                    contentDescription = if (isListening) "Stop Gemini AI" else "Ask Gemini AI",
                    modifier = Modifier.size(28.dp)
                )
                if (!isPremium) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\components\MainBottomBar.kt

```kt
package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import com.fordham.toolbelt.ui.theme.BrandOrange

@Composable
fun MainBottomBar(
    currentPage: Int,
    onTabSelected: (Int) -> Unit
) {
    val isDarkMode = MaterialTheme.colorScheme.background == Color(0xFF000000)
    val borderColor = if (isDarkMode) Color(0xFF333333) else Color.Black
    val borderWidth = if (isDarkMode) 1.dp else 2.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() // Respect system navigation bar insets to prevent device clipping
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp), // Extremely compact tactical height
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val navItems = listOf(
                Triple(0, Icons.Default.Add, "NEW"),
                Triple(1, Icons.Default.History, "PAST"),
                Triple(2, Icons.Default.Receipt, "RECEIPTS"),
                Triple(3, Icons.Default.BarChart, "STATS"),
                Triple(4, Icons.Default.Storefront, "STORES"),
                Triple(5, Icons.Default.Person, "CLIENTS"),
                Triple(6, Icons.Default.Settings, "SETTINGS")
            )
            navItems.forEach { (index, icon, label) ->
                val isSelected = currentPage == index
                val contentColor = if (isSelected) BrandOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label.replace("\n", " "),
                            tint = contentColor,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = label,
                            color = contentColor,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}


```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\components\MainTopBar.kt

```kt
package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    onLedgerClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "INVOICE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 1.sp
                )
                Text(
                    "HAMMER",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        navigationIcon = {
            Icon(
                Icons.Default.Handyman,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, end = 8.dp).size(28.dp)
            )
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                IconButton(onClick = onLedgerClick) {
                    Icon(Icons.Default.AccountBalanceWallet, "Payment ledger", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    )
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\components\PaymentLedgerSheet.kt

```kt
package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.ui.theme.BrandOrange
import com.fordham.toolbelt.ui.viewmodel.PaymentUiState
import com.fordham.toolbelt.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentLedgerSheet(
    uiState: PaymentUiState,
    onDismiss: () -> Unit,
    onOpenPaymentLink: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = BrandOrange)
                Column {
                    Text("PAYMENT LEDGER", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Google Pay, Apple Pay, Stellar, and card-link requests.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                LedgerMetric("REQUESTED", DateTimeUtil.formatMoney(uiState.totalRequested), Modifier.weight(1f))
                LedgerMetric("ACTIVE", uiState.pendingCount.toString(), Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            if (uiState.requests.isEmpty()) {
                Text(
                    "No payment requests yet. Open an invoice and request a deposit or full payment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    items(uiState.requests, key = { it.id.value }) { request ->
                        PaymentLedgerRow(request = request, onOpenPaymentLink = onOpenPaymentLink)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun PaymentRequestCreatedDialog(
    request: InvoicePaymentRequest,
    onDismiss: () -> Unit,
    onOpenPaymentLink: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PAYMENT LINK READY", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${request.invoiceClientName.uppercase()} - ${request.formattedAmount}", fontWeight = FontWeight.Black)
                Text("Mock ${request.providerLabel} payment request created. This is the app-side flow your backend can replace later.")
                Text(request.paymentLink.value, color = BrandOrange, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            TacticalButton(
                onClick = { onOpenPaymentLink(request.paymentLink.value) },
                text = "OPEN LINK",
                icon = { Icon(Icons.Default.OpenInBrowser, null) }
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("DONE") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodPickerSheet(
    requestType: PaymentRequestType,
    onDismiss: () -> Unit,
    onProviderSelected: (PaymentProviderType) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = if (requestType == PaymentRequestType.Deposit) "REQUEST DEPOSIT" else "REQUEST FULL PAYMENT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                "Choose the rail for this invoice payment. These are mock flows until the backend is connected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            PaymentProviderOption(
                title = "Google Pay",
                subtitle = "Future processor-backed wallet payment.",
                icon = { Icon(Icons.Default.Payment, null, tint = BrandOrange) },
                onClick = { onProviderSelected(PaymentProviderType.GooglePay) }
            )
            PaymentProviderOption(
                title = "Apple Pay",
                subtitle = "iOS-ready provider slot for native checkout.",
                icon = { Icon(Icons.Default.Smartphone, null, tint = BrandOrange) },
                onClick = { onProviderSelected(PaymentProviderType.ApplePay) }
            )
            PaymentProviderOption(
                title = "Stellar USDC",
                subtitle = "Stablecoin settlement rail for SCF demo path.",
                icon = { Icon(Icons.Default.Public, null, tint = BrandOrange) },
                onClick = { onProviderSelected(PaymentProviderType.StellarUsdc) }
            )
            PaymentProviderOption(
                title = "Card / Payment Link",
                subtitle = "Hosted fallback link for clients without wallets.",
                icon = { Icon(Icons.Default.CreditCard, null, tint = BrandOrange) },
                onClick = { onProviderSelected(PaymentProviderType.CardLink) }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PaymentProviderOption(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(title.uppercase(), fontWeight = FontWeight.Black)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LedgerMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
            Text(value, style = MaterialTheme.typography.titleMedium, color = BrandOrange, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PaymentLedgerRow(
    request: InvoicePaymentRequest,
    onOpenPaymentLink: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(request.invoiceClientName.uppercase(), fontWeight = FontWeight.Black)
                Text(
                    "${request.type.label()} • ${request.providerLabel} • ${request.statusLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(request.formattedAmount, color = BrandOrange, fontWeight = FontWeight.Black)
                TextButton(onClick = { onOpenPaymentLink(request.paymentLink.value) }) {
                    Text("LINK", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

private fun PaymentRequestType.label(): String = when (this) {
    PaymentRequestType.Deposit -> "DEPOSIT"
    PaymentRequestType.FullBalance -> "FULL PAY"
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\components\PremiumLockDialog.kt

```kt
package com.fordham.toolbelt.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PremiumLockDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PRO FEATURE LOCKED", fontWeight = FontWeight.Black) },
        text = { Text("AI Command Center requires a Pro Account. Enable it in Settings to unlock AI automation and Bento reporting.") },
        confirmButton = {
            TacticalButton(
                onClick = onGoToSettings,
                text = "GO TO SETTINGS"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\components\StatCard.kt

```kt
package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val isDarkMode = MaterialTheme.colorScheme.background == Color(0xFF000000)
    val borderColor = if (isDarkMode) Color.Black else Color.Black
    val borderWidth = if (isDarkMode) 1.dp else 2.dp
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 4.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}


```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\components\TacticalButton.kt

```kt
package com.fordham.toolbelt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun TacticalButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White, // Defaulting to White for contrast against BrandOrange
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "press_scale"
    )

    // Contextual border: glowing in dark mode, sharp black in light mode
    val isDarkMode = MaterialTheme.colorScheme.background == Color(0xFF000000)
    val borderColor = if (isDarkMode) containerColor else Color.Black
    val borderWidth = if (isDarkMode) 1.dp else 2.dp

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    icon()
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text.uppercase(), // Force uppercase for tactical feel
                    fontWeight = FontWeight.Black, 
                    fontSize = 14.sp, 
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\MainPagerContent.kt

```kt
package com.fordham.toolbelt.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.ui.tabs.ClientsTab
import com.fordham.toolbelt.ui.tabs.HistoryTab
import com.fordham.toolbelt.ui.tabs.NewInvoiceTab
import com.fordham.toolbelt.ui.tabs.ReceiptsTab
import com.fordham.toolbelt.ui.tabs.SettingsTab
import com.fordham.toolbelt.ui.tabs.StatsTab
import com.fordham.toolbelt.ui.tabs.SuppliersTab
import com.fordham.toolbelt.ui.viewmodel.AuthViewModel
import com.fordham.toolbelt.ui.viewmodel.ClientsIntent
import com.fordham.toolbelt.ui.viewmodel.ClientsViewModel
import com.fordham.toolbelt.ui.viewmodel.HistoryViewModel
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceIntent
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceUiState
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceViewModel
import com.fordham.toolbelt.ui.viewmodel.PaymentViewModel
import com.fordham.toolbelt.ui.viewmodel.ReceiptsViewModel
import com.fordham.toolbelt.ui.viewmodel.SharedViewModel
import com.fordham.toolbelt.ui.viewmodel.StatsViewModel
import com.fordham.toolbelt.ui.viewmodel.SuppliersViewModel
import com.fordham.toolbelt.util.PlatformActions

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainPagerContent(
    pagerState: PagerState,
    newInvoiceUiState: NewInvoiceUiState,
    businessSettings: BusinessSettings,
    newInvoiceViewModel: NewInvoiceViewModel,
    historyViewModel: HistoryViewModel,
    receiptsViewModel: ReceiptsViewModel,
    statsViewModel: StatsViewModel,
    clientsViewModel: ClientsViewModel,
    suppliersViewModel: SuppliersViewModel,
    authViewModel: AuthViewModel,
    sharedViewModel: SharedViewModel,
    paymentViewModel: PaymentViewModel,
    platformActions: PlatformActions,
    onNavigateToSettings: () -> Unit,
    onChoosePaymentMethod: (Invoice, PaymentRequestType) -> Unit
) {
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), userScrollEnabled = true) { page ->
        when (page) {
            0 -> NewInvoiceTab(
                uiState = newInvoiceUiState,
                businessSettings = businessSettings,
                allClients = clientsViewModel.allClients.collectAsStateWithLifecycle(initialValue = emptyList()).value,
                categories = newInvoiceViewModel.categories,
                onSaveBusinessSettings = { sharedViewModel.saveBusinessSettings(it) },
                onTimerToggle = { newInvoiceViewModel.onIntent(NewInvoiceIntent.ToggleTimer) },
                onHourlyRateChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnHourlyRateChange(it)) },
                onBillLabor = { newInvoiceViewModel.onIntent(NewInvoiceIntent.BillLabor) },
                onLogoUriChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnLogoUriChange(it)) },
                onPhotoCaptured = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnPhotoCaptured(it)) },
                onClientNameChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnClientNameChange(it)) },
                onClientAddressChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnClientAddressChange(it)) },
                onSetInvoiceClientDropdownVisible = { newInvoiceViewModel.onIntent(NewInvoiceIntent.SetClientDropdownVisible(it)) },
                onSaveToClientDirectoryChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnSaveToClientDirectoryChange(it)) },
                onRemovePhoto = { newInvoiceViewModel.onIntent(NewInvoiceIntent.RemovePhoto(it)) },
                onSetReceiptPickerVisible = { newInvoiceViewModel.onIntent(NewInvoiceIntent.SetReceiptPickerVisible(it)) },
                onSetInvoiceCategoryDropdownVisible = { newInvoiceViewModel.onIntent(NewInvoiceIntent.SetCategoryDropdownVisible(it)) },
                onCategoryChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnCategoryChange(it)) },
                onItemDescChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnItemDescChange(it)) },
                onItemAmtChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnItemAmtChange(it)) },
                onProcessInvoiceAi = { newInvoiceViewModel.onIntent(NewInvoiceIntent.ProcessInvoiceAi(it)) },
                onAddManualLineItem = { newInvoiceViewModel.onIntent(NewInvoiceIntent.AddManualLineItem) },
                onRemoveLineItem = { newInvoiceViewModel.onIntent(NewInvoiceIntent.RemoveLineItem(it)) },
                onTaxTextChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnTaxTextChange(it)) },
                onDepositCollectedChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnDepositCollectedChange(it)) },
                onSaveInvoice = { isEst, set, comp -> newInvoiceViewModel.onIntent(NewInvoiceIntent.SaveInvoice(isEst, set, comp)) },
                onLinkReceipt = { r, m -> newInvoiceViewModel.onIntent(NewInvoiceIntent.LinkReceipt(r, m)) },
                onShareFile = { f, t -> platformActions.shareFile(f, t) },
                isPremium = businessSettings.isPremium,
                platformActions = platformActions
            )
            1 -> HistoryTab(
                uiState = historyViewModel.uiState.collectAsStateWithLifecycle().value,
                filteredHistory = historyViewModel.filteredInvoices.collectAsStateWithLifecycle(initialValue = emptyList()).value,
                paymentRequests = paymentViewModel.uiState.collectAsStateWithLifecycle().value.requests,
                onViewPdf = { platformActions.openPdf(it) },
                onSharePdf = { f, t -> platformActions.shareFile(f, t) },
                onRequestDeposit = { onChoosePaymentMethod(it, PaymentRequestType.Deposit) },
                onRequestFullPayment = { onChoosePaymentMethod(it, PaymentRequestType.FullBalance) },
                onSetInvoiceToDelete = { historyViewModel.setInvoiceToDelete(it) },
                onDeleteInvoice = { historyViewModel.deleteInvoice(it) },
                onSearchQueryChange = { historyViewModel.onSearchQueryChange(it) },
                onShowPaidOnlyChange = { historyViewModel.onShowPaidOnlyChange(it) },
                onUpdateInvoice = { historyViewModel.updateInvoice(it) },
                onConvertEstimateToInvoice = { historyViewModel.convertEstimateToInvoice(it) },
                platformActions = platformActions
            )
            2 -> ReceiptsTab(
                uiState = receiptsViewModel.uiState.collectAsStateWithLifecycle().value,
                selectedClient = sharedViewModel.selectedClient.collectAsStateWithLifecycle().value,
                allClients = sharedViewModel.allClients.collectAsStateWithLifecycle(initialValue = emptyList()).value,
                filteredReceipts = receiptsViewModel.filteredReceipts.collectAsStateWithLifecycle(initialValue = emptyList()).value,
                receiptsTotal = receiptsViewModel.receiptsTotal.collectAsStateWithLifecycle(initialValue = 0.0).value,
                totalWithMarkup = receiptsViewModel.totalWithMarkup.collectAsStateWithLifecycle(initialValue = 0.0).value,
                onSetFilterClient = { receiptsViewModel.setFilterClient(it) },
                onSetClearConfirmVisible = { receiptsViewModel.setClearConfirmVisible(it) },
                onClearReceiptItems = { receiptsViewModel.clearReceiptItems() },
                onReceiptUriSelected = { receiptsViewModel.onReceiptUriSelected(it) },
                onSetClientDropdownVisible = { receiptsViewModel.setClientDropdownVisible(it) },
                onSelectClient = { sharedViewModel.selectClient(it) },
                onSetMarkupDialogVisible = { receiptsViewModel.setMarkupDialogVisible(it) },
                onMarkupPercentageChange = { receiptsViewModel.onMarkupPercentageChange(it) },
                onProcessReceipt = { receiptsViewModel.processCapturedReceipt(null) {} },
                onClearCapturedReceipt = { receiptsViewModel.clearCapturedReceiptImage() },
                onToggleReceiptBilled = { receiptsViewModel.toggleReceiptBilled(it) },
                onDeleteReceiptItem = { receiptsViewModel.deleteReceiptItem(it) },
                platformActions = platformActions
            )
            3 -> StatsTab(
                stats = statsViewModel.businessStats.collectAsStateWithLifecycle().value,
                settings = businessSettings,
                onExportCsv = { statsViewModel.exportBentoReport { platformActions.shareFile(it, "Bento Report") } },
                onExportZip = { statsViewModel.exportTaxBundle { platformActions.shareFile(it, "Tax Bundle") } },
                onNavigateToSettings = onNavigateToSettings,
                onInsertStressInvoices = { statsViewModel.createStressTestInvoices() },
                onEraseAllInvoices = { statsViewModel.eraseAllInvoices() }
            )
            4 -> SuppliersTab(
                uiState = suppliersViewModel.uiState.collectAsState().value,
                isAddSheetVisible = suppliersViewModel.isAddSheetVisible.collectAsState().value,
                placeSuggestions = suppliersViewModel.placeSuggestions.collectAsState().value,
                isReorderMode = suppliersViewModel.isReorderMode.collectAsState().value,
                reorderList = suppliersViewModel.reorderList.collectAsState().value,
                onTogglePin = { suppliersViewModel.togglePin(it) },
                onHideSupplier = { suppliersViewModel.hideSupplier(it) },
                onAddClick = { suppliersViewModel.setAddSheetVisible(true) },
                onDismissAdd = { suppliersViewModel.setAddSheetVisible(false) },
                onAddSupplier = { n, c, a, p, l -> suppliersViewModel.addSupplier(n, c, a, p, l) },
                onSearchQueryChange = { suppliersViewModel.onSearchQueryChange(it) },
                onClearSuggestions = { suppliersViewModel.clearSuggestions() },
                onToggleReorder = { a, l -> suppliersViewModel.setReorderMode(a, l) },
                onMoveItem = { f, t -> suppliersViewModel.swapItems(f, t) },
                onSaveOrder = { suppliersViewModel.saveOrder() },
                hiddenSuppliers = suppliersViewModel.hiddenSuppliers.collectAsState().value,
                onRestoreSupplier = { suppliersViewModel.restoreSupplier(it) },
                onLogPurchase = { id, _, amt, _ -> suppliersViewModel.logPurchase(id, amt) },
                onOpenStore = { platformActions.openUrl(it) },
                onSnapPhoto = { suppliersViewModel.onPhotoCaptured(it) },
                photoUri = suppliersViewModel.capturedPhotoUri.collectAsState().value,
                platformActions = platformActions
            )
            5 -> {
                val notes by sharedViewModel.selectedClientNotes.collectAsStateWithLifecycle(initialValue = emptyList())
                ClientsTab(
                    clients = clientsViewModel.allClients.collectAsStateWithLifecycle(initialValue = emptyList()).value,
                    selectedClient = sharedViewModel.selectedClient.collectAsStateWithLifecycle().value,
                    clientInvoices = sharedViewModel.selectedClientInvoices.collectAsStateWithLifecycle().value,
                    summary = sharedViewModel.selectedClientSummary.collectAsStateWithLifecycle().value,
                    jobNotes = notes,
                    jobPhotos = sharedViewModel.selectedClientPhotos.collectAsStateWithLifecycle().value,
                    uiState = clientsViewModel.uiState.collectAsStateWithLifecycle().value,
                    onClientClick = { sharedViewModel.selectClient(it) },
                    onDeleteClient = { clientsViewModel.onIntent(ClientsIntent.DeleteClient(it)) },
                    onSetClientToDelete = { clientsViewModel.onIntent(ClientsIntent.SetClientToDelete(it)) },
                    onBackClick = { sharedViewModel.selectClient(null) },
                    onAddNote = { clientsViewModel.onIntent(ClientsIntent.AddNote(it)) },
                    onDeleteNote = { clientsViewModel.onIntent(ClientsIntent.DeleteNote(it)) },
                    onSummarizeNotes = { clientsViewModel.onIntent(ClientsIntent.SummarizeNotes(notes)) },
                    onLinkReceipt = { r -> sharedViewModel.selectedClient.value?.let { clientsViewModel.onIntent(ClientsIntent.LinkReceipt(r, it.name)) } },
                    onViewPdf = { platformActions.openPdf(it) },
                    onSetNoteText = { clientsViewModel.onIntent(ClientsIntent.OnNoteTextChange(it)) },
                    onSetAddNoteVisible = { clientsViewModel.onIntent(ClientsIntent.SetAddNoteVisible(it)) },
                    onSetReceiptPickerVisible = { clientsViewModel.onIntent(ClientsIntent.SetReceiptPickerVisible(it)) },
                    onClearAiSummary = { clientsViewModel.onIntent(ClientsIntent.ClearAiSummary) },
                    onCallClient = { platformActions.callPhone(it) },
                    onEmailClient = { platformActions.sendEmail(it) },
                    onPhotoCaptured = { uri, invId -> clientsViewModel.onIntent(ClientsIntent.OnPhotoCaptured(uri, invId)) },
                    isPremium = businessSettings.isPremium,
                    platformActions = platformActions
                )
            }
            6 -> SettingsTab(
                settings = businessSettings,
                currentUser = authViewModel.currentUser.collectAsStateWithLifecycle().value,
                onSaveSettings = { sharedViewModel.saveBusinessSettings(it) },
                onSignIn = {
                    platformActions.signInWithGoogle(
                        onSuccess = { authViewModel.signIn(it) },
                        onError = { platformActions.showToast(it) }
                    )
                },
                onSignOut = { authViewModel.signOut() },
                onSync = { authViewModel.triggerBackup() },
                syncState = authViewModel.syncState.collectAsStateWithLifecycle().value,
                platformActions = platformActions
            )
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\MainScreen.kt

```kt
package com.fordham.toolbelt.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.ui.components.*
import com.fordham.toolbelt.ui.theme.ToolbeltTheme
import com.fordham.toolbelt.ui.viewmodel.*
import com.fordham.toolbelt.util.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Responsibility: Main application shell, orchestrating navigation and global state.
 * ADHERENCE: Below 300 line limit.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    newInvoiceViewModel: NewInvoiceViewModel,
    historyViewModel: HistoryViewModel,
    receiptsViewModel: ReceiptsViewModel,
    statsViewModel: StatsViewModel,
    clientsViewModel: ClientsViewModel,
    suppliersViewModel: SuppliersViewModel,
    agentViewModel: AgentViewModel,
    authViewModel: AuthViewModel,
    paymentViewModel: PaymentViewModel,
    sharedViewModel: SharedViewModel,
    voiceAssistant: VoiceAssistant,
    platformActions: PlatformActions,
    initialPage: Int = 0
) {
    val scope = rememberCoroutineScope()
    val newInvoiceUiState by newInvoiceViewModel.uiState.collectAsStateWithLifecycle()
    val agentState by agentViewModel.uiState.collectAsStateWithLifecycle()
    val paymentState by paymentViewModel.uiState.collectAsStateWithLifecycle()
    val currentBusinessSettings by sharedViewModel.businessSettings.collectAsStateWithLifecycle(initialValue = BusinessSettings())

    LaunchedEffect(agentState.lastResponse) {
        agentState.lastResponse?.let { voiceAssistant.speak(it) }
    }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 7 })
    var showPremiumLockDialog by remember { mutableStateOf(false) }
    var showPaymentLedger by remember { mutableStateOf(false) }
    var pendingPaymentInvoice by remember { mutableStateOf<Invoice?>(null) }
    var pendingPaymentType by remember { mutableStateOf<PaymentRequestType?>(null) }

    val handleAgentIntent: (AiAgentIntent) -> Unit = { intent ->
        when (intent) {
            is AiAgentIntent.DraftInvoice -> {
                scope.launch { pagerState.animateScrollToPage(0) }
                intent.data?.let { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnItemDescChange(it)) }
            }
            is AiAgentIntent.SummarizeClient -> {
                scope.launch { pagerState.animateScrollToPage(5) }
                sharedViewModel.selectClientByName(intent.clientName)
            }
            is AiAgentIntent.AnalyzeFinances -> scope.launch { pagerState.animateScrollToPage(3) }
            is AiAgentIntent.FindJob -> {
                scope.launch { pagerState.animateScrollToPage(1) }
                historyViewModel.onSearchQueryChange(intent.query)
            }
            is AiAgentIntent.ScanReceipt -> scope.launch { pagerState.animateScrollToPage(2) }
            is AiAgentIntent.OpenStores -> scope.launch { pagerState.animateScrollToPage(4) }
            is AiAgentIntent.PremiumRequired -> showPremiumLockDialog = true
            else -> {}
        }
    }

    val startAgentListening = {
        if (platformActions.isPermissionGranted(Permission.RECORD_AUDIO)) {
            agentViewModel.setListening(true)
            voiceAssistant.startListening(
                onResult = { command ->
                    agentViewModel.setListening(false)
                    val appContext = "Current Tab: ${pagerState.currentPage}, Client Selected: ${sharedViewModel.selectedClient.value?.name ?: "None"}"
                    agentViewModel.executeAgentCommand(command, appContext, handleAgentIntent)
                },
                onEnd = { agentViewModel.setListening(false) }
            )
        } else {
            platformActions.requestPermission(Permission.RECORD_AUDIO) {}
        }
    }

    ToolbeltTheme(darkTheme = currentBusinessSettings.isDarkMode) {
        Scaffold(
            topBar = { 
                MainTopBar(
                    onLedgerClick = { showPaymentLedger = true },
                    onSettingsClick = { scope.launch { pagerState.animateScrollToPage(6) } }
                )
            },
            bottomBar = {
                MainBottomBar(
                    currentPage = pagerState.currentPage,
                    onTabSelected = { scope.launch { pagerState.animateScrollToPage(it) } }
                )
            },
            floatingActionButton = {
                MainAgentFab(
                    isListening = agentState.isListening,
                    isPremium = currentBusinessSettings.isPremium,
                    onStartListening = {
                        agentViewModel.setAgentActive(true)
                        startAgentListening()
                    },
                    onStopListening = { agentViewModel.setAgentActive(false) },
                    onPremiumRequired = { showPremiumLockDialog = true }
                )
            }
        ) { inner ->
            Box(modifier = Modifier.padding(inner).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                MainPagerContent(
                    pagerState = pagerState,
                    newInvoiceUiState = newInvoiceUiState,
                    businessSettings = currentBusinessSettings,
                    newInvoiceViewModel = newInvoiceViewModel,
                    historyViewModel = historyViewModel,
                    receiptsViewModel = receiptsViewModel,
                    statsViewModel = statsViewModel,
                    clientsViewModel = clientsViewModel,
                    suppliersViewModel = suppliersViewModel,
                    authViewModel = authViewModel,
                    sharedViewModel = sharedViewModel,
                    paymentViewModel = paymentViewModel,
                    platformActions = platformActions,
                    onNavigateToSettings = { scope.launch { pagerState.animateScrollToPage(6) } },
                    onChoosePaymentMethod = { invoice, type ->
                        pendingPaymentInvoice = invoice
                        pendingPaymentType = type
                    }
                )

                MainScreenDialogs(
                    newInvoiceUiState = newInvoiceUiState,
                    showPremiumLock = showPremiumLockDialog,
                    statsError = statsViewModel.errorMessage.collectAsStateWithLifecycle().value,
                    agentError = agentState.errorMessage,
                    onDismissPremium = { showPremiumLockDialog = false },
                    onGoToSettings = { 
                        showPremiumLockDialog = false
                        scope.launch { pagerState.animateScrollToPage(6) }
                    },
                    onDismissAiConf = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnShowAiConfChange(false)) },
                    onAcceptAi = { newInvoiceViewModel.onIntent(NewInvoiceIntent.AcceptAiItems) },
                    onDismissNewInvoiceError = { newInvoiceViewModel.onIntent(NewInvoiceIntent.ClearError) },
                    onDismissStatsError = { statsViewModel.clearError() },
                    onDismissAgentError = { agentViewModel.clearAgentResponse() }
                )

                if (showPaymentLedger) {
                    PaymentLedgerSheet(
                        uiState = paymentState,
                        onDismiss = { showPaymentLedger = false },
                        onOpenPaymentLink = { platformActions.openUrl(it) }
                    )
                }

                val invoiceForPayment = pendingPaymentInvoice
                val paymentType = pendingPaymentType
                if (invoiceForPayment != null && paymentType != null) {
                    PaymentMethodPickerSheet(
                        requestType = paymentType,
                        onDismiss = {
                            pendingPaymentInvoice = null
                            pendingPaymentType = null
                        },
                        onProviderSelected = { provider ->
                            paymentViewModel.createRequest(invoiceForPayment, paymentType, provider)
                            pendingPaymentInvoice = null
                            pendingPaymentType = null
                        }
                    )
                }

                paymentState.latestRequest?.let { request ->
                    PaymentRequestCreatedDialog(
                        request = request,
                        onDismiss = { paymentViewModel.clearLatestRequest() },
                        onOpenPaymentLink = { platformActions.openUrl(it) }
                    )
                }

                AgentOverlay(
                    isActive = agentState.isActive,
                    isProcessing = agentState.isProcessing,
                    currentMode = agentState.currentMode,
                    transcript = agentState.transcript,
                    lastResponse = agentState.lastResponse,
                    isListening = agentState.isListening,
                    onDismiss = { agentViewModel.setAgentActive(false) },
                    onMicClick = { startAgentListening() },
                    onApprove = { agentViewModel.approveToolCall() },
                    pendingApproval = agentState.pendingApproval,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\MainScreenDialogs.kt

```kt
package com.fordham.toolbelt.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.fordham.toolbelt.ui.components.PremiumLockDialog
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceUiState

/**
 * Responsibility: Manage global-level dialogs for the MainScreen (Premium locks, AI confirmation, Errors).
 */
@Composable
fun MainScreenDialogs(
    newInvoiceUiState: NewInvoiceUiState,
    showPremiumLock: Boolean,
    statsError: String?,
    agentError: String?,
    onDismissPremium: () -> Unit,
    onGoToSettings: () -> Unit,
    onDismissAiConf: () -> Unit,
    onAcceptAi: () -> Unit,
    onDismissNewInvoiceError: () -> Unit,
    onDismissStatsError: () -> Unit,
    onDismissAgentError: () -> Unit
) {
    if (showPremiumLock) {
        PremiumLockDialog(
            onDismiss = onDismissPremium,
            onGoToSettings = onGoToSettings
        )
    }

    if (newInvoiceUiState.showAiConf) {
        AlertDialog(
            onDismissRequest = onDismissAiConf,
            title = { Text("Confirm AI", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    newInvoiceUiState.pendingAi.forEach {
                        Text("• " + it.description + " ($" + it.amount + ")", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = { TacticalButton(onClick = onAcceptAi, text = "ADD ALL") },
            dismissButton = { TextButton(onClick = onDismissAiConf) { Text("CANCEL") } }
        )
    }

    newInvoiceUiState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissNewInvoiceError,
            title = { Text("ERROR", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error) },
            text = { Text(error) },
            confirmButton = { TacticalButton(onClick = onDismissNewInvoiceError, text = "OK") }
        )
    }

    statsError?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissStatsError,
            title = { Text("REPORT ERROR", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error) },
            text = { Text(error) },
            confirmButton = { TacticalButton(onClick = onDismissStatsError, text = "OK") }
        )
    }

    agentError?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissAgentError,
            title = { Text("AI SYSTEM ERROR", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error) },
            text = { Text(error) },
            confirmButton = { TacticalButton(onClick = onDismissAgentError, text = "DISMISS") }
        )
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\Navigation.kt

```kt
package com.fordham.toolbelt.ui

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable
    data object NewInvoice : Screen
    @Serializable
    data object History : Screen
    @Serializable
    data object Receipts : Screen
    @Serializable
    data object Stats : Screen
    @Serializable
    data object Clients : Screen
    @Serializable
    data object Suppliers : Screen
    @Serializable
    data object Settings : Screen
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\clients\ClientDirectoryItem.kt

```kt
package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.Client

/**
 * Responsibility: Display a single client in the directory list.
 */
@Composable
fun ClientDirectoryItem(
    client: Client,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
    ) { 
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) { 
            Column(modifier = Modifier.weight(1f)) { 
                Text(client.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(client.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) 
            }
            Row {
                IconButton(onClick = onDeleteClick) { 
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) 
                }
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.secondary) 
            }
        } 
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\clients\ClientFinancialSummaryCard.kt

```kt
package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.usecase.FinancialSummary
import com.fordham.toolbelt.ui.theme.StatsGreen

/**
 * Responsibility: Display a summary of revenue, costs, and profit for a specific client.
 */
@Composable
fun ClientFinancialSummaryCard(
    summary: FinancialSummary,
    hasAvailableReceipts: Boolean,
    onLinkReceiptClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), 
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("JOB SUMMARY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                Text("REVENUE:", fontWeight = FontWeight.Bold)
                Text(summary.formattedRevenue, fontWeight = FontWeight.Black) 
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { 
                Text("COSTS:", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(summary.formattedExpenses, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                    if (hasAvailableReceipts) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onLinkReceiptClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Link, "Link Receipt", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                Text("NET PROFIT:", fontWeight = FontWeight.Black)
                Text(summary.formattedProfit, fontWeight = FontWeight.Black, color = if (summary.profit >= 0) StatsGreen else MaterialTheme.colorScheme.error) 
            }
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\clients\ClientInvoicesSection.kt

```kt
package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.Invoice

/**
 * Responsibility: Display a list of past invoices for the selected client.
 */
@Composable
fun ClientInvoicesSection(
    invoices: List<Invoice>,
    onInvoiceClick: (Invoice) -> Unit,
    onAddPhotoClick: (Invoice) -> Unit
) {
    Column {
        Text("PAST INVOICES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        
        invoices.forEach { inv -> 
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onInvoiceClick(inv) }, 
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) { 
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) { 
                    Column(modifier = Modifier.weight(1f)) { 
                        Text(inv.date.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(inv.itemsSummary.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onAddPhotoClick(inv) }) {
                            Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Text(inv.formattedTotal, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                } 
            } 
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\clients\ClientNotesSection.kt

```kt
package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.JobNote
import com.fordham.toolbelt.ui.components.TacticalButton

/**
 * Responsibility: Display job notes, allow adding new ones, and show AI summaries.
 */
@Composable
fun ClientNotesSection(
    jobNotes: List<JobNote>,
    isPremium: Boolean,
    isSummarizing: Boolean,
    aiSummary: String?,
    onSummarizeClick: () -> Unit,
    onAddNoteClick: () -> Unit,
    onDeleteNoteClick: (JobNote) -> Unit,
    onClearAiSummary: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CLIENT NOTES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Row {
                if (jobNotes.isNotEmpty()) {
                    TextButton(onClick = onSummarizeClick, enabled = !isSummarizing && isPremium) {
                        if (isSummarizing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("AI SUMMARY", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                TextButton(onClick = onAddNoteClick) {
                    Icon(Icons.Default.EditNote, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text("ADD NOTE", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary) 
                }
            }
        }
        
        aiSummary?.let { summaryText ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AI JOB INSIGHTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = onClearAiSummary, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(summaryText.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
            }
        }

        if (jobNotes.isEmpty()) {
            Text("NO NOTES RECORDED FOR THIS CLIENT.", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
        } else {
            jobNotes.forEach { note ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(note.formattedDate.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                            IconButton(onClick = { onDeleteNoteClick(note) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) 
                            }
                        }
                        Text(note.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\clients\ClientPhotosSection.kt

```kt
package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.JobPhoto

/**
 * Responsibility: Display a horizontal carousel of photos linked to the client's jobs.
 */
@Composable
fun ClientPhotosSection(
    jobPhotos: List<JobPhoto>,
    canCapture: Boolean,
    onSnapPhotoClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("JOB PHOTOS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            if (canCapture) {
                TextButton(onClick = onSnapPhotoClick) {
                    Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("SNAP PHOTO", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        if (jobPhotos.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                jobPhotos.forEach { photo ->
                    Card(
                        modifier = Modifier.size(120.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        coil3.compose.AsyncImage(
                            model = photo.localUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\clients\ClientProfileHeader.kt

```kt
package com.fordham.toolbelt.ui.tabs.clients

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.Client

/**
 * Responsibility: Display the header for a client profile with navigation and contact actions.
 */
@Composable
fun ClientProfileHeader(
    client: Client,
    onBackClick: () -> Unit,
    onCallClick: (String) -> Unit,
    onEmailClick: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        horizontalArrangement = Arrangement.SpaceBetween, 
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) { 
            IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("CLIENT PROFILE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black) 
        }
        Row { 
            if (client.phone.value.isNotEmpty()) {
                IconButton(onClick = { onCallClick(client.phone.value) }) { 
                    Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.secondary) 
                }
            }
            if (client.email.value.isNotEmpty()) {
                IconButton(onClick = { onEmailClick(client.email.value) }) { 
                    Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.secondary) 
                }
            }
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\ClientsTab.kt

```kt
package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.usecase.FinancialSummary
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.tabs.clients.*
import com.fordham.toolbelt.ui.viewmodel.ClientsUiState
import com.fordham.toolbelt.util.PlatformActions

/**
 * Responsibility: Main orchestration for the Client Directory & Profiles.
 * ADHERENCE: Below 300 line limit.
 */
@Composable
fun ClientsTab(
    clients: List<Client>,
    selectedClient: Client?,
    clientInvoices: List<Invoice>,
    summary: FinancialSummary?,
    jobNotes: List<JobNote>,
    jobPhotos: List<JobPhoto>,
    uiState: ClientsUiState,
    onClientClick: (Client) -> Unit,
    onDeleteClient: (Client) -> Unit,
    onSetClientToDelete: (Client?) -> Unit,
    onBackClick: () -> Unit,
    onAddNote: (String) -> Unit,
    onDeleteNote: (JobNote) -> Unit,
    onSummarizeNotes: () -> Unit,
    onLinkReceipt: (ReceiptItem) -> Unit,
    onViewPdf: (String) -> Unit,
    onSetNoteText: (String) -> Unit,
    onSetAddNoteVisible: (Boolean) -> Unit,
    onSetReceiptPickerVisible: (Boolean) -> Unit,
    onClearAiSummary: () -> Unit,
    onCallClient: (String) -> Unit,
    onEmailClient: (String) -> Unit,
    onPhotoCaptured: (String, String) -> Unit,
    platformActions: PlatformActions,
    isPremium: Boolean = false
) {
    // --- DIALOGS ---
    if (uiState.clientToDelete != null) {
        AlertDialog(
            onDismissRequest = { onSetClientToDelete(null) },
            title = { Text("Delete Client?", fontWeight = FontWeight.Black) },
            text = { Text("Are you sure you want to delete ${uiState.clientToDelete!!.name}? This will remove them from your directory, but their invoices and receipts will remain.") },
            confirmButton = { 
                TacticalButton(
                    onClick = { 
                        onDeleteClient(uiState.clientToDelete!!)
                        onSetClientToDelete(null)
                    }, 
                    text = "DELETE", 
                    containerColor = MaterialTheme.colorScheme.error 
                ) 
            },
            dismissButton = { TextButton(onClick = { onSetClientToDelete(null) }) { Text("CANCEL") } }
        )
    }

    if (uiState.showAddNote && selectedClient != null) {
        AlertDialog(
            onDismissRequest = { onSetAddNoteVisible(false) },
            title = { Text("NEW JOB NOTE", fontWeight = FontWeight.Black) }, 
            text = { 
                OutlinedTextField(
                    value = uiState.noteText, 
                    onValueChange = { onSetNoteText(it) }, 
                    label = { Text("NOTE CONTENT...", fontWeight = FontWeight.Bold) }, 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(4.dp)
                ) 
            },
            confirmButton = { 
                TacticalButton(
                    onClick = { onAddNote(selectedClient.name) },
                    text = "SAVE NOTE", 
                    enabled = uiState.noteText.isNotBlank()
                ) 
            }
        )
    }

    if (uiState.showReceiptPicker && selectedClient != null) {
        AlertDialog(
            onDismissRequest = { onSetReceiptPickerVisible(false) },
            title = { Text("FLOATING EXPENSE POOL", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text("SELECT A RECEIPT TO LINK TO ${selectedClient.name.uppercase()}:", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(uiState.availableReceipts) { receipt ->
                            ListItem(
                                headlineContent = { Text(receipt.description.uppercase(), fontWeight = FontWeight.Black) },
                                supportingContent = { Text(receipt.formattedPrice) },
                                modifier = Modifier.fillContentSize().clickable { onLinkReceipt(receipt) }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onSetReceiptPickerVisible(false) }) { Text("CANCEL") } }
        )
    }

    // --- MAIN CONTENT ---
    if (selectedClient == null) {
        LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            item { Text("CLIENT DIRECTORY", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black) }
            items(clients) { client -> 
                ClientDirectoryItem(
                    client = client,
                    onClick = { onClientClick(client) },
                    onDeleteClick = { onSetClientToDelete(client) }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outline) }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ClientProfileHeader(
                client = selectedClient,
                onBackClick = onBackClick,
                onCallClick = onCallClient,
                onEmailClick = onEmailClient
            )
            
            Text(selectedClient.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
            
            summary?.let {
                ClientFinancialSummaryCard(
                    summary = it,
                    hasAvailableReceipts = uiState.availableReceipts.isNotEmpty(),
                    onLinkReceiptClick = { onSetReceiptPickerVisible(true) }
                )
            }

            Spacer(Modifier.height(24.dp))
            
            ClientNotesSection(
                jobNotes = jobNotes,
                isPremium = isPremium,
                isSummarizing = uiState.isSummarizing,
                aiSummary = uiState.aiSummary,
                onSummarizeClick = onSummarizeNotes,
                onAddNoteClick = { onSetAddNoteVisible(true) },
                onDeleteNoteClick = onDeleteNote,
                onClearAiSummary = onClearAiSummary
            )

            if (jobPhotos.isNotEmpty() || clientInvoices.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                ClientPhotosSection(
                    jobPhotos = jobPhotos,
                    canCapture = clientInvoices.isNotEmpty(),
                    onSnapPhotoClick = { 
                        platformActions.capturePhoto { uri ->
                            uri?.let { onPhotoCaptured(it, clientInvoices.first().id.value) }
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
            ClientInvoicesSection(
                invoices = clientInvoices,
                onInvoiceClick = { if (it.pdfPath.isNotEmpty()) onViewPdf(it.pdfPath) },
                onAddPhotoClick = { inv ->
                    platformActions.capturePhoto { uri ->
                        uri?.let { onPhotoCaptured(it, inv.id.value) }
                    }
                }
            )
            
            Spacer(Modifier.height(100.dp))
        }
    }
}

private fun Modifier.fillContentSize() = this.fillMaxWidth()

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\components\BusinessProfileDialog.kt

```kt
package com.fordham.toolbelt.ui.tabs.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.ui.components.TacticalButton

@Composable
fun BusinessProfileDialog(
    businessSettings: BusinessSettings,
    onDismiss: () -> Unit,
    onSave: (BusinessSettings) -> Unit
) {
    var tempSettings by remember(businessSettings) { mutableStateOf(businessSettings) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("BUSINESS PROFILE", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tempSettings.businessName,
                    onValueChange = { tempSettings = tempSettings.copy(businessName = it) },
                    label = { Text("BUSINESS NAME") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = tempSettings.businessSlogan,
                    onValueChange = { tempSettings = tempSettings.copy(businessSlogan = it) },
                    label = { Text("SLOGAN") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = tempSettings.businessPhone,
                    onValueChange = { tempSettings = tempSettings.copy(businessPhone = it) },
                    label = { Text("PHONE") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = tempSettings.businessEmail,
                    onValueChange = { tempSettings = tempSettings.copy(businessEmail = it) },
                    label = { Text("EMAIL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = tempSettings.businessAddress,
                    onValueChange = { tempSettings = tempSettings.copy(businessAddress = it) },
                    label = { Text("ADDRESS") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            TacticalButton(
                onClick = { 
                    onSave(tempSettings)
                    onDismiss() 
                },
                text = "SAVE"
            )
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("CANCEL")
            }
        }
    )
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\components\InvoiceLineItemsList.kt

```kt
package com.fordham.toolbelt.ui.tabs.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceUiState
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.LineItem
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.util.DateTimeUtil

@Composable
fun InvoiceLineItemsList(
    uiState: NewInvoiceUiState,
    businessSettings: BusinessSettings,
    categories: List<String>,
    onSetReceiptPickerVisible: (Boolean) -> Unit,
    onSetInvoiceCategoryDropdownVisible: (Boolean) -> Unit,
    onCategoryChange: (String) -> Unit,
    onItemDescChange: (String) -> Unit,
    onItemAmtChange: (String) -> Unit,
    onProcessInvoiceAi: (List<String>) -> Unit,
    onAddManualLineItem: () -> Unit,
    onRemoveLineItem: (LineItem) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("LINE ITEMS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
            if (uiState.availableReceipts.isNotEmpty()) {
                TextButton(onClick = { onSetReceiptPickerVisible(true) }) {
                    Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("LINK UNBILLED", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(
                        onClick = { onSetInvoiceCategoryDropdownVisible(true) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(uiState.selectedCategory.uppercase(), fontWeight = FontWeight.Black)
                    }
                    DropdownMenu(
                        expanded = uiState.showCategoryDropdown,
                        onDismissRequest = { onSetInvoiceCategoryDropdownVisible(false) }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.uppercase(), fontWeight = FontWeight.Bold) },
                                onClick = { 
                                    onCategoryChange(category)
                                    onSetInvoiceCategoryDropdownVisible(false)
                                }
                            ) 
                        }
                    }
                }
                OutlinedTextField(
                    value = uiState.itemDesc,
                    onValueChange = { onItemDescChange(it) },
                    label = { Text("DESCRIPTION", fontWeight = FontWeight.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = uiState.itemAmt,
                        onValueChange = { onItemAmtChange(it) },
                        label = { Text("PRICE ($)", fontWeight = FontWeight.Black) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TacticalButton(
                            onClick = { onProcessInvoiceAi(categories) },
                            text = if (uiState.isProcessingAi) "" else "AI FILL",
                            enabled = !uiState.isProcessingAi && uiState.itemDesc.length > 5 && businessSettings.isPremium,
                            containerColor = MaterialTheme.colorScheme.primary,
                            icon = { if (uiState.isProcessingAi) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Default.AutoAwesome, null) }
                        )
                    }
                }
                TacticalButton(onClick = { onAddManualLineItem() }, text = "ADD ITEM", modifier = Modifier.align(Alignment.End), containerColor = MaterialTheme.colorScheme.secondary, enabled = uiState.canAddManual)
            }
        }
        uiState.lineItems.forEach { item ->
            ListItem(
                headlineContent = { Text(item.category + ": $" + DateTimeUtil.formatDecimal(item.amount, 2), fontWeight = FontWeight.Bold) },
                supportingContent = { Text(item.description) },
                trailingContent = { IconButton(onClick = { onRemoveLineItem(item) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)) } },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
        if (uiState.lineItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text("No items yet. Add labor, parts, or use AI Fill.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
            }
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\HistoryTab.kt

```kt
package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.ui.components.HistoryItemCard
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.viewmodel.HistoryUiState
import com.fordham.toolbelt.util.PlatformActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTab(
    uiState: HistoryUiState,
    filteredHistory: List<Invoice>,
    paymentRequests: List<InvoicePaymentRequest>,
    onViewPdf: (String) -> Unit,
    onSharePdf: (String, String) -> Unit,
    onRequestDeposit: (Invoice) -> Unit,
    onRequestFullPayment: (Invoice) -> Unit,
    onSetInvoiceToDelete: (Invoice?) -> Unit,
    onDeleteInvoice: (Invoice) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onShowPaidOnlyChange: (Boolean) -> Unit,
    onUpdateInvoice: (Invoice) -> Unit,
    onConvertEstimateToInvoice: (Invoice) -> Unit,
    platformActions: PlatformActions
) {
    if (uiState.invoiceToDelete != null) {
        AlertDialog(
            onDismissRequest = { onSetInvoiceToDelete(null) },
            title = { Text("Delete Record?", fontWeight = FontWeight.Black) },
            text = { Text("This will permanently remove this ${if (uiState.invoiceToDelete!!.isEstimate) "estimate" else "invoice"} from your records. The PDF file will remain on your device.") },
            confirmButton = { 
                TacticalButton(
                    onClick = { 
                        onDeleteInvoice(uiState.invoiceToDelete!!)
                        onSetInvoiceToDelete(null)
                    }, 
                    text = "DELETE", 
                    containerColor = MaterialTheme.colorScheme.error
                ) 
            },
            dismissButton = { 
                TextButton(onClick = { onSetInvoiceToDelete(null) }) {
                    Text("CANCEL") 
                } 
            }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        item { 
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "ARCHIVED RECORDS", 
                    style = MaterialTheme.typography.headlineSmall, 
                    color = MaterialTheme.colorScheme.onBackground, 
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    "Track your cash flow history.", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("SEARCH CLIENTS OR ITEMS...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { onSearchQueryChange("") }) { Icon(Icons.Default.Close, null) } }
                    } else null,
                    shape = RoundedCornerShape(4.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.showPaidOnly,
                        onClick = { onShowPaidOnlyChange(!uiState.showPaidOnly) },
                        label = { Text("PAID ONLY", fontWeight = FontWeight.Black) },
                        shape = RoundedCornerShape(4.dp),
                        leadingIcon = if (uiState.showPaidOnly) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.showPaidOnly,
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }

    items(filteredHistory, key = { it.id.value }) { invoice -> 
            HistoryItemCard(
                invoice = invoice,
                paymentRequest = paymentRequests.firstOrNull { it.invoiceId == invoice.id },
                onDelete = { onSetInvoiceToDelete(it) },
                onTogglePaid = { onUpdateInvoice(it.copy(isPaid = !it.isPaid)) },
                onView = { if (it.pdfPath.isNotEmpty()) onViewPdf(it.pdfPath) },
                onShare = { onSharePdf(it.pdfPath, if (it.isEstimate) "Estimate" else "Invoice") },
                onRequestDeposit = onRequestDeposit,
                onRequestFullPayment = onRequestFullPayment,
                onConvert = if (invoice.isEstimate) { 
                    { est -> 
                        onConvertEstimateToInvoice(est)
                        platformActions.showToast("Converted!")
                    } 
                } else null
            ) 
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\NewInvoiceTab.kt

```kt
package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.tabs.components.*
import com.fordham.toolbelt.ui.viewmodel.NewInvoiceUiState
import com.fordham.toolbelt.util.Permission
import com.fordham.toolbelt.util.PlatformActions

@Composable
fun NewInvoiceTab(
    uiState: NewInvoiceUiState,
    businessSettings: BusinessSettings,
    allClients: List<Client>,
    categories: List<String>,
    onSaveBusinessSettings: (BusinessSettings) -> Unit,
    onTimerToggle: () -> Unit,
    onHourlyRateChange: (String) -> Unit,
    onBillLabor: () -> Unit,
    onLogoUriChange: (String?) -> Unit,
    onPhotoCaptured: (String) -> Unit,
    onClientNameChange: (String) -> Unit,
    onClientAddressChange: (String) -> Unit,
    onSetInvoiceClientDropdownVisible: (Boolean) -> Unit,
    onSaveToClientDirectoryChange: (Boolean) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onSetReceiptPickerVisible: (Boolean) -> Unit,
    onSetInvoiceCategoryDropdownVisible: (Boolean) -> Unit,
    onCategoryChange: (String) -> Unit,
    onItemDescChange: (String) -> Unit,
    onItemAmtChange: (String) -> Unit,
    onProcessInvoiceAi: (List<String>) -> Unit,
    onAddManualLineItem: () -> Unit,
    onRemoveLineItem: (LineItem) -> Unit,
    onTaxTextChange: (String) -> Unit,
    onDepositCollectedChange: (String) -> Unit,
    onSaveInvoice: (Boolean, BusinessSettings, (String) -> Unit) -> Unit,
    onLinkReceipt: (ReceiptItem, Double) -> Unit,
    onShareFile: (String, String) -> Unit,
    platformActions: PlatformActions,
    isPremium: Boolean = false
) {
    var showBusinessDialog by remember { mutableStateOf(false) }

    if (showBusinessDialog) {
        BusinessProfileDialog(
            businessSettings = businessSettings,
            onDismiss = { showBusinessDialog = false },
            onSave = { onSaveBusinessSettings(it) }
        )
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("INVOICE DETAILS", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            TextButton(onClick = { showBusinessDialog = true }) {
                Icon(Icons.Default.Business, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("BUSINESS", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        }
        
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(2.dp, if (uiState.timerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("JOB TIMER", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                        Text(uiState.formattedTime, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    TacticalButton(
                        onClick = { onTimerToggle() }, 
                        text = if (!uiState.timerRunning) "START TIMER" else "STOP TIMER", 
                        icon = { Icon(if (!uiState.timerRunning) Icons.Default.PlayArrow else Icons.Default.Stop, null) },
                        containerColor = if (!uiState.timerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                if (!uiState.timerRunning && uiState.elapsedSeconds > 0) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = uiState.hourlyRate,
                            onValueChange = { onHourlyRateChange(it) },
                            label = { Text("RATE ($/HR)", fontWeight = FontWeight.Black) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        TacticalButton(onClick = { onBillLabor() }, text = "BILL TIME")
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        TacticalButton(
            onClick = { 
                platformActions.pickImage { uri ->
                    uri?.let { onLogoUriChange(it) }
                }
            }, 
            text = if (uiState.logoUri == null) "UPLOAD LOGO" else "LOGO READY ✓", 
            modifier = Modifier.fillMaxWidth(), 
            containerColor = MaterialTheme.colorScheme.primary,
            icon = { Icon(Icons.Default.Image, null) }
        )

        Spacer(Modifier.height(10.dp))
        Box {
            OutlinedTextField(
                value = uiState.clientName,
                onValueChange = { onClientNameChange(it) },
                label = { Text("CLIENT NAME", fontWeight = FontWeight.Black) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                trailingIcon = { IconButton(onClick = { onSetInvoiceClientDropdownVisible(true) }) { Icon(Icons.Default.ArrowDropDown, null) } }
            )
            DropdownMenu(expanded = uiState.showClientDropdown, onDismissRequest = { onSetInvoiceClientDropdownVisible(false) }) {
                allClients.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.name.uppercase(), fontWeight = FontWeight.Bold) },
                        onClick = { 
                            onClientNameChange(c.name)
                            onClientAddressChange(c.address)
                            onSetInvoiceClientDropdownVisible(false)
                        }
                    )
                }
            }
        }
        OutlinedTextField(value = uiState.clientAddress, onValueChange = { onClientAddressChange(it) }, label = { Text("CLIENT ADDRESS", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = uiState.saveToClientDirectory, onCheckedChange = { onSaveToClientDirectoryChange(it) }, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
            Text("SAVE TO CLIENT DIRECTORY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("JOB PHOTOS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
            TextButton(onClick = {
                platformActions.capturePhoto { uri ->
                    uri?.let { onPhotoCaptured(it) }
                }
            }) {
                Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("SNAP PHOTO", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (uiState.capturedPhotos.isEmpty()) {
            Text("NO PHOTOS ADDED YET", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.capturedPhotos.forEach { uri ->
                    Box(modifier = Modifier.size(100.dp)) {
                        Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))) {
                            coil3.compose.AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        IconButton(onClick = { onRemovePhoto(uri) }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        InvoiceLineItemsList(
            uiState = uiState,
            businessSettings = businessSettings,
            categories = categories,
            onSetReceiptPickerVisible = onSetReceiptPickerVisible,
            onSetInvoiceCategoryDropdownVisible = onSetInvoiceCategoryDropdownVisible,
            onCategoryChange = onCategoryChange,
            onItemDescChange = onItemDescChange,
            onItemAmtChange = onItemAmtChange,
            onProcessInvoiceAi = onProcessInvoiceAi,
            onAddManualLineItem = onAddManualLineItem,
            onRemoveLineItem = onRemoveLineItem
        )

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = uiState.taxText, onValueChange = { onTaxTextChange(it) }, label = { Text("TAX RATE %", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp))
        OutlinedTextField(value = uiState.depositCollected, onValueChange = { onDepositCollectedChange(it) }, label = { Text("DEPOSIT COLLECTED ($)", fontWeight = FontWeight.Black) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(4.dp))
        
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TacticalButton(onClick = { onSaveInvoice(true, businessSettings) { onShareFile(it, "Estimate") } }, text = "SAVE ESTIMATE", modifier = Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.secondary, enabled = uiState.canSave)
            TacticalButton(onClick = { onSaveInvoice(false, businessSettings) { onShareFile(it, "Invoice") } }, text = "SAVE INVOICE", modifier = Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.primary, enabled = uiState.canSave)
        }
        Spacer(Modifier.height(120.dp))
    }

    var selectedReceipt by remember { mutableStateOf<ReceiptItem?>(null) }
    var showMarkupPrompt by remember { mutableStateOf(false) }
    var markupInput by remember { mutableStateOf("20") }

    if (uiState.showReceiptPicker) {
        AlertDialog(
            onDismissRequest = { onSetReceiptPickerVisible(false) },
            title = { Text("FLOATING EXPENSE POOL", fontWeight = FontWeight.Black) },
            text = {
                if (uiState.availableReceipts.isEmpty()) {
                    Text("NO UNBILLED RECEIPTS FOUND.")
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(uiState.availableReceipts.size) { index ->
                            val receipt = uiState.availableReceipts[index]
                            ListItem(
                                headlineContent = { Text(receipt.description.uppercase(), fontWeight = FontWeight.Black) },
                                supportingContent = { Text(receipt.formattedPrice) },
                                trailingContent = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.clickable { 
                                    selectedReceipt = receipt
                                    showMarkupPrompt = true
                                    onSetReceiptPickerVisible(false)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onSetReceiptPickerVisible(false) }) { Text("CLOSE") } }
        )
    }

    if (showMarkupPrompt && selectedReceipt != null) {
        AlertDialog(
            onDismissRequest = { showMarkupPrompt = false },
            title = { Text("APPLY MARKUP?", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text("LINKING: ${selectedReceipt!!.description}")
                    Text("RAW COST: ${selectedReceipt!!.formattedPrice}")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = markupInput,
                        onValueChange = { markupInput = it },
                        label = { Text("MARKUP PERCENTAGE %", fontWeight = FontWeight.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        onLinkReceipt(selectedReceipt!!, 0.0)
                        showMarkupPrompt = false
                        selectedReceipt = null
                    }) { Text("NO (AT COST)") }
                    TacticalButton(onClick = {
                        val pct = markupInput.toDoubleOrNull() ?: 0.0
                        onLinkReceipt(selectedReceipt!!, pct)
                        showMarkupPrompt = false
                        selectedReceipt = null
                    }, text = "APPLY %")
                }
            }
        )
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\ReceiptsTab.kt

```kt
package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.viewmodel.ReceiptsUiState
import com.fordham.toolbelt.util.PlatformActions

@Composable
fun ReceiptsTab(
    uiState: ReceiptsUiState,
    selectedClient: Client?,
    allClients: List<Client>,
    filteredReceipts: List<ReceiptItem>,
    receiptsTotal: Double,
    totalWithMarkup: Double,
    onSetFilterClient: (String?) -> Unit,
    onSetClearConfirmVisible: (Boolean) -> Unit,
    onClearReceiptItems: () -> Unit,
    onReceiptUriSelected: (String) -> Unit,
    onSetClientDropdownVisible: (Boolean) -> Unit,
    onSelectClient: (Client?) -> Unit,
    onSetMarkupDialogVisible: (Boolean) -> Unit,
    onMarkupPercentageChange: (String) -> Unit,
    onProcessReceipt: () -> Unit,
    onClearCapturedReceipt: () -> Unit,
    onToggleReceiptBilled: (ReceiptItem) -> Unit,
    onDeleteReceiptItem: (ReceiptItem) -> Unit,
    platformActions: PlatformActions
) {
    LaunchedEffect(selectedClient) {
        onSetFilterClient(selectedClient?.name)
    }

    if (uiState.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { onSetClearConfirmVisible(false) },
            title = { Text("Clear All Receipts?", fontWeight = FontWeight.Black) },
            text = { Text("This will permanently delete all logged supplies and receipts. This action cannot be undone.") },
            confirmButton = { 
                TacticalButton(
                    onClick = { 
                        onClearReceiptItems()
                        onSetClearConfirmVisible(false)
                    }, 
                    text = "CLEAR ALL", 
                    containerColor = MaterialTheme.colorScheme.error
                ) 
            },
            dismissButton = { 
                TextButton(onClick = { onSetClearConfirmVisible(false) }) {
                    Text("CANCEL") 
                } 
            }
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "EXPENSE TRACKER", 
            style = MaterialTheme.typography.headlineSmall, 
            color = MaterialTheme.colorScheme.primary, 
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Text("Stop the leaks. Link receipts to specific jobs.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            OutlinedButton(
                onClick = { onSetClientDropdownVisible(true) },
                modifier = Modifier.fillMaxWidth().height(48.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp)),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ComposeColor.Transparent
                )
            ) {
                Text(
                    (selectedClient?.name ?: "SELECT PROJECT / CLIENT").uppercase(), 
                    fontWeight = FontWeight.Black, 
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
            }
            DropdownMenu(
                expanded = uiState.showClientDropdown, 
                onDismissRequest = { onSetClientDropdownVisible(false) },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                DropdownMenuItem(
                    text = { Text("GENERAL EXPENSES", fontWeight = FontWeight.Bold) }, 
                    onClick = { onSelectClient(null); onSetClientDropdownVisible(false) }
                )
                allClients.forEach { client ->
                    DropdownMenuItem(
                        text = { Text(client.name.uppercase(), fontWeight = FontWeight.Bold) }, 
                        onClick = { onSelectClient(client); onSetClientDropdownVisible(false) }
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TacticalButton(
                onClick = { 
                    platformActions.capturePhoto { uri ->
                        uri?.let { onReceiptUriSelected(it) }
                    }
                }, 
                text = "SNAP RECEIPT", 
                modifier = Modifier.weight(1f), 
                containerColor = MaterialTheme.colorScheme.primary,
                icon = { Icon(Icons.Default.CameraAlt, null) }
            )
            TacticalButton(
                onClick = { 
                    platformActions.pickImage { uri ->
                        uri?.let { onReceiptUriSelected(it) }
                    }
                }, 
                text = "UPLOAD", 
                modifier = Modifier.weight(1f), 
                containerColor = MaterialTheme.colorScheme.secondary, 
                icon = { Icon(Icons.Default.PhotoLibrary, null) }
            )
        }

        // Descriptive instructions card to utilize the empty space
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Instructions",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HOW TO TRACK EXPENSES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "1. Select a Client/Project from the dropdown above.\n2. Tap Snap Receipt or Upload to scan an expense image.\n3. Verify the captured image, click Scan & Log Receipt, and our AI engine will automatically extract line items and add them to your logged supplies below.",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Calculate, null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(16.dp))
            }
            TextButton(onClick = { onSetMarkupDialogVisible(true) }) {
                Text("MARKUP TOOL", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
            }
        }
        
        if (uiState.showMarkupDialog) {
            AlertDialog(
                onDismissRequest = { onSetMarkupDialogVisible(false) },
                title = { Text("MATERIAL MARKUP", fontWeight = FontWeight.Black) },
                text = {
                    Column {
                        Text("Add profit margin to your supplies.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.markupPercentage, 
                            onValueChange = { onMarkupPercentageChange(it) },
                            label = { Text("MARKUP %") }, 
                            modifier = Modifier.fillMaxWidth(), 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(4.dp),
                            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Black)
                        )
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("BILLED TOTAL:", fontWeight = FontWeight.Bold)
                                Text("$${totalWithMarkup}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                },
                confirmButton = { TacticalButton(onClick = { onSetMarkupDialogVisible(false) }, text = "APPLY") }
            )
        }

        if (uiState.capturedImageBytes != null) {
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp).padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    coil3.compose.AsyncImage(
                        model = uiState.capturedImageBytes,
                        contentDescription = "Captured Receipt",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = onClearCapturedReceipt,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(ComposeColor.Black.copy(alpha = 0.72f), RoundedCornerShape(999.dp))
                    ) {
                        Icon(Icons.Default.Close, "Remove receipt image", tint = ComposeColor.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        
        if (uiState.capturedImageBytes != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TacticalButton(
                    onClick = onProcessReceipt,
                    text = if (uiState.isProcessing) "ANALYZING..." else "SCAN & LOG RECEIPT",
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = { if (uiState.isProcessing) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Icon(Icons.Default.AutoAwesome, null) }
                )
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("LOGGED SUPPLIES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            TextButton(onClick = { onSetClearConfirmVisible(true) }) {
                Text("PURGE ALL", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredReceipts, key = { it.id.value }) { item ->
                ListItem(
                    headlineContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.description.uppercase(), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                            FilterChip(
                                selected = item.isBilled,
                                onClick = { onToggleReceiptBilled(item) },
                                label = { Text("BILLED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black) },
                                shape = RoundedCornerShape(2.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ComposeColor(0xFF00E676),
                                    selectedLabelColor = ComposeColor.Black
                                ),
                                border = if (item.isBilled) null else FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            )
                        }
                    },
                    supportingContent = { Text(item.formattedDetails, fontWeight = FontWeight.Bold) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.formattedPrice, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            IconButton(onClick = { onDeleteReceiptItem(item) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = ComposeColor.Transparent)
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), 
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TOTAL EXPENSES", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("$${totalWithMarkup}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\SettingsTab.kt

```kt
package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.ui.theme.*
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.viewmodel.SyncState
import com.fordham.toolbelt.util.PlatformActions

@Composable
fun SettingsTab(
    settings: BusinessSettings,
    currentUser: com.fordham.toolbelt.domain.repository.FordhamUser?,
    onSaveSettings: (BusinessSettings) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSync: () -> Unit,
    syncState: SyncState,
    platformActions: PlatformActions
) {
    var tempSettings by remember(settings) { mutableStateOf(settings) }
    val scrollState = rememberScrollState()
    val isDarkMode = MaterialTheme.colorScheme.background == Color(0xFF000000)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            "APPLICATION SETTINGS",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        
        Spacer(Modifier.height(24.dp))

        // ACCOUNT & CLOUD SECTION
        SettingsSection(title = "ACCOUNT & CLOUD SYNC") {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (currentUser == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cloud Backup Inactive", fontWeight = FontWeight.Bold)
                            Text("Sign in with Google to enable cloud backup and syncing.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        TacticalButton(
                            onClick = onSignIn,
                            text = "SIGN IN",
                            containerColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.height(40.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val photoUrl = currentUser.photoUrl
                        if (photoUrl != null) {
                            com.fordham.toolbelt.ui.components.CircleImage(
                                url = photoUrl.value,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentUser.displayName?.value ?: "Contractor", fontWeight = FontWeight.Black)
                            Text(currentUser.email?.value ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        TextButton(onClick = onSignOut) {
                            Text("SIGN OUT", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google Drive Backup", fontWeight = FontWeight.Bold)
                            val statusText = when (syncState) {
                                is SyncState.Syncing -> "Uploading encrypted app backup..."
                                is SyncState.Success -> "Backup complete!"
                                is SyncState.Error -> "Error: ${syncState.message}"
                                else -> "Backs up app data to your private Drive app folder."
                            }
                            val statusColor = when (syncState) {
                                is SyncState.Success -> MaterialTheme.colorScheme.primary
                                is SyncState.Error -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                        }
                        TacticalButton(
                            onClick = onSync,
                            text = if (syncState is SyncState.Syncing) "" else "SYNC NOW",
                            containerColor = if (syncState is SyncState.Success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(40.dp),
                            enabled = syncState !is SyncState.Syncing,
                            icon = {
                                if (syncState is SyncState.Syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else if (syncState is SyncState.Success) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // PREMIUM SECTION
        SettingsSection(title = "PRO FEATURES") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pro Premium Status", fontWeight = FontWeight.Bold)
                    Text(
                        if (tempSettings.isPremium) "UNLOCKED: Bento Reports & AI Control Center active" 
                        else "LOCKED: Basic functionality only",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (tempSettings.isPremium) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                Switch(
                    checked = tempSettings.isPremium,
                    onCheckedChange = { 
                        val newSettings = tempSettings.copy(isPremium = it)
                        tempSettings = newSettings
                        onSaveSettings(newSettings)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandOrange,
                        checkedBorderColor = BrandOrange,
                        uncheckedThumbColor = if (isDarkMode) Color.Gray else Color.White,
                        uncheckedTrackColor = if (isDarkMode) Color(0xFF222222) else Color(0xFFCCCCCC),
                        uncheckedBorderColor = if (isDarkMode) Color(0xFF444444) else Color(0xFF999999)
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // APPEARANCE SECTION
        SettingsSection(title = "APPEARANCE") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark Mode", fontWeight = FontWeight.Bold)
                Switch(
                    checked = tempSettings.isDarkMode,
                    onCheckedChange = { 
                        val newSettings = tempSettings.copy(isDarkMode = it)
                        tempSettings = newSettings
                        onSaveSettings(newSettings)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandOrange,
                        checkedBorderColor = BrandOrange,
                        uncheckedThumbColor = if (isDarkMode) Color.Gray else Color.White,
                        uncheckedTrackColor = if (isDarkMode) Color(0xFF222222) else Color(0xFFCCCCCC),
                        uncheckedBorderColor = if (isDarkMode) Color(0xFF444444) else Color(0xFF999999)
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // PRIVACY & PERMISSIONS SECTION (Google Policy Compliance)
        SettingsSection(title = "PRIVACY & PERMISSIONS") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Sensitive Permissions Disclosure", 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Invoice Hammer requires the following permissions to function. We only access these when you actively use the corresponding features:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                PermissionItem(
                    icon = Icons.Default.Mic,
                    title = "Microphone Access",
                    description = "Used for the AI Command Center to process voice commands. No audio is stored outside of active processing."
                )
                
                PermissionItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Camera Access",
                    description = "Used for scanning receipts and capturing job photos. Images are stored locally in your directory."
                )
                
                PermissionItem(
                    icon = Icons.Default.Storage,
                    title = "File Storage",
                    description = "Used to save, export, and manage your invoices, estimates, and PDF reports."
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Spacer(Modifier.height(32.dp))

        TacticalButton(
            onClick = { 
                onSaveSettings(tempSettings)
                platformActions.showToast("Settings Saved")
            },
            text = "SAVE ALL CHANGES",
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\stats\BentoCard.kt

```kt
package com.fordham.toolbelt.ui.tabs.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Responsibility: Versatile card container for the Bento grid with support for generative backgrounds.
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subValue: String? = null,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary,
    gradient: List<Color>? = null,
    isWide: Boolean = false,
    content: (@Composable () -> Unit)? = null,
    backgroundContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (gradient != null) Modifier.background(Brush.verticalGradient(gradient))
                    else Modifier
                )
        ) {
            backgroundContent?.invoke()

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).graphicsLayer(alpha = 0.5f),
                    tint = color
                )
                
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        letterSpacing = 1.sp
                    )
                    if (content != null) {
                        content()
                    } else {
                        Text(
                            text = value,
                            style = if (isWide) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = color
                        )
                    }
                    if (subValue != null) {
                        Text(
                            text = subValue,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrendLineBackground(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path()
        val width = size.width
        val height = size.height
        
        path.moveTo(0f, height * 0.8f)
        path.quadraticTo(width * 0.2f, height * 0.85f, width * 0.4f, height * 0.6f)
        path.quadraticTo(width * 0.6f, height * 0.35f, width * 0.8f, height * 0.4f)
        path.lineTo(width, height * 0.1f)
        
        drawPath(
            path = path,
            color = color.copy(alpha = 0.3f),
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun TimePulseBackground(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.8f, size.height * 0.4f)
        drawCircle(
            color = color.copy(alpha = 0.15f),
            radius = 60f,
            center = center,
            style = Stroke(width = 4f)
        )
        drawCircle(
            color = color.copy(alpha = 0.1f),
            radius = 100f,
            center = center,
            style = Stroke(width = 2f)
        )
    }
}

@Composable
fun ReceiptDottedBackground(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (i in 0..5) {
            for (j in 0..3) {
                drawCircle(
                    color = color.copy(alpha = 0.2f),
                    radius = 3f,
                    center = Offset(size.width * 0.7f + (i * 15f), size.height * 0.2f + (j * 15f))
                )
            }
        }
    }
}

@Composable
fun WavesBackground(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path()
        val w = size.width
        val h = size.height
        path.moveTo(0f, h * 0.7f)
        for (i in 1..4) {
            val x = w * (i / 4f)
            val y = if (i % 2 == 0) h * 0.6f else h * 0.8f
            path.lineTo(x, y)
        }
        drawPath(path, color.copy(alpha = 0.2f), style = Stroke(4f))
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\stats\BentoGrid.kt

```kt
package com.fordham.toolbelt.ui.tabs.stats

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.BusinessStats
import com.fordham.toolbelt.ui.theme.StatsGreen
import com.fordham.toolbelt.util.DateTimeUtil

/**
 * Responsibility: Layout orchestrator for the Bento-style statistics grid.
 */
@Composable
fun BentoGrid(stats: BusinessStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Main Row: Health & Margin
        Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoCard(
                modifier = Modifier.weight(1.8f),
                title = "NET PROFIT YTD",
                value = stats.formattedNetProfit,
                icon = Icons.Default.TrendingUp,
                color = StatsGreen,
                backgroundContent = {
                    TrendLineBackground(StatsGreen)
                }
            )
            BentoCard(
                modifier = Modifier.weight(1f),
                title = "MARGIN",
                value = "${stats.profitMargin}%",
                icon = Icons.Default.PieChart,
                content = {
                    Box(modifier = Modifier.size(70.dp).padding(top = 4.dp), contentAlignment = Alignment.Center) {
                        DonutChartSmall(stats.netProfit, stats.profitMargin)
                        Text("${stats.profitMargin}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }

        // Second Row: Hours & Unbilled
        Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val totalHours = stats.totalDurationSeconds / 3600.0
            BentoCard(
                modifier = Modifier.weight(1f),
                title = "TOTAL HOURS",
                value = DateTimeUtil.formatDecimal(totalHours, 1),
                subValue = "BILLABLE",
                icon = Icons.Default.Timer,
                color = MaterialTheme.colorScheme.primary,
                backgroundContent = {
                    TimePulseBackground(MaterialTheme.colorScheme.primary)
                }
            )
            BentoCard(
                modifier = Modifier.weight(1f),
                title = "UNBILLED",
                value = stats.formattedUnbilledExpenses,
                subValue = "FLOATING COSTS",
                icon = Icons.Default.ReceiptLong,
                color = MaterialTheme.colorScheme.secondary,
                backgroundContent = {
                    ReceiptDottedBackground(MaterialTheme.colorScheme.secondary)
                }
            )
        }
        
        // Third Row: Total Expenses
        BentoCard(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            title = "TOTAL BUSINESS EXPENSES",
            value = DateTimeUtil.formatMoney(stats.totalExpenses),
            icon = Icons.Default.Payments,
            isWide = true,
            color = MaterialTheme.colorScheme.primary,
            backgroundContent = {
                WavesBackground(MaterialTheme.colorScheme.primary)
            }
        )
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\stats\ProjectStatCard.kt

```kt
package com.fordham.toolbelt.ui.tabs.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.ProjectStat
import com.fordham.toolbelt.ui.theme.StatsGreen

/**
 * Responsibility: Display profitability and progress for a specific project.
 */
@Composable
fun ProjectStatCard(project: ProjectStat) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        border = if (project.profit < 0) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { 
                Text(project.clientName, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(project.formattedProfit, color = if (project.profit >= 0) StatsGreen else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium) 
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { project.progress }, 
                modifier = Modifier.fillMaxWidth().height(6.dp), 
                color = if (project.profit >= 0) StatsGreen else MaterialTheme.colorScheme.error, 
                strokeCap = StrokeCap.Round,
                trackColor = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                Text(project.formattedRevenue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                Text(project.formattedExpenses, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\stats\StatsUiComponents.kt

```kt
package com.fordham.toolbelt.ui.tabs.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import com.fordham.toolbelt.ui.theme.StatsGreen

/**
 * Responsibility: Specialized chart components for the Stats tab.
 */
@Composable
fun DonutChartSmall(profit: Double, margin: Int) {
    val sweepAngle = (margin.toFloat() / 100f * 360f).coerceIn(0f, 360f)
    val arcColor = if (profit >= 0) StatsGreen else MaterialTheme.colorScheme.error
    
    val trackColor = MaterialTheme.colorScheme.outline
    Canvas(modifier = Modifier.fillMaxSize()) { 
        drawArc(trackColor, 0f, 360f, false, style = Stroke(15f))
        drawArc(arcColor, -90f, sweepAngle, false, style = Stroke(15f)) 
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\StatsTab.kt

```kt
package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.BusinessStats
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.tabs.stats.*

/**
 * Responsibility: Main orchestration for the Statistics & Analytics tab.
 * ADHERENCE: Below 300 line limit.
 */
@Composable
fun StatsTab(
    stats: BusinessStats,
    settings: BusinessSettings,
    onExportCsv: () -> Unit,
    onExportZip: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onInsertStressInvoices: () -> Unit,
    onEraseAllInvoices: () -> Unit
) {
    var showLockDialog by remember { mutableStateOf(false) }

    if (showLockDialog) {
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            title = { Text("PRO FEATURE LOCKED", fontWeight = FontWeight.Black) },
            text = { Text("Bento Reporting and Tax Bundles are premium features. Upgrade to Pro in Settings to unlock these tools.") },
            confirmButton = {
                TacticalButton(
                    onClick = { 
                        showLockDialog = false
                        onNavigateToSettings()
                    },
                    text = "GO TO SETTINGS"
                )
            },
            dismissButton = {
                TextButton(onClick = { showLockDialog = false }) { Text("CANCEL") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "BUSINESS ANALYTICS",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = if (settings.isPremium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (settings.isPremium) "PRO ACCOUNT" else "FREE ACCOUNT",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (settings.isPremium) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // BENTO GRID
        BentoGrid(stats)

        Spacer(Modifier.height(24.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TacticalButton(
                onClick = { if (settings.isPremium) onExportCsv() else showLockDialog = true },
                text = "BENTO REPORT", 
                modifier = Modifier.weight(1f), 
                containerColor = if (settings.isPremium) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                icon = { Icon(Icons.Default.TableChart, null) }
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, 
                modifier = Modifier.clickable { onNavigateToSettings() }
            ) {
                Icon(
                    if (settings.isPremium) Icons.Default.LockOpen else Icons.Default.Lock, 
                    null, 
                    tint = if (settings.isPremium) MaterialTheme.colorScheme.primary else Color.Gray, 
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    if (settings.isPremium) "PRO" else "LOCK", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Black, 
                    color = if (settings.isPremium) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }

            TacticalButton(
                onClick = { if (settings.isPremium) onExportZip() else showLockDialog = true },
                text = "TAX BUNDLE", 
                modifier = Modifier.weight(1f), 
                containerColor = if (settings.isPremium) MaterialTheme.colorScheme.secondary else Color.Gray.copy(alpha = 0.5f), 
                icon = { Icon(Icons.Default.Archive, null) }
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "SYSTEM DIAGNOSTICS & STRESS TESTING",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TacticalButton(
                onClick = onInsertStressInvoices,
                text = "SIMULATE 1000",
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.tertiary,
                icon = { Icon(Icons.Default.Bolt, null) }
            )
            TacticalButton(
                onClick = onEraseAllInvoices,
                text = "PURGE VAULT",
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.error,
                icon = { Icon(Icons.Default.Delete, null) }
            )
        }

        Spacer(Modifier.height(32.dp))
        Text("PROJECT PROFITABILITY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        
        stats.projectStats.forEach { project ->
            ProjectStatCard(project)
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\suppliers\AddSupplierBottomSheet.kt

```kt
package com.fordham.toolbelt.ui.tabs.suppliers

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.SupplierCategory
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.util.PlaceSuggestion

/**
 * Responsibility: Bottom sheet for creating a new custom supplier with place suggestions and photo capture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupplierBottomSheet(
    onDismiss: () -> Unit,
    onSave: (String, SupplierCategory, String, String, String?) -> Unit,
    onSnapPhoto: () -> Unit,
    photoUri: String?,
    suggestions: List<PlaceSuggestion>,
    onQueryChange: (String) -> Unit,
    onClearSuggestions: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(SupplierCategory.HARDWARE) }
    var showSuggestions by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "NEW SUPPLIER",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { 
                    name = it
                    onQueryChange(it)
                    showSuggestions = it.isNotBlank()
                },
                label = { Text("Store Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (showSuggestions && suggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    suggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion.name) },
                            onClick = {
                                name = suggestion.name
                                address = suggestion.address
                                showSuggestions = false
                                onClearSuggestions()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text("CATEGORY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                SupplierCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TacticalButton(
                    onClick = onSnapPhoto,
                    text = "SNAP PHOTO",
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                if (photoUri != null) {
                    Spacer(Modifier.width(16.dp))
                    Card(modifier = Modifier.size(60.dp), shape = RoundedCornerShape(8.dp)) {
                        coil3.compose.AsyncImage(
                            model = photoUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            TacticalButton(
                onClick = { onSave(name, category, address, phone, photoUri) },
                text = "CREATE SUPPLIER",
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            )
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\suppliers\QuickLogPurchaseDialog.kt

```kt
package com.fordham.toolbelt.ui.tabs.suppliers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.Supplier
import com.fordham.toolbelt.ui.components.TacticalButton

/**
 * Responsibility: Dialog for quickly logging a manual expense against a specific supplier.
 */
@Composable
fun QuickLogPurchaseDialog(
    supplier: Supplier,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LOG EXPENSE: ${supplier.name.uppercase()}", fontWeight = FontWeight.Black) },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                    label = { Text("Amount ($)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (e.g. Materials for Smith Job)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TacticalButton(
                onClick = { 
                    val valAmount = amount.toDoubleOrNull() ?: 0.0
                    onSave(valAmount, description)
                },
                text = "SAVE",
                enabled = amount.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\suppliers\ReorderItem.kt

```kt
package com.fordham.toolbelt.ui.tabs.suppliers

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Responsibility: Display a supplier item in reorder mode with drag-to-sort capabilities.
 */
@Composable
fun androidx.compose.foundation.lazy.LazyItemScope.ReorderItem(
    index: Int,
    itemCount: Int,
    uiModel: SupplierUiModel,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onDragMove: (Int, Int) -> Unit
) {
    var offsetY by remember { mutableStateOf(0f) }
    val supplier = uiModel.domain
    val moveThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetY.roundToInt()) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Long press and drag to reorder",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(32.dp)
                    .pointerInput(index, itemCount, moveThresholdPx) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offsetY = 0f },
                            onDragEnd = {
                                val targetIndex = (index + (offsetY / moveThresholdPx).roundToInt())
                                    .coerceIn(0, itemCount - 1)
                                if (targetIndex != index) {
                                    onDragMove(index, targetIndex)
                                }
                                offsetY = 0f
                            },
                            onDragCancel = { offsetY = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetY += dragAmount.y
                            }
                        )
                    }
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(supplier.name.uppercase(), fontWeight = FontWeight.Bold)
                Text(supplier.category.name, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\suppliers\SuppliersUiUtils.kt

```kt
package com.fordham.toolbelt.ui.tabs.suppliers

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Responsibility: Small, shared UI elements for the Suppliers tab.
 */
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\suppliers\SupplierTile.kt

```kt
package com.fordham.toolbelt.ui.tabs.suppliers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fordham.toolbelt.util.PlatformActions
import invoicehammer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Responsibility: Display a single supplier in a high-fidelity tile with action menu.
 */
@Composable
fun SupplierTile(
    uiModel: SupplierUiModel,
    onTogglePin: () -> Unit,
    onHide: () -> Unit,
    onLogExpense: () -> Unit,
    platformActions: PlatformActions
) {
    val supplier = uiModel.domain
    var showMenu by remember { mutableStateOf(false) }
    
    Box {
        Card(
            onClick = { 
                platformActions.launchApp(
                    packageName = supplier.packageName, 
                    fallbackUrl = supplier.webUrl
                ) 
            },
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        color = Color.White.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (supplier.logoResName != null) {
                                val logoRes = when(supplier.logoResName) {
                                    "logo_home_depot" -> Res.drawable.logo_home_depot
                                    "logo_lowes" -> Res.drawable.logo_lowes
                                    "logo_ace" -> Res.drawable.logo_ace
                                    "logo_menards" -> Res.drawable.logo_menards
                                    "logo_ferguson" -> Res.drawable.logo_ferguson
                                    "logo_sherwin" -> Res.drawable.logo_sherwin
                                    "logo_grainger" -> Res.drawable.logo_grainger
                                    "logo_abc" -> Res.drawable.logo_abc
                                    "logo_graybar" -> Res.drawable.logo_graybar
                                    "logo_siteone" -> Res.drawable.logo_siteone
                                    "logo_amazon" -> Res.drawable.logo_amazon
                                    "logo_northern" -> Res.drawable.logo_northern
                                    "logo_sunbelt" -> Res.drawable.logo_sunbelt
                                    "logo_hilti" -> Res.drawable.logo_hilti
                                    "logo_mcmaster" -> Res.drawable.logo_mcmaster
                                    else -> null
                                }
                                
                                if (logoRes != null) {
                                    Image(
                                        painter = painterResource(logoRes),
                                        contentDescription = supplier.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Icon(Icons.Default.Storefront, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                Icon(
                                    Icons.Default.Storefront, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.MoreVert,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = supplier.name.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = supplier.category.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            DropdownMenuItem(
                text = { Text("Log Expense", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.AddCard, null) },
                onClick = { onLogExpense(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text(if (supplier.isPinned) "Unpin from Top" else "Pin to Top", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.PushPin, null) },
                onClick = { onTogglePin(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Hide Store", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                onClick = { onHide(); showMenu = false }
            )
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\suppliers\SupplierUiModel.kt

```kt
package com.fordham.toolbelt.ui.tabs.suppliers

import com.fordham.toolbelt.domain.model.Supplier

/**
 * Responsibility: UI-specific representation of a Supplier.
 */
data class SupplierUiModel(
    val domain: Supplier,
    val logoKey: String? = null,
    val categoryIconKey: String? = null
)

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\tabs\SuppliersTab.kt

```kt
package com.fordham.toolbelt.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fordham.toolbelt.domain.model.Supplier
import com.fordham.toolbelt.domain.model.SupplierCategory
import com.fordham.toolbelt.domain.model.SupplierId
import com.fordham.toolbelt.util.PlaceSuggestion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.fordham.toolbelt.ui.components.TacticalButton
import com.fordham.toolbelt.ui.tabs.suppliers.*
import com.fordham.toolbelt.ui.viewmodel.SuppliersData
import com.fordham.toolbelt.ui.viewmodel.SuppliersOutcome
import com.fordham.toolbelt.util.PlatformActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersTab(
    uiState: SuppliersOutcome,
    isAddSheetVisible: Boolean,
    placeSuggestions: List<PlaceSuggestion>,
    isReorderMode: Boolean,
    reorderList: List<SupplierUiModel>,
    onTogglePin: (Supplier) -> Unit,
    onHideSupplier: (SupplierId) -> Unit,
    onAddClick: () -> Unit,
    onDismissAdd: () -> Unit,
    onAddSupplier: (String, SupplierCategory, String, String, String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSuggestions: () -> Unit,
    onToggleReorder: (Boolean, List<SupplierUiModel>) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onSaveOrder: () -> Unit,
    hiddenSuppliers: List<SupplierUiModel>,
    onRestoreSupplier: (SupplierId) -> Unit,
    onLogPurchase: (SupplierId, String, Double, String) -> Unit,
    onOpenStore: (String) -> Unit,
    onSnapPhoto: (String) -> Unit,
    photoUri: String?,
    platformActions: PlatformActions
) {
    var showHiddenStores by remember { mutableStateOf(false) }
    var supplierToLog by remember { mutableStateOf<Supplier?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isReorderMode) "REORDER" else "SUPPLIERS",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isReorderMode) "Set your absolute priority" else "Your personalized supply network",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isReorderMode) {
                    TacticalButton(
                        onClick = onSaveOrder,
                        text = "DONE",
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(
                            onClick = onAddClick,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFFFF6A00),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Supplier")
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { 
                                if (uiState is SuppliersOutcome.Success) {
                                    val fullList = uiState.data.pinnedSuppliers + uiState.data.activeSuppliers
                                    onToggleReorder(true, fullList)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Sort, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isReorderMode) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = reorderList,
                        key = { _, model -> model.domain.id.value }
                    ) { index, uiModel ->
                        ReorderItem(
                            index = index,
                            itemCount = reorderList.size,
                            uiModel = uiModel,
                            onMoveUp = { if (index > 0) onMoveItem(index, index - 1) },
                            onMoveDown = { if (index < reorderList.size - 1) onMoveItem(index, index + 1) },
                            onRemove = { onHideSupplier(uiModel.domain.id) },
                            onDragMove = onMoveItem
                        )
                    }
                }
            } else {
                when (uiState) {
                    is SuppliersOutcome.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is SuppliersOutcome.Success -> {
                        val state = uiState.data
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(
                                start = 16.dp, 
                                top = 16.dp, 
                                end = 16.dp, 
                                bottom = 100.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (state.pinnedSuppliers.isNotEmpty()) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                    SectionHeader(title = "PINNED FAVORITES")
                                }
                                items(state.pinnedSuppliers) { uiModel ->
                                    SupplierTile(
                                        uiModel = uiModel,
                                        onTogglePin = { onTogglePin(uiModel.domain) },
                                        onHide = { onHideSupplier(uiModel.domain.id) },
                                        onLogExpense = { supplierToLog = uiModel.domain },
                                        platformActions = platformActions
                                    )
                                }
                            }

                            if (state.activeSuppliers.isNotEmpty()) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                    SectionHeader(title = "ACTIVE SUPPLIERS")
                                }
                                items(state.activeSuppliers) { uiModel ->
                                    SupplierTile(
                                        uiModel = uiModel,
                                        onTogglePin = { onTogglePin(uiModel.domain) },
                                        onHide = { onHideSupplier(uiModel.domain.id) },
                                        onLogExpense = { supplierToLog = uiModel.domain },
                                        platformActions = platformActions
                                    )
                                }
                            }

                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                Spacer(modifier = Modifier.height(32.dp))
                                if (hiddenSuppliers.isNotEmpty()) {
                                    TextButton(
                                        onClick = { showHiddenStores = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.VisibilityOff, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("MANAGE HIDDEN STORES (${hiddenSuppliers.size})", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    is SuppliersOutcome.Failure -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = uiState.error.value, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        if (showHiddenStores) {
            ModalBottomSheet(
                onDismissRequest = { showHiddenStores = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Text("HIDDEN STORES", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(hiddenSuppliers) { supplier ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(supplier.domain.name.uppercase(), fontWeight = FontWeight.Bold)
                                    TacticalButton(
                                        onClick = { onRestoreSupplier(supplier.domain.id) },
                                        text = "RESTORE"
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }

        if (isAddSheetVisible) {
            AddSupplierBottomSheet(
                onDismiss = onDismissAdd,
                onSave = onAddSupplier,
                onSnapPhoto = { 
                    platformActions.capturePhoto { uri ->
                        uri?.let { onSnapPhoto(it) }
                    }
                },
                photoUri = photoUri,
                suggestions = placeSuggestions,
                onQueryChange = onSearchQueryChange,
                onClearSuggestions = onClearSuggestions
            )
        }

        supplierToLog?.let { supplier ->
            QuickLogPurchaseDialog(
                supplier = supplier,
                onDismiss = { supplierToLog = null },
                onSave = { amount, desc ->
                    onLogPurchase(supplier.id, supplier.name, amount, desc)
                    supplierToLog = null
                }
            )
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\theme\Color.kt

```kt
package com.fordham.toolbelt.ui.theme

import androidx.compose.ui.graphics.Color

// Global Foundation
val GlobalBackground = Color(0xFFE0E0E0) // Original Bone/Gray Background
val DarkBackground = Color(0xFF000000) // Pure Black for Dark Mode
val BrandOrange = Color(0xFFFC6600) // Original Vibrant Construction Orange
val HeaderWhite = Color(0xFFFFFFFF)
val UtilityGray = Color(0xFF7C7C7C)
val Bone = Color(0xFFE3DAC9)

// Tabs - New / Invoice
val FieldBg = Color(0xFFFFFFFF) // White fields in light mode
val FieldBorder = Color(0xFF000000) // Sharp black borders
val PlaceholderGray = Color(0xFF757575)

// Navigation
val BottomNavBg = Color(0xFFFFFFFF)
val BottomNavInactive = Color(0xFF757575)
val SelectedNavPill = Color(0xFFFC6600)
val SelectedNavLabel = Color(0xFF000000)

// Helper Colors (Restored for compilation)
val StatsGreen = Color(0xFF00E676)
val PrimaryYellow = Color(0xFFFFD400)
val ExpensesPurple = Color(0xFF5A3FA8)
val StatsCardBg = Color(0xFF161616)
val StatsCardBorder = Color(0xFF2A2A2A)
val NavPillPurple = Color(0xFF5E547A)
val GlobalCardBg = Color(0xFF171717)
val GlobalCardBorder = Color(0xFF303030)

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\theme\Theme.kt

```kt
package com.fordham.toolbelt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

val IndustrialDarkColors = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    secondary = BrandOrange,
    onSecondary = Color.Black,
    surface = DarkBackground,
    background = DarkBackground,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurface = Color.White,
    onBackground = Color.White,
    error = Color(0xFFE57373),
    outline = Color.White
)

val IndustrialLightColors = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    secondary = BrandOrange,
    onSecondary = Color.White,
    surface = Color.White,
    background = GlobalBackground,
    surfaceVariant = Color.White,
    onSurface = Color.Black,
    onBackground = Color.Black,
    outline = Color.Black
)

val ToolbeltTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun ToolbeltTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) IndustrialDarkColors else IndustrialLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ToolbeltTypography,
        content = content
    )
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\ToolbeltNavHost.kt

```kt
package com.fordham.toolbelt.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.ui.tabs.*
import com.fordham.toolbelt.ui.tabs.suppliers.SupplierUiModel
import com.fordham.toolbelt.ui.viewmodel.*
import com.fordham.toolbelt.util.PlatformActions
import com.fordham.toolbelt.util.Permission

@Composable
fun ToolbeltNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    newInvoiceViewModel: NewInvoiceViewModel,
    sharedViewModel: SharedViewModel,
    clientsViewModel: ClientsViewModel,
    historyViewModel: HistoryViewModel,
    receiptsViewModel: ReceiptsViewModel,
    statsViewModel: StatsViewModel,
    suppliersViewModel: SuppliersViewModel,
    authViewModel: AuthViewModel,
    platformActions: PlatformActions
) {
    NavHost(
        navController = navController,
        startDestination = Screen.NewInvoice,
        modifier = modifier
    ) {
        composable<Screen.NewInvoice> {
            val uiState by newInvoiceViewModel.uiState.collectAsStateWithLifecycle()
            val businessSettings by sharedViewModel.businessSettings.collectAsStateWithLifecycle(initialValue = BusinessSettings())
            val allClients by clientsViewModel.allClients.collectAsStateWithLifecycle(initialValue = emptyList())

            NewInvoiceTab(
                uiState = uiState,
                businessSettings = businessSettings,
                allClients = allClients,
                categories = newInvoiceViewModel.categories,
                onSaveBusinessSettings = { sharedViewModel.saveBusinessSettings(it) },
                onTimerToggle = { newInvoiceViewModel.onIntent(NewInvoiceIntent.ToggleTimer) },
                onHourlyRateChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnHourlyRateChange(it)) },
                onBillLabor = { newInvoiceViewModel.onIntent(NewInvoiceIntent.BillLabor) },
                onLogoUriChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnLogoUriChange(it)) },
                onPhotoCaptured = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnPhotoCaptured(it)) },
                onClientNameChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnClientNameChange(it)) },
                onClientAddressChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnClientAddressChange(it)) },
                onSetInvoiceClientDropdownVisible = { newInvoiceViewModel.onIntent(NewInvoiceIntent.SetClientDropdownVisible(it)) },
                onSaveToClientDirectoryChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnSaveToClientDirectoryChange(it)) },
                onRemovePhoto = { newInvoiceViewModel.onIntent(NewInvoiceIntent.RemovePhoto(it)) },
                onSetReceiptPickerVisible = { newInvoiceViewModel.onIntent(NewInvoiceIntent.SetReceiptPickerVisible(it)) },
                onSetInvoiceCategoryDropdownVisible = { newInvoiceViewModel.onIntent(NewInvoiceIntent.SetCategoryDropdownVisible(it)) },
                onCategoryChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnCategoryChange(it)) },
                onItemDescChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnItemDescChange(it)) },
                onItemAmtChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnItemAmtChange(it)) },
                onProcessInvoiceAi = { newInvoiceViewModel.onIntent(NewInvoiceIntent.ProcessInvoiceAi(it)) },
                onAddManualLineItem = { newInvoiceViewModel.onIntent(NewInvoiceIntent.AddManualLineItem) },
                onRemoveLineItem = { newInvoiceViewModel.onIntent(NewInvoiceIntent.RemoveLineItem(it)) },
                onTaxTextChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnTaxTextChange(it)) },
                onDepositCollectedChange = { newInvoiceViewModel.onIntent(NewInvoiceIntent.OnDepositCollectedChange(it)) },
                onSaveInvoice = { isEstimate, settings, onComplete ->
                    newInvoiceViewModel.onIntent(NewInvoiceIntent.SaveInvoice(isEstimate, settings, onComplete))
                },
                onLinkReceipt = { receipt, markup -> newInvoiceViewModel.onIntent(NewInvoiceIntent.LinkReceipt(receipt, markup)) },
                onShareFile = { file, title -> platformActions.shareFile(file, title) },
                isPremium = businessSettings.isPremium,
                platformActions = platformActions
            )
        }
        composable<Screen.History> {
            val uiState by historyViewModel.uiState.collectAsStateWithLifecycle()
            val filteredHistory by historyViewModel.filteredInvoices.collectAsStateWithLifecycle(initialValue = emptyList())

            HistoryTab(
                uiState = uiState,
                filteredHistory = filteredHistory,
                paymentRequests = emptyList(),
                onViewPdf = { file -> platformActions.openPdf(file) },
                onSharePdf = { file, title -> platformActions.shareFile(file, title) },
                onRequestDeposit = { platformActions.showToast("Open this screen from the main shell to use mock Stellar payments.") },
                onRequestFullPayment = { platformActions.showToast("Open this screen from the main shell to use mock Stellar payments.") },
                onSetInvoiceToDelete = { historyViewModel.setInvoiceToDelete(it) },
                onDeleteInvoice = { historyViewModel.deleteInvoice(it) },
                onSearchQueryChange = { historyViewModel.onSearchQueryChange(it) },
                onShowPaidOnlyChange = { historyViewModel.onShowPaidOnlyChange(it) },
                onUpdateInvoice = { historyViewModel.updateInvoice(it) },
                onConvertEstimateToInvoice = { historyViewModel.convertEstimateToInvoice(it) },
                platformActions = platformActions
            )
        }
        composable<Screen.Receipts> {
            val uiState by receiptsViewModel.uiState.collectAsStateWithLifecycle()
            val selectedClient by sharedViewModel.selectedClient.collectAsStateWithLifecycle()
            val allClients by sharedViewModel.allClients.collectAsStateWithLifecycle(initialValue = emptyList())
            val filteredReceipts by receiptsViewModel.filteredReceipts.collectAsStateWithLifecycle(initialValue = emptyList())
            val receiptsTotal by receiptsViewModel.receiptsTotal.collectAsStateWithLifecycle(initialValue = 0.0)
            val totalWithMarkup by receiptsViewModel.totalWithMarkup.collectAsStateWithLifecycle(initialValue = 0.0)

            ReceiptsTab(
                uiState = uiState,
                selectedClient = selectedClient,
                allClients = allClients,
                filteredReceipts = filteredReceipts,
                receiptsTotal = receiptsTotal,
                totalWithMarkup = totalWithMarkup,
                onSetFilterClient = { receiptsViewModel.setFilterClient(it) },
                onSetClearConfirmVisible = { receiptsViewModel.setClearConfirmVisible(it) },
                onClearReceiptItems = { receiptsViewModel.clearReceiptItems() },
                onReceiptUriSelected = { receiptsViewModel.onReceiptUriSelected(it) },
                onSetClientDropdownVisible = { receiptsViewModel.setClientDropdownVisible(it) },
                onSelectClient = { sharedViewModel.selectClient(it) },
                onSetMarkupDialogVisible = { receiptsViewModel.setMarkupDialogVisible(it) },
                onMarkupPercentageChange = { receiptsViewModel.onMarkupPercentageChange(it) },
                onProcessReceipt = { receiptsViewModel.processCapturedReceipt(selectedClient) {} },
                onClearCapturedReceipt = { receiptsViewModel.clearCapturedReceiptImage() },
                onToggleReceiptBilled = { receiptsViewModel.toggleReceiptBilled(it) },
                onDeleteReceiptItem = { receiptsViewModel.deleteReceiptItem(it) },
                platformActions = platformActions
            )
        }
        composable<Screen.Stats> {
            val stats by statsViewModel.businessStats.collectAsStateWithLifecycle(initialValue = BusinessStats())
            val settings by statsViewModel.businessSettings.collectAsStateWithLifecycle()

            StatsTab(
                stats = stats,
                settings = settings,
                onExportCsv = {
                    statsViewModel.exportBentoReport { file: String ->
                        platformActions.shareFile(file, "Tax Report")
                    }
                },
                onExportZip = {
                    statsViewModel.exportTaxBundle { file: String ->
                        platformActions.shareFile(file, "Tax Bundle")
                    }
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings) },
                onInsertStressInvoices = { statsViewModel.createStressTestInvoices() },
                onEraseAllInvoices = { statsViewModel.eraseAllInvoices() }
            )
        }
        composable<Screen.Clients> {
            val clients by clientsViewModel.allClients.collectAsStateWithLifecycle(initialValue = emptyList())
            val selectedClient by sharedViewModel.selectedClient.collectAsStateWithLifecycle()
            val clientInvoices by sharedViewModel.selectedClientInvoices.collectAsStateWithLifecycle()
            val summary by sharedViewModel.selectedClientSummary.collectAsStateWithLifecycle()
            val jobNotes by sharedViewModel.selectedClientNotes.collectAsStateWithLifecycle()
            val jobPhotos by sharedViewModel.selectedClientPhotos.collectAsStateWithLifecycle()
            val uiState by clientsViewModel.uiState.collectAsStateWithLifecycle()

            ClientsTab(
                clients = clients,
                selectedClient = selectedClient,
                clientInvoices = clientInvoices,
                summary = summary,
                jobNotes = jobNotes,
                jobPhotos = jobPhotos,
                uiState = uiState,
                onClientClick = { sharedViewModel.selectClient(it) },
                onDeleteClient = { clientsViewModel.onIntent(ClientsIntent.DeleteClient(it)) },
                onSetClientToDelete = { clientsViewModel.onIntent(ClientsIntent.SetClientToDelete(it)) },
                onBackClick = { sharedViewModel.selectClient(null) },
                onAddNote = { clientsViewModel.onIntent(ClientsIntent.AddNote(it)) },
                onDeleteNote = { clientsViewModel.onIntent(ClientsIntent.DeleteNote(it)) },
                onSummarizeNotes = { clientsViewModel.onIntent(ClientsIntent.SummarizeNotes(jobNotes)) },
                onLinkReceipt = { receipt -> 
                    selectedClient?.let { clientsViewModel.onIntent(ClientsIntent.LinkReceipt(receipt, it.name)) }
                },
                onViewPdf = { file -> platformActions.openPdf(file) },
                onSetNoteText = { clientsViewModel.onIntent(ClientsIntent.OnNoteTextChange(it)) },
                onSetAddNoteVisible = { clientsViewModel.onIntent(ClientsIntent.SetAddNoteVisible(it)) },
                onSetReceiptPickerVisible = { clientsViewModel.onIntent(ClientsIntent.SetReceiptPickerVisible(it)) },
                onClearAiSummary = { clientsViewModel.onIntent(ClientsIntent.ClearAiSummary) },
                onCallClient = { phone -> 
                    platformActions.callPhone(phone)
                },
                onEmailClient = { email ->
                    platformActions.sendEmail(email)
                },
                onPhotoCaptured = { uri, invId ->
                    clientsViewModel.onIntent(ClientsIntent.OnPhotoCaptured(uri, invId))
                },
                isPremium = sharedViewModel.businessSettings.collectAsStateWithLifecycle(initialValue = BusinessSettings()).value.isPremium,
                platformActions = platformActions
            )
        }
        composable<Screen.Suppliers> {
            val uiState by suppliersViewModel.uiState.collectAsStateWithLifecycle()
            val isAddSheetVisible by suppliersViewModel.isAddSheetVisible.collectAsStateWithLifecycle()
            val placeSuggestions by suppliersViewModel.placeSuggestions.collectAsStateWithLifecycle()
            val isReorderMode by suppliersViewModel.isReorderMode.collectAsStateWithLifecycle()
            val reorderList by suppliersViewModel.reorderList.collectAsStateWithLifecycle()
            val hiddenSuppliers by suppliersViewModel.hiddenSuppliers.collectAsStateWithLifecycle()
            
            SuppliersTab(
                uiState = uiState,
                isAddSheetVisible = isAddSheetVisible,
                placeSuggestions = placeSuggestions,
                isReorderMode = isReorderMode,
                reorderList = reorderList,
                onTogglePin = { suppliersViewModel.togglePin(it) },
                onHideSupplier = { suppliersViewModel.hideSupplier(it) },
                onAddClick = { suppliersViewModel.setAddSheetVisible(true) },
                onDismissAdd = { suppliersViewModel.setAddSheetVisible(false) },
                onAddSupplier = { name, cat, addr, phone, logo -> 
                    suppliersViewModel.addSupplier(name, cat, addr, phone, logo)
                },
                onSearchQueryChange = { suppliersViewModel.onSearchQueryChange(it) },
                onClearSuggestions = { suppliersViewModel.clearSuggestions() },
                onToggleReorder = { active, list -> suppliersViewModel.setReorderMode(active, list) },
                onMoveItem = { from, to -> suppliersViewModel.swapItems(from, to) },
                onSaveOrder = { suppliersViewModel.saveOrder() },
                hiddenSuppliers = hiddenSuppliers,
                onRestoreSupplier = { suppliersViewModel.restoreSupplier(it) },
                onLogPurchase = { id, name, amt, desc -> 
                    suppliersViewModel.logPurchase(id, amt)
                },
                onOpenStore = { url -> platformActions.openUrl(url) },
                onSnapPhoto = { suppliersViewModel.onPhotoCaptured(it) },
                photoUri = suppliersViewModel.capturedPhotoUri.collectAsStateWithLifecycle().value,
                platformActions = platformActions
            )
        }
        composable<Screen.Settings> {
            val settings by sharedViewModel.businessSettings.collectAsStateWithLifecycle(initialValue = BusinessSettings())
            val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
            val syncState by authViewModel.syncState.collectAsStateWithLifecycle()
            
            SettingsTab(
                settings = settings,
                currentUser = currentUser,
                onSaveSettings = { sharedViewModel.saveBusinessSettings(it) },
                onSignIn = { 
                    // This is a fallback if someone navigates directly to Settings route
                    platformActions.showToast("Sign in via the Main Tab bar.")
                },
                onSignOut = { authViewModel.signOut() },
                onSync = { authViewModel.triggerBackup() },
                syncState = syncState,
                platformActions = platformActions
            )
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\UiError.kt

```kt
package com.fordham.toolbelt.ui



// =========================
// UI ERROR MODEL
// =========================

data class UiError(
    val message: String
)

fun Throwable.toUiError(): UiError {
    return UiError(
        message = when {
            this.message?.contains("Network", ignoreCase = true) == true -> "Network error. Check your connection."
            this.message?.contains("Timeout", ignoreCase = true) == true -> "Connection timed out. Please try again."
            else -> "Something went wrong. Please try again."
        }
    )
}



```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\viewmodel\AgentViewModel.kt

```kt
package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.model.agent.DeleteInvoiceApprovalArgs
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.SearchClientsArgs
import com.fordham.toolbelt.domain.model.agent.SendInvoiceApprovalArgs
import com.fordham.toolbelt.domain.model.agent.SessionId
import com.fordham.toolbelt.domain.model.agent.ToolExecutionResult
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.model.agent.AgentOutcome as TypedAgentOutcome
import com.fordham.toolbelt.domain.repository.ToolRegistry
import com.fordham.toolbelt.domain.usecase.RunForemanAgentUseCase
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AgentUiState(
    val isActive: Boolean = false,
    val isProcessing: Boolean = false,
    val lastResponse: String? = null,
    val transcript: String = "",
    val currentMode: AgentMode = AgentMode.RESPONSE,
    val errorMessage: String? = null,
    val isListening: Boolean = false,
    val pendingApproval: ForemanToolCall? = null,
    val pendingTypedApproval: TypedAgentOutcome.RequiresApproval? = null
)

class AgentViewModel(
    private val runForemanAgentUseCase: RunForemanAgentUseCase,
    private val toolRegistry: ToolRegistry
) : ViewModel() {

    private var session = ForemanSession.empty(SessionId(randomUUID()))
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    fun executeAgentCommand(command: String, appContext: String, onIntent: (AiAgentIntent) -> Unit) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    transcript = command,
                    errorMessage = null,
                    pendingApproval = null,
                    pendingTypedApproval = null
                )
            }

            val result = runForemanAgentUseCase(
                command = NaturalLanguage(command),
                session = session,
                systemPrompt = NaturalLanguage(appContext)
            )

            when (result) {
                is TypedAgentOutcome.TextResponse -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            lastResponse = result.response.value,
                            isActive = true
                        )
                    }
                }
                is TypedAgentOutcome.ToolExecuted -> {
                    val summary = summarizeToolResult(result.result)
                    dispatchIntentForToolResult(result.result, onIntent)
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            lastResponse = summary,
                            isActive = true
                        )
                    }
                }
                is TypedAgentOutcome.RequiresApproval -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            pendingApproval = result.toLegacyPendingCall(),
                            pendingTypedApproval = result,
                            isActive = true
                        )
                    }
                }
                is TypedAgentOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = result.error.value
                        )
                    }
                }
                is TypedAgentOutcome.ToolExecutionRequested -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "Agent returned an unhandled tool request.",
                            isActive = true
                        )
                    }
                }
            }
        }
    }

    fun approveToolCall() {
        val pending = _uiState.value.pendingTypedApproval ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, pendingApproval = null, pendingTypedApproval = null) }
            val result = toolRegistry.execute(pending.toolName, pending.arguments)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    lastResponse = summarizeToolResult(result),
                    errorMessage = (result as? ToolExecutionResult.Failure)?.error?.value
                )
            }
        }
    }

    fun setAgentActive(active: Boolean) {
        _uiState.update { it.copy(isActive = active) }
    }

    fun setListening(listening: Boolean) {
        _uiState.update { it.copy(isListening = listening) }
    }

    fun clearAgentResponse() {
        _uiState.update {
            it.copy(
                lastResponse = null,
                errorMessage = null,
                isActive = false,
                pendingApproval = null,
                pendingTypedApproval = null
            )
        }
    }

    private fun summarizeToolResult(result: ToolExecutionResult): String {
        return when (result) {
            is ToolExecutionResult.ClientSearchCompleted -> {
                if (result.clients.isEmpty()) {
                    "No matching clients found."
                } else {
                    "Found ${result.clients.size} matching client${if (result.clients.size == 1) "" else "s"}: ${
                        result.clients.take(3).joinToString { it.displayName.value }
                    }"
                }
            }
            is ToolExecutionResult.DraftInvoiceCreated -> "Draft invoice started for the selected client."
            is ToolExecutionResult.UnbilledReceiptsFound -> {
                "Found ${result.receipts.size} unbilled receipt${if (result.receipts.size == 1) "" else "s"}."
            }
            is ToolExecutionResult.InvoiceApprovalQueued -> "Invoice is ready for approval."
            is ToolExecutionResult.InvoiceDeletionQueued -> "Invoice deleted after approval."
            is ToolExecutionResult.Failure -> result.error.value
        }
    }

    private fun dispatchIntentForToolResult(
        result: ToolExecutionResult,
        onIntent: (AiAgentIntent) -> Unit
    ) {
        when (result) {
            is ToolExecutionResult.DraftInvoiceCreated -> onIntent(AiAgentIntent.DraftInvoice(null))
            is ToolExecutionResult.ClientSearchCompleted -> {
                result.clients.firstOrNull()?.let { onIntent(AiAgentIntent.SummarizeClient(it.displayName.value)) }
            }
            is ToolExecutionResult.UnbilledReceiptsFound -> onIntent(AiAgentIntent.ScanReceipt)
            else -> Unit
        }
    }

    private fun TypedAgentOutcome.RequiresApproval.toLegacyPendingCall(): ForemanToolCall {
        return ForemanToolCall(
            id = toolCallId.value,
            type = when (toolName) {
                ToolName.DeleteInvoiceForApproval -> ToolType.DELETE_INVOICE
                ToolName.SendInvoiceForApproval -> ToolType.UNKNOWN
                else -> ToolType.UNKNOWN
            },
            parameters = when (val args = arguments) {
                is DeleteInvoiceApprovalArgs -> ToolParameters.DeleteInvoice(args.invoiceId)
                is SendInvoiceApprovalArgs -> ToolParameters.DeleteInvoice(args.invoiceId)
                is SearchClientsArgs -> ToolParameters.SearchClients(args.query.value)
                else -> ToolParameters.None
            },
            reasoning = "This action requires your approval before Invoice Hammer executes it."
        )
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\viewmodel\AuthViewModel.kt

```kt
package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.SyncOutcome
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.FordhamUser
import com.fordham.toolbelt.domain.repository.SyncRepository
import com.fordham.toolbelt.domain.repository.IdToken
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    val currentUser: StateFlow<FordhamUser?> = authRepository.currentUser
    
    private val _syncState = kotlinx.coroutines.flow.MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun signIn(idToken: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(IdToken(idToken))
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun triggerBackup() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val result = syncRepository.syncInvoices()
            when (result) {
                is SyncOutcome.Success -> {
                    _syncState.value = SyncState.Success
                    // Reset to idle after a delay so the UI can show success briefly
                    kotlinx.coroutines.delay(3000)
                    _syncState.value = SyncState.Idle
                }
                is SyncOutcome.Failure -> {
                    _syncState.value = SyncState.Error(result.error.value)
                }
            }
        }
    }

    fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\viewmodel\ClientsViewModel.kt

```kt
package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Responsibility: Manage UI state and handle user intents for the Clients feature.
 * Follows UDF pattern per AI_ARCHITECTURE_RULES.md.
 */
data class ClientsUiState(
    val clientToDelete: Client? = null,
    val showAddNote: Boolean = false,
    val noteText: String = "",
    val aiSummary: String? = null,
    val isSummarizing: Boolean = false,
    val availableReceipts: List<ReceiptItem> = emptyList(),
    val showReceiptPicker: Boolean = false
)

sealed interface ClientsIntent {
    data class SetClientToDelete(val client: Client?) : ClientsIntent
    data class DeleteClient(val client: Client) : ClientsIntent
    data class SetAddNoteVisible(val visible: Boolean) : ClientsIntent
    data class OnNoteTextChange(val text: String) : ClientsIntent
    data class AddNote(val clientName: String) : ClientsIntent
    data class DeleteNote(val note: JobNote) : ClientsIntent
    data class SummarizeNotes(val notes: List<JobNote>) : ClientsIntent
    object ClearAiSummary : ClientsIntent
    data class SetReceiptPickerVisible(val visible: Boolean) : ClientsIntent
    data class LinkReceipt(val receipt: ReceiptItem, val clientName: String) : ClientsIntent
    data class OnPhotoCaptured(val uriString: String, val invoiceId: String) : ClientsIntent
}

class ClientsViewModel(
    private val clientRepository: ClientRepository,
    private val receiptRepository: ReceiptRepository,
    private val addJobNoteUseCase: AddJobNoteUseCase,
    private val deleteJobNoteUseCase: DeleteJobNoteUseCase,
    private val deleteClientUseCase: DeleteClientUseCase,
    private val linkReceiptToClientUseCase: LinkReceiptToClientUseCase,
    private val saveJobPhotoUseCase: SaveJobPhotoUseCase,
    private val generateSummaryUseCase: GenerateSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientsUiState())
    val uiState: StateFlow<ClientsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            receiptRepository.getUnassignedReceipts().collect { result ->
                if (result is ReceiptListOutcome.Success) {
                    _uiState.update { it.copy(availableReceipts = result.receipts) }
                }
            }
        }
    }

    val allClients: Flow<List<Client>> = clientRepository.getAllClients()
        .map { result -> if (result is ClientListOutcome.Success) result.clients else emptyList() }

    fun onIntent(intent: ClientsIntent) {
        when (intent) {
            is ClientsIntent.SetClientToDelete -> _uiState.update { it.copy(clientToDelete = intent.client) }
            is ClientsIntent.DeleteClient -> executeDeleteClient(intent.client)
            is ClientsIntent.SetAddNoteVisible -> _uiState.update { it.copy(showAddNote = intent.visible) }
            is ClientsIntent.OnNoteTextChange -> _uiState.update { it.copy(noteText = intent.text) }
            is ClientsIntent.AddNote -> executeAddNote(intent.clientName)
            is ClientsIntent.DeleteNote -> executeDeleteNote(intent.note)
            is ClientsIntent.SummarizeNotes -> executeSummarizeNotes(intent.notes)
            is ClientsIntent.ClearAiSummary -> _uiState.update { it.copy(aiSummary = null) }
            is ClientsIntent.SetReceiptPickerVisible -> _uiState.update { it.copy(showReceiptPicker = intent.visible) }
            is ClientsIntent.LinkReceipt -> executeLinkReceipt(intent.receipt, intent.clientName)
            is ClientsIntent.OnPhotoCaptured -> executeSavePhoto(intent.uriString, intent.invoiceId)
        }
    }

    private fun executeDeleteClient(client: Client) {
        viewModelScope.launch {
            deleteClientUseCase(client)
        }
    }

    private fun executeAddNote(clientName: String) {
        val text = _uiState.value.noteText
        if (text.isBlank()) return
        viewModelScope.launch {
            val result = addJobNoteUseCase(clientName, text)
            if (result is JobNoteOutcome.Success) {
                _uiState.update { it.copy(showAddNote = false, noteText = "") }
            }
        }
    }

    private fun executeDeleteNote(note: JobNote) {
        viewModelScope.launch {
            deleteJobNoteUseCase(note)
        }
    }

    private fun executeSummarizeNotes(notes: List<JobNote>) {
        if (notes.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSummarizing = true, aiSummary = null) }
            val data = notes.joinToString("\n") { it.text }
            val result = generateSummaryUseCase(data)
            if (result is GeminiOutcome.Success) {
                _uiState.update { it.copy(aiSummary = result.text, isSummarizing = false) }
            } else {
                _uiState.update { it.copy(isSummarizing = false) }
            }
        }
    }

    private fun executeLinkReceipt(receipt: ReceiptItem, clientName: String) {
        viewModelScope.launch {
            linkReceiptToClientUseCase(receipt, clientName)
            _uiState.update { it.copy(showReceiptPicker = false) }
        }
    }

    private fun executeSavePhoto(uri: String, invId: String) {
        viewModelScope.launch {
            saveJobPhotoUseCase(uri, invId)
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\viewmodel\HistoryViewModel.kt

```kt
package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class HistoryUiState(
    val invoiceToDelete: Invoice? = null,
    val searchQuery: String = "",
    val showPaidOnly: Boolean = false
)

class HistoryViewModel(
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    val allInvoices: Flow<List<Invoice>> = invoiceRepository.allInvoices

    val filteredInvoices = combine(allInvoices, _uiState) { invoices, state ->
        invoices.filter { 
            (it.clientName.contains(state.searchQuery, ignoreCase = true) || it.itemsSummary.contains(state.searchQuery, ignoreCase = true)) &&
            (!state.showPaidOnly || it.isPaid)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setInvoiceToDelete(invoice: Invoice?) { _uiState.update { it.copy(invoiceToDelete = invoice) } }
    
    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch { invoiceRepository.deleteInvoice(invoice) }
    }

    fun updateInvoice(invoice: Invoice) {
        viewModelScope.launch { invoiceRepository.updateInvoice(invoice) }
    }

    fun onSearchQueryChange(query: String) { _uiState.update { it.copy(searchQuery = query) } }
    fun onShowPaidOnlyChange(paidOnly: Boolean) { _uiState.update { it.copy(showPaidOnly = paidOnly) } }
    
    fun convertEstimateToInvoice(estimate: Invoice) {
        viewModelScope.launch {
            invoiceRepository.updateInvoice(estimate.copy(isEstimate = false))
        }
    }

    fun markInvoicePaid(invoice: Invoice) {
        updateInvoice(invoice.copy(isPaid = true))
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\viewmodel\NewInvoiceDraftEditor.kt

```kt
package com.fordham.toolbelt.ui.viewmodel

import com.fordham.toolbelt.domain.model.DraftInvoice
import com.fordham.toolbelt.domain.model.LineItem
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.repository.DraftRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

internal class NewInvoiceDraftEditor(
    private val draftRepository: DraftRepository
) {
    val draft: Flow<DraftInvoice> = draftRepository.getDraft()

    suspend fun currentDraft(): DraftInvoice = draft.first()

    suspend fun updateDraft(update: (DraftInvoice) -> DraftInvoice) {
        draftRepository.saveDraft(update(currentDraft()))
    }

    suspend fun toggleTimer() {
        val current = currentDraft()
        if (current.timerRunning) {
            draftRepository.saveDraft(current.copy(timerRunning = false))
            return
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val start = if (current.elapsedSeconds > 0) {
            now - (current.elapsedSeconds * 1000)
        } else {
            now
        }
        draftRepository.saveDraft(current.copy(timerRunning = true, startTime = start))
        runTimerLoop()
    }

    suspend fun addManualItem(): Boolean {
        val draft = currentDraft()
        val amount = draft.itemAmt.toDoubleOrNull() ?: 0.0
        if (amount <= 0.0 || draft.itemDesc.isBlank()) return false

        val lineItems = draft.lineItems + LineItem(draft.itemDesc, amount, draft.selectedCategory)
        draftRepository.saveDraft(draft.copy(lineItems = lineItems, itemDesc = "", itemAmt = ""))
        return true
    }

    suspend fun acceptAiItems(items: List<LineItem>) {
        val draft = currentDraft()
        draftRepository.saveDraft(draft.copy(lineItems = draft.lineItems + items, itemDesc = ""))
    }

    suspend fun linkReceipt(receipt: ReceiptItem, markupPercent: Double) {
        val draft = currentDraft()
        val amount = receipt.totalPrice * (1.0 + (markupPercent / 100.0))
        val description = if (markupPercent > 0) {
            "${receipt.description} (incl. ${markupPercent.toInt()}% markup)"
        } else {
            receipt.description
        }

        draftRepository.saveDraft(
            draft.copy(
                lineItems = draft.lineItems + LineItem(description, amount, "Parts"),
                linkedReceiptIds = draft.linkedReceiptIds + receipt.id.value
            )
        )
    }

    suspend fun clearDraft() {
        draftRepository.clearDraft()
    }

    private suspend fun runTimerLoop() {
        while (true) {
            val draft = currentDraft()
            if (!draft.timerRunning) break

            val elapsedSeconds = (Clock.System.now().toEpochMilliseconds() - draft.startTime) / 1000
            draftRepository.saveDraft(draft.copy(elapsedSeconds = elapsedSeconds))
            delay(1000)
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\viewmodel\NewInvoiceViewModel.kt

```kt
package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Responsibility: Manage state for creating a new invoice, utilizing a persistent draft.
 * Follows UDF pattern per AI_ARCHITECTURE_RULES.md.
 */
data class NewInvoiceUiState(
    val clientName: String = "",
    val clientAddress: String = "",
    val taxText: String = "7.0",
    val depositCollected: String = "",
    val hourlyRate: String = "50.0",
    val logoUri: String? = null,
    val lineItems: List<LineItem> = emptyList(),
    val selectedCategory: String = "Drywall",
    val itemDesc: String = "",
    val itemAmt: String = "",
    val isProcessingAi: Boolean = false,
    val pendingAi: List<LineItem> = emptyList(),
    val showAiConf: Boolean = false,
    val showClientDropdown: Boolean = false,
    val showCategoryDropdown: Boolean = false,
    val isListening: Boolean = false,
    val timerRunning: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val startTime: Long = 0L,
    val saveToClientDirectory: Boolean = false,
    val canAddManual: Boolean = false,
    val canSave: Boolean = false,
    val errorMessage: String? = null,
    val capturedPhotos: List<String> = emptyList(),
    val availableReceipts: List<ReceiptItem> = emptyList(),
    val showReceiptPicker: Boolean = false
) {
    val formattedTime: String get() {
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}

sealed interface NewInvoiceIntent {
    data class OnClientNameChange(val name: String) : NewInvoiceIntent
    data class OnClientAddressChange(val address: String) : NewInvoiceIntent
    data class OnTaxTextChange(val tax: String) : NewInvoiceIntent
    data class OnDepositCollectedChange(val amt: String) : NewInvoiceIntent
    data class OnHourlyRateChange(val rate: String) : NewInvoiceIntent
    data class OnLogoUriChange(val uri: String?) : NewInvoiceIntent
    data class OnCategoryChange(val cat: String) : NewInvoiceIntent
    data class OnSaveToClientDirectoryChange(val save: Boolean) : NewInvoiceIntent
    data class OnItemDescChange(val desc: String) : NewInvoiceIntent
    data class OnItemAmtChange(val amt: String) : NewInvoiceIntent
    data class OnPhotoCaptured(val uri: String) : NewInvoiceIntent
    data class RemovePhoto(val uri: String) : NewInvoiceIntent
    data class SetClientDropdownVisible(val visible: Boolean) : NewInvoiceIntent
    data class SetCategoryDropdownVisible(val visible: Boolean) : NewInvoiceIntent
    data class OnListeningStateChange(val visible: Boolean) : NewInvoiceIntent
    data class OnShowAiConfChange(val visible: Boolean) : NewInvoiceIntent
    data class SetReceiptPickerVisible(val visible: Boolean) : NewInvoiceIntent
    object ClearError : NewInvoiceIntent
    object ToggleTimer : NewInvoiceIntent
    object AddManualLineItem : NewInvoiceIntent
    object BillLabor : NewInvoiceIntent
    data class RemoveLineItem(val item: LineItem) : NewInvoiceIntent
    object AcceptAiItems : NewInvoiceIntent
    data class ProcessInvoiceAi(val categories: List<String>) : NewInvoiceIntent
    data class LinkReceipt(val receipt: ReceiptItem, val markupPercent: Double) : NewInvoiceIntent
    data class SaveInvoice(val isEstimate: Boolean, val settings: BusinessSettings, val onGenerated: (String) -> Unit) : NewInvoiceIntent
}

class NewInvoiceViewModel(
    private val receiptRepository: ReceiptRepository,
    private val processInvoiceAiUseCase: ProcessInvoiceAiUseCase,
    private val billLaborUseCase: BillLaborUseCase,
    private val generateAndSaveInvoiceUseCase: GenerateAndSaveInvoiceUseCase,
    private val draftRepository: DraftRepository
) : ViewModel() {

    private val _transientState = MutableStateFlow(TransientUiState())
    private val draftEditor = NewInvoiceDraftEditor(draftRepository)
    
    data class TransientUiState(
        val isProcessingAi: Boolean = false,
        val isListening: Boolean = false,
        val errorMessage: String? = null,
        val showAiConf: Boolean = false,
        val pendingAi: List<LineItem> = emptyList(),
        val showClientDropdown: Boolean = false,
        val showCategoryDropdown: Boolean = false,
        val showReceiptPicker: Boolean = false,
        val availableReceipts: List<ReceiptItem> = emptyList(),
        val clientName: String? = null,
        val clientAddress: String? = null,
        val taxText: String? = null,
        val depositCollected: String? = null,
        val hourlyRate: String? = null,
        val itemDesc: String? = null,
        val itemAmt: String? = null
    )

    val uiState: StateFlow<NewInvoiceUiState> = combine(
        draftEditor.draft,
        _transientState
    ) { draft, transient ->
        NewInvoiceUiState(
            clientName = transient.clientName ?: draft.clientName,
            clientAddress = transient.clientAddress ?: draft.clientAddress,
            taxText = transient.taxText ?: draft.taxRate.toString(),
            depositCollected = transient.depositCollected ?: draft.deposit.toString(),
            hourlyRate = transient.hourlyRate ?: draft.hourlyRate.toString(),
            logoUri = draft.logoUri,
            lineItems = draft.lineItems,
            selectedCategory = draft.selectedCategory,
            itemDesc = transient.itemDesc ?: draft.itemDesc,
            itemAmt = transient.itemAmt ?: draft.itemAmt,
            isProcessingAi = transient.isProcessingAi,
            pendingAi = transient.pendingAi,
            showAiConf = transient.showAiConf,
            showClientDropdown = transient.showClientDropdown,
            showCategoryDropdown = transient.showCategoryDropdown,
            isListening = transient.isListening,
            timerRunning = draft.timerRunning,
            elapsedSeconds = draft.elapsedSeconds,
            startTime = draft.startTime,
            saveToClientDirectory = draft.saveToClientDirectory,
            capturedPhotos = draft.capturedPhotos,
            availableReceipts = transient.availableReceipts,
            showReceiptPicker = transient.showReceiptPicker,
            canAddManual = (transient.itemDesc ?: draft.itemDesc).isNotBlank() && ((transient.itemAmt ?: draft.itemAmt).toDoubleOrNull() ?: 0.0) > 0.0,
            canSave = (transient.clientName ?: draft.clientName).isNotBlank() && draft.lineItems.isNotEmpty(),
            errorMessage = transient.errorMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NewInvoiceUiState())

    init {
        viewModelScope.launch {
            receiptRepository.getUnassignedReceipts().collect { result ->
                if (result is ReceiptListOutcome.Success) {
                    _transientState.update { it.copy(availableReceipts = result.receipts) }
                }
            }
        }

        // Bootstrap transient state with initial saved draft values to prevent cursor jumps
        viewModelScope.launch {
            val initialDraft = draftEditor.currentDraft()
            _transientState.update { 
                it.copy(
                    clientName = initialDraft.clientName,
                    clientAddress = initialDraft.clientAddress,
                    taxText = initialDraft.taxRate.toString(),
                    depositCollected = initialDraft.deposit.toString(),
                    hourlyRate = initialDraft.hourlyRate.toString(),
                    itemDesc = initialDraft.itemDesc,
                    itemAmt = initialDraft.itemAmt
                )
            }
        }
    }

    val categories = listOf("Drywall", "Flooring", "Roofing", "Plumbing", "Electrical", "Painting", "Carpentry", "General Repair")

    fun onIntent(intent: NewInvoiceIntent) {
        when (intent) {
            is NewInvoiceIntent.OnClientNameChange -> {
                _transientState.update { it.copy(clientName = intent.name) }
                updateDraft { it.copy(clientName = intent.name) }
            }
            is NewInvoiceIntent.OnClientAddressChange -> {
                _transientState.update { it.copy(clientAddress = intent.address) }
                updateDraft { it.copy(clientAddress = intent.address) }
            }
            is NewInvoiceIntent.OnTaxTextChange -> {
                _transientState.update { it.copy(taxText = intent.tax) }
                updateDraft { it.copy(taxRate = intent.tax.toDoubleOrNull() ?: 0.0) }
            }
            is NewInvoiceIntent.OnDepositCollectedChange -> {
                _transientState.update { it.copy(depositCollected = intent.amt) }
                updateDraft { it.copy(deposit = intent.amt.toDoubleOrNull() ?: 0.0) }
            }
            is NewInvoiceIntent.OnHourlyRateChange -> {
                _transientState.update { it.copy(hourlyRate = intent.rate) }
                updateDraft { it.copy(hourlyRate = intent.rate.toDoubleOrNull() ?: 0.0) }
            }
            is NewInvoiceIntent.OnLogoUriChange -> updateDraft { it.copy(logoUri = intent.uri) }
            is NewInvoiceIntent.OnCategoryChange -> updateDraft { it.copy(selectedCategory = intent.cat) }
            is NewInvoiceIntent.OnSaveToClientDirectoryChange -> updateDraft { it.copy(saveToClientDirectory = intent.save) }
            is NewInvoiceIntent.OnItemDescChange -> {
                _transientState.update { it.copy(itemDesc = intent.desc) }
                updateDraft { it.copy(itemDesc = intent.desc) }
            }
            is NewInvoiceIntent.OnItemAmtChange -> {
                _transientState.update { it.copy(itemAmt = intent.amt) }
                updateDraft { it.copy(itemAmt = intent.amt) }
            }
            is NewInvoiceIntent.OnPhotoCaptured -> executeAddPhoto(intent.uri)
            is NewInvoiceIntent.RemovePhoto -> executeRemovePhoto(intent.uri)
            is NewInvoiceIntent.SetClientDropdownVisible -> _transientState.update { it.copy(showClientDropdown = intent.visible) }
            is NewInvoiceIntent.SetCategoryDropdownVisible -> _transientState.update { it.copy(showCategoryDropdown = intent.visible) }
            is NewInvoiceIntent.OnListeningStateChange -> _transientState.update { it.copy(isListening = intent.visible) }
            is NewInvoiceIntent.OnShowAiConfChange -> _transientState.update { it.copy(showAiConf = intent.visible) }
            is NewInvoiceIntent.SetReceiptPickerVisible -> _transientState.update { it.copy(showReceiptPicker = intent.visible) }
            is NewInvoiceIntent.ClearError -> _transientState.update { it.copy(errorMessage = null) }
            is NewInvoiceIntent.ToggleTimer -> executeToggleTimer()
            is NewInvoiceIntent.AddManualLineItem -> executeAddManualItem()
            is NewInvoiceIntent.BillLabor -> executeBillLabor()
            is NewInvoiceIntent.RemoveLineItem -> executeRemoveLineItem(intent.item)
            is NewInvoiceIntent.AcceptAiItems -> executeAcceptAiItems()
            is NewInvoiceIntent.ProcessInvoiceAi -> executeProcessAi(intent.categories)
            is NewInvoiceIntent.LinkReceipt -> executeLinkReceipt(intent.receipt, intent.markupPercent)
            is NewInvoiceIntent.SaveInvoice -> executeSaveInvoice(intent.isEstimate, intent.settings, intent.onGenerated)
        }
    }

    private fun updateDraft(update: (DraftInvoice) -> DraftInvoice) {
        viewModelScope.launch {
            draftEditor.updateDraft(update)
        }
    }

    private fun executeAddPhoto(uri: String) = updateDraft { 
        it.copy(capturedPhotos = it.capturedPhotos + uri)
    }

    private fun executeRemovePhoto(uri: String) = updateDraft { 
        it.copy(capturedPhotos = it.capturedPhotos - uri)
    }

    private fun executeToggleTimer() {
        viewModelScope.launch {
            draftEditor.toggleTimer()
        }
    }

    private fun executeAddManualItem() {
        viewModelScope.launch {
            if (draftEditor.addManualItem()) {
                _transientState.update { it.copy(itemDesc = "", itemAmt = "") }
            }
        }
    }

    private fun executeBillLabor() = viewModelScope.launch { billLaborUseCase() }

    private fun executeRemoveLineItem(item: LineItem) = updateDraft { 
        it.copy(lineItems = it.lineItems - item)
    }

    private fun executeAcceptAiItems() {
        viewModelScope.launch {
            draftEditor.acceptAiItems(_transientState.value.pendingAi)
            _transientState.update { it.copy(showAiConf = false, pendingAi = emptyList(), itemDesc = "") }
        }
    }

    private fun executeProcessAi(categories: List<String>) {
        viewModelScope.launch {
            val draft = draftEditor.currentDraft()
            _transientState.update { it.copy(isProcessingAi = true, errorMessage = null) }
            val result = processInvoiceAiUseCase(draft.itemDesc, categories)
            if (result is InvoiceTextOutcome.Success) {
                val ai = result.result
                draftEditor.updateDraft { current -> current.copy(
                    clientName = if (ai.clientName.isNotBlank()) ai.clientName else draft.clientName,
                    clientAddress = if (ai.clientAddress.isNotBlank()) ai.clientAddress else draft.clientAddress
                ) }
                _transientState.update { state ->
                    state.copy(
                        pendingAi = ai.items,
                        showAiConf = ai.items.isNotEmpty(),
                        clientName = if (ai.clientName.isNotBlank()) ai.clientName else state.clientName,
                        clientAddress = if (ai.clientAddress.isNotBlank()) ai.clientAddress else state.clientAddress
                    )
                }
            } else if (result is InvoiceTextOutcome.Failure) {
                _transientState.update { it.copy(errorMessage = "AI Error: ${result.error.value}") }
            }
            _transientState.update { it.copy(isProcessingAi = false) }
        }
    }

    private fun executeLinkReceipt(receipt: ReceiptItem, markup: Double) {
        viewModelScope.launch {
            draftEditor.linkReceipt(receipt, markup)
            _transientState.update { it.copy(showReceiptPicker = false) }
        }
    }

    private fun executeSaveInvoice(isEst: Boolean, set: BusinessSettings, onGen: (String) -> Unit) {
        viewModelScope.launch {
            println("SAVE_INVOICE: Starting executeSaveInvoice...")
            val draft = draftEditor.currentDraft()
            println("SAVE_INVOICE: Loaded draft with client: ${draft.clientName}, items: ${draft.lineItems}")
            val res = generateAndSaveInvoiceUseCase(
                clientName = draft.clientName, clientAddress = draft.clientAddress, saveToClientDirectory = draft.saveToClientDirectory,
                taxRate = draft.taxRate, deposit = draft.deposit, lineItems = draft.lineItems,
                logoUriString = draft.logoUri, businessSettings = set, isEstimate = isEst, elapsedSeconds = draft.elapsedSeconds,
                capturedPhotos = draft.capturedPhotos, linkedReceiptIds = draft.linkedReceiptIds,
                availableReceipts = _transientState.value.availableReceipts, onGenerated = onGen
            )
            when (res) {
                is GenerateInvoiceOutcome.Success -> {
                    println("SAVE_INVOICE: Saved successfully! Clearing draft...")
                    draftEditor.clearDraft()
                    _transientState.update {
                        it.copy(
                            clientName = "",
                            clientAddress = "",
                            taxText = "7.0",
                            depositCollected = "",
                            hourlyRate = "50.0",
                            itemDesc = "",
                            itemAmt = ""
                        )
                    }
                }
                is GenerateInvoiceOutcome.Error -> {
                    println("SAVE_INVOICE_ERROR: ${res.message}")
                    res.exception?.printStackTrace()
                    _transientState.update { it.copy(errorMessage = res.message) }
                }
            }
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\viewmodel\PaymentViewModel.kt

```kt
package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.PaymentLedgerOutcome
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestOutcome
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.usecase.CreatePaymentRequestUseCase
import com.fordham.toolbelt.domain.usecase.GetPaymentLedgerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PaymentUiState(
    val requests: List<InvoicePaymentRequest> = emptyList(),
    val latestRequest: InvoicePaymentRequest? = null,
    val errorMessage: String? = null
) {
    val pendingCount: Int get() = requests.count { it.statusLabel != "PAID" }
    val totalRequested: Double get() = requests.sumOf { it.requestedAmount.value }
}

class PaymentViewModel(
    getPaymentLedgerUseCase: GetPaymentLedgerUseCase,
    private val createPaymentRequestUseCase: CreatePaymentRequestUseCase
) : ViewModel() {

    private val transientState = MutableStateFlow(PaymentUiState())

    val uiState: StateFlow<PaymentUiState> = combine(
        getPaymentLedgerUseCase.ledger.map { outcome ->
            when (outcome) {
                is PaymentLedgerOutcome.Success -> PaymentUiState(requests = outcome.requests)
                is PaymentLedgerOutcome.Failure -> PaymentUiState(errorMessage = outcome.error.value)
            }
        },
        transientState
    ) { ledgerState, transient ->
        ledgerState.copy(
            latestRequest = transient.latestRequest,
            errorMessage = transient.errorMessage ?: ledgerState.errorMessage
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaymentUiState())

    fun requestDeposit(invoice: Invoice, provider: PaymentProviderType) {
        createRequest(invoice, PaymentRequestType.Deposit, provider)
    }

    fun requestFullPayment(invoice: Invoice, provider: PaymentProviderType) {
        createRequest(invoice, PaymentRequestType.FullBalance, provider)
    }

    fun clearLatestRequest() {
        updateTransient(latestRequest = null)
    }

    fun clearError() {
        updateTransient(errorMessage = null)
    }

    fun createRequest(invoice: Invoice, type: PaymentRequestType, provider: PaymentProviderType) {
        viewModelScope.launch {
            when (val outcome = createPaymentRequestUseCase(invoice, type, provider)) {
                is PaymentRequestOutcome.Success -> updateTransient(latestRequest = outcome.request, errorMessage = null)
                is PaymentRequestOutcome.Failure -> updateTransient(errorMessage = outcome.error.value)
            }
        }
    }

    private fun updateTransient(latestRequest: InvoicePaymentRequest? = uiState.value.latestRequest, errorMessage: String? = uiState.value.errorMessage) {
        transientState.update { it.copy(latestRequest = latestRequest, errorMessage = errorMessage) }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\viewmodel\ReceiptsViewModel.kt

```kt
package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.ProcessReceiptOutcome
import com.fordham.toolbelt.domain.model.StorageBytesOutcome
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.StorageRepository
import com.fordham.toolbelt.domain.usecase.ProcessReceiptUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReceiptsUiState(
    val capturedImageBytes: ByteArray? = null,
    val isProcessing: Boolean = false,
    val showClearConfirmDialog: Boolean = false,
    val showClientDropdown: Boolean = false,
    val showMarkupDialog: Boolean = false,
    val markupPercentage: String = "0",
    val errorMessage: String? = null
)

class ReceiptsViewModel(
    private val receiptRepository: ReceiptRepository,
    private val storageRepository: StorageRepository,
    private val processReceiptUseCase: ProcessReceiptUseCase,
    private val settingsRepository: com.fordham.toolbelt.domain.repository.SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptsUiState())
    val uiState: StateFlow<ReceiptsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.businessSettingsFlow.collect { settings ->
                _uiState.update { it.copy(markupPercentage = settings.markupPercentage.toInt().toString()) }
            }
        }
    }

    private val _filterClientName = MutableStateFlow<String?>(null)
    val filterClientName: StateFlow<String?> = _filterClientName.asStateFlow()

    val filteredReceipts: Flow<List<ReceiptItem>> = combine(
        receiptRepository.allItems,
        _filterClientName
    ) { itemsResult, filter ->
        val items = if (itemsResult is ReceiptListOutcome.Success) {
            itemsResult.receipts
        } else emptyList()

        if (filter == null) items.filter { it.clientName == "General" || it.clientName.isEmpty() }
        else items.filter { it.clientName == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val receiptsTotal: Flow<Double> = filteredReceipts.map { items ->
        items.sumOf { it.totalPrice }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalWithMarkup: Flow<Double> = combine(
        receiptsTotal,
        _uiState.map { it.markupPercentage }
    ) { total, markupStr ->
        val markup = markupStr.toDoubleOrNull() ?: 0.0
        total * (1.0 + (markup / 100.0))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setFilterClient(name: String?) {
        _filterClientName.value = name
    }

    fun setClearConfirmVisible(visible: Boolean) { _uiState.update { it.copy(showClearConfirmDialog = visible) } }
    
    fun clearReceiptItems() {
        viewModelScope.launch { receiptRepository.deleteAllItems() }
    }

    fun onReceiptImageCaptured(bytes: ByteArray?) {
        _uiState.update { it.copy(capturedImageBytes = bytes) }
    }

    fun clearCapturedReceiptImage() {
        _uiState.update { it.copy(capturedImageBytes = null, errorMessage = null) }
    }

    fun onReceiptUriSelected(uri: String) {
        viewModelScope.launch {
            val result = storageRepository.getBytesFromUri(uri)
            if (result is StorageBytesOutcome.Success) {
                onReceiptImageCaptured(result.bytes)
            }
        }
    }

    fun setClientDropdownVisible(visible: Boolean) { _uiState.update { it.copy(showClientDropdown = visible) } }
    fun setMarkupDialogVisible(visible: Boolean) { _uiState.update { it.copy(showMarkupDialog = visible) } }
    fun onMarkupPercentageChange(pct: String) { 
        _uiState.update { it.copy(markupPercentage = pct) } 
        viewModelScope.launch {
            val markup = pct.toDoubleOrNull() ?: 0.0
            val current = settingsRepository.businessSettingsFlow.first()
            settingsRepository.saveBusinessSettings(current.copy(markupPercentage = markup))
        }
    }
    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }

    fun deleteReceiptItem(item: ReceiptItem) {
        viewModelScope.launch { receiptRepository.deleteItem(item) }
    }
    
    fun updateReceiptItem(item: ReceiptItem) {
        viewModelScope.launch { receiptRepository.updateItem(item) }
    }

    fun toggleReceiptBilled(item: ReceiptItem) {
        updateReceiptItem(item.copy(isBilled = !item.isBilled))
    }

    fun processCapturedReceipt(selectedClient: Client?, onComplete: () -> Unit = {}) {
        val imageBytes = _uiState.value.capturedImageBytes ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            
            val result = processReceiptUseCase(imageBytes, selectedClient?.name)
            when (result) {
                is ProcessReceiptOutcome.Success -> {
                    _uiState.update { it.copy(capturedImageBytes = null) }
                }
                is ProcessReceiptOutcome.Failure -> {
                    _uiState.update { it.copy(errorMessage = "Failed to process receipt: ${result.error.value}") }
                }
                is ProcessReceiptOutcome.PremiumRequired -> {
                    _uiState.update { it.copy(errorMessage = "Premium subscription required to process receipt") }
                }
            }
            _uiState.update { it.copy(isProcessing = false) }
            onComplete()
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\viewmodel\SharedViewModel.kt

```kt
package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.GetClientFinancialSummaryUseCase
import com.fordham.toolbelt.ui.Screen
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SharedViewModel(
    private val settingsRepository: SettingsRepository,
    private val clientRepository: ClientRepository,
    private val jobNoteRepository: JobNoteRepository,
    private val photoRepository: PhotoRepository,
    private val invoiceRepository: InvoiceRepository,
    private val receiptRepository: ReceiptRepository,
    private val getClientFinancialSummaryUseCase: GetClientFinancialSummaryUseCase
) : ViewModel() {

    private val _selectedClient = MutableStateFlow<Client?>(null)
    val selectedClient: StateFlow<Client?> = _selectedClient.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Screen>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    val businessSettings: Flow<BusinessSettings> = settingsRepository.businessSettingsFlow
    val allClients: Flow<List<Client>> = clientRepository.getAllClients().map { result ->
        if (result is ClientListOutcome.Success) {
            result.clients
        } else emptyList()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedClientSummary: StateFlow<com.fordham.toolbelt.domain.usecase.FinancialSummary?> = _selectedClient.flatMapLatest { client ->
        if (client == null) flowOf(null)
        else getClientFinancialSummaryUseCase(client.name)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedClientInvoices: StateFlow<List<Invoice>> = _selectedClient.flatMapLatest { client ->
        if (client == null) flowOf(emptyList<Invoice>())
        else invoiceRepository.allInvoices.map { list -> 
            list.filter { it.clientName == client.name } 
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedClientPhotos: StateFlow<List<JobPhoto>> = _selectedClient.flatMapLatest { client ->
        if (client == null) flowOf(emptyList())
        else invoiceRepository.allInvoices.flatMapLatest { invoices ->
            val clientInvoiceIds = invoices.filter { it.clientName == client.name }.map { it.id }
            if (clientInvoiceIds.isEmpty()) flowOf(emptyList())
            else {
                val flows = clientInvoiceIds.map { photoRepository.observePhotosForInvoice(it) }
                combine(flows) { array: Array<*> -> 
                    array.flatMap { it as List<JobPhoto> }.sortedByDescending { p -> p.timestamp } 
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())




    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedClientNotes: StateFlow<List<JobNote>> = _selectedClient.flatMapLatest { client ->
        if (client == null) flowOf(emptyList<JobNote>())
        else jobNoteRepository.getNotesByClient(client.name)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectClient(client: Client?) {
        _selectedClient.value = client
    }

    fun selectClientByName(name: String) {
        viewModelScope.launch {
            val clients = allClients.first()
            val match = clients.find { it.name.equals(name, ignoreCase = true) }
            if (match != null) {
                _selectedClient.value = match
            }
        }
    }

    fun navigateTo(screen: Screen) {
        viewModelScope.launch {
            _navigationEvent.emit(screen)
        }
    }

    fun saveBusinessSettings(settings: BusinessSettings) {
        viewModelScope.launch {
            settingsRepository.saveBusinessSettings(settings)
        }
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\viewmodel\StatsViewModel.kt

```kt
package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.BusinessStats
import com.fordham.toolbelt.domain.model.TaxExportOutcome
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.PhoneNumber
import com.fordham.toolbelt.domain.model.EmailAddress
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.usecase.GenerateTaxReportUseCase
import com.fordham.toolbelt.domain.usecase.GetBusinessStatsUseCase
import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatsViewModel(
    private val getBusinessStatsUseCase: GetBusinessStatsUseCase,
    private val generateTaxReportUseCase: GenerateTaxReportUseCase,
    private val settingsRepository: SettingsRepository,
    private val invoiceRepository: InvoiceRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val businessStats: StateFlow<BusinessStats> = getBusinessStatsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessStats())

    val businessSettings: StateFlow<BusinessSettings> = settingsRepository.businessSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessSettings())

    fun togglePremium() {
        viewModelScope.launch {
            val currentSettings = businessSettings.value
            settingsRepository.saveBusinessSettings(
                currentSettings.copy(isPremium = !currentSettings.isPremium)
            )
        }
    }

    fun exportBentoReport(onGenerated: (String) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            when (val outcome = generateTaxReportUseCase.executeBentoReport()) {
                is TaxExportOutcome.Success -> {
                    withContext(Dispatchers.Main) {
                        onGenerated(outcome.path)
                    }
                }
                is TaxExportOutcome.Failure -> {
                    _errorMessage.value = outcome.error.value
                }
                TaxExportOutcome.Loading -> {
                    // No-op or handle loading state if required in future UI upgrades
                }
            }
        }
    }

    fun exportTaxBundle(onGenerated: (String) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            when (val outcome = generateTaxReportUseCase.executeZip()) {
                is TaxExportOutcome.Success -> {
                    withContext(Dispatchers.Main) {
                        onGenerated(outcome.path)
                    }
                }
                is TaxExportOutcome.Failure -> {
                    _errorMessage.value = outcome.error.value
                }
                TaxExportOutcome.Loading -> {
                    // No-op or handle loading state if required in future UI upgrades
                }
            }
        }
    }

    fun createStressTestInvoices() {
        viewModelScope.launch(ioDispatcher) {
            val list = mutableListOf<Invoice>()
            val now = DateTimeUtil.nowEpochMillis()
            val dateStr = "2026-05-17"
            val clients = listOf("Alpha Builders", "Omega Plumbing", "Ironwood Carpentry", "Summit HVAC", "Evergreen Landscapes")
            val items = listOf("Foundations Construction", "Pipe Repairs", "Frame Carpentry", "System Installation", "Sod Layout")
            
            for (i in 1..1000) {
                val client = clients[i % clients.size]
                val desc = items[i % items.size]
                val amt = (100..2000).random().toDouble()
                val isPaid = i % 2 == 0
                val isEstimate = i % 5 != 0
                
                list.add(
                    Invoice(
                        id = InvoiceId(randomUUID()),
                        clientName = client,
                        clientAddress = "123 Construction Rd",
                        clientPhone = PhoneNumber("555-0199"),
                        clientEmail = EmailAddress("stress@test.com"),
                        date = dateStr,
                        totalAmount = amt,
                        depositAmount = if (isPaid) amt * 0.1 else 0.0,
                        itemsSummary = desc,
                        pdfPath = "",
                        isPaid = isPaid,
                        isEstimate = isEstimate,
                        lastUpdated = now - (i * 10000),
                        durationSeconds = (1800..28800).random().toLong()
                    )
                )
            }
            invoiceRepository.insertInvoices(list)
        }
    }

    fun eraseAllInvoices() {
        viewModelScope.launch(ioDispatcher) {
            invoiceRepository.deleteAllInvoices()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

```


---

## .\composeApp\src\commonMain\kotlin\com\fordham\toolbelt\ui\viewmodel\SuppliersViewModel.kt

```kt
package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.usecase.*
import com.fordham.toolbelt.ui.tabs.suppliers.SupplierUiModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SuppliersData(
    val pinnedSuppliers: List<SupplierUiModel>,
    val activeSuppliers: List<SupplierUiModel>
)

sealed interface SuppliersOutcome {
    data object Loading : SuppliersOutcome
    data class Success(val data: SuppliersData) : SuppliersOutcome
    data class Failure(val error: FailureMessage) : SuppliersOutcome
}

class SuppliersViewModel(
    private val getSuppliersUseCase: GetSuppliersUseCase,
    private val updateSupplierOrderUseCase: UpdateSupplierOrderUseCase,
    private val toggleSupplierPinUseCase: ToggleSupplierPinUseCase,
    private val hideSupplierUseCase: HideSupplierUseCase,
    private val addSupplierUseCase: AddSupplierUseCase,
    private val getHiddenSuppliersUseCase: GetHiddenSuppliersUseCase,
    private val restoreSupplierUseCase: RestoreSupplierUseCase,
    private val logSupplierPurchaseUseCase: LogSupplierPurchaseUseCase,
    private val seedSuppliersUseCase: SeedSuppliersUseCase,
    private val placesService: com.fordham.toolbelt.util.PlacesService
) : ViewModel() {
    
    init {
        viewModelScope.launch {
            seedSuppliersUseCase()
        }
    }

    private val _isAddSheetVisible = MutableStateFlow(false)
    val isAddSheetVisible = _isAddSheetVisible.asStateFlow()

    private val _capturedPhotoUri = MutableStateFlow<String?>(null)
    val capturedPhotoUri = _capturedPhotoUri.asStateFlow()

    private val _placeSuggestions = MutableStateFlow<List<com.fordham.toolbelt.util.PlaceSuggestion>>(emptyList())
    val placeSuggestions = _placeSuggestions.asStateFlow()

    private val _isReorderMode = MutableStateFlow(false)
    val isReorderMode = _isReorderMode.asStateFlow()

    private val _reorderList = MutableStateFlow<List<SupplierUiModel>>(emptyList())
    val reorderList = _reorderList.asStateFlow()

    fun onSearchQueryChange(query: String) {
        viewModelScope.launch {
            _placeSuggestions.value = placesService.searchPlaces(query)
        }
    }

    fun clearSuggestions() {
        _placeSuggestions.value = emptyList()
    }

    fun setReorderMode(active: Boolean, currentList: List<SupplierUiModel> = emptyList()) {
        if (active) {
            _reorderList.value = currentList
        }
        _isReorderMode.value = active
    }

    fun swapItems(from: Int, to: Int) {
        val list = _reorderList.value.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        _reorderList.value = list
    }

    fun saveOrder() {
        viewModelScope.launch {
            _reorderList.value.forEachIndexed { index, model ->
                updateSupplierOrderUseCase(model.domain.id, index)
            }
            _isReorderMode.value = false
        }
    }

    val uiState: StateFlow<SuppliersOutcome> = getSuppliersUseCase()
        .map { result ->
            when (result) {
                is SupplierListOutcome.Success -> {
                    val uiModels = result.suppliers.map { it.toUiModel() }
                    val pinned = uiModels.filter { it.domain.isPinned }
                    val active = uiModels.filter { !it.domain.isPinned }
                    SuppliersOutcome.Success(SuppliersData(pinned, active))
                }
                is SupplierListOutcome.Failure -> SuppliersOutcome.Failure(result.error)
            }
        }
        .onStart { emit(SuppliersOutcome.Loading as SuppliersOutcome) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SuppliersOutcome.Loading)

    val hiddenSuppliers: StateFlow<List<SupplierUiModel>> = getHiddenSuppliersUseCase()
        .map { result -> 
            if (result is SupplierListOutcome.Success) {
                result.suppliers.map { it.toUiModel() }
            } else emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreSupplier(id: SupplierId) {
        viewModelScope.launch {
            restoreSupplierUseCase(id)
        }
    }

    fun togglePin(supplier: Supplier) {
        viewModelScope.launch {
            toggleSupplierPinUseCase(supplier.id, !supplier.isPinned)
        }
    }

    fun hideSupplier(id: SupplierId) {
        viewModelScope.launch {
            hideSupplierUseCase(id)
        }
    }

    fun updateOrder(id: SupplierId, newOrder: Int) {
        viewModelScope.launch {
            updateSupplierOrderUseCase(id, newOrder)
        }
    }

    fun addSupplier(name: String, category: SupplierCategory, address: String, phone: String, logoPath: String?) {
        viewModelScope.launch {
            val supplier = Supplier(
                id = SupplierId(com.fordham.toolbelt.util.randomUUID()),
                name = name,
                category = category,
                address = address,
                phone = PhoneNumber(phone),
                customLogoPath = logoPath,
                displayOrder = 0
            )
            addSupplierUseCase(supplier)
            _isAddSheetVisible.value = false
        }
    }

    fun setAddSheetVisible(visible: Boolean) {
        _isAddSheetVisible.value = visible
        if (!visible) _capturedPhotoUri.value = null
    }

    fun onPhotoCaptured(uri: String) {
        _capturedPhotoUri.value = uri
    }

    fun logPurchase(supplierId: SupplierId, amount: Double) {
        viewModelScope.launch {
            logSupplierPurchaseUseCase(supplierId, MoneyAmount(amount))
        }
    }

    private fun Supplier.toUiModel(): SupplierUiModel {
        val logoKey = if (isDefault) {
            when (id.value) {
                "home_depot" -> "logo_home_depot"
                "lowes" -> "logo_lowes"
                "ace" -> "logo_ace"
                "menards" -> "logo_menards"
                "ferguson" -> "logo_ferguson"
                "sherwin" -> "logo_sherwin"
                "grainger" -> "logo_grainger"
                "abc_supply" -> "logo_abc"
                "graybar" -> "logo_graybar"
                "siteone" -> "logo_siteone"
                "amazon_biz" -> "logo_amazon"
                "northern_tool" -> "logo_northern"
                "sunbelt" -> "logo_sunbelt"
                "hilti" -> "logo_hilti"
                "mcmaster" -> "logo_mcmaster"
                else -> null
            }
        } else null
        
        return SupplierUiModel(domain = this, logoKey = logoKey)
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\AppDatabase.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ReceiptEntity::class, 
        InvoiceEntity::class, 
        ClientEntity::class, 
        JobPhotoEntity::class, 
        JobNoteEntity::class,
        SupplierEntity::class,
        DraftInvoiceEntity::class
    ], 
    version = 18,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun clientDao(): ClientDao
    abstract fun photoDao(): PhotoDao
    abstract fun jobNoteDao(): JobNoteDao
    abstract fun supplierDao(): SupplierDao
    abstract fun draftDao(): DraftDao
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\ClientDao.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    suspend fun getAllClientsOnce(): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE name LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%'")
    suspend fun searchClients(query: String): List<ClientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity)

    @Delete
    suspend fun deleteClient(client: ClientEntity)
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\ClientEntity.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fordham.toolbelt.util.randomUUID

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String = randomUUID(),
    val name: String,
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val totalInvoiced: Double = 0.0,
    val isFavorite: Boolean = false
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\DatabaseBuilder.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\DataStoreFactory.kt

```kt
package com.fordham.toolbelt.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

internal const val DATASTORE_FILE_NAME = "settings.preferences_pb"

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\DraftDao.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Query("SELECT * FROM DraftInvoiceEntity WHERE id = 'current_draft'")
    fun getDraft(): Flow<DraftInvoiceEntity?>

    @Upsert
    suspend fun saveDraft(draft: DraftInvoiceEntity)

    @Query("DELETE FROM DraftInvoiceEntity WHERE id = 'current_draft'")
    suspend fun clearDraft()
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\DraftEntities.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
data class DraftInvoiceEntity(
    @PrimaryKey val id: String = "current_draft",
    val clientName: String = "",
    val clientAddress: String = "",
    val taxRate: Double = 7.0,
    val deposit: Double = 0.0,
    val hourlyRate: Double = 50.0,
    val logoUri: String? = null,
    val selectedCategory: String = "Drywall",
    val itemDesc: String = "",
    val itemAmt: String = "",
    val elapsedSeconds: Long = 0L,
    val startTime: Long = 0L,
    val timerRunning: Boolean = false,
    val saveToClientDirectory: Boolean = false,
    val lineItemsJson: String = "[]",
    val capturedPhotosJson: String = "[]",
    val linkedReceiptIdsJson: String = "[]"
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\dto\InvoiceDtos.kt

```kt
package com.fordham.toolbelt.data.dto

import com.fordham.toolbelt.domain.model.AiInvoiceResult
import com.fordham.toolbelt.domain.model.LineItem
import kotlinx.serialization.Serializable

@Serializable
data class LineItemDto(
    val description: String,
    val amount: Double,
    val category: String = "Service"
) {
    fun toDomain() = LineItem(
        description = description,
        amount = amount,
        category = category
    )

    companion object {
        fun fromDomain(domain: LineItem) = LineItemDto(
            description = domain.description,
            amount = domain.amount,
            category = domain.category
        )
    }
}

@Serializable
data class AiInvoiceResultDto(
    val clientName: String = "",
    val clientAddress: String = "",
    val items: List<LineItemDto> = emptyList()
) {
    fun toDomain() = AiInvoiceResult(
        clientName = clientName,
        clientAddress = clientAddress,
        items = items.map { it.toDomain() }
    )
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\DataStoreSettingsRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.SettingsOutcome
import com.fordham.toolbelt.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.*

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val businessSettingsFlow: Flow<BusinessSettings> = dataStore.data.map { prefs ->
        BusinessSettings(
            businessName = prefs[BUSINESS_NAME] ?: "",
            businessSlogan = prefs[BUSINESS_SLOGAN] ?: "",
            businessPhone = prefs[BUSINESS_PHONE] ?: "",
            businessEmail = prefs[BUSINESS_EMAIL] ?: "",
            businessAddress = prefs[BUSINESS_ADDRESS] ?: "",
            isPremium = prefs[IS_PREMIUM] ?: false,
            taxRate = prefs[TAX_RATE] ?: 0.0,
            markupPercentage = prefs[MARKUP_RATE] ?: 0.0,
            logoUri = prefs[LOGO_URI],
            isDarkMode = prefs[DARK_MODE] ?: true,
            useMetricUnits = prefs[USE_METRIC] ?: false,
            notificationsEnabled = prefs[NOTIFICATIONS] ?: true
        )
    }

    override suspend fun getBusinessSettings(): BusinessSettings = businessSettingsFlow.first()

    override suspend fun saveBusinessSettings(settings: BusinessSettings): SettingsOutcome = try {
        dataStore.edit { prefs ->
            prefs[BUSINESS_NAME] = settings.businessName
            prefs[BUSINESS_SLOGAN] = settings.businessSlogan
            prefs[BUSINESS_PHONE] = settings.businessPhone
            prefs[BUSINESS_EMAIL] = settings.businessEmail
            prefs[BUSINESS_ADDRESS] = settings.businessAddress
            prefs[IS_PREMIUM] = settings.isPremium
            prefs[TAX_RATE] = settings.taxRate
            prefs[MARKUP_RATE] = settings.markupPercentage
            prefs[DARK_MODE] = settings.isDarkMode
            prefs[USE_METRIC] = settings.useMetricUnits
            prefs[NOTIFICATIONS] = settings.notificationsEnabled
            settings.logoUri?.let { prefs[LOGO_URI] = it } ?: prefs.remove(LOGO_URI)
        }
        SettingsOutcome.Success
    } catch (e: Exception) {
        SettingsOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save settings"))
    }

    companion object {
        private val BUSINESS_NAME = stringPreferencesKey("business_name")
        private val BUSINESS_SLOGAN = stringPreferencesKey("business_slogan")
        private val BUSINESS_PHONE = stringPreferencesKey("business_phone")
        private val BUSINESS_EMAIL = stringPreferencesKey("business_email")
        private val BUSINESS_ADDRESS = stringPreferencesKey("business_address")
        private val IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val TAX_RATE = doublePreferencesKey("tax_rate")
        private val MARKUP_RATE = doublePreferencesKey("markup_rate")
        private val LOGO_URI = stringPreferencesKey("logo_uri")
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val USE_METRIC = booleanPreferencesKey("use_metric")
        private val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\GeminiAgentLlmGateway.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.ToolParameters
import com.fordham.toolbelt.domain.model.ToolType
import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.CreateDraftInvoiceArgs
import com.fordham.toolbelt.domain.model.agent.DeleteInvoiceApprovalArgs
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.SearchClientsArgs
import com.fordham.toolbelt.domain.model.agent.ToolCallId
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.repository.AgentLlmGateway
import com.fordham.toolbelt.domain.repository.ClientRepository
import com.fordham.toolbelt.domain.repository.GeminiRepository

class GeminiAgentLlmGateway(
    private val geminiRepository: GeminiRepository,
    private val clientRepository: ClientRepository
) : AgentLlmGateway {
    override suspend fun prompt(
        systemPrompt: NaturalLanguage,
        session: ForemanSession,
        functions: List<AgentFunction>
    ): AgentOutcome {
        val command = session.history.lastOrNull()?.content?.value.orEmpty()
        if (command.isBlank()) {
            return AgentOutcome.Failure(FailureMessage("Agent command cannot be blank."))
        }

        return when (val result = geminiRepository.generateToolCall(command, systemPrompt.value)) {
            is com.fordham.toolbelt.domain.model.ToolCallOutcome.Failure -> AgentOutcome.Failure(result.error)
            is com.fordham.toolbelt.domain.model.ToolCallOutcome.Success -> {
                val toolCall = result.toolCall ?: return AgentOutcome.TextResponse(
                    NaturalLanguage("I can help with invoices, clients, receipts, and job history. Try asking me to find a client or start a draft invoice.")
                )

                when (val params = toolCall.parameters) {
                    is ToolParameters.SearchClients -> AgentOutcome.ToolExecutionRequested(
                        toolCallId = ToolCallId(toolCall.id),
                        toolName = ToolName.SearchClients,
                        arguments = SearchClientsArgs(NaturalLanguage(params.query))
                    )

                    is ToolParameters.CreateDraftInvoice -> {
                        val client = clientRepository.searchClients(params.clientName).firstOrNull()
                            ?: return AgentOutcome.Failure(
                                FailureMessage("I could not find a client named ${params.clientName}.")
                            )
                        AgentOutcome.ToolExecutionRequested(
                            toolCallId = ToolCallId(toolCall.id),
                            toolName = ToolName.CreateDraftInvoice,
                            arguments = CreateDraftInvoiceArgs(client.id)
                        )
                    }

                    is ToolParameters.DeleteInvoice -> AgentOutcome.ToolExecutionRequested(
                        toolCallId = ToolCallId(toolCall.id),
                        toolName = ToolName.DeleteInvoiceForApproval,
                        arguments = DeleteInvoiceApprovalArgs(params.invoiceId)
                    )

                    else -> unsupportedTool(toolCall.type)
                }
            }
        }
    }

    private fun unsupportedTool(toolType: ToolType): AgentOutcome {
        return AgentOutcome.TextResponse(
            NaturalLanguage("I understood the request as ${toolType.name}, but that action is not wired into the typed agent yet.")
        )
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\GeminiOcrRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.GeminiOutcome
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.repository.OcrRepository

class GeminiOcrRepository(
    private val geminiRepository: GeminiRepository
) : OcrRepository {
    override suspend fun recognizeText(imageBytes: ByteArray): GeminiOutcome {
        return geminiRepository.processOcrImage(imageBytes)
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\KtorGeminiRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.*
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.data.dto.AiInvoiceResultDto
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.util.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ToolCallDto(
    val toolName: String, 
    val params: Map<String, String> = emptyMap(), 
    val reasoning: String = ""
)

class KtorGeminiRepository(
    private val httpClient: HttpClient,
    private val secretProvider: SecretProvider,
    private val jobNoteDao: com.fordham.toolbelt.data.JobNoteDao,
    private val clientDao: com.fordham.toolbelt.data.ClientDao,
    private val settingsRepository: com.fordham.toolbelt.domain.repository.SettingsRepository
) : GeminiRepository {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val modelName = secretProvider.getGeminiModelName().also { check(it.isNotBlank()) { "Gemini model name is blank" } }
    private val apiKey = secretProvider.getGeminiApiKey().also { check(it.isNotBlank()) { "Gemini API key is blank" } }
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    override suspend fun processTask(type: TaskType, data: String): GeminiOutcome = try {
        val context = jobNoteDao.getRelevantContext(data.take(20))
        val contextString = context.joinToString("\n") { it.text }

        val prompt = """
            Task: ${type.name}
            Context: $contextString
            Input: $data
        """.trimIndent()

        val response = callGemini(prompt)
        val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        GeminiOutcome.Success(text)
    } catch (e: Exception) {
        GeminiOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to process Gemini task"))
    }

    override suspend fun processAgentCommand(input: String): AgentCommandOutcome {
        return AgentCommandOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage("Use generateToolCall instead"))
    }

    override suspend fun generateToolCall(input: String, context: String): ToolCallOutcome = try {
        val toolPrompt = """
            You are the Foreman AI Brain, the central orchestrator for the Invoice Hammer KMP app.
            Your job is to translate user natural language commands into a single, highly accurate tool call.

            [CURRENT CONTEXT]
            $context
            
            [USER INPUT]
            "$input"

            [AVAILABLE TOOLS]
            1. SEARCH_CLIENTS
               - Description: Searches the client directory.
               - Parameters: { "query": "string" }
            2. GET_CLIENT_DETAILS
               - Description: Retrieves full profile and history for a specific client.
               - Parameters: { "clientName": "string" }
            3. CREATE_DRAFT_INVOICE
               - Description: Creates a new draft invoice.
               - Parameters: { "clientName": "string", "amount": "number", "items": "string description" }
            4. DELETE_INVOICE
               - Description: [DESTRUCTIVE] Deletes an existing invoice.
               - Parameters: { "invoiceId": "string" }
            5. ADD_JOB_NOTE
               - Description: Adds a progress note to a client profile.
               - Parameters: { "clientName": "string", "note": "string content" }
            6. OPEN_TAB
               - Description: Navigates to a specific app screen.
               - Parameters: { "tabName": "CLIENTS" | "HISTORY" | "RECEIPTS" | "SETTINGS" | "STATS" | "SUPPLIERS" }

            [CRITICAL SAFETY RULES]
            - Only call DELETE_INVOICE if the user explicitly requests deletion of a specific invoice.
            - If the user's intent is conversational or doesn't map to a tool, return toolName: "UNKNOWN" and explain why in the reasoning.

            [RESPONSE FORMAT]
            You must output valid JSON only. Do not wrap in markdown block.
            {
              "toolName": "SEARCH_CLIENTS" | "GET_CLIENT_DETAILS" | "CREATE_DRAFT_INVOICE" | "DELETE_INVOICE" | "ADD_JOB_NOTE" | "OPEN_TAB" | "UNKNOWN",
              "params": { "key": "value" },
              "reasoning": "Explain step-by-step why you chose this action based on context."
            }
        """.trimIndent()

        val response = callGemini(toolPrompt, responseMimeType = "application/json")
        val resText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        val cleanedJson = AiUtil.cleanJson(resText)
        
        val dto = json.decodeFromString<ToolCallDto>(cleanedJson)
        
        ToolCallOutcome.Success(mapDtoToToolCall(dto))
    } catch (e: Exception) {
        ToolCallOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to generate tool call"))
    }

    private fun mapDtoToToolCall(dto: ToolCallDto): ForemanToolCall? {
        val type = try { ToolType.valueOf(dto.toolName) } catch (e: Exception) { return null }
        if (type == ToolType.UNKNOWN) return null

        val parameters = when (type) {
            ToolType.SEARCH_CLIENTS -> ToolParameters.SearchClients(dto.params["query"] ?: "")
            ToolType.GET_CLIENT_DETAILS -> ToolParameters.GetClientDetails(dto.params["clientName"] ?: "")
            ToolType.CREATE_DRAFT_INVOICE -> ToolParameters.CreateDraftInvoice(
                dto.params["clientName"] ?: "",
                dto.params["amount"]?.toDoubleOrNull() ?: 0.0,
                dto.params["items"] ?: ""
            )
            ToolType.DELETE_INVOICE -> ToolParameters.DeleteInvoice(InvoiceId(dto.params["invoiceId"] ?: ""))
            ToolType.ADD_JOB_NOTE -> ToolParameters.AddJobNote(dto.params["clientName"] ?: "", dto.params["note"] ?: "")
            ToolType.OPEN_TAB -> ToolParameters.OpenTab(dto.params["tabName"] ?: "")
            else -> ToolParameters.None
        }

        return ForemanToolCall(
            id = randomUUID(),
            type = type,
            parameters = parameters,
            reasoning = dto.reasoning
        )
    }

    override suspend fun processInvoiceText(text: String, categories: List<String>): InvoiceTextOutcome = try {
        val prompt = "Extract invoice data: $text"
        val response = callGemini(prompt, responseMimeType = "application/json")
        val resText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        val result = json.decodeFromString<AiInvoiceResultDto>(AiUtil.cleanJson(resText)).toDomain()
        InvoiceTextOutcome.Success(result)
    } catch (e: Exception) {
        InvoiceTextOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to process invoice text"))
    }

    override suspend fun processReceiptImage(imageBytes: ByteArray): ReceiptImageOutcome = try {
        val prompt = "Extract receipt items from this image. Return JSON with 'items' array containing 'description' and 'totalPrice' for each line item."
        val response = callGemini(prompt, imageBytes = imageBytes, responseMimeType = "application/json")
        val resText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        
        val aiResponse = json.decodeFromString<AiReceiptResponse>(AiUtil.cleanJson(resText))
        val items = aiResponse.items.map { 
            ReceiptItem(
                id = ReceiptId(randomUUID()),
                description = it.description,
                totalPrice = it.totalPrice,
                lastUpdated = DateTimeUtil.nowEpochMillis(),
                clientName = ""
            )
        }
        ReceiptImageOutcome.Success(items)
    } catch (e: Exception) {
        ReceiptImageOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to process receipt image"))
    }

    override suspend fun processOcrImage(imageBytes: ByteArray): GeminiOutcome = try {
        val prompt = "Extract all text from this image exactly as written. Return only the raw text. Do not summarize."
        val response = callGemini(prompt, imageBytes = imageBytes, responseMimeType = "text/plain")
        val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        GeminiOutcome.Success(text)
    } catch (e: Exception) {
        GeminiOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to extract text from image"))
    }

    private suspend fun callGemini(
        prompt: String,
        imageBytes: ByteArray? = null,
        responseMimeType: String? = null
    ): GeminiResponse {
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOfNotNull(
                        GeminiPart(text = prompt),
                        imageBytes?.let { 
                            GeminiPart(inlineData = GeminiInlineData("image/jpeg", encodeBase64(it))) 
                        }
                    )
                )
            ),
            generationConfig = responseMimeType?.let { GeminiGenerationConfig(it) }
        )

        return httpClient.post(baseUrl) {
            header("x-goog-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\KtorPowerPayClient.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.PowerPayClient
import com.fordham.toolbelt.data.remote.PowerPayClientOutcome
import com.fordham.toolbelt.data.remote.PowerPayConfig
import com.fordham.toolbelt.data.remote.PowerPayCreatePaymentRequestDto
import com.fordham.toolbelt.data.remote.PowerPayPaymentResponseDto
import com.fordham.toolbelt.domain.model.FailureMessage
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class KtorPowerPayClient(
    private val httpClient: HttpClient,
    private val config: PowerPayConfig
) : PowerPayClient {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override suspend fun createInvoicePayment(request: PowerPayCreatePaymentRequestDto): PowerPayClientOutcome<PowerPayPaymentResponseDto> {
        if (!config.isConfigured) {
            return PowerPayClientOutcome.Failure(FailureMessage("PowerPay backend is not configured yet."))
        }

        return try {
            val response = httpClient.post("${config.baseUrl.trimEnd('/')}/v1/invoice-payments") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                config.apiKey?.takeIf { it.isNotBlank() }?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(json.encodeToString(request))
            }

            val body = response.bodyAsText()
            if (response.status.isSuccess()) {
                PowerPayClientOutcome.Success(json.decodeFromString<PowerPayPaymentResponseDto>(body))
            } else {
                PowerPayClientOutcome.Failure(FailureMessage("PowerPay create payment failed with HTTP ${response.status.value}. $body"))
            }
        } catch (e: Exception) {
            PowerPayClientOutcome.Failure(FailureMessage(e.message ?: "PowerPay create payment failed."))
        }
    }

    override suspend fun getTransactionHistory(): PowerPayClientOutcome<List<PowerPayPaymentResponseDto>> {
        if (!config.isConfigured) {
            return PowerPayClientOutcome.Failure(FailureMessage("PowerPay backend is not configured yet."))
        }

        return try {
            val response = httpClient.get("${config.baseUrl.trimEnd('/')}/v1/transactions") {
                accept(ContentType.Application.Json)
                config.apiKey?.takeIf { it.isNotBlank() }?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }

            val body = response.bodyAsText()
            if (response.status.isSuccess()) {
                PowerPayClientOutcome.Success(json.decodeFromString<List<PowerPayPaymentResponseDto>>(body))
            } else {
                PowerPayClientOutcome.Failure(FailureMessage("PowerPay transaction history failed with HTTP ${response.status.value}. $body"))
            }
        } catch (e: Exception) {
            PowerPayClientOutcome.Failure(FailureMessage(e.message ?: "PowerPay transaction history failed."))
        }
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\KtorSyncRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.BackupFileName
import com.fordham.toolbelt.domain.model.BackupPayload
import com.fordham.toolbelt.domain.model.ClientListOutcome
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.SyncOutcome
import com.fordham.toolbelt.domain.model.SyncUploadOutcome
import com.fordham.toolbelt.domain.model.SupplierListOutcome
import com.fordham.toolbelt.domain.repository.ClientRepository
import com.fordham.toolbelt.domain.repository.DriveAuthTokenProvider
import com.fordham.toolbelt.domain.repository.DriveTokenOutcome
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.domain.repository.SyncRepository
import com.fordham.toolbelt.domain.repository.SupplierRepository
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*

/**
 * Responsibility: Handle cloud synchronization using Ktor. 
 * Replaces platform-specific implementations to ensure KMP compliance.
 */
class KtorSyncRepository(
    private val httpClient: HttpClient,
    private val driveAuthTokenProvider: DriveAuthTokenProvider,
    private val invoiceRepository: InvoiceRepository,
    private val receiptRepository: ReceiptRepository,
    private val clientRepository: ClientRepository,
    private val supplierRepository: SupplierRepository,
    private val settingsRepository: SettingsRepository
) : SyncRepository {
    
    private val driveUploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

    override suspend fun syncInvoices(): SyncOutcome = withContext(Dispatchers.IO) {
        val backup = BackupPayload(buildBackupJson().toString().encodeToByteArray())
        when (val upload = uploadToDrive(BackupFileName("invoice-hammer-backup.json"), backup)) {
            is SyncUploadOutcome.Success -> SyncOutcome.Success
            is SyncUploadOutcome.Failure -> SyncOutcome.Failure(upload.error)
        }
    }

    override suspend fun syncReceipts(): SyncOutcome = withContext(Dispatchers.IO) {
        syncInvoices()
    }

    override suspend fun uploadToDrive(fileName: BackupFileName, content: BackupPayload): SyncUploadOutcome = withContext(Dispatchers.IO) {
        val token = when (val tokenOutcome = driveAuthTokenProvider.getDriveAccessToken()) {
            is DriveTokenOutcome.Success -> tokenOutcome.token.value
            is DriveTokenOutcome.Failure -> return@withContext SyncUploadOutcome.Failure(tokenOutcome.error)
        }

        return@withContext try {
            val boundary = "invoice-hammer-${Clock.System.now().toEpochMilliseconds()}"
            val body = buildMultipartBody(
                boundary = boundary,
                fileName = fileName.value,
                content = content.bytes
            )
            val multipartContentType = ContentType.parse("multipart/related; boundary=$boundary")

            val response = httpClient.post(driveUploadUrl) {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
                setBody(DriveMultipartContent(body, multipartContentType))
            }

            if (response.status.isSuccess()) {
                SyncUploadOutcome.Success("appDataFolder/${fileName.value}")
            } else {
                val responseBody = response.bodyAsText().trim()
                val details = responseBody.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
                SyncUploadOutcome.Failure(
                    FailureMessage("Drive upload failed with HTTP ${response.status.value}.$details")
                )
            }
        } catch (e: Exception) {
            SyncUploadOutcome.Failure(FailureMessage(e.message ?: "Failed to upload backup to Google Drive."))
        }
    }

    private suspend fun buildBackupJson(): JsonObject {
        val invoices = invoiceRepository.allInvoices.first()
        val receipts = when (val outcome = receiptRepository.allItems.first()) {
            is ReceiptListOutcome.Success -> outcome.receipts
            is ReceiptListOutcome.Failure -> emptyList()
        }
        val clients = when (val outcome = clientRepository.getAllClients().first()) {
            is ClientListOutcome.Success -> outcome.clients
            is ClientListOutcome.Failure -> emptyList()
        }
        val visibleSuppliers = when (val outcome = supplierRepository.getVisibleSuppliers().first()) {
            is SupplierListOutcome.Success -> outcome.suppliers
            is SupplierListOutcome.Failure -> emptyList()
        }
        val hiddenSuppliers = when (val outcome = supplierRepository.getHiddenSuppliers().first()) {
            is SupplierListOutcome.Success -> outcome.suppliers
            is SupplierListOutcome.Failure -> emptyList()
        }
        val settings = settingsRepository.getBusinessSettings()

        return buildJsonObject {
            put("schemaVersion", 1)
            put("exportedAtMillis", Clock.System.now().toEpochMilliseconds())
            putJsonObject("settings") {
                put("businessName", settings.businessName)
                put("businessSlogan", settings.businessSlogan)
                put("businessPhone", settings.businessPhone)
                put("businessEmail", settings.businessEmail)
                put("businessAddress", settings.businessAddress)
                put("taxRate", settings.taxRate)
                put("markupPercentage", settings.markupPercentage)
                put("isPremium", settings.isPremium)
                put("isDarkMode", settings.isDarkMode)
                put("useMetricUnits", settings.useMetricUnits)
                put("notificationsEnabled", settings.notificationsEnabled)
            }
            putJsonArray("clients") {
                clients.forEach { client ->
                    addJsonObject {
                        put("id", client.id.value)
                        put("name", client.name)
                        put("email", client.email.value)
                        put("phone", client.phone.value)
                        put("address", client.address)
                        put("notes", client.notes)
                        put("totalInvoiced", client.totalInvoiced)
                        put("isFavorite", client.isFavorite)
                        put("lastUpdated", client.lastUpdated)
                    }
                }
            }
            putJsonArray("invoices") {
                invoices.forEach { invoice ->
                    addJsonObject {
                        put("id", invoice.id.value)
                        put("clientName", invoice.clientName)
                        put("clientAddress", invoice.clientAddress)
                        put("clientPhone", invoice.clientPhone.value)
                        put("clientEmail", invoice.clientEmail.value)
                        put("date", invoice.date)
                        put("totalAmount", invoice.totalAmount)
                        put("depositAmount", invoice.depositAmount)
                        put("itemsSummary", invoice.itemsSummary)
                        put("pdfPath", invoice.pdfPath)
                        put("isPaid", invoice.isPaid)
                        put("isEstimate", invoice.isEstimate)
                        put("lastUpdated", invoice.lastUpdated)
                        put("durationSeconds", invoice.durationSeconds)
                    }
                }
            }
            putJsonArray("receipts") {
                receipts.forEach { receipt ->
                    addJsonObject {
                        put("id", receipt.id.value)
                        put("description", receipt.description)
                        put("quantity", receipt.quantity)
                        put("unitPrice", receipt.unitPrice)
                        put("totalPrice", receipt.totalPrice)
                        put("category", receipt.category)
                        put("clientName", receipt.clientName)
                        put("imagePath", receipt.imagePath)
                        put("isBilled", receipt.isBilled)
                        put("lastUpdated", receipt.lastUpdated)
                        put("supplierName", receipt.supplierName)
                        put("linkedInvoiceId", receipt.linkedInvoiceId?.value)
                    }
                }
            }
            putJsonArray("suppliers") {
                (visibleSuppliers + hiddenSuppliers).forEach { supplier ->
                    addJsonObject {
                        put("id", supplier.id.value)
                        put("name", supplier.name)
                        put("category", supplier.category.name)
                        put("address", supplier.address)
                        put("phone", supplier.phone.value)
                        put("webUrl", supplier.webUrl)
                        put("packageName", supplier.packageName)
                        put("displayOrder", supplier.displayOrder)
                        put("isPinned", supplier.isPinned)
                        put("isHidden", supplier.isHidden)
                        put("customLogoPath", supplier.customLogoPath)
                        put("logoResName", supplier.logoResName)
                        put("isDefault", supplier.isDefault)
                    }
                }
            }
        }
    }

    private fun buildMultipartBody(
        boundary: String,
        fileName: String,
        content: ByteArray
    ): ByteArray {
        val metadata = buildJsonObject {
            put("name", fileName)
            putJsonArray("parents") {
                add("appDataFolder")
            }
        }.toString()

        val prefix = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n")
            append("\r\n")
            append(metadata).append("\r\n")
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json\r\n")
            append("\r\n")
        }.encodeToByteArray()
        val suffix = "\r\n--$boundary--\r\n".encodeToByteArray()

        return prefix + content + suffix
    }

    private class DriveMultipartContent(
        private val bytes: ByteArray,
        override val contentType: ContentType
    ) : OutgoingContent.ByteArrayContent() {
        override val contentLength: Long = bytes.size.toLong()

        override fun bytes(): ByteArray = bytes
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\MockPaymentRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.InvoicePaymentStatus
import com.fordham.toolbelt.domain.model.MoneyAmount
import com.fordham.toolbelt.domain.model.PaymentLedgerOutcome
import com.fordham.toolbelt.domain.model.PaymentLinkUrl
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestId
import com.fordham.toolbelt.domain.model.PaymentRequestOutcome
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.repository.PaymentRepository
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockPaymentRepository : PaymentRepository {
    private val requests = MutableStateFlow<PaymentLedgerOutcome>(PaymentLedgerOutcome.Success(emptyList()))

    override val ledger: Flow<PaymentLedgerOutcome> = requests.asStateFlow()

    override suspend fun createPaymentRequest(invoice: Invoice, type: PaymentRequestType, provider: PaymentProviderType): PaymentRequestOutcome {
        if (invoice.totalAmount <= 0.0) {
            return PaymentRequestOutcome.Failure(FailureMessage("Invoice must have a positive total before requesting payment."))
        }

        val amount = when (type) {
            PaymentRequestType.Deposit -> invoice.depositAmount.takeIf { it > 0.0 } ?: invoice.totalAmount * DEFAULT_DEPOSIT_PERCENT
            PaymentRequestType.FullBalance -> invoice.totalAmount
        }

        val request = InvoicePaymentRequest(
            id = PaymentRequestId(randomUUID()),
            invoiceId = invoice.id,
            invoiceClientName = invoice.clientName,
            type = type,
            provider = provider,
            requestedAmount = MoneyAmount(amount),
            status = InvoicePaymentStatus.Requested,
            paymentLink = PaymentLinkUrl("https://pay.invoicehammer.dev/mock/${provider.pathSegment}/${invoice.id.value}"),
            assetCode = if (provider == PaymentProviderType.StellarUsdc) "USDC" else "USD"
        )

        val current = (requests.value as? PaymentLedgerOutcome.Success)?.requests.orEmpty()
        requests.value = PaymentLedgerOutcome.Success(listOf(request) + current.filterNot { it.invoiceId == invoice.id && it.type == type && it.provider == provider })

        return PaymentRequestOutcome.Success(request)
    }

    override suspend fun refreshLedger(): PaymentLedgerOutcome {
        return requests.value
    }

    private companion object {
        const val DEFAULT_DEPOSIT_PERCENT = 0.30
    }
}

private val PaymentProviderType.pathSegment: String
    get() = when (this) {
        PaymentProviderType.GooglePay -> "google-pay"
        PaymentProviderType.ApplePay -> "apple-pay"
        PaymentProviderType.StellarUsdc -> "stellar-usdc"
        PaymentProviderType.CardLink -> "card"
    }

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\MockPowerPayClient.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.PowerPayClient
import com.fordham.toolbelt.data.remote.PowerPayClientOutcome
import com.fordham.toolbelt.data.remote.PowerPayCreatePaymentRequestDto
import com.fordham.toolbelt.data.remote.PowerPayPaymentResponseDto
import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

class MockPowerPayClient : PowerPayClient {
    private val payments = mutableListOf<PowerPayPaymentResponseDto>()

    override suspend fun createInvoicePayment(request: PowerPayCreatePaymentRequestDto): PowerPayClientOutcome<PowerPayPaymentResponseDto> {
        val response = PowerPayPaymentResponseDto(
            paymentId = randomUUID(),
            invoiceId = request.invoiceId,
            clientName = request.clientName,
            amountUsd = request.amountUsd,
            requestType = request.requestType,
            provider = request.provider,
            status = "requested",
            paymentLinkUrl = "https://pay.invoicehammer.dev/mock/${request.provider}/${request.invoiceId}",
            assetCode = if (request.provider == "stellar_usdc") "USDC" else "USD",
            createdAtMillis = Clock.System.now().toEpochMilliseconds()
        )
        payments.removeAll { it.invoiceId == response.invoiceId && it.requestType == response.requestType && it.provider == response.provider }
        payments.add(0, response)
        return PowerPayClientOutcome.Success(response)
    }

    override suspend fun getTransactionHistory(): PowerPayClientOutcome<List<PowerPayPaymentResponseDto>> {
        return PowerPayClientOutcome.Success(payments.toList())
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\PowerPayPaymentRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.remote.PowerPayClient
import com.fordham.toolbelt.data.remote.PowerPayClientOutcome
import com.fordham.toolbelt.data.remote.PowerPayCreatePaymentRequestDto
import com.fordham.toolbelt.data.remote.PowerPayPaymentResponseDto
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.InvoicePaymentRequest
import com.fordham.toolbelt.domain.model.InvoicePaymentStatus
import com.fordham.toolbelt.domain.model.MoneyAmount
import com.fordham.toolbelt.domain.model.PaymentLedgerOutcome
import com.fordham.toolbelt.domain.model.PaymentLinkUrl
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestId
import com.fordham.toolbelt.domain.model.PaymentRequestOutcome
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.model.StellarTransactionHash
import com.fordham.toolbelt.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PowerPayPaymentRepository(
    private val powerPayClient: PowerPayClient
) : PaymentRepository {
    private val requests = MutableStateFlow<PaymentLedgerOutcome>(PaymentLedgerOutcome.Success(emptyList()))

    override val ledger: Flow<PaymentLedgerOutcome> = requests.asStateFlow()

    override suspend fun createPaymentRequest(
        invoice: Invoice,
        type: PaymentRequestType,
        provider: PaymentProviderType
    ): PaymentRequestOutcome {
        if (invoice.totalAmount <= 0.0) {
            return PaymentRequestOutcome.Failure(FailureMessage("Invoice must have a positive total before requesting payment."))
        }

        val amount = when (type) {
            PaymentRequestType.Deposit -> invoice.depositAmount.takeIf { it > 0.0 } ?: invoice.totalAmount * DEFAULT_DEPOSIT_PERCENT
            PaymentRequestType.FullBalance -> invoice.totalAmount
        }

        val outcome = powerPayClient.createInvoicePayment(
            PowerPayCreatePaymentRequestDto(
                invoiceId = invoice.id.value,
                clientName = invoice.clientName,
                amountUsd = amount,
                requestType = type.wireName,
                provider = provider.wireName,
                description = "${type.descriptionLabel} for ${invoice.clientName}"
            )
        )

        return when (outcome) {
            is PowerPayClientOutcome.Success -> {
                val request = outcome.value.toDomain()
                val current = (requests.value as? PaymentLedgerOutcome.Success)?.requests.orEmpty()
                requests.value = PaymentLedgerOutcome.Success(
                    listOf(request) + current.filterNot {
                        it.invoiceId == request.invoiceId && it.type == request.type && it.provider == request.provider
                    }
                )
                PaymentRequestOutcome.Success(request)
            }
            is PowerPayClientOutcome.Failure -> PaymentRequestOutcome.Failure(outcome.error)
        }
    }

    override suspend fun refreshLedger(): PaymentLedgerOutcome {
        val outcome = powerPayClient.getTransactionHistory()
        val ledgerOutcome = when (outcome) {
            is PowerPayClientOutcome.Success -> PaymentLedgerOutcome.Success(outcome.value.map { it.toDomain() })
            is PowerPayClientOutcome.Failure -> PaymentLedgerOutcome.Failure(outcome.error)
        }
        requests.value = ledgerOutcome
        return ledgerOutcome
    }

    private fun PowerPayPaymentResponseDto.toDomain(): InvoicePaymentRequest {
        return InvoicePaymentRequest(
            id = PaymentRequestId(paymentId),
            invoiceId = InvoiceId(invoiceId),
            invoiceClientName = clientName,
            type = requestType.toPaymentRequestType(),
            provider = provider.toPaymentProviderType(),
            requestedAmount = MoneyAmount(amountUsd),
            status = status.toInvoicePaymentStatus(),
            paymentLink = PaymentLinkUrl(paymentLinkUrl),
            createdAtMillis = createdAtMillis,
            stellarTransactionHash = transactionHash?.let { StellarTransactionHash(it) },
            assetCode = assetCode
        )
    }

    private fun String.toPaymentRequestType(): PaymentRequestType = when (this) {
        "deposit" -> PaymentRequestType.Deposit
        "full_balance" -> PaymentRequestType.FullBalance
        else -> PaymentRequestType.FullBalance
    }

    private fun String.toPaymentProviderType(): PaymentProviderType = when (this) {
        "google_pay" -> PaymentProviderType.GooglePay
        "apple_pay" -> PaymentProviderType.ApplePay
        "stellar_usdc" -> PaymentProviderType.StellarUsdc
        "card_link" -> PaymentProviderType.CardLink
        else -> PaymentProviderType.CardLink
    }

    private fun String.toInvoicePaymentStatus(): InvoicePaymentStatus = when (this) {
        "requested" -> InvoicePaymentStatus.Requested
        "pending" -> InvoicePaymentStatus.Pending
        "paid" -> InvoicePaymentStatus.Paid
        "failed" -> InvoicePaymentStatus.Failed
        "expired" -> InvoicePaymentStatus.Expired
        else -> InvoicePaymentStatus.Pending
    }

    private companion object {
        const val DEFAULT_DEPOSIT_PERCENT = 0.30
    }
}

private val PaymentRequestType.wireName: String
    get() = when (this) {
        PaymentRequestType.Deposit -> "deposit"
        PaymentRequestType.FullBalance -> "full_balance"
    }

private val PaymentRequestType.descriptionLabel: String
    get() = when (this) {
        PaymentRequestType.Deposit -> "Deposit request"
        PaymentRequestType.FullBalance -> "Full payment request"
    }

private val PaymentProviderType.wireName: String
    get() = when (this) {
        PaymentProviderType.GooglePay -> "google_pay"
        PaymentProviderType.ApplePay -> "apple_pay"
        PaymentProviderType.StellarUsdc -> "stellar_usdc"
        PaymentProviderType.CardLink -> "card_link"
    }

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\RepositoryToolRegistry.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.DraftInvoice
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.InvoiceOutcome
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.model.agent.ClientSearchHit
import com.fordham.toolbelt.domain.model.agent.CreateDraftInvoiceArgs
import com.fordham.toolbelt.domain.model.agent.CurrencyAmountCents
import com.fordham.toolbelt.domain.model.agent.DeleteInvoiceApprovalArgs
import com.fordham.toolbelt.domain.model.agent.FunctionParameter
import com.fordham.toolbelt.domain.model.agent.GetUnbilledReceiptsArgs
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.ParameterName
import com.fordham.toolbelt.domain.model.agent.ParameterType
import com.fordham.toolbelt.domain.model.agent.SearchClientsArgs
import com.fordham.toolbelt.domain.model.agent.SendInvoiceApprovalArgs
import com.fordham.toolbelt.domain.model.agent.ToolArguments
import com.fordham.toolbelt.domain.model.agent.ToolDescription
import com.fordham.toolbelt.domain.model.agent.ToolExecutionResult
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.model.agent.UnbilledReceiptSummary
import com.fordham.toolbelt.domain.repository.ClientRepository
import com.fordham.toolbelt.domain.repository.DraftRepository
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.ToolRegistry
import com.fordham.toolbelt.util.randomUUID
import kotlinx.coroutines.flow.first
import kotlin.math.roundToLong

class RepositoryToolRegistry(
    private val clientRepository: ClientRepository,
    private val receiptRepository: ReceiptRepository,
    private val draftRepository: DraftRepository,
    private val invoiceRepository: InvoiceRepository
) : ToolRegistry {
    override fun availableFunctions(): List<AgentFunction> {
        return listOf(
            AgentFunction(
                toolName = ToolName.SearchClients,
                description = ToolDescription("Search the client directory by name, address, phone, or notes."),
                parameters = listOf(FunctionParameter(ParameterName("query"), ParameterType.Text, required = true))
            ),
            AgentFunction(
                toolName = ToolName.GetUnbilledReceipts,
                description = ToolDescription("Find unbilled receipts for a resolved client."),
                parameters = listOf(FunctionParameter(ParameterName("clientId"), ParameterType.Text, required = true))
            ),
            AgentFunction(
                toolName = ToolName.CreateDraftInvoice,
                description = ToolDescription("Start a draft invoice for a resolved client."),
                parameters = listOf(FunctionParameter(ParameterName("clientId"), ParameterType.Text, required = true))
            ),
            AgentFunction(
                toolName = ToolName.SendInvoiceForApproval,
                description = ToolDescription("Queue an invoice send action for explicit user approval."),
                parameters = listOf(FunctionParameter(ParameterName("invoiceId"), ParameterType.Text, required = true))
            ),
            AgentFunction(
                toolName = ToolName.DeleteInvoiceForApproval,
                description = ToolDescription("Queue an invoice delete action for explicit user approval."),
                parameters = listOf(FunctionParameter(ParameterName("invoiceId"), ParameterType.Text, required = true))
            )
        )
    }

    override suspend fun execute(
        toolName: ToolName,
        arguments: ToolArguments
    ): ToolExecutionResult {
        return when (arguments) {
            is SearchClientsArgs -> executeSearchClients(arguments)
            is GetUnbilledReceiptsArgs -> executeGetUnbilledReceipts(arguments)
            is CreateDraftInvoiceArgs -> executeCreateDraftInvoice(arguments)
            is SendInvoiceApprovalArgs -> executeSendInvoiceApproval(arguments)
            is DeleteInvoiceApprovalArgs -> executeDeleteInvoiceApproval(arguments)
        }
    }

    private suspend fun executeSearchClients(arguments: SearchClientsArgs): ToolExecutionResult {
        val clients = clientRepository.searchClients(arguments.query.value)
        return ToolExecutionResult.ClientSearchCompleted(
            clients = clients.map { client ->
                ClientSearchHit(
                    clientId = client.id,
                    displayName = NaturalLanguage(client.name)
                )
            }
        )
    }

    private suspend fun executeGetUnbilledReceipts(arguments: GetUnbilledReceiptsArgs): ToolExecutionResult {
        val client = clientRepository.getAllClients().first().let { outcome ->
            when (outcome) {
                is com.fordham.toolbelt.domain.model.ClientListOutcome.Success ->
                    outcome.clients.firstOrNull { it.id == arguments.clientId }
                is com.fordham.toolbelt.domain.model.ClientListOutcome.Failure -> null
            }
        } ?: return ToolExecutionResult.Failure(
            toolName = ToolName.GetUnbilledReceipts,
            error = FailureMessage("Client was not found.")
        )

        return when (val receipts = receiptRepository.getItemsByClient(client.name).first()) {
            is ReceiptListOutcome.Failure -> ToolExecutionResult.Failure(
                toolName = ToolName.GetUnbilledReceipts,
                error = receipts.error
            )
            is ReceiptListOutcome.Success -> ToolExecutionResult.UnbilledReceiptsFound(
                clientId = arguments.clientId,
                receipts = receipts.receipts
                    .filterNot { it.isBilled }
                    .map { receipt ->
                        UnbilledReceiptSummary(
                            receiptId = receipt.id,
                            supplierName = NaturalLanguage(receipt.supplierName.ifBlank { "General" }),
                            amount = CurrencyAmountCents((receipt.totalPrice * 100.0).roundToLong())
                        )
                    }
            )
        }
    }

    private suspend fun executeCreateDraftInvoice(arguments: CreateDraftInvoiceArgs): ToolExecutionResult {
        val client = clientRepository.getAllClients().first().let { outcome ->
            when (outcome) {
                is com.fordham.toolbelt.domain.model.ClientListOutcome.Success ->
                    outcome.clients.firstOrNull { it.id == arguments.clientId }
                is com.fordham.toolbelt.domain.model.ClientListOutcome.Failure -> null
            }
        } ?: return ToolExecutionResult.Failure(
            toolName = ToolName.CreateDraftInvoice,
            error = FailureMessage("Client was not found.")
        )

        val invoiceId = com.fordham.toolbelt.domain.model.InvoiceId(randomUUID())
        draftRepository.saveDraft(
            DraftInvoice(
                clientName = client.name,
                clientAddress = client.address,
                saveToClientDirectory = false
            )
        )
        return ToolExecutionResult.DraftInvoiceCreated(
            invoiceId = invoiceId,
            clientId = client.id
        )
    }

    private suspend fun executeSendInvoiceApproval(arguments: SendInvoiceApprovalArgs): ToolExecutionResult {
        val invoice = invoiceRepository.getInvoiceById(arguments.invoiceId)
            ?: return ToolExecutionResult.Failure(
                toolName = ToolName.SendInvoiceForApproval,
                error = FailureMessage("Invoice was not found.")
            )
        return ToolExecutionResult.InvoiceApprovalQueued(invoice.id)
    }

    private suspend fun executeDeleteInvoiceApproval(arguments: DeleteInvoiceApprovalArgs): ToolExecutionResult {
        val invoice = invoiceRepository.getInvoiceById(arguments.invoiceId)
            ?: return ToolExecutionResult.Failure(
                toolName = ToolName.DeleteInvoiceForApproval,
                error = FailureMessage("Invoice was not found.")
            )
        return when (val outcome = invoiceRepository.deleteInvoice(invoice)) {
            is InvoiceOutcome.Success -> ToolExecutionResult.InvoiceDeletionQueued(invoice.id)
            is InvoiceOutcome.Failure -> ToolExecutionResult.Failure(
                toolName = ToolName.DeleteInvoiceForApproval,
                error = outcome.error
            )
        }
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\RoomClientRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.ClientDao
import com.fordham.toolbelt.data.toDomain
import com.fordham.toolbelt.data.toEntity
import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ClientOutcome
import com.fordham.toolbelt.domain.model.ClientListOutcome
import com.fordham.toolbelt.domain.repository.ClientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch

class RoomClientRepository(
    private val clientDao: ClientDao
) : ClientRepository {
    override fun getAllClients(): Flow<ClientListOutcome> =
        clientDao.getAllClients()
            .map { list -> ClientListOutcome.Success(list.map { it.toDomain() }) as ClientListOutcome }
            .catch { emit(ClientListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to list clients"))) }
    
    override suspend fun searchClients(query: String): List<Client> =
        clientDao.searchClients(query).map { it.toDomain() }

    override suspend fun insertClient(client: Client): ClientOutcome = try {
        clientDao.insertClient(client.toEntity())
        ClientOutcome.Success
    } catch (e: Exception) {
        ClientOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert client"))
    }

    override suspend fun deleteClient(client: Client): ClientOutcome = try {
        clientDao.deleteClient(client.toEntity())
        ClientOutcome.Success
    } catch (e: Exception) {
        ClientOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to delete client"))
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\RoomDraftRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.DraftDao
import com.fordham.toolbelt.data.DraftInvoiceEntity
import com.fordham.toolbelt.data.dto.LineItemDto
import com.fordham.toolbelt.domain.model.DraftInvoice
import com.fordham.toolbelt.domain.repository.DraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomDraftRepository(
    private val draftDao: DraftDao
) : DraftRepository {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override fun getDraft(): Flow<DraftInvoice> {
        return draftDao.getDraft().map { entity ->
            val e = entity ?: DraftInvoiceEntity()
            DraftInvoice(
                clientName = e.clientName,
                clientAddress = e.clientAddress,
                taxRate = e.taxRate,
                deposit = e.deposit,
                hourlyRate = e.hourlyRate,
                logoUri = e.logoUri,
                selectedCategory = e.selectedCategory,
                itemDesc = e.itemDesc,
                itemAmt = e.itemAmt,
                elapsedSeconds = e.elapsedSeconds,
                startTime = e.startTime,
                timerRunning = e.timerRunning,
                saveToClientDirectory = e.saveToClientDirectory,
                lineItems = try {
                    json.decodeFromString<List<LineItemDto>>(e.lineItemsJson).map { it.toDomain() }
                } catch (t: Throwable) {
                    emptyList()
                },
                capturedPhotos = try {
                    json.decodeFromString<List<String>>(e.capturedPhotosJson)
                } catch (t: Throwable) {
                    emptyList()
                },
                linkedReceiptIds = try {
                    json.decodeFromString<List<String>>(e.linkedReceiptIdsJson)
                } catch (t: Throwable) {
                    emptyList()
                }
            )
        }
    }

    override suspend fun saveDraft(draft: DraftInvoice) {
        val entity = DraftInvoiceEntity(
            clientName = draft.clientName,
            clientAddress = draft.clientAddress,
            taxRate = draft.taxRate,
            deposit = draft.deposit,
            hourlyRate = draft.hourlyRate,
            logoUri = draft.logoUri,
            selectedCategory = draft.selectedCategory,
            itemDesc = draft.itemDesc,
            itemAmt = draft.itemAmt,
            elapsedSeconds = draft.elapsedSeconds,
            startTime = draft.startTime,
            timerRunning = draft.timerRunning,
            saveToClientDirectory = draft.saveToClientDirectory,
            lineItemsJson = json.encodeToString(draft.lineItems.map { LineItemDto.fromDomain(it) }),
            capturedPhotosJson = json.encodeToString(draft.capturedPhotos),
            linkedReceiptIdsJson = json.encodeToString(draft.linkedReceiptIds)
        )
        draftDao.saveDraft(entity)
    }

    override suspend fun clearDraft() {
        draftDao.clearDraft()
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\RoomInvoiceRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.InvoiceDao
import com.fordham.toolbelt.data.toDomain
import com.fordham.toolbelt.data.toEntity
import com.fordham.toolbelt.domain.model.InvoiceOutcome
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomInvoiceRepository(
    private val invoiceDao: InvoiceDao
) : InvoiceRepository {
    override val allInvoices: Flow<List<Invoice>> =
        invoiceDao.getAllInvoices().map { list -> list.map { it.toDomain() } }

    override suspend fun insertInvoice(invoice: Invoice): InvoiceOutcome = try {
        invoiceDao.insertInvoice(invoice.toEntity())
        InvoiceOutcome.Success
    } catch (e: Exception) {
        InvoiceOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert invoice"))
    }
    
    override suspend fun insertInvoices(invoices: List<Invoice>): InvoiceOutcome = try {
        invoiceDao.insertInvoices(invoices.map { it.toEntity() })
        InvoiceOutcome.Success
    } catch (e: Exception) {
        InvoiceOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert invoices"))
    }

    override suspend fun updateInvoice(invoice: Invoice): InvoiceOutcome = try {
        invoiceDao.updateInvoice(invoice.toEntity())
        InvoiceOutcome.Success
    } catch (e: Exception) {
        InvoiceOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to update invoice"))
    }

    override suspend fun deleteInvoice(invoice: Invoice): InvoiceOutcome = try {
        invoiceDao.deleteInvoice(invoice.toEntity())
        InvoiceOutcome.Success
    } catch (e: Exception) {
        InvoiceOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to delete invoice"))
    }

    override suspend fun deleteAllInvoices(): InvoiceOutcome = try {
        invoiceDao.deleteAllInvoices()
        InvoiceOutcome.Success
    } catch (e: Exception) {
        InvoiceOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed bulk database purge transaction"))
    }

    override suspend fun getInvoiceById(id: InvoiceId): Invoice? =
        invoiceDao.getInvoiceById(id.value)?.toDomain()

    override fun getInvoicesByClient(clientName: String): Flow<List<Invoice>> =
        invoiceDao.getInvoicesByClient(clientName).map { list -> list.map { it.toDomain() } }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\RoomJobNoteRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.JobNoteDao
import com.fordham.toolbelt.data.toDomain
import com.fordham.toolbelt.data.toEntity
import com.fordham.toolbelt.domain.model.JobNote
import com.fordham.toolbelt.domain.model.JobNoteOutcome
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.repository.JobNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomJobNoteRepository(
    private val jobNoteDao: JobNoteDao
) : JobNoteRepository {
    override fun getNotesByInvoice(invoiceId: InvoiceId): Flow<List<JobNote>> =
        jobNoteDao.getNotesByInvoice(invoiceId.value).map { list -> list.map { it.toDomain() } }

    override fun getNotesByClient(clientName: String): Flow<List<JobNote>> =
        jobNoteDao.getNotesByClient(clientName).map { list -> list.map { it.toDomain() } }

    override suspend fun insertNote(note: JobNote): JobNoteOutcome = try {
        jobNoteDao.insertNote(note.toEntity())
        JobNoteOutcome.Success
    } catch (e: Exception) {
        JobNoteOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert note"))
    }

    override suspend fun deleteNote(note: JobNote): JobNoteOutcome = try {
        jobNoteDao.deleteNote(note.toEntity())
        JobNoteOutcome.Success
    } catch (e: Exception) {
        JobNoteOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to delete note"))
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\RoomPhotoRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.PhotoDao
import com.fordham.toolbelt.data.toDomain
import com.fordham.toolbelt.data.toEntity
import com.fordham.toolbelt.domain.model.JobPhoto
import com.fordham.toolbelt.domain.model.PhotoOutcome
import com.fordham.toolbelt.domain.model.PhotoListOutcome
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPhotoRepository(
    private val photoDao: PhotoDao
) : PhotoRepository {
    override fun observePhotosForInvoice(invoiceId: InvoiceId): Flow<List<JobPhoto>> =
        photoDao.getPhotosForInvoice(invoiceId.value).map { list -> list.map { it.toDomain() } }

    override suspend fun getPhotosForInvoiceOnce(invoiceId: InvoiceId): PhotoListOutcome = try {
        val photos = photoDao.getPhotosForInvoiceOnce(invoiceId.value).map { it.toDomain() }
        PhotoListOutcome.Success(photos)
    } catch (e: Exception) {
        PhotoListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to retrieve photos once"))
    }

    override suspend fun savePhoto(photo: JobPhoto): PhotoOutcome = try {
        photoDao.insertPhoto(photo.toEntity())
        PhotoOutcome.Success
    } catch (e: Exception) {
        PhotoOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save photo"))
    }

    override suspend fun deletePhoto(photo: JobPhoto): PhotoOutcome = try {
        photoDao.deletePhoto(photo.toEntity())
        PhotoOutcome.Success
    } catch (e: Exception) {
        PhotoOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to delete photo"))
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\RoomReceiptRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.ReceiptDao
import com.fordham.toolbelt.data.toDomain
import com.fordham.toolbelt.data.toEntity
import com.fordham.toolbelt.domain.model.ReceiptOutcome
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch

class RoomReceiptRepository(
    private val receiptDao: ReceiptDao
) : ReceiptRepository {
    override val allItems: Flow<ReceiptListOutcome> = 
        receiptDao.getAllItems()
            .map { list -> ReceiptListOutcome.Success(list.map { it.toDomain() }) as ReceiptListOutcome }
            .catch { emit(ReceiptListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to list receipts"))) }

    override suspend fun insertItem(item: ReceiptItem): ReceiptOutcome = try { 
        receiptDao.insertItems(listOf(item.toEntity()))
        ReceiptOutcome.Success
    } catch (e: Exception) {
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert receipt"))
    }

    override suspend fun insertItems(items: List<ReceiptItem>): ReceiptOutcome = try {
        receiptDao.insertItems(items.map { it.toEntity() })
        ReceiptOutcome.Success
    } catch (e: Exception) {
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to insert receipts"))
    }

    override suspend fun deleteItem(item: ReceiptItem): ReceiptOutcome = try { 
        receiptDao.deleteItem(item.toEntity())
        ReceiptOutcome.Success
    } catch (e: Exception) {
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to delete receipt"))
    }

    override suspend fun deleteAllItems(): ReceiptOutcome = try {
        receiptDao.deleteAllItems()
        ReceiptOutcome.Success
    } catch (e: Exception) {
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to purge receipts"))
    }

    override fun getItemsByClient(clientName: String): Flow<ReceiptListOutcome> = 
        receiptDao.getItemsByClient(clientName)
            .map { list -> ReceiptListOutcome.Success(list.map { it.toDomain() }) as ReceiptListOutcome }
            .catch { emit(ReceiptListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to retrieve receipts by client"))) }

    override fun getUnassignedReceipts(): Flow<ReceiptListOutcome> =
        receiptDao.getUnassignedReceipts()
            .map { list -> ReceiptListOutcome.Success(list.map { it.toDomain() }) as ReceiptListOutcome }
            .catch { emit(ReceiptListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to retrieve unassigned receipts"))) }

    override suspend fun updateItem(item: ReceiptItem): ReceiptOutcome = try {
        receiptDao.updateItem(item.toEntity())
        ReceiptOutcome.Success
    } catch (e: Exception) {
        ReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to update receipt"))
    }
}
```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\implementation\SupplierRepositoryImpl.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.*
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*

class SupplierRepositoryImpl(
    private val supplierDao: SupplierDao,
    private val receiptDao: ReceiptDao
) : SupplierRepository {

    override fun getVisibleSuppliers(): Flow<SupplierListOutcome> {
        return combine(
            supplierDao.getVisibleSuppliers(),
            receiptDao.getAllItems()
        ) { entities, receipts ->
            val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
            
            val models = entities.map { entity ->
                val supplierReceipts = receipts.filter { 
                    val isSameSupplier = it.supplierId == entity.id || 
                        (it.supplierName.isNotBlank() && it.supplierName.equals(entity.name, ignoreCase = true))
                    
                    val receiptYear = Instant.fromEpochMilliseconds(it.lastUpdated)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).year
                    
                    isSameSupplier && receiptYear == currentYear
                }
                
                val analytics = SupplierAnalytics(
                    totalSpendYtd = supplierReceipts.sumOf { it.totalPrice },
                    jobsLinked = supplierReceipts.map { it.clientName }.distinct().size,
                    avgMarkup = 0.0
                )
                entity.toDomain().copy(analytics = analytics)
            }
            SupplierListOutcome.Success(models) as SupplierListOutcome
        }.catch { emit(SupplierListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to retrieve visible suppliers"))) }
    }

    override suspend fun logPurchase(supplierId: SupplierId, amount: MoneyAmount): SupplierOutcome = try {
        val receipt = ReceiptEntity(
            description = "Supplier Purchase",
            quantity = 1.0,
            unitPrice = amount.value,
            totalPrice = amount.value,
            supplierId = supplierId.value,
            lastUpdated = Clock.System.now().toEpochMilliseconds()
        )
        receiptDao.insertItems(listOf(receipt))
        SupplierOutcome.Success
    } catch (e: Exception) {
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to log purchase"))
    }

    override fun getHiddenSuppliers(): Flow<SupplierListOutcome> {
        return supplierDao.getHiddenSuppliers()
            .map { entities -> SupplierListOutcome.Success(entities.map { it.toDomain() }) as SupplierListOutcome }
            .catch { emit(SupplierListOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(it.message ?: "Failed to retrieve hidden suppliers"))) }
    }

    override suspend fun upsertSupplier(supplier: Supplier): SupplierOutcome = try {
        supplierDao.insertSupplier(supplier.toEntity())
        SupplierOutcome.Success
    } catch (e: Exception) {
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save supplier"))
    }

    override suspend fun hideSupplier(id: SupplierId): SupplierOutcome = try {
        supplierDao.hideSupplier(id.value)
        SupplierOutcome.Success
    } catch (e: Exception) {
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to hide supplier"))
    }

    override suspend fun restoreSupplier(id: SupplierId): SupplierOutcome = try {
        supplierDao.restoreSupplier(id.value)
        SupplierOutcome.Success
    } catch (e: Exception) {
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to restore supplier"))
    }

    override suspend fun updateOrder(id: SupplierId, newOrder: Int): SupplierOutcome = try {
        supplierDao.updateOrder(id.value, newOrder)
        SupplierOutcome.Success
    } catch (e: Exception) {
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to update supplier order"))
    }

    override suspend fun togglePin(id: SupplierId, isPinned: Boolean): SupplierOutcome = try {
        val supplier = supplierDao.getVisibleSuppliers().first().find { it.id == id.value }
        supplier?.let {
            supplierDao.insertSupplier(it.copy(isPinned = isPinned))
        }
        SupplierOutcome.Success
    } catch (e: Exception) {
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to pin supplier"))
    }

    override suspend fun seedDefaultSuppliers(): SupplierOutcome = try {
        val suppliers = supplierDao.getVisibleSuppliers().first()
        if (suppliers.isEmpty()) {
            val defaults = listOf(
                SupplierEntity(
                    id = "home_depot",
                    name = "Home Depot",
                    category = SupplierCategory.GENERAL_SUPPLY.name,
                    packageName = "com.thehomedepot",
                    webUrl = "https://www.homedepot.com",
                    displayOrder = 0,
                    logoResName = "logo_home_depot",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "lowes",
                    name = "Lowe's",
                    category = SupplierCategory.GENERAL_SUPPLY.name,
                    packageName = "com.lowes.android",
                    webUrl = "https://www.lowes.com",
                    displayOrder = 1,
                    logoResName = "logo_lowes",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "ace",
                    name = "Ace Hardware",
                    category = SupplierCategory.HARDWARE.name,
                    packageName = "com.acehardware.rewards",
                    webUrl = "https://www.acehardware.com",
                    displayOrder = 2,
                    logoResName = "logo_ace",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "menards",
                    name = "Menards",
                    category = SupplierCategory.GENERAL_SUPPLY.name,
                    packageName = "com.menards.mobile",
                    webUrl = "https://www.menards.com",
                    displayOrder = 3,
                    logoResName = "logo_menards",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "ferguson",
                    name = "Ferguson",
                    category = SupplierCategory.PLUMBING.name,
                    packageName = "com.ferguson.mobile",
                    webUrl = "https://www.ferguson.com",
                    displayOrder = 4,
                    logoResName = "logo_ferguson",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "sherwin",
                    name = "Sherwin-Williams",
                    category = SupplierCategory.PAINT.name,
                    packageName = "com.sherwin.probuyplus",
                    webUrl = "https://www.sherwin-williams.com",
                    displayOrder = 5,
                    logoResName = "logo_sherwin",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "grainger",
                    name = "Grainger",
                    category = SupplierCategory.GENERAL_SUPPLY.name,
                    packageName = "com.grainger.graingerapp",
                    webUrl = "https://www.grainger.com",
                    displayOrder = 6,
                    logoResName = "logo_grainger",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "abc_supply",
                    name = "ABC Supply",
                    category = SupplierCategory.ROOFING.name,
                    packageName = "com.abcsupply.myabcsupply",
                    webUrl = "https://www.abcsupply.com",
                    displayOrder = 7,
                    logoResName = "logo_abc",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "graybar",
                    name = "Graybar",
                    category = SupplierCategory.ELECTRICAL.name,
                    packageName = "com.graybar.mobile",
                    webUrl = "https://www.graybar.com",
                    displayOrder = 8,
                    logoResName = "logo_graybar",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "siteone",
                    name = "SiteOne",
                    category = SupplierCategory.GENERAL_SUPPLY.name,
                    packageName = "com.siteone.mobile",
                    webUrl = "https://www.siteone.com",
                    displayOrder = 9,
                    logoResName = "logo_siteone",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "amazon_biz",
                    name = "Amazon Business",
                    category = SupplierCategory.GENERAL_SUPPLY.name,
                    packageName = "com.amazon.mShop.android.business.shopping",
                    webUrl = "https://www.amazon.com/business",
                    displayOrder = 10,
                    logoResName = "logo_amazon",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "northern_tool",
                    name = "Northern Tool",
                    category = SupplierCategory.HARDWARE.name,
                    packageName = "com.multiservice.ntca",
                    webUrl = "https://www.northerntool.com",
                    displayOrder = 11,
                    logoResName = "logo_northern",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "sunbelt",
                    name = "Sunbelt Rentals",
                    category = SupplierCategory.GENERAL_SUPPLY.name,
                    packageName = "com.sunbeltrentals.app",
                    webUrl = "https://www.sunbeltrentals.com",
                    displayOrder = 12,
                    logoResName = "logo_sunbelt",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "hilti",
                    name = "Hilti",
                    category = SupplierCategory.FASTENERS.name,
                    packageName = "com.hilti.mobile.hiltionline",
                    webUrl = "https://www.hilti.com",
                    displayOrder = 13,
                    logoResName = "logo_hilti",
                    isDefault = true
                ),
                SupplierEntity(
                    id = "mcmaster",
                    name = "McMaster-Carr",
                    category = SupplierCategory.FASTENERS.name,
                    packageName = "com.mcmaster.android",
                    webUrl = "https://www.mcmaster.com",
                    displayOrder = 14,
                    logoResName = "logo_mcmaster",
                    isDefault = true
                )
            )
            supplierDao.insertSuppliers(defaults)
        }
        SupplierOutcome.Success
    } catch (e: Exception) {
        SupplierOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to seed default suppliers"))
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\InvoiceDao.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY lastUpdated DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<InvoiceEntity>)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoices WHERE clientName = :clientName")
    fun getInvoicesByClient(clientName: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: String): InvoiceEntity?

    @Query("DELETE FROM invoices")
    suspend fun deleteAllInvoices()
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\InvoiceEntity.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String = randomUUID(),
    val clientName: String,
    val clientAddress: String,
    val clientPhone: String = "",
    val clientEmail: String = "",
    val date: String,
    val totalAmount: Double,
    val depositAmount: Double = 0.0,
    val itemsSummary: String,
    val pdfPath: String = "",
    val isPaid: Boolean = false,
    val isEstimate: Boolean = false,
    val lastUpdated: Long = DateTimeUtil.nowEpochMillis(),
    val durationSeconds: Long = 0L,
    val isSynced: Boolean = false
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\JobNoteDao.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JobNoteDao {
    @Query("SELECT * FROM job_notes WHERE invoiceId = :invoiceId ORDER BY timestamp DESC")
    fun getNotesByInvoice(invoiceId: String): Flow<List<JobNoteEntity>>

    @Query("SELECT * FROM job_notes WHERE clientName = :clientName ORDER BY timestamp DESC")
    fun getNotesByClient(clientName: String): Flow<List<JobNoteEntity>>

    @Query("""
        SELECT * FROM job_notes 
        WHERE text LIKE '%' || :query || '%'
        ORDER BY timestamp DESC LIMIT 5
    """)
    suspend fun getRelevantContext(query: String): List<JobNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: JobNoteEntity)

    @Delete
    suspend fun deleteNote(note: JobNoteEntity)
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\JobNoteEntity.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID

@Entity(tableName = "job_notes")
data class JobNoteEntity(
    @PrimaryKey val id: String = randomUUID(),
    val clientName: String = "",
    val invoiceId: String? = null,
    val text: String,
    val timestamp: Long = DateTimeUtil.nowEpochMillis(),
    val isSynced: Boolean = false
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\JobPhotoEntity.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.*
import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID

@Entity(
    tableName = "job_photos",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
data class JobPhotoEntity(
    @PrimaryKey val id: String = randomUUID(),
    val invoiceId: String,
    val localUri: String,
    val timestamp: Long = DateTimeUtil.nowEpochMillis()
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\Mappers.kt

```kt
package com.fordham.toolbelt.data

import com.fordham.toolbelt.domain.model.Client as DomainClient
import com.fordham.toolbelt.domain.model.Invoice as DomainInvoice
import com.fordham.toolbelt.domain.model.ReceiptItem as DomainReceiptItem
import com.fordham.toolbelt.domain.model.JobNote as DomainJobNote
import com.fordham.toolbelt.domain.model.JobPhoto as DomainJobPhoto
import com.fordham.toolbelt.domain.model.Supplier as DomainSupplier
import com.fordham.toolbelt.domain.model.*

fun SupplierEntity.toDomain(): DomainSupplier = DomainSupplier(
    id = SupplierId(id),
    name = name,
    category = try { SupplierCategory.valueOf(category) } catch (e: Exception) { SupplierCategory.OTHER },
    address = address,
    phone = PhoneNumber(phone),
    webUrl = webUrl,
    packageName = packageName,
    displayOrder = displayOrder,
    isPinned = isPinned,
    isHidden = isHidden,
    customLogoPath = customLogoPath,
    logoResName = logoResName,
    isDefault = isDefault,
    analytics = SupplierAnalytics()
)

fun DomainSupplier.toEntity(): SupplierEntity = SupplierEntity(
    id = id.value,
    name = name,
    category = category.name,
    address = address,
    phone = phone.value,
    webUrl = webUrl,
    packageName = packageName,
    displayOrder = displayOrder,
    isPinned = isPinned,
    isHidden = isHidden,
    customLogoPath = customLogoPath,
    logoResName = logoResName,
    isDefault = isDefault
)

fun ClientEntity.toDomain(): DomainClient = DomainClient(
    id = ClientId(id),
    name = name,
    email = EmailAddress(email),
    phone = PhoneNumber(phone),
    address = address,
    notes = notes,
    totalInvoiced = totalInvoiced,
    isFavorite = isFavorite
)

fun DomainClient.toEntity(): ClientEntity = ClientEntity(
    id = id.value,
    name = name,
    email = email.value,
    phone = phone.value,
    address = address,
    notes = notes,
    totalInvoiced = totalInvoiced,
    isFavorite = isFavorite
)

fun InvoiceEntity.toDomain(): DomainInvoice = DomainInvoice(
    id = InvoiceId(id),
    clientName = clientName,
    clientAddress = clientAddress,
    clientPhone = PhoneNumber(clientPhone),
    clientEmail = EmailAddress(clientEmail),
    date = date,
    totalAmount = totalAmount,
    depositAmount = depositAmount,
    itemsSummary = itemsSummary,
    pdfPath = pdfPath,
    isPaid = isPaid,
    isEstimate = isEstimate,
    lastUpdated = lastUpdated,
    durationSeconds = durationSeconds
)

fun DomainInvoice.toEntity(): InvoiceEntity = InvoiceEntity(
    id = id.value,
    clientName = clientName,
    clientAddress = clientAddress,
    clientPhone = clientPhone.value,
    clientEmail = clientEmail.value,
    date = date,
    totalAmount = totalAmount,
    depositAmount = depositAmount,
    itemsSummary = itemsSummary,
    pdfPath = pdfPath,
    isPaid = isPaid,
    isEstimate = isEstimate,
    lastUpdated = lastUpdated,
    durationSeconds = durationSeconds
)

fun ReceiptEntity.toDomain(): DomainReceiptItem = DomainReceiptItem(
    id = ReceiptId(id),
    description = description,
    quantity = quantity,
    unitPrice = unitPrice,
    totalPrice = totalPrice,
    category = "Other",
    clientName = clientName,
    imagePath = imagePath,
    isBilled = isBilled,
    lastUpdated = lastUpdated,
    supplierName = supplierName,
    linkedInvoiceId = linkedInvoiceId?.let { InvoiceId(it) }
)

fun DomainReceiptItem.toEntity(): ReceiptEntity = ReceiptEntity(
    id = id.value,
    description = description,
    quantity = quantity,
    unitPrice = unitPrice,
    totalPrice = totalPrice,
    clientName = clientName,
    imagePath = imagePath,
    isBilled = isBilled,
    lastUpdated = lastUpdated,
    supplierName = supplierName,
    linkedInvoiceId = linkedInvoiceId?.value
)

fun JobNoteEntity.toDomain(): DomainJobNote = DomainJobNote(
    id = NoteId(id),
    clientName = clientName,
    invoiceId = invoiceId?.let { InvoiceId(it) },
    text = text,
    timestamp = timestamp
)

fun DomainJobNote.toEntity(): JobNoteEntity = JobNoteEntity(
    id = id.value,
    clientName = clientName,
    invoiceId = invoiceId?.value,
    text = text,
    timestamp = timestamp
)

fun JobPhotoEntity.toDomain(): DomainJobPhoto = DomainJobPhoto(
    id = PhotoId(id),
    invoiceId = InvoiceId(invoiceId),
    localUri = localUri,
    timestamp = timestamp
)

fun DomainJobPhoto.toEntity(): JobPhotoEntity = JobPhotoEntity(
    id = id.value,
    invoiceId = invoiceId.value,
    localUri = localUri,
    timestamp = timestamp
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\PhotoDao.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Query("SELECT * FROM job_photos WHERE invoiceId = :invoiceId")
    fun getPhotosForInvoice(invoiceId: String): Flow<List<JobPhotoEntity>>

    @Query("SELECT * FROM job_photos WHERE invoiceId = :invoiceId")
    suspend fun getPhotosForInvoiceOnce(invoiceId: String): List<JobPhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: JobPhotoEntity)

    @Delete
    suspend fun deletePhoto(photo: JobPhotoEntity)
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\ReceiptDao.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipt_items")
    fun getAllItems(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipt_items WHERE clientName = :clientName OR clientName = 'General'")
    fun getItemsByClient(clientName: String): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipt_items WHERE (clientName = '' OR clientName = 'General') AND linkedInvoiceId IS NULL")
    fun getUnassignedReceipts(): Flow<List<ReceiptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ReceiptEntity>)

    @Update
    suspend fun updateItem(item: ReceiptEntity)

    @Delete
    suspend fun deleteItem(item: ReceiptEntity)

    @Query("DELETE FROM receipt_items")
    suspend fun deleteAllItems()
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\ReceiptEntity.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID

@Entity(tableName = "receipt_items")
data class ReceiptEntity(
    @PrimaryKey val id: String = randomUUID(),
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalPrice: Double,
    val clientName: String = "",
    val imagePath: String = "",
    val isBilled: Boolean = false,
    val linkedInvoiceId: String? = null,
    val lastUpdated: Long = DateTimeUtil.nowEpochMillis(),
    val isSynced: Boolean = false,
    val supplierName: String = "",
    val supplierId: String? = null
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\remote\GeminiDtos.kt

```kt
package com.fordham.toolbelt.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = "user"
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@Serializable
data class GeminiInlineData(
    val mimeType: String,
    val data: String // Base64
)

@Serializable
data class GeminiGenerationConfig(
    val responseMimeType: String? = null
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent
)

@Serializable
data class AiReceiptItemDto(
    val description: String,
    val totalPrice: Double
)

@Serializable
data class AiReceiptResponse(
    val items: List<AiReceiptItemDto>
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\remote\PowerPayClient.kt

```kt
package com.fordham.toolbelt.data.remote

import com.fordham.toolbelt.domain.model.FailureMessage
import kotlinx.serialization.Serializable

data class PowerPayConfig(
    val baseUrl: String,
    val apiKey: String? = null
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank()
}

interface PowerPayClient {
    suspend fun createInvoicePayment(request: PowerPayCreatePaymentRequestDto): PowerPayClientOutcome<PowerPayPaymentResponseDto>
    suspend fun getTransactionHistory(): PowerPayClientOutcome<List<PowerPayPaymentResponseDto>>
}

sealed interface PowerPayClientOutcome<out T> {
    data class Success<T>(val value: T) : PowerPayClientOutcome<T>
    data class Failure(val error: FailureMessage) : PowerPayClientOutcome<Nothing>
}

@Serializable
data class PowerPayCreatePaymentRequestDto(
    val invoiceId: String,
    val clientName: String,
    val amountUsd: Double,
    val requestType: String,
    val provider: String,
    val description: String,
    val appSource: String = "invoice_hammer"
)

@Serializable
data class PowerPayPaymentResponseDto(
    val paymentId: String,
    val invoiceId: String,
    val clientName: String,
    val amountUsd: Double,
    val requestType: String,
    val provider: String,
    val status: String,
    val paymentLinkUrl: String,
    val assetCode: String,
    val transactionHash: String? = null,
    val createdAtMillis: Long
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\remote\PowerPayDtos.kt

```kt
package com.fordham.toolbelt.data.remote

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// PowerPay API DTOs — data layer ONLY. Never exposed to domain or UI.
// ---------------------------------------------------------------------------

@Serializable
data class CreatePaymentRequestDto(
    val appId: String,
    val invoiceId: String,
    val contractorUserId: String,
    val clientName: String,
    val amountUsd: Double,
    val depositAmountUsd: Double? = null,
    val description: String,
    val isTestnet: Boolean = true
)

@Serializable
data class CreatePaymentResponseDto(
    val paymentId: String,
    val status: String,
    val paymentLinkUrl: String? = null,
    val qrCodeUrl: String? = null
)

@Serializable
data class PaymentStatusResponseDto(
    val paymentId: String,
    val invoiceId: String,
    val status: String,
    val amountUsd: Double? = null,
    val amountXlm: String? = null,
    val txHash: String? = null,
    val explorerUrl: String? = null,
    val milestones: List<MilestoneDto> = emptyList()
)

@Serializable
data class MilestoneDto(
    val id: String,
    val paymentId: String,
    val description: String,
    val amountUsd: Double,
    val isReleased: Boolean,
    val releasedAt: String? = null
)

@Serializable
data class ReleaseMilestoneRequestDto(
    val paymentId: String,
    val milestoneId: String
)

@Serializable
data class ReleaseMilestoneResponseDto(
    val success: Boolean,
    val txHash: String? = null,
    val explorerUrl: String? = null
)

@Serializable
data class BalanceResponseDto(
    val accountId: String,
    val xlmBalance: String,
    val usdEstimate: String,
    val isActive: Boolean
)

@Serializable
data class TransactionListResponseDto(
    val transactions: List<TransactionDto>
)

@Serializable
data class TransactionDto(
    val id: String,
    val invoiceId: String,
    val clientName: String,
    val amountUsd: Double? = null,
    val amountXlm: String? = null,
    val status: String,
    val txHash: String? = null,
    val explorerUrl: String? = null,
    val createdAt: String,
    val type: String = "UNKNOWN"
)

@Serializable
data class RequestDepositRequestDto(
    val paymentId: String,
    val depositAmountUsd: Double
)

@Serializable
data class PowerPayErrorDto(
    val code: String,
    val message: String
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\SupplierDao.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE isHidden = 0 ORDER BY isPinned DESC, displayOrder ASC")
    fun getVisibleSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE isHidden = 1")
    fun getHiddenSuppliers(): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<SupplierEntity>)

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("UPDATE suppliers SET isHidden = 1 WHERE id = :id")
    suspend fun hideSupplier(id: String)

    @Query("UPDATE suppliers SET isHidden = 0 WHERE id = :id")
    suspend fun restoreSupplier(id: String)

    @Query("UPDATE suppliers SET displayOrder = :newOrder WHERE id = :id")
    suspend fun updateOrder(id: String, newOrder: Int)
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\data\SupplierEntity.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fordham.toolbelt.util.randomUUID

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: String = randomUUID(),
    val name: String,
    val category: String,
    val address: String = "",
    val phone: String = "",
    val webUrl: String = "",
    val packageName: String = "",
    val displayOrder: Int,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val customLogoPath: String? = null,
    val logoResName: String? = null,
    val isDefault: Boolean = false,
    val totalSpendYtd: Double = 0.0,
    val jobsLinked: Int = 0,
    val avgMarkup: Double = 0.0
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\di\AppModule.kt

```kt
package com.fordham.toolbelt.di

import com.fordham.toolbelt.data.*
import com.fordham.toolbelt.data.implementation.*
import com.fordham.toolbelt.data.remote.PowerPayClient
import com.fordham.toolbelt.data.remote.PowerPayConfig
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.domain.usecase.*
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

fun initKoin(
    additionalModules: List<Module> = emptyList(),
    appDeclaration: KoinAppDeclaration = {}
) = startKoin {
    appDeclaration()
    modules(
        platformModule(),
        dataModule,
        useCaseModule,
        *additionalModules.toTypedArray()
    )
}

val dataModule = module {
    // Network
    single { HttpClient() }

    // Database & DAOs
    single { getRoomDatabase(get()) }
    single { get<AppDatabase>().clientDao() }
    single { get<AppDatabase>().invoiceDao() }
    single { get<AppDatabase>().receiptDao() }
    single { get<AppDatabase>().supplierDao() }
    single { get<AppDatabase>().photoDao() }
    single { get<AppDatabase>().jobNoteDao() }
    single { get<AppDatabase>().draftDao() }

    // Repositories
    single<ClientRepository> { RoomClientRepository(get()) }
    single<InvoiceRepository> { RoomInvoiceRepository(get()) }
    single<ReceiptRepository> { RoomReceiptRepository(get()) }
    single<JobNoteRepository> { RoomJobNoteRepository(get()) }
    single<SupplierRepository> { SupplierRepositoryImpl(get(), get()) }
    single<PhotoRepository> { RoomPhotoRepository(get()) }
    single<DraftRepository> { RoomDraftRepository(get()) }
    single<OcrRepository> { GeminiOcrRepository(get()) }
    single<GeminiRepository> { KtorGeminiRepository(get(), get(), get(), get(), get()) }
    single<AgentLlmGateway> { GeminiAgentLlmGateway(get(), get()) }
    single<ToolRegistry> { RepositoryToolRegistry(get(), get(), get(), get()) }
    single { PowerPayConfig(baseUrl = "") }
    single<PowerPayClient> {
        val config = get<PowerPayConfig>()
        if (config.isConfigured) {
            KtorPowerPayClient(get(), config)
        } else {
            MockPowerPayClient()
        }
    }
    single<PaymentRepository> { PowerPayPaymentRepository(get()) }
    single<ForemanAgentDispatchers> {
        object : ForemanAgentDispatchers {
            override val background = Dispatchers.Default
        }
    }
    single<SyncRepository> { KtorSyncRepository(get(), get(), get(), get(), get(), get(), get()) }
}

val useCaseModule = module {
    factory { AddSupplierUseCase(get()) }
    factory { AddJobNoteUseCase(get()) }
    factory { DeleteJobNoteUseCase(get()) }
    factory { DeleteClientUseCase(get()) }
    factory { LinkReceiptToClientUseCase(get()) }
    factory { SaveJobPhotoUseCase(get()) }
    factory { ProcessInvoiceAiUseCase(get()) }
    factory { BillLaborUseCase(get()) }
    factory { GenerateAndSaveInvoiceUseCase(get(), get(), get(), get(), get(), get()) }
    factory { GenerateSummaryUseCase(get(), get()) }
    factory { GenerateTaxReportUseCase(get(), get(), get(), get()) }
    factory { CreatePaymentRequestUseCase(get()) }
    factory { GetPaymentLedgerUseCase(get()) }
    factory { GetBusinessStatsUseCase(get(), get()) }
    factory { GetClientFinancialSummaryUseCase(get(), get()) }
    factory { GetHiddenSuppliersUseCase(get()) }
    factory { GetSuppliersUseCase(get()) }
    factory { GlobalAiAgentUseCase(get(), get()) }
    factory { RunForemanAgentUseCase(get(), get(), get()) }
    factory { ForemanOrchestrator(get(), get(), get(), get()) }
    factory { HideSupplierUseCase(get()) }
    factory { LogSupplierPurchaseUseCase(get()) }
    factory { ProcessReceiptUseCase(get(), get(), get()) }
    factory { RestoreSupplierUseCase(get()) }
    factory { SaveInvoiceUseCase(get()) }
    factory { SeedSuppliersUseCase(get()) }
    factory { ToggleSupplierPinUseCase(get()) }
    factory { UpdateSupplierOrderUseCase(get()) }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\di\PlatformModule.kt

```kt
package com.fordham.toolbelt.di

import org.koin.core.module.Module

expect fun platformModule(): Module

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\agent\AgentFunction.kt

```kt
package com.fordham.toolbelt.domain.model.agent

sealed interface ParameterType {
    data object Text : ParameterType
    data object Integer : ParameterType
    data object Boolean : ParameterType
}

data class FunctionParameter(
    val name: ParameterName,
    val type: ParameterType,
    val required: Boolean
)

data class AgentFunction(
    val toolName: ToolName,
    val description: ToolDescription,
    val parameters: List<FunctionParameter>
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\agent\AgentOutcome.kt

```kt
package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.FailureMessage

sealed interface AgentOutcome {
    data class TextResponse(
        val response: NaturalLanguage
    ) : AgentOutcome

    data class ToolExecutionRequested(
        val toolCallId: ToolCallId,
        val toolName: ToolName,
        val arguments: ToolArguments
    ) : AgentOutcome

    data class RequiresApproval(
        val toolCallId: ToolCallId,
        val toolName: ToolName,
        val arguments: ToolArguments
    ) : AgentOutcome

    data class ToolExecuted(
        val toolCallId: ToolCallId,
        val result: ToolExecutionResult
    ) : AgentOutcome

    data class Failure(
        val error: FailureMessage
    ) : AgentOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\agent\AgentTypes.kt

```kt
package com.fordham.toolbelt.domain.model.agent

import kotlin.jvm.JvmInline

@JvmInline
value class SessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "SessionId cannot be blank." }
    }
}

@JvmInline
value class NaturalLanguage(val value: String)

@JvmInline
value class ToolCallId(val value: String) {
    init {
        require(value.isNotBlank()) { "ToolCallId cannot be blank." }
    }
}

@JvmInline
value class ParameterName(val value: String) {
    init {
        require(value.matches(IDENTIFIER_PATTERN)) {
            "ParameterName must be a stable identifier."
        }
    }

    private companion object {
        val IDENTIFIER_PATTERN = Regex("[A-Za-z][A-Za-z0-9_]*")
    }
}

@JvmInline
value class ToolDescription(val value: String) {
    init {
        require(value.isNotBlank()) { "ToolDescription cannot be blank." }
    }
}

@JvmInline
value class TimestampMillis(val value: Long) {
    init {
        require(value >= 0L) { "TimestampMillis cannot be negative." }
    }
}

@JvmInline
value class CurrencyAmountCents(val value: Long) {
    init {
        require(value >= 0L) { "CurrencyAmountCents cannot be negative." }
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\agent\ForemanSession.kt

```kt
package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.InvoiceId

sealed interface AgentRole {
    data object User : AgentRole
    data object Foreman : AgentRole
    data object ToolSystem : AgentRole
}

data class ForemanTurn(
    val role: AgentRole,
    val content: NaturalLanguage,
    val timestamp: TimestampMillis,
    val toolCallId: ToolCallId? = null,
    val toolName: ToolName? = null
)

data class ForemanSession(
    val sessionId: SessionId,
    val history: List<ForemanTurn>,
    val activeClient: ClientId?,
    val activeDraftInvoice: InvoiceId?,
    val resolvedEntities: ResolvedEntities
) {
    fun append(turn: ForemanTurn): ForemanSession {
        return copy(history = history + turn)
    }

    companion object {
        fun empty(sessionId: SessionId): ForemanSession {
            return ForemanSession(
                sessionId = sessionId,
                history = emptyList(),
                activeClient = null,
                activeDraftInvoice = null,
                resolvedEntities = ResolvedEntities.empty()
            )
        }
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\agent\ResolvedEntities.kt

```kt
package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.ReceiptId

sealed interface ResolvedEntityId

data class ResolvedClient(
    val id: ClientId
) : ResolvedEntityId

data class ResolvedInvoice(
    val id: InvoiceId
) : ResolvedEntityId

data class ResolvedReceipt(
    val id: ReceiptId
) : ResolvedEntityId

data class ResolvedEntityAlias(
    val alias: NaturalLanguage,
    val entity: ResolvedEntityId
)

class ResolvedEntities private constructor(
    private val aliases: Map<NaturalLanguage, ResolvedEntityId>
) {
    fun resolve(alias: NaturalLanguage): ResolvedEntityId? {
        return aliases[alias]
    }

    fun remember(alias: NaturalLanguage, entity: ResolvedEntityId): ResolvedEntities {
        return ResolvedEntities(aliases + (alias to entity))
    }

    fun forget(alias: NaturalLanguage): ResolvedEntities {
        return ResolvedEntities(aliases - alias)
    }

    fun entries(): List<ResolvedEntityAlias> {
        return aliases.map { (alias, entity) ->
            ResolvedEntityAlias(alias = alias, entity = entity)
        }
    }

    companion object {
        fun empty(): ResolvedEntities = ResolvedEntities(emptyMap())
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\agent\ToolArguments.kt

```kt
package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.InvoiceId

sealed interface ToolArguments {
    val expectedToolName: ToolName
}

data class SearchClientsArgs(
    val query: NaturalLanguage
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.SearchClients
}

data class GetUnbilledReceiptsArgs(
    val clientId: ClientId
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.GetUnbilledReceipts
}

data class CreateDraftInvoiceArgs(
    val clientId: ClientId
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.CreateDraftInvoice
}

data class SendInvoiceApprovalArgs(
    val invoiceId: InvoiceId
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.SendInvoiceForApproval
}

data class DeleteInvoiceApprovalArgs(
    val invoiceId: InvoiceId
) : ToolArguments {
    override val expectedToolName: ToolName = ToolName.DeleteInvoiceForApproval
}

object ToolArgumentValidator {
    fun isCompatible(toolName: ToolName, arguments: ToolArguments): Boolean {
        return toolName == arguments.expectedToolName
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\agent\ToolExecutionResult.kt

```kt
package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.ReceiptId

data class ClientSearchHit(
    val clientId: ClientId,
    val displayName: NaturalLanguage
)

data class UnbilledReceiptSummary(
    val receiptId: ReceiptId,
    val supplierName: NaturalLanguage,
    val amount: CurrencyAmountCents
)

sealed interface ToolExecutionResult {
    val toolName: ToolName

    data class ClientSearchCompleted(
        val clients: List<ClientSearchHit>
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.SearchClients
    }

    data class UnbilledReceiptsFound(
        val clientId: ClientId,
        val receipts: List<UnbilledReceiptSummary>
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.GetUnbilledReceipts
    }

    data class DraftInvoiceCreated(
        val invoiceId: InvoiceId,
        val clientId: ClientId
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.CreateDraftInvoice
    }

    data class InvoiceApprovalQueued(
        val invoiceId: InvoiceId
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.SendInvoiceForApproval
    }

    data class InvoiceDeletionQueued(
        val invoiceId: InvoiceId
    ) : ToolExecutionResult {
        override val toolName: ToolName = ToolName.DeleteInvoiceForApproval
    }

    data class Failure(
        override val toolName: ToolName,
        val error: FailureMessage
    ) : ToolExecutionResult
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\agent\ToolName.kt

```kt
package com.fordham.toolbelt.domain.model.agent

sealed interface ToolName {
    data object SearchClients : ToolName
    data object GetUnbilledReceipts : ToolName
    data object CreateDraftInvoice : ToolName
    data object SendInvoiceForApproval : ToolName
    data object DeleteInvoiceForApproval : ToolName
}

enum class ToolSafety {
    Safe,
    RequiresApproval
}

object ForemanToolPolicy {
    fun safetyFor(toolName: ToolName): ToolSafety {
        return when (toolName) {
            ToolName.SearchClients,
            ToolName.GetUnbilledReceipts,
            ToolName.CreateDraftInvoice -> ToolSafety.Safe
            ToolName.SendInvoiceForApproval,
            ToolName.DeleteInvoiceForApproval -> ToolSafety.RequiresApproval
        }
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\AiAgentIntent.kt

```kt
package com.fordham.toolbelt.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AgentMode {
    ACTION,   // Mode 1: Executing a specific app command
    RESPONSE  // Mode 2: Conversational fallback / answering questions
}

@Serializable
sealed class AiAgentIntent {
    @Serializable
    data class DraftInvoice(val data: String?) : AiAgentIntent()
    @Serializable
    data class SummarizeClient(val clientName: String) : AiAgentIntent()
    @Serializable
    data class AnalyzeFinances(val period: String?) : AiAgentIntent()
    @Serializable
    data class FindJob(val query: String) : AiAgentIntent()
    @Serializable
    data class GeneralQuery(val query: String) : AiAgentIntent()
    @Serializable
    data object ScanReceipt : AiAgentIntent()
    @Serializable
    data object OpenStores : AiAgentIntent()
    @Serializable
    data object PremiumRequired : AiAgentIntent()
    @Serializable
    data object Unknown : AiAgentIntent()
}

@Serializable
data class AiAgentResponse(
    val mode: AgentMode,
    val summary: String,
    val actionTaken: String? = null,
    val suggestedIntent: AiAgentIntent? = null
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\AiInvoiceResult.kt

```kt
package com.fordham.toolbelt.domain.model

data class AiInvoiceResult(
    val clientName: String = "",
    val clientAddress: String = "",
    val items: List<LineItem> = emptyList()
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\AiOutcome.kt

```kt
package com.fordham.toolbelt.domain.model

sealed interface GeminiOutcome {
    data class Success(val text: String) : GeminiOutcome
    data class Failure(val error: FailureMessage) : GeminiOutcome
}

sealed interface AgentCommandOutcome {
    data class Success(val response: AiAgentResponse) : AgentCommandOutcome
    data class Failure(val error: FailureMessage) : AgentCommandOutcome
}

sealed interface ToolCallOutcome {
    data class Success(val toolCall: ForemanToolCall?) : ToolCallOutcome
    data class Failure(val error: FailureMessage) : ToolCallOutcome
}

sealed interface InvoiceTextOutcome {
    data class Success(val result: AiInvoiceResult) : InvoiceTextOutcome
    data class Failure(val error: FailureMessage) : InvoiceTextOutcome
}

sealed interface ReceiptImageOutcome {
    data class Success(val items: List<ReceiptItem>) : ReceiptImageOutcome
    data class Failure(val error: FailureMessage) : ReceiptImageOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\BentoReportData.kt

```kt
package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID

data class BentoReportData(
    val netProfit: Double,
    val grossIncome: Double,
    val expenses: Double,
    val invoices: List<Invoice>,
    val receiptCount: Int,
    val dateGeneratedMillis: Long = DateTimeUtil.nowEpochMillis(),
    val reportId: String = randomUUID(),
    val businessName: String = "Invoice Hammer"
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\BillLaborOutcome.kt

```kt
package com.fordham.toolbelt.domain.model

sealed interface BillLaborOutcome {
    data object Success : BillLaborOutcome
    data class Error(val exception: Throwable, val message: String? = null) : BillLaborOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\BusinessSettings.kt

```kt
package com.fordham.toolbelt.domain.model

data class BusinessSettings(
    val businessName: String = "",
    val businessSlogan: String = "",
    val businessPhone: String = "",
    val businessEmail: String = "",
    val businessAddress: String = "",
    val taxRate: Double = 0.0,
    val markupPercentage: Double = 0.0,
    val logoUri: String? = null,
    val isPremium: Boolean = false,
    val isDarkMode: Boolean = true,
    val useMetricUnits: Boolean = false,
    val notificationsEnabled: Boolean = true
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\BusinessStats.kt

```kt
package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil

data class BusinessStats(
    val netProfit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalDurationSeconds: Long = 0L,
    val unbilledExpenses: Double = 0.0,
    val projectStats: List<ProjectStat> = emptyList(),
    val profitMargin: Int = 0
) {
    val formattedNetProfit: String get() = DateTimeUtil.formatMoney(netProfit)
    val formattedUnbilledExpenses: String get() = DateTimeUtil.formatMoney(unbilledExpenses)
}

data class ProjectStat(
    val clientName: String,
    val revenue: Double,
    val expenses: Double,
    val profit: Double,
    val progress: Float
) {
    val formattedProfit: String get() = DateTimeUtil.formatMoney(profit)
    val formattedRevenue: String get() = "REVENUE: ${DateTimeUtil.formatMoney(revenue)}"
    val formattedExpenses: String get() = "COSTS: ${DateTimeUtil.formatMoney(expenses)}"
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\Client.kt

```kt
package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

data class Client(
    val id: ClientId = ClientId(randomUUID()),
    val name: String,
    val email: EmailAddress = EmailAddress(""),
    val phone: PhoneNumber = PhoneNumber(""),
    val address: String = "",
    val notes: String = "",
    val totalInvoiced: Double = 0.0,
    val isFavorite: Boolean = false,
    val lastUpdated: Long = Clock.System.now().toEpochMilliseconds()
)
```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\ClientOutcome.kt

```kt
package com.fordham.toolbelt.domain.model

sealed interface ClientOutcome {
    data object Success : ClientOutcome
    data class Failure(val error: FailureMessage) : ClientOutcome
}

sealed interface ClientListOutcome {
    data class Success(val clients: List<Client>) : ClientListOutcome
    data class Failure(val error: FailureMessage) : ClientListOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\DraftInvoice.kt

```kt
package com.fordham.toolbelt.domain.model

data class DraftInvoice(
    val clientName: String = "",
    val clientAddress: String = "",
    val taxRate: Double = 7.0,
    val deposit: Double = 0.0,
    val hourlyRate: Double = 50.0,
    val logoUri: String? = null,
    val selectedCategory: String = "Drywall",
    val itemDesc: String = "",
    val itemAmt: String = "",
    val elapsedSeconds: Long = 0L,
    val startTime: Long = 0L,
    val timerRunning: Boolean = false,
    val saveToClientDirectory: Boolean = false,
    val lineItems: List<LineItem> = emptyList(),
    val capturedPhotos: List<String> = emptyList(),
    val linkedReceiptIds: List<String> = emptyList()
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\Exceptions.kt

```kt
package com.fordham.toolbelt.domain.model

class PremiumRequiredException(
    message: String = "This feature requires a Pro Account."
) : Exception(message)

class NetworkUnavailableException(
    message: String = "No internet connection available."
) : Exception(message)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\ForemanAgent.kt

```kt
package com.fordham.toolbelt.domain.model

enum class ToolCategory {
    SAFE,       // Read-only or low-impact creation (e.g. search, open tab, draft)
    DESTRUCTIVE // Danger: deleting, clearing (always needs confirmation)
}

enum class ToolType(val category: ToolCategory) {
    SEARCH_CLIENTS(ToolCategory.SAFE),
    GET_CLIENT_DETAILS(ToolCategory.SAFE),
    CREATE_DRAFT_INVOICE(ToolCategory.SAFE),
    DELETE_INVOICE(ToolCategory.DESTRUCTIVE),
    ADD_JOB_NOTE(ToolCategory.SAFE),
    OPEN_TAB(ToolCategory.SAFE),
    SCAN_RECEIPT(ToolCategory.SAFE),
    SHOW_STATS(ToolCategory.SAFE),
    SYNC_CLOUD(ToolCategory.SAFE),
    UNKNOWN(ToolCategory.SAFE)
}

sealed interface ToolParameters {
    data class SearchClients(val query: String) : ToolParameters
    data class GetClientDetails(val clientName: String) : ToolParameters
    data class CreateDraftInvoice(val clientName: String, val amount: Double, val items: String) : ToolParameters
    data class DeleteInvoice(val invoiceId: InvoiceId) : ToolParameters
    data class AddJobNote(val clientName: String, val note: String) : ToolParameters
    data class OpenTab(val tabName: String) : ToolParameters
    data object None : ToolParameters
}

data class ForemanToolCall(
    val id: String,
    val type: ToolType,
    val parameters: ToolParameters,
    val reasoning: String = ""
)

sealed interface OrchestrationResult {
    data class Executed(val summary: String, val toolCall: ForemanToolCall) : OrchestrationResult
    data class ApprovalRequired(val pendingCall: ForemanToolCall) : OrchestrationResult
    data class Failure(val error: FailureMessage) : OrchestrationResult
    data class ResponseOnly(val text: String) : OrchestrationResult
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\Invoice.kt

```kt
package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil

data class Invoice(
    val id: InvoiceId,
    val clientName: String,
    val clientAddress: String,
    val clientPhone: PhoneNumber = PhoneNumber(""),
    val clientEmail: EmailAddress = EmailAddress(""),
    val date: String,
    val totalAmount: Double,
    val depositAmount: Double = 0.0,
    val itemsSummary: String,
    val pdfPath: String = "",
    val isPaid: Boolean = false,
    val isEstimate: Boolean = false,
    val lastUpdated: Long = DateTimeUtil.nowEpochMillis(),
    val durationSeconds: Long = 0L
) {
    val formattedTotal: String get() = DateTimeUtil.formatMoney(totalAmount)
    val formattedDeposit: String get() = "Deposit Paid: ${DateTimeUtil.formatMoney(depositAmount)}"
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\InvoiceData.kt

```kt
package com.fordham.toolbelt.domain.model

data class InvoiceData(
    val invoiceId: String,
    val clientName: String,
    val clientAddress: String,
    val items: List<LineItem>,
    val taxRate: Double,
    val date: String,
    val logoUriString: String?,
    val settings: BusinessSettings,
    val isEstimate: Boolean = false,
    val deposit: Double = 0.0,
    val photoUris: List<String> = emptyList()
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\InvoiceOutcome.kt

```kt
package com.fordham.toolbelt.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class FailureMessage(val value: String)

sealed interface InvoiceOutcome {
    data object Success : InvoiceOutcome
    data class Failure(val error: FailureMessage) : InvoiceOutcome
}

sealed interface SaveInvoiceOutcome {
    data class Success(val invoice: Invoice) : SaveInvoiceOutcome
    data class Error(val exception: Throwable, val message: String? = null) : SaveInvoiceOutcome
}

sealed interface GenerateInvoiceOutcome {
    data object Success : GenerateInvoiceOutcome
    data class Error(val exception: Throwable, val message: String? = null) : GenerateInvoiceOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\JobNote.kt

```kt
package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil

data class JobNote(
    val id: NoteId,
    val clientName: String = "",
    val invoiceId: InvoiceId? = null,
    val text: String,
    val timestamp: Long = DateTimeUtil.nowEpochMillis()
) {
    val formattedTime: String get() = DateTimeUtil.formatEpoch(timestamp)
    val formattedDate: String get() = DateTimeUtil.formatEpoch(timestamp)
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\JobNoteOutcome.kt

```kt
package com.fordham.toolbelt.domain.model

sealed interface JobNoteOutcome {
    data object Success : JobNoteOutcome
    data class Failure(val error: FailureMessage) : JobNoteOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\JobPhoto.kt

```kt
package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.randomUUID

data class JobPhoto(
    val id: PhotoId = PhotoId(randomUUID()),
    val invoiceId: InvoiceId,
    val localUri: String,
    val timestamp: Long = 0L
)
```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\LineItem.kt

```kt
package com.fordham.toolbelt.domain.model

data class LineItem(
    val description: String,
    val amount: Double,
    val category: String = "Service"
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\Payment.kt

```kt
package com.fordham.toolbelt.domain.model

// ---------------------------------------------------------------------------
// Payment domain models — no Android, no Ktor, no DTOs here.
// ---------------------------------------------------------------------------

enum class PaymentStatus {
    UNPAID,
    PENDING,
    DEPOSIT_PAID,
    MILESTONE_PAID,
    PAID_IN_FULL,
    FAILED,
    UNKNOWN
}

data class ContractorBalance(
    val xlmBalance: String,
    val usdEstimate: String,
    val accountId: String,
    val isActive: Boolean
)

data class PaymentTransaction(
    val id: String,
    val invoiceId: String,
    val clientName: String,
    val amountUsd: Double,
    val amountXlm: String?,
    val status: PaymentStatus,
    val txHash: String?,
    val explorerUrl: String?,
    val createdAt: String,
    val type: PaymentType
)

enum class PaymentType { DEPOSIT, MILESTONE, FULL, UNKNOWN }

data class PaymentRequest(
    val invoiceId: String,
    val contractorUserId: String,
    val clientName: String,
    val amountUsd: Double,
    val depositAmountUsd: Double?,
    val description: String,
    val isTestnet: Boolean = true
)

data class MilestonePayment(
    val id: String,
    val paymentId: String,
    val description: String,
    val amountUsd: Double,
    val isReleased: Boolean,
    val releasedAt: String?
)

data class ActivePayment(
    val paymentId: String,
    val invoiceId: String,
    val status: PaymentStatus,
    val paymentLinkUrl: String?,
    val qrCodeUrl: String?,
    val milestones: List<MilestonePayment>
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\PaymentModels.kt

```kt
package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil
import kotlinx.datetime.Clock
import kotlin.jvm.JvmInline

@JvmInline
value class PaymentRequestId(val value: String)

@JvmInline
value class PaymentLinkUrl(val value: String)

@JvmInline
value class StellarTransactionHash(val value: String)

enum class PaymentRequestType {
    Deposit,
    FullBalance
}

enum class PaymentProviderType {
    GooglePay,
    ApplePay,
    StellarUsdc,
    CardLink
}

enum class InvoicePaymentStatus {
    Requested,
    Pending,
    Paid,
    Failed,
    Expired
}

data class InvoicePaymentRequest(
    val id: PaymentRequestId,
    val invoiceId: InvoiceId,
    val invoiceClientName: String,
    val type: PaymentRequestType,
    val provider: PaymentProviderType,
    val requestedAmount: MoneyAmount,
    val status: InvoicePaymentStatus,
    val paymentLink: PaymentLinkUrl,
    val createdAtMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val paidAtMillis: Long? = null,
    val stellarTransactionHash: StellarTransactionHash? = null,
    val assetCode: String = "USDC"
) {
    val formattedAmount: String get() = DateTimeUtil.formatMoney(requestedAmount.value)
    val statusLabel: String get() = status.name.uppercase()
    val providerLabel: String get() = provider.label
}

val PaymentProviderType.label: String
    get() = when (this) {
        PaymentProviderType.GooglePay -> "Google Pay"
        PaymentProviderType.ApplePay -> "Apple Pay"
        PaymentProviderType.StellarUsdc -> "Stellar USDC"
        PaymentProviderType.CardLink -> "Card / Link"
    }

sealed interface PaymentRequestOutcome {
    data class Success(val request: InvoicePaymentRequest) : PaymentRequestOutcome
    data class Failure(val error: FailureMessage) : PaymentRequestOutcome
}

sealed interface PaymentLedgerOutcome {
    data class Success(val requests: List<InvoicePaymentRequest>) : PaymentLedgerOutcome
    data class Failure(val error: FailureMessage) : PaymentLedgerOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\PhotoOutcome.kt

```kt
package com.fordham.toolbelt.domain.model

sealed interface PhotoOutcome {
    data object Success : PhotoOutcome
    data class Failure(val error: FailureMessage) : PhotoOutcome
}

sealed interface PhotoListOutcome {
    data class Success(val photos: List<JobPhoto>) : PhotoListOutcome
    data class Failure(val error: FailureMessage) : PhotoListOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\ReceiptItem.kt

```kt
package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID

data class ReceiptItem(
    val id: ReceiptId = ReceiptId(randomUUID()),
    val description: String,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val category: String = "Other",
    val clientName: String = "",
    val imagePath: String = "",
    val isBilled: Boolean = false,
    val lastUpdated: Long = DateTimeUtil.nowEpochMillis(),
    val supplierName: String = "",
    val linkedInvoiceId: InvoiceId? = null
) {
    val formattedPrice: String get() = DateTimeUtil.formatMoney(totalPrice)
    val formattedDetails: String get() = "${if (quantity % 1.0 == 0.0) quantity.toInt() else quantity} @ ${DateTimeUtil.formatMoney(unitPrice)} | ${supplierName.ifEmpty { "General" }}"
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\ReceiptOutcome.kt

```kt
package com.fordham.toolbelt.domain.model

sealed interface ReceiptOutcome {
    data object Success : ReceiptOutcome
    data class Failure(val error: FailureMessage) : ReceiptOutcome
}

sealed interface ReceiptListOutcome {
    data class Success(val receipts: List<ReceiptItem>) : ReceiptListOutcome
    data class Failure(val error: FailureMessage) : ReceiptListOutcome
}

sealed interface ProcessReceiptOutcome {
    data class Success(val items: List<ReceiptItem>) : ProcessReceiptOutcome
    data class Failure(val error: FailureMessage) : ProcessReceiptOutcome
    data object PremiumRequired : ProcessReceiptOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\ServiceItem.kt

```kt
package com.fordham.toolbelt.domain.model

import com.fordham.toolbelt.util.DateTimeUtil

data class ServiceItem(
    val name: String,
    val price: Double
) {
    val formattedPrice: String get() = DateTimeUtil.formatMoney(price)
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\SettingsOutcome.kt

```kt
package com.fordham.toolbelt.domain.model

sealed interface SettingsOutcome {
    data object Success : SettingsOutcome
    data class Failure(val error: FailureMessage) : SettingsOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\StorageOutcome.kt

```kt
package com.fordham.toolbelt.domain.model

sealed interface StorageOutcome {
    data class Success(val path: String) : StorageOutcome
    data class Failure(val error: FailureMessage) : StorageOutcome
}

sealed interface StorageBytesOutcome {
    data class Success(val bytes: ByteArray) : StorageBytesOutcome
    data class Failure(val error: FailureMessage) : StorageBytesOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\Supplier.kt

```kt
package com.fordham.toolbelt.domain.model

data class Supplier(
    val id: SupplierId,
    val name: String,
    val category: SupplierCategory,
    val address: String = "",
    val phone: PhoneNumber = PhoneNumber(""),
    val webUrl: String = "",
    val packageName: String = "",
    val displayOrder: Int,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val customLogoPath: String? = null,
    val logoResName: String? = null,
    val isDefault: Boolean = false,
    val analytics: SupplierAnalytics = SupplierAnalytics()
)

enum class SupplierCategory {
    LUMBER, ELECTRICAL, PLUMBING, PAINT, ROOFING, HVAC, HARDWARE, FASTENERS, FLOORING, GENERAL_SUPPLY, OTHER
}

data class SupplierAnalytics(
    val totalSpendYtd: Double = 0.0,
    val jobsLinked: Int = 0,
    val avgMarkup: Double = 0.0
)

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\SupplierOutcome.kt

```kt
package com.fordham.toolbelt.domain.model

sealed interface SupplierOutcome {
    data object Success : SupplierOutcome
    data class Failure(val error: FailureMessage) : SupplierOutcome
}

sealed interface SupplierListOutcome {
    data class Success(val suppliers: List<Supplier>) : SupplierListOutcome
    data class Failure(val error: FailureMessage) : SupplierListOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\SyncOutcome.kt

```kt
package com.fordham.toolbelt.domain.model

sealed interface SyncOutcome {
    data object Success : SyncOutcome
    data class Failure(val error: FailureMessage) : SyncOutcome
}

sealed interface SyncUploadOutcome {
    data class Success(val path: String) : SyncUploadOutcome
    data class Failure(val error: FailureMessage) : SyncUploadOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\TaskType.kt

```kt
package com.fordham.toolbelt.domain.model

enum class TaskType {
    SUMMARIZE,
    GENERATE,
    ANALYZE,
    RECOMMEND
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\TaxExportOutcome.kt

```kt
package com.fordham.toolbelt.domain.model

sealed interface TaxExportOutcome {
    data class Success(val path: String) : TaxExportOutcome
    data class Failure(val error: FailureMessage) : TaxExportOutcome
    data object Loading : TaxExportOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\model\ValueClasses.kt

```kt
package com.fordham.toolbelt.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class ClientId(val value: String)

@JvmInline
value class InvoiceId(val value: String)

@JvmInline
value class SupplierId(val value: String)

@JvmInline
value class ReceiptId(val value: String)

@JvmInline
value class NoteId(val value: String)

@JvmInline
value class PhotoId(val value: String)

@JvmInline
value class EmailAddress(val value: String)

@JvmInline
value class PhoneNumber(val value: String)

@JvmInline
value class MoneyAmount(val value: Double) {
    init {
        require(value >= 0.0) { "Money amount cannot be negative." }
    }
}

@JvmInline
value class BackupFileName(val value: String) {
    init {
        require(value.isNotBlank()) { "Backup file name cannot be blank." }
    }
}

@JvmInline
value class BackupPayload(val bytes: ByteArray) {
    init {
        require(bytes.isNotEmpty()) { "Backup payload cannot be empty." }
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\AgentLlmGateway.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage

interface AgentLlmGateway {
    suspend fun prompt(
        systemPrompt: NaturalLanguage,
        session: ForemanSession,
        functions: List<AgentFunction>
    ): AgentOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\AuthRepository.kt

```kt
package com.fordham.toolbelt.domain.repository

import kotlinx.coroutines.flow.StateFlow
import kotlin.jvm.JvmInline

@JvmInline
value class UserId(val value: String)

@JvmInline
value class EmailAddress(val value: String)

@JvmInline
value class DisplayName(val value: String)

@JvmInline
value class PhotoUrl(val value: String)

@JvmInline
value class IdToken(val value: String)

@JvmInline
value class IsPremium(val value: Boolean)

data class FordhamUser(
    val id: UserId,
    val email: EmailAddress?,
    val displayName: DisplayName?,
    val photoUrl: PhotoUrl?,
    val isPremium: IsPremium = IsPremium(true)
)

data class OperationError(val message: String)

sealed interface AuthOutcome {
    data class Authenticated(val user: FordhamUser) : AuthOutcome
    data class Failure(val error: OperationError) : AuthOutcome
    object SignedOut : AuthOutcome
}

interface AuthRepository {
    val currentUser: StateFlow<FordhamUser?>
    
    suspend fun signInWithGoogle(idToken: IdToken): AuthOutcome
    suspend fun signOut(): AuthOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\ClientRepository.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ClientOutcome
import com.fordham.toolbelt.domain.model.ClientListOutcome
import kotlinx.coroutines.flow.Flow

interface ClientRepository {
    fun getAllClients(): Flow<ClientListOutcome>
    suspend fun searchClients(query: String): List<Client>
    suspend fun insertClient(client: Client): ClientOutcome
    suspend fun deleteClient(client: Client): ClientOutcome
}
```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\DraftRepository.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.DraftInvoice
import kotlinx.coroutines.flow.Flow

interface DraftRepository {
    fun getDraft(): Flow<DraftInvoice>
    suspend fun saveDraft(draft: DraftInvoice)
    suspend fun clearDraft()
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\DriveAuthTokenProvider.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.FailureMessage

@JvmInline
value class DriveAccessToken(val value: String) {
    init {
        require(value.isNotBlank()) { "DriveAccessToken cannot be blank." }
    }
}

sealed interface DriveTokenOutcome {
    data class Success(val token: DriveAccessToken) : DriveTokenOutcome
    data class Failure(val error: FailureMessage) : DriveTokenOutcome
}

interface DriveAuthTokenProvider {
    suspend fun getDriveAccessToken(): DriveTokenOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\ForemanAgentDispatchers.kt

```kt
package com.fordham.toolbelt.domain.repository

import kotlinx.coroutines.CoroutineDispatcher

interface ForemanAgentDispatchers {
    val background: CoroutineDispatcher
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\GeminiRepository.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.*

interface GeminiRepository {
    suspend fun processTask(type: TaskType, data: String): GeminiOutcome
    suspend fun processAgentCommand(input: String): AgentCommandOutcome
    
    // New Agentic Brain Contract
    suspend fun generateToolCall(input: String, context: String): ToolCallOutcome

    // Migrated from GeminiParser
    suspend fun processInvoiceText(text: String, categories: List<String>): InvoiceTextOutcome
    suspend fun processReceiptImage(imageBytes: ByteArray): ReceiptImageOutcome
    
    // OCR capability for Raw Text Extraction
    suspend fun processOcrImage(imageBytes: ByteArray): GeminiOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\InvoiceEngine.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.InvoiceData

interface InvoiceEngine {
    fun generatePdf(data: InvoiceData): String?
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\InvoiceRepository.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.InvoiceOutcome
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.InvoiceId
import kotlinx.coroutines.flow.Flow

interface InvoiceRepository {
    val allInvoices: Flow<List<Invoice>>
    suspend fun insertInvoice(invoice: Invoice): InvoiceOutcome
    suspend fun insertInvoices(invoices: List<Invoice>): InvoiceOutcome
    suspend fun updateInvoice(invoice: Invoice): InvoiceOutcome
    suspend fun deleteInvoice(invoice: Invoice): InvoiceOutcome
    suspend fun getInvoiceById(id: InvoiceId): Invoice?
    fun getInvoicesByClient(clientName: String): Flow<List<Invoice>>
    suspend fun deleteAllInvoices(): InvoiceOutcome
}
```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\JobNoteRepository.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.JobNote
import com.fordham.toolbelt.domain.model.JobNoteOutcome
import com.fordham.toolbelt.domain.model.InvoiceId
import kotlinx.coroutines.flow.Flow

interface JobNoteRepository {
    fun getNotesByInvoice(invoiceId: InvoiceId): Flow<List<JobNote>>
    fun getNotesByClient(clientName: String): Flow<List<JobNote>>
    suspend fun insertNote(note: JobNote): JobNoteOutcome
    suspend fun deleteNote(note: JobNote): JobNoteOutcome
}
```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\OcrRepository.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.GeminiOutcome

interface OcrRepository {
    suspend fun recognizeText(imageBytes: ByteArray): GeminiOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\PaymentRepository.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentLedgerOutcome
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestOutcome
import com.fordham.toolbelt.domain.model.PaymentRequestType
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    val ledger: Flow<PaymentLedgerOutcome>
    suspend fun createPaymentRequest(invoice: Invoice, type: PaymentRequestType, provider: PaymentProviderType): PaymentRequestOutcome
    suspend fun refreshLedger(): PaymentLedgerOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\PhotoRepository.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.JobPhoto
import com.fordham.toolbelt.domain.model.PhotoOutcome
import com.fordham.toolbelt.domain.model.PhotoListOutcome
import com.fordham.toolbelt.domain.model.InvoiceId
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun observePhotosForInvoice(invoiceId: InvoiceId): Flow<List<JobPhoto>>
    suspend fun getPhotosForInvoiceOnce(invoiceId: InvoiceId): PhotoListOutcome
    suspend fun savePhoto(photo: JobPhoto): PhotoOutcome
    suspend fun deletePhoto(photo: JobPhoto): PhotoOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\ReceiptRepository.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.ReceiptOutcome
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.model.ReceiptItem
import kotlinx.coroutines.flow.Flow

interface ReceiptRepository {
    val allItems: Flow<ReceiptListOutcome>
    suspend fun insertItem(item: ReceiptItem): ReceiptOutcome
    suspend fun insertItems(items: List<ReceiptItem>): ReceiptOutcome
    suspend fun deleteItem(item: ReceiptItem): ReceiptOutcome
    suspend fun deleteAllItems(): ReceiptOutcome
    fun getItemsByClient(clientName: String): Flow<ReceiptListOutcome>
    fun getUnassignedReceipts(): Flow<ReceiptListOutcome>
    suspend fun updateItem(item: ReceiptItem): ReceiptOutcome
}
```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\SettingsRepository.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.SettingsOutcome
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val businessSettingsFlow: Flow<BusinessSettings>
    suspend fun getBusinessSettings(): BusinessSettings
    suspend fun saveBusinessSettings(settings: BusinessSettings): SettingsOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\StorageRepository.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.StorageOutcome
import com.fordham.toolbelt.domain.model.StorageBytesOutcome

interface StorageRepository {
    suspend fun saveBitmapBytesToPictures(imageBytes: ByteArray, prefix: String): StorageOutcome
    suspend fun getBytesFromUri(uriString: String): StorageBytesOutcome
    suspend fun saveUriToPictures(uriString: String, prefix: String): StorageOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\SupplierRepository.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.Supplier
import com.fordham.toolbelt.domain.model.SupplierOutcome
import com.fordham.toolbelt.domain.model.SupplierListOutcome
import com.fordham.toolbelt.domain.model.SupplierId
import com.fordham.toolbelt.domain.model.MoneyAmount
import kotlinx.coroutines.flow.Flow

interface SupplierRepository {
    fun getVisibleSuppliers(): Flow<SupplierListOutcome>
    fun getHiddenSuppliers(): Flow<SupplierListOutcome>
    suspend fun upsertSupplier(supplier: Supplier): SupplierOutcome
    suspend fun hideSupplier(id: SupplierId): SupplierOutcome
    suspend fun restoreSupplier(id: SupplierId): SupplierOutcome
    suspend fun updateOrder(id: SupplierId, newOrder: Int): SupplierOutcome
    suspend fun seedDefaultSuppliers(): SupplierOutcome
    suspend fun logPurchase(supplierId: SupplierId, amount: MoneyAmount): SupplierOutcome
    suspend fun togglePin(id: SupplierId, isPinned: Boolean): SupplierOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\SyncRepository.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.BackupFileName
import com.fordham.toolbelt.domain.model.BackupPayload
import com.fordham.toolbelt.domain.model.SyncOutcome
import com.fordham.toolbelt.domain.model.SyncUploadOutcome

interface SyncRepository {
    suspend fun syncInvoices(): SyncOutcome
    suspend fun syncReceipts(): SyncOutcome
    suspend fun uploadToDrive(fileName: BackupFileName, content: BackupPayload): SyncUploadOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\repository\ToolRegistry.kt

```kt
package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.agent.AgentFunction
import com.fordham.toolbelt.domain.model.agent.ToolArguments
import com.fordham.toolbelt.domain.model.agent.ToolExecutionResult
import com.fordham.toolbelt.domain.model.agent.ToolName

interface ToolRegistry {
    fun availableFunctions(): List<AgentFunction>

    suspend fun execute(
        toolName: ToolName,
        arguments: ToolArguments
    ): ToolExecutionResult
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\AddJobNoteUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.JobNote
import com.fordham.toolbelt.domain.model.JobNoteOutcome
import com.fordham.toolbelt.domain.model.NoteId
import com.fordham.toolbelt.domain.repository.JobNoteRepository
import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

/**
 * Responsibility: Logic for creating and persisting a new job note.
 */
class AddJobNoteUseCase(
    private val repository: JobNoteRepository
) {
    suspend operator fun invoke(clientName: String, text: String): JobNoteOutcome {
        val note = JobNote(
            id = NoteId(randomUUID()),
            clientName = clientName,
            text = text,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        return repository.insertNote(note)
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\AddSupplierUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.Supplier
import com.fordham.toolbelt.domain.repository.SupplierRepository

class AddSupplierUseCase(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(supplier: Supplier) = repository.upsertSupplier(supplier)
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\BillLaborUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.BillLaborOutcome
import com.fordham.toolbelt.domain.model.LineItem
import com.fordham.toolbelt.domain.repository.DraftRepository
import kotlinx.coroutines.flow.first

/**
 * Responsibility: Convert elapsed timer seconds into a labor line item and update the draft.
 */
class BillLaborUseCase(
    private val draftRepository: DraftRepository
) {
    suspend operator fun invoke(): BillLaborOutcome = try {
        val draft = draftRepository.getDraft().first()
        val hours = draft.elapsedSeconds / 3600.0
        val rate = draft.hourlyRate
        val item = LineItem(
            description = "Labor: ${"%.2f".format(hours)} hours", 
            amount = hours * rate, 
            category = "Labor"
        )
        
        val updatedItems = draft.lineItems + item
        
        draftRepository.saveDraft(draft.copy(
            lineItems = updatedItems,
            elapsedSeconds = 0L,
            timerRunning = false
        ))
        BillLaborOutcome.Success
    } catch (e: Throwable) {
        BillLaborOutcome.Error(e, e.message)
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\CreatePaymentRequestUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.PaymentProviderType
import com.fordham.toolbelt.domain.model.PaymentRequestOutcome
import com.fordham.toolbelt.domain.model.PaymentRequestType
import com.fordham.toolbelt.domain.repository.PaymentRepository

class CreatePaymentRequestUseCase(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(invoice: Invoice, type: PaymentRequestType, provider: PaymentProviderType): PaymentRequestOutcome {
        return repository.createPaymentRequest(invoice, type, provider)
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\DeleteClientUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.Client
import com.fordham.toolbelt.domain.model.ClientOutcome
import com.fordham.toolbelt.domain.repository.ClientRepository

/**
 * Responsibility: Logic for deleting a client from the directory.
 */
class DeleteClientUseCase(
    private val repository: ClientRepository
) {
    suspend operator fun invoke(client: Client): ClientOutcome {
        return repository.deleteClient(client)
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\DeleteJobNoteUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.JobNote
import com.fordham.toolbelt.domain.model.JobNoteOutcome
import com.fordham.toolbelt.domain.repository.JobNoteRepository

/**
 * Responsibility: Logic for deleting a job note.
 */
class DeleteJobNoteUseCase(
    private val repository: JobNoteRepository
) {
    suspend operator fun invoke(note: JobNote): JobNoteOutcome = repository.deleteNote(note)
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\ForemanOrchestrator.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.*

class ForemanOrchestrator(
    private val geminiRepository: GeminiRepository,
    private val invoiceRepository: InvoiceRepository,
    private val clientRepository: ClientRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend fun processCommand(input: String, appContext: String): OrchestrationResult {
        // 1. Check Premium Status (Safety Gate)
        val settings = settingsRepository.getBusinessSettings()
        if (!settings.isPremium) {
            return OrchestrationResult.Failure(FailureMessage("Premium subscription required for Agentic commands"))
        }

        // 2. Generate Tool Call from Repository
        val result = geminiRepository.generateToolCall(input, appContext)
        
        return when (result) {
            is ToolCallOutcome.Success -> {
                val toolCall = result.toolCall ?: return OrchestrationResult.ResponseOnly("I'm not sure how to help with that yet.")
                
                // 3. Deterministic Safety Gate
                if (toolCall.type.category == ToolCategory.DESTRUCTIVE) {
                    OrchestrationResult.ApprovalRequired(toolCall)
                } else {
                    executeSafeTool(toolCall)
                }
            }
            is ToolCallOutcome.Failure -> OrchestrationResult.Failure(result.error)
        }
    }

    private suspend fun executeSafeTool(toolCall: ForemanToolCall): OrchestrationResult {
        return try {
            when (val params = toolCall.parameters) {
                is ToolParameters.SearchClients -> {
                    val clients = clientRepository.searchClients(params.query)
                    OrchestrationResult.Executed("Found ${clients.size} clients matching '${params.query}'", toolCall)
                }
                is ToolParameters.OpenTab -> {
                    OrchestrationResult.Executed("Navigating to ${params.tabName} tab", toolCall)
                }
                is ToolParameters.AddJobNote -> {
                    // Logic to add a note
                    OrchestrationResult.Executed("Added note for ${params.clientName}", toolCall)
                }
                else -> OrchestrationResult.Executed(toolCall.reasoning, toolCall)
            }
        } catch (e: Exception) {
            OrchestrationResult.Failure(FailureMessage("Failed to execute tool ${toolCall.type}: ${e.message}"))
        }
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\GenerateAndSaveInvoiceUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.*
import com.fordham.toolbelt.util.DateTimeUtil
import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

class GenerateAndSaveInvoiceUseCase(
    private val saveInvoiceUseCase: SaveInvoiceUseCase,
    private val clientRepository: ClientRepository,
    private val photoRepository: PhotoRepository,
    private val storageRepository: StorageRepository,
    private val receiptRepository: ReceiptRepository,
    private val engine: InvoiceEngine
) {
    suspend operator fun invoke(
        clientName: String,
        clientAddress: String,
        saveToClientDirectory: Boolean,
        taxRate: Double,
        deposit: Double,
        lineItems: List<LineItem>,
        logoUriString: String?,
        businessSettings: BusinessSettings,
        isEstimate: Boolean,
        elapsedSeconds: Long,
        capturedPhotos: List<String>,
        linkedReceiptIds: List<String>,
        availableReceipts: List<ReceiptItem>,
        onGenerated: (String) -> Unit
    ): GenerateInvoiceOutcome {
        return try {
            println("GENERATE_SAVE_USECASE: Step 1 - Saving client if saveToClientDirectory=$saveToClientDirectory is true...")
            // 1. Save to Client Directory if requested
            if (saveToClientDirectory && clientName.isNotEmpty()) {
                clientRepository.insertClient(Client(id = ClientId(randomUUID()), name = clientName, address = clientAddress))
            }

            println("GENERATE_SAVE_USECASE: Step 2 - Preparing data...")
            // 2. Prepare Data
            val date = DateTimeUtil.getNowFormatted()
            val subtotal = lineItems.sumOf { it.amount }
            val invoiceId = randomUUID()
            
            val data = InvoiceData(
                invoiceId = invoiceId,
                clientName = clientName, 
                clientAddress = clientAddress, 
                items = lineItems, 
                taxRate = taxRate, 
                date = date, 
                logoUriString = logoUriString, 
                settings = businessSettings, 
                isEstimate = isEstimate, 
                deposit = deposit,
                photoUris = capturedPhotos
            )

            println("GENERATE_SAVE_USECASE: Step 3 - Generating PDF...")
            // 3. Generate PDF
            val path = engine.generatePdf(data)
            if (path == null) {
                println("GENERATE_SAVE_USECASE: PDF generation FAILED!")
                return GenerateInvoiceOutcome.Error(Exception("Failed to generate PDF"))
            }
            println("GENERATE_SAVE_USECASE: PDF generated successfully at $path")

            println("GENERATE_SAVE_USECASE: Step 4 - Saving Invoice to Database...")
            // 4. Save Invoice to Database
            val saveResult = saveInvoiceUseCase(
                clientName = clientName,
                clientAddress = clientAddress,
                subtotal = subtotal,
                taxRate = taxRate,
                depositAmount = deposit,
                itemsSummary = lineItems.joinToString { it.description },
                pdfPath = path,
                isEstimate = isEstimate,
                durationSeconds = elapsedSeconds
            )

            if (saveResult is SaveInvoiceOutcome.Success) {
                val savedInvoice = saveResult.invoice
                println("GENERATE_SAVE_USECASE: Invoice saved to DB successfully with ID: ${savedInvoice.id}")
                
                println("GENERATE_SAVE_USECASE: Step 5 - Persisting Linked Receipts...")
                // 5. Persist Linked Receipts
                linkedReceiptIds.forEach { receiptId ->
                    val receipt = availableReceipts.find { it.id.value == receiptId }
                    if (receipt != null) {
                        receiptRepository.updateItem(receipt.copy(
                            linkedInvoiceId = savedInvoice.id,
                            isBilled = true,
                            clientName = savedInvoice.clientName
                        ))
                    }
                }

                println("GENERATE_SAVE_USECASE: Step 6 - Saving Job Photos...")
                // 6. Save Photos
                capturedPhotos.forEach { photoUri ->
                    val vaultResult = storageRepository.saveUriToPictures(photoUri, "JOB")
                    val finalUri = if (vaultResult is StorageOutcome.Success) vaultResult.path else photoUri
                    
                    photoRepository.savePhoto(
                        JobPhoto(
                            id = PhotoId(randomUUID()),
                            invoiceId = savedInvoice.id,
                            localUri = finalUri,
                            timestamp = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }

                println("GENERATE_SAVE_USECASE: Step 7 - Triggering onGenerated callback...")
                onGenerated(path)
                println("GENERATE_SAVE_USECASE: Done!")
                GenerateInvoiceOutcome.Success
            } else if (saveResult is SaveInvoiceOutcome.Error) {
                println("GENERATE_SAVE_USECASE: SaveInvoiceUseCase FAILED: ${saveResult.message}")
                GenerateInvoiceOutcome.Error(saveResult.exception, saveResult.message)
            } else {
                println("GENERATE_SAVE_USECASE: SaveInvoiceUseCase returned unknown state")
                GenerateInvoiceOutcome.Error(Exception("Unknown error saving invoice"))
            }
        } catch (e: Exception) {
            println("GENERATE_SAVE_USECASE: Exception caught: ${e.message}")
            e.printStackTrace()
            GenerateInvoiceOutcome.Error(e, "Error saving ${if (isEstimate) "estimate" else "invoice"}: ${e.message}")
        }
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\GenerateSummaryUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class GenerateSummaryUseCase(
    private val geminiRepository: GeminiRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(data: String): GeminiOutcome {
        val settings = settingsRepository.businessSettingsFlow.first()
        if (!settings.isPremium) {
            return GeminiOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage("Premium Required"))
        }
        return geminiRepository.processTask(TaskType.SUMMARIZE, data)
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\GenerateTaxReportUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.util.TaxExporter
import kotlinx.coroutines.flow.first

class GenerateTaxReportUseCase(
    private val invoiceRepository: InvoiceRepository,
    private val receiptRepository: ReceiptRepository,
    private val settingsRepository: SettingsRepository,
    private val taxExporter: TaxExporter
) {
    suspend fun executeBentoReport(): TaxExportOutcome {
        val settings = settingsRepository.businessSettingsFlow.first()
        if (!settings.isPremium) {
            return TaxExportOutcome.Failure(FailureMessage("Premium subscription required"))
        }

        val invoices = invoiceRepository.allInvoices.first()
        
        val receiptsResult = receiptRepository.allItems.first()
        val receipts = when (receiptsResult) {
            is ReceiptListOutcome.Success -> receiptsResult.receipts
            is ReceiptListOutcome.Failure -> {
                return TaxExportOutcome.Failure(receiptsResult.error)
            }
        }

        return taxExporter.exportBentoReport(invoices, receipts)
    }

    suspend fun executeZip(): TaxExportOutcome {
        val settings = settingsRepository.businessSettingsFlow.first()
        if (!settings.isPremium) {
            return TaxExportOutcome.Failure(FailureMessage("Premium subscription required"))
        }

        val invoices = invoiceRepository.allInvoices.first()
        
        val receiptsResult = receiptRepository.allItems.first()
        val receipts = when (receiptsResult) {
            is ReceiptListOutcome.Success -> receiptsResult.receipts
            is ReceiptListOutcome.Failure -> {
                return TaxExportOutcome.Failure(receiptsResult.error)
            }
        }

        return taxExporter.exportFullTaxBundle(invoices, receipts)
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\GetBusinessStatsUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.BusinessStats
import com.fordham.toolbelt.domain.model.ProjectStat
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetBusinessStatsUseCase(
    private val invoiceRepository: InvoiceRepository,
    private val receiptRepository: ReceiptRepository
) {
    operator fun invoke(): Flow<BusinessStats> {
        return combine(
            invoiceRepository.allInvoices,
            receiptRepository.allItems
        ) { invoices, receiptsResult ->
            val receipts = if (receiptsResult is ReceiptListOutcome.Success) {
                receiptsResult.receipts
            } else {
                emptyList()
            }

            // Core filters
            val paidNonEstimateInvoices = invoices.filter { it.isPaid && !it.isEstimate }
            val nonEstimateInvoices = invoices.filter { !it.isEstimate }

            // Global Calculations
            val revenue = paidNonEstimateInvoices.sumOf { it.totalAmount }
            val expenses = receipts.sumOf { it.totalPrice }
            val netProfit = revenue - expenses
            val profitMargin = if (revenue > 0.0) ((netProfit / revenue) * 100.0).toInt() else 0

            val totalDurationSeconds = paidNonEstimateInvoices.sumOf { it.durationSeconds }
            val unbilledExpenses = receipts.filter { !it.isBilled }.sumOf { it.totalPrice }

            // Client-Specific Project Stats
            val allClients = (invoices.map { it.clientName } + receipts.map { it.clientName })
                .filter { it.isNotBlank() }
                .distinct()

            val projectStats = allClients.map { client ->
                val clientInvoices = nonEstimateInvoices.filter { it.clientName == client }
                val clientPaidInvoices = clientInvoices.filter { it.isPaid }
                val clientReceipts = receipts.filter { it.clientName == client }

                val clientRevenue = clientPaidInvoices.sumOf { it.totalAmount }
                val clientExpenses = clientReceipts.sumOf { it.totalPrice }
                val clientProfit = clientRevenue - clientExpenses

                val progress = if (clientInvoices.isNotEmpty()) {
                    (clientPaidInvoices.size.toDouble() / clientInvoices.size.toDouble()).toFloat()
                } else {
                    0.0f
                }

                ProjectStat(
                    clientName = client,
                    revenue = clientRevenue,
                    expenses = clientExpenses,
                    profit = clientProfit,
                    progress = progress
                )
            }.sortedByDescending { it.revenue }

            BusinessStats(
                netProfit = netProfit,
                totalExpenses = expenses,
                totalDurationSeconds = totalDurationSeconds,
                unbilledExpenses = unbilledExpenses,
                projectStats = projectStats,
                profitMargin = profitMargin
            )
        }
    }
}


```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\GetClientFinancialSummaryUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.ReceiptListOutcome
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.util.DateTimeUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class FinancialSummary(
    val revenue: Double,
    val expenses: Double,
    val profit: Double
) {
    val formattedRevenue: String get() = DateTimeUtil.formatMoney(revenue)
    val formattedExpenses: String get() = DateTimeUtil.formatMoney(expenses)
    val formattedProfit: String get() = DateTimeUtil.formatMoney(profit)
}

class GetClientFinancialSummaryUseCase(
    private val invoiceRepository: InvoiceRepository,
    private val receiptRepository: ReceiptRepository
) {
    operator fun invoke(clientName: String): Flow<FinancialSummary> {
        return combine(
            invoiceRepository.allInvoices,
            receiptRepository.allItems
        ) { invoices, receiptsResult ->
            val revenue = invoices
                .filter { it.clientName == clientName && it.isPaid && !it.isEstimate }
                .sumOf { it.totalAmount }

            val expenses = if (receiptsResult is ReceiptListOutcome.Success) {
                receiptsResult.receipts
                    .filter { it.clientName == clientName }
                    .sumOf { it.totalPrice }
            } else 0.0
            
            val profit = revenue - expenses
            FinancialSummary(revenue, expenses, profit)
        }
    }
}
```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\GetHiddenSuppliersUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.SupplierListOutcome
import com.fordham.toolbelt.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.Flow

class GetHiddenSuppliersUseCase(
    private val repository: SupplierRepository
) {
    operator fun invoke(): Flow<SupplierListOutcome> = repository.getHiddenSuppliers()
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\GetPaymentLedgerUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.repository.PaymentRepository

class GetPaymentLedgerUseCase(
    repository: PaymentRepository
) {
    val ledger = repository.ledger
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\GetSuppliersUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.SupplierListOutcome
import com.fordham.toolbelt.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.Flow

class GetSuppliersUseCase(
    private val repository: SupplierRepository
) {
    operator fun invoke(): Flow<SupplierListOutcome> = repository.getVisibleSuppliers()
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\GlobalAiAgentUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class GlobalAiAgentUseCase(
    private val geminiRepository: GeminiRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(userInput: String): AgentCommandOutcome {
        val settings = settingsRepository.businessSettingsFlow.first()
        if (!settings.isPremium) {
            return AgentCommandOutcome.Failure(FailureMessage("Premium Required"))
        }
        return geminiRepository.processAgentCommand(userInput)
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\HideSupplierUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.SupplierId
import com.fordham.toolbelt.domain.repository.SupplierRepository

class HideSupplierUseCase(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(id: SupplierId) = repository.hideSupplier(id)
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\LinkReceiptToClientUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.ReceiptOutcome
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.repository.ReceiptRepository

/**
 * Responsibility: Logic for associating a floating receipt with a specific client.
 */
class LinkReceiptToClientUseCase(
    private val repository: ReceiptRepository
) {
    suspend operator fun invoke(receipt: ReceiptItem, clientName: String): ReceiptOutcome {
        return repository.updateItem(receipt.copy(
            clientName = clientName,
            isBilled = true
        ))
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\LogSupplierPurchaseUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.MoneyAmount
import com.fordham.toolbelt.domain.model.SupplierId
import com.fordham.toolbelt.domain.repository.SupplierRepository

class LogSupplierPurchaseUseCase(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(id: SupplierId, amount: MoneyAmount) = repository.logPurchase(id, amount)
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\ProcessInvoiceAiUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.InvoiceTextOutcome
import com.fordham.toolbelt.domain.repository.GeminiRepository

/**
 * Responsibility: Extract invoice data from raw text using AI.
 */
class ProcessInvoiceAiUseCase(
    private val geminiRepository: GeminiRepository
) {
    suspend operator fun invoke(text: String, categories: List<String>): InvoiceTextOutcome {
        return geminiRepository.processInvoiceText(text, categories)
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\ProcessReceiptUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.repository.ReceiptRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class ProcessReceiptUseCase(
    private val geminiRepository: GeminiRepository,
    private val receiptRepository: ReceiptRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray, clientName: String? = null): ProcessReceiptOutcome {
        return try {
            val settings = settingsRepository.businessSettingsFlow.first()
            if (!settings.isPremium) {
                ProcessReceiptOutcome.PremiumRequired
            } else {
                val result = geminiRepository.processReceiptImage(imageBytes)
                when (result) {
                    is ReceiptImageOutcome.Success -> {
                        val items = if (clientName != null) {
                            result.items.map { it.copy(clientName = clientName) }
                        } else {
                            result.items
                        }
                        receiptRepository.insertItems(items)
                        ProcessReceiptOutcome.Success(items)
                    }
                    is ReceiptImageOutcome.Failure -> {
                        ProcessReceiptOutcome.Failure(result.error)
                    }
                }
            }
        } catch (e: Exception) {
            ProcessReceiptOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to process receipt"))
        }
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\RestoreSupplierUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.SupplierId
import com.fordham.toolbelt.domain.repository.SupplierRepository

class RestoreSupplierUseCase(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(id: SupplierId) = repository.restoreSupplier(id)
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\RunForemanAgentUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.agent.AgentOutcome
import com.fordham.toolbelt.domain.model.agent.AgentRole
import com.fordham.toolbelt.domain.model.agent.ForemanSession
import com.fordham.toolbelt.domain.model.agent.ForemanTurn
import com.fordham.toolbelt.domain.model.agent.ForemanToolPolicy
import com.fordham.toolbelt.domain.model.agent.NaturalLanguage
import com.fordham.toolbelt.domain.model.agent.TimestampMillis
import com.fordham.toolbelt.domain.model.agent.ToolArgumentValidator
import com.fordham.toolbelt.domain.model.agent.ToolArguments
import com.fordham.toolbelt.domain.model.agent.ToolName
import com.fordham.toolbelt.domain.model.agent.ToolSafety
import com.fordham.toolbelt.domain.repository.AgentLlmGateway
import com.fordham.toolbelt.domain.repository.ForemanAgentDispatchers
import com.fordham.toolbelt.domain.repository.ToolRegistry
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class RunForemanAgentUseCase(
    private val llmGateway: AgentLlmGateway,
    private val toolRegistry: ToolRegistry,
    private val dispatchers: ForemanAgentDispatchers
) {
    suspend operator fun invoke(
        command: NaturalLanguage,
        session: ForemanSession,
        systemPrompt: NaturalLanguage,
        timestamp: TimestampMillis = TimestampMillis(Clock.System.now().toEpochMilliseconds())
    ): AgentOutcome = withContext(dispatchers.background) {
        // withContext keeps the full run inside structured concurrency and remains cancellable.
        if (command.value.isBlank()) {
            return@withContext AgentOutcome.Failure(FailureMessage("Agent command cannot be blank."))
        }

        val promptedSession = session.append(
            ForemanTurn(
                role = AgentRole.User,
                content = command,
                timestamp = timestamp
            )
        )

        when (val outcome = llmGateway.prompt(systemPrompt, promptedSession, toolRegistry.availableFunctions())) {
            is AgentOutcome.TextResponse -> outcome
            is AgentOutcome.Failure -> outcome
            is AgentOutcome.ToolExecutionRequested -> handleToolRequest(outcome)
            is AgentOutcome.RequiresApproval -> validateApprovalRequest(outcome)
            is AgentOutcome.ToolExecuted -> AgentOutcome.Failure(
                FailureMessage("LLM gateway cannot mark tools as executed.")
            )
        }
    }

    private suspend fun handleToolRequest(
        request: AgentOutcome.ToolExecutionRequested
    ): AgentOutcome {
        val validationFailure = validateToolRequest(
            toolName = request.toolName,
            arguments = request.arguments
        )
        if (validationFailure != null) return validationFailure

        return when (ForemanToolPolicy.safetyFor(request.toolName)) {
            ToolSafety.RequiresApproval -> AgentOutcome.RequiresApproval(
                toolCallId = request.toolCallId,
                toolName = request.toolName,
                arguments = request.arguments
            )
            ToolSafety.Safe -> AgentOutcome.ToolExecuted(
                toolCallId = request.toolCallId,
                result = toolRegistry.execute(request.toolName, request.arguments)
            )
        }
    }

    private fun validateApprovalRequest(
        request: AgentOutcome.RequiresApproval
    ): AgentOutcome {
        val validationFailure = validateToolRequest(
            toolName = request.toolName,
            arguments = request.arguments
        )
        if (validationFailure != null) return validationFailure

        return when (ForemanToolPolicy.safetyFor(request.toolName)) {
            ToolSafety.RequiresApproval -> request
            ToolSafety.Safe -> AgentOutcome.Failure(
                FailureMessage("Tool does not require approval: ${request.toolName}.")
            )
        }
    }

    private fun validateToolRequest(
        toolName: ToolName,
        arguments: ToolArguments
    ): AgentOutcome.Failure? {
        return if (ToolArgumentValidator.isCompatible(toolName, arguments)) {
            null
        } else {
            AgentOutcome.Failure(
                FailureMessage("Tool arguments do not match requested tool.")
            )
        }
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\SaveInvoiceUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

class SaveInvoiceUseCase(
    private val repository: InvoiceRepository
) {
    suspend operator fun invoke(
        clientName: String,
        clientAddress: String,
        subtotal: Double,
        taxRate: Double,
        depositAmount: Double,
        itemsSummary: String,
        pdfPath: String,
        isEstimate: Boolean,
        durationSeconds: Long = 0L
    ): SaveInvoiceOutcome {
        val total = (subtotal * (1 + taxRate / 100)) - depositAmount
        val invoice = Invoice(
            id = InvoiceId(randomUUID()),
            clientName = clientName,
            clientAddress = clientAddress,
            clientPhone = PhoneNumber(""), // Optional
            clientEmail = EmailAddress(""), // Optional
            date = com.fordham.toolbelt.util.DateTimeUtil.getNowFormatted(),
            totalAmount = total,
            depositAmount = depositAmount,
            itemsSummary = itemsSummary,
            pdfPath = pdfPath,
            isPaid = false,
            isEstimate = isEstimate,
            lastUpdated = Clock.System.now().toEpochMilliseconds(),
            durationSeconds = durationSeconds
        )
        return try {
            val result = repository.insertInvoice(invoice)
            if (result is InvoiceOutcome.Success) {
                SaveInvoiceOutcome.Success(invoice)
            } else {
                val errorMsg = (result as? InvoiceOutcome.Failure)?.error?.value ?: "Failed to save invoice"
                SaveInvoiceOutcome.Error(Exception(errorMsg), errorMsg)
            }
        } catch (e: Exception) {
            SaveInvoiceOutcome.Error(e, e.message ?: "Failed to save invoice")
        }
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\SaveJobPhotoUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.repository.PhotoRepository
import com.fordham.toolbelt.util.randomUUID
import kotlinx.datetime.Clock

/**
 * Responsibility: Logic for creating and persisting a job photo record.
 */
class SaveJobPhotoUseCase(
    private val repository: PhotoRepository
) {
    suspend operator fun invoke(uriString: String, invoiceId: String): PhotoOutcome {
        val photo = JobPhoto(
            id = PhotoId(randomUUID()),
            invoiceId = InvoiceId(invoiceId),
            localUri = uriString,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        return repository.savePhoto(photo)
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\SeedSuppliersUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.repository.SupplierRepository

class SeedSuppliersUseCase(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke() = repository.seedDefaultSuppliers()
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\ToggleSupplierPinUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.SupplierId
import com.fordham.toolbelt.domain.repository.SupplierRepository

class ToggleSupplierPinUseCase(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(id: SupplierId, isPinned: Boolean) = repository.togglePin(id, isPinned)
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\domain\usecase\UpdateSupplierOrderUseCase.kt

```kt
package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.SupplierId
import com.fordham.toolbelt.domain.repository.SupplierRepository

class UpdateSupplierOrderUseCase(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(id: SupplierId, newOrder: Int) = repository.updateOrder(id, newOrder)
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\util\AiUtil.kt

```kt
package com.fordham.toolbelt.util

object AiUtil {
    fun cleanJson(raw: String): String {
        var cleaned = raw.trim()
        
        // Remove markdown code blocks if present
        if (cleaned.contains("```")) {
            // Try to find content between ```json and ``` or just ``` and ```
            val regex = "```(?:json)?\\s*([\\s\\S]*?)\\s*```".toRegex()
            val match = regex.find(cleaned)
            if (match != null) {
                cleaned = match.groupValues[1].trim()
            } else {
                // Fallback: just strip the markers
                cleaned = cleaned.replace("```json", "").replace("```", "").trim()
            }
        }

        // Find the first '{' and last '}' to isolate the JSON object
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        
        if (start != -1 && end != -1 && end > start) {
            cleaned = cleaned.substring(start, end + 1)
        }
        
        return cleaned
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\util\Base64Util.kt

```kt
package com.fordham.toolbelt.util

expect fun encodeBase64(bytes: ByteArray): String
expect fun decodeBase64(base64: String): ByteArray

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\util\DateTimeUtil.kt

```kt
package com.fordham.toolbelt.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DateTimeUtil {
    fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
    
    fun formatEpoch(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dt.monthNumber}/${dt.dayOfMonth}/${dt.year}"
    }

    fun formatMoney(amount: Double): String {
        val sign = if (amount < 0) "-" else ""
        val absAmount = if (amount < 0) -amount else amount
        return "$sign$${formatDecimal(absAmount, 2)}"
    }

    fun formatDecimal(value: Double, decimals: Int): String {
        val multiplier = 10.0.pow(decimals)
        val roundedValue = kotlin.math.round(value * multiplier) / multiplier
        val parts = roundedValue.toString().split(".")
        val whole = parts[0]
        var fraction = if (parts.size > 1) parts[1] else ""
        while (fraction.length < decimals) fraction += "0"
        if (fraction.length > decimals) fraction = fraction.substring(0, decimals)
        return if (decimals > 0) "$whole.$fraction" else whole
    }

    fun getNowFormatted(): String = formatEpoch(nowEpochMillis())
}

private fun Double.pow(n: Int): Double {
    var result = 1.0
    repeat(n) { result *= this }
    return result
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\util\PlacesService.kt

```kt
package com.fordham.toolbelt.util

import kotlinx.coroutines.delay

data class PlaceSuggestion(
    val name: String,
    val address: String,
    val phone: String = "",
    val website: String = ""
)

class PlacesService {
    
    // Simulating zero-trust local search for common contractor suppliers.
    private val mockPlaces = listOf(
        PlaceSuggestion("Sherwin-Williams Pro Center", "123 Paint St, Industrial Park", "(555) 012-3456", "https://www.sherwin-williams.com"),
        PlaceSuggestion("Sherwin-Williams Commercial", "456 Commercial Way", "(555) 987-6543"),
        PlaceSuggestion("Home Depot Pro Desk", "789 Contractor Ln", "(555) 111-2222"),
        PlaceSuggestion("Ferguson Plumbing Supply", "321 Pipe Rd", "(555) 333-4444"),
        PlaceSuggestion("ABC Supply Co. Inc.", "555 Roofing Ave", "(555) 444-5555"),
        PlaceSuggestion("Graybar Electric", "888 Voltage Blvd", "(555) 777-8888")
    )

    suspend fun searchPlaces(query: String): List<PlaceSuggestion> {
        if (query.isBlank() || query.length < 2) return emptyList()
        delay(200)
        return mockPlaces.filter { it.name.contains(query, ignoreCase = true) }
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\util\PlatformActions.kt

```kt
package com.fordham.toolbelt.util

interface PlatformActions {
    fun openUrl(url: String)
    fun shareFile(path: String, title: String)
    fun openPdf(path: String)
    fun callPhone(phoneNumber: String)
    fun sendEmail(email: String)
    fun requestPermission(permission: String, onGranted: () -> Unit)
    fun isPermissionGranted(permission: String): Boolean
    fun showToast(message: String)
    fun launchApp(packageName: String, fallbackUrl: String)
    fun signInWithGoogle(onSuccess: (String) -> Unit, onError: (String) -> Unit)
    fun signOut()
    fun authenticateBiometric(title: String, subtitle: String, onSuccess: () -> Unit, onError: (String) -> Unit)
    fun isBiometricAvailable(): Boolean
    fun pickImage(onResult: (String?) -> Unit)
    fun capturePhoto(onResult: (String?) -> Unit)
    fun scheduleNotification(id: String, title: String, body: String, delayMillis: Long)
}

object Permission {
    const val RECORD_AUDIO = "record_audio"
    const val CAMERA = "camera"
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\util\SecretProvider.kt

```kt
package com.fordham.toolbelt.util

interface SecretProvider {
    fun getGeminiApiKey(): String
    fun getGeminiModelName(): String
    fun getGoogleClientId(): String
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\util\StringObfuscator.kt

```kt
package com.fordham.toolbelt.util

/**
 * A simple but effective runtime string de-obfuscator to thwart static analysis.
 * Strings are XOR-ed with an internal key.
 */
object StringObfuscator {
    private const val INTERNAL_KEY = "F0RDH4M_T00LB3LT_PR0T0C0L"

    /**
     * Decodes an XOR-obfuscated Base64 string.
     */
    fun decode(obfuscated: String): String {
        return try {
            val decodedBytes = decodeBase64(obfuscated)
            val keyBytes = INTERNAL_KEY.encodeToByteArray()
            val result = ByteArray(decodedBytes.size)
            for (i in decodedBytes.indices) {
                result[i] = (decodedBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            result.decodeToString()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Helper to obfuscate strings during development.
     */
    fun obfuscate(input: String): String {
        val keyBytes = INTERNAL_KEY.encodeToByteArray()
        val inputBytes = input.encodeToByteArray()
        val result = ByteArray(inputBytes.size)
        for (i in inputBytes.indices) {
            result[i] = (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return encodeBase64(result)
    }
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\util\TaxExporter.kt

```kt
package com.fordham.toolbelt.util

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.model.TaxExportOutcome

interface TaxExporter {
    suspend fun exportBentoReport(invoices: List<Invoice>, receipts: List<ReceiptItem>): TaxExportOutcome
    suspend fun exportFullTaxBundle(invoices: List<Invoice>, receipts: List<ReceiptItem>): TaxExportOutcome
}

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\util\UuidUtil.kt

```kt
package com.fordham.toolbelt.util

expect fun randomUUID(): String

```


---

## .\shared\src\commonMain\kotlin\com\fordham\toolbelt\util\VoiceAssistant.kt

```kt
package com.fordham.toolbelt.util

interface VoiceAssistant {
    fun speak(text: String)
    fun startListening(onResult: (String) -> Unit, onEnd: () -> Unit)
    fun stopListening()
    fun destroy()
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\data\AndroidDatabaseBuilder.kt

```kt
package com.fordham.toolbelt.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fordham.toolbelt.util.SecurityManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

// Note: We'll inject this via Koin
class AndroidDatabaseBuilder(
    private val context: Context,
    private val securityManager: SecurityManager
) {
    fun create(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = context.getDatabasePath("invoice_hammer.db")
        val passphrase = securityManager.getDatabasePassphrase().toByteArray()
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder<AppDatabase>(
            context = context,
            name = dbFile.absolutePath
        ).openHelperFactory(factory)
            .fallbackToDestructiveMigration()
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\data\implementation\AndroidDriveAuthTokenProvider.kt

```kt
package com.fordham.toolbelt.data.implementation

import android.content.Context
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.repository.DriveAccessToken
import com.fordham.toolbelt.domain.repository.DriveAuthTokenProvider
import com.fordham.toolbelt.domain.repository.DriveTokenOutcome
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidDriveAuthTokenProvider(
    private val context: Context
) : DriveAuthTokenProvider {
    override suspend fun getDriveAccessToken(): DriveTokenOutcome = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: return@withContext DriveTokenOutcome.Failure(
                FailureMessage("Sign in with Google before starting Drive backup.")
            )

        val accountName = account.account?.name
            ?: account.email
            ?: return@withContext DriveTokenOutcome.Failure(
                FailureMessage("Google account name is unavailable for Drive backup.")
            )

        return@withContext try {
            val token = GoogleAuthUtil.getToken(
                context,
                accountName,
                "oauth2:$DRIVE_APPDATA_SCOPE"
            )
            DriveTokenOutcome.Success(DriveAccessToken(token))
        } catch (e: UserRecoverableAuthException) {
            DriveTokenOutcome.Failure(
                FailureMessage("Drive permission is required. Sign out, sign in again, and approve Drive backup access.")
            )
        } catch (e: Exception) {
            DriveTokenOutcome.Failure(
                FailureMessage(e.message ?: "Unable to get Google Drive access token.")
            )
        }
    }

    private companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\data\implementation\AndroidStorageRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.fordham.toolbelt.domain.model.StorageOutcome
import com.fordham.toolbelt.domain.model.StorageBytesOutcome
import com.fordham.toolbelt.domain.repository.StorageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class AndroidStorageRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) : StorageRepository {

    override suspend fun saveBitmapBytesToPictures(imageBytes: ByteArray, prefix: String): StorageOutcome = withContext(ioDispatcher) {
        try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: throw IllegalArgumentException("Failed to decode image bytes")
            
            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
            val internalDir = File(context.filesDir, "vault/photos")
            if (!internalDir.exists()) internalDir.mkdirs()
            
            val file = File(internalDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            StorageOutcome.Success(file.absolutePath)
        } catch (e: Exception) {
            StorageOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save image bytes to internal storage"))
        }
    }

    override suspend fun getBytesFromUri(uriString: String): StorageBytesOutcome = withContext(ioDispatcher) {
        try {
            val uri = Uri.parse(uriString)
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: throw IllegalArgumentException("Failed to open input stream for URI")
            }
            val bytes = bitmap.let { bmp ->
                ByteArrayOutputStream().use { stream ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    stream.toByteArray()
                }
            }
            StorageBytesOutcome.Success(bytes)
        } catch (e: Exception) {
            StorageBytesOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to decode and retrieve image bytes from Uri"))
        }
    }

    override suspend fun saveUriToPictures(uriString: String, prefix: String): StorageOutcome = withContext(ioDispatcher) {
        try {
            val bytesResult = getBytesFromUri(uriString)
            if (bytesResult is StorageBytesOutcome.Success) {
                val saveResult = saveBitmapBytesToPictures(bytesResult.bytes, prefix)
                if (saveResult is StorageOutcome.Success) {
                    StorageOutcome.Success(saveResult.path)
                } else {
                    StorageOutcome.Failure((saveResult as StorageOutcome.Failure).error)
                }
            } else {
                StorageOutcome.Failure((bytesResult as StorageBytesOutcome.Failure).error)
            }
        } catch (e: Exception) {
            StorageOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save Uri to internal storage"))
        }
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\data\implementation\FirebaseAuthRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.AuthOutcome
import com.fordham.toolbelt.domain.repository.OperationError
import com.fordham.toolbelt.domain.repository.FordhamUser
import com.fordham.toolbelt.domain.repository.UserId
import com.fordham.toolbelt.domain.repository.EmailAddress
import com.fordham.toolbelt.domain.repository.DisplayName
import com.fordham.toolbelt.domain.repository.PhotoUrl
import com.fordham.toolbelt.domain.repository.IdToken
import com.fordham.toolbelt.domain.repository.IsPremium
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val _currentUser = MutableStateFlow(auth.currentUser?.toFordhamUser())
    override val currentUser: StateFlow<FordhamUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser?.toFordhamUser()
        }
    }

    override suspend fun signInWithGoogle(idToken: IdToken): AuthOutcome {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken.value, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user?.toFordhamUser()
            if (user != null) {
                AuthOutcome.Authenticated(user)
            } else {
                AuthOutcome.Failure(OperationError("User is null after sign in"))
            }
        } catch (e: Exception) {
            AuthOutcome.Failure(OperationError(e.message ?: "Google Sign-In failed"))
        }
    }

    override suspend fun signOut(): AuthOutcome {
        return try {
            auth.signOut()
            AuthOutcome.SignedOut
        } catch (e: Exception) {
            AuthOutcome.Failure(OperationError(e.message ?: "Sign out failed"))
        }
    }

    private fun com.google.firebase.auth.FirebaseUser.toFordhamUser() = FordhamUser(
        id = UserId(uid),
        email = email?.let { EmailAddress(it) },
        displayName = displayName?.let { DisplayName(it) },
        photoUrl = photoUrl?.toString()?.let { PhotoUrl(it) },
        isPremium = IsPremium(true)
    )
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\data\implementation\SettingsRepositoryImpl.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.SettingsDataStore
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.domain.model.SettingsOutcome
import com.fordham.toolbelt.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SettingsRepositoryImpl(
    private val dataStore: SettingsDataStore
) : SettingsRepository {
    override val businessSettingsFlow: Flow<BusinessSettings> = dataStore.businessSettingsFlow

    override suspend fun getBusinessSettings(): BusinessSettings = dataStore.businessSettingsFlow.first()

    override suspend fun saveBusinessSettings(settings: BusinessSettings): SettingsOutcome = try {
        dataStore.saveBusinessSettings(settings)
        SettingsOutcome.Success
    } catch (e: Exception) {
        SettingsOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save settings"))
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\data\SettingsDataStore.kt

```kt
package com.fordham.toolbelt.data

import android.content.SharedPreferences
import com.fordham.toolbelt.domain.model.BusinessSettings
import com.fordham.toolbelt.util.SecurityManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SettingsDataStore(
    private val securityManager: SecurityManager
) {
    private val encryptedPrefs = securityManager.getEncryptedPrefs()

    val businessSettingsFlow: Flow<BusinessSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(getSettings())
        }
        encryptedPrefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getSettings())
        awaitClose { encryptedPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun getSettings(): BusinessSettings {
        return BusinessSettings(
            businessName = encryptedPrefs.getString(BUSINESS_NAME, "") ?: "",
            businessSlogan = encryptedPrefs.getString(BUSINESS_SLOGAN, "") ?: "",
            businessPhone = encryptedPrefs.getString(BUSINESS_PHONE, "") ?: "",
            businessEmail = encryptedPrefs.getString(BUSINESS_EMAIL, "") ?: "",
            businessAddress = encryptedPrefs.getString(BUSINESS_ADDRESS, "") ?: "",
            isPremium = encryptedPrefs.getBoolean(IS_PREMIUM, false),
            taxRate = encryptedPrefs.getFloat(TAX_RATE, 0.0f).toDouble(),
            markupPercentage = encryptedPrefs.getFloat(MARKUP_RATE, 0.0f).toDouble(),
            logoUri = encryptedPrefs.getString(LOGO_URI, null),
            isDarkMode = encryptedPrefs.getBoolean(DARK_MODE, true),
            useMetricUnits = encryptedPrefs.getBoolean(USE_METRIC, false),
            notificationsEnabled = encryptedPrefs.getBoolean(NOTIFICATIONS, true)
        )
    }

    suspend fun saveBusinessSettings(settings: BusinessSettings) {
        encryptedPrefs.edit().apply {
            putString(BUSINESS_NAME, settings.businessName)
            putString(BUSINESS_SLOGAN, settings.businessSlogan)
            putString(BUSINESS_PHONE, settings.businessPhone)
            putString(BUSINESS_EMAIL, settings.businessEmail)
            putString(BUSINESS_ADDRESS, settings.businessAddress)
            putBoolean(IS_PREMIUM, settings.isPremium)
            putFloat(TAX_RATE, settings.taxRate.toFloat())
            putFloat(MARKUP_RATE, settings.markupPercentage.toFloat())
            putBoolean(DARK_MODE, settings.isDarkMode)
            putBoolean(USE_METRIC, settings.useMetricUnits)
            putBoolean(NOTIFICATIONS, settings.notificationsEnabled)
            
            if (settings.logoUri != null) {
                putString(LOGO_URI, settings.logoUri)
            } else {
                remove(LOGO_URI)
            }
        }.apply()
    }

    companion object {
        private const val BUSINESS_NAME = "business_name"
        private const val BUSINESS_SLOGAN = "business_slogan"
        private const val BUSINESS_PHONE = "business_phone"
        private const val BUSINESS_EMAIL = "business_email"
        private const val BUSINESS_ADDRESS = "business_address"
        private const val IS_PREMIUM = "is_premium"
        private const val TAX_RATE = "tax_rate"
        private const val MARKUP_RATE = "markup_rate"
        private const val LOGO_URI = "logo_uri"
        private const val DARK_MODE = "dark_mode"
        private const val USE_METRIC = "use_metric"
        private const val NOTIFICATIONS = "notifications_enabled"
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\di\PlatformModule.kt

```kt
package com.fordham.toolbelt.di

import android.content.Context
import androidx.room.RoomDatabase
import com.fordham.toolbelt.data.AppDatabase
import com.fordham.toolbelt.data.SettingsDataStore
import com.fordham.toolbelt.pdf.BentoReportEngine
import com.fordham.toolbelt.pdf.InvoiceEngine
import com.fordham.toolbelt.data.*
import com.fordham.toolbelt.data.implementation.DataStoreSettingsRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import com.fordham.toolbelt.data.implementation.AndroidStorageRepository
import com.fordham.toolbelt.data.implementation.AndroidDriveAuthTokenProvider
import com.fordham.toolbelt.data.implementation.FirebaseAuthRepository
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.DriveAuthTokenProvider
import com.fordham.toolbelt.util.VoiceAssistant
import com.fordham.toolbelt.util.*
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { Dispatchers.IO }
    single<SecretProvider> { AndroidSecretProvider(get()) }
    single<DriveAuthTokenProvider> { AndroidDriveAuthTokenProvider(get()) }
    single<PlatformActions> { AndroidPlatformActions(get()) }
    single<AuthRepository> { FirebaseAuthRepository() }
    single<VoiceAssistant> { AndroidVoiceAssistant(get()) }
    single { PlacesService() }
    single { SecurityManager(get()) }
    single { createDataStore { get<Context>().filesDir.resolve(DATASTORE_FILE_NAME).absolutePath } }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
    single<com.fordham.toolbelt.domain.repository.StorageRepository> { AndroidStorageRepository(get(), get()) }
    single<com.fordham.toolbelt.domain.repository.InvoiceEngine> { InvoiceEngine(get(), get()) }
    single { BentoReportEngine(get(), get()) }
    single<com.fordham.toolbelt.util.TaxExporter> { AndroidTaxExporter(get(), get()) }
    single<RoomDatabase.Builder<AppDatabase>> { AndroidDatabaseBuilder(get<Context>(), get()).create() }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\di\WorkerModule.kt

```kt
package com.fordham.toolbelt.di

import com.fordham.toolbelt.worker.UnpaidInvoiceWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val workerModule = module {
    workerOf(::UnpaidInvoiceWorker)
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\pdf\BentoReportEngine.kt

```kt
package com.fordham.toolbelt.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.fordham.toolbelt.domain.model.BentoReportData
import com.fordham.toolbelt.util.SecurityManager
import java.io.File
import java.io.FileOutputStream

class BentoReportEngine(
    private val context: Context,
    private val securityManager: SecurityManager
) {
    fun generateBentoPdf(data: BentoReportData): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        paint.color = Color.BLACK
        paint.textSize = 24f
        canvas.drawText("Bento Business Report", 50f, 50f, paint)
        
        paint.textSize = 14f
        canvas.drawText("Gross Income: ${data.grossIncome}", 50f, 100f, paint)
        canvas.drawText("Expenses: ${data.expenses}", 50f, 120f, paint)
        canvas.drawText("Net Profit: ${data.netProfit}", 50f, 140f, paint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Bento_Report.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\pdf\InvoiceEngine.kt

```kt
package com.fordham.toolbelt.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.fordham.toolbelt.domain.model.InvoiceData
import com.fordham.toolbelt.util.SecurityManager
import java.io.File
import java.io.FileOutputStream

class InvoiceEngine(
    private val context: Context,
    private val securityManager: SecurityManager
) : com.fordham.toolbelt.domain.repository.InvoiceEngine {
    
    override fun generatePdf(data: InvoiceData): String? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        
        // --- PAGE 1: BILLING SUMMARY ---
        val page1 = pdfDocument.startPage(pageInfo)
        val canvas1 = page1.canvas
        val paint = Paint().apply { 
            isAntiAlias = true 
            isFilterBitmap = true
            isDither = true
        }
        
        // Color Tokens
        val orangeColor = Color.rgb(252, 102, 0)
        val charcoalColor = Color.rgb(30, 30, 30)
        val grayColor = Color.rgb(100, 100, 100)
        val lightGrayColor = Color.rgb(245, 245, 245)
        
        // 1. Draw Business Details (Top Left)
        paint.color = orangeColor
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val bizName = data.settings.businessName.ifBlank { "INVOICE HAMMER" }.uppercase()
        canvas1.drawText(bizName, 50f, 60f, paint)
        
        paint.color = grayColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val slogan = data.settings.businessSlogan.ifBlank { "Professional Field Services" }
        canvas1.drawText(slogan, 50f, 75f, paint)
        
        paint.color = charcoalColor
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var contactOffset = 90f
        if (data.settings.businessPhone.isNotBlank()) {
            canvas1.drawText("P: ${data.settings.businessPhone}", 50f, contactOffset, paint)
            contactOffset += 13f
        }
        if (data.settings.businessEmail.isNotBlank()) {
            canvas1.drawText("E: ${data.settings.businessEmail}", 50f, contactOffset, paint)
            contactOffset += 13f
        }
        if (data.settings.businessAddress.isNotBlank()) {
            canvas1.drawText("A: ${data.settings.businessAddress}", 50f, contactOffset, paint)
        }
        
        // 2. Draw Logo (Top Right)
        val logoBitmap = decodeUriToBitmap(context, data.logoUriString ?: data.settings.logoUri, targetW = 160, targetH = 160)
        if (logoBitmap != null) {
            val scaledLogo = getResizedBitmap(logoBitmap, 80, 80)
            canvas1.drawBitmap(scaledLogo, 465f, 40f, paint)
            // Draw a thin charcoal border around logo
            paint.color = charcoalColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas1.drawRect(465f, 40f, 545f, 120f, paint)
            paint.style = Paint.Style.FILL
        }
        
        // Divider line
        paint.color = orangeColor
        paint.strokeWidth = 3f
        canvas1.drawLine(50f, 145f, 545f, 145f, paint)
        
        // 3. Client & Metadata Split Boxes (Y = 160f)
        // Billed To (Left Column)
        paint.color = orangeColor
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("BILLED TO:", 50f, 175f, paint)
        
        paint.color = charcoalColor
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText(data.clientName.uppercase(), 50f, 190f, paint)
        
        paint.color = grayColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText(data.clientAddress.ifBlank { "No Job Address Provided" }, 50f, 203f, paint)
        
        // Document Meta (Right Column)
        paint.color = orangeColor
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("DOCUMENT DETAILS:", 330f, 175f, paint)
        
        paint.color = charcoalColor
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val docType = if (data.isEstimate) "ESTIMATE" else "INVOICE"
        canvas1.drawText("$docType #${data.invoiceId.take(8).uppercase()}", 330f, 190f, paint)
        
        paint.color = grayColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("DATE: ${data.date}", 330f, 203f, paint)
        
        // 4. Line Items Table (Y = 230f)
        var currentY = 230f
        
        // Table Header
        paint.color = charcoalColor
        canvas1.drawRect(50f, currentY, 545f, currentY + 20f, paint)
        
        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("DESCRIPTION", 60f, currentY + 13f, paint)
        canvas1.drawText("CATEGORY", 320f, currentY + 13f, paint)
        drawTextRightAligned(canvas1, "TOTAL", 535f, currentY + 13f, paint)
        
        currentY += 20f
        
        // Table Rows
        data.items.forEachIndexed { idx, item ->
            // Background fill for alternating rows
            if (idx % 2 == 1) {
                paint.color = lightGrayColor
                canvas1.drawRect(50f, currentY, 545f, currentY + 22f, paint)
            }
            
            paint.color = charcoalColor
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            
            // Truncate description if too long
            val descText = if (item.description.length > 40) item.description.take(37) + "..." else item.description
            canvas1.drawText(descText, 60f, currentY + 14f, paint)
            
            paint.color = grayColor
            canvas1.drawText(item.category.uppercase(), 320f, currentY + 14f, paint)
            
            paint.color = charcoalColor
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val amtText = "$${String.format("%.2f", item.amount)}"
            drawTextRightAligned(canvas1, amtText, 535f, currentY + 14f, paint)
            
            // Draw a thin horizontal dividing line
            paint.color = Color.rgb(220, 220, 220)
            paint.strokeWidth = 0.5f
            canvas1.drawLine(50f, currentY + 22f, 545f, currentY + 22f, paint)
            
            currentY += 22f
        }
        
        // Calculations Block (Y starts after items list)
        currentY += 15f
        
        val subtotal = data.items.sumOf { it.amount }
        val taxAmount = subtotal * (data.taxRate / 100.0)
        val totalAmount = subtotal + taxAmount - data.deposit
        
        paint.color = grayColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        canvas1.drawText("Subtotal:", 330f, currentY, paint)
        drawTextRightAligned(canvas1, "$${String.format("%.2f", subtotal)}", 535f, currentY, paint)
        currentY += 14f
        
        canvas1.drawText("Tax (${data.taxRate}%):", 330f, currentY, paint)
        drawTextRightAligned(canvas1, "$${String.format("%.2f", taxAmount)}", 535f, currentY, paint)
        currentY += 14f
        
        if (data.deposit > 0.0) {
            canvas1.drawText("Deposit Collected:", 330f, currentY, paint)
            drawTextRightAligned(canvas1, "-$${String.format("%.2f", data.deposit)}", 535f, currentY, paint)
            currentY += 14f
        }
        
        // Grand Total Box
        currentY += 5f
        paint.color = orangeColor
        canvas1.drawRect(330f, currentY, 545f, currentY + 26f, paint)
        
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("TOTAL DUE:", 340f, currentY + 16f, paint)
        
        paint.textSize = 12f
        val grandTotalText = "$${String.format("%.2f", totalAmount)}"
        drawTextRightAligned(canvas1, grandTotalText, 535f, currentY + 17f, paint)
        
        // Centered Footer Thank You Note
        paint.color = grayColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("THANK YOU FOR YOUR BUSINESS!", 195f, 790f, paint)
        
        pdfDocument.finishPage(page1)
        
        // --- PAGE 2+: HIGH-RESOLUTION JOB SITE GALLERY ---
        // Galaxy-class screens (S25 Ultra: 500 DPI) upscale a standard 595×842 pt page ~3×.
        // Standard 240×130 pt cells = only 240×130 actual pixels → looks blocky when zoomed.
        // Fix: use a 3× resolution gallery page (1785×2526 pt) so each photo cell is
        // 720×390 real pixels — 9× more data, crisp on any screen.
        if (data.photoUris.isNotEmpty()) {
            val SCALE = 3f
            val galleryPageInfo = PdfDocument.PageInfo.Builder(
                (595 * SCALE).toInt(),   // 1785
                (842 * SCALE).toInt(),   // 2526
                1
            ).create()

            // Scaled color tokens (reuse from page 1)
            var photoIdx = 0
            var pageNum = 2

            while (photoIdx < data.photoUris.size) {
                val pageG = pdfDocument.startPage(galleryPageInfo)
                val canvasG = pageG.canvas

                // Reset Paint for Gallery
                paint.reset()
                paint.isAntiAlias = true
                paint.isFilterBitmap = true
                paint.isDither = true

                // Page Header  (all coords × SCALE)
                paint.color = orangeColor
                paint.textSize = 18f * SCALE
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvasG.drawText("JOB SITE GALLERY - PAGE ${pageNum - 1}", 50f * SCALE, 60f * SCALE, paint)

                paint.color = charcoalColor
                paint.strokeWidth = 2f * SCALE
                canvasG.drawLine(50f * SCALE, 72f * SCALE, 545f * SCALE, 72f * SCALE, paint)

                // Draw 2×4 grid (up to 8 photos per page)
                // Cell dimensions: 720×390 real pixels (was 240×130 at 1×)
                val cellW = 240f * SCALE   // 720
                val cellH = 130f * SCALE   // 390
                val colGap = 255f * SCALE  // 765  (col spacing matches old 255)
                val rowGap = 165f * SCALE  // 495  (row spacing matches old 165)
                val marginX = 50f * SCALE  // 150
                val gridStartY = 90f * SCALE // 270

                val photosOnThisPage = minOf(8, data.photoUris.size - photoIdx)
                for (i in 0 until photosOnThisPage) {
                    val currentUri = data.photoUris[photoIdx + i]
                    val row = i / 2
                    val col = i % 2

                    val x = marginX + col * colGap
                    val y = gridStartY + row * rowGap

                    // Decode at full cell resolution (720×390) for crisp output
                    val photoBitmap = decodeUriToBitmap(context, currentUri, targetW = 720, targetH = 390)
                    if (photoBitmap != null) {
                        val srcRect = android.graphics.Rect(0, 0, photoBitmap.width, photoBitmap.height)
                        val dstRect = android.graphics.RectF(x, y, x + cellW, y + cellH)
                        canvasG.drawBitmap(photoBitmap, srcRect, dstRect, paint)

                        // Border around photo
                        paint.color = charcoalColor
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 1.5f * SCALE
                        canvasG.drawRect(x, y, x + cellW, y + cellH, paint)
                        paint.style = Paint.Style.FILL

                        // Caption: left col = BEFORE (charcoal), right col = AFTER (orange)
                        if (col == 0) {
                            paint.color = charcoalColor
                            paint.textSize = 8f * SCALE
                            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            canvasG.drawText("[ BEFORE WORK ]", x + 5f * SCALE, y + cellH + 13f * SCALE, paint)
                        } else {
                            paint.color = orangeColor
                            paint.textSize = 8f * SCALE
                            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            canvasG.drawText("[ AFTER WORK ]", x + 5f * SCALE, y + cellH + 13f * SCALE, paint)
                        }
                    }
                }

                pdfDocument.finishPage(pageG)
                photoIdx += photosOnThisPage
                pageNum++
            }
        }

        
        // --- SAVE AND SHUT DOWN ---
        val internalDir = File(context.filesDir, "vault/invoices")
        if (!internalDir.exists()) internalDir.mkdirs()
        
        val file = File(internalDir, "${if (data.isEstimate) "Estimate" else "Invoice"}_${data.invoiceId}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file.absolutePath
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }
    
    /**
     * Decodes a URI or file path to a Bitmap, pre-sampled to avoid loading massive camera
     * images at full resolution. inSampleSize is calculated so the decoded bitmap is
     * at least 2x the target size, giving Bitmap.createScaledBitmap enough data for
     * high-quality bilinear filtering without wasting memory.
     *
     * Also reads EXIF orientation and rotates the bitmap to correct sideways/upside-down
     * photos — BitmapFactory.decodeStream ignores EXIF tags by default.
     */
    private fun decodeUriToBitmap(context: Context, uriStr: String?, targetW: Int = 240, targetH: Int = 130): Bitmap? {
        if (uriStr.isNullOrEmpty()) return null
        return try {
            val uri = android.net.Uri.parse(uriStr)
            // First pass: read bounds only to calculate inSampleSize
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
            val sampleSize = calculateInSampleSize(boundsOpts, targetW, targetH)
            // Second pass: decode at reduced size
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
            // Third pass: read EXIF orientation and correct rotation
            val rotationDeg = context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = androidx.exifinterface.media.ExifInterface(stream)
                exifToDegrees(exif.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                ))
            } ?: 0
            if (bitmap != null && rotationDeg != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotationDeg.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            // Fallback: file path
            try {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(uriStr, boundsOpts)
                val sampleSize = calculateInSampleSize(boundsOpts, targetW, targetH)
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bitmap = BitmapFactory.decodeFile(uriStr, decodeOpts)
                val exif = androidx.exifinterface.media.ExifInterface(uriStr)
                val rotationDeg = exifToDegrees(exif.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                ))
                if (bitmap != null && rotationDeg != 0) {
                    val matrix = Matrix()
                    matrix.postRotate(rotationDeg.toFloat())
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                } else {
                    bitmap
                }
            } catch (ex: Exception) {
                null
            }
        }
    }

    /** Converts an ExifInterface orientation constant to a clockwise rotation in degrees. */
    private fun exifToDegrees(exifOrientation: Int): Int = when (exifOrientation) {
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90  -> 90
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }


    /**
     * Calculates the largest inSampleSize that is a power of 2 and keeps the decoded
     * bitmap at least 2x the target dimensions, so bilinear filtering has enough
     * source data to produce a crisp result.
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val rawH = options.outHeight
        val rawW = options.outWidth
        var inSampleSize = 1
        if (rawH > reqHeight * 2 || rawW > reqWidth * 2) {
            val halfH = rawH / 2
            val halfW = rawW / 2
            while (halfH / inSampleSize >= reqHeight * 2 && halfW / inSampleSize >= reqWidth * 2) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Scales a bitmap to exactly newWidth x newHeight using bilinear filtering (filter=true).
     * Uses fit-inside (Math.min) to avoid stretching, then centre-crops to the exact cell.
     */
    private fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val scaleWidth = newWidth.toFloat() / bm.width
        val scaleHeight = newHeight.toFloat() / bm.height
        // fit-inside: pick the SMALLER scale so the whole image fits, then crop to cell
        val scale = minOf(scaleWidth, scaleHeight)
        val fittedW = (bm.width * scale).toInt().coerceAtLeast(1)
        val fittedH = (bm.height * scale).toInt().coerceAtLeast(1)
        // bilinear filtering via filter=true for crispness
        val fitted = Bitmap.createScaledBitmap(bm, fittedW, fittedH, true)
        // Centre-crop to exact cell dimensions
        val cropX = ((fittedW - newWidth) / 2).coerceAtLeast(0)
        val cropY = ((fittedH - newHeight) / 2).coerceAtLeast(0)
        val safeW = minOf(newWidth, fittedW - cropX)
        val safeH = minOf(newHeight, fittedH - cropY)
        return Bitmap.createBitmap(fitted, cropX, cropY, safeW, safeH)
    }
    
    private fun drawTextRightAligned(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(text, x, y, paint)
        paint.textAlign = Paint.Align.LEFT
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\util\AndroidPlatformActions.kt

```kt
package com.fordham.toolbelt.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class AndroidPlatformActions(private val context: Context) : PlatformActions {
    init {
        println("PLATFORM_ACTIONS: Instance created: ${this.hashCode()}")
    }
    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun shareFile(path: String, title: String) {
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    override fun openPdf(path: String) {
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun callPhone(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun sendEmail(email: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun requestPermission(permission: String, onGranted: () -> Unit) {
        // This usually needs to be handled in the Activity, 
        // but for now we'll provide the logic to check/ask.
        // In a real KMP app, we might use a library like MOKO Permissions.
    }

    override fun isPermissionGranted(permission: String): Boolean {
        val androidPermission = when(permission) {
            Permission.CAMERA -> android.Manifest.permission.CAMERA
            Permission.RECORD_AUDIO -> android.Manifest.permission.RECORD_AUDIO
            else -> return false
        }
        return androidx.core.content.ContextCompat.checkSelfPermission(context, androidPermission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun launchApp(packageName: String, fallbackUrl: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            openUrl(fallbackUrl)
        }
    }

    // Platform-specific hooks
    var activity: androidx.fragment.app.FragmentActivity? = null
    var googleSignInLauncher: ((onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit)? = null

    override fun signInWithGoogle(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        googleSignInLauncher?.invoke(onSuccess, onError) ?: onError("Sign-in launcher not registered")
    }

    override fun signOut() {
        // Sign out logic will be handled via AuthRepository, 
        // but we can clear platform-specific cache here if needed.
    }

    override fun authenticateBiometric(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        activity?.let {
            val authenticator = BiometricAuthenticator(it)
            authenticator.promptAuthenticate(title, subtitle, null, onSuccess, onError)
        } ?: onError("No active activity context")
    }

    override fun isBiometricAvailable(): Boolean {
        return activity?.let { BiometricAuthenticator(it).isBiometricAvailable() } ?: false
    }

    private var onImagePicked: ((String?) -> Unit)? = null
    var imagePickerLauncher: (() -> Unit)? = null
    var cameraLauncher: (() -> Unit)? = null

    fun handleImageResult(uri: String?) {
        onImagePicked?.invoke(uri)
    }

    override fun pickImage(onResult: (String?) -> Unit) {
        println("PLATFORM_ACTIONS: pickImage called")
        onImagePicked = onResult
        if (imagePickerLauncher == null) {
            println("PLATFORM_ACTIONS: imagePickerLauncher is NULL")
        }
        imagePickerLauncher?.invoke() ?: onResult(null)
    }

    override fun capturePhoto(onResult: (String?) -> Unit) {
        println("PLATFORM_ACTIONS: capturePhoto called")
        onImagePicked = onResult
        if (cameraLauncher == null) {
            println("PLATFORM_ACTIONS: cameraLauncher is NULL")
        }
        cameraLauncher?.invoke() ?: onResult(null)
    }

    override fun scheduleNotification(id: String, title: String, body: String, delayMillis: Long) {
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.fordham.toolbelt.worker.UnpaidInvoiceWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        ).setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            id,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\util\AndroidSecretProvider.kt

```kt
package com.fordham.toolbelt.util

import android.content.Context

class AndroidSecretProvider(private val context: Context) : SecretProvider {
    override fun getGeminiApiKey(): String {
        val key = NativeSecrets.getGeminiKey()
        check(key.isNotBlank()) { "Gemini API Key is missing from NativeSecrets" }
        return key
    }

    override fun getGeminiModelName(): String {
        // Model name can be managed here or fetched via remote config
        return "gemini-1.5-flash"
    }

    override fun getGoogleClientId(): String {
        return "716278040823-ngqvn2n3td42nrr6nbe4e3jlki348apa.apps.googleusercontent.com"
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\util\AndroidTaxExporter.kt

```kt
package com.fordham.toolbelt.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.fordham.toolbelt.domain.model.BentoReportData
import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.model.TaxExportOutcome
import com.fordham.toolbelt.pdf.BentoReportEngine
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AndroidTaxExporter(
    private val context: Context,
    private val bentoReportEngine: BentoReportEngine
) : TaxExporter {

    private val folderName = "Invoice Hammer"

    override suspend fun exportBentoReport(
        invoices: List<Invoice>,
        receipts: List<ReceiptItem>
    ): TaxExportOutcome = try {
        val paidInvoices = invoices.filter { it.isPaid && !it.isEstimate }
        val totalIncome = paidInvoices.sumOf { it.totalAmount }
        val totalExpenses = receipts.sumOf { it.totalPrice }
        
        val reportData = BentoReportData(
            netProfit = totalIncome - totalExpenses,
            grossIncome = totalIncome,
            expenses = totalExpenses,
            invoices = paidInvoices,
            receiptCount = receipts.size
        )

        val pdfFile = bentoReportEngine.generateBentoPdf(reportData) 
            ?: throw Exception("Failed to generate Bento PDF")

        if (Build.VERSION.SDK_INT >= 29) {
            try {
                val cv = ContentValues().apply { 
                    put(MediaStore.MediaColumns.DISPLAY_NAME, pdfFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/$folderName") 
                }
                context.contentResolver.insert(MediaStore.Files.getContentUri("external"), cv)?.let { uri ->
                    context.contentResolver.openOutputStream(uri)?.use { os -> 
                        pdfFile.inputStream().use { it.copyTo(os) }
                    }
                }
            } catch (e: Exception) { 
                e.printStackTrace() 
            }
        }
        TaxExportOutcome.Success(pdfFile.absolutePath)
    } catch (e: Exception) {
        TaxExportOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to generate report summary"))
    }

    override suspend fun exportFullTaxBundle(
        invoices: List<Invoice>,
        receipts: List<ReceiptItem>
    ): TaxExportOutcome = try {
        val name = "Tax_Bundle_${System.currentTimeMillis()}.zip"
        val cacheFile = File(context.cacheDir, name)
        
        ZipOutputStream(FileOutputStream(cacheFile).buffered()).use { zos ->
            val addedPaths = mutableSetOf<String>()

            // 1. Add Bento PDF Summary
            val summaryResult = exportBentoReport(invoices, receipts)
            if (summaryResult is TaxExportOutcome.Success) {
                val pdfEntry = "Business_Report.pdf"
                zos.putNextEntry(ZipEntry(pdfEntry))
                File(summaryResult.path).inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
                addedPaths.add(pdfEntry)
            }

            // 2. Add Invoice PDFs
            invoices.filter { it.pdfPath.isNotEmpty() }.distinctBy { it.pdfPath }.forEach { invoice ->
                val f = File(invoice.pdfPath)
                if (f.exists() && f.isFile && f.length() > 0) {
                    val entryPath = "Invoices/${f.name}"
                    if (addedPaths.add(entryPath)) {
                        try {
                            zos.putNextEntry(ZipEntry(entryPath))
                            f.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        } catch (e: Exception) {
                            try { zos.closeEntry() } catch (ex: Exception) {}
                        }
                    }
                }
            }
        }

        // 3. MediaStore Integration (API 29+)
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                val cv = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/$folderName")
                }
                context.contentResolver.insert(MediaStore.Files.getContentUri("external"), cv)?.let { uri ->
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        cacheFile.inputStream().use { it.copyTo(os) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        TaxExportOutcome.Success(cacheFile.absolutePath)
    } catch (e: Exception) {
        TaxExportOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to assemble tax bundle zip"))
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\util\AndroidVoiceAssistant.kt

```kt
package com.fordham.toolbelt.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class AndroidVoiceAssistant(private val context: Context) : TextToSpeech.OnInitListener, VoiceAssistant {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setPitch(0.9f)
            tts?.setSpeechRate(1.0f)
            isTtsReady = true
        } else {
            Log.e("VoiceAssistant", "TTS Initialization failed")
        }
    }

    override fun speak(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.w("VoiceAssistant", "TTS not ready yet")
        }
    }

    override fun startListening(onResult: (String) -> Unit, onEnd: () -> Unit) {
        speechRecognizer?.destroy()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p0: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(p0: Float) {}
                override fun onBufferReceived(p0: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(p0: Int) {
                    Log.e("VoiceAssistant", "SpeechRec Error code: $p0")
                    onEnd()
                }
                override fun onResults(p0: Bundle?) {
                    val m = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Log.d("VoiceAssistant", "SpeechRec Results: $m")
                    if (!m.isNullOrEmpty()) onResult(m[0])
                    onEnd()
                }
                override fun onPartialResults(p0: Bundle?) {}
                override fun onEvent(p0: Int, p1: Bundle?) {}
            })
            startListening(intent)
        }
    }

    override fun stopListening() {
        speechRecognizer?.stopListening()
    }

    override fun destroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\util\Base64Util.kt

```kt
package com.fordham.toolbelt.util

import android.util.Base64

actual fun encodeBase64(bytes: ByteArray): String {
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

actual fun decodeBase64(base64: String): ByteArray {
    return Base64.decode(base64, Base64.DEFAULT)
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\util\BiometricAuthenticator.kt

```kt
package com.fordham.toolbelt.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import javax.crypto.Cipher

class BiometricAuthenticator(private val activity: FragmentActivity) {

    private val executor: Executor = ContextCompat.getMainExecutor(activity)

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    fun promptAuthenticate(
        title: String,
        subtitle: String,
        cipher: Cipher? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Not a terminal error, just a failed attempt
            }
        })

        if (cipher != null) {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\util\FileUtil.kt

```kt
package com.fordham.toolbelt.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object FileUtil {
    fun shareFile(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val extension = file.extension.lowercase()
        val mimeType = when (extension) {
            "zip" -> "application/zip"
            "csv" -> "text/csv"
            "pdf" -> "application/pdf"
            else -> "*/*"
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "$title from Invoice Hammer")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share $title"))
    }

    fun openPdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "No PDF viewer found", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\util\IntegrityCheck.kt

```kt
package com.fordham.toolbelt.util

import android.content.Context
import android.os.Build
import android.os.Debug
import java.io.File

object IntegrityCheck {

    /**
     * Protocol: Check if the app is being debugged.
     */
    fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    /**
     * Protocol: Basic emulator detection to thwart automated dynamic analysis.
     */
    fun isRunningOnEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.contains("generic")
                || Build.FINGERPRINT.contains("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    /**
     * Protocol: Check for root access (Su binary).
     */
    fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\util\NativeSecrets.kt

```kt
package com.fordham.toolbelt.util

object NativeSecrets {
    init {
        System.loadLibrary("toolbelt-secrets")
    }

    external fun getGeminiKey(): String
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\util\OcrParser.kt

```kt
package com.fordham.toolbelt.util

import java.math.BigDecimal

object OcrParser {
    // Matches digits followed by a period and exactly two digits (e.g., 12.99, 1045.00)
    private val CURRENCY_REGEX = Regex("""\b\d+\.\d{2}\b""")

    fun extractMaximumPrice(rawText: String): BigDecimal {
        return CURRENCY_REGEX.findAll(rawText)
            .mapNotNull { it.value.toBigDecimalOrNull() }
            .maxOrNull() ?: BigDecimal.ZERO
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\util\SecurityManager.kt

```kt
package com.fordham.toolbelt.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import java.util.*

class SecurityManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_vault",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getEncryptedPrefs(): SharedPreferences = encryptedPrefs

    /**
     * Retrieves or generates a 64-character passphrase for SQLCipher.
     * This passphrase is stored in EncryptedSharedPreferences which is 
     * backed by the hardware-protected Android Keystore.
     */
    fun getDatabasePassphrase(): String {
        var passphrase = encryptedPrefs.getString(KEY_DB_PASSPHRASE, null)
        if (passphrase == null) {
            passphrase = generateSecurePassphrase()
            encryptedPrefs.edit().putString(KEY_DB_PASSPHRASE, passphrase).apply()
        }
        return passphrase
    }

    private fun generateSecurePassphrase(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(bytes)
        } else {
            android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        }
    }

    /**
     * Provision a Cipher linked to a biometric-required key.
     */
    fun getBiometricCipher(): Cipher {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        
        if (!keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                BIOMETRIC_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build()
            
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }

        val key = keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }

    companion object {
        private const val KEY_DB_PASSPHRASE = "db_passphrase_v1"
        private const val BIOMETRIC_KEY_ALIAS = "biometric_vault_key"
    }
}

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\util\UuidUtil.kt

```kt
package com.fordham.toolbelt.util

import java.util.UUID

actual fun randomUUID(): String = UUID.randomUUID().toString()

```


---

## .\shared\src\androidMain\kotlin\com\fordham\toolbelt\worker\UnpaidInvoiceWorker.kt

```kt
package com.fordham.toolbelt.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fordham.toolbelt.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.first

class UnpaidInvoiceWorker(
    context: Context,
    params: WorkerParameters,
    private val invoiceRepository: InvoiceRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val invoices = invoiceRepository.allInvoices.first()
            val unpaidInvoices = invoices.filter { !it.isPaid && !it.isEstimate }
                .sortedByDescending { it.lastUpdated }
                .take(3)

            if (unpaidInvoices.isNotEmpty()) {
                val message = unpaidInvoices.joinToString("\n") { 
                    "${it.clientName}: ${it.formattedTotal}" 
                }
                showNotification(message)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(message: String) {
        val channelId = "unpaid_invoices_reminder"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Unpaid Invoices",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for unpaid invoices"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Note: MainActivity is still in :app module, so we use a string-based intent or move it
        val intent = Intent().apply {
            setClassName(applicationContext.packageName, "com.fordham.toolbelt.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "HISTORY")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Unpaid Invoices Reminder")
            .setContentText("Tap to view unpaid invoices")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}

```


---

## .\shared\src\iosMain\kotlin\com\fordham\toolbelt\data\implementation\IosAuthRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.AuthOutcome
import com.fordham.toolbelt.domain.repository.OperationError
import com.fordham.toolbelt.domain.repository.FordhamUser
import com.fordham.toolbelt.domain.repository.UserId
import com.fordham.toolbelt.domain.repository.EmailAddress
import com.fordham.toolbelt.domain.repository.DisplayName
import com.fordham.toolbelt.domain.repository.PhotoUrl
import com.fordham.toolbelt.domain.repository.IdToken
import com.fordham.toolbelt.domain.repository.IsPremium
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.native.concurrent.ThreadLocal

class IosUserBridgeDto(
    val id: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isPremium: Boolean
)

interface IosAuthBridge {
    suspend fun signInWithGoogle(idToken: String): IosUserBridgeDto
    suspend fun signOut()
    fun getCurrentUser(): IosUserBridgeDto?
}

@ThreadLocal
object IosAuthServiceProvider {
    var bridge: IosAuthBridge? = null
}

class IosAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow<FordhamUser?>(null)
    override val currentUser: StateFlow<FordhamUser?> = _currentUser.asStateFlow()

    init {
        _currentUser.value = IosAuthServiceProvider.bridge?.getCurrentUser()?.toFordhamUser()
    }

    override suspend fun signInWithGoogle(idToken: IdToken): AuthOutcome {
        val bridge = IosAuthServiceProvider.bridge 
            ?: return AuthOutcome.Failure(OperationError("iOS Google Sign-In bridge not initialized"))
            
        return try {
            val dto = bridge.signInWithGoogle(idToken.value)
            val user = dto.toFordhamUser()
            _currentUser.value = user
            AuthOutcome.Authenticated(user)
        } catch (e: Exception) {
            AuthOutcome.Failure(OperationError(e.message ?: "Native iOS Auth Bridge Failed"))
        }
    }

    override suspend fun signOut(): AuthOutcome {
        val bridge = IosAuthServiceProvider.bridge 
            ?: return AuthOutcome.Failure(OperationError("iOS Google Sign-In bridge not initialized"))
            
        return try {
            bridge.signOut()
            _currentUser.value = null
            AuthOutcome.SignedOut
        } catch (e: Exception) {
            AuthOutcome.Failure(OperationError(e.message ?: "Native iOS Sign-Out Failed"))
        }
    }

    private fun IosUserBridgeDto.toFordhamUser() = FordhamUser(
        id = UserId(id),
        email = email?.let { EmailAddress(it) },
        displayName = displayName?.let { DisplayName(it) },
        photoUrl = photoUrl?.let { PhotoUrl(it) },
        isPremium = IsPremium(isPremium)
    )
}

```


---

## .\shared\src\iosMain\kotlin\com\fordham\toolbelt\data\implementation\IosDriveAuthTokenProvider.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.repository.DriveAccessToken
import com.fordham.toolbelt.domain.repository.DriveAuthTokenProvider
import com.fordham.toolbelt.domain.repository.DriveTokenOutcome
import kotlin.native.concurrent.ThreadLocal

interface IosDriveAuthBridge {
    suspend fun getDriveAccessToken(): String
}

@ThreadLocal
object IosDriveAuthServiceProvider {
    var bridge: IosDriveAuthBridge? = null
}

class IosDriveAuthTokenProvider : DriveAuthTokenProvider {
    override suspend fun getDriveAccessToken(): DriveTokenOutcome {
        val bridge = IosDriveAuthServiceProvider.bridge
            ?: return DriveTokenOutcome.Failure(
                FailureMessage("iOS Drive auth bridge is not initialized.")
            )

        return try {
            DriveTokenOutcome.Success(DriveAccessToken(bridge.getDriveAccessToken()))
        } catch (e: Exception) {
            DriveTokenOutcome.Failure(
                FailureMessage(e.message ?: "Unable to get iOS Drive access token.")
            )
        }
    }
}

```


---

## .\shared\src\iosMain\kotlin\com\fordham\toolbelt\data\implementation\IosStorageRepository.kt

```kt
package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.domain.model.StorageOutcome
import com.fordham.toolbelt.domain.model.StorageBytesOutcome
import com.fordham.toolbelt.domain.repository.StorageRepository
import platform.Foundation.*
import platform.UIKit.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = this.usePinned {
    NSData.create(bytes = it.addressOf(0), length = this.size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return bytes
}

class IosStorageRepository : StorageRepository {
    private val fileManager = NSFileManager.defaultManager

    override suspend fun saveBitmapBytesToPictures(imageBytes: ByteArray, prefix: String): StorageOutcome = try {
        val fileName = "${prefix}_${NSDate().timeIntervalSince1970}.jpg"
        val docsDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        val vaultDir = "$docsDir/vault/photos"
        
        if (!fileManager.fileExistsAtPath(vaultDir)) {
            fileManager.createDirectoryAtPath(vaultDir, withIntermediateDirectories = true, attributes = null, error = null)
        }
        
        val filePath = "$vaultDir/$fileName"
        val nsData = imageBytes.toNSData()
        nsData.writeToFile(filePath, atomically = true)
        
        StorageOutcome.Success(filePath)
    } catch (e: Exception) {
        StorageOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save image bytes to internal iOS storage"))
    }

    override suspend fun getBytesFromUri(uriString: String): StorageBytesOutcome = try {
        val nsData = NSData.dataWithContentsOfFile(uriString) 
            ?: throw Exception("Could not read file at $uriString")
        StorageBytesOutcome.Success(nsData.toByteArray())
    } catch (e: Exception) {
        StorageBytesOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to decode and retrieve image bytes on iOS"))
    }

    override suspend fun saveUriToPictures(uriString: String, prefix: String): StorageOutcome = try {
        val fileName = "${prefix}_${NSDate().timeIntervalSince1970}.jpg"
        val docsDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        val vaultDir = "$docsDir/vault/photos"
        
        if (!fileManager.fileExistsAtPath(vaultDir)) {
            fileManager.createDirectoryAtPath(vaultDir, withIntermediateDirectories = true, attributes = null, error = null)
        }
        
        val newFilePath = "$vaultDir/$fileName"
        fileManager.copyItemAtPath(uriString, newFilePath, error = null)
        
        StorageOutcome.Success(newFilePath)
    } catch (e: Exception) {
        StorageOutcome.Failure(com.fordham.toolbelt.domain.model.FailureMessage(e.message ?: "Failed to save Uri path to internal iOS storage"))
    }
}

```


---

## .\shared\src\iosMain\kotlin\com\fordham\toolbelt\data\IosDatabaseBuilder.kt

```kt
package com.fordham.toolbelt.data

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.NativeSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSHomeDirectory

fun getIosDatabaseBuilder(passphrase: String): RoomDatabase.Builder<AppDatabase> {
    val dbFile = NSHomeDirectory() + "/Documents/invoice_hammer.db"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile,
        factory =  { AppDatabase::class.instantiateImpl() }
    ).setDriver(NativeSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration()
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: SQLiteConnection) {
                db.prepare("PRAGMA key = '$passphrase'").step()
            }
        })
}

```


---

## .\shared\src\iosMain\kotlin\com\fordham\toolbelt\di\PlatformModule.kt

```kt
package com.fordham.toolbelt.di

import androidx.room.RoomDatabase
import com.fordham.toolbelt.data.AppDatabase
import com.fordham.toolbelt.data.implementation.IosAuthRepository
import com.fordham.toolbelt.data.implementation.IosDriveAuthTokenProvider
import com.fordham.toolbelt.data.implementation.IosStorageRepository
import com.fordham.toolbelt.domain.repository.AuthRepository
import com.fordham.toolbelt.domain.repository.DriveAuthTokenProvider
import com.fordham.toolbelt.domain.repository.StorageRepository
import com.fordham.toolbelt.util.IosPlatformActions
import com.fordham.toolbelt.util.PlatformActions
import com.fordham.toolbelt.data.*
import com.fordham.toolbelt.data.implementation.DataStoreSettingsRepository
import com.fordham.toolbelt.domain.repository.SettingsRepository
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import com.fordham.toolbelt.pdf.IosInvoiceEngine

import com.fordham.toolbelt.util.IosSecurityServiceProvider

actual fun platformModule(): Module = module {
    single<SecretProvider> { com.fordham.toolbelt.util.IosSecretProvider() }
    single<DriveAuthTokenProvider> { IosDriveAuthTokenProvider() }
    single<PlatformActions> { IosPlatformActions() }
    single<com.fordham.toolbelt.util.VoiceAssistant> { com.fordham.toolbelt.util.IosVoiceAssistant() }
    single { com.fordham.toolbelt.util.PlacesService() }
    single<AuthRepository> { IosAuthRepository() }
    single { 
        createDataStore { 
            val documentDirectory = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory, NSUserDomainMask, true
            ).first() as String
            "$documentDirectory/$DATASTORE_FILE_NAME"
        } 
    }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
    single<StorageRepository> { IosStorageRepository() }
    single<com.fordham.toolbelt.domain.repository.InvoiceEngine> { IosInvoiceEngine() }
    single<com.fordham.toolbelt.util.TaxExporter> { com.fordham.toolbelt.util.IosTaxExporter() }
    single<RoomDatabase.Builder<AppDatabase>> {
        val passphrase = IosSecurityServiceProvider.bridge?.getDatabasePassphrase()
        check(passphrase != null) { "iOS Security Bridge/Keychain is NOT initialized" }

        getIosDatabaseBuilder(passphrase)
    }
}

```


---

## .\shared\src\iosMain\kotlin\com\fordham\toolbelt\pdf\IosInvoiceEngine.kt

```kt
package com.fordham.toolbelt.pdf

import com.fordham.toolbelt.domain.model.InvoiceData
import com.fordham.toolbelt.domain.repository.InvoiceEngine
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*

class IosInvoiceEngine : InvoiceEngine {

    override fun generatePdf(data: InvoiceData): String? {
        val fileName = "${if (data.isEstimate) "Estimate" else "Invoice"}_${data.invoiceId}.pdf"
        val path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        val filePath = "$path/$fileName"

        val renderer = UIGraphicsPDFRenderer(bounds = CGRectMake(0.0, 0.0, 595.0, 842.0), format = UIGraphicsPDFRendererFormat())
        
        val pdfData = renderer.PDFDataWithActions { context ->
            // --- PAGE 1: BILLING SUMMARY ---
            context!!.beginPage()
            val cgContext = UIGraphicsGetCurrentContext()
            CGContextSetInterpolationQuality(cgContext, kCGInterpolationHigh)
            
            // Color Tokens
            val orangeColor = UIColor.colorWithRed(252.0 / 255.0, 102.0 / 255.0, 0.0 / 255.0, 1.0)
            val charcoalColor = UIColor.colorWithRed(30.0 / 255.0, 30.0 / 255.0, 30.0 / 255.0, 1.0)
            val grayColor = UIColor.colorWithRed(100.0 / 255.0, 100.0 / 255.0, 100.0 / 255.0, 1.0)
            val lightGrayColor = UIColor.colorWithRed(245.0 / 255.0, 245.0 / 255.0, 245.0 / 255.0, 1.0)
            
            // 1. Draw Business Details (Top Left)
            val bizName = (data.settings.businessName.ifBlank { "INVOICE HAMMER" }).uppercase()
            val bizAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(22.0),
                NSForegroundColorAttributeName to orangeColor
            )
            (bizName as NSString).drawAtPoint(CGPointMake(50.0, 40.0), withAttributes = bizAttributes as Map<Any?, *>)
            
            val slogan = data.settings.businessSlogan.ifBlank { "Professional Field Services" }
            val sloganAttributes = mapOf(
                NSFontAttributeName to UIFont.italicSystemFontOfSize(9.0),
                NSForegroundColorAttributeName to grayColor
            )
            (slogan as NSString).drawAtPoint(CGPointMake(50.0, 68.0), withAttributes = sloganAttributes as Map<Any?, *>)
            
            val contactAttributes = mapOf(
                NSFontAttributeName to UIFont.systemFontOfSize(10.0),
                NSForegroundColorAttributeName to charcoalColor
            )
            var contactOffset = 82.0
            if (data.settings.businessPhone.isNotBlank()) {
                ("P: ${data.settings.businessPhone}" as NSString).drawAtPoint(CGPointMake(50.0, contactOffset), withAttributes = contactAttributes as Map<Any?, *>)
                contactOffset += 13.0
            }
            if (data.settings.businessEmail.isNotBlank()) {
                ("E: ${data.settings.businessEmail}" as NSString).drawAtPoint(CGPointMake(50.0, contactOffset), withAttributes = contactAttributes as Map<Any?, *>)
                contactOffset += 13.0
            }
            if (data.settings.businessAddress.isNotBlank()) {
                ("A: ${data.settings.businessAddress}" as NSString).drawAtPoint(CGPointMake(50.0, contactOffset), withAttributes = contactAttributes as Map<Any?, *>)
            }
            
            // 2. Draw Logo (Top Right)
            val logoImage = data.logoUriString?.let { UIImage.imageWithContentsOfFile(it) }
                ?: data.settings.logoUri?.let { UIImage.imageWithContentsOfFile(it) }
            if (logoImage != null) {
                logoImage.drawInRect(CGRectMake(465.0, 40.0, 80.0, 80.0))
                // Draw a thin charcoal border around logo
                CGContextSetStrokeColorWithColor(cgContext, charcoalColor.CGColor)
                CGContextSetLineWidth(cgContext, 1.0)
                CGContextStrokeRect(cgContext, CGRectMake(465.0, 40.0, 80.0, 80.0))
            }
            
            // Divider line
            CGContextSetStrokeColorWithColor(cgContext, orangeColor.CGColor)
            CGContextSetLineWidth(cgContext, 3.0)
            CGContextBeginPath(cgContext)
            CGContextMoveToPoint(cgContext, 50.0, 145.0)
            CGContextAddLineToPoint(cgContext, 545.0, 145.0)
            CGContextStrokePath(cgContext)
            
            // 3. Client & Metadata Split Boxes (Y = 160.0)
            val sectionHeaderAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(10.0),
                NSForegroundColorAttributeName to orangeColor
            )
            val boldBodyAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(11.0),
                NSForegroundColorAttributeName to charcoalColor
            )
            val normalBodyAttributes = mapOf(
                NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                NSForegroundColorAttributeName to grayColor
            )
            
            // Billed To (Left Column)
            ("BILLED TO:" as NSString).drawAtPoint(CGPointMake(50.0, 165.0), withAttributes = sectionHeaderAttributes as Map<Any?, *>)
            (data.clientName.uppercase() as NSString).drawAtPoint(CGPointMake(50.0, 180.0), withAttributes = boldBodyAttributes as Map<Any?, *>)
            (data.clientAddress.ifBlank { "No Job Address Provided" } as NSString).drawAtPoint(CGPointMake(50.0, 193.0), withAttributes = normalBodyAttributes as Map<Any?, *>)
            
            // Document Meta (Right Column)
            ("DOCUMENT DETAILS:" as NSString).drawAtPoint(CGPointMake(330.0, 165.0), withAttributes = sectionHeaderAttributes as Map<Any?, *>)
            val docType = if (data.isEstimate) "ESTIMATE" else "INVOICE"
            ("$docType #${data.invoiceId.take(8).uppercase()}" as NSString).drawAtPoint(CGPointMake(330.0, 180.0), withAttributes = boldBodyAttributes as Map<Any?, *>)
            ("DATE: ${data.date}" as NSString).drawAtPoint(CGPointMake(330.0, 193.0), withAttributes = normalBodyAttributes as Map<Any?, *>)
            
            // 4. Line Items Table (Y = 230.0)
            var currentY = 230.0
            
            // Table Header
            CGContextSetFillColorWithColor(cgContext, charcoalColor.CGColor)
            CGContextFillRect(cgContext, CGRectMake(50.0, currentY, 495.0, 20.0))
            
            val tableHeaderAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(9.0),
                NSForegroundColorAttributeName to UIColor.whiteColor
            )
            ("DESCRIPTION" as NSString).drawAtPoint(CGPointMake(60.0, currentY + 4.0), withAttributes = tableHeaderAttributes as Map<Any?, *>)
            ("CATEGORY" as NSString).drawAtPoint(CGPointMake(320.0, currentY + 4.0), withAttributes = tableHeaderAttributes as Map<Any?, *>)
            ("TOTAL" as NSString).drawAtPoint(CGPointMake(495.0, currentY + 4.0), withAttributes = tableHeaderAttributes as Map<Any?, *>)
            
            currentY += 20.0
            
            // Table Rows
            data.items.forEachIndexed { idx, item ->
                if (idx % 2 == 1) {
                    CGContextSetFillColorWithColor(cgContext, lightGrayColor.CGColor)
                    CGContextFillRect(cgContext, CGRectMake(50.0, currentY, 495.0, 22.0))
                }
                
                val itemDescAttributes = mapOf(
                    NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                    NSForegroundColorAttributeName to charcoalColor
                )
                val itemCatAttributes = mapOf(
                    NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                    NSForegroundColorAttributeName to grayColor
                )
                val itemAmtAttributes = mapOf(
                    NSFontAttributeName to UIFont.boldSystemFontOfSize(9.0),
                    NSForegroundColorAttributeName to charcoalColor
                )
                
                val descText = if (item.description.length > 40) item.description.take(37) + "..." else item.description
                (descText as NSString).drawAtPoint(CGPointMake(60.0, currentY + 5.0), withAttributes = itemDescAttributes as Map<Any?, *>)
                (item.category.uppercase() as NSString).drawAtPoint(CGPointMake(320.0, currentY + 5.0), withAttributes = itemCatAttributes as Map<Any?, *>)
                
                val amtText = NSString.stringWithFormat("$%.2f", item.amount)
                (amtText as NSString).drawAtPoint(CGPointMake(495.0, currentY + 5.0), withAttributes = itemAmtAttributes as Map<Any?, *>)
                
                // Thin row divider line
                CGContextSetStrokeColorWithColor(cgContext, UIColor.colorWithRed(220.0/255.0, 220.0/255.0, 220.0/255.0, 1.0).CGColor)
                CGContextSetLineWidth(cgContext, 0.5)
                CGContextBeginPath(cgContext)
                CGContextMoveToPoint(cgContext, 50.0, currentY + 22.0)
                CGContextAddLineToPoint(cgContext, 545.0, currentY + 22.0)
                CGContextStrokePath(cgContext)
                
                currentY += 22.0
            }
            
            // Calculations Block
            currentY += 15.0
            val subtotal = data.items.sumOf { it.amount }
            val taxAmount = subtotal * (data.taxRate / 100.0)
            val totalAmount = subtotal + taxAmount - data.deposit
            
            val calcLabelAttributes = mapOf(
                NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                NSForegroundColorAttributeName to grayColor
            )
            
            ("Subtotal:" as NSString).drawAtPoint(CGPointMake(330.0, currentY), withAttributes = calcLabelAttributes as Map<Any?, *>)
            (NSString.stringWithFormat("$%.2f", subtotal) as NSString).drawAtPoint(CGPointMake(495.0, currentY), withAttributes = calcLabelAttributes as Map<Any?, *>)
            currentY += 14.0
            
            ("Tax (${data.taxRate}%):" as NSString).drawAtPoint(CGPointMake(330.0, currentY), withAttributes = calcLabelAttributes as Map<Any?, *>)
            (NSString.stringWithFormat("$%.2f", taxAmount) as NSString).drawAtPoint(CGPointMake(495.0, currentY), withAttributes = calcLabelAttributes as Map<Any?, *>)
            currentY += 14.0
            
            if (data.deposit > 0.0) {
                ("Deposit Collected:" as NSString).drawAtPoint(CGPointMake(330.0, currentY), withAttributes = calcLabelAttributes as Map<Any?, *>)
                (NSString.stringWithFormat("-$%.2f", data.deposit) as NSString).drawAtPoint(CGPointMake(495.0, currentY), withAttributes = calcLabelAttributes as Map<Any?, *>)
                currentY += 14.0
            }
            
            // Grand Total Box
            currentY += 5.0
            CGContextSetFillColorWithColor(cgContext, orangeColor.CGColor)
            CGContextFillRect(cgContext, CGRectMake(330.0, currentY, 215.0, 26.0))
            
            val totalBoxLabelAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(10.0),
                NSForegroundColorAttributeName to UIColor.whiteColor
            )
            val totalBoxValAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(12.0),
                NSForegroundColorAttributeName to UIColor.whiteColor
            )
            ("TOTAL DUE:" as NSString).drawAtPoint(CGPointMake(340.0, currentY + 6.0), withAttributes = totalBoxLabelAttributes as Map<Any?, *>)
            
            val grandTotalText = NSString.stringWithFormat("$%.2f", totalAmount)
            (grandTotalText as NSString).drawAtPoint(CGPointMake(490.0, currentY + 5.0), withAttributes = totalBoxValAttributes as Map<Any?, *>)
            
            // Footer text
            val footerAttributes = mapOf(
                NSFontAttributeName to UIFont.boldSystemFontOfSize(9.0),
                NSForegroundColorAttributeName to grayColor
            )
            ("THANK YOU FOR YOUR BUSINESS!" as NSString).drawAtPoint(CGPointMake(195.0, 790.0), withAttributes = footerAttributes as Map<Any?, *>)
            
            // --- PAGE 2+: DYNAMIC JOB SITE GALLERY ---
            if (data.photoUris.isNotEmpty()) {
                var photoIdx = 0
                var pageNum = 2
                
                while (photoIdx < data.photoUris.size) {
                    context.beginPage()
                    CGContextSetInterpolationQuality(cgContext, kCGInterpolationHigh)
                    
                    // Reset drawing color for header
                    val galleryTitleAttributes = mapOf(
                        NSFontAttributeName to UIFont.boldSystemFontOfSize(18.0),
                        NSForegroundColorAttributeName to orangeColor
                    )
                    ("JOB SITE GALLERY - PAGE ${pageNum - 1}" as NSString).drawAtPoint(CGPointMake(50.0, 45.0), withAttributes = galleryTitleAttributes as Map<Any?, *>)
                    
                    CGContextSetStrokeColorWithColor(cgContext, charcoalColor.CGColor)
                    CGContextSetLineWidth(cgContext, 2.0)
                    CGContextBeginPath(cgContext)
                    CGContextMoveToPoint(cgContext, 50.0, 72.0)
                    CGContextAddLineToPoint(cgContext, 545.0, 72.0)
                    CGContextStrokePath(cgContext)
                    
                    val photosOnThisPage = minOf(8, data.photoUris.size - photoIdx)
                    for (i in 0 until photosOnThisPage) {
                        val currentUri = data.photoUris[photoIdx + i]
                        val row = i / 2
                        val col = i % 2
                        
                        val x = 50.0 + col * 255.0
                        val y = 90.0 + row * 165.0
                        
                        val photoImage = UIImage.imageWithContentsOfFile(currentUri)
                        if (photoImage != null) {
                            photoImage.drawInRect(CGRectMake(x, y, 240.0, 130.0))
                            
                            // Thin border
                            CGContextSetStrokeColorWithColor(cgContext, charcoalColor.CGColor)
                            CGContextSetLineWidth(cgContext, 1.5)
                            CGContextStrokeRect(cgContext, CGRectMake(x, y, 240.0, 130.0))
                            
                            // Caption Under photo
                            if (col == 0) {
                                val captionAttributes = mapOf(
                                    NSFontAttributeName to UIFont.boldSystemFontOfSize(8.0),
                                    NSForegroundColorAttributeName to charcoalColor
                                )
                                ("[ BEFORE WORK ]" as NSString).drawAtPoint(CGPointMake(x + 5.0, y + 138.0), withAttributes = captionAttributes as Map<Any?, *>)
                            } else {
                                val captionAttributes = mapOf(
                                    NSFontAttributeName to UIFont.boldSystemFontOfSize(8.0),
                                    NSForegroundColorAttributeName to orangeColor
                                )
                                ("[ AFTER WORK ]" as NSString).drawAtPoint(CGPointMake(x + 5.0, y + 138.0), withAttributes = captionAttributes as Map<Any?, *>)
                            }
                        }
                    }
                    
                    photoIdx += photosOnThisPage
                    pageNum++
                }
            }
        }

        return if (pdfData.writeToFile(filePath, atomically = true)) {
            filePath
        } else {
            null
        }
    }
}

```


---

## .\shared\src\iosMain\kotlin\com\fordham\toolbelt\util\Base64Util.kt

```kt
package com.fordham.toolbelt.util

import platform.Foundation.NSData
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual fun encodeBase64(bytes: ByteArray): String {
    val data = bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    }
    return data.base64EncodedStringWithOptions(0UL)
}

@OptIn(ExperimentalForeignApi::class)
actual fun decodeBase64(base64: String): ByteArray {
    val data = NSData.create(base64EncodedString = base64, options = 0UL)
        ?: return ByteArray(0)
    
    return ByteArray(data.length.toInt()).apply {
        usePinned { pinned ->
            platform.Foundation.memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
    }
}

```


---

## .\shared\src\iosMain\kotlin\com\fordham\toolbelt\util\IosPlatformActions.kt

```kt
package com.fordham.toolbelt.util

import platform.Foundation.*
import platform.UIKit.*
import platform.AVFoundation.*
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.darwin.*
import platform.UserNotifications.*

class IosPlatformActions : PlatformActions {
    private fun runOnMain(block: () -> Unit) {
        dispatch_async(dispatch_get_main_queue()) {
            block()
        }
    }

    override fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    }

    override fun shareFile(path: String, title: String) {
        val url = NSURL.fileURLWithPath(path)
        val activityViewController = UIActivityViewController(listOf(url), null)
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(activityViewController, animated = true, completion = null)
    }

    override fun openPdf(path: String) {
        // QuickLook would be better, but UIDocumentInteractionController is simpler for a bridge
        val url = NSURL.fileURLWithPath(path)
        val interactionController = UIDocumentInteractionController.interactionControllerWithURL(url)
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.let {
            interactionController.delegate = null // Simple case
            interactionController.presentPreviewAnimated(true)
        }
    }

    override fun callPhone(phoneNumber: String) {
        openUrl("tel:$phoneNumber")
    }

    override fun sendEmail(email: String) {
        openUrl("mailto:$email")
    }

    override fun requestPermission(permission: String, onGranted: () -> Unit) {
        when (permission) {
            Permission.RECORD_AUDIO -> {
                AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                    if (granted) runOnMain(onGranted)
                }
            }
            Permission.CAMERA -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    if (granted) runOnMain(onGranted)
                }
            }
            else -> onGranted()
        }
    }

    override fun isPermissionGranted(permission: String): Boolean {
        return when (permission) {
            Permission.RECORD_AUDIO -> AVAudioSession.sharedInstance().recordPermission() == AVAudioSessionRecordPermissionGranted
            Permission.CAMERA -> AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized
            else -> true
        }
    }

    override fun showToast(message: String) {
        // iOS doesn't have "Toasts". Usually done with a temporary alert or a custom view.
        val alert = UIAlertController.alertControllerWithTitle(null, message, UIAlertControllerStyleAlert)
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(alert, animated = true) {
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 2_000_000_000L), dispatch_get_main_queue()) {
                alert.dismissViewControllerAnimated(true, null)
            }
        }
    }

    override fun launchApp(packageName: String, fallbackUrl: String) {
        // iOS uses Custom URL Schemes, not package names.
        openUrl(fallbackUrl)
    }

    override fun signInWithGoogle(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val bridge = IosPlatformActionsServiceProvider.bridge
        if (bridge != null) {
            bridge.signInWithGoogle(
                onSuccess = { token -> runOnMain { onSuccess(token) } },
                onError = { message -> runOnMain { onError(message) } }
            )
        } else {
            onError("IosPlatformActionsBridge not initialized")
        }
    }

    override fun signOut() {
        // Sign out logic
    }

    override fun authenticateBiometric(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val context = LAContext()
        var error: NSError? = null
        if (context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)) {
            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = title
            ) { success, evalError ->
                if (success) {
                    runOnMain(onSuccess)
                } else {
                    runOnMain {
                        onError(evalError?.localizedDescription ?: "Authentication failed")
                    }
                }
            }
        } else {
            onError("Biometrics not available")
        }
    }

    override fun isBiometricAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)
    }

    private var imagePickerDelegate: ImagePickerDelegate? = null

    override fun pickImage(onResult: (String?) -> Unit) {
        if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceTypePhotoLibrary)) {
            presentImagePicker(UIImagePickerControllerSourceTypePhotoLibrary, onResult)
        } else {
            showToast("Photo library not available")
            onResult(null)
        }
    }

    override fun capturePhoto(onResult: (String?) -> Unit) {
        if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceTypeCamera)) {
            presentImagePicker(UIImagePickerControllerSourceTypeCamera, onResult)
        } else {
            showToast("Camera not available on this device")
            onResult(null)
        }
    }

    private fun presentImagePicker(sourceType: UIImagePickerControllerSourceType, onResult: (String?) -> Unit) {
        val picker = UIImagePickerController()
        picker.sourceType = sourceType
        
        imagePickerDelegate = ImagePickerDelegate { path ->
            onResult(path)
            imagePickerDelegate = null // Clear reference when done
        }
        
        picker.delegate = imagePickerDelegate

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootViewController != null) {
            rootViewController.presentViewController(picker, animated = true, completion = null)
        } else {
            onResult(null)
        }
    }

    override fun scheduleNotification(id: String, title: String, body: String, delayMillis: Long) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
            if (granted) {
                val content = UNMutableNotificationContent().apply {
                    setTitle(title)
                    setBody(body)
                    setSound(UNNotificationSound.defaultSound)
                }

                val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                    delayMillis.toDouble() / 1000.0,
                    repeats = false
                )

                val request = UNNotificationRequest.requestWithIdentifier(id, content, trigger)
                center.addNotificationRequest(request, null)
            }
        }
    }
}

class ImagePickerDelegate(
    private val onImagePicked: (String?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        if (image != null) {
            val data = UIImageJPEGRepresentation(image, 0.8)
            val tempDir = NSTemporaryDirectory()
            val fileName = NSUUID().UUIDString + ".jpg"
            val path = tempDir + fileName
            data?.writeToFile(path, atomically = true)
            onImagePicked(path)
        } else {
            onImagePicked(null)
        }
        picker.dismissViewControllerAnimated(true, null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        onImagePicked(null)
        picker.dismissViewControllerAnimated(true, null)
    }
}

```


---

## .\shared\src\iosMain\kotlin\com\fordham\toolbelt\util\IosPlatformActionsBridge.kt

```kt
package com.fordham.toolbelt.util

import kotlin.native.concurrent.ThreadLocal

interface IosPlatformActionsBridge {
    fun signInWithGoogle(onSuccess: (String) -> Unit, onError: (String) -> Unit)
}

@ThreadLocal
object IosPlatformActionsServiceProvider {
    var bridge: IosPlatformActionsBridge? = null
}

```


---

## .\shared\src\iosMain\kotlin\com\fordham\toolbelt\util\IosSecretProvider.kt

```kt
package com.fordham.toolbelt.util

class IosSecretProvider : SecretProvider {
    override fun getGeminiApiKey(): String {
        val key = IosSecurityServiceProvider.bridge
            ?.getSecret(GEMINI_API_KEY)
            .orEmpty()
        check(key.isNotBlank()) { "Gemini API Key is missing for iOS" }
        return key
    }

    override fun getGeminiModelName(): String {
        return "gemini-1.5-flash"
    }

    override fun getGoogleClientId(): String {
        return "716278040823-ngqvn2n3td42nrr6nbe4e3jlki348apa.apps.googleusercontent.com"
    }

    private companion object {
        const val GEMINI_API_KEY = "gemini_api_key"
    }
}

```


---

## .\shared\src\iosMain\kotlin\com\fordham\toolbelt\util\IosSecurityServiceProvider.kt

```kt
package com.fordham.toolbelt.util

import kotlin.native.concurrent.ThreadLocal

interface IosSecurityBridge {
    fun getDatabasePassphrase(): String
    fun saveSecret(key: String, value: String)
    fun getSecret(key: String): String?
}

@ThreadLocal
object IosSecurityServiceProvider {
    var bridge: IosSecurityBridge? = null
}

```


---

## .\shared\src\iosMain\kotlin\com\fordham\toolbelt\util\IosTaxExporter.kt

```kt
package com.fordham.toolbelt.util

import com.fordham.toolbelt.domain.model.Invoice
import com.fordham.toolbelt.domain.model.ReceiptItem
import com.fordham.toolbelt.domain.model.TaxExportOutcome

class IosTaxExporter : TaxExporter {
    
    override suspend fun exportBentoReport(
        invoices: List<Invoice>,
        receipts: List<ReceiptItem>
    ): TaxExportOutcome {
        // Concrete bridge stub for iOS Bento Report Generation
        return TaxExportOutcome.Failure(
            com.fordham.toolbelt.domain.model.FailureMessage("Tax Bento PDF report generation is not currently supported on iOS.")
        )
    }

    override suspend fun exportFullTaxBundle(
        invoices: List<Invoice>,
        receipts: List<ReceiptItem>
    ): TaxExportOutcome {
        // Concrete bridge stub for iOS Full ZIP Tax Bundle Generation
        return TaxExportOutcome.Failure(
            com.fordham.toolbelt.domain.model.FailureMessage("ZIP Tax bundle export is not currently supported on iOS.")
        )
    }
}

```


---

## .\shared\src\iosMain\kotlin\com\fordham\toolbelt\util\IosVoiceAssistant.kt

```kt
package com.fordham.toolbelt.util

import platform.AVFoundation.*
import platform.Foundation.*
import platform.Speech.*
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_async

class IosVoiceAssistant : VoiceAssistant {
    private val synthesizer = AVSpeechSynthesizer()
    private val speechRecognizer = SFSpeechRecognizer(NSLocale.localeWithLocaleIdentifier("en-US"))
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private val audioEngine = AVAudioEngine()

    private fun runOnMain(block: () -> Unit) {
        dispatch_async(dispatch_get_main_queue()) {
            block()
        }
    }

    override fun speak(text: String) {
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
        
        // Ensure session is set for playback
        val audioSession = AVAudioSession.sharedInstance()
        audioSession.setCategory(AVAudioSessionCategoryPlayback, error = null)
        audioSession.setActive(true, error = null)
        
        synthesizer.speakUtterance(utterance)
    }

    override fun startListening(onResult: (String) -> Unit, onEnd: () -> Unit) {
        if (speechRecognizer?.isAvailable() == false) {
            SFSpeechRecognizer.requestAuthorization { status ->
                if (status == SFSpeechRecognizerAuthorizationStatusAuthorized) {
                    runOnMain {
                        startListening(onResult, onEnd)
                    }
                } else {
                    runOnMain(onEnd)
                }
            }
            return
        }

        if (recognitionTask != null) {
            recognitionTask?.cancel()
            recognitionTask = null
        }

        val audioSession = AVAudioSession.sharedInstance()
        try {
            audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker, error = null)
            audioSession.setMode(AVAudioSessionModeMeasurement, error = null)
            audioSession.setActive(true, withOptions = AVAudioSessionCategoryOptionNotifyOthersOnDeactivation, error = null)
        } catch (e: Exception) {
            onEnd()
            return
        }

        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        val inputNode = audioEngine.inputNode
        
        recognitionTask = speechRecognizer?.recognitionTaskWithRequest(recognitionRequest!!) { result, error ->
            if (result != null) {
                runOnMain {
                    onResult(result.bestTranscription.formattedString)
                }
            }
            
            if (error != null || (result?.isFinal() == true)) {
                stopListening()
                runOnMain(onEnd)
            }
        }

        val recordingFormat = inputNode.outputFormatForBus(0u)
        inputNode.removeTapOnBus(0u) // Defensive
        inputNode.installTapOnBus(0u, bufferSize = 1024u, format = recordingFormat) { buffer, _ ->
            recognitionRequest?.appendAudioPCMBuffer(buffer!!)
        }

        audioEngine.prepare()
        try {
            audioEngine.startAndReturnError(null)
        } catch (e: Exception) {
            onEnd()
        }
    }

    override fun stopListening() {
        if (audioEngine.isRunning()) {
            audioEngine.stop()
            audioEngine.inputNode.removeTapOnBus(0u)
        }
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        recognitionTask = null
        recognitionRequest = null
    }

    override fun destroy() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundaryImmediate)
        stopListening()
    }
}

```


---

## .\shared\src\iosMain\kotlin\com\fordham\toolbelt\util\UuidUtil.kt

```kt
package com.fordham.toolbelt.util

import platform.Foundation.NSUUID

actual fun randomUUID(): String = NSUUID().UUIDString()

```


---

## .\composeApp\src\iosMain\kotlin\com\fordham\toolbelt\MainViewController.kt

```kt
package com.fordham.toolbelt

import androidx.compose.ui.window.ComposeUIViewController
import com.fordham.toolbelt.data.implementation.IosAuthServiceProvider
import com.fordham.toolbelt.di.initKoin
import com.fordham.toolbelt.di.viewModelModule
import com.fordham.toolbelt.util.IosPlatformActionsServiceProvider
import com.fordham.toolbelt.util.IosSecurityServiceProvider
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}

fun initKoinIos() {
    checkIosBridgesInitialized()
    initKoin(
        additionalModules = listOf(viewModelModule)
    )
}

private fun checkIosBridgesInitialized() {
    check(IosAuthServiceProvider.bridge != null) { "iOS auth bridge must be registered before initKoinIos()." }
    check(IosSecurityServiceProvider.bridge != null) { "iOS security bridge must be registered before initKoinIos()." }
    check(IosPlatformActionsServiceProvider.bridge != null) { "iOS platform actions bridge must be registered before initKoinIos()." }
}

```


---

## .\iosApp\iosApp\ContentView.swift

```swift
import SwiftUI
import LocalAuthentication
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct VaultLockView: View {
    @Binding var isAuthenticated: Bool
    @State private var errorMessage: String? = nil

    var body: some View {
        ZStack {
            Color(red: 0.06, green: 0.06, blue: 0.06)
                .ignoresSafeArea()

            VStack(spacing: 24) {
                Image(systemName: "lock.fill")
                    .font(.system(size: 64))
                    .foregroundColor(Color(red: 1.0, green: 0.84, blue: 0.0)) // Gold

                Text("Vault Locked")
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundColor(.white)

                if let error = errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                }

                Button(action: authenticate) {
                    Text("Unlock Vault")
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .padding()
                        .frame(maxWidth: 200)
                        .background(Color.blue)
                        .cornerRadius(12)
                }
            }
        }
        .onAppear {
            authenticate()
        }
    }

    func authenticate() {
        let context = LAContext()
        var error: NSError?

        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: "Authenticate to access Invoice Hammer") { success, authenticationError in
                DispatchQueue.main.async {
                    if success {
                        isAuthenticated = true
                    } else {
                        errorMessage = authenticationError?.localizedDescription ?? "Authentication failed"
                    }
                }
            }
        } else {
            // Biometrics not available, fallback to passcode
            context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: "Unlock to access Invoice Hammer") { success, authenticationError in
                DispatchQueue.main.async {
                    if success {
                        isAuthenticated = true
                    } else {
                        errorMessage = "Authentication required"
                    }
                }
            }
        }
    }
}

struct ContentView: View {
    @State private var isAuthenticated = false

    var body: some View {
        if isAuthenticated {
            ComposeView()
                .ignoresSafeArea(.all, edges: .bottom)
        } else {
            VaultLockView(isAuthenticated: $isAuthenticated)
        }
    }
}

```


---

## .\iosApp\iosApp\FirebaseAuthBridge.swift

```swift
import Foundation
import ComposeApp
import FirebaseAuth
import GoogleSignIn

class FirebaseAuthBridge: IosAuthBridge {
    
    func signInWithGoogle(idToken: String) async throws -> IosUserBridgeDto {
        // Exchange the idToken from Google for Firebase credentials
        let credential = GoogleAuthProvider.credential(withIDToken: idToken,
                                                       accessToken: nil) // Access token is optional for ID Token exchange
        
        let result = try await Auth.auth().signIn(with: credential)
        return IosUserBridgeDto(
            id: result.user.uid,
            email: result.user.email,
            displayName: result.user.displayName,
            photoUrl: result.user.photoURL?.absoluteString,
            isPremium: true
        )
    }
    
    func signOut() async throws {
        try Auth.auth().signOut()
        GIDSignIn.sharedInstance.signOut()
    }
    
    func getCurrentUser() -> IosUserBridgeDto? {
        guard let firebaseUser = Auth.auth().currentUser else { return nil }
        return IosUserBridgeDto(
            id: firebaseUser.uid,
            email: firebaseUser.email,
            displayName: firebaseUser.displayName,
            photoUrl: firebaseUser.photoURL?.absoluteString,
            isPremium: true
        )
    }
}

```


---

## .\iosApp\iosApp\GoogleService-Info.plist

```plist
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>CLIENT_ID</key>
	<string>716278040823-6afkdloh5qs359mr896tl725ge7gl8h0.apps.googleusercontent.com</string>
	<key>REVERSED_CLIENT_ID</key>
	<string>com.googleusercontent.apps.716278040823-6afkdloh5qs359mr896tl725ge7gl8h0</string>
	<key>ANDROID_CLIENT_ID</key>
	<string>716278040823-pksk3q4s3g2n40t52ordl99oj49oc6es.apps.googleusercontent.com</string>
	<key>API_KEY</key>
	<string>AIzaSyAgvmA9HNfzk8oAma_6pI44ZPZodz-FTYo</string>
	<key>GCM_SENDER_ID</key>
	<string>716278040823</string>
	<key>PLIST_VERSION</key>
	<string>1</string>
	<key>BUNDLE_ID</key>
	<string>com.fordham.toolbelt</string>
	<key>PROJECT_ID</key>
	<string>toolbelt-3812f381</string>
	<key>STORAGE_BUCKET</key>
	<string>toolbelt-3812f381.firebasestorage.app</string>
	<key>IS_ADS_ENABLED</key>
	<false></false>
	<key>IS_ANALYTICS_ENABLED</key>
	<false></false>
	<key>IS_APPINVITE_ENABLED</key>
	<true></true>
	<key>IS_GCM_ENABLED</key>
	<true></true>
	<key>IS_SIGNIN_ENABLED</key>
	<true></true>
	<key>GOOGLE_APP_ID</key>
	<string>1:716278040823:ios:a63b8193aa9de15f33ebf2</string>
</dict>
</plist>

```


---

## .\iosApp\iosApp\Info.plist

```plist
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDisplayName</key>
    <string>Invoice Hammer</string>
    <key>CFBundleName</key>
    <string>Invoice Hammer</string>
    <key>NSCameraUsageDescription</key>
    <string>We need access to your camera to scan receipts and capture job site photos.</string>
    <key>NSPhotoLibraryUsageDescription</key>
    <string>We need access to your photo library to attach existing images to your invoices and suppliers.</string>
    <key>NSFaceIDUsageDescription</key>
    <string>FaceID is used to securely unlock your encrypted Invoice Hammer Vault.</string>
    <key>NSSpeechRecognitionUsageDescription</key>
    <string>We use speech recognition to allow you to dictate invoice line items and notes hands-free.</string>
    <key>NSMicrophoneUsageDescription</key>
    <string>We need microphone access for voice-to-text dictation of invoices and expenses.</string>
    <key>CFBundleURLTypes</key>
    <array>
        <dict>
            <key>CFBundleTypeRole</key>
            <string>Editor</string>
            <key>CFBundleURLSchemes</key>
            <array>
                <string>com.googleusercontent.apps.716278040823-6afkdloh5qs359mr896tl725ge7gl8h0</string>
            </array>
        </dict>
    </array>
</dict>
</plist>

```


---

## .\iosApp\iosApp\iOSApp.swift

```swift
import SwiftUI
import ComposeApp
import FirebaseCore

@main
struct iOSApp: App {
    init() {
        // Initialize Firebase
        FirebaseApp.configure()
        
        // Register iOS-specific bridges before Koin can resolve platform services.
        IosAuthServiceProvider.shared.bridge = FirebaseAuthBridge()
        IosSecurityServiceProvider.shared.bridge = KeyChainSecurityBridge()
        IosPlatformActionsServiceProvider.shared.bridge = PlatformActionsBridge()

        // Initialize KMP Shared Layer after native bridges are available.
        MainViewControllerKt.initKoinIos()
        
        // Apply Hardware File Protection to the Database
        secureDatabaseFile()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
    
    private func secureDatabaseFile() {
        let fileManager = FileManager.default
        let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        let dbURL = documentsURL.appendingPathComponent("invoice_hammer.db")
        let walURL = documentsURL.appendingPathComponent("invoice_hammer.db-wal")
        let shmURL = documentsURL.appendingPathComponent("invoice_hammer.db-shm")
        
        let files = [dbURL, walURL, shmURL]
        
        for file in files {
            if fileManager.fileExists(atPath: file.path) {
                do {
                    var attributes = try fileManager.attributesOfItem(atPath: file.path)
                    let protection = attributes[.protectionKey] as? FileProtectionType
                    if protection != .complete {
                        try fileManager.setAttributes([.protectionKey: FileProtectionType.complete], ofItemAtPath: file.path)
                        print("Applied NSFileProtectionComplete to \(file.lastPathComponent)")
                    }
                } catch {
                    print("Failed to secure \(file.lastPathComponent): \(error)")
                }
            }
        }
    }
}

```


---

## .\iosApp\iosApp\KeyChainSecurityBridge.swift

```swift
import Foundation
import ComposeApp
import Security

class KeyChainSecurityBridge: IosSecurityBridge {
    
    private let service = "com.fordham.toolbelt.db"
    private let account = "db_passphrase_v1"

    func getDatabasePassphrase() -> String {
        if let existing = readValue(account: account) {
            return existing
        } else {
            let newPassphrase = UUID().uuidString + UUID().uuidString // 72 chars
            saveValue(newPassphrase, account: account)
            return newPassphrase
        }
    }

    func saveSecret(key: String, value: String) {
        saveValue(value, account: key)
    }

    func getSecret(key: String) -> String? {
        readValue(account: key)
    }

    private func saveValue(_ value: String, account: String) {
        let data = value.data(using: .utf8)!
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlock
        ]
        
        SecItemDelete(query as CFDictionary)
        SecItemAdd(query as CFDictionary, nil)
    }

    private func readValue(account: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var dataTypeRef: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &dataTypeRef)
        
        if status == errSecSuccess, let data = dataTypeRef as? Data {
            return String(data: data, encoding: .utf8)
        }
        return nil
    }
}

```


---

## .\iosApp\iosApp\PlatformActionsBridge.swift

```swift
import Foundation
import ComposeApp
import GoogleSignIn
import UIKit

class PlatformActionsBridge: NSObject, IosPlatformActionsBridge {
    
    func signInWithGoogle(onSuccess: @escaping (String) -> Void, onError: @escaping (String) -> Void) {
        guard let rootViewController = UIApplication.shared.windows.first?.rootViewController else {
            onError("Root view controller not found")
            return
        }
        
        GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController) { result, error in
            if let error = error {
                onError(error.localizedDescription)
                return
            }
            
            guard let user = result?.user,
                  let idToken = user.idToken?.tokenString else {
                onError("Failed to obtain ID token")
                return
            }
            
            onSuccess(idToken)
        }
    }
}

```

