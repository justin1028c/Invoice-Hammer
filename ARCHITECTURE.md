# Digital Toolbelt — Architecture Audit & Flow Document
**Project:** InvoiceApp (`com.fordham.toolbelt`)  
**Version:** 1.0 (Clean Architecture Sync Complete)  
**Last Audit:** October 2023

## 1. Executive Summary
Digital Toolbelt is a professional-grade field service management application built using **Modern Android Development (MAD)** principles. It follows a strict **Clean Architecture** pattern to separate business logic from implementation details, enabling AI-driven receipt parsing, automated PDF generation, and secure financial data management.

---

## 2. High-Level Architecture
The project is divided into three primary layers, following the Dependency Rule (dependencies point inward).

### A. UI Layer (Presentation)
- **Framework:** 100% Jetpack Compose.
- **State Management:** Unidirectional Data Flow (UDF) via `MainViewModel`.
- **Navigation:** Type-safe Compose Navigation 2.8+ using `Screen` sealed classes.
- **Entry Point:** `MainActivity.kt` acts as the single Activity host and provides system-level services (STT, File Sharing).

### B. Domain Layer (Business Logic - Pure Kotlin)
- **Models:** Plain data classes in `domain.model`. No Room or Android dependencies.
- **Interfaces:** Repository definitions in `domain.repository`.
- **Use Cases:** Single-responsibility logic classes in `domain.usecase` (e.g., `SaveInvoiceUseCase`, `ProcessReceiptUseCase`).
- **Audit Note:** This layer is the "source of truth" and contains no external framework logic.

### C. Data Layer (Implementation)
- **Framework:** Room (SQLite) with KSP.
- **Entities:** Room-annotated classes in `data/`.
- **DAOs:** Data Access Objects for local persistence.
- **Implementations:** `data.implementation` contains concrete Room repositories that map internal entities to domain models using `Mappers.kt`.
- **Integrations:** `GeminiParser` (AI), `SettingsDataStore` (Preferences), `InvoiceEngine` (PDF).

---

## 3. Data Flow Audit (Standard Operation)

### Example: Processing a Receipt
1.  **UI:** User triggers `onProcessReceipt` in `ReceiptsTab`.
2.  **ViewModel:** `MainViewModel.processCapturedReceipt()` is called.
3.  **Use Case:** ViewModel delegates to `ProcessReceiptUseCase.execute(bitmap)`.
4.  **AI Integration:** Use Case calls `GeminiParser` to extract JSON from the image.
5.  **Persistence:** Use Case calls `StorageRepository` to save the image and `ReceiptRepository` to save the metadata.
6.  **Implementation:** `RoomReceiptRepository` converts the Domain model to a Room Entity via `Mappers.kt` and writes to `ReceiptDao`.
7.  **Reactive UI:** The `ReceiptDao` emits an updated `Flow`, which propagates back through the Repository and ViewModel to update the UI.

---

## 4. Dependency Injection (Hilt)
Dependency injection is managed via three primary modules:

| Module | Responsibility |
| :--- | :--- |
| **AppModule** | Provides Singletons: `AppDatabase`, DAOs, `TaxExporter`, `InvoiceEngine`, `GeminiParser`. |
| **RepositoryModule** | Abstract module using `@Binds` to link Domain Interfaces to Data Implementations. |
| **UseCaseModule** | Provides Use Case instances to the ViewModel. |

---

## 5. Technical Stack & Safety
- **Database Version:** 13 (with Migration 12 -> 13).
- **Security:** `FileProvider` for sharing; `BuildConfig` for AI API keys.
- **Concurrency:** Coroutines/Flow. Business logic runs on `Dispatchers.IO`; UI updates on `Dispatchers.Main`.
- **Performance:** Pre-calculated StateFlows in ViewModel avoid recomposition lag.

---

## 6. Migration Status (Audit Pass)
| Module | Pattern | Status |
| :--- | :--- | :--- |
| Client | Clean Architecture | ✅ 100% Migrated |
| Invoice | Clean Architecture | ✅ 100% Migrated |
| Receipt | Clean Architecture | ✅ 100% Migrated |
| Service | Clean Architecture | ✅ 100% Migrated |
| JobNote | Clean Architecture | ✅ 100% Migrated |
| JobPhoto | Clean Architecture | ✅ 100% Migrated |

---

## 7. Maintenance Rules (For Future Audits)
1. **Rule L1:** Domain layer must never import `android.*` or `data.*`.
2. **Rule L2:** All business logic must live in ViewModels or Use Cases.
3. **Rule L3:** Use `Mappers.kt` for all data transformations.
4. **Rule L4:** Always bump Room version and add a Migration object for schema changes.
5. **Rule L5:** UI state must be managed via `StateFlow` per-screen classes.

---
**Audit Status: COMPLIANT**  
*The Digital Toolbelt architecture currently meets production-grade standards for modularity, testability, and scalability.*
