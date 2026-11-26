-- FoxDrop · 08 · Stores and catalog (§5.2, §8)
-- Every store-owned row carries province_id, tied to its store's province by a composite FK, so a row can never
-- claim a different province than its store (province isolation, §0 rule 5).

create table public.stores (
  id uuid primary key default gen_random_uuid(),
  province_id uuid not null references public.provinces (id),
  owner_user_id uuid not null references public.users (id),
  name text not null check (char_length(name) between 2 and 120),
  category public.store_category not null,
  description text check (char_length(description) <= 1000),
  logo_path text,
  banner_path text,
  location geography(Point, 4326) not null,
  address_line text not null check (char_length(address_line) <= 300),
  barangay text check (char_length(barangay) <= 120),
  city text not null check (char_length(city) <= 120),
  phone text check (phone ~ '^\+63\d{10}$'),
  -- {"mon":[["08:00","20:00"]], ...} in Asia/Manila
  opening_hours jsonb not null default '{}'::jsonb,
  prep_time_min integer not null default 15 check (prep_time_min between 1 and 240),
  status public.store_status not null default 'DRAFT',
  rating_avg numeric(3, 2) not null default 0 check (rating_avg between 0 and 5),
  rating_count integer not null default 0 check (rating_count >= 0),
  accepts_cod boolean not null default true,
  accepts_pickup boolean not null default true,
  fee_status public.store_fee_status not null default 'CURRENT',
  fee_overdue_since date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint stores_id_province_key unique (id, province_id)
);
create index stores_location_gix on public.stores using gist (location);
create index stores_province_status_idx on public.stores (province_id, status);
create index stores_owner_idx on public.stores (owner_user_id);
create trigger stores_updated_at before update on public.stores
  for each row execute function private.set_updated_at();

-- Role scopes now that stores exist: a store role must point at a store in the same province.
alter table public.user_roles
  add constraint user_roles_store_fk
  foreign key (store_id, province_id) references public.stores (id, province_id) on delete cascade;

create table public.product_categories (
  id uuid primary key default gen_random_uuid(),
  store_id uuid not null,
  province_id uuid not null,
  name text not null check (char_length(name) between 1 and 60),
  sort_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint product_categories_store_fk
    foreign key (store_id, province_id) references public.stores (id, province_id) on delete cascade,
  constraint product_categories_id_store_key unique (id, store_id)
);
create index product_categories_store_idx on public.product_categories (store_id, sort_order);
create trigger product_categories_updated_at before update on public.product_categories
  for each row execute function private.set_updated_at();

create table public.products (
  id uuid primary key default gen_random_uuid(),
  store_id uuid not null,
  province_id uuid not null,
  category_id uuid,
  name text not null check (char_length(name) between 1 and 120),
  description text check (char_length(description) <= 1000),
  price_centavos bigint not null check (price_centavos > 0),
  image_path text,
  is_available boolean not null default true,
  stock integer check (stock >= 0), -- null = not tracked
  -- [{"group":"Size","required":true,"max":1,"choices":[{"name":"Large","price_centavos":2000}]}]
  options jsonb not null default '[]'::jsonb check (jsonb_typeof(options) = 'array'),
  sort_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint products_store_fk
    foreign key (store_id, province_id) references public.stores (id, province_id) on delete cascade,
  -- a product's category must belong to the same store
  constraint products_category_fk
    foreign key (category_id, store_id) references public.product_categories (id, store_id) on delete set null (category_id)
);
create index products_store_idx on public.products (store_id, sort_order);
create index products_province_idx on public.products (province_id) where is_available;
create trigger products_updated_at before update on public.products
  for each row execute function private.set_updated_at();

-- "Exclusive Promos & Deals" (§9.1). Discounts are paid by the promo owner (§6, §7 rule 3).
create table public.promos (
  id uuid primary key default gen_random_uuid(),
  code text not null check (code ~ '^[A-Z0-9]{3,20}$'),
  scope public.promo_scope not null,
  province_id uuid references public.provinces (id),
  store_id uuid,
  type public.promo_type not null,
  value integer not null check (value >= 0), -- bps for PERCENT, centavos for FIXED, ignored for FREE_DELIVERY
  max_discount_centavos bigint check (max_discount_centavos > 0),
  min_subtotal_centavos bigint not null default 0 check (min_subtotal_centavos >= 0),
  usage_limit integer check (usage_limit > 0),
  per_user_limit integer not null default 1 check (per_user_limit > 0),
  starts_at timestamptz not null,
  ends_at timestamptz not null,
  is_active boolean not null default true,
  created_by uuid references public.users (id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint promos_window check (ends_at > starts_at),
  constraint promos_percent_range check (type <> 'PERCENT' or value between 1 and 10000),
  constraint promos_scope_fields check (
    case scope
      when 'PLATFORM' then province_id is null and store_id is null
      when 'PROVINCE' then province_id is not null and store_id is null
      when 'STORE' then province_id is not null and store_id is not null
    end
  ),
  constraint promos_store_fk
    foreign key (store_id, province_id) references public.stores (id, province_id) on delete cascade
);
create unique index promos_code_scope_key on public.promos (code, coalesce(province_id, '00000000-0000-0000-0000-000000000000'::uuid))
  where is_active;
create trigger promos_updated_at before update on public.promos
  for each row execute function private.set_updated_at();

create table public.favorites (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users (id) on delete cascade,
  province_id uuid not null references public.provinces (id),
  store_id uuid references public.stores (id) on delete cascade,
  product_id uuid references public.products (id) on delete cascade,
  created_at timestamptz not null default now(),
  constraint favorites_one_target check (num_nonnulls(store_id, product_id) = 1),
  constraint favorites_unique unique nulls not distinct (user_id, store_id, product_id)
);
create index favorites_user_idx on public.favorites (user_id);
