# Progress

A new session reads this first and continues from **Next**. Never restart a finished phase.

## Current phase: 2 — Operator & licensing & partner onboarding (not started)

## Done
- **Phase 0 — Foundation** ✅ (2026-11-25). Report: `docs/PHASE_0_REPORT.md`. Merged into `main` locally.
- **Phase 1 — Database, auth, roles** ✅ for the code and Android checks (2026-11-25). Report: `docs/PHASE_1_REPORT.md`.
  Merged into `main` locally. **Database checks are still open:** migrations, seed and pgTAP were parsed but have
  never run (no Docker yet).

## Next
1. **As soon as Docker + the Supabase CLI are installed:** run `supabase start`, `supabase db reset` and
   `supabase test db`. Fix anything they report **before** building on the schema, then tick the pgTAP items in
   PHASE_1_REPORT.
2. Write the Phase 2 plan in `docs/plans/phase-2.md` (MASTER_PROMPT §13 Phase 2, §4, §5.1, §6). It covers:
   - operator application and license payment (PayMongo test mode)
   - license states and renewal
   - zone map editor and rate card editor
   - store and rider applications with document upload, plus the approval flows
   - `admin_api.*` functions with pgTAP tests
   - any flow that grants a role must refresh the user's session, because routing reads the JWT claim
     (DECISIONS 34)
3. Carry-overs from the Phase 1 report: make `support_tickets.province_id` server-derived (Phase 6), plan the
   per-province platform/system wallets (Phase 5), and add one global session-expiry handler (Phase 2).

## Blocked / waiting for owner (docs/OWNER_TODO.md)
- Docker Desktop, the Supabase CLI and Deno are not installed, so migrations, pgTAP and Edge Functions cannot run
  locally.
- There is no GitHub repository, so CI has not run.
- The Google OAuth client IDs are needed for real Google sign-in.
