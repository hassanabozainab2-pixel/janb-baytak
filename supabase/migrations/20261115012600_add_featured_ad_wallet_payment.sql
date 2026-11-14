alter table public.ads
  add column if not exists payment_reference text,
  add column if not exists payment_amount integer not null default 200,
  add column if not exists payment_status text not null default 'pending';
alter table public.ads drop constraint if exists ads_payment_amount_check;
alter table public.ads add constraint ads_payment_amount_check check (payment_amount = 200);
alter table public.ads drop constraint if exists ads_payment_status_check;
alter table public.ads add constraint ads_payment_status_check check (payment_status in ('pending','approved','rejected'));
