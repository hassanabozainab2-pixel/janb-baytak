create or replace function public.guard_subscription_expiry_admin_only()
returns trigger
language plpgsql
set search_path = public, pg_temp
as $$
begin
  if new.subscription_expires_at is distinct from old.subscription_expires_at
     and not public.is_app_admin() then
    raise exception 'Subscription expiry can only be changed by an administrator';
  end if;
  return new;
end;
$$;
