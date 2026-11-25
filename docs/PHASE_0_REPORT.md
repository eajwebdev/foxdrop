# Phase 0 report — Foundation

Date: 2026-11-25 · Branch: `phase-0` (merged into `main` locally; no GitHub remote yet)

## What was built
- Gradle multi-module project: 3 apps, 8 core modules, 2 feature modules, `build-logic` convention plugins,
  pinned version catalog. Toolchain AGP 9.3.3 / Gradle 9.7.1 / Kotlin 2.4.21, compile/target SDK 37 (DECISIONS 1–4).
- Animated splash (traced fox vector, exit animation), first-run intro, 3-page onboarding per app, location explainer.
- Animated Log In ⇄ Sign Up (shared elements, sliding pill tabs, staggered fields), phone OTP step (6 boxes, shake on
  wrong code, green on success, resend countdown, FLAG_SECURE), email sign-in/sign-up, Google button with the
  official "G" (Google sign-in itself arrives in Phase 1).
- Design system: brand light/dark theme, Poppins, tokens, `FoxMotion` honouring "Remove animations", flat `FoxCard`
  (no shadows, soft border), buttons with loading → ✓, skeletons, empty/error states, adaptive navigation shell.
- Encrypted session storage (DataStore + Tink + Android Keystore) behind supabase-kt; the app runs safely without a
  backend configured.
- Icons for all three apps generated from `reference/logos`; Supabase local config; CI workflows; docs.

## How to test
1. `.\gradlew :app-customer:installDebug` (also `:app-rider`, `:app-business`) on an emulator or phone.
2. First launch: splash → intro (tap to skip) → 3 pages → location explainer (customer/rider) → Log In.
3. Type `0917 123 4567` → "Send code". Without `local.properties` keys the app shows "not connected to its server yet".
4. Toggle Log In ⇄ Sign Up, Mobile ⇄ Email, dark mode, and Settings › Accessibility › Remove animations.

## Quality Gate (§19)
- [x] `./gradlew check` green — Spotless/ktlint, detekt + Compose rules, Android Lint (warnings as errors),
      81 unit/ViewModel/screenshot tests, Kover merged domain/data/common/model **90.8 %** (gate 80 %)
- [x] `./gradlew verifyRoborazziDebug` green — 51 baselines (light/dark, phone/tablet, font 1.5)
- [ ] pgTAP + Deno tests; `supabase db lint` — **not verified**: no migrations/functions yet, and Docker is not
      installed on this PC (docs/OWNER_TODO.md)
- [x] Phase acceptance checks:
  - [x] All 3 apps launch to a branded animated splash + login; dark/light readable — installed and walked through on
        the emulator (Medium Phone API 36.1) and covered by screenshot tests
  - [x] `./gradlew check` green on this checkout
  - [x] A `feature → feature` dependency fails the build (`:feature:onboarding -> :feature:auth` →
        "Dependency rule broken"); the rule now also blocks core → feature/app
- [x] New screens have loading/empty/error states, dark mode, previews, screenshot tests, §11A animations
- [x] `/simplify` (4 reviewers; fixes applied, skips listed below), `/code-review high` (10 findings, all fixed),
      `/security-review` (no findings at the reporting bar; one informational item recorded as DECISIONS 25)
- [x] No secrets committed; no money calculated on device
- [x] Docs updated: PROGRESS, OWNER_TODO, DECISIONS, ARCHITECTURE, CHANGELOG, README developer setup
- [x] Every check above was run in this phase, except those marked "not verified"

## Not verified (and why)
- **CI workflows** (`.github/workflows/*`) — no GitHub remote yet.
- **Real sign-in end to end** — needs the local Supabase stack (Docker) or a hosted project; Phase 1.
- **Cold start < 1.5 s** — the emulator was CPU-starved (system apps also stalled); measure in Phase 7 on a
  release build with a baseline profile on a real mid-range phone.
- **Linux CI screenshot parity** — baselines were recorded on Windows; re-record on CI if it reports small diffs.

## Review items deliberately left for later
- Shared "app shell" module for the three near-identical activities/NavHosts (when home screens grow).
- Shared JVM test fakes (test fixtures) for pure-Kotlin modules.
- One shared module for the per-app security XML (with certificate pinning, Phase 7).
- Font subsetting; `FoxButton` width animation without subcomposition.
- Per-app redirect schemes + `handleDeeplinks` for email confirmation / Google sign-in (Phase 1).
