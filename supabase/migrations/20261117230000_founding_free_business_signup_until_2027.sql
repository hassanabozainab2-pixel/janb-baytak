create or replace function public.set_founding_offer_business_expiry()
returns trigger
language plpgsql
set search_path = public, pg_temp
as $$
declare
  v_cutoff timestamptz := timestamptz '2027-01-01 00:00:00+02';
begin
  if now() >= v_cutoff then
    raise exception 'انتهت فترة التسجيل المجاني للأنشطة التجارية';
  end if;
  new.subscription_expires_at := v_cutoff;
  return new;
end;
$$;

drop trigger if exists businesses_founding_offer_expiry on public.businesses;
create trigger businesses_founding_offer_expiry
before insert on public.businesses
for each row execute function public.set_founding_offer_business_expiry();

drop policy if exists businesses_owner_insert on public.businesses;
create policy businesses_owner_insert
on public.businesses
for insert
to authenticated
with check (
  owner_id = (select auth.uid())
  and status = 'pending'
  and is_featured = false
);

alter table public.businesses disable trigger businesses_approval_admin_only;
alter table public.businesses disable trigger businesses_subscription_expiry_admin_only;

update public.businesses b
set subscription_expires_at = s.ends_at
from (
  select business_id, max(ends_at) as ends_at
  from public.subscriptions
  where status = 'active' and business_id is not null
  group by business_id
) s
where b.id = s.business_id
  and b.subscription_expires_at is null;

update public.businesses b
set subscription_expires_at = timestamptz '2027-01-01 00:00:00+02'
where b.subscription_expires_at is null
  and not exists (
    select 1 from public.subscriptions s
    where s.business_id = b.id and s.status = 'active' and s.ends_at > now()
  );

alter table public.businesses enable trigger businesses_approval_admin_only;
alter table public.businesses enable trigger businesses_subscription_expiry_admin_only;
