-- Give newly created doctor profiles the founding offer expiry.
create or replace function public.set_founding_offer_doctor_expiry()
returns trigger
language plpgsql
set search_path = public, pg_temp
as $function$
declare
  v_cutoff timestamptz := timestamptz '2027-01-01 00:00:00+02';
begin
  if now() >= v_cutoff then
    raise exception 'انتهت فترة التسجيل المجاني للأطباء';
  end if;
  new.subscription_expires_at := v_cutoff;
  return new;
end;
$function$;

drop trigger if exists doctors_founding_offer_expiry on public.doctors;
create trigger doctors_founding_offer_expiry
before insert on public.doctors
for each row execute function public.set_founding_offer_doctor_expiry();

-- Public visitors only see active doctors with a current subscription.
drop policy if exists "Public can read active doctors" on public.doctors;
create policy "Public can read active doctors"
on public.doctors
for select
using (is_active = true and subscription_expires_at is not null and subscription_expires_at > now());
