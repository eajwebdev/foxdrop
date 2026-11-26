# Progress

A new session reads this first and continues from **Next**. Never restart a finished phase.

## Current phase: 2 — Operator & licensing & partner onboarding (not started)

## Done
- **Phase 0 — Foundation** ✅ (2026-11-25). Report: `docs/PHASE_0_REPORT.md`.
- **Phase 1 — Database, auth, roles** ✅ (2026-11-25). Report: `docs/PHASE_1_REPORT.md`.
- **Phase 1b — backend moved to Laravel + PostgreSQL** ✅ (2026-11-26). Report: `docs/PHASE_1B_REPORT.md`.
  All merged into `main` locally.

## How to run things
See `docs/DEVELOPMENT.md`:
1. Run `. .\tools\dev\backend-env.ps1` (portable PHP 8.4 + PostgreSQL 17 in `D:\devtools`).
2. Run `php artisan serve --host=127.0.0.1` in `backend/`.
3. The apps use `FOXDROP_API_URL=http://10.0.2.2:8000/api/v1/`.

## Next
1. Write the Phase 2 plan in `docs/plans/phase-2.md` (MASTER_PROMPT §13 Phase 2, §4, §5.1, §6, §17). It covers:
   - operator application and license payment (PayMongo test mode)
   - license states and renewal
   - zone map editor and rate card editor
   - store and rider applications with document upload, plus the approval flows
   - admin approval Actions with Pest tests
2. Rules that apply from now on:
   - Every endpoint that reads provincial rows uses `visibleTo($user)` and gets a Pest test proving that another
     province sees 0 rows (DECISIONS 52).
   - Flows that grant a role refresh the user's roles (`/me`).
   - Introduce one error envelope `{message, code, errors}` and the sign-in Action family (PHASE_1B_REPORT "left
     for later").

## Blocked / waiting for owner (docs/OWNER_TODO.md)
- There is no GitHub repository, so CI has not run.
- Real SMS provider and Google OAuth client IDs (before real users test).
