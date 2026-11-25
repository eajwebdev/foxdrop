-- Phase 1 acceptance (§13): the operator of Province A reads 0 rows of Province B
-- (orders, stores, riders, wallets, licenses, roles). Also checks customer / store / rider / anon visibility.
begin;
create extension if not exists pgtap with schema extensions;
select plan(20);

-- ------------------------------------------------------------------ fixture (as postgres, RLS bypassed)
-- Province A = Negros Occidental (seed). Province B = Iloilo with its own operator, store, rider, order.
update public.provinces set short_code = 'ILO' where psgc_code = '0603000000';

insert into auth.users (id, aud, role, phone)
values
  ('00000000-0000-4000-8000-0000000000b1', 'authenticated', 'authenticated', '639170000101'), -- operator B
  ('00000000-0000-4000-8000-0000000000b2', 'authenticated', 'authenticated', '639170000102'), -- store owner B
  ('00000000-0000-4000-8000-0000000000b3', 'authenticated', 'authenticated', '639170000103'), -- rider B
  ('00000000-0000-4000-8000-0000000000b4', 'authenticated', 'authenticated', '639170000104'); -- customer B

do $$
declare
  ilo uuid := (select id from public.provinces where psgc_code = '0603000000');
  neg uuid := (select id from public.provinces where psgc_code = '0604500000');
begin
  insert into public.province_licenses (province_id, operator_user_id, status, start_at, end_at, fee_centavos)
  values (ilo, '00000000-0000-4000-8000-0000000000b1', 'ACTIVE', now(), now() + interval '365 days', 1500000);
  insert into public.user_roles (user_id, role, province_id)
  values ('00000000-0000-4000-8000-0000000000b1', 'operator', ilo);

  insert into public.stores (id, province_id, owner_user_id, name, category, location, address_line, city, status)
  values
    ('00000000-0000-4000-a000-0000000000b1', ilo, '00000000-0000-4000-8000-0000000000b2', 'Iloilo Eats', 'FOOD',
     extensions.st_setsrid(extensions.st_makepoint(122.56, 10.72), 4326)::extensions.geography,
     'J.M. Basa St.', 'Iloilo City', 'SUBMITTED');
  insert into public.user_roles (user_id, role, province_id, store_id)
  values ('00000000-0000-4000-8000-0000000000b2', 'store_owner', ilo, '00000000-0000-4000-a000-0000000000b1');

  insert into public.riders (id, user_id, province_id, vehicle_type, status)
  values ('00000000-0000-4000-c000-0000000000b1', '00000000-0000-4000-8000-0000000000b3', ilo, 'MOTORCYCLE', 'APPROVED');

  insert into public.wallets (owner_type, owner_id, province_id, kind)
  values ('STORE', '00000000-0000-4000-a000-0000000000b1', ilo, 'STORE_TOPUP');

  insert into public.orders (
    code, province_id, customer_id, store_id, rider_id, fulfillment, payment_method, status,
    item_subtotal_centavos, delivery_fee_centavos, total_centavos, delivery_otp
  ) values
    ('FD-ILO-000001', ilo, '00000000-0000-4000-8000-0000000000b4', '00000000-0000-4000-a000-0000000000b1',
     '00000000-0000-4000-c000-0000000000b1', 'DELIVERY', 'COD', 'PLACED', 10000, 4000, 14000, '1111'),
    ('FD-NEG-000001', neg, '00000000-0000-4000-8000-000000000001', '00000000-0000-4000-a000-000000000001',
     '00000000-0000-4000-c000-000000000001', 'DELIVERY', 'COD', 'PLACED', 17800, 4000, 21800, '2222');
end;
$$;

-- ------------------------------------------------------------------ operator A (Negros Occidental)
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"00000000-0000-4000-8000-000000000006","role":"authenticated"}', true);

select is((select count(*)::int from public.orders o
           join public.provinces p on p.id = o.province_id where p.psgc_code = '0603000000'),
          0, 'operator A reads 0 orders of province B');
select is((select count(*)::int from public.orders where code = 'FD-NEG-000001'), 1, 'operator A reads own province orders');
select is((select count(*)::int from public.stores where id = '00000000-0000-4000-a000-0000000000b1'),
          0, 'operator A reads 0 unpublished stores of province B');
select is((select count(*)::int from public.riders where id = '00000000-0000-4000-c000-0000000000b1'),
          0, 'operator A reads 0 riders of province B');
select is((select count(*)::int from public.wallets w
           join public.provinces p on p.id = w.province_id where p.psgc_code = '0603000000'),
          0, 'operator A reads 0 wallets of province B');
select is((select count(*)::int from public.province_licenses l
           join public.provinces p on p.id = l.province_id where p.psgc_code = '0603000000'),
          0, 'operator A reads 0 licenses of province B');
select is((select count(*)::int from public.user_roles where user_id = '00000000-0000-4000-8000-0000000000b1'),
          0, 'operator A reads 0 role grants of province B');
select is((select count(*)::int from public.riders), 3, 'operator A reads the 3 riders of its own province');

-- ------------------------------------------------------------------ operator B (Iloilo)
select set_config('request.jwt.claims', '{"sub":"00000000-0000-4000-8000-0000000000b1","role":"authenticated"}', true);
select is((select count(*)::int from public.orders where code = 'FD-NEG-000001'), 0, 'operator B reads 0 orders of province A');
select is((select count(*)::int from public.orders where code = 'FD-ILO-000001'), 1, 'operator B reads own province orders');
select is((select count(*)::int from public.riders), 1, 'operator B reads only its own rider');

-- ------------------------------------------------------------------ customer / store / rider / anon
select set_config('request.jwt.claims', '{"sub":"00000000-0000-4000-8000-000000000002","role":"authenticated"}', true);
select is((select count(*)::int from public.orders), 0, 'a customer reads 0 orders of other customers');
select is((select count(*)::int from public.addresses), 1, 'a customer reads only own addresses');

select set_config('request.jwt.claims', '{"sub":"00000000-0000-4000-8000-000000000001","role":"authenticated"}', true);
select is((select count(*)::int from public.orders), 1, 'a customer reads own order');

select set_config('request.jwt.claims', '{"sub":"00000000-0000-4000-8000-000000000008","role":"authenticated"}', true);
select is((select count(*)::int from public.orders), 0, 'another store reads 0 orders of Mama''s Kitchen');

select set_config('request.jwt.claims', '{"sub":"00000000-0000-4000-8000-000000000007","role":"authenticated"}', true);
select is((select count(*)::int from public.orders), 1, 'store owner reads own store order');

select set_config('request.jwt.claims', '{"sub":"00000000-0000-4000-8000-000000000004","role":"authenticated"}', true);
select is((select count(*)::int from public.orders), 0, 'a rider reads 0 orders not assigned to them');

select set_config('request.jwt.claims', '{"sub":"00000000-0000-4000-8000-000000000003","role":"authenticated"}', true);
select is((select count(*)::int from public.orders), 1, 'the assigned rider reads the order');

reset role;
set local role anon;
select set_config('request.jwt.claims', '{"role":"anon"}', true);
select throws_ok('select count(*) from public.orders', '42501', null, 'anon has no access to orders at all');
select is((select count(*)::int from public.stores where status = 'ACTIVE'), 3,
          'anon sees the 3 published stores of serviceable provinces');

select * from finish();
rollback;
