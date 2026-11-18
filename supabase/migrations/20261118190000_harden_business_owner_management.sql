-- Secure business ownership and lifecycle fields from owner-side updates.
alter table public.businesses alter column is_active set default false;

create or replace function public.guard_business_approval_admin_only()
returns trigger
language plpgsql
set search_path = public, pg_temp
as $function$
begin
  if not public.is_app_admin() then
    if tg_op = 'INSERT' then
      if new.status is distinct from 'pending'
         or new.is_active is distinct from false
         or new.is_featured is distinct from false
         or new.subscription_expires_at is not null then
        raise exception 'Only the designated administrator can approve, activate, feature, or extend a business';
      end if;
    else
      if new.status is distinct from old.status
         or new.is_active is distinct from old.is_active
         or new.is_featured is distinct from old.is_featured
         or new.subscription_expires_at is distinct from old.subscription_expires_at then
        raise exception 'Only the designated administrator can approve, activate, feature, or extend a business';
      end if;
    end if;
  end if;
  return new;
end;
$function$;

drop policy if exists businesses_admin_read on public.businesses;
drop policy if exists businesses_admin_update on public.businesses;
