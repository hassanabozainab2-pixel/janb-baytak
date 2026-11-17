-- Hide approved businesses from public visitors after their subscription expires.
-- Owners retain access to their own business row so they can renew.
drop policy if exists businesses_public_or_owner_read on public.businesses;
create policy businesses_public_or_owner_read
on public.businesses
for select
using (
  (status = 'approved' and is_active = true and subscription_expires_at is not null and subscription_expires_at > now())
  or (select auth.uid()) = owner_id
);
