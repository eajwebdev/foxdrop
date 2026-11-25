# Architecture

FoxDrop is one repository: three Android apps, a Supabase backend, and (from Phase W1) a Laravel admin web.
This file describes what exists now; MASTER_PROMPT.md describes the target.

## Android modules

```
app-customer   app-rider   app-business        ← wire features together; MainActivity, NavHost, home shell
     │              │            │
     └──── feature:onboarding, feature:auth ───  ← screens + ViewModels (MVI); depend on core only
                     │
   core:designsystem   core:domain   core:data   core:common   core:model
                         ▲              │
                         │        core:network (Supabase)   core:datastore (DataStore + Tink)
   core:testing (test helpers, fakes, screenshot helper)
```

| Module | Kind | Contents |
|---|---|---|
| `core:model` | pure Kotlin | `Money` (Long centavos), `AppRole`, `FoxDropApp` |
| `core:common` | pure Kotlin | `FoxResult`/`DomainError`, dispatcher qualifiers, `PesoFormatter`, `ManilaTime`, PH phone/email/password validation |
| `core:domain` | pure Kotlin | repository interfaces (`AuthRepository`, `AppPreferencesRepository`), `ResolveStartDestinationUseCase` |
| `core:datastore` | Android | `SecureStore` (encrypted values), `AppPreferencesDataSource` |
| `core:network` | Android | `SupabaseClientProvider`, `EncryptedSessionManager`, `AuthRemoteDataSource`, error mapping |
| `core:data` | Android | repository implementations, dispatcher + app-scope providers |
| `core:designsystem` | Android/Compose | theme, tokens, `FoxMotion`, components, splash, `FoxNavHost`, previews |
| `core:testing` | Android | `MainDispatcherRule`, fakes, `captureFoxScreenshots` |
| `feature:onboarding` | feature | first-run intro, pager, location explainer, `StartupViewModel` |
| `feature:auth` | feature | Log In ⇄ Sign Up, phone OTP, `SessionViewModel` |

Rules (enforced by the build): features depend only on `core:*`; domain has no Android or Supabase imports.

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

- Session tokens: supabase-kt → `EncryptedSessionManager` → `SecureStore` (Tink AES-256-GCM; keyset protected by an
  Android Keystore key; the storage key is the AEAD associated data).
- No backups; HTTPS only in release; debug allows HTTP only to the local Supabase host.
- `FLAG_SECURE` on the OTP step (`SecureScreen()`); R8 full mode in release; Timber planted in debug only.

## Backend

`supabase/config.jsonc` (CLI 3.4) is committed. Migrations, RLS and pgTAP tests start in Phase 1.
