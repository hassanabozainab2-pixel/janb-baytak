create table if not exists public.activity_analytics_events (
  id uuid primary key default gen_random_uuid(),
  business_id uuid references public.businesses(id) on delete cascade,
  doctor_id uuid references public.doctors(id) on delete cascade,
  event_type text not null check (event_type in ('view','call_click','whatsapp_click')),
  session_id uuid not null,
  created_at timestamptz not null default now(),
  constraint activity_analytics_one_activity check (
    (business_id is not null and doctor_id is null)
    or (business_id is null and doctor_id is not null)
  )
);
create index if not exists activity_analytics_business_period_idx on public.activity_analytics_events (business_id,event_type,created_at desc) where business_id is not null;
create index if not exists activity_analytics_doctor_period_idx on public.activity_analytics_events (doctor_id,event_type,created_at desc) where doctor_id is not null;
create unique index if not exists activity_analytics_business_view_session_uidx on public.activity_analytics_events (business_id,session_id) where business_id is not null and event_type='view';
create unique index if not exists activity_analytics_doctor_view_session_uidx on public.activity_analytics_events (doctor_id,session_id) where doctor_id is not null and event_type='view';
alter table public.activity_analytics_events enable row level security;
drop policy if exists activity_analytics_public_insert on public.activity_analytics_events;
create policy activity_analytics_public_insert on public.activity_analytics_events for insert to anon,authenticated
with check (
 (business_id is not null and exists(select 1 from public.businesses b where b.id=activity_analytics_events.business_id and b.status='approved' and b.is_active=true))
 or
 (doctor_id is not null and exists(select 1 from public.doctors d where d.id=activity_analytics_events.doctor_id and d.is_active=true))
);
drop policy if exists activity_analytics_owner_read on public.activity_analytics_events;
create policy activity_analytics_owner_read on public.activity_analytics_events for select to authenticated
using (
 (business_id is not null and exists(select 1 from public.businesses b where b.id=activity_analytics_events.business_id and b.owner_id=(select auth.uid())))
 or
 (doctor_id is not null and exists(select 1 from public.doctor_submissions ds where ds.doctor_id=activity_analytics_events.doctor_id and ds.user_id=(select auth.uid()) and ds.status='approved'))
 or coalesce((auth.jwt()->'app_metadata'->>'role')='admin',false)
);
grant usage on schema public to anon,authenticated;
grant insert on public.activity_analytics_events to anon,authenticated;
grant select on public.activity_analytics_events to authenticated;
