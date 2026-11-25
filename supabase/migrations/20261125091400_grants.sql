-- FoxDrop · 14 · Table and function privileges (§17: money and status tables are not writable by clients)
-- Supabase grants everything on new public tables to anon/authenticated by default. Start from nothing and grant
-- only what the apps need; RLS (migration 13) then decides which rows.

revoke all on all tables in schema public from anon, authenticated;
revoke all on all sequences in schema public from anon, authenticated;
revoke all on all functions in schema public from public, anon, authenticated;

alter default privileges for role postgres in schema public revoke all on tables from anon, authenticated;
alter default privileges for role postgres in schema public revoke all on sequences from anon, authenticated;
alter default privileges for role postgres in schema public revoke all on functions from public, anon, authenticated;

-- ---------------------------------------------------------------- reads
-- Signed-out visitors: only the public storefront and reference data.
grant select on
  public.provinces, public.province_directory, public.platform_settings, public.delivery_rate_cards,
  public.stores, public.product_categories, public.products, public.promos, public.reviews
to anon;

grant select on all tables in schema public to authenticated;

-- Secret columns are never readable directly:
--   orders.pickup_code / orders.delivery_otp → public.my_order_codes() (customer only)
--   riders.license_no_encrypted → server only
revoke select on public.orders from authenticated;
grant select (
  id, code, province_id, customer_id, store_id, rider_id, quote_id, fulfillment, payment_method, payment_status,
  status, item_subtotal_centavos, delivery_fee_centavos, discount_centavos, total_centavos,
  platform_commission_centavos, operator_commission_centavos, rider_earning_centavos,
  operator_delivery_cut_centavos, store_net_centavos, rate_card_snapshot, distance_km, dropoff_snapshot,
  dropoff_location, notes, eta_at, placed_at, accepted_at, preparing_at, ready_at, rider_assigned_at, picked_up_at,
  delivered_at, completed_at, cancelled_at, cancel_reason, created_at, updated_at
) on public.orders to authenticated;

revoke select on public.riders from authenticated;
grant select (
  id, user_id, province_id, vehicle_type, plate_no, license_no_last4, license_expiry, status, availability,
  last_location, last_seen_at, rating_avg, rating_count, rejection_reason, approved_by, approved_at, created_at,
  updated_at
) on public.riders to authenticated;

-- ---------------------------------------------------------------- writes (personal data only)
grant insert, update, delete on public.addresses, public.favorites, public.device_tokens to authenticated;
grant update (full_name, avatar_url, default_address_id) on public.profiles to authenticated;
grant update (read_at) on public.notifications to authenticated;
grant insert on public.support_tickets, public.ticket_messages, public.app_events to authenticated;

-- ---------------------------------------------------------------- functions callable from the apps
grant execute on function public.my_order_codes(uuid) to authenticated;
