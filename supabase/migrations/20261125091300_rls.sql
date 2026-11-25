-- FoxDrop · 13 · Row Level Security (§8 RLS summary, §17)
-- RLS is enabled AND forced on every table in `public`; with no matching policy a query returns nothing.
-- Who sees what:
--   customer  → own rows (addresses, orders, quotes, payments, tickets, notifications)
--   store     → rows of the stores they own/work for
--   rider     → own rider record and offered/assigned orders
--   operator  → every provincial row of their province(s)
--   admin     → everything (super_admin, admin_staff)
-- Writes from apps are limited to personal data (addresses, favorites, device tokens, profile basics, tickets,
-- notification read state, funnel events). Money, status, license and catalog writes go through server functions.

do $$
declare
  t record;
begin
  for t in select tablename from pg_tables where schemaname = 'public' loop
    execute format('alter table public.%I enable row level security', t.tablename);
    execute format('alter table public.%I force row level security', t.tablename);
  end loop;
end;
$$;

-- ---------------------------------------------------------------- reference data (public)
create policy provinces_read on public.provinces for select to anon, authenticated using (true);
create policy platform_settings_read on public.platform_settings for select to anon, authenticated using (true);
create policy rate_cards_read on public.delivery_rate_cards for select to anon, authenticated using (true);

-- ---------------------------------------------------------------- identity
create policy profiles_read on public.profiles for select to authenticated using (
  user_id = (select auth.uid())
  or (select private.is_platform_admin())
  or exists (
    select 1 from public.user_roles ur
    where ur.user_id = profiles.user_id and ur.province_id in (select private.operator_province_ids())
  )
);
create policy profiles_update_own on public.profiles for update to authenticated
  using (user_id = (select auth.uid()))
  with check (
    user_id = (select auth.uid())
    and (
      default_address_id is null
      or exists (select 1 from public.addresses a where a.id = default_address_id and a.user_id = (select auth.uid()))
    )
  );

create policy user_roles_read on public.user_roles for select to authenticated using (
  user_id = (select auth.uid())
  or (select private.is_platform_admin())
  or province_id in (select private.operator_province_ids())
);
-- Supabase Auth reads roles for the access-token hook.
create policy user_roles_auth_hook on public.user_roles for select to supabase_auth_admin using (true);

create policy addresses_own on public.addresses for all to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

-- ---------------------------------------------------------------- licensing & territory
create policy operator_applications_read on public.operator_applications for select to authenticated using (
  applicant_user_id = (select auth.uid()) or (select private.is_platform_admin())
);
create policy province_licenses_read on public.province_licenses for select to authenticated using (
  operator_user_id = (select auth.uid())
  or (select private.is_platform_admin())
  or province_id in (select private.operator_province_ids())
);
create policy delivery_zones_read on public.delivery_zones for select to authenticated using (
  (select private.is_platform_admin()) or province_id in (select private.operator_province_ids())
);

-- ---------------------------------------------------------------- stores & catalog
-- Public storefront: active/paused stores in a serviceable province. Members, operators and admins see all.
create policy stores_public_read on public.stores for select to anon, authenticated using (
  status in ('ACTIVE', 'PAUSED') and province_id in (select private.serviceable_province_ids())
);
create policy stores_staff_read on public.stores for select to authenticated using (
  id in (select private.member_store_ids())
  or province_id in (select private.operator_province_ids())
  or (select private.is_platform_admin())
);

-- Catalog rows are visible whenever their store is (the stores policies apply inside the subquery).
create policy product_categories_read on public.product_categories for select to anon, authenticated using (
  exists (select 1 from public.stores s where s.id = product_categories.store_id)
);
create policy products_read on public.products for select to anon, authenticated using (
  exists (select 1 from public.stores s where s.id = products.store_id)
);

create policy promos_public_read on public.promos for select to anon, authenticated using (
  is_active and now() between starts_at and ends_at
);
create policy promos_staff_read on public.promos for select to authenticated using (
  (select private.is_platform_admin())
  or province_id in (select private.operator_province_ids())
  or store_id in (select private.member_store_ids())
);

create policy favorites_own on public.favorites for all to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

-- ---------------------------------------------------------------- riders
create policy riders_read on public.riders for select to authenticated using (
  user_id = (select auth.uid())
  or province_id in (select private.operator_province_ids())
  or (select private.is_platform_admin())
);
create policy rider_documents_read on public.rider_documents for select to authenticated using (
  rider_id = (select private.current_rider_id())
  or province_id in (select private.operator_province_ids())
  or (select private.is_platform_admin())
);
create policy rider_locations_read on public.rider_locations for select to authenticated using (
  rider_id = (select private.current_rider_id())
  or province_id in (select private.operator_province_ids())
  or (select private.is_platform_admin())
);

-- ---------------------------------------------------------------- orders
create policy quotes_read on public.quotes for select to authenticated using (
  customer_id = (select auth.uid()) or (select private.is_platform_admin())
);
create policy orders_read on public.orders for select to authenticated using (
  customer_id = (select auth.uid())
  or store_id in (select private.member_store_ids())
  or (rider_id is not null and rider_id = (select private.current_rider_id()))
  or province_id in (select private.operator_province_ids())
  or (select private.is_platform_admin())
);
-- Children follow their order's visibility.
create policy order_items_read on public.order_items for select to authenticated using (
  exists (select 1 from public.orders o where o.id = order_items.order_id)
);
create policy order_events_read on public.order_events for select to authenticated using (
  exists (select 1 from public.orders o where o.id = order_events.order_id)
);
create policy proof_of_delivery_read on public.proof_of_delivery for select to authenticated using (
  exists (select 1 from public.orders o where o.id = proof_of_delivery.order_id)
);
create policy order_offers_read on public.order_offers for select to authenticated using (
  rider_id = (select private.current_rider_id())
  or province_id in (select private.operator_province_ids())
  or (select private.is_platform_admin())
);
create policy reviews_read on public.reviews for select to anon, authenticated using (true);

-- ---------------------------------------------------------------- money
create policy payments_read on public.payments for select to authenticated using (
  payer_user_id = (select auth.uid())
  or province_id in (select private.operator_province_ids())
  or (select private.is_platform_admin())
);
create policy webhook_events_read on public.webhook_events for select to authenticated using (
  (select private.is_platform_admin())
);
create policy wallets_read on public.wallets for select to authenticated using (
  id in (select private.visible_wallet_ids())
);
create policy ledger_entries_read on public.ledger_entries for select to authenticated using (
  wallet_id in (select private.visible_wallet_ids())
);
create policy ledger_txns_read on public.ledger_txns for select to authenticated using (
  (select private.is_platform_admin())
  or province_id in (select private.operator_province_ids())
  or exists (select 1 from public.ledger_entries e where e.txn_id = ledger_txns.id)
);
create policy topups_read on public.topups for select to authenticated using (
  store_id in (select private.member_store_ids())
  or province_id in (select private.operator_province_ids())
  or (select private.is_platform_admin())
);
create policy remittances_read on public.remittances for select to authenticated using (
  rider_id = (select private.current_rider_id())
  or province_id in (select private.operator_province_ids())
  or (select private.is_platform_admin())
);
create policy settlement_batches_read on public.settlement_batches for select to authenticated using (
  province_id in (select private.operator_province_ids()) or (select private.is_platform_admin())
);
create policy settlement_lines_read on public.settlement_lines for select to authenticated using (
  province_id in (select private.operator_province_ids())
  or (select private.is_platform_admin())
  or store_id in (select private.member_store_ids())
  or rider_id = (select private.current_rider_id())
);
create policy payouts_read on public.payouts for select to authenticated using (
  exists (select 1 from public.settlement_lines l where l.id = payouts.settlement_line_id)
);

-- ---------------------------------------------------------------- support, notifications, audit
create policy support_tickets_read on public.support_tickets for select to authenticated using (
  created_by = (select auth.uid())
  or province_id in (select private.operator_province_ids())
  or (select private.is_platform_admin())
);
create policy support_tickets_create on public.support_tickets for insert to authenticated with check (
  created_by = (select auth.uid())
  and status = 'OPEN'
  and assigned_to is null
  and private.has_role(requester_role)
  and (order_id is null or exists (select 1 from public.orders o where o.id = support_tickets.order_id))
);
create policy ticket_messages_read on public.ticket_messages for select to authenticated using (
  exists (select 1 from public.support_tickets t where t.id = ticket_messages.ticket_id)
);
create policy ticket_messages_create on public.ticket_messages for insert to authenticated with check (
  sender_id = (select auth.uid())
  and exists (select 1 from public.support_tickets t where t.id = ticket_messages.ticket_id)
);
create policy disputes_read on public.disputes for select to authenticated using (
  opened_by = (select auth.uid())
  or exists (select 1 from public.orders o where o.id = disputes.order_id and o.customer_id = (select auth.uid()))
  or province_id in (select private.operator_province_ids())
  or (select private.is_platform_admin())
);
create policy notifications_own on public.notifications for select to authenticated using (
  user_id = (select auth.uid())
);
create policy notifications_mark_read on public.notifications for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));
create policy device_tokens_own on public.device_tokens for all to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));
create policy audit_logs_read on public.audit_logs for select to authenticated using (
  (select private.is_platform_admin()) or province_id in (select private.operator_province_ids())
);
create policy app_events_insert on public.app_events for insert to authenticated with check (
  user_id = (select auth.uid())
);
