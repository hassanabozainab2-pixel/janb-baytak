-- Add optional video media to business offers. Existing image offers remain compatible.
alter table public.offers
  add column if not exists media_path text,
  add column if not exists media_type text not null default 'image';

do $$
begin
  if not exists (
    select 1 from pg_constraint
    where conname = 'offers_media_type_check'
      and conrelid = 'public.offers'::regclass
  ) then
    alter table public.offers
      add constraint offers_media_type_check
      check (media_type in ('image', 'video'));
  end if;
end $$;

-- Keep the existing owner-folder RLS policies; allow common mobile video formats.
update storage.buckets
set file_size_limit = 52428800,
    allowed_mime_types = array[
      'image/jpeg',
      'image/png',
      'image/webp',
      'video/mp4',
      'video/webm',
      'video/quicktime',
      'video/3gpp'
    ]
where id = 'janb-media';
