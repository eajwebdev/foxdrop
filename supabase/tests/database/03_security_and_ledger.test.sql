-- §17 / §7: RLS on every table, no client writes to money/status tables, secret columns hidden,
-- append-only zero-sum ledger, sign-up trigger and the access-token roles claim.
begin;
create extension if not exists pgtap with schema extensions;
select plan(17);

-- ------------------------------------------------------------------ schema-wide guarantees
select is(
  (select count(*)::int from pg_class c join pg_namespace n on n.oid = c.relnamespace
   where n.nspname = 'public' and c.relkind = 'r' and not (c.relrowsecurity and c.relforcerowsecurity)),
  0, 'every public table has RLS enabled and forced'
);
select is(
  (select count(*)::int from information_schema.role_table_grants
   where table_schema = 'public' and grantee in ('anon', 'authenticated')
     and privilege_type in ('INSERT', 'UPDATE', 'DELETE')
     and table_name in ('orders', 'order_events', 'payments', 'wallets', 'ledger_txns', 'ledger_entries',
                        'province_licenses', 'remittances', 'topups', 'settlement_batches', 'settlement_lines',
                        'payouts', 'riders', 'stores', 'user_roles')),
  0, 'apps have no INSERT/UPDATE/DELETE on money, status, license or role tables'
);

-- ------------------------------------------------------------------ sign-up trigger + JWT claim
insert into auth.users (id, aud, role, phone, raw_user_meta_data)
values ('00000000-0000-4000-8000-0000000000d1', 'authenticated', 'authenticated', '639170000301',
        '{"full_name":"Test Customer"}');
select is((select full_name from public.profiles where user_id = '00000000-0000-4000-8000-0000000000d1'),
          'Test Customer', 'sign-up creates a profile with the full name');
select is((select phone from public.profiles where user_id = '00000000-0000-4000-8000-0000000000d1'),
          '+639170000301', 'sign-up stores the phone in E.164');
select is((select role::text from public.user_roles where user_id = '00000000-0000-4000-8000-0000000000d1'),
          'customer', 'sign-up grants the customer role');
select ok(
  public.custom_access_token_hook(
    '{"user_id":"00000000-0000-4000-8000-000000000006","claims":{"sub":"00000000-0000-4000-8000-000000000006"}}'
  ) -> 'claims' -> 'app_roles' @> '[{"role":"operator"}]',
  'the access-token hook adds the operator role claim'
);

-- ------------------------------------------------------------------ append-only, zero-sum ledger
insert into public.ledger_txns (id, type, idempotency_key)
values ('00000000-0000-4000-e000-000000000001', 'TEST', 'test-balanced-0001');
insert into public.ledger_entries (txn_id, wallet_id, amount_centavos, type)
select '00000000-0000-4000-e000-000000000001', id, -10000, 'TEST' from public.wallets where kind = 'GATEWAY_CLEARING';
insert into public.ledger_entries (txn_id, wallet_id, amount_centavos, type)
select '00000000-0000-4000-e000-000000000001', id, 10000, 'TEST' from public.wallets where kind = 'PLATFORM_REVENUE';
select lives_ok('set constraints public.ledger_entries_balanced immediate', 'a balanced transaction is accepted');
select is((select balance_centavos from public.wallets where kind = 'PLATFORM_REVENUE'), 10000::bigint,
          'wallet balance follows the ledger');
select is((select balance_centavos from public.wallets where kind = 'GATEWAY_CLEARING'), -10000::bigint,
          'the clearing wallet goes negative (sign convention §7)');
set constraints public.ledger_entries_balanced deferred;

select throws_ok('update public.ledger_entries set amount_centavos = 1', '23001', null, 'ledger entries cannot be edited');
select throws_ok('delete from public.ledger_txns', '23001', null, 'ledger transactions cannot be deleted');

insert into public.ledger_txns (id, type, idempotency_key)
values ('00000000-0000-4000-e000-000000000002', 'TEST', 'test-unbalanced-0002');
insert into public.ledger_entries (txn_id, wallet_id, amount_centavos, type)
select '00000000-0000-4000-e000-000000000002', id, 500, 'TEST' from public.wallets where kind = 'PLATFORM_REVENUE';
select throws_ok('set constraints public.ledger_entries_balanced immediate', '23514', null,
                 'a transaction that does not sum to zero is rejected');

-- ------------------------------------------------------------------ as an app user
insert into public.orders (
  code, province_id, customer_id, store_id, fulfillment, payment_method, status,
  item_subtotal_centavos, delivery_fee_centavos, total_centavos, delivery_otp
)
select 'FD-NEG-000777', s.province_id, '00000000-0000-4000-8000-000000000001', s.id, 'DELIVERY', 'COD', 'PLACED',
       12900, 4000, 16900, '4321'
from public.stores s where s.id = '00000000-0000-4000-a000-000000000001';

set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"00000000-0000-4000-8000-000000000001","role":"authenticated"}', true);
select throws_ok('select delivery_otp from public.orders', '42501', null, 'the delivery OTP column is not readable directly');
select is((select delivery_otp from public.my_order_codes(
            (select id from public.orders where code = 'FD-NEG-000777'))), '4321',
          'the customer reads their own delivery OTP through my_order_codes()');
select throws_ok(
  $$ update public.orders set status = 'COMPLETED' where code = 'FD-NEG-000777' $$, '42501', null,
  'a customer cannot change an order status'
);
select throws_ok(
  $$ insert into public.wallets (owner_type, kind) values ('PLATFORM', 'PLATFORM_REVENUE') $$, '42501', null,
  'a client cannot create wallets'
);

select set_config('request.jwt.claims', '{"sub":"00000000-0000-4000-8000-000000000003","role":"authenticated"}', true);
select is((select count(*)::int from public.my_order_codes((select id from public.orders where code = 'FD-NEG-000777'))),
          0, 'nobody but the customer gets the codes');

select * from finish();
rollback;
