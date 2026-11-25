-- FoxDrop · 06 · Operator applications and province licenses (§5.1, §8)

create table public.operator_applications (
  id uuid primary key default gen_random_uuid(),
  applicant_user_id uuid not null references auth.users (id) on delete cascade,
  province_id uuid not null references public.provinces (id),
  business_name text not null check (char_length(business_name) between 2 and 160),
  registration_number text check (char_length(registration_number) <= 60), -- DTI / SEC number
  contact_phone text not null check (contact_phone ~ '^\+63\d{10}$'),
  contact_email text check (char_length(contact_email) <= 254),
  valid_id_path text, -- kyc-docs bucket
  business_permit_path text, -- kyc-docs bucket
  status public.application_status not null default 'SUBMITTED',
  reviewed_by uuid references auth.users (id) on delete set null,
  reviewed_at timestamptz,
  rejection_reason text check (char_length(rejection_reason) <= 500),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index operator_applications_province_idx on public.operator_applications (province_id, status);
create index operator_applications_applicant_idx on public.operator_applications (applicant_user_id);
create trigger operator_applications_updated_at before update on public.operator_applications
  for each row execute function private.set_updated_at();

create table public.province_licenses (
  id uuid primary key default gen_random_uuid(),
  province_id uuid not null references public.provinces (id),
  operator_user_id uuid not null references auth.users (id),
  application_id uuid references public.operator_applications (id) on delete set null,
  status public.license_status not null default 'APPLIED',
  start_at timestamptz,
  end_at timestamptz,
  fee_centavos bigint not null check (fee_centavos > 0),
  payment_id uuid, -- FK added in the money migration
  renewal_of uuid references public.province_licenses (id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint province_licenses_term check (end_at is null or start_at is null or end_at > start_at),
  constraint province_licenses_active_has_term check (
    status not in ('ACTIVE', 'GRACE') or (start_at is not null and end_at is not null)
  )
);
-- Exclusivity (§5.1): at most one live license per province. A second insert for the same province fails.
create unique index province_licenses_one_live
  on public.province_licenses (province_id)
  where status in ('APPLIED', 'PENDING_PAYMENT', 'ACTIVE', 'GRACE');
create index province_licenses_operator_idx on public.province_licenses (operator_user_id);
create index province_licenses_end_idx on public.province_licenses (end_at) where status in ('ACTIVE', 'GRACE');
create trigger province_licenses_updated_at before update on public.province_licenses
  for each row execute function private.set_updated_at();

-- Customers only see provinces with an ACTIVE or GRACE license (§5.1). Readable without exposing license rows.
-- A set, so policies evaluate it once per query (`province_id in (select ...)`), not once per storefront row.
create or replace function private.serviceable_province_ids()
returns setof uuid
language sql
stable
security definer
set search_path = ''
as $$
  select pl.province_id
  from public.province_licenses pl
  join public.provinces p on p.id = pl.province_id
  where pl.status in ('ACTIVE', 'GRACE') and p.is_enabled;
$$;
revoke all on function private.serviceable_province_ids() from public;
grant execute on function private.serviceable_province_ids() to anon, authenticated, service_role;

-- Public province list for the apps: "FoxDrop is coming soon to your province" when not serviceable (§9.1).
create view public.province_directory
with (security_invoker = true)
as
select
  p.id,
  p.psgc_code,
  p.name,
  p.region_name,
  p.center,
  p.id in (select private.serviceable_province_ids()) as is_serviceable
from public.provinces p;
