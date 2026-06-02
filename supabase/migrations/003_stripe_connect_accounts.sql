-- Stripe Connect: maps Firebase / Google contractor user id → Stripe Express account id.
-- Written only by Edge Functions (service role). Mobile app never reads this table directly.

create table if not exists public.stripe_connect_accounts (
    contractor_user_id text primary key,
    stripe_account_id text not null unique,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists stripe_connect_accounts_stripe_id_idx
    on public.stripe_connect_accounts (stripe_account_id);

alter table public.stripe_connect_accounts enable row level security;

-- No anon/authenticated policies: access via service role in Edge Functions only.
