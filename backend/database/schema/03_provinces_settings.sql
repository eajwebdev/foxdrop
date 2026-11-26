-- FoxDrop · 03 · Provinces and platform settings (§8, §14)

create table public.provinces (
  id uuid primary key default gen_random_uuid(),
  psgc_code text not null unique check (psgc_code ~ '^\d{10}$'),
  name text not null,
  short_code text unique check (short_code ~ '^[A-Z]{3}$'), -- for order codes like FD-NEG-000123
  region_code text not null check (region_code ~ '^\d{10}$'),
  region_name text not null,
  center geography(Point, 4326),
  timezone text not null default 'Asia/Manila',
  is_enabled boolean not null default true, -- Super Admin "service coverage" switch (§9.5 step 7)
  store_fee_centavos bigint not null default 1500 check (store_fee_centavos >= 0),
  store_fee_grace_days integer not null default 7 check (store_fee_grace_days >= 0),
  rider_cash_limit_centavos bigint not null default 500000 check (rider_cash_limit_centavos >= 0),
  remit_within_hours integer not null default 24 check (remit_within_hours > 0),
  allow_net_remittance boolean not null default true,
  operator_commission_bps integer not null default 0 check (operator_commission_bps between 0 and 10000),
  dispatch_radii_km integer[] not null default '{5,8,12}',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
comment on table public.provinces is 'All 82 PH provinces + NCR (PSGC). One licensed operator per province (§5.1).';
create index provinces_center_gix on public.provinces using gist (center);
create trigger provinces_updated_at before update on public.provinces
  for each row execute function private.set_updated_at();

-- Exactly one row (id is always true).
create table public.platform_settings (
  id boolean primary key default true check (id),
  license_fee_centavos bigint not null default 1500000 check (license_fee_centavos > 0),
  license_term_days integer not null default 365 check (license_term_days > 0),
  license_grace_days integer not null default 15 check (license_grace_days >= 0),
  default_store_fee_centavos bigint not null default 1500 check (default_store_fee_centavos >= 0),
  platform_commission_bps integer not null default 0 check (platform_commission_bps between 0 and 10000),
  operator_commission_cap_bps integer not null default 0 check (operator_commission_cap_bps between 0 and 10000),
  default_rider_share_bps integer not null default 8000 check (default_rider_share_bps between 0 and 10000),
  offer_timeout_seconds integer not null default 30 check (offer_timeout_seconds > 0),
  store_response_timeout_seconds integer not null default 300 check (store_response_timeout_seconds > 0),
  unpaid_order_timeout_seconds integer not null default 900 check (unpaid_order_timeout_seconds > 0),
  -- feature flags, payment toggles, minimum app versions, notification templates (§9.5 step 7)
  settings jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now(),
  updated_by uuid references public.users (id) on delete set null
);
comment on table public.platform_settings is 'Single row of platform-wide defaults (§14). Changed only by Super Admin.';
create trigger platform_settings_updated_at before update on public.platform_settings
  for each row execute function private.set_updated_at();

insert into public.platform_settings (id) values (true);
