plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("com.android.library")
    id("maven-publish")
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.jetbrains.dokka)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "securevault"
            isStatic = true
        }
    }

    explicitApi()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
        
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        
        androidMain.dependencies {
            implementation(libs.androidx.security.crypto)
        }
        
        iosMain.dependencies {
            // Native platform dependencies are resolved automatically
        }
    }
}

android {
    namespace = "com.fordham.toolbelt.securevault"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

publishing {
    publications {
        matching { it.name in listOf("kotlinMultiplatform", "androidRelease", "iosArm64", "iosX64", "iosSimulatorArm64") }
            .all {
                val publication = this as? MavenPublication ?: return@all
                publication.pom {
                    name.set("secure-vault")
                    description.set("Hardware-backed security library for Kotlin Multiplatform")
                    url.set("https://github.com/Justin1028c/invoice-hammer")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("justin1028c")
                            name.set("Justin")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/Justin1028c/invoice-hammer.git")
                        developerConnection.set("scm:git:ssh://github.com/Justin1028c/invoice-hammer.git")
                        url.set("https://github.com/Justin1028c/invoice-hammer")
                    }
                }
            }
    }
}
