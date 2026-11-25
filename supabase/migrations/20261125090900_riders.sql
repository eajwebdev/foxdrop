-- FoxDrop · 09 · Riders, documents, location history (§5.3, §8, §10, §17)

create table public.riders (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null unique references auth.users (id) on delete cascade,
  province_id uuid not null references public.provinces (id),
  vehicle_type public.vehicle_type not null,
  plate_no text check (plate_no ~ '^[A-Z0-9 -]{2,12}$'),
  -- Driver's license number: encrypted at rest (pgcrypto, key in Vault, §17). Only the last 4 are readable.
  license_no_encrypted bytea,
  license_no_last4 text check (license_no_last4 ~ '^[A-Z0-9]{4}$'),
  license_expiry date,
  status public.rider_status not null default 'APPLIED',
  availability public.rider_availability not null default 'OFFLINE',
  last_location extensions.geography(Point, 4326),
  last_seen_at timestamptz,
  rating_avg numeric(3, 2) not null default 0 check (rating_avg between 0 and 5),
  rating_count integer not null default 0 check (rating_count >= 0),
  rejection_reason text check (char_length(rejection_reason) <= 500),
  approved_by uuid references auth.users (id) on delete set null,
  approved_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint riders_id_province_key unique (id, province_id),
  constraint riders_online_requires_approval check (availability = 'OFFLINE' or status = 'APPROVED')
);
create index riders_dispatch_idx on public.riders (province_id, availability) where status = 'APPROVED';
create index riders_last_location_gix on public.riders using gist (last_location);
create trigger riders_updated_at before update on public.riders
  for each row execute function private.set_updated_at();

create table public.rider_documents (
  id uuid primary key default gen_random_uuid(),
  rider_id uuid not null,
  province_id uuid not null,
  doc_type public.rider_document_type not null,
  storage_path text not null, -- kyc-docs bucket, private
  expires_on date,
  status public.review_status not null default 'PENDING',
  reviewed_by uuid references auth.users (id) on delete set null,
  reviewed_at timestamptz,
  created_at timestamptz not null default now(),
  constraint rider_documents_rider_fk
    foreign key (rider_id, province_id) references public.riders (id, province_id) on delete cascade
);
create index rider_documents_rider_idx on public.rider_documents (rider_id);
create index rider_documents_province_idx on public.rider_documents (province_id, status);

-- Throttled points (one every ~30 s) kept 30 days (§10, §17 retention). Mock locations are stored and flagged.
create table public.rider_locations (
  id bigint generated always as identity primary key,
  rider_id uuid not null,
  province_id uuid not null,
  order_id uuid, -- FK added in the orders migration
  location extensions.geography(Point, 4326) not null,
  speed_mps real check (speed_mps >= 0),
  bearing real check (bearing >= 0 and bearing < 360),
  accuracy_m real check (accuracy_m >= 0),
  is_mock boolean not null default false,
  recorded_at timestamptz not null default now(),
  constraint rider_locations_rider_fk
    foreign key (rider_id, province_id) references public.riders (id, province_id) on delete cascade
);
create index rider_locations_rider_time_idx on public.rider_locations (rider_id, recorded_at desc);
create index rider_locations_recorded_idx on public.rider_locations (recorded_at);
create index rider_locations_province_time_idx on public.rider_locations (province_id, recorded_at desc);
create index rider_locations_location_gix on public.rider_locations using gist (location);

-- The current user's rider id, if they are a rider.
create or replace function private.current_rider_id()
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
  select r.id from public.riders r where r.user_id = (select auth.uid());
$$;
revoke all on function private.current_rider_id() from public;
grant execute on function private.current_rider_id() to authenticated, service_role;

-- PII encryption with a key from Supabase Vault (secret name 'foxdrop_pii_key'). Server-side only.
create or replace function private.pii_key()
returns text
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  key text;
begin
  select ds.decrypted_secret into key from vault.decrypted_secrets ds where ds.name = 'foxdrop_pii_key';
  if key is null then
    raise exception 'Vault secret foxdrop_pii_key is missing';
  end if;
  return key;
end;
$$;

create or replace function private.encrypt_pii(plain text)
returns bytea
language sql
security definer
set search_path = ''
as $$ select extensions.pgp_sym_encrypt(plain, private.pii_key()); $$;

create or replace function private.decrypt_pii(cipher bytea)
returns text
language sql
security definer
set search_path = ''
as $$ select extensions.pgp_sym_decrypt(cipher, private.pii_key()); $$;
-- Not callable by app users at all; Edge Functions use service_role.
revoke all on function private.pii_key() from public, anon, authenticated;
revoke all on function private.encrypt_pii(text) from public, anon, authenticated;
revoke all on function private.decrypt_pii(bytea) from public, anon, authenticated;
grant execute on function private.pii_key() to service_role;
grant execute on function private.encrypt_pii(text) to service_role;
grant execute on function private.decrypt_pii(bytea) to service_role;
