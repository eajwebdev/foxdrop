# Progress

A new session reads this first and continues from **Next**. Never restart a finished phase.

## Current phase: 3 — Catalog & customer ordering (not started)

## Done
- **Phase 0 — Foundation** ✅ (2026-11-25). Report: `docs/PHASE_0_REPORT.md`.
- **Phase 1 — Database, auth, roles** ✅ (2026-11-25). Report: `docs/PHASE_1_REPORT.md`.
- **Phase 1b — backend on Laravel + PostgreSQL** ✅ (2026-11-26). Report: `docs/PHASE_1B_REPORT.md`.
- **Phase 2 — Operator licensing & partner onboarding** ✅ (2026-11-26). Report: `docs/PHASE_2_REPORT.md`.
- **Phase W1 — Super Admin web foundation** ✅ (2026-11-26). Report: `docs/PHASE_W1_REPORT.md`.
  All merged into `main` locally.

## How to run things
See `docs/DEVELOPMENT.md`:
1. Run `. .\tools\dev\backend-env.ps1`.
2. In `backend/`, run `php artisan serve --host=127.0.0.1`.
3. The apps use `FOXDROP_API_URL=http://10.0.2.2:8000/api/v1/`.

Admin web: in `backend/` run `npm install` and `npm run build`, then open http://127.0.0.1:8000/admin
(`admin@foxdrop.local` / `foxdrop-local-admin`; set up 2FA at first sign-in).

## Next
1. Write the Phase 3 plan in `docs/plans/phase-3.md` (MASTER_PROMPT §13 Phase 3, §9.1, §9.3, §6, §7).
   Phase 3 covers:
   - store catalog CRUD
   - customer browse, search and categories
   - cart and fulfillment choice
   - server-side `quote` with the Routes API
   - checkout with COD and online payment (PayMongo + webhook)
   - store incoming-order alarm with accept/reject
   - pick-up flow with a code
2. The Google Maps / Routes API key is needed for real distances (OWNER_TODO). Until then, use a straight-line
   distance fallback behind the same interface.

## Blocked / waiting for owner (docs/OWNER_TODO.md)
- There is no GitHub repository, so CI has not run.
- PayMongo test keys (real checkout), the Google OAuth client, a real SMS provider and a Maps key (Phase 3).
