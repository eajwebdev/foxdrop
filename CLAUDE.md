# FoxDrop – rules for the AI agent (always loaded)

The full spec is in `MASTER_PROMPT.md`. Business source of truth is the images in `reference/`. These rules always apply:

- Native Android only: Kotlin + Jetpack Compose + Material 3. Clean Architecture + MVI, Hilt, Coroutines/Flow, multi-module with `build-logic` convention plugins. The apps talk to the FoxDrop API with Ktor; no backend SDK.
- Backend: `backend/` — Laravel 13 (PHP 8.4) + PostgreSQL 17 + PostGIS, API under `/api/v1` with Sanctum tokens, Reverb for realtime, queues + scheduler for jobs. Follow `backend/CLAUDE.md` (Laravel Boost guidelines) inside `backend/`. Schema changes only via new, forward-only migrations (`backend/database/schema/*.sql` run by migrations for rules Laravel's schema builder cannot express).
- Money is `Long` centavos (PHP `int`). All prices, fees, distance, wallet and order-status changes are computed on the server. Ledger entries are never edited or deleted, and each transaction sums to zero (enforced by the database).
- Every provincial row has `province_id`; Laravel policies and `visibleTo()` scopes isolate provinces, and Pest tests on real PostgreSQL prove it.
- Never commit secrets. The Android apps hold only the API base URL, the restricted Maps key and the Google web client ID.
- Every screen: loading skeleton, empty state, error + retry, dark mode, previews, screenshot test, animations from §11A using `FoxMotion` tokens, respects "Remove animations".
- Super Admin = web dashboard inside `backend/` (Phase W1): Inertia.js v3 (current starter kit, DECISIONS 71) + React + TypeScript in `backend/resources/js`, shadcn/ui + Tailwind, Vite (NOT Laravel Mix). Admin changes go through Laravel Actions with policies and an audit log. 2FA required.
- Brand: pink `#F2014F`, navy `#05153C`. Logos in `reference/logos/`.
- Work phase by phase (§13). End every phase with the Quality Gate (§19): `./gradlew check`, backend Pest + PHPStan + Pint, `/simplify`, `/code-review`, `/security-review`, phase report, commit/PR.
- When unsure, use the defaults in §14, record the choice in `docs/DECISIONS.md`, and continue.

## Commands
- Local backend tools (Windows, portable, no Docker): `. .\tools\dev\backend-env.ps1` puts PHP 8.4 and PostgreSQL 17 on `PATH` and starts the database.
- Backend (in `backend/`): `php artisan serve`, `vendor/bin/pest`, `vendor/bin/phpstan analyse`, `vendor/bin/pint --test`, `php artisan migrate:fresh --seed --drop-types --drop-views` (local demo data; sign in with `+63 917 000 0001…0009`, code `123456`)
- Build & checks: `./gradlew check`
- Unit tests: `./gradlew testDebugUnitTest`
- Screenshot tests: `./gradlew verifyRoborazziDebug` (record: `recordRoborazziDebug`)
- Apps → local API: `FOXDROP_API_URL=http://10.0.2.2:8000/api/v1/` in `local.properties`
