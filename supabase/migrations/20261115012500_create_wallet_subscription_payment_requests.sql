-- Paid subscription wallet transfer requests for Janb Baytak.
create table if not exists public.subscription_payment_requests (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.businesses(id) on delete cascade,
  owner_id uuid not null references auth.users(id) on delete cascade,
  plan_code text not null check (plan_code in ('weekly','monthly','yearly')),
  amount_egp integer not null check (
    (plan_code = 'weekly' and amount_egp = 50) or
    (plan_code = 'monthly' and amount_egp = 150) or
    (plan_code = 'yearly' and amount_egp = 1200)
  ),
  wallet_number text not null default '01101143675',
  payment_method text not null default 'etisalat_cash' check (payment_method = 'etisalat_cash'),
  transaction_reference text not null,
  status text not null default 'pending' check (status in ('pending','approved','rejected')),
  admin_note text,
  reviewed_at timestamptz,
  reviewed_by uuid references auth.users(id),
  created_at timestamptz not null default now()
);
create unique index if not exists subscription_payment_requests_transaction_reference_uidx
  on public.subscription_payment_requests (transaction_reference);
create index if not exists subscription_payment_requests_owner_created_idx
  on public.subscription_payment_requests (owner_id, created_at desc);
create index if not exists subscription_payment_requests_status_created_idx
  on public.subscription_payment_requests (status, created_at desc);
alter table public.subscription_payment_requests enable row level security;
grant select, insert, update on public.subscription_payment_requests to authenticated;
drop policy if exists subscription_payment_requests_owner_read on public.subscription_payment_requests;
create policy subscription_payment_requests_owner_read on public.subscription_payment_requests
for select to authenticated using ((select auth.uid()) = owner_id);
drop policy if exists subscription_payment_requests_owner_insert on public.subscription_payment_requests;
create policy subscription_payment_requests_owner_insert on public.subscription_payment_requests
for insert to authenticated with check (
  (select auth.uid()) = owner_id
  and exists (select 1 from public.businesses b where b.id = business_id and b.owner_id = (select auth.uid()))
  and status = 'pending'
  and reviewed_at is null and reviewed_by is null
);
drop policy if exists subscription_payment_requests_admin_all on public.subscription_payment_requests;
create policy subscription_payment_requests_admin_all on public.subscription_payment_requests
for all to authenticated using (public.is_app_admin()) with check (public.is_app_admin());
