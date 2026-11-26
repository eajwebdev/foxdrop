# Changelog

All notable changes, per phase. Format: Keep a Changelog; commits follow Conventional Commits.

## [Unreleased] — Phase 1b: backend on Laravel + PostgreSQL

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
