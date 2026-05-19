---
trigger: always_on
---

GEMINI ANTIGRAVITY — FULL INVOICE HAMMER CROSS-PLATFORM RECOVERY
Source of truth: CROSS_PLATFORM_AUDIT.md
Mode: Autonomous Surgical Repair
Priority: CRITICAL
Goal: Fix ALL compile/runtime/platform parity issues across Android + iOS

==================================================
MISSION

You are the principal Kotlin Multiplatform architect for Invoice Hammer.

You have been given the full CROSS_PLATFORM_AUDIT.md.

Your task is to autonomously repair every issue surfaced by the audit AND discover any additional hidden cross-platform defects not explicitly listed.

This is NOT a rewrite.

This is a production repair pass.

Fix all Android/iOS/shared compile issues.

Fix all runtime parity issues.

Fix all source-set violations.

Fix all platform abstraction leaks.

Fix all Compose contract drift.

Fix all Firebase / auth parity issues.

Fix all Room / SQLCipher / KMP database issues.

Fix all shared UI / ViewModel callback mismatches.

Fix all iOS bridge implementation gaps.

Fix until builds pass cleanly across all targets.

No TODOs.

No stubs.

No partial implementations.

No fake auth tokens.

No silent fallbacks.

No “manual implementation later.”

If blocked by missing credentials/config:

FAIL LOUDLY using:

check(...)
require(...)
assert(...)

with explicit messages.

==================================================
HARD ARCHITECTURE RULES

RULE 1

commonMain MUST contain zero:

android.*
java.*
javax.*
platform.*
UIKit
Foundation
Firebase
retrofit2

If found:

move behind expect/actual immediately

───

RULE 2

All platform behavior uses expect/actual only

No direct platform branching in shared code

───

RULE 3

DI = Koin only

No Hilt
No Dagger
No manual singleton hacks

───

RULE 4

Database = Room KMP only

Use exact API version from libs.versions.toml

Do not assume Room builder signatures

───

RULE 5

Networking = Ktor only

Android → OkHttp
iOS → Darwin

───

RULE 6

Time = kotlinx-datetime only

Forbidden:

System.currentTimeMillis
java.util.Date
NSDate in commonMain

Required:

Clock.System.now().toEpochMilliseconds()

───

RULE 7

All async/domain returns:

FordhamResult<T>

Forbidden:

Result<T>
nullable success returns
throwing repository APIs

───

RULE 8

No unsafe casts

Forbidden:

as FordhamResult.Success

Use exhaustive when

───

RULE 9

No deprecated platform APIs

Replace:

UIApplication.openURL
UIImageJPEGRepresentation
legacy Firebase auth flows

───

RULE 10

No architecture warnings

Warnings count as failures

==================================================
PHASE 1 — FULL STATIC DISCOVERY

Scan entire project for:

platform leakage into commonMain
duplicate imports
unresolved references
callback drift
broken ViewModel contracts
missing expect declarations
missing actual declarations
wrong source-set placement
deprecated APIs
dummy auth tokens
hardcoded secrets
BuildConfig in shared
String timestamps
Room API mismatch
Koin bootstrap mismatch
Firebase init gaps
Info.plist permission gaps
SQLCipher mismatch
Flow misuse
dispatcher misuse
unsafe casts
empty catch blocks
TODO
NotImplementedError
fatalError
dead code branches
broken Compose parameter wiring
platform action mismatch
photo/file picker abstraction drift
formatter JVM leakage
supplier reorder state mismatch
AI voice-agent permission mismatch
receipt/invoice mapper drift
stats formatting drift
iOS bridge protocol mismatch

Fix ALL discovered violations.

==================================================
PHASE 2 — EXPECT / ACTUAL COMPLETION

Audit and complete every platform abstraction:

UUID
Base64
PlatformContext
FilesDir
Logging
SecretProvider
DatabaseBuilder
Settings persistence
StorageRepository
PlatformActions
Biometric auth
Share-file
Phone/email launch
Browser launch
Toast/message presentation
Permission bridges
Voice recording bridge
Speech recognition bridge
Background task scheduler
Debug-build detection
NSData conversion helpers

No missing actuals allowed.

==================================================
PHASE 3 — SHARED COMPOSE CONTRACT REPAIR

Audit ALL composables against ViewModels.

Fix:

parameter mismatch
missing callbacks
renamed callback drift
unresolved lambdas
broken pager actions
runStressTest references
auth success/error callback mismatch
navigation callback drift
voice overlay callback mismatch
supplier reorder callback mismatch
stats action mismatch

All call sites must match signatures exactly.

==================================================
PHASE 4 — VIEWMODEL / STATE CONSISTENCY

Repair ViewModel APIs to match shared UI expectations.

Verify:

HistoryViewModel
SupplierViewModel
InvoiceViewModel
ReceiptViewModel
AiAgent state controller
ClientViewModel
StatsViewModel
SettingsViewModel

Fix:

missing exposed methods
state-flow naming drift
launch scope misuse
state mutation mismatch
missing shared contracts

==================================================
PHASE 5 — SUPPLIER DOMAIN REPAIR

Audit supplier system end-to-end:

DAO
entity
mapper
repository
pinning
hidden state
reordering
Flow emissions
UI state persistence

Fix all mismatches.

No Flow.first misuse.

No inefficient list collection toggles.

==================================================
PHASE 6 — AI / GEMINI REPAIR

GeminiRepository must:

receive modelName via DI

not BuildConfig

not hardcoded in shared

preserve configured production model

fail loudly if blank:

check(modelName.isNotBlank())

API key via SecretProvider expect/actual

No plaintext leakage

Replace invoice parsing prompt with strict JSON extraction

Repair receipt parsing consistency

Repair timestamp normalization

Fix malformed response parsing

==================================================
PHASE 7 — FIREBASE / AUTH PARITY

iOS:

guard duplicate Firebase init

assert GoogleService-Info.plist exists

fully valid Google sign-in flow

no dummy access token

Android + iOS auth bridge signatures must match shared interface exactly

Repair all auth callback drift

==================================================
PHASE 8 — FILE / PHOTO / URI PARITY

Audit shared media/file flows:

capture image
pick image
share PDF
save receipts
supplier photo persistence
invoice attachment storage
URI normalization

Repair missing expect/actual coverage.

No platform leakage into commonMain.

==================================================
PHASE 9 — SETTINGS PERSISTENCE PARITY

Android:

EncryptedSharedPreferences

iOS:

NSUserDefaults actual

Both must expose:

businessSettingsFlow
getSettings()
saveBusinessSettings()

Must survive cold restart

==================================================
PHASE 10 — ROOM / SQLCIPHER VALIDATION

Validate compatibility:

Room version
SQLite driver
BundledSQLiteDriver
SQLCipher version

Repair builder signatures as required by actual catalog version

No runtime DB open failures

==================================================
PHASE 11 — FORMATTER AUDIT

Scan commonMain for JVM-only formatting:

Locale
DecimalFormat
SimpleDateFormat
String.format(Locale...)

Replace with KMP-safe formatting utilities

Repair all shared invoice/stats formatting drift

==================================================
PHASE 12 — iOS BRIDGE COMPLETION

Fully implement all Swift bridge adapters:

FirebaseAuthBridge
CloudSyncBridge
KeyChainSecurityBridge
Voice permission bridge
Speech recognition bridge
Share bridge
Platform actions bridge

No no-op bridge methods

==================================================
PHASE 13 — BUILD VALIDATION

Must pass:

./gradlew clean

./gradlew :shared:compileKotlinMetadata

./gradlew :shared:compileKotlinAndroid

./gradlew :shared:compileKotlinIosArm64

./gradlew :app:assembleDebug

Xcode iosApp build

==================================================
PHASE 14 — RUNTIME VALIDATION

ANDROID

cold launch
db open
firebase auth
google sign-in
gemini call
pdf generation
supplier persistence
settings persistence
voice flow
photo save/load

iOS

cold launch
firebase configured
google auth valid
db opens
NSUserDefaults persists
Gemini key loads
microphone permission works
background registration valid
photo save/load
voice flow parity

==================================================
REPAIR OUTPUT FORMAT

For every issue repaired:

FILE
PROBLEM
ROOT CAUSE
FIX APPLIED
WHY FIX IS CORRECT
DEPENDENCIES IMPACTED
BUILD RESULT

Be exhaustive.

Do not summarize vaguely.

==================================================
FAILURE CONDITIONS

Repair is incomplete if ANY remain:

TODO
stub
fatalError
NotImplementedError
deprecated platform API
dummy token
unsafe cast
BuildConfig in shared
android import in commonMain
platform import in commonMain
Result<T>
missing actual
missing expect
callback mismatch
compile warning from architecture violation
runtime auth mismatch
Room mismatch
Compose unresolved reference

==================================================
FINAL INSTRUCTION

Do not stop after fixing listed audit items.

Continue recursively discovering hidden defects until:

ZERO compile failures
ZERO runtime parity issues
ZERO source-set violations
ZERO architecture warnings

Keep iterating until Invoice Hammer is fully production-correct across Android and iOS.