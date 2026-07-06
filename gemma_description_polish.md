# Local Gemma Description Polish for Voice Invoices

This document describes how Invoice Hammer uses on-device LLMs (specifically **Gemma 3n E2B**) to clean up noisy Speech-to-Text (STT) description fragments in voice invoices without compromising numeric accuracy.

---

## 1. The Core Architecture

The voice-to-invoice pipeline splits processing into two distinct layers: a **Deterministic Parser** for structure, math, and data safety, and a **Local LLM** for semantic cleanup.

```
                  ┌───────────────────────┐
                  │   User Voice Input    │
                  └───────────┬───────────┘
                              │
                  ┌───────────▼───────────┐
                  │  Speech-to-Text (STT) │
                  └───────────┬───────────┘
                              │ (Raw Transcript)
                  ┌───────────▼───────────┐
                  │ Deterministic Parser  │
                  └───────────┬───────────┘
                              │ (Structured Data: Client, Address, Math, etc.)
                              │ (Raw Descriptions: "caught vanity", "adjusted to doors")
            Yes   ┌───────────▼───────────┐
      ┌──────────►│ Gemma Local Supported?│
      │           └───────────┬───────────┘
      │                       │ No
      │           ┌───────────▼───────────┐
      │           │ Skip Polish Pass      │──┐
      │           │ (Use original desc)   │  │
      │           └───────────────────────┘  │
┌─────▼───────────────────┐                  │
│ Gemma Polish Pass       │                  │
│ (8s Max Timeout)        │                  │
└─────────────┬───────────┘                  │
              │                              │
              ├──────────────────────────────┘
              │
              ▼
  ┌───────────────────────┐
  │ UpdateDraftInvoice    │
  │ (Rendered to User)    │
  └───────────────────────┘
```

1. **Deterministic Parsing First:** The transcript is parsed by [ParseVoiceInvoiceDeterministicallyUseCase](file:///c:/Users/Justin/AndroidStudioProjects/invoice-hammer-app/shared/src/commonMain/kotlin/com/fordham/toolbelt/domain/usecase/ParseVoiceInvoiceDeterministicallyUseCase.kt) to identify client name, address, quantities, amounts, categories, tax, and deposit.
2. **Local LLM Check:** The pipeline checks if the local model is supported (e.g. model downloaded on Android, skipped on iOS).
3. **Cosmetic Polish Pass:** The raw descriptions are sent to [PolishLineItemDescriptionsUseCase](file:///c:/Users/Justin/AndroidStudioProjects/invoice-hammer-app/shared/src/commonMain/kotlin/com/fordham/toolbelt/domain/usecase/PolishLineItemDescriptionsUseCase.kt). Gemma reformats them into professional trade descriptions.
4. **Draft Update:** The finalized invoice draft is updated and rendered to the user on the confirmation card.

---

## 2. Safety Constraints (The Firewall)

To ensure this feature remains zero-risk and highly reliable for busy contractors on job sites:

* **The Math Firewall:** The local LLM **never** sees or processes dollar amounts, quantities, tax rates, or subtotals. It only receives description strings. This prevents numerical hallucinations.
* **Non-Blocking Fallback:** If Gemma is not downloaded, times out (8-second maximum limit), fails generation, or returns a malformed response, the pipeline silently drops the polish step and proceeds using the deterministic parser's original descriptions. The user's flow is never interrupted.
* **Local-Only:** The pass runs entirely on-device, meaning it uses zero data/network resources and is completely air-gapped.

---

## 3. Prompt Design & Output Contract

The polish pass uses a targeted system-user prompt configured in [LineItemDescriptionPolishPrompt](file:///c:/Users/Justin/AndroidStudioProjects/invoice-hammer-app/shared/src/commonMain/kotlin/com/fordham/toolbelt/data/implementation/LineItemDescriptionPolishPrompt.kt):

* **Structured Format:** Gemma is prompted to return a JSON array containing exactly the same number of elements as the input array.
* **Mid-Array Priming:** The prompt ends with `[` to force the model to output a valid JSON list format without markdown code blocks, preambles, or conversational filler.
* **Trade Context Rules:** The prompt instructs the model to preserve numbers/quantities already present (e.g. `"2 doors"`) while correcting trade slang/STT mishears (e.g. `"caught"` → `"caulked"`).

### Example Transformation:
* **Raw Input:** `["Completed punch list repairs caught the bathroom vanity", "repaired 1 cracked transition strip"]`
* **Gemma Polish:** `["Caulked bathroom vanity", "Repaired 1 cracked transition strip"]`

---

## 4. How to Verify via logs

You can monitor the polish process in real-time by running the following command:

```powershell
adb logcat -v time | Select-String "VoiceInvoicePipeline"
```

Look for these specific logs:

* **`DESCRIPTIONS_POLISHED`:** Indicates a successful polish pass. Shows the mapping of each item:
  `DESCRIPTIONS_POLISHED count=2 'caught vanity' -> 'Caulked bathroom vanity'`
* **`DESCRIPTIONS_POLISH_SKIPPED`:** Indicates the local model is not downloaded or not initialized, so original parser outputs are used.
* **`DESCRIPTIONS_POLISH_FAILED`:** The local LLM returned an error.
* **`DESCRIPTIONS_POLISH_COUNT_MISMATCH`:** The LLM did not return the exact number of array elements matching the input list (discarded for safety).
