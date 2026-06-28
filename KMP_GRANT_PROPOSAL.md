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

## 🤖 Securing Edge-AI & Local LLM Workflows
With the emergence of mobile-first AI features, local LLM execution, and client-side generative AI integration (such as Google’s Gemini SDK and on-device Llama models), KMP developers face a critical threat vector: **AI Data Leakage on the Edge**.

Applications running offline voice transcription, contractor job-memory logs, or automated text generation handle massive amounts of sensitive user transcripts, customer contracts, and billing records directly in RAM and offline storage. `:secure-vault` is designed specifically to secure this next generation of mobile applications:
* **Obfuscated AI Key Management**: Prevents reverse-engineering of cloud Gemini API keys or server endpoints by storing private key material inside the device's hardware enclave.
* **SQLCipher Database Encryption**: Encrypts offline context caches (e.g. database-linked LLM memory logs and vector transcripts) so they cannot be retrieved by unauthorized actors on rooted or jailbroken devices.
* **Asynchronous Thread Confinement**: Ensures that heavy AI processing pipelines (which consume significant CPU/RAM) never block the UI thread during key resolution, maintaining smooth 60fps rendering even under heavy on-device model loads.

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

* **Open-Source Distribution**:
  Published and compiled on JitPack. The package is ready for multiplatform integration:
  `implementation("com.gitlab.Justin1028c.invoice-hammer:secure-vault:1.0.0")`
  * GitLab Repository: [Justin1028c/invoice-hammer](https://gitlab.com/Justin1028c/invoice-hammer)
  * JitPack Release: [JitPack Package Page](https://jitpack.io/#com.gitlab.Justin1028c/invoice-hammer)
* **Architecture Walkthrough Video**:
  A full walkthrough of the codebase architecture, expect/actual parity setup, binary compatibility API verification, and build compilation proof.
  * Demo Video: [KMP Secure Vault Demo (Google Drive)](https://drive.google.com/file/d/1P8dOjEBrAG_W-q5Qqx3swI2cuU8WKrEa/view?usp=sharing)
* **Live API Reference Documentation**:
  API documentation is built via Dokka V2 and auto-deployed via GitLab CI/CD on every push.
  * Live Documentation Site: [secure-vault API Docs Reference](https://justin1028c.gitlab.io/invoice-hammer/api-docs/)
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

