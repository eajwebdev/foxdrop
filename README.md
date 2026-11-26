# FoxDrop

Province-exclusive delivery platform for the Philippines: one licensed operator per province, with local stores, riders and customers.

**Status: Phases 0, 1 and 1b built.** The three Android apps sign in with a phone code, email or Google through the FoxDrop API (Laravel + PostgreSQL), and each account lands on its own home. Progress: `docs/PROGRESS.md`. Things only you can do: `docs/OWNER_TODO.md`.

## What will be built

| Part | For whom | Technology |
|---|---|---|
| **FoxDrop** (Android) | Customers | Kotlin + Jetpack Compose |
| **FoxDrop Rider** (Android) | Riders | Kotlin + Jetpack Compose |
| **FoxDrop Business** (Android) | Stores and Provincial Operators (screens change by role) | Kotlin + Jetpack Compose |
| **FoxDrop Admin** (web, inside `backend/`) | Super Admin and admin staff | Laravel + Vite + Inertia.js + React + TypeScript |
| **Backend** (in `backend/`) | All of the above, one database | Laravel 13 API + PostgreSQL 17 + PostGIS, Reverb (live updates), queues |

Payments: PayMongo (GCash, Maya, card) and Cash on Delivery. Maps: Google Maps. Push: Firebase.

## What is in this folder

- `MASTER_PROMPT.md`: the full plan. Business rules, apps, database, money rules (§7), screens (§9), animations (§11A), build phases with tests (§13), code standards (§16), security (§17), Super Admin web (§20).
- `CLAUDE.md`: the short rules Claude Code loads automatically in every session.
- `reference/FoxDrop_Province_Delivery_Workflow.png`: the whole business model.
- `reference/workflows/`: the 4 user workflows (Customer, Super Admin, Provincial Operator, Rider).
- `reference/logos/`: the 4 logos.

The images in `reference/` are the source of truth for how the business works. Do not edit them.

## What you need on your computer

Install these before starting (Windows is fine):

- **Android Studio** (latest), plus a phone or an emulator.
- **Git**.
- For the backend: **PHP 8.4**, **Composer**, **PostgreSQL 17 + PostGIS**, **Node.js 22 LTS** (admin web). On Windows these can be portable copies, no Docker or admin rights needed: see `docs/DEVELOPMENT.md`.
- **Claude Code**, run on this same computer so it can build and run the apps.

Keep the project in a short path without spaces, for example `D:\FoxDrop`.

## Accounts and keys

You do not need all of these on day one. The agent builds with a local backend and test doubles first, and writes what it needs from you in `docs/OWNER_TODO.md`.

| Account | Used for | Needed about |
|---|---|---|
| GitHub | Code storage, automatic checks on every change | Phase 0 |
| Google Cloud (Maps SDK for Android + Routes API) | Maps, road distance for delivery fees | Phase 2 |
| PayMongo (start in test mode) | GCash, Maya and card payments | Phase 2 |
| Firebase | Push notifications, crash reports | Phase 3 |
| SMS provider (for example Twilio) | Login codes sent to real phones | Before real users test |
| Email (SMTP) provider | License reminders, report emails | Phase 6 |
| Hostinger VPS | Hosting the backend: API, admin web, PostgreSQL, live updates. Hostinger's MySQL is not used | Before real users test / Phase 7 |
| Google Play Console | Publishing the 3 apps | Phase 7 |

**Never paste secret keys into the chat or into app code.** Put them only where the agent tells you: `local.properties`, `backend/.env` (on your computer or the server) and GitHub secrets. None of these files are committed.

## How to start

1. Open Claude Code in this folder.
2. Type:
   `Read MASTER_PROMPT.md and all images in /reference, then build FoxDrop phase by phase. Start with Phase 0.`
3. The agent works through the phases and continues by itself. It stops only if a check cannot pass or if everything left is waiting for you.

To continue later in a new session, type:
`Read CLAUDE.md, MASTER_PROMPT.md and docs/PROGRESS.md, then continue from the current phase.`

If you prefer to check each phase before the next one starts, add `Stop after each phase and wait for me.` to the first message.

## Build phases

| Phase | What you get |
|---|---|
| 0 | Project foundation: 3 apps open to an animated splash and login screen |
| 1 | Database, login (phone code, email, Google), roles, demo data |
| 1b | Backend moved to Laravel + PostgreSQL (your hosting choice) |
| 2 | Operator application, ₱15,000 license payment, zones, rate card, store and rider approval |
| W1 | Admin web basics: login with 2FA, applications, licenses, approvals, users |
| 3 | Store catalog, customer browsing, cart, checkout (COD and online), store accepts orders |
| 4 | Rider app, automatic rider assignment, live tracking, proof of delivery |
| 5 | Money: ledger, store top-up wallet, COD remittance, store fees, settlement |
| 6 | Dashboards, reports with export, reviews, support and disputes |
| W2 | Admin web complete: dashboard, live map, payments, reports, settings, disputes |
| 7 | Security hardening, performance, release builds, Play Store text, runbook |

## What to check after each phase

- Read `docs/PHASE_X_REPORT.md`: what was built, how to test it, screenshots, and the checklist of tests that passed. Anything the agent could not test is listed as "not verified".
- Read `docs/OWNER_TODO.md`: things only you can do (create an account, add a key, turn on an API).
- Open the apps on your phone or emulator and try the flow yourself.
- `docs/DECISIONS.md` lists every choice the agent made when the plan was not specific.

## Developer setup

Tested on Windows 11 with Android Studio 2025.3. Commands are for PowerShell; on macOS/Linux use `./gradlew`.

1. Open the folder in Android Studio once (it creates `local.properties` with your SDK path). Gradle uses Android
   Studio's bundled JDK; on the command line set it first:
   `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`
2. Optional, for a backend: set up the local API once (`docs/DEVELOPMENT.md`), run it with
   `php artisan serve --host=127.0.0.1` in `backend/`, and put `FOXDROP_API_URL=http://10.0.2.2:8000/api/v1/` in
   `local.properties`. Without it the apps still run and say they are not connected yet.
3. Build and install, e.g. the customer app: `.\gradlew :app-customer:installDebug` (also `:app-rider`, `:app-business`).

| Task | Command |
|---|---|
| All checks (format, detekt, lint, unit tests, coverage) | `.\gradlew check` |
| Screenshot tests | `.\gradlew verifyRoborazziDebug` (re-record after an intended UI change: `recordRoborazziDebug`) |
| Fix formatting | `.\gradlew spotlessApply` |
| Local backend (portable PHP + PostgreSQL) | `. .\tools\dev\backend-env.ps1`, then in `backend/`: `php artisan migrate:fresh --seed --drop-types --drop-views` and `php artisan serve --host=127.0.0.1` |
| Backend checks | in `backend/`: `vendor/bin/pest`, `vendor/bin/phpstan analyse`, `vendor/bin/pint --test` |
| Regenerate icons/splash from `reference/logos` | `tools\assets\.venv\Scripts\python tools\assets\make_assets.py` (create the venv from `tools/assets/requirements.txt` first) |

Local phone login (once the local backend runs): demo numbers `0917 000 0001` to `0917 000 0009`, code `123456`.
Customers 0001–0002, riders 0003–0005, operator 0006 (business app), store owners 0007–0009 (business app).

## Main decisions (most can be changed later in Platform Settings)

- One operator per province. License ₱15,000 per year, with 15 days grace after expiry.
- Store fee ₱15 per month, taken from the store's top-up wallet on the 1st. After 7 unpaid days the store is suspended until it tops up.
- Delivery fee: ₱40 covers the first 2 km, then ₱10 per km, up to 15 km. Rider gets 80%, operator 20%.
- Commission on items: 0% for platform and operator by default.
- Rider can hold up to ₱5,000 COD cash and must remit within 24 hours.
- Settlement to stores and riders is weekly; payouts are recorded by hand in version 1.
- All prices, fees and wallet balances are calculated on the server, never on the phone.
- Super Admin uses the web dashboard only, and 2FA is required.
