# Invoice Hammer — Stellar Community Fund (SCF) Technical Specification

Invoice Hammer is a Kotlin Multiplatform (KMP) system designed for non-custodial invoice staging, tracking, and instant automated settlement on the Stellar network. It provides independent contractors and merchants with a lightweight, peer-to-peer alternative to traditional payment processors.

---

## 1. Executive Summary & Value Proposition

Traditional payment processors charge contractors between 1.5% and 3.5% in transaction fees, enforce arbitrary holding periods (2–5 business days), and introduce centralized custodial risks. 

**Invoice Hammer** addresses this by shifting the settlement rail to **Stellar USDC**.
* **Zero Markup Fees**: Transaction fees are limited to the baseline Stellar network fee (fraction of a cent).
* **Instant Settlement**: Invoices are settled on-chain directly to the contractor’s wallet within seconds.
* **Non-Custodial Design**: Invoice Hammer does not maintain custody of funds or manage private key databases. All signing occurs locally.

---

## 2. Live Proof & Project Demo

* **Public Repository**: [Invoice Hammer KMP Repository](https://gitlab.com/Justin1028c/invoice-hammer)
* **Live Hosted Spec Page**: [Stellar Invoice Hammer Specification Site](https://invoice-hammer-1f7efb.gitlab.io)


---

## 3. Architecture & Custody Model

Invoice Hammer leverages Kotlin Multiplatform (KMP) to share core business logic and storage models, while keeping key signing operations localized.

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

### Security & Custody Architecture
* **Local Transaction Signing**: Private keys never leave the client's device. For live operations, signing key pairs are securely generated and stored in the **iOS Keychain (via Secure Enclave)** and **Android Keystore**, bridged through KMP expect/actual platform declarations.
* **Cryptographic Staging**: Invoices are compiled locally into Stellar Transaction Envelopes. The app signs the transaction payload locally before dispatching it to the Stellar Network via the PowerPay settlement client.
* **Encrypted Storage**: Invoice data, client directories, and metadata are persisted locally using a Room KMP database encrypted with **SQLCipher**, preventing unauthorized access to business history on-device.

---

## 4. Stellar Ecosystem Alignment

Invoice Hammer utilizes the Stellar network because of its optimal features for micro-payments and day-to-day merchant transactions:
1. **Low-Latency Settlement**: Stellar transactions settle in under 5 seconds, resolving cash-flow constraints for freelance contractors.
2. **Stablecoin Focus (USDC)**: By using USDC on Stellar, merchants avoid cryptocurrency price volatility while bypassing traditional banking rails.
3. **SEP-24 / SEP-31 Integration Paths**: Future iterations will align with Stellar Ecosystem Proposals (SEPs) to support direct fiat on/off-ramps (e.g., Anchor integrations).

---

## 5. Development Roadmap & Tranche Plan

The project is structured into three discrete 30-day milestones (Tranches):

| Tranche | Milestone | Technical Deliverables | Funding allocation |
| :--- | :--- | :--- | :--- |
| **Tranche 1** | Local Staging MVP | Room KMP schema design, SQLCipher setup, client management UI, and PDF generator. | 35% |
| **Tranche 2** | Testnet Integration | Secure Enclave / Keystore platform bridge, mock PowerPay client, Stellar Testnet transaction staging, and webhooks. | 40% |
| **Tranche 3** | Mainnet & Audit | Production-grade UI/UX polish, third-party security audit of key bridging, and Mainnet deployment. | 25% |

---

## 6. Solo Developer & Technical Background

* **Kotlin Multiplatform Expertise**: Built with strict adherence to Clean Architecture—enforcing a pure domain core containing zero platform dependencies, zero primitive obsession (using `@JvmInline value class` representation), and Swift-friendly sealed interface boundaries.
* **Systems Engineering**: Comprehensive experience in embedded device cryptography, database security, and distributed settlement rails.

---

## 7. Open Source Alignment & Licensing
Invoice Hammer is committed to the open-source ethos of the Stellar community. All codebase components, libraries, and platform bridging structures are fully open-source and licensed under the [MIT License](file:///c:/Users/Justin/AndroidStudioProjects/InvoiceApp/LICENSE). Reviewers and builders can inspect, compile, audit, or fork the project freely.

