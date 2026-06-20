-- Authoritative paid-state for Stripe Checkout (remote client pay + contractor polling).

create table if not exists public.stripe_invoice_payments (
    invoice_id text primary key,
    contractor_user_id text not null,
    checkout_session_id text,
    payment_intent_id text,
    status text not null default 'pending',
    paid_at timestamptz,
    updated_at timestamptz not null default now()
);

create unique index if not exists stripe_invoice_payments_session_idx
    on public.stripe_invoice_payments (checkout_session_id)
    where checkout_session_id is not null;

create index if not exists stripe_invoice_payments_contractor_idx
    on public.stripe_invoice_payments (contractor_user_id);

alter table public.stripe_invoice_payments enable row level security;
