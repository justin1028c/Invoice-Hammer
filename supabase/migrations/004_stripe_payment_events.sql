-- Optional audit log for Stripe webhooks (stripe-webhook Edge Function).

create table if not exists public.stripe_payment_events (
    id bigserial primary key,
    stripe_event_id text not null unique,
    payment_intent_id text not null,
    invoice_id text,
    contractor_user_id text,
    amount_cents bigint,
    currency text,
    status text not null,
    raw_metadata jsonb,
    created_at timestamptz not null default now()
);

create index if not exists stripe_payment_events_invoice_idx
    on public.stripe_payment_events (invoice_id);

alter table public.stripe_payment_events enable row level security;
