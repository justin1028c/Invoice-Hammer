# Builds the fast debug APK and prepares it for wireless beta download.
param(
    [switch]$SkipGradle
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$dist = Join-Path $root "dist\release"
New-Item -ItemType Directory -Force -Path $dist | Out-Null

$apkSource = Join-Path $root "app\build\outputs\apk\debug\androidApp-debug.apk"
$apkDest = Join-Path $dist "androidApp-universal.apk"

if (-not $SkipGradle) {
    Write-Host "Building fast debug APK..."
    & .\gradlew.bat ":androidApp:assembleDebug"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

if (-not (Test-Path $apkSource)) {
    throw "Debug APK not found: $apkSource"
}

Copy-Item $apkSource $apkDest -Force

$apkInfo = Get-Item $apkDest
$sizeMb = [math]::Round($apkInfo.Length / 1MB, 1)
$version = "1.0-debug"

$html = @"
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Invoice Hammer - Android Debug Build</title>
  <style>
    body { font-family: system-ui, sans-serif; max-width: 32rem; margin: 2rem auto; padding: 0 1rem; }
    a.btn { display: inline-block; background: #0288d1; color: #fff; padding: 1rem 1.5rem;
      border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 1.1rem; }
    ol { line-height: 1.6; }
    .meta { color: #555; font-size: 0.9rem; }
    .warning { background: #fff3cd; color: #664d03; padding: 0.75rem; border-radius: 6px; margin: 1rem 0; border: 1px solid #ffe69c; }
  </style>
</head>
<body>
  <h1>Invoice Hammer (Debug)</h1>
  <div class="warning">
    <strong>Developer Build:</strong> This is a non-obfuscated fast debug build.
  </div>
  <p class="meta">Version $version | $sizeMb MB | Android 8.0+</p>
  <p><a class="btn" href="androidApp-universal.apk" download>Download Debug APK</a></p>
  <h2>Install on your phone</h2>
  <ol>
    <li>Connect to the <strong>same Wi-Fi</strong> as the computer hosting this page.</li>
    <li>Tap <strong>Download Debug APK</strong> above.</li>
    <li>Allow install from browser / unknown sources when prompted.</li>
    <li>Open the downloaded file and tap <strong>Install</strong>.</li>
  </ol>
</body>
</html>
"@

Set-Content -Path (Join-Path $dist "index.html") -Value $html -Encoding UTF8

Write-Host ""
Write-Host "Done."
Write-Host "  Fast Debug APK: $apkDest"
Write-Host "  Page:           $(Join-Path $dist 'index.html')"
Write-Host ""
Write-Host "Start download server if not running:  .\scripts\serve-beta-download.ps1"
