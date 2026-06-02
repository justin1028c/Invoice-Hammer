# Foreman setup checklist

Use this once per machine / Supabase project so voice and typed Foreman commands reach Gemini through the server proxy (no API key in the app).

## 1. Supabase secrets

From repo root (after `supabase login`):

```powershell
.\scripts\deploy-foreman-gemini-supabase.ps1
```

Then set secrets (rotate any key that was ever embedded in the app):

```bash
supabase secrets set \
  GEMINI_API_KEY=your_google_ai_studio_key \
  FOREMAN_BACKEND_API_KEY=your_long_random_gate_secret \
  GEMINI_MODEL=gemini-3.1-flash-lite \
  --project-ref ygvqmexpvdsdnxzmlfml
```

| Secret | Purpose |
|--------|---------|
| `GEMINI_API_KEY` | Google AI Studio key (server only) |
| `FOREMAN_BACKEND_API_KEY` | Gate for `x-foreman-backend-key` header |
| `GEMINI_MODEL` | Optional fallback if app omits model in request |

## 2. App `local.properties`

Copy from `local.properties.example` and set:

```properties
foreman.gemini.backend.url=https://ygvqmexpvdsdnxzmlfml.supabase.co/functions/v1/foreman-gemini-api
foreman.backend.api.key=<same value as FOREMAN_BACKEND_API_KEY>
gemini.model.name=gemini-3.1-flash-lite
```

Rebuild the Android app after changing these values.

## 3. iOS (Mac build)

Same three values in `iosApp/project.yml` (or Xcode build settings): `FOREMAN_GEMINI_BACKEND_URL`, `FOREMAN_BACKEND_API_KEY`, `GEMINI_MODEL_NAME`. They are seeded into Keychain at launch via `iOSApp.swift`.

## 4. Verify

| Check | Expected |
|-------|----------|
| Tab-only voice (“open new invoice tab”) | Instant local nav via `ForemanTabNavigation` — no network |
| Foreman with Pro + tokens | LLM tools via proxy; no “Gemini config not ready” |
| Missing `foreman.*` in `local.properties` | Foreman LLM fails fast; tab nav still works |

## 5. Smoke on device (manual)

1. “Open new invoice tab” → lands on New Invoice tab in one turn.
2. “Find client Acme” → client search (requires proxy + Pro).
3. Quick invoice with line items → save confirm flow.

Details: `supabase/functions/foreman-gemini-api/README.md`
