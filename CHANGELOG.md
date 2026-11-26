# Changelog

All notable changes, per phase. Format: Keep a Changelog; commits follow Conventional Commits.

## [Unreleased]

## Phase W1: Super Admin web foundation

### Added
- Admin web at `/admin` inside `backend/` (Inertia v3, React 19, TypeScript, Tailwind v4, shadcn/ui):
  - sign-in for admin accounts with required TOTP 2FA and recovery codes, and "confirm it's you" for dangerous actions
  - admin-staff permissions and the audit log viewer
  - franchises: provinces, license assign, offline payment, suspend / lift, operator applications
  - stores and riders across provinces, with the admin override
  - users (search, suspend / reactivate) and admin staff management
- Shared Actions and Laravel policies behind every admin change, a CSP with nonces and security headers, schema 13–15,
  and the `foxdrop:super-admin` command.
- The operator application status screen in the business app re-checks while waiting for the admin.
- Tests: 218 backend, Vitest, a Playwright E2E, and generated access tests (2FA required, 403 without permission).

### Security
- Admin accounts cannot use the app API. A password or 2FA change signs the account out everywhere.
- The super-admin command will not promote an account registered through the app.

## Phase 2: Operator licensing & partner onboarding

### Added
- Provincial operator license: apply for an available province (409 when taken), admin approval (Artisan command
  until the admin web), ₱15,000 checkout (PayMongo or local fake), ACTIVE for 365 days, renewal extends a year,
  daily ACTIVE → GRACE → EXPIRED job, license fee posted to the ledger.
- Operator home: license card, store and rider approval lists and review screens (documents, approve, reject with a
  reason), delivery zones (circles), rate card editor with a server-side fee preview, monthly store fee.
- Store application (business app) and rider registration (rider app) with KYC photos, status screens and
  fix-and-resend.
- Private KYC storage (re-encoded, size-capped, 10-minute signed links), signed idempotent PayMongo webhook,
  delivery fee calculator, audited Actions; 170 backend tests, 252 Android tests.

### Security
- An operator role counts only while the province's current license is theirs; reviews need an operating license;
  nobody reviews their own application; KYC photos are never cached on the device's disk.

## Phase 1b: backend on Laravel + PostgreSQL

### Changed
- Backend is `backend/`: Laravel 13 API (Sanctum tokens, rate limits) on PostgreSQL 17 + PostGIS, replacing Supabase.
  The Phase 1 schema and its database rules were ported; province isolation is enforced in Laravel and proven by Pest
  on a real database (79 backend tests).
- Android apps talk to the FoxDrop API with Ktor; roles open the right home instantly and offline; sign-up name is sent
  with the code.

### Removed
- `supabase/` (migrations, RLS, pgTAP, config), deep-link auth redirects, the email-confirmation sign-up path.

## Phase 1: Database, auth, roles

### Added
- Supabase schema for every §8 table (15 migrations): RLS enabled and forced on every table, province isolation,
  client writes limited to personal data, append-only zero-sum ledger, one live license per province, hidden
  secret columns, Vault-encrypted PII, storage buckets.
- `app_roles` JWT claim (access-token hook); PSGC province seed and Negros Occidental demo data; 43 pgTAP assertions
  (written, not yet run — no Docker).
- Role routing in all three apps (customer / rider / store / operator homes, "use the web dashboard" for admins),
  working offline from the token claim.
- Google sign-in via Credential Manager (nonce-checked), SMS code autofill (SMS User Consent), per-app auth
  redirects with PKCE.

### Changed
- `ErrorState` takes an optional secondary action (used for "Sign out").

## Phase 0: Foundation

### Added
- Gradle multi-module project with `build-logic` convention plugins and a pinned version catalog.
- Three apps (FoxDrop, FoxDrop Rider, FoxDrop Business): branded animated splash, first-run intro, onboarding,
  location explainer, animated Log In ⇄ Sign Up with phone OTP, adaptive home shells.
- Design system: brand theme (light/dark), Poppins, spacing/shape/motion tokens with "Remove animations" support,
  buttons with loading → ✓ morph, OTP field, skeletons, empty/error states, pill tabs, navigation shell.
- Encrypted session storage (DataStore + Tink + Android Keystore) behind supabase-kt.
- Launcher icons and splash vector generated from `reference/logos` (`tools/assets/make_assets.py`).
- Supabase local config with test OTPs; CI workflows for Android, backend and secret scanning.
- 77 unit/ViewModel/screenshot tests and 51 screenshot baselines.
