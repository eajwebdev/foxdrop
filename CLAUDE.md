# FoxDrop – rules for the AI agent (always loaded)

The full spec is in `MASTER_PROMPT.md`. Business source of truth is the images in `reference/`. These rules always apply:

- Native Android only: Kotlin + Jetpack Compose + Material 3. Clean Architecture + MVI, Hilt, Coroutines/Flow, multi-module with `build-logic` convention plugins.
- Backend: Supabase (Postgres + PostGIS + RLS + Realtime + Edge Functions). Schema changes only via numbered migrations in `supabase/migrations`.
- Money is `Long` centavos. All prices, fees, distance, wallet and order-status changes are computed on the server. Ledger entries are never edited or deleted, and each transaction sums to zero.
- Every provincial row has `province_id`; RLS must isolate provinces (pgTAP proves it).
- Never commit secrets. Only the Supabase anon key and restricted Maps key may be in the app.
- Every screen: loading skeleton, empty state, error + retry, dark mode, previews, screenshot test, animations from §11A using `FoxMotion` tokens, respects "Remove animations".
- Super Admin = web dashboard in `admin-web/`: Laravel + Vite + Inertia.js v2 + React + TypeScript (React in `resources/js`), shadcn/ui + Tailwind. NOT Laravel Mix. Connects server-side to the same Supabase Postgres via the least-privilege `admin_web` role; writes only through `admin_api.*` SQL functions; Laravel tables only in the `admin` schema, never in `public`. 2FA required.
- Brand: pink `#F2014F`, navy `#05153C`. Logos in `reference/logos/`.
- Work phase by phase (§13). End every phase with the Quality Gate (§19): `./gradlew check`, backend tests, `/simplify`, `/code-review`, `/security-review`, phase report, commit/PR.
- When unsure, use the defaults in §14, record the choice in `docs/DECISIONS.md`, and continue.

## Commands
- Admin web (in `admin-web/`): `composer test` / `./vendor/bin/pest`, `./vendor/bin/phpstan analyse`, `./vendor/bin/pint --test`, `npm run lint`, `npm run types`, `npm run build`, `composer run dev`
- Build & checks: `./gradlew check`
- Unit tests: `./gradlew testDebugUnitTest`
- Screenshot tests: `./gradlew verifyRoborazziDebug` (record: `recordRoborazziDebug`)
- Backend: `supabase start`, `supabase db reset`, `supabase test db`, `deno test supabase/functions`
