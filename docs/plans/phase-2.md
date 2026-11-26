# Phase 2 plan — Operator & licensing & partner onboarding

Source: MASTER_PROMPT §13 Phase 2, §5.1–5.3, §6, §7 rule 7, §9.2 step 1, §9.4 steps 1–5 & 8, §10 (payments), §12,
§17; reference images `03_Provincial_Operator_Workflow.png` (steps 1–5, 8) and `04_Rider_Workflow.png` (step 1).
API contract: `docs/API.md` (Phase 2 section). Decisions: DECISIONS 55–64.

## Flows
1. **Operator** (business app, any signed-in user):
   - Apply for an **available** province (no live license) with business details and documents. The license is
     created as `APPLIED`, which blocks the province for others.
   - Admin approves: the license becomes `PENDING_PAYMENT`. Until Phase W1 this is
     `php artisan foxdrop:operator-application approve <id>`; reject frees the province.
   - The applicant pays ₱15,000 through the checkout (PayMongo, or the local fake checkout). The license becomes
     **ACTIVE** with `end_at = now + 365 days`, the operator role is granted, and the operator wallets are created.
     The ledger records rule 7 (`GATEWAY_CLEARING −fee`, `PLATFORM_REVENUE +fee`).
   - **Renewal**: paying again while ACTIVE/GRACE extends the same license by 365 days from the later of now and
     `end_at`.
   - **Daily job**: ACTIVE past `end_at` → GRACE; GRACE past `end_at + 15 days` → EXPIRED.
2. **Territory** (operator):
   - Circle delivery zones (centre + radius, stored as a PostGIS polygon); the API also accepts polygons.
   - Versioned rate card with a server-computed live fee preview (the same `DeliveryFeeCalculator` the Phase 3
     quote will use).
   - Province store fee.
3. **Store application** (business app, any signed-in user in a serviceable province): store details + location
   (current GPS position) + business permit + valid ID → `SUBMITTED`. The operator (or an admin, overriding)
   approves → **ACTIVE**: visible to customers, `store_owner` role, store wallets. Or rejects → `REJECTED` with a
   reason; the owner can fix it and resubmit.
4. **Rider application** (rider app): province, vehicle, driver's license number (encrypted) + expiry, and photos
   (valid ID, driver's license, selfie; OR/CR and vehicle photo unless on a bicycle) → `DOCS_REVIEW`. The operator
   approves → **APPROVED** + `rider` role + rider wallets, or rejects with a reason (resubmit allowed).
5. **Role refresh**: onboarding status screens re-check `/me`. When a role appears, `RoleGate` re-routes to the new
   home.

## Backend (`backend/`)
- **Schema** `12_phase2_onboarding.sql`:
  - `store_documents`; `store_status` gains `REJECTED`; `stores.rejection_reason`
  - `province_licenses.paid_through` (helper), `payments.expires_at`
  - `delivery_zones.center` + `radius_m`
  - the `province_directory` view adds `is_available`
- **Payments**:
  - a `PaymentProvider` contract with two drivers: `PayMongoCheckout` (Checkout Sessions API, test keys from env)
    and `FakeCheckout` (local/testing only; web page `/dev/checkout/{payment}` with Pay / Cancel)
  - `POST /webhooks/paymongo`: signature verified (`Paymongo-Signature`, HMAC-SHA256), idempotent via
    `webhook_events`
  - `HandlePaymentSucceeded` dispatches on purpose (`LICENSE` now; `ORDER`/`TOPUP` later)
- **Ledger**: `PostLedgerTxn` Action (idempotency key, sums to zero, one DB transaction), used for rule 7.
- **Actions**: `SubmitOperatorApplication`, `ReviewOperatorApplication`, `StartLicensePayment`,
  `ActivateLicensePayment`, `AdvanceLicenseLifecycle`, `CreateDeliveryZone`, `PublishRateCard`, `SubmitStore`,
  `ReviewStore`, `SubmitRiderApplication`, `ReviewRider`, `GrantRole`, `RecordAudit`.
- **Policies**: an operator of the province, or an admin (override), may review; applicants see only their own.
- **Documents**: the private `kyc` disk (images only, ≤ 8 MB, re-encoded server-side to strip EXIF), served through
  10-minute signed URLs.
- **Commands**:
  - `foxdrop:operator-application {approve|reject} {id} [--reason=]` (admin, until W1)
  - `foxdrop:licenses:advance` (scheduled daily 00:05 Asia/Manila)
- **Tests (Pest)**, including all three acceptance checks.

## Android
- **core:payments**: open a checkout URL in a Custom Tab. **core:media**: pick a photo (Photo Picker) or take one
  (camera), compressed to JPEG ≤ 1600 px. **core:location**: one-shot current location (permission helpers).
- **feature:operator-license**: apply, application status, pay / renew with payment polling, license card (status,
  days left, renew).
- **feature:operator-zones-rates**: zone list, add a circle zone, rate card editor with live preview, store fee.
- **feature:operator-partners**: pending stores and riders; detail with document photos; approve / reject with a
  reason.
- **feature:store-onboarding**: store application form + status.
- **feature:rider-onboarding**: rider registration (personal details, valid ID, driver's license, vehicle details)
  + status (pending / approved / rejected with reason).
- **Business app**:
  - "not registered" → a **Get started** home: open a store / become a provincial operator, plus application
    statuses
  - the operator home tabs become: Dashboard (license card), Stores (approvals), Riders (approvals), Orders
    (Phase 3), Account (zones & rates, store fee, sign out)
- **Rider app**: "not registered" → rider registration.
- Every screen gets: skeleton, empty, error + retry, dark mode, previews, screenshot tests, FoxMotion.

## Acceptance (§13)
- Applying for an already-licensed province is impossible (409, Pest + app message).
- Paying the test license flips the license to ACTIVE with an end date +365 days (Pest through the webhook and the
  fake checkout; manual on the emulator).
- An approved store appears to customers (`/provinces` serviceable + `Store::published`); a rejected one does not
  (Pest).
