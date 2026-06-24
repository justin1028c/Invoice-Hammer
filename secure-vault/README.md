# :secure-vault — KMP Hardware-Backed Storage Library

`:secure-vault` is a Kotlin Multiplatform (KMP) library that provides secure, hardware-backed credential storage utilizing the **Android Keystore (EncryptedSharedPreferences)** and **iOS Keychain Services** with strict thread-confinement guarantees and Swift-safe API design.

---

## 🚀 Quick Start — Non-Blocking Room/SQLCipher Passphrase Pre-warming

To prevent main-thread blockings, deadlocks on iOS, and Android Application Not Responding (ANRs) during database initialization, use the asynchronous `DatabaseProvider` pattern:

```kotlin
// commonMain/kotlin/.../DatabaseProvider.kt
public class DatabaseProvider(
    private val secureVault: SecureVaultGateway,
    private val platformContext: PlatformContext,
    private val ioDispatcher: CoroutineDispatcher
) {
    private var databaseInstance: AppDatabase? = null
    private val mutex = Mutex()

    public suspend fun getDatabase(): AppDatabase {
        return databaseInstance ?: mutex.withLock {
            databaseInstance ?: initializeDatabase().also { databaseInstance = it }
        }
    }

    private suspend fun initializeDatabase(): AppDatabase {
        val label = SecretKeyLabel("db_passphrase_v1")
        val secretValue = when (val result = secureVault.retrieveSecret(label)) {
            is VaultOperationResult.Success -> result.secret.value
            is VaultOperationResult.Failure -> {
                val newPassphrase = generateSecurePassphrase()
                when (val storeResult = secureVault.storeSecret(label, SecretValue(newPassphrase))) {
                    is VaultOperationResult.Success -> newPassphrase
                    is VaultOperationResult.Failure -> throw IllegalStateException("Key storage failed")
                }
            }
        }
        
        return buildRoomDatabase(secretValue.encodeToByteArray(), platformContext)
    }
}
```

```kotlin
// Platform Builders (Synchronous but Purely Functional)
internal expect fun buildRoomDatabase(passphrase: ByteArray, context: PlatformContext): AppDatabase

// androidMain Android Implementation
internal actual fun buildRoomDatabase(passphrase: ByteArray, context: PlatformContext): AppDatabase {
    val factory = SupportOpenHelperFactory(passphrase)
    return Room.databaseBuilder<AppDatabase>(context.context, "invoice_hammer.db")
        .openHelperFactory(factory)
        .build()
}

// iosMain iOS Implementation
internal actual fun buildRoomDatabase(passphrase: ByteArray, context: PlatformContext): AppDatabase {
    val dbFile = NSHomeDirectory() + "/Documents/invoice_hammer.db"
    return Room.databaseBuilder<AppDatabase>(name = dbFile, factory = { AppDatabaseConstructor.initialize() })
        .setDriver(BundledSQLiteDriver())
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: SQLiteConnection) {
                db.prepare("PRAGMA key = '${passphrase.decodeToString().replace("'", "''")}'").step()
            }
        })
        .build()
}
```

---

## 🛠️ Installation

Add the dependency to your version catalog (`libs.versions.toml`):

```toml
[plugins]
binary-compatibility-validator = { id = "org.jetbrains.kotlinx.binary-compatibility-validator", version = "0.17.0" }
jetbrains-dokka = { id = "org.jetbrains.dokka", version = "2.0.0" }
```

Include `:secure-vault` in your `:shared` module's dependencies:

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(project(":secure-vault"))
    }
}
```

---

## 🛡️ Key Features

1. **Domain Purity**: 100% Kotlin Common APIs. No platform namespaces leaked.
2. **Explicit Concurrency Control**:INITIALIZATION requires an explicit `CoroutineDispatcher` ensuring cryptographic execution blocks are bound to background thread pools.
3. **Eradication of Primitive Obsession**: Public boundaries enforce `@JvmInline value class SecretKeyLabel` and `SecretValue` to prevent developer errors at boundaries.
4. **Swift Interoperability**: Non-generic sealed interface outcomes (`VaultOperationResult`) map to clean native enums when bridged.
5. **Open Source Compliant**: Tracks signature dumps via the JetBrains `binary-compatibility-validator` and Dokka KDoc engines.

---

## 📄 License
This library is licensed under the **Apache License 2.0** - see the root `LICENSE` file for details.
