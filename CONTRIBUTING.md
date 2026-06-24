# Contributing to Secure Vault

First off, thank you for considering contributing to `:secure-vault`! It's people like you who make the Kotlin Multiplatform ecosystem an amazing developer space.

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

## How Can I Contribute?

### Reporting Bugs
* Check the existing issues/merge requests to ensure the bug hasn't already been reported.
* Open a new issue detailing:
  * Steps to reproduce the bug.
  * Expected vs. actual behavior.
  * Platform target details (Android SDK version, iOS version, Kotlin version).

### Submitting Code & Feature Requests
* Fork the repository and create your branch from `main`.
* **API Stability**: If you modify public API interfaces under `:secure-vault`, you MUST run the binary compatibility tasks:
  * Run `./gradlew :secure-vault:apiDump` to generate updated signatures.
  * Add the updated `api/secure-vault.api` to your pull request.
* **Testing**: Write unit tests under `commonTest` for any new logic or boundary wrappers. Run tests locally using `./gradlew :secure-vault:testDebugUnitTest`.
* **Documentation**: Document public classes, functions, and properties with proper KDoc. Verify that Dokka compiles documentation cleanly via `./gradlew :secure-vault:dokkaHtml`.
* Ensure your code adheres to standard Kotlin style guides.

## Core Development Standards

1. **Clean Architecture Firewall**: Keep common Main contracts pure. Do not import platform-specific dependencies in `commonMain`.
2. **Eradicate Primitive Obsession**: Box IDs and credentials inside `@JvmInline value class` wrappers.
3. **Explicit Concurrency Control**: Never use implicit dispatchers or blocking I/O calls (`runBlocking`) inside platform vaults. Inject background dispatchers explicitly.

## License
By contributing to this repository, you agree that your contributions will be licensed under the Apache License 2.0.
