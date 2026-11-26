# Phase W1 report — Super Admin web foundation

Date: 2026-11-26 · Branch: `phase-w1` (merged into `main` locally; no GitHub remote yet) · Plan: `docs/plans/phase-w1.md`
· Decisions 71–82

## What was built
The admin web lives at `/admin` inside `backend/`: Inertia v3, React 19, TypeScript strict, Tailwind v4 and shadcn/ui,
with the official Laravel starter kit's structure.

- **Sign-in and access**
  - Email and password for active admin accounts only; everyone else gets "credentials do not match".
  - **2FA is required.** An admin without an authenticator reaches only the setup page, which shows a QR code and
    setup key, asks for a code, then shows the recovery codes.
  - 2FA cannot be turned off, and a confirmed secret cannot be swapped.
  - Sessions end after 30 idle minutes; "remember me" is never honoured.
  - Login lockout: 5 tries a minute, and 20 an hour per email.
  - **"Confirm it's you"** (password + authenticator code, valid 15 minutes) is asked inside the dialog of every
    dangerous action.
  - A password change signs out every other session and every API token.
  - Admin accounts have no access to the app API (DECISIONS 82).
- **Roles and permissions**
  - `user_roles` decides who is an admin; `admin_permissions` holds what admin staff may do, using 7 permissions.
  - Routes check them with `can:` middleware, and every shared Action authorizes through a Laravel policy.
- **Audit**
  - Every admin action writes `audit_logs`, as do sign-ins, sign-outs, failed sign-ins and 2FA events.
  - The **audit log viewer** filters by who, action, entity and dates, and shows before → after values.
- **Layout**
  - A navy sidebar shaped like the reference image; later modules are marked "Soon".
  - Breadcrumbs, a ⌘K/Ctrl K command palette, and a light/dark/system theme toggle.
  - Flat cards, toasts, motion that respects reduced-motion, and skeletons, empty and error + retry states on every
    list.
- **Pages**
  - **Dashboard**: what needs attention and platform totals, refreshed every 15 s while in use. Each admin sees
    only the numbers their permissions cover.
  - **Franchises**:
    - All 83 provinces with license status.
    - Province detail with the current license, payments and history.
    - **Assign** a province, **record an offline payment** (activates or renews through the same payment path as
      PayMongo, posted to the ledger), **suspend / lift suspension**.
    - Operator applications with a document viewer, approve and reject.
  - **Stores / Riders**: every province, with filters in the URL. Review with the KYC documents, approve or reject,
    and suspend / reinstate / close or deactivate (override).
  - **Users**: search by name, any phone spelling or email; role and status filters; detail with roles and history;
    suspend / reactivate (signs them out of every app).
  - **Settings**:
    - My security: password, recovery codes, signed-in devices.
    - **Admin staff** (super admin only): create, set permissions, reset password or 2FA, remove.
- **Security headers**: CSP with a per-request nonce (Vite too), `frame-ancestors 'none'`, X-Frame-Options DENY,
  nosniff, Referrer-Policy, Permissions-Policy, and HSTS on HTTPS.
- **Schema 13–15**:
  - 2FA columns and `admin_permissions`.
  - `manual` payments and the `OFFLINE_RECEIPTS` wallet.
  - SUSPENDED keeps the province exclusive.
  - `status_reason` columns.
  - User list and search indexes (pg_trgm).
- **Server command**: `php artisan foxdrop:super-admin you@example.com` creates the first super admin, or resets a
  password or 2FA.
- **Android**: the operator application status screen re-checks by itself while it waits (every 10 s, slowing to
  60 s), so a web approval shows up within seconds.

## How to test (local)
1. In `backend/`:
   - `. ..\tools\dev\backend-env.ps1`
   - `php artisan migrate:fresh --seed --drop-types --drop-views`
   - `npm install`
   - `npm run build`
   - `php artisan serve --host=127.0.0.1`
2. Open <http://127.0.0.1:8000/admin> and sign in as `admin@foxdrop.local` / `foxdrop-local-admin`. Set up any
   authenticator app with the QR code or key, then save the recovery codes.
3. **Approve and activate an operator.**
   - Go to Franchises › Operator applications › **Ramos Cebu Deliveries** and press Approve.
   - Open **Cebu**, press Record payment and enter a reference. Cebu turns Active / "Can order".
4. **Watch the approval arrive in the app.** In the business app, sign in as `0917 000 0013` (code `123456`) and open
   "View status" before approving. Within about 10 s of the web action the app moves on by itself.
5. **Try a limited account.** Sign in as `staff@foxdrop.local` / `foxdrop-local-staff`. Franchises are read-only,
   partners can be managed, users can be viewed, and the audit log and staff pages are hidden.

## Quality Gate (§19 + §20.7)
- [x] **Backend**:
  - `vendor/bin/pest` **218 passed** (1,380 assertions, real PostgreSQL).
  - `vendor/bin/phpstan analyse` (level 8) **0 errors**.
  - `vendor/bin/pint --test` **passed**.
  - `migrate:fresh --seed` works.
- [x] **Admin web**:
  - `npm run lint` (ESLint, 0 warnings), `npm run types` (tsc strict), `npm run format:check` (Prettier) and
    `npm run test` (Vitest, 4) all pass; `npm run build` succeeds.
  - **Playwright E2E passes in 10 s**: sign in, required 2FA setup, approve the operator application, record the
    payment, province live, audit log.
  - Dispute refunds and exports join this E2E in W2.
- [x] **Android**: `./gradlew check verifyRoborazziDebug` **green**, after the operator-license polling change and its
  ViewModel tests.
- [x] **Phase acceptance (§13 W1)**:
  - [x] **An admin without 2FA cannot open any page.** A generated Pest test calls every admin route and expects a
        redirect to the setup page.
  - [x] **Admin pages change money and status only through the same Actions the API uses.**
    - A generated test gives a permission-less admin-staff account **403 on every permissioned route** (more than
      30 routes).
    - A Pest arch test plus a source scan prove **no admin controller uses `DB` or writes a model**.
  - [x] **Approving an operator application in the web shows up immediately in the operator's Android app.**
    - Pest checks it through the same API the app polls.
    - **On the emulator** the app moved from "Under review" to the operator home ("License · Cebu · Active, 364
      days left") about 11 s after the web action, with nobody touching the phone. Screenshots `09`–`10` are in
      `docs/screenshots/w1/`.
  - [x] **Every admin action appears in the audit log.** Each Action test checks its row, and the E2E reads them in
        the viewer.
- [x] **Reviews**:
  - **`/simplify`** (4 reviewers):
    - Status-change rules moved onto the models, and the pages show only server-allowed changes.
    - Shared helpers for sessions, 2FA reset, identity confirmation, LIKE patterns and presenters.
    - Cached dashboard, cheaper audit sort, user indexes, staff N+1 removed.
    - Framework messages kept off error pages, idle-aware polling, dead kit code removed.
    - Deferred items: DECISIONS 81.
  - **`/code-review high`**: 9 findings, all fixed.
    - The staff page crashed with a 500 once anyone had signed in.
    - An Android import order would have failed Spotless.
    - Policies were missing.
    - Dashboard numbers ignored permissions.
    - An offline payment could be recorded while an online checkout was still open, so the operator could pay twice.
    - Non-UUID filters were not ignored.
    - A manual refresh raced the background poll.
    - The confirm fields stayed hidden when the browser clock was behind.
    - CLAUDE.md still named Inertia v2.
  - **`/security-review`**: no findings at the reporting bar. Two below-bar findings were fixed anyway (DECISIONS 82):
    - Admins could get API tokens without 2FA.
    - The super-admin command could promote an app-registered account.
- [x] No secrets committed. Demo passwords are only in the local seeder (refused outside local/testing).
- [x] Docs updated: `PROGRESS.md`, `DEVELOPMENT.md`, `ARCHITECTURE.md`, `API.md`, `OWNER_TODO.md`, README,
      `DECISIONS.md` 71–82, `CHANGELOG.md`.

## Found by the E2E run and fixed
- The sticky table header sat inside a scroll container and covered the first row. The header is no longer sticky.

## Not verified (and why)
- **CI**: there is no GitHub repository yet. The `backend` workflow now also runs lint, types, Prettier, Vitest,
  build, `npm audit` and the Playwright E2E.
- **Production HTTPS / HSTS / secure cookies**: there is no VPS until Phase 7. The settings switch on with
  `APP_ENV=production` or HTTPS.
- **The emulator's own browser**: not used for the admin web. The web was tested in Chromium on the PC through
  Playwright.

## Left for later
- **W2**: dashboard KPIs and charts, live orders and map, payments and webhook log, reports and exports, platform
  settings, support and disputes, saved filters with partial reloads.
- **Phase 6**: status-first indexes for very large partner lists (DECISIONS 81).
- **Phase 7**: password reset by e-mail, once mail is configured.
