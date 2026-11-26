-- FoxDrop · 16 · Phase 3: quotes become orders, order codes, idempotent placing, refunds (§5.4, §6, DECISIONS 83–92)

-- Order codes FD-<province short code>-000123: one sequence for the whole platform keeps codes unique and short.
-- A province gets its short code the first time it takes an order (Province::shortCode()).
create sequence public.order_code_seq start 1;

-- A quote is used by at most one order (DECISIONS 86); `distance_source` says how the distance was measured.
alter table public.quotes
  add column used_at timestamptz,
  add column distance_source text check (distance_source in ('routes', 'straight_line')),
  add column split jsonb; -- commissions, rider earning and operator cut, copied onto the order

-- Placing an order twice with the same Idempotency-Key returns the first order.
alter table public.orders
  add column idempotency_key text check (char_length(idempotency_key) between 8 and 100),
  add column pickup_code_attempts smallint not null default 0,
  add column pickup_code_locked_until timestamptz;
create unique index orders_customer_idempotency_key on public.orders (customer_id, idempotency_key)
  where idempotency_key is not null;
create unique index orders_quote_key on public.orders (quote_id) where quote_id is not null;

-- Refunds (DECISIONS 89).
alter table public.payments
  add column refunded_at timestamptz,
  add column provider_payment_id text; -- the gateway's payment id, needed to refund it

-- The order jobs look for stale PENDING_PAYMENT / PLACED orders every minute.
create index orders_waiting_idx on public.orders (status, created_at) where status in ('PENDING_PAYMENT', 'PLACED');
create index payments_order_idx on public.payments (order_id, status) where order_id is not null;
