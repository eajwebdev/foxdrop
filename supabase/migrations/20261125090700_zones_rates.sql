-- FoxDrop · 07 · Delivery zones and versioned rate cards (§6, §8)

create table public.delivery_zones (
  id uuid primary key default gen_random_uuid(),
  province_id uuid not null references public.provinces (id),
  name text not null check (char_length(name) between 1 and 80),
  area extensions.geography(MultiPolygon, 4326) not null,
  is_active boolean not null default true,
  created_by uuid references auth.users (id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index delivery_zones_area_gix on public.delivery_zones using gist (area);
create index delivery_zones_province_idx on public.delivery_zones (province_id) where is_active;
create trigger delivery_zones_updated_at before update on public.delivery_zones
  for each row execute function private.set_updated_at();

-- Rate cards are never edited: a change is a new row with a later effective_from. Orders keep a snapshot (§6).
-- delivery_fee = max(min_fee, base_fare + ceil_to_0.1(max(0, km − base_km)) × rate_per_km) + surcharges
create table public.delivery_rate_cards (
  id uuid primary key default gen_random_uuid(),
  province_id uuid not null references public.provinces (id),
  base_fare_centavos bigint not null default 4000 check (base_fare_centavos >= 0),
  base_km numeric(5, 1) not null default 2.0 check (base_km >= 0),
  rate_per_km_centavos bigint not null default 1000 check (rate_per_km_centavos >= 0),
  min_fee_centavos bigint not null default 4000 check (min_fee_centavos >= 0),
  max_km numeric(5, 1) not null default 15.0 check (max_km > 0),
  rider_share_bps integer not null default 8000 check (rider_share_bps between 0 and 10000),
  night_surcharge_centavos bigint not null default 0 check (night_surcharge_centavos >= 0),
  bad_weather_surcharge_centavos bigint not null default 0 check (bad_weather_surcharge_centavos >= 0),
  extra_stop_centavos bigint not null default 0 check (extra_stop_centavos >= 0),
  effective_from timestamptz not null default now(),
  created_by uuid, -- no FK: rate cards are append-only and outlive deleted accounts
  created_at timestamptz not null default now()
);
create index delivery_rate_cards_current_idx on public.delivery_rate_cards (province_id, effective_from desc);

create or replace function private.forbid_change()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  raise exception '% rows are append-only', tg_table_name using errcode = 'restrict_violation';
end;
$$;

create trigger delivery_rate_cards_append_only before update or delete on public.delivery_rate_cards
  for each row execute function private.forbid_change();
