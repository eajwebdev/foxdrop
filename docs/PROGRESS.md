# Progress

A new session reads this first and continues from **Next**. Never restart a finished phase.

## Current phase: W1 — Super Admin web foundation (not started)

## Done
- **Phase 0 — Foundation** ✅ (2026-11-25). Report: `docs/PHASE_0_REPORT.md`.
- **Phase 1 — Database, auth, roles** ✅ (2026-11-25). Report: `docs/PHASE_1_REPORT.md`.
- **Phase 1b — backend on Laravel + PostgreSQL** ✅ (2026-11-26). Report: `docs/PHASE_1B_REPORT.md`.
- **Phase 2 — Operator licensing & partner onboarding** ✅ (2026-11-26). Report: `docs/PHASE_2_REPORT.md`.
  All merged into `main` locally.

## How to run things
See `docs/DEVELOPMENT.md`:
1. Run `. .\tools\dev\backend-env.ps1`.
2. In `backend/`, run `php artisan serve --host=127.0.0.1`.
3. The apps use `FOXDROP_API_URL=http://10.0.2.2:8000/api/v1/`.

Admin approval of operator applications until W1: `php artisan foxdrop:operator-application approve|reject <id>`.

## Next
1. Write the Phase W1 plan in `docs/plans/phase-w1.md` (MASTER_PROMPT §13 W1, §20). It covers:
   - the Inertia v2 + React + TypeScript admin inside `backend/` (`resources/js`), with shadcn/ui + Tailwind
   - admin login with required 2FA (TOTP)
   - roles and permissions, the audit log viewer, and the layout shell
   - operator applications (approve / reject using the same Action as the Artisan command)
   - province licenses (assign / renew / suspend)
   - store and rider approvals with override
   - users management
2. Install Node.js packages for the admin web (npm is available). Laravel's React starter kit or Inertia manual
   setup, Vite.

## Blocked / waiting for owner (docs/OWNER_TODO.md)
- There is no GitHub repository, so CI has not run.
- PayMongo test keys (real checkout), the Google OAuth client, a real SMS provider and a Maps key (Phase 3).
