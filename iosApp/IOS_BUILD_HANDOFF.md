# iOS Build Handoff

This folder contains the Swift host for Invoice Hammer. The shared app logic lives in:

- `../shared/src/commonMain`
- `../shared/src/iosMain`
- `../composeApp/src/commonMain`
- `../composeApp/src/iosMain`

The Windows-side audit verified that `:shared:compileCommonMainKotlinMetadata` and
`:composeApp:compileCommonMainKotlinMetadata` build cleanly, and that
`:androidApp:compileDebugKotlin` + `:androidApp:testDebugUnitTest` still pass. The
Kotlin/Native iOS targets cannot be built on Windows, so the first compile that
actually links `ComposeApp.framework` happens on James's Mac.

---

## The ONE-COMMAND workflow

Everything Xcode needs is generated from `iosApp/project.yml` via
[XcodeGen](https://github.com/yonaskolb/XcodeGen). The `.xcodeproj` is throwaway
(it is git-ignored on purpose) — `project.yml` is the source of truth.

### Prerequisites (one-time, on James's Mac)

```bash
brew install xcodegen
brew install --cask temurin@21   # if you don't already have JDK 21
xcode-select --install            # only if you've never installed CLT
```

### The single command

```bash
cd /path/to/InvoiceApp
bash iosApp/bootstrap_ios.sh
```

That script:

1. Verifies `xcodegen` is on PATH (and tells you how to install it if not).
2. Soft-checks that Java 21 is on PATH (warns but continues — Android Studio's
   bundled JDK is fine).
3. Generates `iosApp/iosApp.xcodeproj` from `iosApp/project.yml`.
4. Runs `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` — this is
   the smoke test that exercises Kotlin/Native against the Apple toolchain
   for the first time. **If this passes, the rest of the build will succeed.**

After it finishes:

```bash
open iosApp/iosApp.xcodeproj
```

In Xcode → `iosApp` target → Signing & Capabilities → set your **Team**, then
⌘B / ⌘R. The Run Script build phase (`embedAndSignAppleFrameworkForXcode`)
already wires the Kotlin/Native framework into the build.

---

## What's already wired up for you

The XcodeGen spec (`iosApp/project.yml`) configures every piece of the Xcode
project that previously required manual clicks:

| Concern                                  | Where it's configured                                        |
|------------------------------------------|--------------------------------------------------------------|
| Product bundle ID `com.fordham.toolbelt` | `targets.iosApp.settings.base.PRODUCT_BUNDLE_IDENTIFIER`     |
| Info.plist path                          | `INFOPLIST_FILE = iosApp/Info.plist`                         |
| Entitlements path                        | `CODE_SIGN_ENTITLEMENTS = iosApp/iosApp.entitlements`        |
| Deployment target iOS 14.0               | `options.deploymentTarget.iOS` + target override             |
| `GoogleService-Info.plist` as resource   | `targets.iosApp.resources`                                   |
| All Swift sources                        | `targets.iosApp.sources` (`iosApp/iosApp/` directory)        |
| User Script Sandboxing **off**           | `ENABLE_USER_SCRIPT_SANDBOXING = NO`                         |
| Run Script: build Kotlin/Native fwk      | `targets.iosApp.preBuildScripts[0]`                          |
| Firebase SPM pins                        | `packages.Firebase` → `from: 11.0.0`                         |
| GoogleSignIn SPM pins                    | `packages.GoogleSignIn` → `from: 7.1.0`                      |
| `ComposeApp.framework` link              | `OTHER_LDFLAGS = -framework ComposeApp` + framework search paths |

### Bundle ID + URL scheme verification

- `GoogleService-Info.plist` `BUNDLE_ID` = `com.fordham.toolbelt` ✅
- Android `applicationId` (`app/build.gradle.kts`) = `com.fordham.toolbelt` ✅
- `Info.plist` `CFBundleURLSchemes[0]` = `com.googleusercontent.apps.716278040823-6afkdloh5qs359mr896tl725ge7gl8h0`
- `GoogleService-Info.plist` `REVERSED_CLIENT_ID` matches the above ✅

These were audited from Windows; no change needed on Mac.

### SPM versions — why these specific pins

- **firebase-ios-sdk `from: 11.0.0`.** Firebase iOS 11.x is the current stable
  major (released 2024) and drops support for iOS 12, which matches our
  iOS 14 floor. Products linked: `FirebaseCore` and `FirebaseAuth`.
- **GoogleSignIn-iOS `from: 7.1.0`.** GoogleSignIn 7.x is the current major
  and is what `FirebaseAuthBridge.signOut()` and
  `PlatformActionsBridge.signInWithGoogle(...)` already call. Products
  linked: `GoogleSignIn` and `GoogleSignInSwift`.

If SPM resolution picks a newer minor/patch on first build, that is expected
— SemVer-compatible upgrades are fine. To pin exactly, change `from:` to
`exactVersion:` in `iosApp/project.yml`.

### Entitlements

`iosApp/iosApp.entitlements` declares a single capability:

- `keychain-access-groups = [ $(AppIdentifierPrefix)com.fordham.toolbelt ]`

`KeyChainSecurityBridge.swift` reads and writes a generic-password keychain
item; the entitlement is the standard default Xcode would auto-generate.
`$(AppIdentifierPrefix)` is resolved per-machine by Xcode using James's
team — no need to hard-code anything.

No Push, App Groups, or Background Modes are declared. Verified against
`Info.plist` and the four Swift bridge files — none of them require those
capabilities.

---

## Bridge Registration

`iOSApp.swift` registers native bridges **before** Koin starts:

- `IosAuthServiceProvider.shared.bridge = FirebaseAuthBridge()`
- `IosSecurityServiceProvider.shared.bridge = KeyChainSecurityBridge()`
- `IosPlatformActionsServiceProvider.shared.bridge = PlatformActionsBridge()`
- `IosDriveAuthServiceProvider.shared.bridge = DriveAuthBridge()`

`MainViewControllerKt.initKoinIos()` asserts all four bridges are non-null
and the error message names the Swift file you need to look at. Do not
move `initKoinIos()` above bridge registration.

---

## What was verified from Windows

- `commonMain` has no Android-only imports (no `android.*`, no `java.io.File`,
  no `java.util.concurrent`, no `kotlinx.coroutines.android`). The only
  `androidx.*` packages used in `commonMain` are the KMP-capable artifacts:
  Room, DataStore, Lifecycle-Compose, Navigation-Compose, Compose UI.
- Every `expect` in `shared/commonMain` has an iOS `actual`:
  - `encodeBase64` / `decodeBase64` → `shared/src/iosMain/.../Base64Util.kt`
  - `randomUUID` → `shared/src/iosMain/.../UuidUtil.kt`
  - `platformModule` → `shared/src/iosMain/.../di/PlatformModule.kt`
- `composeApp/commonMain` has no `expect` declarations (only
  `MainViewController` in `iosMain`).
- `composeApp/build.gradle.kts` exports `:shared` into the iOS framework with
  `baseName = "ComposeApp"`, `isStatic = true`, and all three iOS targets
  enabled (`iosX64`, `iosArm64`, `iosSimulatorArm64`).
- `shared/build.gradle.kts` puts Ktor Darwin, sqlite-bundled,
  kotlinx-serialization, kotlinx-datetime, kotlinx-coroutines, Room (via KSP
  for all three iOS targets), DataStore, and Koin into commonMain/iosMain.
  Nothing iOS-side is gated behind `androidMain`.
- `IosDatabaseBuilder.kt` uses `NativeSQLiteDriver` from
  `androidx.sqlite:sqlite:2.5.0-alpha13`,
  `setQueryCoroutineContext(Dispatchers.IO)`, `fallbackToDestructiveMigration()`,
  and applies the SQLCipher passphrase via `PRAGMA key`. The passphrase is
  single-quote-escaped defensively.
- `IosSecurityServiceProvider`/`KeyChainSecurityBridge` reads return `nil`
  for missing keys; only `IosSecretProvider.getGeminiApiKey()` throws
  (lazily, on first AI call — same behavior as Android).
- `iOSApp.swift` configures Firebase, registers all four bridges, calls
  `initKoinIos()`, then applies `NSFileProtectionComplete` to the SQLCipher
  DB files. Order is correct.

---

## Fixes applied from Windows (do not need attention on the Mac)

| File | Why |
|------|-----|
| `composeApp/src/iosMain/.../MainViewController.kt` | `checkIosBridgesInitialized()` also checks `IosDriveAuthServiceProvider.bridge`; all four error messages point at the matching Swift file. |
| `shared/src/iosMain/.../data/IosDatabaseBuilder.kt` | `PRAGMA key` value is `replace("'", "''")`-escaped so the open-callback can't be broken by a non-UUID passphrase. |
| `shared/src/commonMain/.../domain/repository/DriveAuthTokenProvider.kt` | Was missing `import kotlin.jvm.JvmInline`. Compiled on Android because `kotlin.jvm.*` is auto-imported on JVM, but would have failed `:shared:compileCommonMainKotlinMetadata` and the Kotlin/Native iOS compile. |
| `shared/src/commonMain/.../domain/usecase/BillLaborUseCase.kt` | Replaced JVM-only `"%.2f".format(hours)` with a multiplatform `formatTwoDecimals(Double)` helper using `kotlin.math.round`. Same blocker as above on Kotlin/Native. |

---

## Expected Current Limits (no change needed before first build)

- PowerPay is KMP-shared and runs in mock mode until `PowerPayConfig.baseUrl`
  is set to James's Vercel URL (see `shared/src/commonMain/.../di/AppModule.kt`).
- iOS Google Drive sync has a native bridge stub registered
  (`DriveAuthBridge.swift`). It throws an `NSError` until Drive-scoped iOS
  auth is wired with `GIDSignIn` and an additional scope. The Kotlin side
  converts that into a `DriveTokenOutcome.Failure`, so the rest of the app
  still functions.
- Tax report ZIP/PDF export has iOS stubs (`IosTaxExporter.kt`) that return
  `TaxExportOutcome.Failure` with a clear message.
- `IosSecretProvider.getGeminiApiKey()` throws `IllegalStateException` if no
  `gemini_api_key` is in the Keychain. To enable Gemini on iOS, save the key
  via `IosSecurityBridge.saveSecret("gemini_api_key", "<key>")` from a debug
  screen or a one-shot Swift call.

## Caveats discovered during the audit (James please scan these)

1. **Deprecated UIKit API in `PlatformActionsBridge.swift`.**
   `UIApplication.shared.windows.first?.rootViewController` is deprecated since
   iOS 15. It still compiles and runs on iOS 14+, but Xcode will surface a
   warning. To silence: switch to
   `UIApplication.shared.connectedScenes.compactMap { ($0 as? UIWindowScene)?.keyWindow }.first?.rootViewController`
   or pass the presenting controller in.
2. **Same pattern in `IosPlatformActions.kt`**
   (`UIApplication.sharedApplication.keyWindow?.rootViewController`). Same
   deprecation, same severity (warning, not error).
3. **No App Groups / Push / Background Modes entitlements set up.** The app
   does not currently depend on any of these. If you decide to add push
   notifications later, add the Push capability and extend
   `iosApp/iosApp.entitlements` — out of scope for the first compile.
4. **Keychain sharing** is not enabled and not needed for the current
   single-app scope.
5. **`fallbackToDestructiveMigration()` is deprecated** on Room 2.7+ on both
   Android and iOS — the build warns but still works. The new API takes a
   boolean. Leaving as-is to keep behavior identical between platforms; this
   is a follow-up if you want it.
6. **Suspend bridges:** `IosAuthBridge.signInWithGoogle(idToken:)`,
   `IosAuthBridge.signOut()`, and `IosDriveAuthBridge.getDriveAccessToken()`
   are Kotlin `suspend fun` declared on an interface. Kotlin/Native exposes
   them as Obj-C completion-handler protocol methods; Swift automatically
   bridges these to `async throws` on the implementation side, which is
   exactly how `FirebaseAuthBridge.swift` and `DriveAuthBridge.swift` are
   written. If Xcode complains it cannot find the protocol selectors,
   double-check `SWIFT_VERSION >= 5.5` and `IPHONEOS_DEPLOYMENT_TARGET >=
   14.0` (the XcodeGen spec sets both).

---

## Gradle tasks confirmed green on Windows

- `./gradlew :androidApp:compileDebugKotlin`
- `./gradlew :androidApp:testDebugUnitTest`
- `./gradlew :shared:compileCommonMainKotlinMetadata`
- `./gradlew :composeApp:compileCommonMainKotlinMetadata`

`:shared:compileKotlinMetadata` / `:composeApp:compileKotlinMetadata` and any
`linkDebugFrameworkIos*` / `embedAndSignAppleFrameworkForXcode` tasks are
skipped on Windows because Kotlin/Native iOS targets are disabled there.
`bootstrap_ios.sh` runs the simulator-arm64 link as the smoke test on
James's Mac.

---

## Appendix — If `bootstrap_ios.sh` fails or you need to do it by hand

The bootstrap script is intentionally simple. If it fails at the smoke-test
step or you want to understand what it's doing, here is the equivalent
manual workflow.

### A. Generate the Xcode project manually

```bash
cd iosApp
xcodegen generate --spec project.yml --project .
```

If `xcodegen` is not installed: `brew install xcodegen`.

### B. Build the Kotlin/Native framework manually

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

On the first run this downloads Kotlin/Native, llvm, and a Konan-managed
toolchain — expect several minutes and a few hundred MB of cache writes.

### C. Full manual Xcode setup (only if XcodeGen is unavailable)

This is the legacy path — only use it if you can't get XcodeGen working.

1. Pull the repo on macOS with Xcode 15+ installed.
2. Create the `iosApp.xcodeproj` in `iosApp/`:
   - File → New → Project → iOS App, product name `iosApp`,
     Interface SwiftUI, Language Swift, organization identifier
     `com.fordham`, bundle id `com.fordham.toolbelt`.
   - Save it inside the existing `iosApp/` folder so the existing Swift
     sources sit next to the new `.xcodeproj`.
3. Add these existing Swift files from `iosApp/iosApp` to the iOS app target:
   - `iOSApp.swift`
   - `ContentView.swift`
   - `FirebaseAuthBridge.swift`
   - `KeyChainSecurityBridge.swift`
   - `PlatformActionsBridge.swift`
   - `DriveAuthBridge.swift`
4. Add SPM dependencies (File → Add Package Dependencies):
   - `https://github.com/firebase/firebase-ios-sdk` → 11.0.0 or newer.
     Products: `FirebaseCore`, `FirebaseAuth`.
   - `https://github.com/google/GoogleSignIn-iOS` → 7.1.0 or newer.
     Products: `GoogleSignIn`, `GoogleSignInSwift`.
5. Add `iosApp/iosApp/GoogleService-Info.plist` to the iOS app target.
6. Add `iosApp/iosApp/iosApp.entitlements` and point
   `CODE_SIGN_ENTITLEMENTS` at it.
7. Add `iosApp/iosApp/Info.plist` to the iOS app target. It already contains:
   - `NSCameraUsageDescription`
   - `NSPhotoLibraryUsageDescription`
   - `NSFaceIDUsageDescription`
   - `NSSpeechRecognitionUsageDescription`
   - `NSMicrophoneUsageDescription`
   - `CFBundleURLTypes` with the Google reversed client id
     `com.googleusercontent.apps.716278040823-6afkdloh5qs359mr896tl725ge7gl8h0`
8. Add a Run Script build phase **before** "Compile Sources" with shell `/bin/sh`:

   ```sh
   cd "$SRCROOT/.."
   ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
   ```

   - Input File List: leave empty.
   - Output Files: `$(BUILT_PRODUCTS_DIR)/ComposeApp.framework`.
9. Build settings:
   - `FRAMEWORK_SEARCH_PATHS` should include
     `$(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.
   - `ENABLE_USER_SCRIPT_SANDBOXING = NO` (otherwise the Gradle script in
     the build phase will be blocked).
   - `IPHONEOS_DEPLOYMENT_TARGET >= 14.0`.
10. `ComposeApp.framework` is embedded by the Gradle task above; it does not
    need a manual "Embed & Sign" entry as long as the Run Script build
    phase is present and points at `embedAndSignAppleFrameworkForXcode`.
11. Build the iOS Simulator (Any iOS Simulator) target first, then a
    physical device target.

### D. Common failure modes

| Symptom | Likely cause | Fix |
|---|---|---|
| `xcodegen: command not found` | XcodeGen not installed | `brew install xcodegen` |
| `Unable to locate a Java Runtime` | JAVA_HOME not set to 21 | `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` |
| `unable to read project ... is blocked from accessing` | Script sandboxing on | already off in `project.yml`; if you opened an old project, re-run `bootstrap_ios.sh` |
| `ld: framework not found ComposeApp` | Run Script phase didn't run, or wrong configuration name | Make sure the phase is BEFORE Compile Sources and that `$CONFIGURATION` matches (Debug / Release) |
| SPM "missing package product" | Stale `Package.resolved` | Xcode → File → Packages → Reset Package Caches, then re-run |
| `Operation not permitted` writing to derived data | Sandboxing still on | Confirm `ENABLE_USER_SCRIPT_SANDBOXING = NO` in build settings |
