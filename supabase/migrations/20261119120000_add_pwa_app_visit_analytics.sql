
-- Anonymous PWA visit analytics. No names, phone numbers, IP addresses, or device fingerprints are stored.
create table if not exists public.app_visit_events (
  id uuid primary key default gen_random_uuid(),
  visitor_id uuid not null,
  session_id uuid not null unique,
  created_at timestamptz not null default now()
);
create index if not exists app_visit_events_created_at_idx on public.app_visit_events (created_at desc);
create index if not exists app_visit_events_visitor_created_idx on public.app_visit_events (visitor_id, created_at desc);
alter table public.app_visit_events enable row level security;
drop policy if exists app_visit_events_public_insert on public.app_visit_events;
create policy app_visit_events_public_insert on public.app_visit_events
  for insert to anon, authenticated
  with check (visitor_id is not null and session_id is not null);
drop policy if exists app_visit_events_admin_read on public.app_visit_events;
create policy app_visit_events_admin_read on public.app_visit_events
  for select to authenticated using (public.is_app_admin());
revoke all on public.app_visit_events from public;
grant insert on public.app_visit_events to anon, authenticated;
grant select on public.app_visit_events to authenticated;
grant usage on schema public to anon, authenticated;

create or replace function public.admin_app_visit_analytics(p_days integer default 30)
returns jsonb
language plpgsql stable security invoker set search_path = ''
as $function$
declare result jsonb;
begin
  if not coalesce(public.is_app_admin(), false) then
    raise insufficient_privilege using message = 'Admin access required';
  end if;
  if p_days is not null and (p_days < 1 or p_days > 3650) then
    raise invalid_parameter_value using message = 'p_days must be null or between 1 and 3650';
  end if;
  with scoped as (
    select visitor_id, created_at,
      (created_at at time zone 'Africa/Cairo')::date as visit_day
    from public.app_visit_events
    where p_days is null or created_at >= now() - make_interval(days => p_days)
  ), daily as (
    select visit_day, count(*)::bigint as visits,
      count(distinct visitor_id)::bigint as unique_visitors
    from scoped group by visit_day
  )
  select jsonb_build_object(
    'total_visits',(select count(*) from scoped),
    'unique_visitors',(select count(distinct visitor_id) from scoped),
    'today_visits',(select count(*) from scoped where visit_day=(now() at time zone 'Africa/Cairo')::date),
    'today_unique_visitors',(select count(distinct visitor_id) from scoped where visit_day=(now() at time zone 'Africa/Cairo')::date),
    'last_visit',(select max(created_at) from scoped),
    'daily',coalesce((select jsonb_agg(jsonb_build_object('day',visit_day,'visits',visits,'unique_visitors',unique_visitors) order by visit_day desc) from daily),'[]'::jsonb)
  ) into result;
  return result;
end;
$function$;
revoke all on function public.admin_app_visit_analytics(integer) from public;
grant execute on function public.admin_app_visit_analytics(integer) to authenticated;
