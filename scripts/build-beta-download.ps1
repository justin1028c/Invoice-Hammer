# Builds obfuscated beta AAB + universal APK (debug-signed for Google Sign-In / Firebase).
param(
    [switch]$SkipGradle,
    [switch]$ProductionSigning
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$localProps = Join-Path $root "local.properties"
if (-not (Test-Path $localProps)) {
    Write-Host "No local.properties - run scripts/create-beta-keystore.ps1 first."
    & "$PSScriptRoot\create-beta-keystore.ps1"
}

$gradleTarget = if ($ProductionSigning) { "bundleRelease" } else { "bundleBeta" }
$variant = if ($ProductionSigning) { "release" } else { "beta" }

if ($ProductionSigning) {
    $keystore = Join-Path $root "keystore\invoicehammer-beta.jks"
    if (-not (Test-Path $keystore)) {
        & "$PSScriptRoot\create-beta-keystore.ps1"
    }
}

$jbr = "C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
$btDir = Join-Path $root "tools"
$btJar = Join-Path $btDir "bundletool.jar"
$dist = Join-Path $root "dist\release"
New-Item -ItemType Directory -Force -Path $btDir, $dist | Out-Null

if (-not (Test-Path $btJar)) {
    Write-Host "Downloading bundletool..."
    Invoke-WebRequest -Uri "https://github.com/google/bundletool/releases/download/1.17.2/bundletool-all-1.17.2.jar" `
        -OutFile $btJar -UseBasicParsing
}

if (-not $SkipGradle) {
    if ($ProductionSigning) {
        Write-Host "Building production-signed release (requires beta SHA-1 in Firebase)..."
    } else {
        Write-Host "Building obfuscated beta (debug-signed, Google Sign-In works with current Firebase)..."
    }
    & .\gradlew.bat ":androidApp:$gradleTarget" --no-daemon
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$bundle = Join-Path $root "app\build\outputs\bundle\$variant\androidApp-$variant.aab"
if (-not (Test-Path $bundle)) {
    throw "AAB not found: $bundle"
}

if ($ProductionSigning) {
    $props = @{}
    Get-Content $localProps | ForEach-Object {
        if ($_ -match '^\s*([^#=]+)=(.*)$') { $props[$matches[1].Trim()] = $matches[2].Trim() }
    }
    $storeFile = Join-Path $root ($props["release.storeFile"] -replace '/', '\')
    $storePass = $props["release.storePassword"]
    $keyAlias = $props["release.keyAlias"]
    $keyPass = $props["release.keyPassword"]
} else {
    $storeFile = "$env:USERPROFILE\.android\debug.keystore"
    $storePass = "android"
    $keyAlias = "androiddebugkey"
    $keyPass = "android"
}

Copy-Item $bundle (Join-Path $dist "androidApp-$variant.aab") -Force

$apksOut = Join-Path $dist "androidApp-universal.apks"
$apkOut = Join-Path $dist "androidApp-universal.apk"

Write-Host "Building universal signed APK..."
& $jbr -jar $btJar build-apks `
    --bundle=$bundle `
    --output=$apksOut `
    --mode=universal `
    --ks=$storeFile `
    --ks-pass="pass:$storePass" `
    --ks-key-alias=$keyAlias `
    --key-pass="pass:$keyPass" `
    --overwrite

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($apksOut)
$entry = $zip.Entries | Where-Object { $_.Name -eq "universal.apk" } | Select-Object -First 1
if (-not $entry) { $zip.Dispose(); throw "universal.apk not found inside $apksOut" }
[System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $apkOut, $true)
$zip.Dispose()

$apkInfo = Get-Item $apkOut
$aabInfo = Get-Item (Join-Path $dist "androidApp-$variant.aab")
$sizeMb = [math]::Round($apkInfo.Length / 1MB, 1)
$version = "1.0 (1)"

$html = @"
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Invoice Hammer - Android Beta</title>
  <style>
    body { font-family: system-ui, sans-serif; max-width: 32rem; margin: 2rem auto; padding: 0 1rem; }
    a.btn { display: inline-block; background: #1b5e20; color: #fff; padding: 1rem 1.5rem;
      border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 1.1rem; }
    ol { line-height: 1.6; }
    .meta { color: #555; font-size: 0.9rem; }
  </style>
</head>
<body>
  <h1>Invoice Hammer Beta</h1>
  <p class="meta">Version $version | Universal APK | ${sizeMb} MB | Android 8.0+</p>
  <p><a class="btn" href="androidApp-universal.apk" download>Download APK</a></p>
  <h2>Install on your phone</h2>
  <ol>
    <li>Connect to the <strong>same Wi-Fi</strong> as the computer hosting this page.</li>
    <li>Tap <strong>Download APK</strong> above.</li>
    <li>Allow install from browser / unknown sources when prompted.</li>
    <li>Open the downloaded file and tap <strong>Install</strong>.</li>
  </ol>
  <p class="meta">AAB on this server: <code>androidApp-$variant.aab</code></p>
</body>
</html>
"@
Set-Content -Path (Join-Path $dist "index.html") -Value $html -Encoding UTF8

Write-Host ""
Write-Host "Done."
Write-Host "  APK: $apkOut"
Write-Host "  AAB: $(Join-Path $dist "androidApp-$variant.aab")"
Write-Host "  Page: $(Join-Path $dist 'index.html')"
Write-Host ""
Write-Host "Start download server:  .\scripts\serve-beta-download.ps1"
