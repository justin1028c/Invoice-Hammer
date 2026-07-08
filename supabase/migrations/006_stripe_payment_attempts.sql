-- Immutable server snapshots and idempotent Stripe payment operations.
create table if not exists public.stripe_invoice_snapshots (
    contractor_user_id text not null,
    invoice_id text not null,
    amount_cents bigint not null check (amount_cents >= 50),
    currency text not null check (currency ~ '^[a-z]{3}$'),
    created_at timestamptz not null default now(),
    primary key (contractor_user_id, invoice_id)
);

create table if not exists public.stripe_payment_attempts (
    operation_id uuid primary key,
    contractor_user_id text not null,
    invoice_id text not null,
    amount_cents bigint not null check (amount_cents >= 50),
    currency text not null check (currency ~ '^[a-z]{3}$'),
    stripe_account_id text not null,
    payment_provider text not null,
    payment_intent_id text unique,
    checkout_session_id text unique,
    status text not null default 'creating',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    foreign key (contractor_user_id, invoice_id)
      references public.stripe_invoice_snapshots(contractor_user_id, invoice_id)
);

alter table public.stripe_invoice_snapshots enable row level security;
alter table public.stripe_payment_attempts enable row level security;

-- Service-role Edge Functions only. No anon/authenticated table policies.
