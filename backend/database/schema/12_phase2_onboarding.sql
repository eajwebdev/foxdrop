-- FoxDrop · 12 · Phase 2: partner onboarding, payments for licenses, circle zones (§5.1–5.3, §6, §10)

-- Stores can be rejected with a reason and resubmitted (DECISIONS 59).
-- (A new enum value cannot be used in the transaction that adds it; nothing below uses it.)
alter type public.store_status add value if not exists 'REJECTED';
alter table public.stores
  add column rejection_reason text check (char_length(rejection_reason) <= 300);

-- Documents a store owner submits with the application (business permit, valid ID). Private disk, signed URLs.
create table public.store_documents (
  id uuid primary key default gen_random_uuid(),
  store_id uuid not null,
  province_id uuid not null,
  doc_type text not null check (doc_type in ('BUSINESS_PERMIT', 'VALID_ID')),
  storage_path text not null,
  status public.review_status not null default 'PENDING',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint store_documents_store_fk
    foreign key (store_id, province_id) references public.stores (id, province_id) on delete cascade,
  constraint store_documents_one_per_type unique (store_id, doc_type)
);
create trigger store_documents_updated_at before update on public.store_documents
  for each row execute function private.set_updated_at();

-- A rider has one current document per type (resubmitting replaces it).
alter table public.rider_documents
  add constraint rider_documents_one_per_type unique (rider_id, doc_type);
-- Its index leads with rider_id, so the plain rider_id index from 08 is redundant.
drop index public.rider_documents_rider_idx;

-- Checkout sessions expire; the local fake checkout is a provider too (DECISIONS 57).
alter table public.payments
  add column expires_at timestamptz;
alter table public.payments drop constraint payments_provider_check;
alter table public.payments
  add constraint payments_provider_check check (provider in ('paymongo', 'xendit', 'fake'));
create index payments_license_idx on public.payments (license_id, status) where license_id is not null;

-- Circle zones keep their centre and radius for editing; `area` stays the polygon used for coverage checks.
alter table public.delivery_zones
  add column center geography(Point, 4326),
  add column radius_m integer check (radius_m between 500 and 30000);

-- Review queues for operators. The store one also serves (province_id, status) lookups, replacing the 07 index.
create index stores_province_review_idx on public.stores (province_id, status, created_at);
drop index public.stores_province_status_idx;
create index riders_province_review_idx on public.riders (province_id, status, created_at);

-- The public province list says where customers can order (serviceable) and where an operator can apply
-- (available = enabled and no live license).
drop view public.province_directory;
create view public.province_directory as
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
    where pl.province_id = p.id and pl.status in ('APPLIED', 'PENDING_PAYMENT', 'ACTIVE', 'GRACE')
  ) as is_available
from public.provinces p;
