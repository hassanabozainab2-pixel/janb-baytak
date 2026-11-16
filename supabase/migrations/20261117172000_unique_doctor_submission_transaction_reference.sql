create unique index if not exists doctor_submissions_transaction_reference_uidx
on public.doctor_submissions (transaction_reference)
where transaction_reference is not null;
