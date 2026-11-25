-- FoxDrop seed (supabase db reset). Provinces are reference data; everything after the demo marker is demo-only.

-- Generated from the PSGC province list (psgc.cloud mirror of PSA data, 2026-11-25). 82 provinces + NCR.
insert into public.provinces (psgc_code, name, region_code, region_name) values
  ('0102800000', 'Ilocos Norte', '0100000000', 'Region I (Ilocos Region)'),
  ('0102900000', 'Ilocos Sur', '0100000000', 'Region I (Ilocos Region)'),
  ('0103300000', 'La Union', '0100000000', 'Region I (Ilocos Region)'),
  ('0105500000', 'Pangasinan', '0100000000', 'Region I (Ilocos Region)'),
  ('0200900000', 'Batanes', '0200000000', 'Region II (Cagayan Valley)'),
  ('0201500000', 'Cagayan', '0200000000', 'Region II (Cagayan Valley)'),
  ('0203100000', 'Isabela', '0200000000', 'Region II (Cagayan Valley)'),
  ('0205000000', 'Nueva Vizcaya', '0200000000', 'Region II (Cagayan Valley)'),
  ('0205700000', 'Quirino', '0200000000', 'Region II (Cagayan Valley)'),
  ('0300800000', 'Bataan', '0300000000', 'Region III (Central Luzon)'),
  ('0301400000', 'Bulacan', '0300000000', 'Region III (Central Luzon)'),
  ('0304900000', 'Nueva Ecija', '0300000000', 'Region III (Central Luzon)'),
  ('0305400000', 'Pampanga', '0300000000', 'Region III (Central Luzon)'),
  ('0306900000', 'Tarlac', '0300000000', 'Region III (Central Luzon)'),
  ('0307100000', 'Zambales', '0300000000', 'Region III (Central Luzon)'),
  ('0307700000', 'Aurora', '0300000000', 'Region III (Central Luzon)'),
  ('0401000000', 'Batangas', '0400000000', 'Region IV-A (CALABARZON)'),
  ('0402100000', 'Cavite', '0400000000', 'Region IV-A (CALABARZON)'),
  ('0403400000', 'Laguna', '0400000000', 'Region IV-A (CALABARZON)'),
  ('0405600000', 'Quezon', '0400000000', 'Region IV-A (CALABARZON)'),
  ('0405800000', 'Rizal', '0400000000', 'Region IV-A (CALABARZON)'),
  ('1704000000', 'Marinduque', '1700000000', 'MIMAROPA Region'),
  ('1705100000', 'Occidental Mindoro', '1700000000', 'MIMAROPA Region'),
  ('1705200000', 'Oriental Mindoro', '1700000000', 'MIMAROPA Region'),
  ('1705300000', 'Palawan', '1700000000', 'MIMAROPA Region'),
  ('1705900000', 'Romblon', '1700000000', 'MIMAROPA Region'),
  ('0500500000', 'Albay', '0500000000', 'Region V (Bicol Region)'),
  ('0501600000', 'Camarines Norte', '0500000000', 'Region V (Bicol Region)'),
  ('0501700000', 'Camarines Sur', '0500000000', 'Region V (Bicol Region)'),
  ('0502000000', 'Catanduanes', '0500000000', 'Region V (Bicol Region)'),
  ('0504100000', 'Masbate', '0500000000', 'Region V (Bicol Region)'),
  ('0506200000', 'Sorsogon', '0500000000', 'Region V (Bicol Region)'),
  ('0600400000', 'Aklan', '0600000000', 'Region VI (Western Visayas)'),
  ('0600600000', 'Antique', '0600000000', 'Region VI (Western Visayas)'),
  ('0601900000', 'Capiz', '0600000000', 'Region VI (Western Visayas)'),
  ('0603000000', 'Iloilo', '0600000000', 'Region VI (Western Visayas)'),
  ('0604500000', 'Negros Occidental', '0600000000', 'Region VI (Western Visayas)'),
  ('0607900000', 'Guimaras', '0600000000', 'Region VI (Western Visayas)'),
  ('0701200000', 'Bohol', '0700000000', 'Region VII (Central Visayas)'),
  ('0702200000', 'Cebu', '0700000000', 'Region VII (Central Visayas)'),
  ('0704600000', 'Negros Oriental', '0700000000', 'Region VII (Central Visayas)'),
  ('0706100000', 'Siquijor', '0700000000', 'Region VII (Central Visayas)'),
  ('0802600000', 'Eastern Samar', '0800000000', 'Region VIII (Eastern Visayas)'),
  ('0803700000', 'Leyte', '0800000000', 'Region VIII (Eastern Visayas)'),
  ('0804800000', 'Northern Samar', '0800000000', 'Region VIII (Eastern Visayas)'),
  ('0806000000', 'Samar', '0800000000', 'Region VIII (Eastern Visayas)'),
  ('0806400000', 'Southern Leyte', '0800000000', 'Region VIII (Eastern Visayas)'),
  ('0807800000', 'Biliran', '0800000000', 'Region VIII (Eastern Visayas)'),
  ('0907200000', 'Zamboanga del Norte', '0900000000', 'Region IX (Zamboanga Peninsula)'),
  ('0907300000', 'Zamboanga del Sur', '0900000000', 'Region IX (Zamboanga Peninsula)'),
  ('0908300000', 'Zamboanga Sibugay', '0900000000', 'Region IX (Zamboanga Peninsula)'),
  ('1001300000', 'Bukidnon', '1000000000', 'Region X (Northern Mindanao)'),
  ('1001800000', 'Camiguin', '1000000000', 'Region X (Northern Mindanao)'),
  ('1003500000', 'Lanao del Norte', '1000000000', 'Region X (Northern Mindanao)'),
  ('1004200000', 'Misamis Occidental', '1000000000', 'Region X (Northern Mindanao)'),
  ('1004300000', 'Misamis Oriental', '1000000000', 'Region X (Northern Mindanao)'),
  ('1102300000', 'Davao del Norte', '1100000000', 'Region XI (Davao Region)'),
  ('1102400000', 'Davao del Sur', '1100000000', 'Region XI (Davao Region)'),
  ('1102500000', 'Davao Oriental', '1100000000', 'Region XI (Davao Region)'),
  ('1108200000', 'Davao de Oro', '1100000000', 'Region XI (Davao Region)'),
  ('1108600000', 'Davao Occidental', '1100000000', 'Region XI (Davao Region)'),
  ('1204700000', 'Cotabato', '1200000000', 'Region XII (SOCCSKSARGEN)'),
  ('1206300000', 'South Cotabato', '1200000000', 'Region XII (SOCCSKSARGEN)'),
  ('1206500000', 'Sultan Kudarat', '1200000000', 'Region XII (SOCCSKSARGEN)'),
  ('1208000000', 'Sarangani', '1200000000', 'Region XII (SOCCSKSARGEN)'),
  ('1400100000', 'Abra', '1400000000', 'Cordillera Administrative Region (CAR)'),
  ('1401100000', 'Benguet', '1400000000', 'Cordillera Administrative Region (CAR)'),
  ('1402700000', 'Ifugao', '1400000000', 'Cordillera Administrative Region (CAR)'),
  ('1403200000', 'Kalinga', '1400000000', 'Cordillera Administrative Region (CAR)'),
  ('1404400000', 'Mountain Province', '1400000000', 'Cordillera Administrative Region (CAR)'),
  ('1408100000', 'Apayao', '1400000000', 'Cordillera Administrative Region (CAR)'),
  ('1600200000', 'Agusan del Norte', '1600000000', 'Region XIII (Caraga)'),
  ('1600300000', 'Agusan del Sur', '1600000000', 'Region XIII (Caraga)'),
  ('1606700000', 'Surigao del Norte', '1600000000', 'Region XIII (Caraga)'),
  ('1606800000', 'Surigao del Sur', '1600000000', 'Region XIII (Caraga)'),
  ('1608500000', 'Dinagat Islands', '1600000000', 'Region XIII (Caraga)'),
  ('1900700000', 'Basilan', '1900000000', 'Bangsamoro Autonomous Region In Muslim Mindanao (BARMM)'),
  ('1903600000', 'Lanao del Sur', '1900000000', 'Bangsamoro Autonomous Region In Muslim Mindanao (BARMM)'),
  ('1906600000', 'Sulu', '1900000000', 'Bangsamoro Autonomous Region In Muslim Mindanao (BARMM)'),
  ('1907000000', 'Tawi-Tawi', '1900000000', 'Bangsamoro Autonomous Region In Muslim Mindanao (BARMM)'),
  ('1908700000', 'Maguindanao del Norte', '1900000000', 'Bangsamoro Autonomous Region In Muslim Mindanao (BARMM)'),
  ('1908800000', 'Maguindanao del Sur', '1900000000', 'Bangsamoro Autonomous Region In Muslim Mindanao (BARMM)'),
  ('1300000000', 'Metro Manila (NCR)', '1300000000', 'National Capital Region (NCR)')
on conflict (psgc_code) do nothing;

-- ======================================================================== demo data (LOCAL DEVELOPMENT ONLY)
-- `supabase db reset` loads this. Never run it against the hosted project.
-- Demo sign-in: phone OTP with code 123456 (config.jsonc auth.sms.test_otp), numbers below.
--   +63 917 000 0001  Juan Dela Cruz      customer
--   +63 917 000 0002  Maria Santos        customer
--   +63 917 000 0003  Paolo Reyes         rider
--   +63 917 000 0004  Carlo Mendoza       rider
--   +63 917 000 0005  Ana Villanueva      rider
--   +63 917 000 0006  Liza Gonzaga        operator, Negros Occidental
--   +63 917 000 0007  Mama's Kitchen      store owner
--   +63 917 000 0008  QuickMart           store owner
--   +63 917 000 0009  Bacolod Botika      store owner
--   +63 917 000 0010  FoxDrop Admin       super admin (web dashboard only; admin@foxdrop.local)

-- Local-only key for PII encryption (§17). Production: set a strong secret in the dashboard's Vault.
select vault.create_secret('local-dev-only-not-a-real-key-0001', 'foxdrop_pii_key', 'FoxDrop PII encryption key (local)')
where not exists (select 1 from vault.secrets where name = 'foxdrop_pii_key');

update public.provinces
   set short_code = 'NEG',
       center = extensions.st_setsrid(extensions.st_makepoint(122.9509, 10.6765), 4326)::extensions.geography
 where psgc_code = '0604500000';

-- ------------------------------------------------------------------------ users
-- auth.users + phone identities. Token columns must be '' (not null) for Supabase Auth to sign these users in.
with demo (n, full_name) as (
  values
    (1, 'Juan Dela Cruz'), (2, 'Maria Santos'), (3, 'Paolo Reyes'), (4, 'Carlo Mendoza'), (5, 'Ana Villanueva'),
    (6, 'Liza Gonzaga'), (7, 'Rosa Lacson'), (8, 'Ramon Yulo'), (9, 'Teresa Montelibano'), (10, 'FoxDrop Admin')
)
insert into auth.users (
  instance_id, id, aud, role, phone, phone_confirmed_at, email, email_confirmed_at, encrypted_password,
  raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
  confirmation_token, recovery_token, email_change_token_new, email_change, phone_change, phone_change_token,
  email_change_token_current, reauthentication_token
)
select
  '00000000-0000-0000-0000-000000000000',
  ('00000000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
  'authenticated',
  'authenticated',
  '6391700000' || lpad(n::text, 2, '0'),
  now(),
  case when n = 10 then 'admin@foxdrop.local' end,
  case when n = 10 then now() end,
  case when n = 10 then extensions.crypt('foxdrop-local-admin', extensions.gen_salt('bf')) else '' end,
  jsonb_build_object('provider', 'phone', 'providers', jsonb_build_array('phone')),
  jsonb_build_object('full_name', full_name),
  now(), now(), '', '', '', '', '', '', '', ''
from demo
on conflict (id) do nothing;

insert into auth.identities (id, user_id, provider_id, provider, identity_data, last_sign_in_at, created_at, updated_at)
select
  gen_random_uuid(), u.id, u.phone, 'phone',
  jsonb_build_object('sub', u.id::text, 'phone', u.phone, 'phone_verified', true),
  now(), now(), now()
from auth.users u
where u.id::text like '00000000-0000-4000-8000-%'
on conflict do nothing;

-- ------------------------------------------------------------------------ Negros Occidental: license + operator
do $$
declare
  neg uuid := (select id from public.provinces where psgc_code = '0604500000');
  operator_user uuid := '00000000-0000-4000-8000-000000000006';
  admin_user uuid := '00000000-0000-4000-8000-000000000010';
begin
  insert into public.user_roles (user_id, role) values (admin_user, 'super_admin') on conflict do nothing;
  insert into public.user_roles (user_id, role, province_id) values (operator_user, 'operator', neg) on conflict do nothing;

  insert into public.operator_applications (
    id, applicant_user_id, province_id, business_name, registration_number, contact_phone, status, reviewed_by, reviewed_at
  ) values (
    '00000000-0000-4000-9000-000000000001', operator_user, neg, 'Gonzaga Logistics', 'DTI-0604-000123',
    '+639170000006', 'APPROVED', admin_user, now() - interval '31 days'
  ) on conflict (id) do nothing;

  insert into public.province_licenses (
    id, province_id, operator_user_id, application_id, status, start_at, end_at, fee_centavos
  ) values (
    '00000000-0000-4000-9000-000000000002', neg, operator_user, '00000000-0000-4000-9000-000000000001', 'ACTIVE',
    now() - interval '30 days', now() + interval '335 days', 1500000
  ) on conflict (id) do nothing;

  insert into public.delivery_rate_cards (province_id, created_by, effective_from)
  values (neg, operator_user, now() - interval '30 days');

  -- Coverage: a box around Bacolod City, Talisay and Silay.
  insert into public.delivery_zones (province_id, name, area, created_by)
  values (
    neg, 'Metro Bacolod',
    extensions.st_geomfromtext(
      'MULTIPOLYGON(((122.88 10.58, 123.08 10.58, 123.08 10.82, 122.88 10.82, 122.88 10.58)))', 4326
    )::extensions.geography,
    operator_user
  );

  -- ---------------------------------------------------------------------- stores + products
  insert into public.stores (
    id, province_id, owner_user_id, name, category, description, location, address_line, barangay, city, phone,
    opening_hours, prep_time_min, status, rating_avg, rating_count
  ) values
    ('00000000-0000-4000-a000-000000000001', neg, '00000000-0000-4000-8000-000000000007', 'Mama''s Kitchen', 'FOOD',
     'Home-style burgers, fries and inasal.',
     extensions.st_setsrid(extensions.st_makepoint(122.9530, 10.6740), 4326)::extensions.geography,
     'Lacson St.', 'Barangay 17', 'Bacolod City', '+639170000007',
     '{"mon":[["08:00","21:00"]],"tue":[["08:00","21:00"]],"wed":[["08:00","21:00"]],"thu":[["08:00","21:00"]],"fri":[["08:00","22:00"]],"sat":[["08:00","22:00"]],"sun":[["09:00","20:00"]]}',
     15, 'ACTIVE', 4.8, 124),
    ('00000000-0000-4000-a000-000000000002', neg, '00000000-0000-4000-8000-000000000008', 'QuickMart', 'GROCERIES',
     'Groceries and household essentials.',
     extensions.st_setsrid(extensions.st_makepoint(122.9600, 10.6800), 4326)::extensions.geography,
     'Araneta Ave.', 'Singcang', 'Bacolod City', '+639170000008',
     '{"mon":[["07:00","22:00"]],"tue":[["07:00","22:00"]],"wed":[["07:00","22:00"]],"thu":[["07:00","22:00"]],"fri":[["07:00","22:00"]],"sat":[["07:00","22:00"]],"sun":[["07:00","22:00"]]}',
     20, 'ACTIVE', 4.7, 88),
    ('00000000-0000-4000-a000-000000000003', neg, '00000000-0000-4000-8000-000000000009', 'Bacolod Botika', 'MEDICINE',
     'Pharmacy: over-the-counter medicine and health essentials.',
     extensions.st_setsrid(extensions.st_makepoint(122.9450, 10.6690), 4326)::extensions.geography,
     'Burgos St.', 'Villamonte', 'Bacolod City', '+639170000009',
     '{"mon":[["08:00","20:00"]],"tue":[["08:00","20:00"]],"wed":[["08:00","20:00"]],"thu":[["08:00","20:00"]],"fri":[["08:00","20:00"]],"sat":[["08:00","20:00"]]}',
     10, 'ACTIVE', 4.9, 41)
  on conflict (id) do nothing;

  insert into public.user_roles (user_id, role, province_id, store_id) values
    ('00000000-0000-4000-8000-000000000007', 'store_owner', neg, '00000000-0000-4000-a000-000000000001'),
    ('00000000-0000-4000-8000-000000000008', 'store_owner', neg, '00000000-0000-4000-a000-000000000002'),
    ('00000000-0000-4000-8000-000000000009', 'store_owner', neg, '00000000-0000-4000-a000-000000000003')
  on conflict do nothing;

  insert into public.product_categories (id, store_id, province_id, name, sort_order) values
    ('00000000-0000-4000-b000-000000000001', '00000000-0000-4000-a000-000000000001', neg, 'Burgers & Mains', 1),
    ('00000000-0000-4000-b000-000000000002', '00000000-0000-4000-a000-000000000001', neg, 'Sides & Drinks', 2),
    ('00000000-0000-4000-b000-000000000003', '00000000-0000-4000-a000-000000000002', neg, 'Pantry', 1),
    ('00000000-0000-4000-b000-000000000004', '00000000-0000-4000-a000-000000000003', neg, 'Medicine', 1)
  on conflict (id) do nothing;

  insert into public.products (store_id, province_id, category_id, name, description, price_centavos, sort_order, options)
  values
    ('00000000-0000-4000-a000-000000000001', neg, '00000000-0000-4000-b000-000000000001', 'Cheeseburger',
     'Quarter-pound beef patty with cheese.', 12900, 1,
     '[{"group":"Add-ons","required":false,"max":2,"choices":[{"name":"Extra cheese","price_centavos":2000},{"name":"Bacon","price_centavos":3500}]}]'),
    ('00000000-0000-4000-a000-000000000001', neg, '00000000-0000-4000-b000-000000000001', 'Chicken Inasal',
     'Grilled Bacolod-style chicken with garlic rice.', 15900, 2, '[]'),
    ('00000000-0000-4000-a000-000000000001', neg, '00000000-0000-4000-b000-000000000002', 'Fries (Regular)',
     null, 4900, 3, '[]'),
    ('00000000-0000-4000-a000-000000000001', neg, '00000000-0000-4000-b000-000000000002', 'Iced Tea',
     null, 3900, 4, '[]'),
    ('00000000-0000-4000-a000-000000000002', neg, '00000000-0000-4000-b000-000000000003', 'Rice 5 kg',
     'Well-milled rice.', 27500, 1, '[]'),
    ('00000000-0000-4000-a000-000000000002', neg, '00000000-0000-4000-b000-000000000003', 'Eggs (1 dozen)',
     null, 9600, 2, '[]'),
    ('00000000-0000-4000-a000-000000000002', neg, '00000000-0000-4000-b000-000000000003', 'Bottled Water 1.5 L',
     null, 3500, 3, '[]'),
    ('00000000-0000-4000-a000-000000000003', neg, '00000000-0000-4000-b000-000000000004', 'Paracetamol 500 mg (10 tablets)',
     null, 4500, 1, '[]'),
    ('00000000-0000-4000-a000-000000000003', neg, '00000000-0000-4000-b000-000000000004', 'Vitamin C 500 mg (30 tablets)',
     null, 18000, 2, '[]'),
    ('00000000-0000-4000-a000-000000000003', neg, '00000000-0000-4000-b000-000000000004', 'Face Masks (box of 50)',
     null, 15000, 3, '[]');

  -- ---------------------------------------------------------------------- riders
  insert into public.riders (
    id, user_id, province_id, vehicle_type, plate_no, license_no_last4, license_expiry, status, approved_by, approved_at,
    rating_avg, rating_count
  ) values
    ('00000000-0000-4000-c000-000000000001', '00000000-0000-4000-8000-000000000003', neg, 'MOTORCYCLE', 'NEG 1234',
     'A001', current_date + 400, 'APPROVED', operator_user, now() - interval '20 days', 4.9, 328),
    ('00000000-0000-4000-c000-000000000002', '00000000-0000-4000-8000-000000000004', neg, 'MOTORCYCLE', 'NEG 5678',
     'A002', current_date + 200, 'APPROVED', operator_user, now() - interval '18 days', 4.7, 112),
    ('00000000-0000-4000-c000-000000000003', '00000000-0000-4000-8000-000000000005', neg, 'BICYCLE', null,
     'A003', current_date + 300, 'APPROVED', operator_user, now() - interval '10 days', 4.8, 57)
  on conflict (id) do nothing;

  insert into public.user_roles (user_id, role, province_id) values
    ('00000000-0000-4000-8000-000000000003', 'rider', neg),
    ('00000000-0000-4000-8000-000000000004', 'rider', neg),
    ('00000000-0000-4000-8000-000000000005', 'rider', neg)
  on conflict do nothing;

  -- ---------------------------------------------------------------------- customer address
  insert into public.addresses (user_id, province_id, label, location, address_line, landmark, barangay, city)
  values
    ('00000000-0000-4000-8000-000000000001', neg, 'Home',
     extensions.st_setsrid(extensions.st_makepoint(122.9700, 10.6900), 4326)::extensions.geography,
     '12 Rosario St.', 'Near the chapel', 'Taculing', 'Bacolod City'),
    ('00000000-0000-4000-8000-000000000002', neg, 'Home',
     extensions.st_setsrid(extensions.st_makepoint(122.9400, 10.6600), 4326)::extensions.geography,
     '45 Mabini St.', null, 'Villamonte', 'Bacolod City');

  -- ---------------------------------------------------------------------- wallets (§7)
  insert into public.wallets (owner_type, owner_id, province_id, kind) values
    ('PLATFORM', null, null, 'PLATFORM_REVENUE'),
    ('SYSTEM', null, null, 'GATEWAY_CLEARING'),
    ('SYSTEM', null, null, 'CUSTOMER_REFUND'),
    ('OPERATOR', operator_user, neg, 'OPERATOR_REVENUE'),
    ('OPERATOR', operator_user, neg, 'OPERATOR_CASH')
  on conflict do nothing;
  insert into public.wallets (owner_type, owner_id, province_id, kind)
  select 'STORE', s.id, s.province_id, k.kind
  from public.stores s cross join (values ('STORE_TOPUP'::public.wallet_kind), ('STORE_PAYABLE')) as k (kind)
  where s.province_id = neg
  on conflict do nothing;
  insert into public.wallets (owner_type, owner_id, province_id, kind)
  select 'RIDER', r.id, r.province_id, k.kind
  from public.riders r cross join (values ('RIDER_EARNINGS'::public.wallet_kind), ('RIDER_CASH')) as k (kind)
  where r.province_id = neg
  on conflict do nothing;
end;
$$;
