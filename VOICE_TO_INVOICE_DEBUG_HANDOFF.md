# Voice To Invoice Debug Handoff

## Context

Invoice Hammer voice-to-invoice is currently a hybrid pipeline:

1. Android `SpeechRecognizer` turns speech into text.
2. Android STT returns multiple hypotheses and the app reranks them.
3. `ParseVoiceInvoiceDeterministicallyUseCase` parses obvious invoice fields with rules/regex.
4. The Foreman/local/cloud AI path can still act as fallback/agent layer.
5. Code computes totals, tax, deposit, and applies the draft.

The recent debugging centered on contractor-style dictation where Android STT produces near-miss words and the deterministic parser splits line items incorrectly.

## Key Files

- `shared/src/androidMain/kotlin/com/fordham/toolbelt/util/AndroidVoiceAssistant.kt`
  - Android STT setup and candidate reranking.
  - Changed `RecognizerIntent.EXTRA_MAX_RESULTS` from `1` to `8`.
  - Reads `SpeechRecognizer.CONFIDENCE_SCORES`.
  - Ranks STT candidates with invoice-domain signals.

- `shared/src/commonMain/kotlin/com/fordham/toolbelt/domain/usecase/ParseVoiceInvoiceDeterministicallyUseCase.kt`
  - Main deterministic voice invoice parser.
  - Extracts client name, address, line items, tax, deposit.
  - Most line-item bugs have been here.

- `app/src/test/java/com/fordham/toolbelt/domain/usecase/ProcessInvoiceAiUseCaseTest.kt`
  - Regression tests for voice-to-invoice parsing.
  - Add any new real-world failed transcript here first.

## Recent Bugs And Fixes

### 1. Client Name / Address Swallowing Work Text

Failure:

The parser kept asking for a client name or misread fields because address extraction swallowed the work description after the ZIP.

Example:

```text
make an invoice for client Marcus Hill at 1189 West Briar Court Macon Georgia 31210
42 linear feet of baseboard ...
```

Fix:

- `extractRawAddress()` now has an `afterClientAtZip` regex that stops address extraction at ZIP code.

Regression:

- `deterministic parser stops address at zip before work details`

### 2. Weird Line Descriptions From Price Phrase Bleed

Failure:

Descriptions became chopped leftovers:

```text
Go wash the 32 ft
Cents per square foot sealed back steps
A
```

Root cause:

The money regex and segment splitter let price phrases bleed into the next work item.

Fixes:

- Money regex handles `cents`.
- Cents are converted to dollars with `rawAmount / 100.0`.
- `extractPostPriceDescription()` handles price-before-description cases like `$35 material pickup fee`.
- Cleanup maps:
  - `go wash` -> `pressure washed`
  - trims leading `foot`, `feet`, `ft`, `square foot`, `square feet`

Regression:

- `deterministic parser keeps cents per square foot and post price descriptions clean`

### 3. `75 cents per square foot` Split Incorrectly

Failure:

Transcript:

```text
... to washed 120 square feet at 75 cents per square foot back the steps at 185 dollars ...
```

The parser produced:

```text
To washed 120 square feet
Back the steps
At a 35 deer at a
```

Fixes:

- Explicit money pattern for:

```text
at 75 cents per square foot
```

- Cleanup:
  - `to washed` -> `pressure washed`
  - `back the steps` -> `sealed back steps`
  - noisy phrase before `$35 markup fee` falls back to post-price description.

Regression:

- `deterministic parser keeps noisy deck and wash line descriptions coherent`

### 4. `two shut off valves` Heard As `to shut off valve`

Failure:

Android STT output:

```text
replace one garbage disposal at 275 to shut off valve at 65 each
```

The parser generated:

```text
To shut off valve: $65
```

Expected:

```text
2 shut off valve: $130
```

Fix:

- Parser repairs leading `to shut off valve` to `2 shut off valve`.

Regression:

- `deterministic parser repairs shut off valve stt quantity confusion`

### 5. `two nail pops` Heard As `to nail pops`

Failure from screenshot/logs:

```text
repaired to nail pops at $30 each
```

App produced:

```text
To nail pops: $30
```

Expected:

```text
2 nail pops: $60
```

Important log detail:

Android STT returned a correct alternate:

```text
repaired two nail pops at $30 each
```

but the reranker selected the wrong higher-confidence candidate:

```text
repaired to nail pops at $30 each
```

Fixes:

- `AndroidVoiceAssistant.invoiceSpeechScore()`:
  - penalizes `to nail pops/outlets/receptacles/valves/boards/fixtures`
  - rewards `two nail pops/outlets/receptacles/valves/boards/fixtures`
- Parser repairs:
  - `to nail pops` -> `2 nail pops`
  - same pattern for outlets, receptacles, valves, boards, fixtures.
- Cleanup removes leading `the`, so `The hallway trim` becomes `Hallway trim`.

Regression:

- `deterministic parser repairs to nail pops stt quantity confusion`

## Current Test Command

Use this for parser work:

```powershell
.\gradlew.bat :androidApp:testDebugUnitTest --tests "com.fordham.toolbelt.domain.usecase.ProcessInvoiceAiUseCaseTest"
```

Install app after fixes:

```powershell
.\gradlew.bat :androidApp:installDebug
```

The runnable Android app module is `androidApp`, not `composeApp`.

## Useful Log Filters

```powershell
adb logcat -d -v time |
  Select-String -Pattern "VoiceAssistant|VoiceInvoiceTrace|DeterministicInvoice|Agent command accepted|Foreman command|Line phrase|Parsed voice" -Context 0,3 |
  Select-Object -Last 180
```

Most useful log lines:

- `SpeechRecognizer ranked results:`
- `Voice final transcript=`
- `Agent command accepted=`
- `Foreman command=`
- `Line phrase=`
- `Parsed voice invoice`
- `UpdateDraftInvoice argsClient=... lineItems=...`

## Current Architecture Weakness

The parser still uses a money-match segmentation strategy:

```text
description before price -> price -> next description before next price
```

This works for many contractor invoices, but it is brittle when:

- STT confuses `two` with `to`.
- A price phrase contains unit words like `per square foot`.
- A task starts immediately after a price with no punctuation.
- The user says a deposit after a line item.
- Multiple actions are chained in one long sentence.

## Recommended Next Step

Move line-item extraction from raw money-match splitting to a two-stage parser:

1. Normalize transcript into clauses.
   - Insert boundaries before action verbs:
     - `installed`
     - `painted`
     - `repaired`
     - `replaced`
     - `hauled`
     - `sealed`
     - `pressure washed`
     - `added`
   - Do not split inside price units like `per square foot`.

2. Parse each clause independently.
   - Extract quantity.
   - Extract description.
   - Extract price.
   - Decide unit-priced vs flat-priced.

This would reduce the repeated “fix one phrase, another phrase breaks” cycle.

## Golden Real-World Transcripts

Keep these as regression material:

```text
make an invoice for client Marcus Hill at 1189 West Briar Court Macon Georgia 31210 installed 42 linear feet of baseboard at 6.50 per foot painted the hallway trim for $225 repaired to nail pops at $30 each and apply $50 deposit already paid
```

Expected:

- Client: `Marcus Hill`
- Address: `1189 West Briar Court Macon GA 31210`
- `42 linear feet of baseboard` = `42 x 6.50 = 273`
- `Hallway trim` = `225`
- `2 nail pops` = `2 x 30 = 60`
- Deposit = `50`

```text
create an invoice for Eleanor Brooks at 71426 Maple Ridge Lane Savannah Georgia 31405 I replaced three damaged deck boards at $48 each to washed 120 square feet at 75 cents per square foot back the steps at 185 dollars at a 35 deer at a $35 markup fee
```

Expected:

- `3 damaged deck boards` = `144`
- `Pressure washed 120 square feet` = `90`
- `Sealed back steps` = `185`
- `Markup fee` = `35`

```text
start an invoice for Nadia Coleman at 506 Lakeview Drive Augusta Georgia 30907 replace one garbage disposal at 275 to shut off valve at 65 each hauled away the old unit for $40 in charge 8% tax
```

Expected:

- `1 garbage disposal` = `275`
- `2 shut off valve` = `130`
- `Hauled away the old unit` = `40`
- Tax = `8`

## Product Guidance

Do not rely on the local LLM for money, totals, tax, or validation.

Good local LLM uses:

- Clean up descriptions.
- Suggest professional wording.
- Classify line item categories.
- Ask only for missing/uncertain fields.
- Summarize job notes.
- Draft customer-facing invoice notes.

Keep deterministic code in charge of:

- Amount extraction.
- Quantity math.
- Tax/deposit calculations.
- Required fields.
- Final invoice application.

