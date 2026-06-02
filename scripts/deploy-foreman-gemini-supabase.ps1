# Deploy Foreman Gemini proxy to Supabase (run from repo root).
# Prerequisites:
#   supabase login  OR  $env:SUPABASE_ACCESS_TOKEN = "sbp_...."
#   supabase secrets set GEMINI_API_KEY=... FOREMAN_BACKEND_API_KEY=... GEMINI_MODEL=...

$ErrorActionPreference = "Stop"
$ProjectRef = "ygvqmexpvdsdnxzmlfml"
$Root = Split-Path -Parent $PSScriptRoot

if (-not $env:SUPABASE_ACCESS_TOKEN) {
    $loggedIn = supabase projects list 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Log in with: supabase login" -ForegroundColor Red
        Write-Host 'Or: $env:SUPABASE_ACCESS_TOKEN = "sbp_..."'
        exit 1
    }
}

Set-Location $Root

Write-Host "Linking project $ProjectRef ..."
supabase link --project-ref $ProjectRef --yes

Write-Host "Deploying foreman-gemini-api ..."
supabase functions deploy foreman-gemini-api --no-verify-jwt --project-ref $ProjectRef

Write-Host ""
Write-Host "Set secrets (if not already):" -ForegroundColor Yellow
Write-Host "  supabase secrets set GEMINI_API_KEY=... FOREMAN_BACKEND_API_KEY=... GEMINI_MODEL=gemini-3.1-flash-lite --project-ref $ProjectRef"
Write-Host ""
Write-Host "App local.properties:" -ForegroundColor Yellow
Write-Host "  foreman.gemini.backend.url=https://$ProjectRef.supabase.co/functions/v1/foreman-gemini-api"
Write-Host "  foreman.backend.api.key=<same as FOREMAN_BACKEND_API_KEY secret>"
Write-Host ""
Write-Host "Rotate any Gemini key previously embedded in the mobile app." -ForegroundColor Yellow
