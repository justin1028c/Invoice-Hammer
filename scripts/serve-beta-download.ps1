# Serves dist/release over HTTP so phones on the same network can download the beta APK.
param(
    [int]$Port = 8765
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $root "dist\release"
$apk = Join-Path $dist "androidApp-universal.apk"

if (-not (Test-Path $apk)) {
    Write-Host "APK missing - building first..."
    & "$PSScriptRoot\build-beta-download.ps1"
}

$ip = (Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object { $_.IPAddress -notlike "127.*" -and $_.PrefixOrigin -ne "WellKnown" } |
    Select-Object -First 1 -ExpandProperty IPAddress)

if (-not $ip) { $ip = "127.0.0.1" }

$url = "http://${ip}:${Port}/"
Write-Host ""
Write-Host "Beta download server"
Write-Host "  Folder: $dist"
Write-Host "  URL:    $url"
Write-Host "  APK:    ${url}androidApp-universal.apk"
Write-Host ""
Write-Host "On the phone: open the URL in Chrome, tap Download APK."
Write-Host "Press Ctrl+C to stop."
Write-Host ""

Set-Location $dist
python -m http.server $Port --bind 0.0.0.0
