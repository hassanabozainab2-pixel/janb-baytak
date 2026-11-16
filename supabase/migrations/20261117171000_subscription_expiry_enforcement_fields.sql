alter table public.businesses add column if not exists subscription_expires_at timestamptz;
alter table public.doctors add column if not exists subscription_expires_at timestamptz;
create or replace function public.guard_subscription_expiry_admin_only()
returns trigger language plpgsql as $$
begin
  if new.subscription_expires_at is distinct from old.subscription_expires_at
     and not public.is_app_admin() then
    raise exception 'Subscription expiry can only be changed by an administrator';
  end if;
  return new;
end;
$$;
drop trigger if exists businesses_subscription_expiry_admin_only on public.businesses;
create trigger businesses_subscription_expiry_admin_only
before update of subscription_expires_at on public.businesses
for each row execute function public.guard_subscription_expiry_admin_only();
drop trigger if exists doctors_subscription_expiry_admin_only on public.doctors;
create trigger doctors_subscription_expiry_admin_only
before update of subscription_expires_at on public.doctors
for each row execute function public.guard_subscription_expiry_admin_only();
