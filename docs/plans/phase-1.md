# Phase 1 plan — Database, auth, roles

Source: MASTER_PROMPT.md §13 Phase 1, §5, §7, §8, §17 (backend), §3 (auth).

## Constraints right now
- Docker / Supabase CLI are not installed (docs/OWNER_TODO.md). Migrations, seed and pgTAP tests are written now
  and verified as soon as they are; the phase report lists them as "waiting for owner" until then.
- Android work that needs no backend (role routing, Credential Manager wiring, deep links) is unit/screenshot tested.

## Database (supabase/migrations, one concern per file, timestamped names)
| # | File | Contents |
|---|---|---|
| 01 | `extensions` | postgis, pgcrypto (schema `extensions`) |
| 02 | `foundation` | enums (`app_role`, all status enums from §5), `set_updated_at()` trigger fn |
| 03 | `provinces_settings` | `provinces` (PSGC), `platform_settings` (single row) |
| 04 | `identity` | `profiles`, `user_roles`, `addresses`; `handle_new_user` trigger (profile + customer role) |
| 05 | `auth_helpers` | `custom_access_token_hook` (adds `app_roles` claim), RLS helpers: `has_role`, `is_platform_admin`, `operator_province_ids`, … all `SECURITY DEFINER SET search_path = ''` |
| 06 | `licensing` | `operator_applications`, `province_licenses` + unique partial index (one live license per province) |
| 07 | `zones_rates` | `delivery_zones` (geography polygon), `delivery_rate_cards` (versioned) |
| 08 | `stores_catalog` | `stores`, `store_staff`, `product_categories`, `products`, `promos`, `favorites` |
| 09 | `riders` | `riders`, `rider_documents`, `rider_locations` |
| 10 | `orders` | `quotes`, `orders`, `order_items`, `order_events`, `order_offers`, `proof_of_delivery`, `reviews` |
| 11 | `money` | `payments`, `webhook_events`, `wallets`, `ledger_txns`, `ledger_entries` (append-only + zero-sum deferred trigger), `topups`, `remittances`, `settlement_batches`, `settlement_lines`, `payouts` |
| 12 | `support_audit` | `support_tickets`, `ticket_messages`, `disputes`, `notifications`, `device_tokens`, `audit_logs`, `app_events` |
| 13 | `rls` | RLS enabled **and forced** on every table; deny by default; policies per §8 RLS summary |
| 14 | `grants` | clients get SELECT only where RLS allows; **no INSERT/UPDATE/DELETE** on money, ledger, order-status, license, wallet tables (writes come later through RPC/Edge Functions) |
| 15 | `storage` | buckets `store-media`, `product-media` (public), `kyc-docs`, `pod`, `receipts` (private) + policies |

Indexes: GIST on every geography column, `(province_id, status, created_at)` on orders, `(rider_id, status)`.

## Seed (supabase/seed.sql)
All 82 provinces + NCR (PSGC codes), `platform_settings` defaults (§14), demo data: Negros Occidental ACTIVE license,
1 operator, 3 stores with products, 3 riders, 2 customers, 1 super admin (fixed UUIDs, phone `+63 917 000 000x`).
Test OTPs move from `config.jsonc` into a **local-only** `[auth.sms.test_otp]` override (DECISIONS 25).

## pgTAP (supabase/tests/database)
- operator of Province A reads 0 rows of Province B: orders, stores, riders, wallets (+ ledger, licenses)
- second live license for the same province fails; a new one after EXPIRED succeeds
- authenticated users cannot write money/status tables directly; ledger rows cannot be updated/deleted;
  a ledger txn that does not sum to zero is rejected
- custom access token hook adds the user's roles; customers see only their own orders/addresses

## Android
- `RoleRepository` (domain) reading the signed-in user's roles from `user_roles` (RLS: own rows).
- Role routing: business app → Store home / Operator home / "Apply" placeholder; rider app → rider home or
  "register as rider" (Phase 4); Super Admin → "use the web dashboard".
- Per-app auth redirect schemes (`foxdrop-customer://auth` …) + intent-filters + `handleDeeplinks`.
- Google sign-in via Credential Manager → `signInWith(IDToken)`; enabled when `GOOGLE_WEB_CLIENT_ID` is set,
  otherwise the current "coming soon" message stays.
- SMS User Consent autofill for the OTP field.

## Acceptance (§13)
- pgTAP: cross-province reads return 0 rows · unique live license enforced · each demo user lands on the right home.
