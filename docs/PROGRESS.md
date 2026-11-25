# Progress

A new session reads this first and continues from **Next**. Never restart a finished phase.

## Current phase: 0 — Foundation (Quality Gate in progress)

Branch: `phase-0` (no GitHub remote yet; commits stay local until the owner creates a repository).

### Done
- Gradle multi-module project, `build-logic` convention plugins, pinned version catalog (toolchain: docs/DECISIONS.md 1–4).
- 3 apps with animated splash, first-run intro, onboarding, location explainer, animated Log In ⇄ Sign Up + phone OTP UI,
  adaptive home shells. Installed and walked through on the emulator.
- Design system (theme, tokens, FoxMotion with "Remove animations", components, previews).
- Encrypted session storage (DataStore + Tink + Keystore) behind supabase-kt; app runs without a backend configured.
- Icons and splash vector generated from `reference/logos`.
- `supabase init` (CLI 3.4 → `supabase/config.jsonc`) with test OTPs.
- CI workflows written (android, backend, security) — not run yet (no remote).
- `./gradlew check` green; `verifyRoborazziDebug` green; 77 tests; merged domain/data coverage 90.8 %.
- Feature → feature dependency rule proven to fail the build.

### Next
1. Finish the Phase 0 Quality Gate: apply `/simplify` fixes, `/code-review high`, `/security-review`, re-run checks,
   write `docs/PHASE_0_REPORT.md`, commit on `phase-0`, merge to `main`.
2. Phase 1 — Database, auth, roles (needs Docker + Supabase CLI from docs/OWNER_TODO.md to verify locally; migrations
   and pgTAP tests can be written before that).

### Blocked / waiting for owner
- Docker Desktop + Supabase CLI + Deno not installed → cannot run `supabase start` / pgTAP locally.
- No GitHub repository → CI not run.
