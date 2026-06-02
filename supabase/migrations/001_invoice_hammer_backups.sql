-- Invoice Hammer — Supabase backup table (run in Supabase SQL Editor)
-- Stores one JSON backup snapshot per signed-in Firebase/Google user id.

create table if not exists public.invoice_hammer_backups (
    user_id text primary key,
    backup_json jsonb not null,
    exported_at_millis bigint not null,
    schema_version int not null default 1,
    updated_at timestamptz not null default now()
);

create index if not exists invoice_hammer_backups_exported_idx
    on public.invoice_hammer_backups (exported_at_millis desc);

alter table public.invoice_hammer_backups enable row level security;

-- DEV / demo: permissive policy when using the anon key from the mobile app.
-- Tighten before production (e.g. Supabase Auth JWT: auth.uid()::text = user_id).
drop policy if exists invoice_hammer_backup_dev_all on public.invoice_hammer_backups;
create policy invoice_hammer_backup_dev_all
    on public.invoice_hammer_backups
    for all
    using (true)
    with check (true);
