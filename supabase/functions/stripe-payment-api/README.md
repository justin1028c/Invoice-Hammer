# stripe-payment-api (Supabase Edge Function)

Implements the HTTP contract used by `KtorStripePaymentBackendClient` in the KMP app.

## Deploy

1. Run SQL migrations in the Supabase dashboard (or `supabase db push`):
   - `003_stripe_connect_accounts.sql`
   - `004_stripe_payment_events.sql` (optional webhook audit log)

2. Set function secrets (Dashboard → Edge Functions → Secrets, or CLI):

```bash
supabase secrets set \
  STRIPE_SECRET_KEY=sk_test_... \
  STRIPE_WEBHOOK_SECRET=whsec_... \
  STRIPE_BACKEND_API_KEY=your-random-shared-secret \
  # Optional — defaults to hosted pages on this Edge Function:
  # STRIPE_CONNECT_RETURN_URL=https://YOUR_PROJECT.supabase.co/functions/v1/stripe-payment-api/v1/connect/return
  # STRIPE_CONNECT_REFRESH_URL=.../v1/connect/refresh
```

`SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` are injected automatically when deployed on Supabase.

3. Deploy the function:

```bash
supabase functions deploy stripe-payment-api --no-verify-jwt
```

4. Point the app at the function **base URL** (no trailing slash):

```properties
stripe.payment.backend.url=https://YOUR_PROJECT.supabase.co/functions/v1/stripe-payment-api
stripe.backend.api.key=your-random-shared-secret
```

The app calls:

- `POST …/v1/payments/intent`
- `GET …/v1/connect/status?userId=…`

## Endpoints

### `POST /v1/payments/intent`

Creates a **direct charge** on the contractor’s Connect Express account with `application_fee_amount`.

| `paymentProvider` | Behavior |
|-------------------|----------|
| `google_pay`, `apple_pay`, `card_link` | Stripe Checkout → `hostedCheckoutUrl` + `clientSecret` |
| `card_terminal`, `tap_to_pay`, `bluetooth_reader` | PaymentIntent only → `clientSecret` (Payment Sheet / Terminal) |

If Connect onboarding is incomplete, returns **409** with `{ "error": "…", "onboardingUrl": "…" }`.

### `GET /v1/connect/status?userId=…`

Returns `{ accountId, chargesEnabled, payoutsEnabled }` (matches `StripeConnectAccountStatusResponse`).

### `POST /v1/connect/onboard` (optional)

Body: `{ "contractorUserId": "…" }` → `{ "onboardingUrl", "accountId" }` for Settings / deep link.

## Webhooks

Deploy `stripe-webhook` and register in Stripe Dashboard:

```bash
supabase functions deploy stripe-webhook --no-verify-jwt
```

Webhook URL: `https://YOUR_PROJECT.supabase.co/functions/v1/stripe-webhook`

Events: `payment_intent.succeeded` (logs + optional `stripe_payment_events` row).

## Security

- Never put `STRIPE_SECRET_KEY` in the mobile app.
- Set `STRIPE_BACKEND_API_KEY` and the same value in `local.properties` as `stripe.backend.api.key` (sent as `x-stripe-backend-key`).
- `verify_jwt = false` in `config.toml` because the app uses Firebase auth, not Supabase Auth JWTs.

## Test mode

Use Stripe test keys (`sk_test_`, `pk_test_`). Create a test Connect account by triggering any payment or `GET /v1/connect/status` for your Firebase user id.
