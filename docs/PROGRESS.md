# Progress

A new session reads this first and continues from **Next**. Never restart a finished phase.

## Current phase: 1 — Database, auth, roles (not started)

## Done
- **Phase 0 — Foundation** ✅ (2026-11-25). Report: `docs/PHASE_0_REPORT.md`. Merged into `main` locally.

## Next
1. Phase 1 plan → `docs/plans/phase-1.md` (MASTER_PROMPT §13 Phase 1, §5, §8, §17).
2. Migrations from §8 with RLS + pgTAP; JWT role claims; PSGC + demo seed; move test OTPs to a local-only override
   (DECISIONS 25).
3. Real auth flows in the apps: phone OTP (+ SMS User Consent), email, Google via Credential Manager with per-app
   redirect schemes and `handleDeeplinks`; role routing in the business app.

## Blocked / waiting for owner (docs/OWNER_TODO.md)
- Docker Desktop + Supabase CLI + Deno not installed → migrations and pgTAP can be written but not run locally.
- No GitHub repository → CI not run.
