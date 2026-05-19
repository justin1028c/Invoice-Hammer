# Read Me First, James

Welcome. This page exists for exactly one reason: to get you from a fresh
`git clone` to a buildable Xcode project in **one command**, so you can spend
your time writing iOS code instead of clicking through Xcode's "Add Package
Dependency" wizard.

## What this app is

**Invoice Hammer** is a Kotlin Multiplatform Compose app for tradespeople
(electricians, plumbers, contractors). It handles invoicing, expense
capture, supplier price tracking, encrypted vaults, AI-assisted line-item
extraction, and (where supported) Google Drive backup. Android is feature
complete; iOS is the host wrapper we're polishing for the Stellar Community
Fund submission.

Your contribution gets the iOS host parity-clean. That's the missing piece
for the SCF application.

## The one-liner

```bash
cd /path/to/InvoiceApp
bash iosApp/bootstrap_ios.sh
```

That's it. The script will:

1. Verify `xcodegen` is installed (it'll tell you to `brew install xcodegen`
   if not).
2. Generate `iosApp/iosApp.xcodeproj` from `iosApp/project.yml` — the
   project file is the source of truth, the `.xcodeproj` is throwaway.
3. Run `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`. This
   is the smoke test — if Kotlin/Native can produce a framework for the
   iOS simulator, the rest of the build will succeed.

When it's done you'll see a green banner. At that point:

```bash
open iosApp/iosApp.xcodeproj
```

In Xcode: select the `iosApp` target → Signing & Capabilities → pick your
Apple Developer **Team**. Build (⌘B), then run (⌘R).

## If the script fails

See `iosApp/IOS_BUILD_HANDOFF.md` → section **"If bootstrap_ios.sh fails
or you need to do it by hand"**. It has the manual-Xcode fallback as well
as common-failure debugging tips.

## What's NOT implemented on iOS (don't waste cycles)

These are intentional stubs. They're not bugs — Android has the real
implementation, iOS has a documented placeholder that returns a clean
sealed failure outcome. Pulled from the audit:

- **Google Drive sync.** `DriveAuthBridge.swift` throws an `NSError`; the
  Kotlin layer converts it to `DriveTokenOutcome.Failure`. Wiring requires
  `GIDSignIn` with the Drive scope on iOS. **Optional for first SCF cut.**
- **PowerPay payments.** Runs in mock mode on both platforms until
  `PowerPayConfig.baseUrl` is set to the Vercel URL. KMP-shared, not iOS-
  specific.
- **Tax report ZIP/PDF export.** `IosTaxExporter.kt` returns
  `TaxExportOutcome.Failure` with a clear message. No Swift work needed
  for the first build.
- **Gemini API key on iOS.** `IosSecretProvider.getGeminiApiKey()` throws
  if nothing is in the Keychain. To enable: `IosSecurityBridge.saveSecret(
  "gemini_api_key", "<key>")` from a debug screen or one-shot call.
- **`PlatformActionsBridge.swift` uses deprecated** `UIApplication.shared.windows`.
  It still compiles and runs on iOS 14+ — Xcode just emits a warning.
  Fixing it is a nice early PR if you want a low-risk warm-up.

## Before your first commit

- [ ] Set your **Development Team** in Xcode (Signing & Capabilities) — it
      only writes to local Xcode prefs, not to anything committed.
- [ ] Do **NOT** commit `iosApp/iosApp.xcodeproj/` — it's gitignored. If
      `git status` ever shows it as untracked, the gitignore is correct;
      that's expected. The `project.yml` is the source of truth.
- [ ] Do **NOT** commit your team prefix into `iosApp/iosApp.entitlements`.
      The file uses `$(AppIdentifierPrefix)` which Xcode resolves locally.
- [ ] When you edit `iosApp/project.yml`, re-run `bash iosApp/bootstrap_ios.sh`
      (or just `xcodegen generate --spec iosApp/project.yml --project iosApp`)
      and commit the YAML change only.

## Architecture in 30 seconds

- `commonMain` (Kotlin) — domain entities, use cases, repository contracts,
  sealed operation outcomes, value classes. Zero platform imports.
- `iosMain` (Kotlin) — `actual` implementations using Apple frameworks via
  Kotlin/Native interop. Exposes `MainViewController()` and registers
  Koin.
- `iosApp/iosApp/*.swift` — thin Swift host. Configures Firebase, wires
  the four native bridges (`FirebaseAuth`, `Keychain`, `PlatformActions`,
  `DriveAuth`), then hands off to Compose via `ComposeView`.

The repository contracts are sealed-result based (no `Result<T>`, no
exception-driven control flow for expected failures). When you write a new
iOS bridge, return a sealed outcome on the Kotlin side and let Swift
throw/return naturally — Kotlin/Native bridges Swift `async throws` to
Kotlin `suspend` automatically.

## Questions?

If anything in `IOS_BUILD_HANDOFF.md` contradicts what you see in the code,
the code is right and the doc is stale — open an issue or ping me and
I'll fix the doc.

Welcome aboard. Let's ship this.
