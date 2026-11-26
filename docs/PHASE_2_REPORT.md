# Phase 2 report — Operator licensing & partner onboarding

Date: 2026-11-26 · Branch: `phase-2` (merged into `main` locally; no GitHub remote yet)

## What was built
- **Provincial operator** (business app → Get started → Apply for a province):
  - Pick an available province, add business details and a valid ID photo, and submit; the province is then blocked
    for others.
  - An admin approves. Until the admin web exists this is
    `php artisan foxdrop:operator-application approve <id>`.
  - Pay ₱15,000 through the checkout. Locally this is a fake checkout; PayMongo is used once keys are set.
  - The license becomes **ACTIVE for 365 days**, the operator role is granted, and the app moves to the operator home
    by itself.
  - Renewal adds a year. A daily job moves ACTIVE → GRACE → EXPIRED.
  - The license fee is posted to the ledger (rule 7).
- **Operator home**:
  - Dashboard: a license card with days left and Renew.
  - Stores and Riders: approval lists with Pending / Approved / Rejected tabs, and review screens with the document
    photos, Approve, and Reject with a reason.
  - Account: delivery zones (circles), rate card editor with a live fee preview from the server, and the monthly
    store fee.
- **Store application** (business app → Open a store):
  - Province, store details, current location, business permit and ID photos.
  - The status screen shows Submitted, Active, or Rejected with the reason and a fix-and-resend form that keeps the
    province.
- **Rider registration** (rider app):
  - The four-part checklist from the rider workflow: personal details, valid ID, driver's license, vehicle details.
  - The status screen shows Pending, Approved, or Rejected with the reason and a resend form.
- **Backend**:
  - Payment provider (PayMongo checkout sessions + local fake), signed and idempotent webhook, payment purpose handlers.
  - Private KYC photo storage: re-encoded (EXIF/GPS dropped), size-capped, 10-minute signed links.
  - Delivery fee calculator, operator access rules (DECISIONS 65), audited Actions, Artisan commands.
  - `docs/API.md` documents every endpoint.

## How to test (local)
1. `. .\tools\dev\backend-env.ps1`, then in `backend/` run
   `php artisan migrate:fresh --seed --drop-types --drop-views` and `php artisan serve --host=127.0.0.1`.
2. **Become an operator.**
   - In the business app, sign in as `0917 000 0002` (code `123456`).
   - Tap Apply for a province, pick e.g. Aklan, enter a business name and contact number, choose a valid ID photo,
     and submit.
   - Approve with `php artisan foxdrop:operator-application approve <id>`. The id is in the database; the newest
     application is the one to approve.
   - In the app, tap Check status, then Pay ₱15,000.00 and press **Pay (test)** on the checkout page. The app switches
     to the operator home with the license Active, 364 days left.
3. **Approve a partner.** Sign in as `0917 000 0006` (Negros operator) and open Stores. Lola Nena's Bakery is waiting:
   approve it. Under Riders, reject Jomar Tan with a reason.
4. **See the result as the rider.** In the rider app, sign in as `0917 000 0012`. The rejection and its reason show,
   with a Fix and send again button.

## Quality Gate (§19)
- [x] **Backend**:
  - `vendor/bin/pest` **170 passed**, all three acceptance checks among them.
  - `vendor/bin/phpstan analyse` (level 8) **0 errors**.
  - `vendor/bin/pint --test` **passed**.
  - `migrate:fresh --seed` works.
- [x] **Android**: `./gradlew check verifyRoborazziDebug` **green**, 252 unit, ViewModel and screenshot tests.
  Coverage stays at or above the 80 % gate inside `check`.
- [x] **Phase acceptance**:
  - [x] **Applying for an already-licensed province is impossible.** It returns 409 (Pest). The app's picker shows
        only available provinces.
  - [x] **Paying the test license flips it to ACTIVE with end date +365 days.**
    - Pest checks this through both the fake checkout and a signed PayMongo webhook.
    - On the emulator: Aklan, valid until 2027-11-26, operator role granted, app re-routed to the operator home.
  - [x] **An approved store appears to customers; a rejected one does not.**
    - Pest checks this.
    - On the emulator, the operator approved Lola Nena's Bakery. It became ACTIVE and published, its owner got the
      `store_owner` role and both store wallets, and the approval was audited.
- [x] **Every new screen** has a skeleton, empty state, error + retry, dark mode, previews, screenshot tests and
      FoxMotion.
- [x] **Reviews**:
  - `/code-review high`: 9 real findings, all fixed. The worst was that a former operator kept access after the
    province was re-licensed.
  - `/simplify`: 4 reviewers. Fixes applied; deferred items are in DECISIONS 67.
  - `/security-review`: no findings at the reporting bar. Two lower notes were handled: self-review is now blocked
    (DECISIONS 69), and zone clipping is planned for Phase 3 (DECISIONS 70).
- [x] No secrets committed. No money is calculated on the device; fees are previewed by the server.

## Found by the emulator end-to-end run and fixed
- The fake checkout's buttons posted to `127.0.0.1`, which on a phone is the phone itself. They now post to relative
  URLs.
- Laravel Boost's browser logger was injected into local pages. It is turned off.
- A resubmitted application lost its province. It is kept now.
- The rejection notes had a doubled full stop. Fixed.

## Not verified (and why)
- **Pressing Pay inside the Custom Tab on this emulator.** The emulator's Chrome renders every page blank, Laravel's
  own `/up` page included; this is a GPU/emulator issue. The same Pay request was sent from the computer instead,
  with the page's session and CSRF token, and the app then picked up the payment by itself.
- **Real PayMongo.** It needs test keys (OWNER_TODO). The driver and webhook are tested against faked HTTP and real
  signature maths.
- **Store application submitted from the phone (GPS + two photos).** Covered by ViewModel and screenshot tests, and
  the server side by Pest; the full form was not filled on the emulator.
- **CI**: there is no GitHub remote yet.

## Left for later (DECISIONS 58, 67, 70)
- Polygon zone drawing on a map (needs the Maps key; Phase 3).
- Clipping zones to the province.
- Moving the payment flow into `core:payments` with the second payment type.
- `RoleRepository.refresh()` in place of the navigation plumbing.
- Shared onboarding ViewModel helper and photo-slot model.
- Review list pagination.
- Push notifications for approvals (Phase 3/6).
