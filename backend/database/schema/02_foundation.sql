-- FoxDrop · 02 · Enums and shared helpers
-- Status enums mirror MASTER_PROMPT §5 exactly (UPPER_CASE labels). Roles mirror §5 `app_role`.
-- Money is always bigint centavos; percentages are integer basis points (1 % = 100 bps).

create type public.app_role as enum (
  'customer', 'rider', 'store_owner', 'store_staff', 'operator', 'operator_staff', 'super_admin', 'admin_staff'
);

-- §5.1 (AVAILABLE is a province without a live license, so it is not a license status)
create type public.license_status as enum (
  'APPLIED', 'PENDING_PAYMENT', 'ACTIVE', 'GRACE', 'EXPIRED', 'SUSPENDED', 'REJECTED'
);
create type public.application_status as enum ('SUBMITTED', 'APPROVED', 'REJECTED', 'WITHDRAWN');

-- §5.2
create type public.store_status as enum ('DRAFT', 'SUBMITTED', 'APPROVED', 'ACTIVE', 'PAUSED', 'SUSPENDED', 'CLOSED');
create type public.store_category as enum ('FOOD', 'GROCERIES', 'MEDICINE', 'MORE');
create type public.store_fee_status as enum ('CURRENT', 'OVERDUE');

-- §5.3
create type public.rider_status as enum ('APPLIED', 'DOCS_REVIEW', 'APPROVED', 'REJECTED', 'SUSPENDED', 'DEACTIVATED');
create type public.rider_availability as enum ('OFFLINE', 'ONLINE', 'ON_JOB');
create type public.vehicle_type as enum ('MOTORCYCLE', 'BICYCLE', 'TRICYCLE', 'CAR');
create type public.rider_document_type as enum ('VALID_ID', 'DRIVERS_LICENSE', 'OR_CR', 'VEHICLE_PHOTO', 'SELFIE');
create type public.review_status as enum ('PENDING', 'APPROVED', 'REJECTED');

-- §5.4
create type public.order_status as enum (
  'PENDING_PAYMENT', 'PLACED', 'ACCEPTED', 'PREPARING', 'READY_FOR_PICKUP', 'RIDER_ASSIGNED', 'RIDER_AT_STORE',
  'PICKED_UP', 'ARRIVING', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'DISPUTED'
);
create type public.fulfillment_type as enum ('DELIVERY', 'PICKUP');
create type public.payment_method as enum ('ONLINE', 'COD');
create type public.order_payment_status as enum ('UNPAID', 'PENDING', 'PAID', 'FAILED', 'REFUNDED');
create type public.offer_status as enum ('OFFERED', 'ACCEPTED', 'DECLINED', 'EXPIRED');

-- §7
create type public.payment_purpose as enum ('ORDER', 'TOPUP', 'LICENSE');
create type public.payment_status as enum ('PENDING', 'PAID', 'FAILED', 'EXPIRED', 'REFUNDED');
create type public.wallet_owner_type as enum ('STORE', 'RIDER', 'OPERATOR', 'PLATFORM', 'SYSTEM');
create type public.wallet_kind as enum (
  'STORE_TOPUP', 'STORE_PAYABLE', 'RIDER_EARNINGS', 'RIDER_CASH', 'OPERATOR_REVENUE', 'OPERATOR_CASH',
  'PLATFORM_REVENUE', 'GATEWAY_CLEARING', 'CUSTOMER_REFUND'
);
create type public.topup_method as enum ('ONLINE', 'CASH');
create type public.topup_status as enum ('PENDING', 'CONFIRMED', 'FAILED');
create type public.remittance_method as enum ('CASH', 'GCASH');
create type public.remittance_status as enum ('SUBMITTED', 'CONFIRMED', 'REJECTED');
create type public.settlement_status as enum ('DRAFT', 'FINALIZED', 'PAID');
create type public.payee_type as enum ('STORE', 'RIDER');
create type public.settlement_line_status as enum ('PENDING', 'PAID');

-- §6 promos ("Exclusive Promos & Deals")
create type public.promo_scope as enum ('PLATFORM', 'PROVINCE', 'STORE');
create type public.promo_type as enum ('PERCENT', 'FIXED', 'FREE_DELIVERY');

-- §9.5 support & disputes
create type public.ticket_status as enum ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED');
create type public.dispute_status as enum ('OPEN', 'RESOLVED');
create type public.dispute_resolution as enum ('REFUND', 'ADJUSTMENT', 'PENALTY', 'NONE');

-- Keeps updated_at honest on every table that has it.
create or replace function private.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;
