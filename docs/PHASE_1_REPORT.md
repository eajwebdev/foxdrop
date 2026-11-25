# Phase 1 report — Database, auth, roles

Date: 2026-11-25 · Branch: `phase-1` (merged into `main` locally; no GitHub remote yet)

## What was built
- **Database** (`supabase/migrations`, 15 files): every §8 table, PostGIS, enums for every status in §5.
  - RLS is enabled **and forced** on every table. Province isolation goes through `private.*` SECURITY DEFINER
    helpers that read `user_roles` live.
  - Clients can write only personal data (addresses, favorites, device tokens, profile basics, tickets, read state,
    funnel events). Money, status, license, role and catalog tables have no client write grant.
  - The ledger is append-only (UPDATE, DELETE and TRUNCATE all raise). Every transaction sums to zero, checked by a
    deferred constraint trigger. Wallet balances change only through the ledger.
  - One live license per province, enforced by a unique partial index.
  - Secret columns (`orders.delivery_otp` / `pickup_code`, `riders.license_no_encrypted`) are not client-readable.
  - PII is encrypted with a Vault key. There are 5 storage buckets, with own-folder rules for KYC documents.
- **Roles in the JWT:** `custom_access_token_hook` adds `app_roles` to every token.
- **Seed:** 83 PSGC provinces. Demo Negros Occidental: an ACTIVE license, 1 operator, 3 stores with products,
  3 riders, 2 customers and 1 super admin. A second province (Iloilo) appears only inside the isolation test.
- **pgTAP tests** (`supabase/tests/database`, 43 assertions):
  - province A ⇄ B isolation across orders, stores, riders, wallets, licenses and roles
  - license exclusivity
  - client writes rejected on money and status tables
  - ledger rows immutable, and an unbalanced transaction rejected
  - secret columns hidden
- **Apps:**
  - **Role routing** (`RoleGate`):
    - Customer → customer home. Rider → rider home or "not registered yet". Business → operator home or store
      home, or "apply first".
    - Admin accounts get "use the web dashboard" in every app.
    - Routing reads the token claim, so it works offline.
    - The gate has a loading skeleton, plus an error screen with retry and sign out.
  - **Google sign-in:** Credential Manager with a SHA-256 nonce, then a Supabase ID-token sign-in. It turns on when
    `GOOGLE_WEB_CLIENT_ID` is set.
  - **SMS code autofill:** uses SMS User Consent (no SMS permission).
  - **Auth redirects:** one scheme per app (`foxdrop-<app>://auth`), handled with **PKCE**.

## How to test
1. Install Docker Desktop and the Supabase CLI (docs/OWNER_TODO.md), then run `supabase start`,
   `supabase db reset` (migrations + seed) and `supabase test db`.
2. Put the local URL and anon key in `local.properties` and install the three apps (`.\gradlew :app-customer:installDebug` …).
3. Sign in with the demo numbers (the code is `123456` locally). Each account should land on its home:
   - 0001 / 0002: customer
   - 0003–0005: rider
   - 0006: operator (business app)
   - 0007–0009: store owners (business app)
4. Sign in to the rider app with a customer number: it should show "not registered yet" with a sign-out button.

## Quality Gate (§19)
- [x] `./gradlew check` is green:
  - Spotless/ktlint, detekt + Compose rules, Android Lint (warnings as errors)
  - 105 unit/ViewModel/screenshot tests, 0 failures
  - Kover merged coverage of domain/data/common/model: **85.8 %** (gate 80 %)
- [x] `./gradlew verifyRoborazziDebug` is green. New baselines: role gate (loading, failed, 3 mismatch screens), and
      the store and operator homes.
- [ ] **pgTAP (`supabase test db`) and `supabase db lint`: not verified.** Docker and the Supabase CLI are not
      installed on this PC. Every migration, the seed and the 3 test files were parsed with libpg_query (the
      PostgreSQL 17 parser, including PL/pgSQL bodies): all parse. They have **not** run against a database.
- [ ] Deno tests: none yet (the first Edge Functions arrive in Phase 2/3).
- [ ] Phase acceptance checks:
  - [ ] pgTAP shows cross-province reads return 0 rows and a second live license fails: written, **not run**
        (waiting for Docker).
  - [x] Each demo user lands on the correct home: covered by `ResolveHomeUseCaseTest`, which mirrors the seed accounts,
        and `RoleGateTest`. Not yet verified end to end against a live backend.
- [x] Every new screen has a loading skeleton, empty/mismatch and error + retry states, dark mode, previews,
      screenshot tests, and a FoxMotion fade that respects "Remove animations".
- [x] Reviews:
  - `/code-review high`: 10 findings, all fixed.
  - `/simplify`: 4 reviewers. Fixes applied; skipped items are listed below.
  - `/security-review`: 1 candidate (deep-link session injection), scored 7/10, under the report bar of 8.
    **Fixed anyway** (PKCE).
- [x] No secrets committed: test OTPs and the demo admin password are local-dev only (DECISIONS 32). No money is
      calculated on the device.
- [x] Docs updated: PROGRESS, DECISIONS 27–35, CHANGELOG, plan.
- [x] Every check above was run in this phase, except those marked "not verified".

## Not verified (and why)
- **Migrations, seed and pgTAP against a real Postgres:** no Docker on this PC. The first thing to do once Docker
  is installed is `supabase db reset && supabase test db`, and to fix whatever it reports before Phase 2 builds on
  this schema.
- **Real sign-in on a device** (OTP, email, Google, redirects): needs the local stack or a hosted project, plus a
  Google OAuth client (OWNER_TODO).
- **CI workflows:** no GitHub remote yet.

## Review items deliberately left for later
- **Secret columns:** `orders` and `riders` hide them with column-level grants, so a new column must also be added
  to the grants list. Moving the secrets to side tables (`order_secrets`, `rider_private`) is cleaner; reconsider
  in Phase 3, when orders are first written.
- **Hot global wallets:** `PLATFORM_REVENUE`, `GATEWAY_CLEARING` and `CUSTOMER_REFUND` are single rows that every
  posting would lock. Make them per-province, or derive their balances, in Phase 5 before any posting function
  exists.
- **`support_tickets.province_id` is client-supplied** (insert policy). Derive it from the order or role in a
  trigger when the support screens arrive (Phase 6). Today a wrong value only misroutes a ticket; it exposes no
  data.
- **Placeholder homes:** the store, operator, customer and rider homes repeat an "Account" tab and a sign-out
  string. They will be replaced by real screens in Phases 2–4.
- **Session expiry** is handled in `RoleGate`. Move it to one global handler when more screens call the backend
  (Phase 2).
- **Debug and release builds share an auth scheme**, so a phone with both installed shows a chooser. Verified App
  Links replace custom schemes in Phase 7.
- **Not changed, with reasons:**
  - The ledger balance check stays per entry. A per-transaction trigger would miss entries added to an older
    transaction.
  - There is no single `can_read_province(p)` policy helper. It would turn today's hashed sub-plans into per-row
    function calls.
  - pgTAP keeps the literal JWT-claim lines. A `pg_temp` login helper's privileges under `set role` cannot be
    checked without Docker.
