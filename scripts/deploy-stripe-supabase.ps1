# Deploy Stripe Connect backend to Supabase (run from repo root after one-time login).
# Prerequisites:
#   1. npm install -g supabase   (or use npx supabase)
#   2. Create token: https://supabase.com/dashboard/account/tokens
#   3. $env:SUPABASE_ACCESS_TOKEN = "sbp_...."
#   4. STRIPE_SECRET_KEY already set in project Edge Function secrets

$ErrorActionPreference = "Stop"
$ProjectRef = "ygvqmexpvdsdnxzmlfml"
$Root = Split-Path -Parent $PSScriptRoot

if (-not $env:SUPABASE_ACCESS_TOKEN) {
    Write-Host "Missing SUPABASE_ACCESS_TOKEN." -ForegroundColor Red
    Write-Host "Create one at: https://supabase.com/dashboard/account/tokens"
    Write-Host 'Then:  $env:SUPABASE_ACCESS_TOKEN = "sbp_your_token"'
    exit 1
}

Set-Location $Root

Write-Host "Linking project $ProjectRef ..."
supabase link --project-ref $ProjectRef --yes

Write-Host "Pushing database migrations ..."
supabase db push --yes

Write-Host "Deploying stripe-payment-api ..."
supabase functions deploy stripe-payment-api --no-verify-jwt --project-ref $ProjectRef

Write-Host "Deploying stripe-webhook (optional) ..."
supabase functions deploy stripe-webhook --no-verify-jwt --project-ref $ProjectRef

Write-Host "Verifying endpoint ..."
$uri = "https://$ProjectRef.supabase.co/functions/v1/stripe-payment-api/v1/connect/status?userId=deploy-test"
try {
    $r = Invoke-WebRequest -Uri $uri -Method GET -UseBasicParsing
    Write-Host "HTTP $($r.StatusCode) - function is live." -ForegroundColor Green
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    if ($code -eq 400 -or $code -eq 401 -or $code -eq 200) {
        Write-Host "HTTP $code - function responded (not 404)." -ForegroundColor Green
    } else {
        Write-Host "HTTP $code - check deployment." -ForegroundColor Yellow
    }
}

Write-Host "Done. Rebuild app: .\gradlew.bat :androidApp:installDebug"
