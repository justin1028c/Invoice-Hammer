alter table public.stripe_invoice_payments
    add column if not exists refunded_at timestamptz,
    add column if not exists disputed_at timestamptz,
    add column if not exists failure_code text;

create index if not exists stripe_payment_attempts_intent_idx
    on public.stripe_payment_attempts (payment_intent_id)
    where payment_intent_id is not null;
