# Architecture

FoxDrop is one repository: three Android apps and one Laravel backend (`backend/`) that serves the apps' API and,
from Phase W1, the admin web.
This file describes what exists now; MASTER_PROMPT.md describes the target.

## Android modules

```
app-customer   app-rider   app-business        ← wire features together; MainActivity, NavHost, home shell
     │              │            │
     └──── feature:onboarding, feature:auth ───  ← screens + ViewModels (MVI); depend on core only
                     │
   core:designsystem   core:domain   core:data   core:common   core:model
                         ▲              │
                         │        core:network (FoxDrop API, Ktor)   core:datastore (DataStore + Tink)
   core:testing (test helpers, fakes, screenshot helper)
```

| Module | Kind | Contents |
|---|---|---|
| `core:model` | pure Kotlin | `Money` (Long centavos), `AppRole`, `FoxDropApp` |
| `core:common` | pure Kotlin | `FoxResult`/`DomainError`, dispatcher qualifiers, `PesoFormatter`, `ManilaTime`, PH phone/email/password validation |
| `core:domain` | pure Kotlin | repository interfaces (`AuthRepository`, `RoleRepository`, `AppPreferencesRepository`), `ResolveStartDestinationUseCase`, `ResolveHomeUseCase` (role routing) |
| `core:datastore` | Android | `SecureStore` (encrypted values), `AppPreferencesDataSource` |
| `core:network` | Android | `FoxApiClient` (Ktor, bearer token, global 401 → sign-out), `SessionStore` (encrypted token + last user), auth and roles data sources, error mapping |
| `core:data` | Android | repository implementations, dispatcher + app-scope providers |
| `core:designsystem` | Android/Compose | theme, tokens, `FoxMotion`, components, splash, `FoxNavHost`, previews |
| `core:testing` | Android | `MainDispatcherRule`, fakes, `captureFoxScreenshots` |
| `feature:onboarding` | feature | first-run intro, pager, location explainer, `StartupViewModel` |
| `feature:auth` | feature | Log In ⇄ Sign Up, phone OTP (SMS autofill), Google sign-in, `SessionViewModel`, `RoleGate` (role routing) |

Rules (enforced by the build): features depend only on `core:*`; domain has no Android or network imports.

## Build

`build-logic/convention` holds the convention plugins: `foxdrop.android.application`, `.android.library`,
`.android.feature`, `.android.compose`, `.hilt`, `.room`, `.jvm.library`, `.roborazzi`, `.kover`, `.quality` (root).
All versions live in `gradle/libs.versions.toml`. Toolchain choices are explained in `docs/DECISIONS.md` (1–4).

## Screen pattern (MVI)

```
XRoute (Hilt ViewModel, collects StateFlow + one-off effects)
  └─ XScreen(state, onAction)          ← stateless; previews + screenshot tests use this
ViewModel.onAction(action)
  └─ XReducer.reduce(state, action) → (newState, command?)   ← pure, unit-tested
       └─ ViewModel runs the command (repository call / timer) and feeds the result back as an action
```

`feature:auth` is the reference implementation (`AuthContract.kt`, `AuthReducer.kt`, `AuthViewModel.kt`).

## Startup

1. `installFoxSplash` shows the navy splash with the animated fox while `StartupViewModel` resolves the first
   screen (max 1.5 s).
2. `ResolveStartDestinationUseCase`: signed in → Home; onboarding not done → Intro; otherwise → Auth.
3. The splash exits (icon grows + fades, circular reveal) unless animations are turned off.

## Security on the device (§17, Phase 0 part)

- Session token (Sanctum, 90 days) + last known user: `EncryptedSessionStore` → `SecureStore` (Tink AES-256-GCM; keyset protected by an
  Android Keystore key; the storage key is the AEAD associated data).
- No backups; HTTPS only in release; debug allows HTTP only to the local API (`10.0.2.2`, `localhost`).
- `FLAG_SECURE` on the OTP step (`SecureScreen()`); R8 full mode in release; Timber planted in debug only.

## Backend (`backend/`)

Laravel 13 on PHP 8.4, PostgreSQL 17 + PostGIS. Local setup without Docker: `docs/DEVELOPMENT.md`.

```
Android apps ──HTTPS──▶ /api/v1 (routes/api.php)
                          ├─ FormRequest validation, rate limits (otp-send, otp-verify, sign-in)
                          ├─ Controllers (thin) → Actions / Services (IssueApiToken, OtpService, Google verifier)
                          ├─ Sanctum bearer tokens, one per device; EnsureAccountIsActive
                          └─ Eloquent models with visibleTo(User) scopes (province isolation)
                        PostgreSQL: database/schema/*.sql (via migrations)
                          ├─ enums, checks, composite province FKs
                          ├─ ledger: append-only triggers + deferred zero-sum check, balances by trigger
                          └─ one live license per province (unique partial index)
Admin web (browser) ──▶ /admin (routes/admin.php, session + CSRF, CSP with nonces)
                          ├─ Fortify sign-in (admins only) → required TOTP 2FA → "confirm it's you" for dangerous actions
                          ├─ can:<permission> middleware (admin_permissions; super_admin has all)
                          ├─ Controllers/Admin (thin) → the same Actions as the API (permission checks + audit_logs)
                          └─ Queries/Admin (reads) → Inertia v3 pages in resources/js (React 19 + TS + shadcn/ui)
Reverb (Phase 3/4)   ──▶ live order status and rider location
```

| Area | Where |
|---|---|
| API routes | `backend/routes/api.php` (`api.v1.*` names) |
| Sign-in | `app/Http/Controllers/Api/V1/Auth/*`, `app/Services/Auth/*`, `app/Actions/Auth/IssueApiToken.php` |
| Province isolation | `visibleTo()` scopes on `Order`, `Store`, `Rider`, `Wallet`, `ProvinceLicense`, `UserRole`; tests in `tests/Feature/Data/ProvinceIsolationTest.php` |
| Database rules | `database/schema/*.sql`; tests in `tests/Feature/Data/DatabaseRulesTest.php` |
| Seed | `ProvinceSeeder` (83 PSGC provinces), `DemoSeeder` (local/testing only) |
| Config | `config/foxdrop.php` (OTP, test codes, token lifetime, Google client IDs, SMS driver) |
| Admin web | `routes/admin.php`, `app/Http/Controllers/Admin/*`, `app/Queries/Admin/*`, `app/Http/Middleware/Admin/*`, `app/Providers/FortifyServiceProvider.php`, pages in `resources/js/pages`, shared pieces in `resources/js/components/admin` |
| Admin permissions | `App\Enums\AdminPermission`, `admin_permissions` table, `User::hasAdminPermission()`, `Gate::before` in `AppServiceProvider`, `App\Support\AdminGuard` inside Actions |
| Admin tests | `tests/Feature/Admin/*` (access + generated 403 test, architecture, Actions), `resources/js/**/*.test.ts` (Vitest), `tests/e2e/*` (Playwright) |
