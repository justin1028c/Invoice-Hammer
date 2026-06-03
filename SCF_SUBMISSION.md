# Invoice Hammer — Stellar Community Fund (SCF) submission

## Project URL & Core Links
* **Repository Link**: `https://gitlab.com/Justin1028c/invoice-hammer`
* **Direct Submission File**: `https://gitlab.com/Justin1028c/invoice-hammer/-/blob/main/SCF_SUBMISSION.md`


---

## 1. Project Hook & Value Proposition
Invoice Hammer is a non-custodial, peer-to-peer invoice staging and settlement application designed for contractors and merchants on the Stellar network. By routing payments directly over Stellar's native USDC rails, Invoice Hammer eliminates traditional merchant fee markups (1.5% - 3.5%) and provides instant, secure, on-chain settlement directly to the contractor's self-custodied wallet.

---

## 2. Technical Architecture & Custody Model
The application is built using a strict Kotlin Multiplatform (KMP) Clean Architecture structure, ensuring a highly testable business core separated from platform frameworks.

```mermaid
graph TD
    subgraph Client Device (iOS / Android)
        subgraph UI Layer (composeApp)
            VM[PaymentViewModel]
            UI[PaymentLedgerSheet]
        end
        subgraph Pure Domain (commonMain)
            RepoContract[PaymentRepository Interface]
            Entities[Invoice & PaymentRequest Entities]
            Outcomes[Sealed Payment Outcomes]
        end
        subgraph Data & Platform Layers (shared)
            RepoImpl[MockPaymentRepository]
            DB[Room KMP Database + SQLCipher]
            Enclave[Secure Enclave / Keystore Bridge]
        end
    end
    subgraph External Infrastructure
        PP[PowerPay Gateway / Event Webhook]
        Stellar((Stellar Network / USDC Ledger))
    end

    UI -->|Displays State| VM
    VM -->|Invokes| RepoContract
    RepoImpl -.->|Implements| RepoContract
    RepoImpl -->|Reads/Writes| DB
    RepoImpl -->|Requests Signature| Enclave
    RepoImpl -->|Dispatches Staged Tx| PP
    PP -->|Submits Tx & Relays Hash| Stellar
    Stellar -.->|Confirms Transaction| PP
    PP -.->|Triggers Webhook Event| RepoImpl
```

### Custody and Security Specifications
* **Key Custody**: Zero central custody. All private keys for signing transactions remain strictly on the user's local secure storage. Key generation and cryptographic signing are performed using the **iOS Keychain / Secure Enclave** and **Android Keystore**, exposed to the shared codebase through KMP `expect`/`actual` declarations.
* **On-Chain Verification**: Invoices are staged as Stellar transactions locally, cryptographically signed, and published to the network. The transaction hash is stored in a local, encrypted Room database (using **SQLCipher**) as verifiable cryptographic proof of payment.

---

## 3. Stellar Ecosystem Alignment & Value Add
* **Immediate Settlement**: Transactions settle within 5 seconds on the Stellar network, removing the standard multi-day processing delay of traditional payment providers.
* **Network Fees**: Avoids arbitrary fee extraction. Invoice Hammer charges zero platform markups, letting contractors keep 100% of their invoiced revenue, paying only standard Stellar network fees (a fraction of a cent per transaction).
* **Future SEP Integration**: Staged to align with Stellar standards like SEP-24 and SEP-31 to support direct merchant cash-out rails to localized bank accounts.

---

## 4. Development Roadmap & Tranches

* **Tranche 1 (MVP - Days 1–30)**: Core KMP setup, Room database configuration with SQLCipher encryption, UI invoice creation screens, and local PDF invoice generation.
* **Tranche 2 (Testnet - Days 31–60)**: Native secure key storage bridge (Secure Enclave / Android Keystore), Mock PowerPay client implementation, and testnet transaction staging and execution.
* **Tranche 3 (Mainnet & Production - Days 61–90)**: Production UI/UX implementation, third-party security audits of cryptographic bridges, and final mainnet deployment.

---

## 5. Technical Background & Team Capability
The development team consists of systems engineers specialized in high-performance Kotlin Multiplatform development, cryptographically secure storage patterns, and clean architecture enforcement. The repository strictly separates concerns to guarantee stability, testability, and multiplatform compile completeness for both Android and iOS targets.

---

## 6. Open Source Alignment & Licensing
Invoice Hammer is committed to the open-source ethos of the Stellar community. All codebase components, libraries, and platform bridging structures are fully open-source and licensed under the [MIT License](file:///c:/Users/Justin/AndroidStudioProjects/InvoiceApp/LICENSE). Reviewers and builders can inspect, compile, audit, or fork the project freely.

