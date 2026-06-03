# Invoice Hammer

## The Hook
Invoice Hammer delivers non-custodial invoice staging and automated settlement for independent contractors using the Stellar network. By bridging client payments to on-chain USDC settlement rails, contractors receive prompt, trustless payments while retaining full custody of their invoice history and ledger balances.

---

## Clean Architecture Boundary & Stellar Transaction Flow
Invoice Hammer is architected as a pure Kotlin Multiplatform (KMP) system. The core domain logic is strictly decoupled from platform frameworks and external APIs, enforcing boundaries using strong value classes and sealed operation interfaces.

```mermaid
graph TD
    subgraph UI & Presenters (composeApp)
        A[PaymentViewModel]
        B[PaymentLedgerSheet]
    end

    subgraph Pure Domain (commonMain)
        C[PaymentRepository Interface]
        D[InvoicePaymentRequest Entity]
        E[StellarTransactionHash Value Class]
        F[PaymentRequestOutcome Sealed Interface]
    end

    subgraph Data & Infra (shared)
        G[MockPaymentRepository]
        H[PowerPayClient Implementation]
        I[PaymentRequestDao Room DB]
    end

    subgraph Stellar Network Rails
        J((Stellar Ledger / USDC Rail))
        K[PowerPay Gateway / API]
    end

    A -->|Calls| C
    G -.->|Implements| C
    G -->|Calls| H
    G -->|Persists| I
    H -->|Initiates Tx / Settles USDC| K
    K -->|Submits Tx & Proof| J
    J -.->|Emits Tx Hash & Explorer URL| K
    K -.->|Payment Event webhook| H
    H -.->|Returns Transaction Proof| G
    G -.->|Emits Success Outcome| A
```

---

## Working Demo
- **GitHub / GitLab Repository**: [Invoice Hammer Repository](https://gitlab.com/Justin1028c/invoice-hammer)
- **60-Second Demo Video**: [Watch the App Staging & Settling an Invoice on Stellar](https://www.loom.com/share/demo-video-id)
