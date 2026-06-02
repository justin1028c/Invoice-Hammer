# Prints SHA-1 / SHA-256 for Firebase Google Sign-In configuration.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

Write-Host "=== Debug keystore (Android Studio default) ==="
$debugKs = "$env:USERPROFILE\.android\debug.keystore"
if (Test-Path $debugKs) {
    keytool -list -v -keystore $debugKs -storepass android -alias androiddebugkey 2>&1 |
        Select-String -Pattern "SHA1:|SHA256:"
} else {
    Write-Host "Debug keystore not found."
}

Write-Host ""
Write-Host "=== Beta release keystore (InvoiceHammer-1.0-beta.apk) ==="
$betaKs = Join-Path $root "keystore\invoicehammer-beta.jks"
if (Test-Path $betaKs) {
    $pass = "InvoiceHammerBeta2026"
    if (Test-Path (Join-Path $root "local.properties")) {
        $lp = Get-Content (Join-Path $root "local.properties") -Raw
        if ($lp -match 'release\.storePassword=(.+)') { $pass = $matches[1].Trim() }
    }
    keytool -list -v -keystore $betaKs -storepass $pass 2>&1 |
        Select-String -Pattern "SHA1:|SHA256:"
} else {
    Write-Host "Run scripts/create-beta-keystore.ps1 first."
}

Write-Host ""
Write-Host "Add each SHA-1 to Firebase Console:"
Write-Host "  Project settings -> Your apps -> Android (com.fordham.toolbelt) -> Add fingerprint"
Write-Host "Then download an updated google-services.json into app/google-services.json"
