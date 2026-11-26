# Phase 1b report — backend moved to Laravel + PostgreSQL

Date: 2026-11-26 · Branch: `phase-1b` (merged into `main` locally; no GitHub remote yet)

## Why
The owner chose to host one backend on a Hostinger VPS instead of Supabase (DECISIONS 36). Phase 1 was the only phase
built on Supabase, so it was moved now, before anything else depends on it.

## What was built
- **Local tools without Docker or admin rights**: portable PHP 8.4, PostgreSQL 17 + PostGIS 3.7 in `D:\devtools`
  (`docs/DEVELOPMENT.md`, `tools/dev/backend-env.ps1`).
- **`backend/`**: Laravel 13 with Sanctum, Reverb (configured for Phase 3/4), Pest, Larastan level 8, Pint and
  Laravel Boost.
- **Schema**: the Phase 1 schema now runs on a real PostgreSQL database for the first time. It is in
  `backend/database/schema/*.sql`, loaded by Laravel migrations. Every database rule is kept:
  - checks and enums
  - province-tying foreign keys
  - append-only ledger, with the zero-sum check
  - one live license per province

  Two rules were added after review: ledger entries must match their wallet's province, and favourites and reviews
  are tied to their province.
- **API v1**:
  - phone OTP send/verify: only a hash of the code is stored; codes expire after 5 minutes, allow 5 attempts, work
    once, and a new one can be sent after 60 s
  - email register/login
  - Google ID-token sign-in: verified with Google's keys, audience, nonce, one use per token, and safe account linking
  - logout
  - `/me` with roles
  - public province directory
  - rate limits per phone/email, loose per IP
- **Province isolation** in Laravel: one `VisibleToViewer` trait and a `visibilityRule` on each provincial model.
- **Seed**: 83 PSGC provinces in every environment. The same demo data as Phase 1 in local/testing only.
- **Android**:
  - a Ktor client to the API, with the token stored encrypted
  - a `401` signs out only the session the request was sent with
  - roles come from the saved copy first, then `/me`, so the app opens to its home instantly and offline
  - the deep-link redirect machinery was removed
- **Docs and CI**:
  - MASTER_PROMPT, CLAUDE.md, README, ARCHITECTURE, DEVELOPMENT, OWNER_TODO and DECISIONS 36–54 updated
  - the backend CI job runs Pint, Larastan, Pest on a PostGIS service, and `composer audit`
  - `supabase/` was deleted

## How to test
1. `. .\tools\dev\backend-env.ps1`, then in `backend/` run
   `php artisan migrate:fresh --seed --drop-types --drop-views` and `php artisan serve --host=127.0.0.1`.
2. `local.properties`: `FOXDROP_API_URL=http://10.0.2.2:8000/api/v1/`. Install an app, e.g.
   `.\gradlew :app-business:installDebug`.
3. Sign in with `0917 000 0006` (operator) or `0917 000 0007` (store owner), code `123456`. You should land on the
   operator home or the store home.
4. Turn on airplane mode and reopen the app: it still opens to the same home. Turn airplane mode off, open
   Account › Sign out: you return to login, and the token is deleted on the server.

## Quality Gate (§19)
- [x] **Backend** (`backend/`):
  - `vendor/bin/pest`: **79 passed**. Tests run on real PostgreSQL + PostGIS and cover:
    - phone OTP, email, Google (including real RS256 tokens and key rotation), sessions
    - province isolation, ported from the Phase 1 pgTAP suite
    - license exclusivity, ledger rules, province-tying rules
  - `vendor/bin/phpstan analyse` (level 8): **0 errors**
  - `vendor/bin/pint --test`: **passed**
- [x] `./gradlew check verifyRoborazziDebug`: **green**, with **121** unit, ViewModel and screenshot tests. Kover
      coverage of domain/data/common/model: **91.7 %** (gate 80 %).
- [x] **Migrations and seed**: `migrate:fresh --seed` runs repeatedly on PostgreSQL 17 + PostGIS 3.7.
- [x] **Phase acceptance**:
  - [x] An operator of province A reads 0 rows of province B (orders, stores, riders, wallets, licenses, roles).
        Proven by Pest; this was "not verified" in Phase 1.
  - [x] A second live license for a province fails.
  - [x] Unbalanced or edited ledger rows fail.
  - [x] Each demo account gets the right home. Checked on the emulator against the real local API for the operator
        (0006) and the store owner (0007), including offline and sign-out. The other accounts are covered by
        `ResolveHomeUseCaseTest` and the backend `/me` tests.
- [x] **Reviews**:
  - `/code-review high`: 9 real findings, all fixed. 1 false positive (it read the pre-branch `CLAUDE.md`).
  - `/simplify`: 4 reviewers; fixes applied, skipped items listed below. It also found a real bug: every Google
    sign-in shared one rate-limit bucket. Fixed, with a test.
  - `/security-review`: no findings at the bar. One deploy note was recorded in DECISIONS 48.
- [x] No secrets committed (`.env` is ignored; `.env.example` has empty keys). No money is calculated on the device.
- [x] Every check above was run in this phase.

## Not verified (and why)
- **CI workflows**: there is no GitHub remote yet.
- **Real SMS and real Google sign-in**: these need an SMS provider and a Google OAuth client (OWNER_TODO). Locally,
  codes are fixed for the demo numbers.
- **Linux CI screenshot parity**: unchanged from Phase 0.

## Review items deliberately left for later
- **Sign-in Actions**: one `ResolveUserByIdentity` / sign-in Action family per method, with the suspension check
  before any side effects. Phase 2, when the admin web needs the same logic.
- **One error envelope** (`{message, code, errors}`) mapped by code on Android. Phase 2, with the first domain
  errors.
- **Server-driven resend countdown**: the app uses the same 60 s constant as the server (DECISIONS 53).
- **Single source for the bearer token**: the bearer header and the 401 check read the session separately. A small
  gap remains; revisit if token refresh arrives.
- **Small cleanups**: deterministic IDs for every demo row; a shared `device_name` rule.
- **Redis** for the cache and rate limits on the VPS (DECISIONS 53). Hot platform wallets per province (Phase 5,
  from the Phase 1 report).
