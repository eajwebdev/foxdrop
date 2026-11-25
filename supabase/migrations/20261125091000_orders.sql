-- FoxDrop · 10 · Quotes, orders, events, dispatch offers, proof of delivery, reviews (§5.4, §5.5, §6, §8)
-- Orders are written only by server functions (place-order, order_transition, dispatch). Clients read them.

create table public.quotes (
  id uuid primary key default gen_random_uuid(),
  province_id uuid not null references public.provinces (id),
  customer_id uuid not null references auth.users (id) on delete cascade,
  store_id uuid not null,
  fulfillment public.fulfillment_type not null,
  dropoff_location extensions.geography(Point, 4326),
  distance_km numeric(6, 1) check (distance_km >= 0),
  item_subtotal_centavos bigint not null check (item_subtotal_centavos >= 0),
  delivery_fee_centavos bigint not null check (delivery_fee_centavos >= 0),
  discount_centavos bigint not null default 0 check (discount_centavos >= 0),
  total_centavos bigint not null check (total_centavos >= 0),
  rate_card_id uuid references public.delivery_rate_cards (id),
  rate_card_snapshot jsonb,
  promo_id uuid references public.promos (id),
  items jsonb not null check (jsonb_typeof(items) = 'array'),
  expires_at timestamptz not null default now() + interval '10 minutes',
  created_at timestamptz not null default now(),
  constraint quotes_store_fk foreign key (store_id, province_id) references public.stores (id, province_id),
  constraint quotes_total check (total_centavos = item_subtotal_centavos + delivery_fee_centavos - discount_centavos),
  constraint quotes_delivery_has_dropoff check (fulfillment = 'PICKUP' or dropoff_location is not null)
);
create index quotes_customer_idx on public.quotes (customer_id, created_at desc);

create table public.orders (
  id uuid primary key default gen_random_uuid(),
  code text not null unique check (code ~ '^FD-[A-Z]{3}-\d{6,}$'),
  province_id uuid not null references public.provinces (id),
  customer_id uuid not null references auth.users (id),
  store_id uuid not null,
  rider_id uuid,
  quote_id uuid references public.quotes (id),
  fulfillment public.fulfillment_type not null,
  payment_method public.payment_method not null,
  payment_status public.order_payment_status not null default 'UNPAID',
  status public.order_status not null,
  -- amounts (all server-computed, §6)
  item_subtotal_centavos bigint not null check (item_subtotal_centavos >= 0),
  delivery_fee_centavos bigint not null check (delivery_fee_centavos >= 0),
  discount_centavos bigint not null default 0 check (discount_centavos >= 0),
  total_centavos bigint not null check (total_centavos >= 0),
  platform_commission_centavos bigint not null default 0 check (platform_commission_centavos >= 0),
  operator_commission_centavos bigint not null default 0 check (operator_commission_centavos >= 0),
  rider_earning_centavos bigint not null default 0 check (rider_earning_centavos >= 0),
  operator_delivery_cut_centavos bigint not null default 0 check (operator_delivery_cut_centavos >= 0),
  store_net_centavos bigint not null default 0,
  rate_card_snapshot jsonb,
  distance_km numeric(6, 1) check (distance_km >= 0),
  dropoff_snapshot jsonb, -- address text, landmark, contact name/phone at order time
  dropoff_location extensions.geography(Point, 4326),
  -- Secret codes: never selectable directly (see grants migration); read through participant-only functions.
  pickup_code text check (pickup_code ~ '^\d{4}$'),
  delivery_otp text check (delivery_otp ~ '^\d{4}$'),
  notes text check (char_length(notes) <= 500),
  eta_at timestamptz,
  placed_at timestamptz,
  accepted_at timestamptz,
  preparing_at timestamptz,
  ready_at timestamptz,
  rider_assigned_at timestamptz,
  picked_up_at timestamptz,
  delivered_at timestamptz,
  completed_at timestamptz,
  cancelled_at timestamptz,
  cancel_reason text check (char_length(cancel_reason) <= 300),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint orders_id_province_key unique (id, province_id),
  constraint orders_store_fk foreign key (store_id, province_id) references public.stores (id, province_id),
  constraint orders_rider_fk foreign key (rider_id, province_id) references public.riders (id, province_id),
  constraint orders_total check (total_centavos = item_subtotal_centavos + delivery_fee_centavos - discount_centavos),
  constraint orders_pickup_no_fee check (fulfillment = 'DELIVERY' or delivery_fee_centavos = 0),
  constraint orders_pickup_has_code check (fulfillment = 'DELIVERY' or pickup_code is not null),
  constraint orders_picked_up_after_ready check (picked_up_at is null or ready_at is not null) -- §5.4
);
create index orders_province_status_idx on public.orders (province_id, status, created_at desc);
create index orders_rider_status_idx on public.orders (rider_id, status) where rider_id is not null;
create index orders_customer_idx on public.orders (customer_id, created_at desc);
create index orders_store_idx on public.orders (store_id, status, created_at desc);
create index orders_dropoff_gix on public.orders using gist (dropoff_location);
create trigger orders_updated_at before update on public.orders
  for each row execute function private.set_updated_at();

create table public.order_items (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null,
  province_id uuid not null,
  product_id uuid references public.products (id) on delete set null,
  name text not null, -- snapshot
  unit_price_centavos bigint not null check (unit_price_centavos >= 0), -- snapshot incl. options
  quantity integer not null check (quantity between 1 and 99),
  options jsonb not null default '[]'::jsonb,
  line_total_centavos bigint not null,
  constraint order_items_order_fk
    foreign key (order_id, province_id) references public.orders (id, province_id) on delete cascade,
  constraint order_items_line_total check (line_total_centavos = unit_price_centavos * quantity)
);
create index order_items_order_idx on public.order_items (order_id);

-- Every status change (§5.4). Append-only.
create table public.order_events (
  id bigint generated always as identity primary key,
  order_id uuid not null,
  province_id uuid not null,
  actor_user_id uuid references auth.users (id) on delete set null,
  actor_role public.app_role,
  from_status public.order_status,
  to_status public.order_status not null,
  location extensions.geography(Point, 4326),
  note text check (char_length(note) <= 500),
  created_at timestamptz not null default now(),
  constraint order_events_order_fk
    foreign key (order_id, province_id) references public.orders (id, province_id) on delete cascade
);
create index order_events_order_idx on public.order_events (order_id, created_at);
create trigger order_events_append_only before update or delete on public.order_events
  for each row execute function private.forbid_change();

-- Dispatch attempts (§5.5).
create table public.order_offers (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null,
  province_id uuid not null,
  rider_id uuid not null,
  round smallint not null check (round between 1 and 3),
  status public.offer_status not null default 'OFFERED',
  distance_to_store_m integer check (distance_to_store_m >= 0),
  estimated_earning_centavos bigint not null check (estimated_earning_centavos >= 0),
  offered_at timestamptz not null default now(),
  expires_at timestamptz not null,
  responded_at timestamptz,
  constraint order_offers_order_fk
    foreign key (order_id, province_id) references public.orders (id, province_id) on delete cascade,
  constraint order_offers_rider_fk
    foreign key (rider_id, province_id) references public.riders (id, province_id) on delete cascade,
  constraint order_offers_unique unique (order_id, rider_id, round)
);
create index order_offers_rider_idx on public.order_offers (rider_id, status, expires_at);
-- Only one accepted offer per order (two riders can never both win, §5.5).
create unique index order_offers_one_accepted on public.order_offers (order_id) where status = 'ACCEPTED';

alter table public.rider_locations
  add constraint rider_locations_order_fk foreign key (order_id) references public.orders (id) on delete set null;

-- POD (§9.2 step 7): a photo is always required.
create table public.proof_of_delivery (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null unique,
  province_id uuid not null,
  photo_path text not null, -- pod bucket, private, EXIF stripped
  recipient_name text check (char_length(recipient_name) <= 120),
  otp_verified boolean not null default false,
  unreachable_reason text check (char_length(unreachable_reason) <= 300),
  signature_path text,
  location extensions.geography(Point, 4326),
  captured_at timestamptz not null default now(),
  constraint proof_of_delivery_order_fk
    foreign key (order_id, province_id) references public.orders (id, province_id) on delete cascade,
  constraint proof_of_delivery_otp_or_reason check (otp_verified or unreachable_reason is not null)
);

-- One review per order, only after COMPLETED (enforced by the submit function in Phase 6).
create table public.reviews (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null unique,
  province_id uuid not null,
  customer_id uuid not null references auth.users (id) on delete cascade,
  store_id uuid not null,
  rider_id uuid,
  store_rating smallint not null check (store_rating between 1 and 5),
  rider_rating smallint check (rider_rating between 1 and 5),
  comment text check (char_length(comment) <= 1000),
  created_at timestamptz not null default now(),
  constraint reviews_order_fk
    foreign key (order_id, province_id) references public.orders (id, province_id) on delete cascade,
  constraint reviews_store_fk foreign key (store_id, province_id) references public.stores (id, province_id),
  constraint reviews_rider_needs_rider check (rider_rating is null or rider_id is not null)
);
create index reviews_store_idx on public.reviews (store_id, created_at desc);
create index reviews_rider_idx on public.reviews (rider_id, created_at desc) where rider_id is not null;

-- Participant-only access to the secret codes (§9.1 step 7, §9.3 pick-up code):
--   delivery OTP → the customer (they read it to the rider); pick-up code → the customer.
create or replace function public.my_order_codes(target_order uuid)
returns table (pickup_code text, delivery_otp text)
language sql
stable
security definer
set search_path = ''
as $$
  select o.pickup_code, o.delivery_otp
  from public.orders o
  where o.id = target_order and o.customer_id = (select auth.uid());
$$;
revoke all on function public.my_order_codes(uuid) from public, anon;
grant execute on function public.my_order_codes(uuid) to authenticated;
