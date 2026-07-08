# stripe-payment-api (Supabase Edge Function)

Implements the HTTP contract used by `KtorStripePaymentBackendClient` in the KMP app.

## Deploy

1. Run SQL migrations in the Supabase dashboard (or `supabase db push`):
   - `003_stripe_connect_accounts.sql`
   - `004_stripe_payment_events.sql` (optional webhook audit log)
   - `005_stripe_invoice_payments.sql` (paid-state for contractor polling)
   - `006_stripe_payment_attempts.sql` (frozen invoice terms + idempotent attempts)
   - `007_stripe_payment_lifecycle.sql` (refund/dispute/failure state)

2. Set function secrets (Dashboard → Edge Functions → Secrets, or CLI):

```bash
supabase secrets set \
  STRIPE_SECRET_KEY=sk_test_... \
  STRIPE_WEBHOOK_SECRET=whsec_... \
  FIREBASE_PROJECT_ID=your-firebase-project-id \
  STRIPE_APPLICATION_FEE_BPS=100 \
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
```

The app calls:

- `POST …/v1/payments/intent`
- `GET …/v1/payments/verify?session_id=…`
- `GET …/v1/payments/invoice-status?invoiceId=…&contractorUserId=…`
- `GET …/v1/connect/status?userId=…`

Public browser pages (no auth):

- `GET …/v1/payments/success?invoice_id=…&session_id=…` → redirects to `invoicehammer://payment-success`
- `GET …/v1/payments/cancel?invoice_id=…` → redirects to `invoicehammer://payment-cancelled`

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

Events: `payment_intent.succeeded`, `checkout.session.completed` (updates `stripe_invoice_payments`).

## Security

- Never put `STRIPE_SECRET_KEY` in the mobile app.
- Every operational endpoint requires a Firebase ID token. The function verifies
  its Google signature, issuer, audience, and expiry, then derives the contractor
  identity from the token subject.
- `STRIPE_APPLICATION_FEE_BPS` is server-owned. Never accept a platform fee from
  the mobile request.
- Apply migrations through `007` before deploying either Stripe function. Each authenticated
  payment publication freezes its invoice amount/currency, and each `operationId`
  maps to one Stripe object through Stripe's idempotency mechanism.
- `verify_jwt = false` in `config.toml` because the app uses Firebase auth, not Supabase Auth JWTs.

## Test mode

Use Stripe test keys (`sk_test_`, `pk_test_`). Create a test Connect account by triggering any payment or `GET /v1/connect/status` for your Firebase user id.
