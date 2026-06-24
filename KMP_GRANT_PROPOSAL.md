# Kotlin Foundation Grant Submission — Project Proposal

**Project Name:** `:secure-vault`
**Author:** Justin
**Target Platform Parity:** Android (minSdk 26), iOS (Arm64, Simulator, X64), JVM/Desktop
**License:** Apache 2.0 (Open Source)

---

## 💡 Executive Summary
The Kotlin Multiplatform (KMP) ecosystem has seen rapid adoption, yet secure hardware-backed storage continues to rely on fragmented, platform-specific custom integrations. Developers are often forced to choose between simplistic Key/Value wrappers (which compromise security) or complex native implementations that leak platform details and block the main UI thread during database pre-warming or key resolution.

`:secure-vault` is a production-grade, lightweight, and zero-overhead KMP library designed to bridge the domain-specific security firewall. It exposes a pure Kotlin boundary for hardware-backed credential storage (Android Keystore / iOS Keychain Services) while aggressively enforcing modern Kotlin design paradigms: **domain purity, explicit concurrency ownership, primitive eradication, and Swift-safe interoperability.**

---

## 🛠️ Core Architectural Pillars & Innovations

### 1. Clean Architecture Firewall
The public library APIs reside strictly in the `commonMain` source set. The library enforces a rigid boundary separating domain logic from platform details:
* **Purity**: Zero Java, Android, or iOS UIKit/Foundation classes leak into common contracts.
* **PlatformContext wrapping**: Employs an expect/actual `PlatformContext` class that wraps Android `Context` and iOS `NSObject`. This architecture prevents the expect-actual modality mismatch compilation error commonly encountered when using platform types directly in expect class signatures.

### 2. Eradication of Primitive Obsession
To prevent developer mistakes (such as passing a plain string key where a value is expected) and ensure compile-time contract verification:
* Every identifier and secure payload is encapsulated in `@JvmInline value class` wrappers: `SecretKeyLabel` and `SecretValue`.
* Under JVM targets, these compile to raw primitives, ensuring zero performance or allocation overhead while providing maximum type safety during development.

### 3. Explicit Concurrency & Thread Confinement
Mobile platforms penalize blocking I/O and cryptographic operations. `:secure-vault` eliminates main-thread starvation:
* The factory initializer `createSecureVault` enforces the injection of an explicit `CoroutineDispatcher`.
* Both `AndroidSecureVault` and `IosSecureVault` run entirely within `withContext(ioDispatcher)`, ensuring that all cryptographic computations and filesystem writes (EncryptedSharedPreferences / SecItemAdd) occur off the main UI thread.

### 4. Asynchronous Database Provider Integration
During database setup, most developers utilize `runBlocking` to fetch encryption keys, causing deadlocks on iOS or Application Not Responding (ANR) crashes on Android. `:secure-vault` supports a deferred, asynchronous Room/SQLCipher pre-warming model:
* Database builders are pure, synchronous, and stateless configurations consuming `ByteArray` passphrases.
* Key resolution is delegated to a suspending, non-blocking `DatabaseProvider` in `commonMain` utilizing a coroutine `Mutex` to safely coordinate concurrent calls.

### 5. Swift-Safe Ergonomics
The library is designed for absolute ergonomics when bridged to iOS Swift/Objective-C:
* **Sealed Interface Outcome**: The library exposes a typed, non-generic `VaultOperationResult` interface (containing `Success` and `Failure` data classes). This avoids generic sealed wrappers (e.g. `Result<T>`) which bridge poorly to Swift, ensuring a clean 1:1 Swift Enum translation.

---

## 📊 KMP Ecosystem Compliance & Toolchain Setup

To meet the Kotlin Foundation's strict package distribution metrics, the library includes a complete JetBrains-grade verification pipeline:

* **Explicit API Mode (`explicitApi()`)**:
  Active in Gradle configurations. Enforces that all public-facing definitions specify explicit visibility (`public`, `internal`) and explicit return types, guarding against API leaks.
* **Binary Compatibility Validation**:
  Integrated the `kotlinx-binary-compatibility-validator` plugin. Tracks API stability against a generated baseline signature dump (`api/secure-vault.api`), ensuring future updates maintain backward compatibility.
* **Dokka KDoc Compiler**:
  100% KDoc coverage across public gateway contracts, parameters, and inline types. Builds Dokka HTML reference guides cleanly.
* **Unit Testing Coverage**:
  Cross-platform unit tests configured in `commonTest` (utilizing `kotlinx-coroutines-test` and a lightweight mock `FakeSecureVault`), ensuring platform-independent correctness.
* **Automated CI/CD Verification**:
  Active GitLab CI/CD YAML configuration that compiles modules, executes unit tests, verifies binary compatibility, and automatically publishes updated Dokka HTML documents directly to GitLab Pages on every release branch commit.
