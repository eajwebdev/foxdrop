-- Phase 1 acceptance (§13): one live license per province is enforced by the database (§5.1).
begin;
create extension if not exists pgtap with schema extensions;
select plan(6);

insert into auth.users (id, aud, role, phone)
values ('00000000-0000-4000-8000-0000000000c1', 'authenticated', 'authenticated', '639170000201');

-- Negros Occidental already has an ACTIVE license (seed).
select throws_ok(
  $$ insert into public.province_licenses (province_id, operator_user_id, status, fee_centavos)
     select id, '00000000-0000-4000-8000-0000000000c1', 'APPLIED', 1500000
     from public.provinces where psgc_code = '0604500000' $$,
  '23505', null,
  'a second application for an already-licensed province fails'
);

select throws_ok(
  $$ insert into public.province_licenses (province_id, operator_user_id, status, start_at, end_at, fee_centavos)
     select id, '00000000-0000-4000-8000-0000000000c1', 'ACTIVE', now(), now() + interval '365 days', 1500000
     from public.provinces where psgc_code = '0604500000' $$,
  '23505', null,
  'a second ACTIVE license for the same province fails'
);

select throws_ok(
  $$ insert into public.province_licenses (province_id, operator_user_id, status, fee_centavos)
     select id, '00000000-0000-4000-8000-0000000000c1', 'ACTIVE', 1500000
     from public.provinces where psgc_code = '0600400000' $$,
  '23514', null,
  'an ACTIVE license needs a start and end date'
);

select lives_ok(
  $$ insert into public.province_licenses (province_id, operator_user_id, status, fee_centavos)
     select id, '00000000-0000-4000-8000-0000000000c1', 'APPLIED', 1500000
     from public.provinces where psgc_code = '0601900000' $$,
  'an unlicensed province (Capiz) accepts an application'
);

-- After the current license expires the province can be licensed again.
update public.province_licenses set status = 'EXPIRED'
 where province_id = (select id from public.provinces where psgc_code = '0604500000');
select lives_ok(
  $$ insert into public.province_licenses (province_id, operator_user_id, status, fee_centavos)
     select id, '00000000-0000-4000-8000-0000000000c1', 'APPLIED', 1500000
     from public.provinces where psgc_code = '0604500000' $$,
  'an expired province can be applied for again'
);

select is(
  (select count(*)::int from public.province_directory where is_serviceable),
  0,
  'with its license expired, no province is serviceable to customers'
);

select * from finish();
rollback;
