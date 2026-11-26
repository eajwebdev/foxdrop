-- FoxDrop · 14 · Super Admin web foundation (Phase W1, §20): 2FA, offline license payments, suspensions.

-- Fortify TOTP (§20.4). The secret and recovery codes are stored encrypted by Laravel (APP_KEY).
alter table public.users
  add column two_factor_secret text,
  add column two_factor_recovery_codes text,
  add column two_factor_confirmed_at timestamptz;

-- An admin records an offline license payment as a `manual` payment; its bank reference is the intent id, unique
-- per provider, so one transfer cannot be recorded twice (DECISIONS 76).
alter table public.payments drop constraint payments_provider_check;
alter table public.payments
  add constraint payments_provider_check check (provider in ('paymongo', 'xendit', 'fake', 'manual'));

alter table public.wallets drop constraint wallets_kind_matches_owner;
alter table public.wallets add constraint wallets_kind_matches_owner check (
  case owner_type
    when 'STORE' then kind in ('STORE_TOPUP', 'STORE_PAYABLE') and owner_id is not null and province_id is not null
    when 'RIDER' then kind in ('RIDER_EARNINGS', 'RIDER_CASH') and owner_id is not null and province_id is not null
    when 'OPERATOR' then kind in ('OPERATOR_REVENUE', 'OPERATOR_CASH') and province_id is not null
    when 'PLATFORM' then kind = 'PLATFORM_REVENUE' and owner_id is null
    when 'SYSTEM' then kind in ('GATEWAY_CLEARING', 'CUSTOMER_REFUND', 'OFFLINE_RECEIPTS') and owner_id is null
  end
);

-- A suspended license still holds its province (DECISIONS 77): it stays in the one-live-license index and the
-- province is not offered to new applicants.
drop index public.province_licenses_one_live;
create unique index province_licenses_one_live
  on public.province_licenses (province_id)
  where status in ('APPLIED', 'PENDING_PAYMENT', 'ACTIVE', 'GRACE', 'SUSPENDED');

create or replace view public.province_directory as
select
  p.id,
  p.psgc_code,
  p.name,
  p.region_name,
  p.center,
  p.is_enabled and exists (
    select 1 from public.province_licenses pl where pl.province_id = p.id and pl.status in ('ACTIVE', 'GRACE')
  ) as is_serviceable,
  p.is_enabled and not exists (
    select 1 from public.province_licenses pl
    where pl.province_id = p.id and pl.status in ('APPLIED', 'PENDING_PAYMENT', 'ACTIVE', 'GRACE', 'SUSPENDED')
  ) as is_available
from public.provinces p;

-- Why an admin or operator suspended, closed or deactivated something; shown to the partner (DECISIONS 79).
alter table public.province_licenses
  add column status_reason text check (char_length(status_reason) <= 300);
alter table public.stores
  add column status_reason text check (char_length(status_reason) <= 300);
alter table public.riders
  add column status_reason text check (char_length(status_reason) <= 300);

-- What an admin_staff account may do in the admin web (DECISIONS 72). super_admin holds every permission; who is
-- an admin at all stays in user_roles. Only the super admin changes these rows (CreateAdminStaff,
-- UpdateAdminPermissions).
create table public.admin_permissions (
  user_id uuid not null references public.users (id) on delete cascade,
  permission text not null check (permission in (
    'franchises.view', 'franchises.manage', 'partners.view', 'partners.manage', 'users.view', 'users.manage',
    'audit.view'
  )),
  granted_by uuid references public.users (id) on delete set null,
  created_at timestamptz not null default now(),
  primary key (user_id, permission)
);
