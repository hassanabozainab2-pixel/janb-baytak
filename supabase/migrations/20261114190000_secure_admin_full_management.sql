-- Secure administrator policies for Janb Baytak.
-- Admin status is read only from trusted app_metadata in the signed JWT.
create or replace function public.is_app_admin()
returns boolean
language sql
stable
set search_path = ''
as $$
  select coalesce((auth.jwt() -> 'app_metadata' ->> 'role') = 'admin', false);
$$;

revoke all on function public.is_app_admin() from public;
grant execute on function public.is_app_admin() to anon, authenticated;

drop policy if exists businesses_admin_all on public.businesses;
create policy businesses_admin_all on public.businesses
for all to authenticated
using (public.is_app_admin())
with check (public.is_app_admin());

drop policy if exists categories_admin_all on public.categories;
create policy categories_admin_all on public.categories
for all to authenticated
using (public.is_app_admin())
with check (public.is_app_admin());

drop policy if exists products_admin_all on public.products;
create policy products_admin_all on public.products
for all to authenticated
using (public.is_app_admin())
with check (public.is_app_admin());

drop policy if exists services_admin_all on public.services;
create policy services_admin_all on public.services
for all to authenticated
using (public.is_app_admin())
with check (public.is_app_admin());

drop policy if exists offers_admin_all on public.offers;
create policy offers_admin_all on public.offers
for all to authenticated
using (public.is_app_admin())
with check (public.is_app_admin());

drop policy if exists business_images_admin_all on public.business_images;
create policy business_images_admin_all on public.business_images
for all to authenticated
using (public.is_app_admin())
with check (public.is_app_admin());

drop policy if exists profiles_admin_read on public.profiles;
create policy profiles_admin_read on public.profiles
for select to authenticated
using (public.is_app_admin());

drop policy if exists ads_admin_all on public.ads;
create policy ads_admin_all on public.ads
for all to authenticated
using (public.is_app_admin())
with check (public.is_app_admin());

drop policy if exists subscriptions_admin_all on public.subscriptions;
create policy subscriptions_admin_all on public.subscriptions
for all to authenticated
using (public.is_app_admin())
with check (public.is_app_admin());

drop policy if exists janb_media_admin_all on storage.objects;
create policy janb_media_admin_all on storage.objects
for all to authenticated
using (bucket_id = 'janb-media' and public.is_app_admin())
with check (bucket_id = 'janb-media' and public.is_app_admin());
