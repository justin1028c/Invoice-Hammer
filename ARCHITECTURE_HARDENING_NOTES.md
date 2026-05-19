# Architecture Hardening Notes

This pass intentionally kept changes close to the audited findings to preserve Android behavior and iOS handoff readiness.

## Deferred Primitive Boundary Work

- `GenerateInvoiceRequest`, `SaveInvoiceRequest`, and `ProcessReceiptRequest` now collapse broad public parameter lists into explicit request objects. Their fields still mirror current UI/draft primitives because introducing value objects for every invoice form field would require broader UI validation and draft-state migration.
- `GlobalAiAgentUseCase` and `ForemanOrchestrator` now accept `NaturalLanguage` for user/system text. Other older use cases with primitive strings remain out of scope for this pass unless they are touched by the audited workflows.

## Dispatcher Ownership

- `KtorSyncRepository` now accepts an IO dispatcher through construction with a KMP-safe default.
- `StatsViewModel` now accepts both work and main dispatchers through construction.
- `DatabaseBuilder` still uses Room's `setQueryCoroutineContext(Dispatchers.IO)` directly. This is database infrastructure wiring, not domain logic, and Room requires a coroutine context at the builder boundary on both Android and iOS.
