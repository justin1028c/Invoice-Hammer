# foreman-gemini-api (Supabase Edge Function)

Proxies Google Gemini `generateContent` so the **Gemini API key never ships in the mobile app** (Phase A).

## Secrets

```bash
supabase secrets set \
  GEMINI_API_KEY=your_google_ai_studio_key \
  FOREMAN_BACKEND_API_KEY=your_long_random_gate_secret \
  GEMINI_MODEL=gemini-3.1-flash-lite \
  --project-ref YOUR_PROJECT_REF
```

Rotate any key that was previously embedded in the Android native library or iOS plist.

## Deploy

```bash
supabase functions deploy foreman-gemini-api --no-verify-jwt --project-ref YOUR_PROJECT_REF
```

## App configuration (`local.properties`)

```properties
foreman.gemini.backend.url=https://YOUR_PROJECT.supabase.co/functions/v1/foreman-gemini-api
foreman.backend.api.key=your_long_random_gate_secret
gemini.model.name=gemini-3.1-flash-lite
```

Must match `FOREMAN_BACKEND_API_KEY` on the function.

## Endpoint

### `POST /v1/generate`

**Headers:** `x-foreman-backend-key: <gate secret>` (if configured on server)

**Body:**

```json
{
  "model": "gemini-3.1-flash-lite",
  "request": { "contents": [ ... ], "generationConfig": { ... } }
}
```

`request` is the standard Gemini `generateContent` JSON (same shape the app used when calling Google directly).

**Response:** Raw Gemini API JSON passthrough.
