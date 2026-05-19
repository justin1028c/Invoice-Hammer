#!/bin/bash
#
# bootstrap_ios.sh — one-shot iOS build environment bootstrapper for
# Invoice Hammer. Designed to be run from a fresh clone of the repo on macOS:
#
#     cd /path/to/InvoiceApp
#     bash iosApp/bootstrap_ios.sh
#
# What it does, in order:
#   1. Verifies XcodeGen is installed.
#   2. (Soft check) Verifies Java 21 is on PATH.
#   3. Generates `iosApp/iosApp.xcodeproj` from `iosApp/project.yml`.
#   4. Runs the Kotlin/Native linker for the iOS simulator (arm64) — this
#      is the real smoke test for the iOS build and is the first command
#      that actually exercises iosMain Kotlin against Apple toolchains.
#
# This script DOES NOT open Xcode and DOES NOT try to build the Xcode
# project — that requires a Development Team and is left to James.
#
set -euo pipefail

# ----- styling helpers -----------------------------------------------------
if [[ -t 1 ]]; then
    BOLD="$(printf '\033[1m')"
    RED="$(printf '\033[31m')"
    GREEN="$(printf '\033[32m')"
    YELLOW="$(printf '\033[33m')"
    CYAN="$(printf '\033[36m')"
    RESET="$(printf '\033[0m')"
else
    BOLD=""; RED=""; GREEN=""; YELLOW=""; CYAN=""; RESET=""
fi

step()    { echo "${BOLD}${CYAN}==>${RESET} ${BOLD}$*${RESET}"; }
ok()      { echo "${GREEN}    ✓${RESET} $*"; }
warn()    { echo "${YELLOW}    ! $*${RESET}"; }
fail()    { echo "${RED}    ✗ $*${RESET}" >&2; }

# ----- locate repo root ----------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

echo
echo "${BOLD}Invoice Hammer — iOS bootstrap${RESET}"
echo "Repo root: $REPO_ROOT"
echo

# ----- 1. xcodegen --------------------------------------------------------
step "Checking for XcodeGen"
if ! command -v xcodegen >/dev/null 2>&1; then
    fail "xcodegen not found on PATH."
    echo
    echo "    Install it with Homebrew:"
    echo "        ${BOLD}brew install xcodegen${RESET}"
    echo
    echo "    Or via Mint / from source:"
    echo "        https://github.com/yonaskolb/XcodeGen#installing"
    echo
    exit 1
fi
ok "$(xcodegen --version 2>&1 | head -n 1)"

# ----- 2. java 21 ---------------------------------------------------------
step "Checking for Java 21"
JAVA_VERSION_OUTPUT="$(java -version 2>&1 || true)"
if echo "$JAVA_VERSION_OUTPUT" | grep -E 'version "21' >/dev/null; then
    ok "$(echo "$JAVA_VERSION_OUTPUT" | head -n 1)"
else
    warn "Java 21 not detected on PATH."
    warn "Detected: $(echo "$JAVA_VERSION_OUTPUT" | head -n 1 || echo 'none')"
    warn "Android Studio's bundled JDK is fine — Gradle will pick it up via JAVA_HOME."
    warn "If gradlew fails below, install temurin-21 (e.g. \`brew install --cask temurin@21\`)."
fi

# ----- 3. xcodegen generate ----------------------------------------------
step "Generating iosApp.xcodeproj from iosApp/project.yml"
if [[ ! -f "$REPO_ROOT/iosApp/project.yml" ]]; then
    fail "iosApp/project.yml not found at $REPO_ROOT/iosApp/project.yml."
    exit 1
fi
(
    cd "$REPO_ROOT/iosApp"
    xcodegen generate --spec project.yml --project .
)
ok "Generated $REPO_ROOT/iosApp/iosApp.xcodeproj"

# ----- 4. gradle smoke test ----------------------------------------------
step "Smoke test: linkDebugFrameworkIosSimulatorArm64"
echo "    (This is the first command that compiles iosMain Kotlin against Apple toolchains.)"
echo "    (It will take a few minutes on the first run while KMP/Native warms its caches.)"
echo

if [[ ! -x "$REPO_ROOT/gradlew" ]]; then
    chmod +x "$REPO_ROOT/gradlew"
fi

if ! "$REPO_ROOT/gradlew" :composeApp:linkDebugFrameworkIosSimulatorArm64 --console=plain; then
    fail "Gradle smoke test failed."
    echo
    echo "    The most common causes are:"
    echo "      • Wrong JDK on PATH (need 21). Try \`export JAVA_HOME=\$(/usr/libexec/java_home -v 21)\`."
    echo "      • Xcode command line tools not selected. Try \`sudo xcode-select -s /Applications/Xcode.app/Contents/Developer\`."
    echo "      • Stale Gradle cache. Try \`./gradlew --stop && rm -rf ~/.gradle/caches/transforms-*\`."
    echo
    echo "    See iosApp/IOS_BUILD_HANDOFF.md → 'If bootstrap_ios.sh fails' for more."
    exit 1
fi
ok "Kotlin/Native iOS simulator framework linked successfully."

# ----- success banner ----------------------------------------------------
echo
echo "${GREEN}${BOLD}╔════════════════════════════════════════════════════════════════╗${RESET}"
echo "${GREEN}${BOLD}║                  iOS bootstrap complete.                       ║${RESET}"
echo "${GREEN}${BOLD}╚════════════════════════════════════════════════════════════════╝${RESET}"
echo
echo "Next steps:"
echo "  1. ${BOLD}open iosApp/iosApp.xcodeproj${RESET}"
echo "  2. Select the ${BOLD}iosApp${RESET} target → Signing & Capabilities → set ${BOLD}Team${RESET}."
echo "  3. Pick a simulator (e.g. iPhone 15) and hit ⌘B to build, then ⌘R to run."
echo
echo "Reminder: ${BOLD}iosApp/iosApp.xcodeproj${RESET} is git-ignored — do not commit it."
echo "Edit ${BOLD}iosApp/project.yml${RESET} and re-run this script to change project config."
echo
