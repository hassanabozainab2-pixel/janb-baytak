-- Product image library and global search for Janb Baytak.
create table if not exists public.product_image_library (
 id uuid primary key default gen_random_uuid(), name text not null, category_name text not null,
 category_icon text not null default '📦', aliases text not null default '', image_url text, storage_path text,
 status text not null default 'active' check(status in ('pending','active','rejected')),
 is_active boolean not null default true, created_by uuid references auth.users(id) on delete set null,
 created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create unique index if not exists product_image_library_unique_name_category_idx on public.product_image_library(category_name,name);
create index if not exists product_image_library_category_idx on public.product_image_library(category_name);
create index if not exists product_image_library_name_idx on public.product_image_library using gin(to_tsvector('simple',name||' '||aliases));
alter table public.product_image_library enable row level security;
grant select,insert,update on public.product_image_library to anon,authenticated;
drop policy if exists "library public active read" on public.product_image_library;
create policy "library public active read" on public.product_image_library for select to anon,authenticated using ((is_active=true and status='active') or public.is_app_admin());
drop policy if exists "library authenticated suggestions" on public.product_image_library;
create policy "library authenticated suggestions" on public.product_image_library for insert to authenticated with check (created_by=(select auth.uid()) and status='pending');
drop policy if exists "library admin manage" on public.product_image_library;
create policy "library admin manage" on public.product_image_library for all to authenticated using (public.is_app_admin()) with check (public.is_app_admin());
insert into public.product_image_library(name,category_name,category_icon,aliases) values
('بوتاجاز','أجهزة كهربائية','🔌','موقد'),
('تكييف','أجهزة كهربائية','🔌','مكيف'),
('تلفزيون','أجهزة كهربائية','🔌','شاشة'),
('ثلاجة','أجهزة كهربائية','🔌','تلاجة، براد'),
('خلاط كهربائي','أجهزة كهربائية','🔌','خلاط'),
('غسالة','أجهزة كهربائية','🔌','غسالة ملابس'),
('كيتل','أجهزة كهربائية','🔌','غلاية كهربائية'),
('مروحة','أجهزة كهربائية','🔌',''),
('مكواة','أجهزة كهربائية','🔌',''),
('ميكروويف','أجهزة كهربائية','🔌',''),
('بوت','أحذية','👟',''),
('حذاء أطفال','أحذية','👟','جزمة أطفال'),
('حذاء حريمي','أحذية','👟','جزمة حريمي'),
('حذاء رجالي','أحذية','👟','جزمة رجالي'),
('حذاء طبي','أحذية','👟',''),
('حذاء كلاسيك','أحذية','👟',''),
('شبشب','أحذية','👟',''),
('شبشب منزلي','أحذية','👟',''),
('صندل','أحذية','👟',''),
('كوتشي','أحذية','👟','حذاء رياضي، سنيكرز'),
('جردل','أدوات منزلية','🏠','سطل'),
('حلة','أدوات منزلية','🏠','حلل، قدر'),
('سكينة مطبخ','أدوات منزلية','🏠','سكاكين'),
('صينية','أدوات منزلية','🏠','صواني'),
('طاسة','أدوات منزلية','🏠','مقلاة'),
('طبق','أدوات منزلية','🏠','أطباق تقديم'),
('طقم أطباق','أدوات منزلية','🏠','أطباق'),
('طقم حلل','أدوات منزلية','🏠',''),
('كوبايات','أدوات منزلية','🏠','أكواب'),
('ملاعق','أدوات منزلية','🏠','معالق'),
('بلطي','أسماك','🐟','سمك بلطي'),
('بوري','أسماك','🐟','سمك بوري'),
('جمبري','أسماك','🐟','روبيان'),
('دنيس','أسماك','🐟',''),
('سردين','أسماك','🐟',''),
('فيليه','أسماك','🐟','سمك فيليه'),
('قاروص','أسماك','🐟','سي باس'),
('كابوريا','أسماك','🐟','سلطعون'),
('كاليماري','أسماك','🐟','سبيط'),
('ماكريل','أسماك','🐟',''),
('بطانية','بطاطين ومفروشات','🛏️','بطاطين'),
('خدادية','بطاطين ومفروشات','🛏️','خداديات'),
('ستارة','بطاطين ومفروشات','🛏️','ستائر'),
('سجادة','بطاطين ومفروشات','🛏️','سجاد'),
('كوفرتة','بطاطين ومفروشات','🛏️','كوفرته'),
('لحاف','بطاطين ومفروشات','🛏️',''),
('مخدة','بطاطين ومفروشات','🛏️','وسادة'),
('مرتبة','بطاطين ومفروشات','🛏️','مراتب'),
('مفرش سرير','بطاطين ومفروشات','🛏️',''),
('ملاية سرير','بطاطين ومفروشات','🛏️','ملايات'),
('أرز','بقالة ومواد غذائية','🛒','رز، أرز أبيض، أرز مصري'),
('بيض','بقالة ومواد غذائية','🛒','بيض أبيض، بيض أحمر'),
('جبنة','بقالة ومواد غذائية','🛒','جبن، جبنة بيضاء'),
('دقيق','بقالة ومواد غذائية','🛒','طحين'),
('زبادي','بقالة ومواد غذائية','🛒','روب'),
('زيت','بقالة ومواد غذائية','🛒','زيت طعام، زيت عباد الشمس'),
('سكر','بقالة ومواد غذائية','🛒','سكر أبيض، سكر ناعم'),
('سمن','بقالة ومواد غذائية','🛒','سمنة، سمن نباتي'),
('شاي','بقالة ومواد غذائية','🛒','شاى'),
('عدس','بقالة ومواد غذائية','🛒','عدس أصفر، عدس بجبة'),
('فول','بقالة ومواد غذائية','🛒','فول مدمس، فول معبأ'),
('قهوة','بقالة ومواد غذائية','🛒','بن، قهوه'),
('لبن','بقالة ومواد غذائية','🛒','حليب'),
('معلبات','بقالة ومواد غذائية','🛒','تونة، صلصة، فاصوليا معلبة'),
('مكرونة','بقالة ومواد غذائية','🛒','مكرونه، مكرونة قلم، اسباجتي'),
('باذنجان','خضار','🥬','بتنجان'),
('بامية','خضار','🥬',''),
('بصل','خضار','🥬','بصل أحمر، بصل أبيض'),
('بطاطس','خضار','🥬','بطاطا'),
('ثوم','خضار','🥬','توم'),
('جزر','خضار','🥬',''),
('خس','خضار','🥬',''),
('خيار','خضار','🥬','قثاء'),
('سبانخ','خضار','🥬',''),
('طماطم','خضار','🥬','بندورة'),
('فاصوليا خضراء','خضار','🥬','فاصوليا'),
('فلفل','خضار','🥬','فلفل أخضر، فلفل ألوان'),
('كوسة','خضار','🥬','كوسا'),
('ليمون','خضار','🥬',''),
('ملوخية','خضار','🥬','ملوخيه'),
('أناناس','فاكهة','🍎',''),
('برتقال','فاكهة','🍎','يوسفي'),
('بطيخ','فاكهة','🍎',''),
('بلح','فاكهة','🍎','تمر'),
('تفاح','فاكهة','🍎',''),
('تين','فاكهة','🍎',''),
('جوافة','فاكهة','🍎',''),
('خوخ','فاكهة','🍎',''),
('رمان','فاكهة','🍎',''),
('شمام','فاكهة','🍎','كانتالوب'),
('عنب','فاكهة','🍎',''),
('فراولة','فاكهة','🍎',''),
('كمثرى','فاكهة','🍎',''),
('مانجو','فاكهة','🍎',''),
('موز','فاكهة','🍎',''),
('برجر','لحوم','🥩','برجر لحمة'),
('ريش','لحوم','🥩','ريش ضاني'),
('سجق','لحوم','🥩','سوسيس بلدي'),
('كبدة','لحوم','🥩','كبدة بلدي'),
('كفتة','لحوم','🥩','كفتة مشوية'),
('لحم بقري','لحوم','🥩','لحمة بقري، لحوم بلدي'),
('لحم ضاني','لحوم','🥩','لحمة ضاني، خروف'),
('لحم مفروم','لحوم','🥩','كفتة مفرومة'),
('لحم مكعبات','لحوم','🥩','مكعبات لحمة'),
('موزة','لحوم','🥩','موزة لحمة'),
('أحمر شفاه','مستحضرات تجميل','💄','روج'),
('بلسم','مستحضرات تجميل','💄',''),
('بودرة','مستحضرات تجميل','💄','بودرة وجه'),
('شامبو','مستحضرات تجميل','💄',''),
('عطر','مستحضرات تجميل','💄','برفان'),
('غسول وجه','مستحضرات تجميل','💄',''),
('كحل','مستحضرات تجميل','💄',''),
('كريم مرطب','مستحضرات تجميل','💄','كريم بشرة'),
('ماسكارا','مستحضرات تجميل','💄',''),
('واقي شمس','مستحضرات تجميل','💄','صن بلوك'),
('بلوزة حريمي','ملابس','👕','بلوزه'),
('بنطلون جينز','ملابس','👕','جينز'),
('بيجامة','ملابس','👕','بيجامات'),
('ترنج','ملابس','👕','ترينج'),
('تيشيرت رجالي','ملابس','👕','تي شيرت'),
('جاكيت','ملابس','👕',''),
('عباية','ملابس','👕','عبايات'),
('فستان','ملابس','👕','فساتين'),
('قميص رجالي','ملابس','👕',''),
('ملابس أطفال','ملابس','👕','لبس أطفال'),
('سائل أطباق','منظفات','🧴','صابون مواعين'),
('صابون','منظفات','🧴','صابونة'),
('كلور','منظفات','🧴','مبيض'),
('مسحوق غسيل','منظفات','🧴','مسحوق'),
('مطهر أرضيات','منظفات','🧴','ديتول أرضيات'),
('معطر جو','منظفات','🧴',''),
('مناديل','منظفات','🧴','مناديل ورقية'),
('منظف حمام','منظفات','🧴',''),
('منظف زجاج','منظفات','🧴',''),
('منعم ملابس','منظفات','🧴','داوني'),
('أسلاك كهرباء','مواد بناء وتشطيبات','🧱','سلك كهرباء'),
('أسمنت','مواد بناء وتشطيبات','🧱','سمنت'),
('بورسلين','مواد بناء وتشطيبات','🧱',''),
('جبس','مواد بناء وتشطيبات','🧱','جبس بورد'),
('دهان','مواد بناء وتشطيبات','🧱','بويات'),
('رخام','مواد بناء وتشطيبات','🧱','رخام سلالم'),
('رمل','مواد بناء وتشطيبات','🧱',''),
('زلط','مواد بناء وتشطيبات','🧱','سن'),
('سيراميك','مواد بناء وتشطيبات','🧱','بلاط'),
('طوب','مواد بناء وتشطيبات','🧱','طوب أحمر'),
('معجون','مواد بناء وتشطيبات','🧱',''),
('مواسير','مواد بناء وتشطيبات','🧱','مواسير مياه')
on conflict (category_name,name) do nothing;
create or replace function public.janb_normalize_search(p_text text) returns text language sql immutable parallel safe as $$
 select regexp_replace(translate(lower(coalesce(p_text,'')), 'أإآٱىةؤئ', 'اااايهوي'), '[ًٌٍَُِّْـ]', '', 'g');
$$;
create or replace function public.janb_global_search(search_text text,result_limit integer default 100)
returns table(result_type text,id uuid,title text,description text,business_id uuid,business_name text,category_name text,image_url text,price numeric,discount_percent numeric,relevance integer)
language sql stable set search_path=public as $$
with q as (select public.janb_normalize_search(trim(search_text)) term),
terms as (select regexp_split_to_table(term,'\\s+') term from q where term<>''),
all_rows as (
 select 'business'::text typ,b.id,b.name title,b.description,b.id business_id,b.name business_name,c.name category_name,b.logo_url image_url,null::numeric price,null::numeric discount_percent,concat_ws(' ',b.name,b.description,b.address,b.city,b.phone,c.name) haystack
 from businesses b left join categories c on c.id=b.category_id where b.status='approved' and b.is_active=true
 union all
 select 'product',p.id,p.name,p.description,p.business_id,b.name,c.name,p.image_url,p.price,null::numeric,concat_ws(' ',p.name,p.description,b.name,b.address,b.city,c.name)
 from products p join businesses b on b.id=p.business_id left join categories c on c.id=b.category_id where p.is_active=true and b.status='approved' and b.is_active=true
 union all
 select 'service',s.id,s.name,s.description,s.business_id,b.name,c.name,null::text,s.price,null::numeric,concat_ws(' ',s.name,s.description,s.price_note,b.name,b.address,b.city,c.name)
 from services s join businesses b on b.id=s.business_id left join categories c on c.id=b.category_id where s.is_active=true and b.status='approved' and b.is_active=true
 union all
 select 'offer',o.id,o.title,o.description,o.business_id,b.name,c.name,null::text,null::numeric,o.discount_percent,concat_ws(' ',o.title,o.description,b.name,c.name)
 from offers o join businesses b on b.id=o.business_id left join categories c on c.id=b.category_id where o.is_active=true and b.status='approved' and b.is_active=true
),
matched as (
 select a.*,case when public.janb_normalize_search(a.title)=q.term then 100 when public.janb_normalize_search(a.title) like q.term||'%' then 80 when public.janb_normalize_search(a.haystack) like '%'||q.term||'%' then 60 else 30 end score
 from all_rows a cross join q where q.term<>'' and (public.janb_normalize_search(a.haystack) like '%'||q.term||'%' or exists(select 1 from terms t where length(t.term)>1 and public.janb_normalize_search(a.haystack) like '%'||t.term||'%'))
)
select typ,id,title,description,business_id,business_name,category_name,image_url,price,discount_percent,score from matched order by score desc,title limit greatest(1,least(coalesce(result_limit,100),200));
$$;
grant execute on function public.janb_global_search(text,integer) to anon,authenticated;
