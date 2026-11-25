# FoxDrop — MASTER BUILD PROMPT (Native Android: Kotlin + Jetpack Compose · Super Admin Web: Laravel + React)

> **How to use:** Give this whole file to your AI coding agent (Claude Code or similar) from inside this folder.
> Say: *"Read MASTER_PROMPT.md and the images in /reference, then build FoxDrop following it phase by phase."*
> The agent must finish one phase, pass its acceptance checks, commit, and only then start the next phase.
> **To resume in a new session:** *"Read CLAUDE.md, MASTER_PROMPT.md and docs/PROGRESS.md, then continue from the current phase."*

**Section map:** §0 rules · §1 business · §2 apps · §3 tech stack · §4 repo structure · §5 roles & status machines · §6 delivery fee · §7 ledger & money · §8 data model · §9 screens per app · §10 cross-cutting · §11 branding · §11A premium UI & motion · §12 Edge Functions & cron · §13 phases & acceptance checks · §14 defaults · §15 definition of done · §16 engineering standards · §17 security · §18 workflow & tools · §19 Quality Gate · §20 Super Admin web.

**If two parts of this file disagree:** `reference/` images win on business flow; §7 wins on money; §17 wins on security; §14 wins on default numbers; otherwise the more specific section wins. Record the conflict and the choice in `docs/DECISIONS.md`.

---

## 0. ROLE AND RULES FOR THE AI AGENT

You are a senior Android + backend engineer. Build **FoxDrop**, a **province-exclusive delivery platform for the Philippines**, as native Android apps in **Kotlin + Jetpack Compose** with a **Supabase** backend.

Hard rules:
1. **Read every image in `/reference` first.** They are the business source of truth:
   - `reference/FoxDrop_Province_Delivery_Workflow.png` — whole business model.
   - `reference/workflows/01_Customer_Workflow.png` — customer 8 steps.
   - `reference/workflows/02_Super_Admin_Workflow.png` — platform owner 7 modules + support/disputes.
   - `reference/workflows/03_Provincial_Operator_Workflow.png` — franchisee 8 steps + 8 modules.
   - `reference/workflows/04_Rider_Workflow.png` — rider 8 steps.
   - `reference/logos/*.png` — 4 logo concepts (see §11 Branding).
2. Work **phase by phase** (§13). Never skip acceptance checks. If a check fails, fix it before moving on.
3. **Money is always `Long` centavos** (₱1.00 = 100). Never `Double`/`Float` for money. All balances come from a ledger (§7), never from a single mutable number edited by the app.
4. **All money moves and status changes happen on the server** (Postgres functions / Edge Functions). Android apps only *request* them. Never trust the client for prices, fees, distance, or wallet balances.
5. **Every row that belongs to a province has `province_id`**, and Row Level Security (RLS) must stop an operator, store, or rider from seeing another province's data.
6. Keep code compiling at all times. Write tests as you go. Commit after each phase with a clear message.
7. The **Super Admin web dashboard** is Laravel + Inertia + React/TS in `admin-web/` (§20). Follow the **premium UI/UX & motion spec (§11A)**, **engineering standards (§16)**, **security hardening (§17)**, and pass the **Quality Gate (§19)** at the end of every phase.
8. When something is unclear, choose the **default listed in §14 (Assumptions)**, write the choice in `docs/DECISIONS.md`, and continue. Do not stop to ask.
9. Never put secret keys in the Android app. Only the Supabase **anon** key and Google Maps key (restricted to the app's SHA-1) go in the app. PayMongo secret key lives only in Edge Function secrets.
10. **Never block on missing accounts or keys, and never invent them.** Develop against the **local Supabase stack** (`supabase start`), PayMongo **test mode**, and fakes/mocks for PayMongo, Routes API, FCM and SMS (local auth uses fixed test OTPs). When a real key or an owner action is needed (create an account, enable an API, pay for something, upload to Play), add it to `docs/OWNER_TODO.md` with exact steps, keep building everything that does not depend on it, and mark the affected acceptance check as "waiting for owner" in the phase report. Stop only when nothing else in the phase can move forward.
11. **Keep `docs/PROGRESS.md` up to date** (current phase, what is done, what is next, open blockers) at the end of every working session and every phase. A new session reads it first and resumes from there; it never restarts a finished phase.
12. **Do not claim a check passed unless you ran it and saw it pass.** Phase reports show the real command output; anything not run (no emulator, no key) is listed as not verified.

---

## 1. BUSINESS SUMMARY (from the reference images)

| Party | What they do | How money flows |
|---|---|---|
| **Platform Owner / Super Admin (FoxDrop — the app owner)** | Owns the system, sells province licenses, approves/oversees everything across all provinces, handles disputes, sets global defaults. | Receives **₱15,000 / year per province license**. Optional platform commission (default 0%). |
| **Provincial Operator (Franchisee)** | **Exactly ONE per province.** Applies, pays ₱15,000/yr, activates province, sets coverage zones and **delivery rate per KM**, recruits/approves stores and riders, handles store **top-up wallets**, collects rider COD remittances, monitors orders, renews yearly. | Earns **store fees (default ₱15 / store / month, configurable)** + operator share of delivery fees. |
| **Store / Merchant** | Registers, gets approved by operator, manages products/catalog, accepts orders, prepares items, keeps a **top-up wallet** for fees. | Receives order proceeds minus applicable fees. Monthly store fee deducted from top-up wallet. |
| **Rider** | Registers with documents (ID, driver's license, vehicle), gets approved, goes online, accepts jobs, picks up, delivers, takes **Proof of Delivery (POD)**, collects cash if COD, **remits cash daily**. | Earns delivery earnings based on **per-KM rate**. |
| **Customer** | Signs up, browses nearby stores, adds to cart, chooses **Delivery or Pick Up**, pays **Online (GCash, Maya, Card)** or **COD**, tracks rider live, receives, rates store + rider. | Pays item subtotal + delivery fee. |

Two order payment flows (from main infographic):
- **A. Online e-payment:** Customer pays → gateway verifies transaction → store accepts → rider assigned & delivers → completed.
- **B. Cash on Delivery:** Store accepts → rider delivers & collects cash → POD + cash collection log + daily remittance tracking → completed.

Settlement & reporting: Merchant gets proceeds (less fees), Rider gets delivery earnings (per KM), Operator earns store fees + delivery ops share, Platform gets license fee.

---

## 2. APPS TO BUILD (one repo: 3 Android app modules + 1 web dashboard)

| App module | Package | Users | Notes |
|---|---|---|---|
| `:app-customer` | `ph.foxdrop.customer` | Customer | Public Play Store app "FoxDrop". |
| `:app-rider` | `ph.foxdrop.rider` | Rider | "FoxDrop Rider". Foreground location service. |
| `:app-business` | `ph.foxdrop.business` | Store/Merchant, Provincial Operator | "FoxDrop Business". After login, route by role to a different home. **Adaptive layout**: phone = bottom bar; tablet/landscape (≥ 840dp) = navigation rail/drawer like the dashboards in the images. |
| `admin-web/` (not Android) | — | **Super Admin + admin staff** | **FoxDrop Admin web dashboard: Laravel + Inertia.js + React + TypeScript** (React lives in `admin-web/resources/js`). Full spec in **§20**. |

Why this split: customer and rider have very different permissions (background location) and Play Store listings; store and operator share a lot of dashboard/report UI, so they share one app with role-based navigation. The **Super Admin works on a big screen with large tables, exports and settings**, so it is a **web dashboard**, connected to the **same Supabase Postgres database** (one database, one set of money rules). Super Admin accounts cannot log in to the Android apps.

---

## 3. TECH STACK (use these exact choices)

**Android**
- Latest **stable** Kotlin 2.x, AGP, Gradle and Compose BOM that work together (check the official compatibility tables at the start of Phase 0 instead of trusting memory; no alpha/beta/RC), the JDK that AGP version requires (17 or newer), Gradle Version Catalog (`libs.versions.toml`) with every version pinned, minSdk 26, target/compile latest stable. Record the chosen versions in `docs/DECISIONS.md`.
- Jetpack Compose + Material 3 (+ `material3-adaptive` for the business app), Compose Navigation with **type-safe routes** (kotlinx.serialization).
- Architecture: **Clean Architecture + MVI** (full rules in §16): `State` data class + `Action` sealed interface + one-off `Effect` via `Channel`, pure reducer, `ViewModel` exposing `StateFlow`. Repositories in `:core:data` (offline-first, single source of truth). Use cases in `:core:domain`.
- DI: **Hilt**. Async: Coroutines + Flow. Serialization: kotlinx.serialization.
- Backend client: **supabase-kt** (`auth-kt`, `postgrest-kt`, `realtime-kt`, `storage-kt`, `functions-kt`) with Ktor OkHttp engine.
- Local cache: **Room** (cart, recent orders, offline rider job queue) + **DataStore** (settings, session).
- Images: Coil 3. Maps: **Google Maps Compose** (`maps-compose`) + Fused Location Provider. Navigation hand-off: intent to Google Maps / Waze.
- Camera for POD & documents: CameraX (or `ActivityResultContracts.TakePicture`) + image compression before upload.
- Push: **Firebase Cloud Messaging** (only FCM; data lives in Supabase).
- Background: WorkManager (retry uploads, remittance sync), Foreground Service type `location` for online riders.
- Payments UI: open PayMongo checkout URL in **Chrome Custom Tabs**, return by deep link `foxdrop://payment/result`, then confirm status from server (never trust the redirect alone).
- Testing: **JUnit4** (needed by Compose UI tests, Robolectric and Roborazzi), Turbine, MockK, Compose UI tests, Robolectric where useful.
- Quality: Spotless + ktlint, detekt, Android Lint (details in §16); `./gradlew check` must pass with zero errors.

**Backend: Supabase**
- Postgres (the version the current Supabase CLI ships) + **PostGIS** (distance, zones), RLS on every table. Local development uses the Supabase CLI stack (`supabase start`, needs Docker); `supabase/config.toml` is committed.
- Supabase Auth: **phone OTP (+63)** as primary, email/password, Google sign-in (customer screen shows Google + Facebook; implement Google now, Facebook behind a feature flag).
- SMS for OTP: production needs an SMS provider (a Supabase built-in provider such as Twilio, or a PH provider through the Auth **Send SMS hook**). Wrap it so it can be changed; local/dev uses fixed test OTPs from `config.toml`. Email (license reminders, admin exports): any SMTP provider, configured by env only.
- Supabase Realtime: order status changes + rider live location (Broadcast channel per order).
- Supabase Storage buckets: `store-media` (public), `product-media` (public), `kyc-docs` (private), `pod` (private), `receipts` (private).
- Edge Functions (TypeScript/Deno) for: payments (PayMongo), webhooks, distance/quote, dispatch, scheduled jobs (pg_cron), report export, FCM sends.
- **Payment gateway: PayMongo** (GCash, Maya, cards; supports PH). Wrap behind a `PaymentProvider` interface so Xendit can be added later.
- Distance: Google **Routes API** (road distance) called from an Edge Function; fallback to PostGIS straight-line × 1.3 if API fails.

---

## 4. REPOSITORY STRUCTURE

```
foxdrop/
├─ CLAUDE.md  MASTER_PROMPT.md  README.md  CHANGELOG.md  local.properties.example
├─ .github/workflows/      (CI: android, backend, admin-web — see §16)
├─ build-logic/            (Gradle convention plugins — see §16)
├─ gradle/libs.versions.toml
├─ app-customer/            app-rider/            app-business/
├─ baselineprofile/        (Macrobenchmark + Baseline Profile generator)
├─ .maestro/               (E2E flows)
├─ admin-web/              (Laravel + Inertia + React/TS Super Admin dashboard — see §20)
├─ core/
│  ├─ designsystem/   (theme, colors, typography, FoxDrop components)
│  ├─ model/          (pure Kotlin domain models, Money, enums)
│  ├─ network/        (Supabase client, DTOs, mappers)
│  ├─ data/           (repositories)
│  ├─ database/       (Room)
│  ├─ datastore/
│  ├─ location/       (location client, permission helpers)
│  ├─ notifications/  (FCM, channels)
│  ├─ payments/       (PaymentProvider, custom tabs, deep link)
│  ├─ media/          (camera, compression, upload)
│  ├─ common/         (Result, dispatchers, time = Asia/Manila, PHP formatter)
│  └─ testing/
├─ feature/
│  ├─ auth/  onboarding/
│  ├─ customer-home/ store/ cart/ checkout/ tracking/ orders/ reviews/ profile/
│  ├─ rider-onboarding/ rider-jobs/ rider-delivery/ rider-earnings/ rider-cash/
│  ├─ store-dashboard/ store-orders/ store-catalog/ store-wallet/ store-profile/
│  ├─ operator-dashboard/ operator-stores/ operator-riders/ operator-zones-rates/
│  │  operator-wallets/ operator-remittance/ operator-orders/ operator-reports/ operator-license/ operator-settings/
│  └─ support/ (tickets/chat shared by all roles)
├─ supabase/
│  ├─ config.toml
│  ├─ migrations/   (numbered SQL files)
│  ├─ seed.sql      (PSGC provinces, demo data)
│  ├─ functions/    (edge functions)
│  └─ tests/        (pgTAP tests for RLS + money functions)
├─ docs/  (PROGRESS.md, OWNER_TODO.md, DECISIONS.md, ARCHITECTURE.md, API.md, RUNBOOK.md, TEST_PLAN.md,
│          PHASE_X_REPORT.md, plans/, adr/)
└─ reference/ (images given by the owner — do not modify)
```
There are **no `feature/admin-*` Android modules**: every Super Admin screen lives in `admin-web/` (§20).

---

## 5. ROLES, PERMISSIONS, AND STATUS MACHINES

Roles (`app_role` enum): `customer`, `rider`, `store_owner`, `store_staff`, `operator`, `operator_staff`, `super_admin`, `admin_staff`.
A user can have several roles (`user_roles` table with optional `province_id` / `store_id` scope). JWT custom claims hook adds `roles` for RLS helpers: `auth_has_role(role, province_id)`.

### 5.1 Province license
`AVAILABLE → APPLIED → PENDING_PAYMENT → ACTIVE → GRACE (15 days after expiry) → EXPIRED` (+ `SUSPENDED`, `REJECTED`).
- **Exclusivity:** unique partial index: only one license per `province_id` with status in (`APPLIED`,`PENDING_PAYMENT`,`ACTIVE`,`GRACE`). Customer sees only provinces with ACTIVE/GRACE license.
- Fee from `platform_settings.license_fee_centavos` (default 1,500,000 = ₱15,000), term 365 days.
- Renewal reminders: 30, 14, 7, 1 days before expiry (push + email). In GRACE: operations continue, banner shown. EXPIRED: province hidden from customers, operator read-only; admin can re-assign province.

### 5.2 Store
`DRAFT → SUBMITTED → APPROVED → ACTIVE ⇄ PAUSED (store closed/opening hours) → SUSPENDED (by operator/admin or unpaid fee) → CLOSED`.
Monthly store fee (`provinces.store_fee_centavos`, default 1,500 = ₱15) is **deducted from store top-up wallet on the 1st of each month (Asia/Manila)** by pg_cron. If balance insufficient → wallet may go negative up to `-store_fee`, store gets warnings; after `store_fee_grace_days` (default 7) unpaid → `SUSPENDED` automatically; auto-restored after top-up.

### 5.3 Rider
`APPLIED → DOCS_REVIEW → APPROVED → (OFFLINE ⇄ ONLINE ⇄ ON_JOB) → SUSPENDED/DEACTIVATED`.
Documents: personal details, valid ID, driver's license (+expiry), vehicle details (type, plate, OR/CR photo), selfie. Rider blocked from new COD jobs when `cash_on_hand > provinces.rider_cash_limit_centavos` (default ₱5,000) or has unremitted cash older than 24 h.

### 5.4 Order (most important — implement as a server-side state machine function `order_transition(order_id, to_status, payload)` that validates who may do each step)
```
PENDING_PAYMENT (online only) ─pay ok─▶ PLACED
                └─timeout 15 min / failed─▶ CANCELLED
PLACED (COD placed directly here) ─store accepts─▶ ACCEPTED ─▶ PREPARING ─▶ READY_FOR_PICKUP
   └─store rejects / 5 min no answer ─▶ CANCELLED (auto refund if online)
DELIVERY orders:   ACCEPTED/PREPARING/READY_FOR_PICKUP ─rider assigned─▶ RIDER_ASSIGNED ─▶ RIDER_AT_STORE ─▶ PICKED_UP ─▶ ARRIVING ─▶ DELIVERED (needs POD) ─▶ COMPLETED
PICK-UP orders:    READY_FOR_PICKUP ─customer collects (store enters 4-digit code)─▶ COMPLETED
Any active ─▶ CANCELLED (rules per role) | DISPUTED (customer/admin opens dispute)
```
Dispatch starts at ACCEPTED (§5.5), so a rider can be assigned while the store is still preparing. The store's own progress is kept in milestone timestamps (`preparing_at`, `ready_at`) and does not depend on the rider steps; `PICKED_UP` is only allowed after `ready_at` is set.
Every transition writes `order_events` (who, when, from, to, lat/lng, note). Customer timeline (Order Placed, Preparing, On the Way, Arriving Soon) maps from these.

### 5.5 Rider assignment (dispatch)
- When order becomes ACCEPTED (delivery type): Edge Function `dispatch` finds ONLINE, approved, same-province riders within `dispatch_radius_km` (default 5 km, then 8, then 12) ordered by distance, not over cash limit; sends offer to the nearest one (push + realtime) with **30-second** accept window showing store distance, customer distance, and **estimated earnings** (like rider image step 3). Decline/timeout → next rider. After 3 rounds no taker → alert operator to **manual assign** (operator image: "Assign and track deliveries").
- Operator can always manually assign/re-assign.
- Use `SELECT … FOR UPDATE SKIP LOCKED` so two riders can't accept the same order.

---

## 6. DELIVERY FEE AND EARNINGS FORMULA

Per province (`delivery_rate_cards`, editable by operator, versioned — orders store a snapshot):
```
road_km        = Routes API distance store → customer (rounded up to 0.1 km)
delivery_fee   = max(min_fee, base_fare + ceil_to_0.1(road_km) × rate_per_km) + surcharges
                 (optional: night surcharge, bad-weather toggle, extra per additional stop — all default 0)
rider_earning  = delivery_fee × rider_share_pct        (default 80%)
operator_cut   = delivery_fee − rider_earning           (default 20%)
platform_comm  = item_subtotal × platform_commission_pct (default 0%, admin-configurable)
operator_comm  = item_subtotal × operator_commission_pct (default 0%, operator-configurable within admin cap)
store_net      = item_subtotal − platform_comm − operator_comm
total          = item_subtotal + delivery_fee − discount   (what the customer pays)
```
All percentages are computed in integer centavos, rounded half-up; any rounding remainder goes to the operator so the splits always add up exactly. A promo `discount` is paid by whoever owns the promo (store promo → taken from `store_net`; province promo → operator; platform promo → platform), see §7.
Defaults (seed): `base_fare ₱40` covers first 2 km, `rate_per_km ₱10` after that, `min_fee ₱40`, `max_distance_km 15`. (Example in the customer image: Delivery Fee ₱40.)
- Delivery zones: operator draws/picks coverage polygons (PostGIS `geography(Polygon)`) or radius; customer address outside all zones → "Not yet available in your area".
- Quote is calculated by Edge Function `quote` and returned with a `quote_id` valid 10 min; `place_order` uses the quote, never client numbers.
- Pick-up orders: delivery_fee = 0.

---

## 7. WALLETS, LEDGER, COD, REMITTANCE, SETTLEMENT

Use a **double-entry ledger**. Tables: `wallets(id, owner_type, owner_id, province_id, kind, currency='PHP')`, `ledger_entries(id, txn_id, wallet_id, amount_centavos (+credit/−debit), type, ref_type, ref_id, created_at)`, `ledger_txns(id, type, idempotency_key UNIQUE, created_by, memo)`. Every txn's entries **sum to zero**. Balances = `SUM(amount)` (cache in a materialized/maintained column updated by trigger only).

Wallet kinds:
- `STORE_TOPUP` — merchant prepaid balance for fees. Top-up via PayMongo (GCash/Maya/card) **or** cash paid to operator (operator records it with receipt photo; admin sees audit).
- `STORE_PAYABLE` — what FoxDrop/operator owes the store from orders.
- `RIDER_EARNINGS` — what is owed to the rider.
- `RIDER_CASH` — COD cash physically held by the rider (a liability to the operator).
- `OPERATOR_REVENUE`, `OPERATOR_CASH` (cash received from riders), `PLATFORM_REVENUE`, `GATEWAY_CLEARING`, `CUSTOMER_REFUND`.

**Sign convention (use it everywhere):** a **positive** balance means the system owes that wallet's owner money, or the owner has earned it (`STORE_TOPUP`, `STORE_PAYABLE`, `RIDER_EARNINGS`, `OPERATOR_REVENUE`, `PLATFORM_REVENUE`, `CUSTOMER_REFUND`). The cash/clearing wallets (`GATEWAY_CLEARING`, `RIDER_CASH`, `OPERATOR_CASH`) go **negative** while that party is physically holding money that belongs to others, so `rider cash_on_hand = −balance(RIDER_CASH)`. Money entering the system is always a negative entry on a cash/clearing wallet matched by positive entries on the wallets that are owed.

Posting rules (implement each as one SQL function, idempotent; each one sums to zero):
1. **Online order COMPLETED:** `GATEWAY_CLEARING −total` → `STORE_PAYABLE +store_net`, `RIDER_EARNINGS +rider_earning`, `OPERATOR_REVENUE +operator_cut+operator_comm`, `PLATFORM_REVENUE +platform_comm`. An online order cancelled before COMPLETED has no ledger entries yet: refund through PayMongo and record it on `payments`.
2. **COD order DELIVERED:** rider collects `total` cash → `RIDER_CASH −total` with the same positive splits as rule 1 (store_net to STORE_PAYABLE, etc.).
3. **Promo discount:** when `discount > 0`, the same txn also posts `−discount` on the wallet of the promo owner (store promo → `STORE_PAYABLE`, province promo → `OPERATOR_REVENUE`, platform promo → `PLATFORM_REVENUE`) so the txn still sums to zero.
4. **Remittance:** rider submits remittance (amount, method: cash to operator / GCash transfer with reference + screenshot). Operator **confirms** → `RIDER_CASH +amount`, `OPERATOR_CASH −amount`. If `allow_net_remittance` (default true) the rider may keep unpaid earnings: an extra pair `RIDER_EARNINGS −earnings_due`, `RIDER_CASH +earnings_due`, so `net_remit = cash_on_hand − earnings_due`. Show **Daily Remittance Tracking**: per rider per day collected / remitted / outstanding; overdue >24 h flagged red.
5. **Store top-up:** online → `GATEWAY_CLEARING −amount`, `STORE_TOPUP +amount`; cash paid to operator → `OPERATOR_CASH −amount`, `STORE_TOPUP +amount`.
6. **Monthly store fee:** `STORE_TOPUP −store_fee` → `OPERATOR_REVENUE +store_fee`.
7. **License payment:** `GATEWAY_CLEARING −license_fee` → `PLATFORM_REVENUE +license_fee`.
8. **Payouts (settlement):** weekly (default Monday) operator generates **settlement batch** per store and per rider: amounts in STORE_PAYABLE / RIDER_EARNINGS; operator marks each paid with method + reference → `STORE_PAYABLE −x` (or `RIDER_EARNINGS −x`), `OPERATOR_CASH +x` (cash) or `GATEWAY_CLEARING +x` (transfer). Payout via gateway API is a later enhancement; v1 = record manual payout.
9. **Refunds/adjustments/penalties** (admin/operator via disputes): always new reversing entries; never edit or delete ledger rows. Enforce this in the database: no `UPDATE`/`DELETE` grants on `ledger_*` and a trigger that raises on any attempt; a deferred constraint trigger rejects any txn whose entries do not sum to zero.

Reports must reconcile: for any period, sum of all entries = 0, and `orders.total` of completed orders = gateway + COD cash amounts.

---

## 8. DATA MODEL (Postgres — create via migrations)

Core tables (all have `id uuid pk default gen_random_uuid()`, `created_at`, `updated_at`; provincial ones have `province_id`):
- `provinces` (PSGC code, name, region, center geography, status, store_fee_centavos, rider_cash_limit_centavos, timezone default 'Asia/Manila') — seed **all 82 PH provinces + NCR** from PSGC.
- `province_licenses` (province_id, operator_user_id, status, start_at, end_at, fee_centavos, payment_id, renewal_of).
- `operator_applications` (applicant info, business name, DTI/SEC, valid ID, requested province, status, reviewer).
- `profiles` (user_id, full_name, phone, email, avatar_url, default_address_id), `user_roles`, `addresses` (label, geography point, landmark, barangay, city, province_id).
- `delivery_zones` (province_id, name, area geography, active), `delivery_rate_cards` (province_id, base_fare, base_km, rate_per_km, min_fee, max_km, rider_share_pct, effective_from, created_by).
- `stores` (province_id, owner_user_id, name, category [food, groceries, medicine, more], logo, banner, location point, address, opening_hours jsonb, prep_time_min, status, rating_avg, rating_count, accepts_cod bool, accepts_pickup bool, fee_status), `store_staff`.
- `product_categories`, `products` (store_id, name, description, price_centavos, image_url, is_available, stock nullable, options jsonb for add-ons/variants), `promos` (code, scope province/store, type percent/fixed/free-delivery, limits, dates) — "Exclusive Promos & Deals".
- `riders` (user_id, province_id, vehicle_type, plate_no, license_no, license_expiry, status, online, last_location geography, last_seen_at, rating_avg, rating_count), `rider_documents`.
- `carts` (local Room only), `quotes`.
- `orders` (code like FD-NEG-000123, province_id, customer_id, store_id, rider_id, fulfillment [DELIVERY|PICKUP], payment_method [ONLINE|COD], payment_status, status, item_subtotal, delivery_fee, discount, total, rate_card_snapshot jsonb, distance_km, dropoff address snapshot + point, pickup_code, notes, eta_at, timestamps per milestone), `order_items` (snapshot name/price/qty/options), `order_events`, `order_offers` (dispatch attempts).
- `proof_of_delivery` (order_id, photo_path, recipient_name, otp_verified bool, signature_path nullable, lat/lng, captured_at).
- `payments` (provider, intent_id, checkout_url, purpose [ORDER|TOPUP|LICENSE], amount, status, raw webhook jsonb), `webhook_events` (idempotency).
- `wallets`, `ledger_txns`, `ledger_entries`, `topups`, `remittances` (rider_id, amount, method, reference, proof_path, status SUBMITTED/CONFIRMED/REJECTED, confirmed_by), `settlement_batches`, `settlement_lines`, `payouts`.
- `rider_locations` (rider_id, order_id nullable, point, speed, is_mock, recorded_at — throttled, kept 30 days, §10), `favorites` (customer_id, store_id / product_id), `app_events` (funnel events, §10).
- `reviews` (order_id, store_rating, rider_rating, comment) — one per order, only after COMPLETED.
- `support_tickets`, `ticket_messages`, `disputes` (order_id, reason, resolution [REFUND|ADJUSTMENT|PENALTY|NONE], amount, resolved_by), `notifications`, `device_tokens`, `audit_logs` (every admin/operator write), `platform_settings` (single row, key/value jsonb: license_fee, default_store_fee, commissions caps, payment toggles, feature flags).

Indexes: GIST on all geography columns; `(province_id, status, created_at)` on orders; `(rider_id, status)`.
RLS summary: customers see own orders/addresses; stores see own store's orders/products; riders see offered/assigned orders only (customer phone visible only while active; mask after completion); operators see everything with their `province_id`; super_admin sees all. Write **pgTAP tests** proving cross-province reads return 0 rows.

---

## 9. FEATURE SPEC PER APP (screens)

### 9.1 Customer app (match `01_Customer_Workflow.png` 1→8)
1. **Sign up / Log in:** phone OTP, email, Google. Onboarding asks location permission → detect province; if no active license → "FoxDrop is coming soon to your province" + notify-me.
2. **Browse stores:** "Deliver to: My Location" selector, search (stores + products), category chips (Food, Groceries, Medicine, More), Nearby Stores list with rating, category, ETA range (e.g., 15–30 min), open/closed, promo banners.
3. **Store & product:** product detail with image, price, qty stepper, options, favorite (heart), Add to Cart. Cart limited to one store (ask to clear when switching).
4. **Choose fulfillment:** Deliver to Me / Pick Up at store.
5. **Checkout / Order summary:** items, subtotal, delivery fee (from `quote`), promo code, total; payment method **Online (GCash, Maya, Card)** or **Cash on Delivery**; notes; Place Order.
6. **Track order:** timeline (Placed, Preparing, On the way, Arriving soon) with times, live map with rider marker moving (Realtime), ETA, rider name/photo/plate, call/chat rider (masked), cancel if allowed.
7. **Receive:** show 4-digit **delivery OTP** to give the rider (POD); pick-up code for pick-up orders.
8. **Rate & Review:** stars for store and rider + comment, Submit Review.
Plus: Orders history & reorder, addresses, profile, notifications, help/support ticket, promos list.

### 9.2 Rider app (match `04_Rider_Workflow.png` 1→8)
1. **Register & verify:** personal details, valid ID, driver's license, vehicle details → Submit → status screen (Pending/Approved/Rejected with reason).
2. **Go online:** big Go Online / Offline toggle; starts foreground location service (notification visible), sends location every 5 s while ON_JOB, 15 s while idle; battery-aware.
3. **Delivery request:** full-screen offer card: store (distance), customer (distance), estimated earnings, countdown 30 s, Accept / Decline, sound + vibration.
4. **Order accepted:** pickup store, deliver-to name, items, fee, payment type (COD amount to collect highlighted).
5. **Pick up from store:** Arrived at Store (geofence 150 m check) → confirm items checklist → Picked Up.
6. **Navigate:** in-app map with route + Navigate button (Google Maps/Waze intent).
7. **Confirm delivery & payment:** Arrived → take POD photo (required) + enter customer OTP (required unless customer unreachable → photo + reason, flagged for operator) → if COD: confirm "Payment Collected ₱X" → Complete. Works offline: queue in Room + WorkManager upload.
8. **Earnings & ratings:** Today's earnings, completed orders, total distance, rating with review count, history, **Cash on hand & Remit** screen (submit remittance, see confirmed/pending), payout history.
Plus: support, profile, document expiry reminders.

### 9.3 Business app — Store / Merchant role
Dashboard (today orders, sales, wallet balance, rating), **incoming order alarm** (loud sound until accepted/rejected, auto-reject after 5 min), order board (New / Preparing / Ready / Done), set prep time, mark Ready, pick-up code verify, catalog CRUD (categories, products, photos, availability toggle, options), opening hours & pause store, **Top-up wallet** (balance, top-up via GCash/Maya/card, history, upcoming ₱15 fee), payouts & statements, reviews, staff accounts, store profile, support.

### 9.4 Business app — Provincial Operator role (match `03_Provincial_Operator_Workflow.png`)
1. **Register & apply** for a province (only AVAILABLE provinces selectable) with documents.
2. **Subscribe & pay ₱15,000/year** online (PayMongo).
3. **Set up territory:** activate province, draw coverage zones on map, base settings.
4. **Onboard partners:** approve/reject stores and riders (view documents), set store fee (default ₱15/month).
5. **Manage operations:** products & stores, **delivery rate per KM** (rate card editor with live fee preview), delivery zones, handle top-up wallets (record cash top-ups, view balances/transactions).
6. **Monitor orders & earnings:** live order board + live map of riders, manual assign/reassign, POD viewer, commissions & fees view, rider performance.
7. **Reports:** orders & revenue, store and rider performance, commission & fee summary, **Daily Remittance Tracking**, settlement batches; **export PDF / Excel(CSV/XLSX)**.
8. **Renew annually:** license status card, days left, Renew (pay ₱15,000).
Dashboard KPIs like the image: Total Orders, Gross Sales, Commissions, Active Riders, Orders Trend (7 days), Top Stores. Province selector shown (e.g., "Negros Occidental").

### 9.5 Super Admin — built in the **web dashboard (§20)**, not in the Android apps (match `02_Super_Admin_Workflow.png`)
1. **Login & access:** secure login, **2FA (TOTP) required**, role-based access (super admin, admin staff with permissions).
2. **Dashboard:** Total Orders, Active Stores, Active Riders, Provinces (active), Order Trend 30 days, Payment Summary (Online vs COD vs Others, total ₱), platform health & alerts.
3. **Manage users & franchises:** create/update/suspend users, operator applications, assign province license, license status & renewals, admin/sub-admin/staff accounts.
4. **Approve stores & riders** across provinces (can override operator).
5. **Monitor orders & payments:** live/historical orders, POD, COD collection & remittance status per province, online payments (GCash, Maya, Card), webhook log.
6. **Analytics & reports:** orders, revenue, commissions, province/franchise performance, store & rider productivity, export Excel/PDF.
7. **Platform settings:** default fees (license fee, default store fee, commission caps, default rate card), service coverage (enable provinces), payment gateway config/toggles, notifications, app preferences, feature flags, min app version (force update).
8. **Support & dispute management (ongoing):** inbox of tickets from customers/stores/riders, investigate order + payment + delivery timeline, apply resolution (refund, adjustment, penalty) → ledger entries, dispute history log.

---

## 10. CROSS-CUTTING REQUIREMENTS

- **Realtime tracking:** rider app publishes to Realtime Broadcast channel `order:{id}:location` (~5 s); server persists a throttled point every 30 s to `rider_locations` (keep 30 days). Customer map animates marker smoothly between points.
- **Notifications (FCM):** order status for customer; new order for store (high-priority, custom sound channel); offers for rider; approvals, low wallet balance, license expiry, remittance overdue for operator; disputes for admin. Edge Function `notify` sends them; store tokens in `device_tokens`.
- **Payments (PayMongo):** Edge Function `create-payment` (purpose ORDER/TOPUP/LICENSE) returns checkout URL; `paymongo-webhook` verifies signature, is idempotent (`webhook_events`), updates `payments`, then calls the right posting function. Apps poll `payments.status` + subscribe realtime after returning from Custom Tab. Unpaid online orders auto-cancel after 15 min.
- **Offline & bad signal (provinces!):** cache store list/menus, show offline banner, queue rider actions (Picked Up, POD, Complete) with WorkManager retries and server idempotency keys.
- **Localization:** English default; strings in `strings.xml` ready for **Filipino (tl)** translation; currency format `₱1,234.00`; dates in Asia/Manila.
- **Accessibility:** content descriptions, 48dp touch targets, contrast AA, dynamic font sizes.
- **Security & privacy (Data Privacy Act of 2012):** private buckets with signed URLs; mask customer phone to rider after delivery; consent checkbox and privacy policy screen; delete-account flow; audit logs for admin/operator actions; rate limiting on OTP; R8/proguard on release; no secrets in APK.
- **Performance:** cold start < 1.5 s on a mid-range device with the baseline profile (§11A.8), paginate lists (Paging 3), image thumbnails.
- **Observability:** Firebase Crashlytics, structured logs in Edge Functions, `app_events` table for key funnel events.

---

## 11. BRANDING / DESIGN SYSTEM (`:core:designsystem`)

Colors (sampled from the logos):
- **Primary – FoxDrop Pink/Red `#F2014F`** (buttons, highlights, active states)
- **Secondary – Navy `#05153C`** (headers, dark surfaces, text on light)
- Background `#FFFFFF` / `#F7F7FA`, Surface tint pink-50 `#FFE8EF`, Success `#1DB954`, Warning `#FFB020`, Error `#D32F2F`.
- Provide light + dark themes (dark: navy background, pink accents). Dynamic color OFF (brand first).
- Typography: bold, rounded geometric sans (e.g., **Poppins** or **Montserrat** via downloadable fonts) — headings ExtraBold Italic feel like the logo; body Regular.
- Shapes: rounded 16dp cards, pill buttons (like the image buttons "Sign Up", "Place Order", "Go Online").
- Logos in `/reference/logos`:
  - `foxdrop_swift_fox_delivery_logo.png` (fox + parcel + wordmark) → **customer app icon & splash** (crop the fox+parcel mark for the adaptive icon foreground, pink/white on navy background).
  - `foxdrop_delivery_logo_with_fox_emblem.png` (FD monogram) → **Business app icon**.
  - `foxdrop_dynamic_fox_delivery_logo.png` (round badge) → **Rider app icon** and rider jacket-style badges.
  - `foxdrop_speedy_delivery_logo.png` (wordmark + "DELIVERING NEAR YOU") → login headers, empty states, receipts.
  - Generate all densities with Android Studio Image Asset rules (adaptive icon foreground/background + monochrome). If you cannot run Image Asset, write a script (Python/Pillow) to produce `mipmap-*` PNGs and `ic_launcher_foreground` from the source PNGs.
- Reusable components: `FoxButton`, `FoxTopBar`, `StoreCard`, `ProductCard`, `QuantityStepper`, `MoneyText`, `StatusTimeline`, `KpiCard`, `TrendChart` (Vico charts), `MapCard`, `EmptyState`, `OfflineBanner`, `CountdownOfferCard`, `PhotoCapture`.

---

## 11A. PREMIUM UI/UX & MOTION (target: feels like a top-tier delivery app)

**Libraries (add to version catalog):** `androidx.core:core-splashscreen`, Compose Animation (`AnimatedContent`, `SharedTransitionLayout`, `AnimatedVisibility`, `Animatable`, `rememberInfiniteTransition`, `updateTransition`), `com.airbnb.android:lottie-compose`, `androidx.compose.material3:material3` + `material3-adaptive-navigation-suite`, Coil 3 (crossfade), Vico (charts), `androidx.biometric`, Roborazzi (screenshot tests). Use Material 3 **Expressive** components/motion scheme only where they are stable in the chosen version.

**Motion tokens** (put in `:core:designsystem` → `FoxMotion`): durations short 150 ms, medium 300 ms, long 450 ms; easing `EmphasizedDecelerate` for enter, `EmphasizedAccelerate` for exit; springs `spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)` for playful bounces. Every animation uses these tokens; no magic numbers in screens.

### 11A.1 Splash screen (all 3 apps)
1. **Android 12+ SplashScreen API** via `core-splashscreen` (works back to API 23): theme `Theme.FoxDrop.Starting` with `windowSplashScreenBackground = navy #05153C`, `windowSplashScreenAnimatedIcon` = **Animated Vector Drawable** of the fox mark (fox slides in + parcel drops, ≤ 800 ms), `postSplashScreenTheme`.
2. `installSplashScreen().setKeepOnScreenCondition { !viewModel.isReady }` while session + remote config load (hard cap 1.5 s).
3. `setOnExitAnimationListener`: icon scales up 1.0→1.15 and fades, background circular-reveals into the app (300 ms).
4. **First launch only:** branded intro screen in Compose — Lottie animation `res/raw/fox_run.json` (fox runs across, leaves a pink speed trail, wordmark "FoxDrop – Delivering near you" slides up). If no Lottie file exists yet, build the same effect in pure Compose (`Animatable` offset/scale/alpha on the logo PNG + `Canvas` speed lines) and leave a TODO slot for a designer Lottie. Then onboarding pager (3 pages, parallax illustrations, animated page indicator) → location permission explainer → auth.
5. Returning user: splash → home directly. Never show a fake delay.

### 11A.2 Login ⇄ Create Account (animated)
- One `AuthScreen` with `mode: Login | SignUp`, not two separate destinations, so the switch is smooth.
- Wrap in `SharedTransitionLayout`. **Logo** uses `Modifier.sharedElement` (moves/shrinks between modes), **form card** and **primary button** use `Modifier.sharedBounds` (card grows to fit the extra fields), header text uses `AnimatedContent(targetState = mode, transitionSpec = { (slideInHorizontally { it/3 } + fadeIn()) togetherWith (slideOutHorizontally { -it/3 } + fadeOut()) using SizeTransform(clip = false) })`.
- Pill tab switch "Log In | Sign Up" with sliding indicator (`animateDpAsState` on offset + width).
- Extra sign-up fields (name, email, terms checkbox) enter with **stagger** (40 ms per field) using `AnimatedVisibility` + `expandVertically + fadeIn`.
- Phone OTP: 6 animated boxes, auto-advance, paste support, **SMS User Consent API** autofill, resend countdown; wrong code → horizontal **shake** (`Animatable` keyframes) + error haptic; success → boxes turn green one by one then navigate.
- Buttons morph to a loading indicator (`animateContentSize`) and then to a ✓ check; disable double tap.
- Google sign-in via **Credential Manager** (`androidx.credentials`), not the deprecated GoogleSignIn API.
- Keyboard handling: `imePadding()`, IME actions move focus, `autofill` hints for phone/email/password.

### 11A.3 Navigation transitions
- Navigation Compose with type-safe routes; default **shared-axis X** transitions (forward slide+fade, back reverse), modal flows use **shared-axis Y**, tabs use fade-through.
- **Predictive back** enabled (`android:enableOnBackInvokedCallback="true"`); custom sheets use `PredictiveBackHandler`.
- List → detail: store card image/name shared-element into the store screen (`SharedTransitionLayout` + `AnimatedVisibilityScope` from NavHost).
- **Edge-to-edge** everywhere (`enableEdgeToEdge()`), correct insets, transparent system bars.

### 11A.4 Micro-interactions (implement all)
- Press feedback: buttons/cards scale to 0.96 on press (`interactionSource` + `animateFloatAsState`).
- **Add to cart:** product thumbnail flies along a curve to the cart icon, cart badge bounces (spring), light haptic.
- Quantity stepper: number slides up/down (`AnimatedContent`), tick haptic.
- Favorite heart: spring scale + small particle burst.
- **Place Order:** button → progress → ✓; then success screen with Lottie (fox delivering parcel / confetti) and a strong haptic (`HapticFeedbackConstants.CONFIRM`).
- **Tracking screen:** rider marker **smoothly interpolated** between GPS points (`Animatable` on lat/lng + bearing rotation), route polyline draws in, status timeline fills step by step, `BottomSheetScaffold` with ETA chip that counts down, pulsing dot on "On the way".
- **Rider:** big "Go Online" button morphs color/shape, pulsing ring when online (`rememberInfiniteTransition`); new offer card slides up full-screen with circular 30 s countdown ring, sound + vibration pattern; **swipe-to-confirm** for Picked Up and Complete (prevents accidental taps).
- **Store:** new order banner pulses + looping alarm sound until handled.
- Dashboards: KPI numbers count up, charts animate in (Vico), pull-to-refresh (`PullToRefreshBox`).
- Lists: `Modifier.animateItem()` for insert/remove/reorder, stable keys.

### 11A.5 Loading, empty, error states
- **Skeleton shimmer** (`Modifier.shimmer()` built with `rememberInfiniteTransition` + `Brush.linearGradient`) shaped like the real content for every list/detail. No full-screen spinners for content.
- Images: Coil crossfade + brand-tinted placeholder; product grid aspect ratios fixed (no layout jump).
- Empty states: small illustration/Lottie + friendly message + action button.
- Errors: inline retry, offline banner slides down when connectivity lost; optimistic UI for cart/favorites.

### 11A.6 Theming & layout
- Color scheme generated from seed `#F2014F` with **Material Theme Builder**, light + dark (navy surfaces), brand colors fixed (dynamic color off).
- Tokens: spacing (4/8/12/16/24/32), shapes (8/16/24/pill), elevation, typography (Poppins/Montserrat via downloadable fonts), motion (above).
- Adaptive: `WindowSizeClass` + `NavigationSuiteScaffold` (bottom bar on phone, rail on tablet, drawer on large), `ListDetailPaneScaffold` for operator/admin orders & approvals.

### 11A.7 Accessibility & inclusivity
- Respect **"Remove animations"** (animator duration scale = 0 → instant transitions, no Lottie loop).
- Touch targets ≥ 48 dp, contrast WCAG AA, TalkBack labels, `semantics` for maps/charts, font scale up to 200 % (`@PreviewFontScale`), no info by color alone (status chips have icon + text).

### 11A.8 Performance of UI
- Animate with `graphicsLayer { }` lambdas and deferred state reads; strong skipping on; stable/immutable UI models (`kotlinx.collections.immutable`).
- **Baseline Profiles** + **Macrobenchmark**: cold start < 1.5 s, store list scroll jank < 1 % frames > 16 ms on a mid-range device.

### 11A.9 Premium UX acceptance checklist
✅ Animated splash + first-run intro ✅ Login ⇄ Sign Up shared-element transition ✅ Shared-axis navigation + predictive back ✅ Add-to-cart fly animation ✅ Smooth rider marker on map ✅ Swipe-to-confirm for rider ✅ Skeletons on all lists ✅ Haptics on key actions ✅ Dark mode ✅ Reduce-motion respected ✅ Roborazzi screenshot tests for every screen (light/dark, phone/tablet) ✅ Baseline profile shipped.

---

## 12. EDGE FUNCTIONS & SCHEDULED JOBS

Functions: `quote`, `place-order`, `dispatch`, `rider-accept`, `create-payment`, `paymongo-webhook`, `notify`, `export-report` (CSV/XLSX + PDF), `settlement-generate`, `admin-resolve-dispute`.
pg_cron jobs (Asia/Manila): every minute → expire offers, auto-cancel unpaid/unanswered orders; hourly → flag overdue remittances, auto-offline stale riders (no location 10 min); daily 00:05 → license status changes + reminders, document expiry; monthly 1st 00:10 → store fees; weekly Monday 06:00 → settlement batches draft.

---

## 13. BUILD PHASES & ACCEPTANCE CHECKS

Each phase ends with the **Quality Gate** (§19): `./gradlew check` green (spotless, detekt, lint, unit tests, Kover, screenshot tests), `supabase db reset` + pgTAP + Edge Function tests green, `/simplify` → `/code-review` → `/security-review` passes with all findings fixed or written in `docs/DECISIONS.md`, a short `docs/PHASE_X_REPORT.md` (what's done, how to test, screenshots), and a commit/PR (Conventional Commits).

**Phase 0 — Foundation**
`git init` if the folder is not a repository yet (with `.gitignore` covering `local.properties`, `.env`, keystores, `google-services.json`, build output), version catalog, `build-logic` convention plugins, 3 app modules + core modules, Hilt, design tokens + `FoxDropTheme` with brand colors, logos/app icons, animated splash (§11A.1), animated Login ⇄ Create Account screen (§11A.2, UI only — real auth is Phase 1), shimmer/skeleton + empty/error components, edge-to-edge, navigation shells, secure storage (§17), `supabase init` + local stack running, Supabase config via `local.properties` / `BuildConfig` (keys NOT committed; provide `local.properties.example`), CI workflow (GitHub Actions, the PR checks in §16), `docs/PROGRESS.md`, `docs/DECISIONS.md` and `docs/OWNER_TODO.md` created.
✅ All 3 apps launch to a branded animated splash + login screen. Dark/light both readable. ✅ `./gradlew check` green on a clean checkout. ✅ A `feature → feature` dependency fails the build.

**Phase 1 — Database, auth, roles**
All migrations from §8, RLS, JWT role claims, PSGC province seed, demo seed (1 active province "Negros Occidental", 1 operator, 3 stores with products, 3 riders, 2 customers, 1 super admin). Auth flows (phone OTP, email, Google) in all apps; role routing in business app.
✅ pgTAP: operator of Province A reads 0 rows of Province B (orders, stores, riders, wallets). ✅ Unique active license per province enforced (second insert fails). ✅ Each demo user lands on the correct home.

**Phase 2 — Operator & licensing & partner onboarding**
Operator application, license payment (PayMongo test mode), license states + renewal, territory/zones map editor, rate card editor with preview, store & rider applications (with document upload) and approval flows, admin approval/override as `admin_api.*` DB functions with pgTAP tests (their web UI comes in Phase W1).
✅ Applying for an already-licensed province is impossible. ✅ Paying the test license flips status to ACTIVE with end date +365 days. ✅ Approved store appears to customers; rejected does not.

**Phase W1 — Super Admin web foundation (after Phase 2, see §20)**
Laravel + Inertia + React/TS app in `admin-web/`, own `admin` schema, admin login + required 2FA, roles/permissions, audit log, premium layout shell, then: operator applications, province licenses (assign/renew/suspend), store & rider approvals with override, users management.
✅ Admin without 2FA cannot open any page. ✅ `admin_web` DB role cannot `UPDATE` money/status tables directly (test proves permission denied). ✅ Approving an operator application in the web shows up immediately in the operator's Android app. ✅ Every admin action appears in the audit log.

**Phase 3 — Catalog & customer ordering**
Store catalog CRUD, customer browse/search/categories, cart, fulfillment choice, `quote` with Routes API, checkout with COD and Online, PayMongo checkout via Custom Tabs + webhook, store incoming order alarm and accept/reject, pick-up flow with code.
✅ Fee = formula in §6 for 3 test distances (unit test on server function). ✅ Tampering with price in client has no effect. ✅ Online order stays PENDING_PAYMENT until webhook; failed payment cancels. ✅ Store not answering in 5 min auto-cancels (and refunds online).

**Phase 4 — Rider app, dispatch, live tracking, POD**
Rider onboarding, online toggle + foreground service, dispatch offers with 30 s countdown, accept race-safety, pickup geofence, navigation, POD (photo + OTP), offline queue, live tracking on customer map, operator live rider map + manual assign.
✅ Two riders accepting the same offer: exactly one wins (integration test). ✅ Order cannot reach DELIVERED without POD photo. ✅ Customer sees rider marker move within ~5 s latency. ✅ Rider actions done offline sync correctly after reconnect.

**Phase 5 — Money: ledger, top-ups, COD remittance, store fees, settlement**
Ledger functions & posting rules (§7), store top-up (online + operator-recorded cash), monthly store fee job, rider cash-on-hand + cash limit, remittance submit/confirm, daily remittance tracking, settlement batches & payout recording, earnings screens.
✅ Property test: after random 500 orders (mix COD/online/pickup/cancel/refund) every ledger txn sums to 0 and wallet balances equal expected. ✅ Store with ₱0 wallet on fee day → negative then suspended after grace, restored after top-up. ✅ Rider over cash limit receives no COD offers.

**Phase 6 — Dashboards, reports, reviews, support & disputes**
Operator dashboards (Android) (KPIs, trends, payment summary), reports with filters + export PDF/Excel, reviews & ratings, support tickets for all roles, disputes with refund/adjustment/penalty posting (DB functions; the admin UI is Phase W2), audit logs, operator settings screens (platform settings UI is Phase W2), notifications everywhere.
✅ Dashboard numbers equal SQL ground truth for seed data. ✅ Exported CSV opens in Excel with correct ₱ totals. ✅ Refund via dispute creates reversing ledger entries and notifies customer.

**Phase W2 — Super Admin web complete (after Phase 6, see §20)**
Dashboard KPIs + charts (Total Orders, Active Stores, Active Riders, Provinces, 30-day trend, Payment Summary), live orders + live rider map, payments & webhook log, COD/remittance per province, analytics & reports with queued Excel/PDF export, platform settings, support & dispute center with refund/adjustment/penalty, admin staff management.
✅ Web dashboard numbers equal the same SQL ground truth as the operator app. ✅ Dispute refund from web creates reversing ledger entries (via DB function) and the customer gets a push. ✅ Export of 50,000 orders runs in the queue and downloads without timeout. ✅ Lighthouse (desktop) performance ≥ 90, accessibility ≥ 95.

**Phase 7 — Hardening & release**
Filipino strings scaffold, accessibility pass, admin-web production deploy + security headers check, performance (baseline profiles), Crashlytics, force-update check, privacy policy/delete account, R8 release builds, signed AAB instructions, Play Store listing text for 3 apps, `docs/RUNBOOK.md` (deploy Supabase, set secrets, rotate keys, add a new province, handle a dispute).
✅ Release AABs build. ✅ End-to-end manual test script in `docs/TEST_PLAN.md` passes: operator buys province → approves store & rider → customer orders COD → rider delivers with POD → rider remits → operator confirms → settlement shows correct store/rider/operator amounts; then same with online payment.

---

## 14. ASSUMPTIONS / DEFAULTS (change in Platform Settings later)

| Item | Default |
|---|---|
| License fee | ₱15,000 / province / year, 15-day grace |
| Store fee | ₱15 / store / month, charged 1st of month from top-up wallet, 7-day grace then suspend |
| Rate card | base ₱40 (first 2 km) + ₱10/km after, min ₱40, max 15 km |
| Rider share of delivery fee | 80% rider / 20% operator |
| Commission on items | Platform 0%, Operator 0% (configurable; admin sets a cap) |
| Rider COD cash limit | ₱5,000; remit within 24 h |
| Offer timeout / store response timeout / unpaid online order | 30 s / 5 min / 15 min |
| Settlement | weekly, manual payout recording in v1 |
| Payment gateway | PayMongo (GCash, Maya, Card); COD always available unless store disables |
| Promo discount | paid by the promo owner (store / operator / platform) |
| Rounding | integer centavos, half-up; remainder to operator |
| Cart | one store per order |
| Languages | English now, Filipino-ready |
| Super Admin | web dashboard only (Laravel + Inertia + React, §20); 2FA required |

---

## 15. DEFINITION OF DONE

- All phases' acceptance checks pass and are documented.
- No money calculated on device; ledger balances always sum to zero.
- Province isolation proven by tests.
- The 3 apps match the flows and look of the reference images (pink `#F2014F` + navy `#05153C`, FoxDrop logos).
- Premium UX checklist in §11A.9 is all ✅; no screen shows a plain spinner for a list, every screen has loading/empty/error states, all animations respect "Remove animations".
- Security checklist in §17 is all ✅; MobSF scan of release APK has no High findings; `/security-review` clean.
- Code standards in §16 are enforced by CI (the build fails if they are broken).
- Super Admin web (§20) passes its quality gate (§20.7), uses the same database and DB functions as the apps, and every admin action is audited.
- `README.md` explains setup in simple steps: create Supabase project → run migrations → set secrets (PayMongo, Google Maps, FCM, SMS) → put keys in `local.properties` → run each app → run and deploy `admin-web`. Keep the README's existing owner sections and add a "Developer setup" section from Phase 0 on; update it whenever setup steps change.
- `docs/OWNER_TODO.md` has no open item that blocks release.


---

## 16. ENGINEERING STANDARDS (senior level, enforced by CI)

**Architecture**
- **Clean Architecture** layers per feature: `presentation` (Compose + ViewModel) → `domain` (use cases, models, repository interfaces, pure Kotlin) → `data` (repository impl, Supabase DTOs, Room, mappers). Domain never imports Android or Supabase.
- **MVI** per screen: `data class XState`, `sealed interface XAction`, `sealed interface XEffect` (navigation/snackbar via `Channel`, collected once), reducer is a pure function and unit-tested. Expose `StateFlow` with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`; collect with `collectAsStateWithLifecycle()`.
- Screens split into `XRoute` (gets ViewModel) and stateless `XScreen(state, onAction)` so previews/tests need no Hilt.
- **Offline-first** repositories: Room is the single source of truth for cached data; network refreshes write to Room.
- Typed errors: `sealed interface DomainError` (Network, Unauthorized, NotFound, Validation, Conflict, Unknown) and `Result`-style returns; no exceptions leaking to UI; no `!!`; no `GlobalScope`; inject `CoroutineDispatcher`s.

**Modularization & build**
- `build-logic/` **convention plugins**: `foxdrop.android.application`, `foxdrop.android.library`, `foxdrop.android.feature`, `foxdrop.android.compose`, `foxdrop.hilt`, `foxdrop.room`, `foxdrop.jvm.library`. Module build files stay tiny.
- Dependency rule: `feature:*` depends only on `core:*`, never on another feature (checked by a Gradle task in CI). Apps wire features together.
- Gradle: version catalog, configuration cache + build cache on, `dependency verification` (`gradle/verification-metadata.xml`), Renovate/Dependabot for updates.

**Code quality**
- **Spotless + ktlint** (formatting), **detekt** with **Compose rules** (`io.nlopez.compose.rules:detekt`), **Android Lint** with `warningsAsErrors` for release. Compose compiler stability reports reviewed for hot screens.
- Kotlin official style, meaningful names, functions < 40 lines where reasonable, KDoc on public core APIs. No commented-out code. No TODO without an issue link.
- Logging: Timber in debug only; never log tokens, phone numbers, addresses, or payment data.

**Testing (test pyramid)**
- Unit: reducers, ViewModels (Turbine + `MainDispatcherRule`), use cases, mappers, fee formula mirror tests. **Kover ≥ 80 %** on `domain` and `data` modules (CI fails below).
- UI: Compose UI tests for auth, checkout, rider delivery, store accept; **Roborazzi** screenshot tests (light/dark, phone/tablet, font scale 1.5).
- Backend: **pgTAP** for RLS + ledger functions; **Deno tests** for every Edge Function (with mocked PayMongo/Routes).
- E2E: **Maestro** flows: customer COD order, customer online order, rider full delivery, operator approval.
- Performance: Macrobenchmark + Baseline Profile generator module `:baselineprofile`.

**CI/CD (GitHub Actions)**
- On every PR: `spotlessCheck detekt lint testDebugUnitTest koverVerify verifyRoborazziDebug assembleDebug`, `supabase db lint`, pgTAP, Deno tests, **gitleaks** secret scan.
- On tag `v*`: signed release AABs (keystore from GitHub Secrets), optional upload to Play **internal testing** track (Gradle Play Publisher).
- Conventional Commits, one branch + PR per phase, `CHANGELOG.md`, Architecture Decision Records in `docs/adr/`.

**Backend standards**
- Schema changes only through numbered migrations (never click-edit in the dashboard). SQL functions `SECURITY DEFINER` must `SET search_path = ''` and check the caller's role inside.
- Edge Functions in TypeScript with **zod** input validation, consistent error JSON `{ "code": "...", "message": "..." }`, idempotency keys on all money/state writes, structured logs with request id.
- Generate typed DTOs/API docs in `docs/API.md` for every RPC and function.

---

## 17. SECURITY HARDENING (OWASP MASVS L1 + key L2 controls)

**Backend (most important — the app can be reverse-engineered, the server cannot)**
- RLS **enabled and forced on every table**, deny by default; pgTAP proves cross-province and cross-user isolation.
- Money and order status tables are **not writable directly** by clients — only via RPC/Edge Functions that check role, province, and state.
- Edge Functions verify the JWT and role on every call; `service_role` key used only inside functions, never in apps.
- Rate limits: OTP send, login, place-order, quote, remittance submit (per user + per IP).
- PayMongo **webhook signature verification**, replay protection (timestamp + `webhook_events` idempotency), amounts re-checked against `orders`.
- Storage: private buckets for KYC, POD, receipts; access only via **short-lived signed URLs (≤ 5 min)**; upload size/type limits.
- Sensitive values (government ID numbers, license numbers) encrypted at rest with **pgcrypto using a key stored in Supabase Vault**; show masked in UI.
- **MFA (TOTP)**: required for Super Admin and admin staff, recommended for operators (setting). Supabase leaked-password protection on.
- **Audit log** for every admin/operator write (who, what, before/after, IP, device). Point-in-time recovery backups enabled.

**Android apps**
- Session tokens stored with **DataStore + Google Tink AEAD** using an **Android Keystore**-backed key (do not use the deprecated `security-crypto` EncryptedSharedPreferences).
- Network: HTTPS only (`network_security_config` with `cleartextTrafficPermitted=false`); **certificate pinning** with OkHttp `CertificatePinner` for the Supabase domain using public-key hashes of the CA/intermediate **plus a backup pin**, and a remote-config kill switch so a cert change never bricks the app.
- **Play Integrity API** (standard request) on login, place order, rider go-online, POD complete, payout; token verified in an Edge Function. Riders on rooted/emulated/tampered devices cannot go online.
- **Fake-GPS protection:** reject locations where `Location.isMock` (API 31+) / `isFromMockProvider` is true; server also rejects impossible speed jumps.
- `FLAG_SECURE` on OTP, payment, wallet, and payout screens. **Biometric prompt** (`androidx.biometric`) before payouts, settlement, and wallet top-up recording in the business app.
- R8 **full mode**, obfuscation, strip logs in release; `android:allowBackup="false"` / data-extraction rules exclude tokens and databases; all components `exported=false` unless required; deep links via **verified Android App Links** (`assetlinks.json`) and every deep-link parameter validated; payments only in **Custom Tabs**, never WebView.
- Input validation on both client and server; strip EXIF from uploaded photos (POD location is stored in its own fields, not in the image).
- No secrets in the APK beyond the restricted Maps key and Supabase anon key; keys in `local.properties`/CI secrets, never committed.

**Privacy (Philippine Data Privacy Act 2012)**: consent screen, privacy policy, data-retention rules (rider location 30 days, KYC deleted 1 year after deactivation), in-app account deletion, data export on request.

**Security verification**: `/security-review` every phase; pgTAP RLS suite; **MobSF** static scan of release APK in Phase 7 (no High findings); dependency vulnerability scan in CI.

---

## 18. SUGGESTED CLAUDE CODE SKILLS, TOOLS & WORKFLOW

Set up once:
- For `admin-web/`: install PHP 8.3+, Composer, Node 22 LTS; use the **Laravel Boost** MCP server (official, gives the agent Laravel docs + app introspection) if available.
- Run **`/init`** after Phase 0 and keep `CLAUDE.md` in the repo root (a starter `CLAUDE.md` is included in this folder — keep its rules).
- Build on a computer with **Android Studio + Android SDK** installed (Claude Code CLI or desktop app on your own machine), so the agent can run Gradle, the emulator, and tests. Cloud sessions need a setup script that installs the Android command-line tools.
- Local backend needs **Docker Desktop + Supabase CLI + Deno**. The owner's machine is **Windows**: commands in this file are written POSIX-style (`./gradlew`); in PowerShell use `.\gradlew`. Keep every script cross-platform (Gradle tasks, Composer/npm scripts, or Deno — no bash-only scripts), commit `.gitattributes` so `gradlew` keeps LF line endings, and keep the project path short and free of spaces.
- Connect the **Supabase MCP server** (official) so the agent can apply migrations, run SQL, and read logs; connect **GitHub** for PRs and CI results. Optional: Figma MCP if you later make designs.

Per phase workflow:
0. Read `docs/PROGRESS.md` and `docs/OWNER_TODO.md`; re-read the sections of this file the phase touches (and look at the matching `reference/` image again).
1. **Plan mode** (or the `Plan` agent): write the phase plan into `docs/plans/phase-X.md` before coding.
2. Implement in small commits; use the **Explore** agent to search the codebase when it grows.
3. Run tests; use the **`run`** skill to launch the app and take screenshots for the phase report.
4. **`/simplify`** — clean up duplication and over-complex code.
5. **`/code-review high`** — fix every correctness finding.
6. **`/security-review`** — mandatory for Phases 1, 2, 3, 5, 7 (auth, licensing, payments, ledger, release); recommended for all.
7. Update `docs/PROGRESS.md`, commit + open PR (if a GitHub remote exists; otherwise commit on a `phase-X` branch and merge locally), check CI is green, then start the next phase.

---

## 19. QUALITY GATE (copy this checklist into every PHASE_X_REPORT.md)

- [ ] `./gradlew check` green (spotless, detekt, lint, unit tests, Kover ≥ 80 %, Roborazzi)
- [ ] pgTAP + Deno tests green; `supabase db lint` clean
- [ ] Phase acceptance checks (§13) all ✅ with evidence
- [ ] New screens have loading/empty/error states, dark mode, previews, screenshot tests, animations per §11A
- [ ] `/simplify`, `/code-review`, `/security-review` done; findings fixed or recorded in `docs/DECISIONS.md`
- [ ] No secrets committed (gitleaks clean); no money calculated on device
- [ ] Docs updated (`PROGRESS.md`, `OWNER_TODO.md`, `ARCHITECTURE.md`, `API.md`, ADRs, README developer setup)
- [ ] Every check above was really run in this phase; anything not run is listed as "not verified" with the reason

---

## 20. SUPER ADMIN WEB DASHBOARD — Laravel + Inertia.js + React (TypeScript)

**Goal:** the Platform Owner manages all provinces from a browser: every feature in §9.5 and `reference/workflows/02_Super_Admin_Workflow.png` (sidebar like the image: Dashboard, Franchises, Users, Stores, Riders, Orders, Payments, Reports, Support, Settings).

### 20.1 Stack (use Laravel's current official way — NOT Laravel Mix)
- Laravel Mix is legacy. Use **Laravel 12 (or newest stable) + Vite + Inertia.js v2 + React 19 + TypeScript**, started from the **official Laravel React starter kit** (`laravel new admin-web` → React). React code lives in **`admin-web/resources/js/`** — exactly the Laravel best practice the owner asked for. No separate SPA, no public API for the admin.
- UI: **Tailwind CSS v4 + shadcn/ui** (included in the starter kit), **lucide-react** icons, **TanStack Table** (sorting, filters, column visibility, server-side pagination), **Recharts** via shadcn charts, **@vis.gl/react-google-maps** for the live rider map, **sonner** toasts, **cmdk** command palette (Ctrl/⌘+K), **framer-motion** (`motion`) for subtle page/list animations, dark mode toggle.
- Typed routes: **Laravel Wayfinder** (or Ziggy) so React calls routes with types. Forms: Inertia `useForm` + Laravel **FormRequest** validation (server is the source of truth).
- Auth & security packages: starter-kit auth + **Laravel Fortify 2FA (TOTP + recovery codes)**, **spatie/laravel-permission** (roles/permissions), **spatie/laravel-activitylog** + DB `audit_logs`, **spatie/laravel-csp** (Content Security Policy).
- Exports: **maatwebsite/excel** (XLSX/CSV) and **spatie/laravel-pdf** or **barryvdh/laravel-dompdf** (PDF), run as **queued jobs**; Redis queue + **Laravel Horizon**; files stored privately with temporary signed download URLs.
- Testing & quality: **Pest** (feature + unit), **Larastan** (PHPStan level 8+), **Laravel Pint**, **Rector** (optional), **ESLint + Prettier**, **TypeScript strict**, **Vitest + React Testing Library**, **Playwright** E2E.

### 20.2 Connection to the ONE database (Supabase Postgres)
- Laravel connects **server-side only** to the same Supabase Postgres (`DB_CONNECTION=pgsql`, Supabase **session pooler / direct** host, `sslmode=require`). The browser never sees DB credentials or the Supabase service key.
- Use a **dedicated Postgres role `admin_web`** (created by a Supabase migration), **not** `postgres` and **not** `service_role`:
  - `SELECT` on the `public` tables the admin needs.
  - **No direct INSERT/UPDATE/DELETE** on money, ledger, order-status, license or wallet tables.
  - All admin changes go through **SQL functions in an `admin_api` schema** (e.g. `admin_api.approve_operator(actor_id, application_id)`, `admin_api.resolve_dispute(...)`, `admin_api.update_platform_settings(...)`), `SECURITY DEFINER`, `SET search_path = ''`, which check the actor is an active admin with the right permission, write `audit_logs`, and reuse the same ledger posting functions as the apps. Same rules everywhere → admin web and Android apps always agree.
- **Schema ownership rule:** the `public` schema is owned by `supabase/migrations` only. Laravel's own tables (admins, sessions, cache, jobs, failed_jobs, permissions, activity log) live in a separate **`admin` schema** (`search_path=admin` for Laravel migrations). Laravel migrations must **never** create/alter tables in `public`.
- Eloquent models for `public` tables are **read models** (`$guarded`, no `save()` on protected tables — enforce with a base `ReadOnlyModel` that throws on write). Use `DB::select('select admin_api.fn(...)')` wrapped in Action classes for writes.
- Admin users: `admin.admins` table linked to a Supabase `auth.users` id (for audit and so admin actions show a real actor). Admin accounts are created only by a Super Admin (no public registration — remove the starter kit's register route).
- Notifications to app users (approval, dispute result) go through the existing **`notify` Edge Function** (called from a queued job with a server-side secret), so push logic stays in one place.

### 20.3 Code structure (`admin-web/`)
```
app/
  Actions/            (one class per admin action, calls admin_api.* functions)
  Http/Controllers/   (thin: authorize → call Action → return Inertia::render / redirect)
  Http/Requests/      (FormRequest validation)
  Models/ReadModels/  (read-only Eloquent for public schema)  Models/Admin.php
  Policies/  Queries/ (report & dashboard queries, cached)  Jobs/ (exports, notify)
  Support/Money.php   (centavos ↔ "₱1,234.00", never floats)
resources/js/
  app.tsx  ssr.tsx
  layouts/AdminLayout.tsx   (sidebar, topbar, breadcrumbs, command palette)
  pages/  Dashboard/ Franchises/ Users/ Stores/ Riders/ Orders/ Payments/ Reports/ Support/ Settings/ Audit/
  components/ui/ (shadcn)  components/data-table/  components/charts/  components/kpi-card.tsx
  hooks/  lib/ (money.ts, date.ts Asia/Manila)  types/ (generated + shared DTOs)
routes/web.php  (all routes behind auth + 2FA + permission middleware)
tests/Feature  tests/Unit   tests/e2e (Playwright)
```
Rules: controllers ≤ ~30 lines; business logic in Actions/DB functions; TypeScript `strict`, no `any`; every page has loading (Inertia **deferred props** + skeletons), empty, and error states.

### 20.4 Pages & features (map to the image's 7 steps + support)
1. **Login & access:** email + password, **2FA required** (TOTP; recovery codes), optional IP allowlist, session timeout 30 min idle, "log out other devices", login history.
2. **Dashboard:** KPI cards (Total Orders, Active Stores, Active Riders, Active Provinces, Gross Sales, Platform Revenue), **Order Trend 30 days** line chart, **Payment Summary** donut (Online / COD / Others + total ₱), license expiries coming up, platform health & alerts (failed webhooks, overdue remittances, stuck orders). Auto-refresh with Inertia v2 **`usePoll`** (15 s).
3. **Franchises & users:** province list (82 + NCR) with license status, operator applications (review docs, approve/reject), assign/renew/suspend license, record license payments; users search (customers, riders, stores, operators), suspend/reactivate, admin staff & permissions.
4. **Approvals:** store and rider applications across provinces, document viewer (signed URLs), approve/suspend/deactivate with reason (override operator).
5. **Orders & payments:** filterable order table, order detail with full timeline (`order_events`), POD photo, map of route; **live rider map** per province (poll 10 s); online payments (GCash/Maya/Card) and **webhook log** with replay; COD collection & **remittance** status per province/rider.
6. **Analytics & reports:** orders, revenue, commissions, province/franchise performance, store & rider productivity; date range + province filters; **Export Excel/PDF** (queued, emailed/downloadable).
7. **Platform settings:** license fee, default store fee, commission caps, default rate card, service coverage (enable provinces), payment gateway toggles, notification templates, feature flags, minimum app versions — every change audited with before/after.
8. **Support & disputes:** ticket inbox (customer/store/rider), conversation thread, linked order investigation view, resolution actions **refund / adjustment / penalty** (calls `admin_api.resolve_dispute` → ledger), dispute history log.
9. **Audit log viewer:** who, what, when, IP, before/after diff, filter by admin/entity.

### 20.5 Premium admin UI/UX
- Same brand as the apps: primary `#F2014F`, navy `#05153C` sidebar, shadcn theme tokens (CSS variables) for light/dark; Poppins/Montserrat headings, Inter body.
- Collapsible sidebar, breadcrumbs, **⌘K command palette** (jump to order/store/rider by code), keyboard shortcuts, sticky table headers, saved filters in URL query, bulk actions with confirm dialogs, optimistic toasts.
- Motion: page fade/slide (150–250 ms), KPI count-up, chart draw-in, row highlight on update; respect `prefers-reduced-motion`.
- Skeleton loaders on every page (deferred props), empty states with illustration, error boundaries.
- Responsive down to tablet; usable on phone for emergencies.
- Accessibility: keyboard navigation, focus rings, ARIA from Radix/shadcn, contrast AA.

### 20.6 Security (admin web is the most powerful door — protect it most)
- No public registration; 2FA required; strong password rules; login **rate limiting** + lockout; session cookies `Secure`, `HttpOnly`, `SameSite=Lax`; session regenerate on login; CSRF (Laravel default).
- Every route behind `auth`, `verified-2fa`, and **permission middleware**; Policies on every action; test that a user without permission gets 403 for every route (generated test).
- **Security headers:** CSP (spatie/laravel-csp, no `unsafe-inline` scripts — use Vite nonce), HSTS, X-Frame-Options DENY, Referrer-Policy, Permissions-Policy.
- DB: `admin_web` least-privilege role, SSL to DB, writes only via `admin_api` functions; secrets only in `.env` on the server (never committed; `.env.example` only).
- Re-authenticate (password confirm + 2FA) before dangerous actions: refunds above a threshold, license changes, settings changes, admin role changes.
- Audit everything; alerts on suspicious activity (many failed logins, bulk exports).
- `composer audit` + `npm audit` in CI, Dependabot/Renovate, gitleaks.
- Deploy: HTTPS only (Laravel Cloud / Forge / Ploi on a VPS), `APP_DEBUG=false`, queue worker + Horizon protected by permission, daily backups of the `admin` schema (Supabase PITR covers the DB).

### 20.7 Admin web quality gate (added to §19 for Phases W1/W2)
- [ ] `./vendor/bin/pest` green (feature tests for every page + permission 403 tests), `phpstan` level 8+ clean, `pint --test` clean
- [ ] `npm run lint`, `npm run types` (tsc --noEmit), `npm run test` (Vitest) green; `npm run build` succeeds
- [ ] Playwright E2E: login with 2FA → approve operator → resolve dispute with refund → export report
- [ ] `/simplify`, `/code-review`, `/security-review` done on `admin-web/`
- [ ] CI job `admin-web` runs all of the above on every PR

---

**Build order: Phase 0 → 1 → 2 → W1 → 3 → 4 → 5 → 6 → W2 → 7. Start with Phase 0, or with the phase named in `docs/PROGRESS.md` if it exists. After each phase, print a short summary (what was built, how to test it, open `OWNER_TODO` items) and continue to the next phase. Stop only when the Quality Gate cannot pass or when everything left is waiting for the owner (rule 10).**
