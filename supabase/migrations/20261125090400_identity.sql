-- FoxDrop · 04 · Profiles, roles, addresses (§5, §8)

create table public.profiles (
  user_id uuid primary key references auth.users (id) on delete cascade,
  full_name text check (char_length(full_name) <= 120),
  phone text check (phone ~ '^\+63\d{10}$'),
  email text check (char_length(email) <= 254),
  avatar_url text,
  default_address_id uuid, -- FK added below, after addresses exists
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create trigger profiles_updated_at before update on public.profiles
  for each row execute function private.set_updated_at();

-- A user can hold several roles, each optionally scoped to a province or a store (§5).
-- store_id gets its FK in the stores migration.
create table public.user_roles (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  role public.app_role not null,
  province_id uuid references public.provinces (id) on delete cascade,
  store_id uuid,
  granted_by uuid references auth.users (id) on delete set null,
  created_at timestamptz not null default now(),
  constraint user_roles_scope check (
    case
      when role in ('operator', 'operator_staff', 'rider') then province_id is not null and store_id is null
      when role in ('store_owner', 'store_staff') then province_id is not null and store_id is not null
      else province_id is null and store_id is null -- customer, super_admin, admin_staff are global
    end
  ),
  constraint user_roles_unique unique nulls not distinct (user_id, role, province_id, store_id)
);
-- Lookups by user_id use user_roles_unique (user_id is its leading column).
create index user_roles_province_idx on public.user_roles (province_id) where province_id is not null;
create index user_roles_store_idx on public.user_roles (store_id) where store_id is not null;

create table public.addresses (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  province_id uuid not null references public.provinces (id),
  label text not null default 'Home' check (char_length(label) <= 40),
  location extensions.geography(Point, 4326) not null,
  address_line text not null check (char_length(address_line) <= 300),
  landmark text check (char_length(landmark) <= 200),
  barangay text check (char_length(barangay) <= 120),
  city text not null check (char_length(city) <= 120),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index addresses_user_idx on public.addresses (user_id);
create index addresses_location_gix on public.addresses using gist (location);
create trigger addresses_updated_at before update on public.addresses
  for each row execute function private.set_updated_at();

alter table public.profiles
  add constraint profiles_default_address_fk
  foreign key (default_address_id) references public.addresses (id) on delete set null;

-- Every new account gets a profile and the customer role. Phone comes from Supabase Auth (E.164 without '+').
create or replace function private.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (user_id, full_name, phone, email)
  values (
    new.id,
    nullif(left(trim(new.raw_user_meta_data ->> 'full_name'), 120), ''),
    case when new.phone ~ '^63\d{10}$' then '+' || new.phone end,
    new.email
  )
  on conflict (user_id) do nothing;

  insert into public.user_roles (user_id, role) values (new.id, 'customer')
  on conflict on constraint user_roles_unique do nothing;
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function private.handle_new_user();
