alter table public.subscription_payment_requests
  add column if not exists receipt_path text;
alter table public.doctor_submissions
  add column if not exists receipt_path text;
alter table public.subscription_payment_requests
  drop constraint if exists subscription_payment_requests_plan_code_check,
  drop constraint if exists subscription_payment_requests_amount_egp_check;
alter table public.subscription_payment_requests
  add constraint subscription_payment_requests_plan_code_check
    check (plan_code in ('self_managed_yearly','marketing_agent_yearly')),
  add constraint subscription_payment_requests_amount_egp_check
    check ((plan_code = 'self_managed_yearly' and amount_egp = 1500) or
           (plan_code = 'marketing_agent_yearly' and amount_egp = 3000));
alter table public.doctor_submissions
  drop constraint if exists doctor_submissions_plan_code_check,
  drop constraint if exists doctor_submissions_amount_egp_check;
alter table public.doctor_submissions
  add constraint doctor_submissions_plan_code_check
    check (plan_code is null or plan_code = 'self_managed_yearly'),
  add constraint doctor_submissions_amount_egp_check
    check (amount_egp is null or amount_egp = 1500);
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('payment-receipts','payment-receipts',false,5242880,array['image/jpeg','image/png','image/webp'])
on conflict (id) do update set public=false,file_size_limit=5242880,allowed_mime_types=array['image/jpeg','image/png','image/webp'];
drop policy if exists janb_payment_receipts_owner_insert on storage.objects;
create policy janb_payment_receipts_owner_insert on storage.objects for insert to authenticated
with check (bucket_id='payment-receipts' and (storage.foldername(name))[1]=(select auth.uid())::text);
drop policy if exists janb_payment_receipts_owner_or_admin_read on storage.objects;
create policy janb_payment_receipts_owner_or_admin_read on storage.objects for select to authenticated
using (bucket_id='payment-receipts' and ((storage.foldername(name))[1]=(select auth.uid())::text or public.is_app_admin()));
drop policy if exists janb_payment_receipts_owner_or_admin_delete on storage.objects;
create policy janb_payment_receipts_owner_or_admin_delete on storage.objects for delete to authenticated
using (bucket_id='payment-receipts' and ((storage.foldername(name))[1]=(select auth.uid())::text or public.is_app_admin()));
