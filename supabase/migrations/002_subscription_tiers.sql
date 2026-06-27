-- Invoice Hammer — subscription tiers + user entitlements (Google Play / App Store)

create table if not exists public.subscription_tiers (
    id text primary key,
    display_name text not null,
    description text not null default '',
    sort_order int not null default 0,
    google_play_product_id text,
    apple_product_id text,
    billing_period text not null default 'monthly',
    price_label text not null default '',
    features jsonb not null default '{}'::jsonb,
    is_active boolean not null default true,
    updated_at timestamptz not null default now()
);

create table if not exists public.user_entitlements (
    user_id text primary key,
    tier_id text not null references public.subscription_tiers (id),
    source text not null default 'manual',
    purchase_token text,
    expires_at_millis bigint,
    updated_at_millis bigint not null,
    updated_at timestamptz not null default now()
);

create index if not exists user_entitlements_tier_idx on public.user_entitlements (tier_id);

alter table public.subscription_tiers enable row level security;
alter table public.user_entitlements enable row level security;

drop policy if exists subscription_tiers_read on public.subscription_tiers;
create policy subscription_tiers_read
    on public.subscription_tiers for select using (true);

drop policy if exists user_entitlements_dev_all on public.user_entitlements;
drop policy if exists user_entitlements_user_policy on public.user_entitlements;

create policy user_entitlements_user_policy
    on public.user_entitlements
    for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

insert into public.subscription_tiers (
    id, display_name, description, sort_order,
    google_play_product_id, apple_product_id, billing_period, price_label, features
) values
(
    'free',
    'Free',
    'Core invoicing and client tools.',
    0,
    null,
    null,
    'none',
    'Free',
    '{"ai_agent":false,"receipt_ocr":false,"tax_export":false,"bento_reports":false,"foreman_agent":false}'::jsonb
),
(
    'pro_monthly',
    'Pro Monthly',
    'AI Command Center, receipt OCR, Bento reports, and tax bundles.',
    1,
    'invoice_hammer_pro_monthly',
    'invoice_hammer_pro_monthly',
    'monthly',
    'Pro / month',
    '{"ai_agent":true,"receipt_ocr":true,"tax_export":true,"bento_reports":true,"foreman_agent":true,"bluetooth_card_reader":true,"recurring_card_billing":true,"instant_payouts":true}'::jsonb
),
(
    'pro_yearly',
    'Pro Yearly',
    'All Pro features — best value annually.',
    2,
    'invoice_hammer_pro_yearly',
    'invoice_hammer_pro_yearly',
    'yearly',
    'Pro / year',
    '{"ai_agent":true,"receipt_ocr":true,"tax_export":true,"bento_reports":true,"foreman_agent":true,"bluetooth_card_reader":true,"recurring_card_billing":true,"instant_payouts":true}'::jsonb
)
on conflict (id) do update set
    display_name = excluded.display_name,
    description = excluded.description,
    google_play_product_id = excluded.google_play_product_id,
    apple_product_id = excluded.apple_product_id,
    features = excluded.features,
    price_label = excluded.price_label,
    updated_at = now();
