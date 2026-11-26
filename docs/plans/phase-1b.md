# Phase 1b plan — move the backend from Supabase to Laravel

Owner decision (2026-11-26): self-host one Laravel backend (API + admin web) with PostgreSQL on a Hostinger VPS
later, instead of Supabase. Done now because Phase 1 was the only phase built on Supabase.

## Scope
1. **Local tools** without Docker or admin rights: portable PHP 8.4, PostgreSQL 17 + PostGIS in `D:\devtools`
   (`docs/DEVELOPMENT.md`, `tools/dev/backend-env.ps1`).
2. **`backend/`**: Laravel 13, Sanctum, Reverb (configured, used from Phase 3), Pest, Larastan level 8, Pint,
   Laravel Boost.
3. **Schema**: port `supabase/migrations` to `backend/database/schema/*.sql`, run by migrations. Keep every database
   rule; drop the Supabase-only parts (auth schema, RLS, grants, Vault, Storage, access-token hook).
4. **Authorization**: `visibleTo(User)` scopes per provincial model; port the pgTAP tests to Pest (isolation,
   license exclusivity, ledger rules).
5. **API v1**: phone OTP send/verify, email register/login, Google ID-token sign-in, logout, `/me` with roles,
   public province directory. Rate limits on every sign-in route.
6. **Seed**: 83 PSGC provinces (all environments); demo data (local/testing only) equal to Phase 1's.
7. **Android**: replace supabase-kt with a Ktor client; encrypted session store; roles from `/me` with an offline
   copy; global `401` → sign-out; remove the deep-link redirect handling.
8. **Docs and CI**: MASTER_PROMPT, CLAUDE.md, README, ARCHITECTURE, DECISIONS 36–47, OWNER_TODO, backend CI job.

## Acceptance
- Migrations and seed run on a real PostgreSQL + PostGIS; `migrate:fresh` works repeatedly.
- Pest: operator of province A reads 0 rows of province B (orders, stores, riders, wallets, licenses, roles); a
  second live license fails; unbalanced or edited ledger rows fail; each demo account gets the right roles.
- An app signs in against the local API on the emulator and lands on the right home; sign-out deletes the token.
- `./gradlew check verifyRoborazziDebug`, `pest`, `phpstan`, `pint --test` green; `/simplify`, `/code-review`,
  `/security-review` done.
