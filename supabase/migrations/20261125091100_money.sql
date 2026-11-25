-- FoxDrop · 11 · Payments, double-entry ledger, top-ups, remittances, settlement (§7, §8)
-- Ledger rules enforced by the database itself:
--   * ledger_txns / ledger_entries are append-only (UPDATE/DELETE/TRUNCATE raise)
--   * every transaction sums to zero (deferred constraint trigger, checked at COMMIT)
--   * wallet balances change only through the ledger trigger
-- Posting functions (one per §7 rule) arrive in Phase 5; clients never write these tables.

create table public.payments (
  id uuid primary key default gen_random_uuid(),
  province_id uuid references public.provinces (id),
  purpose public.payment_purpose not null,
  provider text not null default 'paymongo' check (provider in ('paymongo', 'xendit')),
  intent_id text,
  checkout_url text,
  amount_centavos bigint not null check (amount_centavos > 0),
  status public.payment_status not null default 'PENDING',
  payer_user_id uuid references auth.users (id) on delete set null,
  order_id uuid references public.orders (id),
  license_id uuid references public.province_licenses (id),
  topup_id uuid, -- FK added below
  raw jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint payments_intent_key unique (provider, intent_id),
  constraint payments_target check (
    case purpose
      when 'ORDER' then order_id is not null
      when 'LICENSE' then license_id is not null
      when 'TOPUP' then topup_id is not null
    end
  )
);
create index payments_payer_idx on public.payments (payer_user_id, created_at desc);
create index payments_province_idx on public.payments (province_id, status, created_at desc);
create trigger payments_updated_at before update on public.payments
  for each row execute function private.set_updated_at();

alter table public.province_licenses
  add constraint province_licenses_payment_fk foreign key (payment_id) references public.payments (id);

-- Webhook idempotency (§10): the same provider event is processed once.
create table public.webhook_events (
  id uuid primary key default gen_random_uuid(),
  provider text not null,
  event_id text not null,
  event_type text not null,
  payload jsonb not null,
  received_at timestamptz not null default now(),
  processed_at timestamptz,
  error text,
  constraint webhook_events_key unique (provider, event_id)
);

create table public.wallets (
  id uuid primary key default gen_random_uuid(),
  owner_type public.wallet_owner_type not null,
  owner_id uuid, -- store id / rider id / operator user id; null for PLATFORM and SYSTEM wallets
  province_id uuid references public.provinces (id),
  kind public.wallet_kind not null,
  currency char(3) not null default 'PHP' check (currency = 'PHP'),
  balance_centavos bigint not null default 0, -- maintained only by ledger_entries_apply()
  created_at timestamptz not null default now(),
  constraint wallets_unique unique nulls not distinct (owner_type, owner_id, province_id, kind),
  constraint wallets_kind_matches_owner check (
    case owner_type
      when 'STORE' then kind in ('STORE_TOPUP', 'STORE_PAYABLE') and owner_id is not null and province_id is not null
      when 'RIDER' then kind in ('RIDER_EARNINGS', 'RIDER_CASH') and owner_id is not null and province_id is not null
      when 'OPERATOR' then kind in ('OPERATOR_REVENUE', 'OPERATOR_CASH') and province_id is not null
      when 'PLATFORM' then kind = 'PLATFORM_REVENUE' and owner_id is null
      when 'SYSTEM' then kind in ('GATEWAY_CLEARING', 'CUSTOMER_REFUND') and owner_id is null
    end
  )
);
create index wallets_owner_idx on public.wallets (owner_type, owner_id);
create index wallets_province_idx on public.wallets (province_id);

create table public.ledger_txns (
  id uuid primary key default gen_random_uuid(),
  type text not null check (char_length(type) between 3 and 60),
  idempotency_key text not null unique check (char_length(idempotency_key) between 8 and 200),
  province_id uuid references public.provinces (id),
  created_by uuid, -- no FK: the ledger is append-only and outlives deleted accounts
  memo text check (char_length(memo) <= 500),
  created_at timestamptz not null default now()
);
create index ledger_txns_province_idx on public.ledger_txns (province_id, created_at desc);

create table public.ledger_entries (
  id bigint generated always as identity primary key,
  txn_id uuid not null references public.ledger_txns (id),
  wallet_id uuid not null references public.wallets (id),
  amount_centavos bigint not null check (amount_centavos <> 0), -- + credit / − debit (sign convention §7)
  type text not null check (char_length(type) between 3 and 60),
  ref_type text check (char_length(ref_type) <= 40),
  ref_id uuid,
  province_id uuid references public.provinces (id),
  created_at timestamptz not null default now()
);
create index ledger_entries_txn_idx on public.ledger_entries (txn_id);
create index ledger_entries_wallet_idx on public.ledger_entries (wallet_id, created_at desc);
create index ledger_entries_ref_idx on public.ledger_entries (ref_type, ref_id);

-- Append-only ledger.
create trigger ledger_txns_append_only before update or delete on public.ledger_txns
  for each row execute function private.forbid_change();
create trigger ledger_entries_append_only before update or delete on public.ledger_entries
  for each row execute function private.forbid_change();
create trigger ledger_txns_no_truncate before truncate on public.ledger_txns
  for each statement execute function private.forbid_change();
create trigger ledger_entries_no_truncate before truncate on public.ledger_entries
  for each statement execute function private.forbid_change();

-- Every transaction must sum to zero once the posting transaction commits.
create or replace function private.ledger_txn_must_balance()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
  total numeric;
begin
  select sum(e.amount_centavos) into total from public.ledger_entries e where e.txn_id = new.txn_id;
  if total <> 0 then
    raise exception 'ledger txn % does not balance (sum = %)', new.txn_id, total
      using errcode = 'check_violation';
  end if;
  return null;
end;
$$;
create constraint trigger ledger_entries_balanced
  after insert on public.ledger_entries
  deferrable initially deferred
  for each row execute function private.ledger_txn_must_balance();

-- Wallet balance follows the ledger and nothing else.
create or replace function private.ledger_entries_apply()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  update public.wallets w
     set balance_centavos = w.balance_centavos + new.amount_centavos
   where w.id = new.wallet_id;
  return null;
end;
$$;
create trigger ledger_entries_apply_balance
  after insert on public.ledger_entries
  for each row execute function private.ledger_entries_apply();

create table public.topups (
  id uuid primary key default gen_random_uuid(),
  province_id uuid not null,
  store_id uuid not null,
  amount_centavos bigint not null check (amount_centavos > 0),
  method public.topup_method not null,
  status public.topup_status not null default 'PENDING',
  payment_id uuid references public.payments (id),
  receipt_path text, -- receipts bucket (cash top-ups recorded by the operator)
  recorded_by uuid references auth.users (id) on delete set null,
  confirmed_at timestamptz,
  ledger_txn_id uuid references public.ledger_txns (id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint topups_store_fk foreign key (store_id, province_id) references public.stores (id, province_id),
  constraint topups_cash_has_receipt check (method <> 'CASH' or receipt_path is not null)
);
create index topups_store_idx on public.topups (store_id, created_at desc);
create index topups_province_idx on public.topups (province_id, created_at desc);
create trigger topups_updated_at before update on public.topups
  for each row execute function private.set_updated_at();

alter table public.payments
  add constraint payments_topup_fk foreign key (topup_id) references public.topups (id);

create table public.remittances (
  id uuid primary key default gen_random_uuid(),
  province_id uuid not null,
  rider_id uuid not null,
  amount_centavos bigint not null check (amount_centavos > 0),
  earnings_deducted_centavos bigint not null default 0 check (earnings_deducted_centavos >= 0),
  method public.remittance_method not null,
  reference text check (char_length(reference) <= 80),
  proof_path text, -- receipts bucket
  status public.remittance_status not null default 'SUBMITTED',
  confirmed_by uuid references auth.users (id) on delete set null,
  confirmed_at timestamptz,
  rejection_reason text check (char_length(rejection_reason) <= 300),
  ledger_txn_id uuid references public.ledger_txns (id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint remittances_rider_fk foreign key (rider_id, province_id) references public.riders (id, province_id),
  constraint remittances_gcash_has_reference check (method <> 'GCASH' or reference is not null)
);
create index remittances_rider_idx on public.remittances (rider_id, created_at desc);
create index remittances_province_status_idx on public.remittances (province_id, status, created_at desc);
create trigger remittances_updated_at before update on public.remittances
  for each row execute function private.set_updated_at();

create table public.settlement_batches (
  id uuid primary key default gen_random_uuid(),
  province_id uuid not null references public.provinces (id),
  period_start date not null,
  period_end date not null,
  status public.settlement_status not null default 'DRAFT',
  created_by uuid references auth.users (id) on delete set null,
  finalized_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint settlement_batches_period check (period_end >= period_start),
  constraint settlement_batches_id_province_key unique (id, province_id),
  constraint settlement_batches_unique_period unique (province_id, period_start, period_end)
);
create trigger settlement_batches_updated_at before update on public.settlement_batches
  for each row execute function private.set_updated_at();

create table public.settlement_lines (
  id uuid primary key default gen_random_uuid(),
  batch_id uuid not null,
  province_id uuid not null,
  payee_type public.payee_type not null,
  store_id uuid,
  rider_id uuid,
  amount_centavos bigint not null check (amount_centavos > 0),
  status public.settlement_line_status not null default 'PENDING',
  paid_at timestamptz,
  constraint settlement_lines_batch_fk
    foreign key (batch_id, province_id) references public.settlement_batches (id, province_id) on delete cascade,
  constraint settlement_lines_store_fk foreign key (store_id, province_id) references public.stores (id, province_id),
  constraint settlement_lines_rider_fk foreign key (rider_id, province_id) references public.riders (id, province_id),
  constraint settlement_lines_payee check (
    (payee_type = 'STORE' and store_id is not null and rider_id is null)
    or (payee_type = 'RIDER' and rider_id is not null and store_id is null)
  )
);
create index settlement_lines_batch_idx on public.settlement_lines (batch_id);
create index settlement_lines_province_idx on public.settlement_lines (province_id);
create index settlement_lines_store_idx on public.settlement_lines (store_id) where store_id is not null;
create index settlement_lines_rider_idx on public.settlement_lines (rider_id) where rider_id is not null;

create table public.payouts (
  id uuid primary key default gen_random_uuid(),
  province_id uuid not null references public.provinces (id),
  settlement_line_id uuid not null unique references public.settlement_lines (id),
  amount_centavos bigint not null check (amount_centavos > 0),
  method text not null check (method in ('CASH', 'GCASH', 'BANK_TRANSFER', 'MAYA')),
  reference text check (char_length(reference) <= 80),
  paid_by uuid references auth.users (id) on delete set null,
  paid_at timestamptz not null default now(),
  ledger_txn_id uuid references public.ledger_txns (id)
);

-- Wallets the current user may see (owner of the wallet, its province operator, or platform admin). A set, so RLS
-- evaluates it once per query (`id in (select ...)`) instead of once per ledger row.
create or replace function private.visible_wallet_ids()
returns setof uuid
language sql
stable
security definer
set search_path = ''
as $$
  select w.id from public.wallets w
  where (select private.is_platform_admin())
     or (w.province_id is not null and w.province_id in (select private.operator_province_ids()))
     or (w.owner_type = 'STORE' and w.owner_id in (select private.member_store_ids()))
     or (w.owner_type = 'RIDER' and w.owner_id = (select private.current_rider_id()));
$$;
revoke all on function private.visible_wallet_ids() from public;
grant execute on function private.visible_wallet_ids() to authenticated, service_role;
