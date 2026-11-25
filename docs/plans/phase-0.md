# Phase 0 plan — Foundation

Source: MASTER_PROMPT.md §13 Phase 0, §3, §4, §11, §11A.1–11A.7, §16, §17 (Android storage/network), §19.

## Environment found (2026-11-25)

| Tool | Status |
|---|---|
| Android Studio 2025.3.1 (bundled JBR = JDK 21) | ✅ used as `JAVA_HOME` for Gradle |
| Android SDK: platforms 34/35/36/36.1, build-tools 36.1.0, emulator + AVD `Medium_Phone_API_36.1` | ✅ |
| SDK `cmdline-tools` (sdkmanager) | ❌ missing (not needed for Phase 0; AGP downloads what it needs) |
| Git 2.47 | ✅ |
| Node 22.13, Composer 2.8 | ✅ |
| PHP 8.2.12 (XAMPP) | ⚠️ admin-web needs PHP 8.3+ (Phase W1) |
| Python 3.13 | ✅ (icon/asset scripts) |
| Docker Desktop | ❌ missing → `supabase start`, `supabase test db` cannot run |
| Supabase CLI | ❌ not installed globally → use `npx supabase` for `init` only |
| Deno | ❌ missing (Edge Functions start in Phase 2/3) |
| Redis | ❌ missing (admin-web queues, Phase W1) |
| GitHub CLI `gh` / remote | ❌ no remote → commit locally on a `phase-0` branch |

## Versions (latest stable that are officially compatible with each other)

Kotlin 2.4.21 is tested with AGP ≤ 9.3.x and Gradle ≤ 9.7.x (kotlinlang.org compatibility table), so:
AGP **9.3.3**, Gradle **9.7.1**, Kotlin **2.4.21** (AGP built-in Kotlin + KGP on the build-logic classpath), KSP **2.3.14**,
Compose BOM **2026.11.01**, Hilt **2.60.1**, supabase-kt **3.9.0**, Ktor **3.6.1**, compileSdk/targetSdk **36**, minSdk **26**.
Full list in `gradle/libs.versions.toml`; recorded in `docs/DECISIONS.md`.

## Steps

1. **Repo hygiene:** `git init`, `.gitignore` (local.properties, .env, keystores, google-services.json, build output), `.gitattributes` (LF for `gradlew`, binary for PNG/JAR), `.editorconfig`.
2. **Gradle skeleton:** wrapper 9.7.1, `settings.gradle.kts` (typesafe project accessors, configuration + build cache), `libs.versions.toml` with every version pinned.
3. **`build-logic` convention plugins:** `foxdrop.android.application`, `.android.library`, `.android.feature`, `.android.compose`, `.hilt`, `.room` (ready for Phase 1), `.jvm.library`, `.quality` (Spotless + ktlint, detekt + Compose rules, Android Lint config), `.kover`, `.roborazzi`. Feature → feature dependency rule enforced when the dependency is declared (fails configuration).
4. **Modules (only the ones with real content in Phase 0; others are added in the phase that needs them):**
   - `:core:model` (Money in Long centavos, roles), `:core:common` (dispatchers, `DomainError`, `Result`, `Asia/Manila` time, `₱` formatter)
   - `:core:domain` (repository interfaces, use cases — pure Kotlin)
   - `:core:datastore` (encrypted session store: DataStore + Tink AEAD with Android Keystore master key; app-preferences store)
   - `:core:network` (Supabase client from `BuildConfig`, keys from `local.properties`)
   - `:core:data` (session + preferences + auth repository implementations)
   - `:core:designsystem` (tokens, `FoxDropTheme`, `FoxMotion`, components, shimmer, empty/error states, reduce-motion support)
   - `:core:testing` (`MainDispatcherRule`, fakes, screenshot helpers)
   - `:feature:onboarding` (first-run intro + 3-page pager + location explainer)
   - `:feature:auth` (animated Login ⇄ Sign Up, phone OTP UI — MVI with a pure reducer)
   - `:app-customer`, `:app-rider`, `:app-business` (splash, nav shells, Hilt, edge-to-edge, predictive back)
5. **Brand assets:** Poppins bundled as font resources (OFL; works offline and in screenshot tests), launcher icons per app generated from `reference/logos` by `tools/assets/make_icons.py` (adaptive foreground/background + monochrome), splash Animated Vector Drawable traced from the fox mark.
6. **Splash (§11A.1):** `core-splashscreen` theme (navy background, AVD icon), keep-on-screen until session check is done (hard cap 1.5 s), exit animation (scale 1.0→1.15 + fade + circular reveal), first-launch intro in pure Compose (logo + speed lines; TODO slot for designer Lottie), returning user goes straight on.
7. **Auth UI (§11A.2):** one `AuthScreen` with `mode`, `SharedTransitionLayout` (logo `sharedElement`, card/button `sharedBounds`), sliding pill tab, staggered sign-up fields, 6-box OTP (auto-advance, paste, shake + haptic on error, green fill on success), button → loading → ✓. Phone OTP and email calls go through `AuthRepository` (Supabase implementation); live end-to-end auth is verified in Phase 1. Google button present; Credential Manager flow is Phase 1.
8. **Security (§17, Android part):** HTTPS-only network security config (debug allows cleartext only to the local Supabase on `10.0.2.2`/`localhost`), `allowBackup=false` + data-extraction rules, components not exported, R8 full mode on release, Timber in debug only.
9. **Backend:** `supabase init` (via `npx`) → `supabase/config.toml` committed, with fixed test OTPs for local phone auth. Migrations start in Phase 1.
10. **CI:** `.github/workflows/android.yml` (spotlessCheck, detekt, lint, unit tests, koverVerify, verifyRoborazziDebug, assembleDebug), `backend.yml` (supabase start + db lint + pgTAP), `security.yml` (gitleaks). admin-web job is added in Phase W1.
11. **Tests:** reducer/ViewModel/serializer/formatter unit tests; Roborazzi screenshots for every Phase 0 screen (light/dark, phone/tablet, font scale 1.5).
12. **Docs:** PROGRESS, DECISIONS, OWNER_TODO, ARCHITECTURE, PHASE_0_REPORT, CHANGELOG, README developer setup.
13. **Quality gate (§19):** `./gradlew check`, launch all 3 apps on the emulator and screenshot them, `/simplify`, `/code-review high`, `/security-review`, commit on `phase-0`.

## Acceptance checks
- All 3 apps launch to a branded animated splash + login screen; dark/light both readable.
- `./gradlew check` green on a clean checkout.
- A `feature → feature` dependency fails the build.

## Known limits for this phase
- No Docker → `supabase start` / pgTAP cannot run locally; backend CI workflow is written but not run here.
- No GitHub remote → CI workflows are written but not run; recorded as "not verified".
