# Phase W1 plan — Super Admin web foundation

Source: MASTER_PROMPT §13 (W1), §9.5, §20; `reference/workflows/02_Super_Admin_Workflow.png`. Decisions 71–80.

## Scope
1. **Stack inside `backend/`**: the official Laravel React starter kit structure (Inertia v3, React 19, TypeScript
   strict, Tailwind v4, shadcn/ui, Fortify, Wayfinder) on plain Vite 8, with ESLint + Prettier + Vitest
   (DECISIONS 71). React in `backend/resources/js`.
2. **Sign-in & access**:
   - Email + password, for admin accounts only (`super_admin`, `admin_staff`, status ACTIVE). There is no
     registration and no e-mail password reset yet.
   - 2FA (TOTP + recovery codes) is required. An admin without confirmed 2FA reaches only the setup page and sign-out.
   - 30-minute idle sessions, "sign out other sessions", and login rate limit plus lockout.
   - Dangerous actions (licenses, suspensions, admin staff) need a fresh "confirm it's you" (password +
     authenticator code, valid 15 min).
3. **Roles & permissions** (DECISIONS 72):
   - `user_roles` decides who is an admin. An `admin_permissions` table holds the admin-staff permissions;
     `super_admin` has every permission. spatie/laravel-permission was tried and dropped: its `roles` relation clashes
     with `User::roles()`.
   - Permissions: `franchises.view`, `franchises.manage`, `partners.view`, `partners.manage`, `users.view`,
     `users.manage`, `audit.view`.
   - Admin staff management is for the super admin only.
4. **Audit**:
   - Every admin action writes `audit_logs` through the existing `RecordAudit`, as do sign-in, sign-out, failed
     sign-in and 2FA changes.
   - The audit log viewer has filters and a before/after diff.
5. **Layout shell**:
   - Navy sidebar matching the reference (Dashboard, Franchises, Users, Stores, Riders, Orders, Payments, Reports,
     Support, Settings, Audit log; later-phase items are marked "Soon").
   - Topbar with breadcrumbs, a ⌘K command palette and a light/dark toggle.
   - Skeletons (deferred props), empty and error states, toasts, motion that respects `prefers-reduced-motion`.
6. **Pages**:
   - **Dashboard**: a "needs attention" summary (applications waiting, licenses expiring in 30 days, provinces
     active). The full KPIs and charts are W2.
   - **Franchises**:
     - Province list (83) with license status and filters.
     - Province detail with license history and actions: assign to an operator, record an offline payment
       (activates or renews), suspend, reinstate.
     - Operator applications: list, review with documents, approve / reject.
   - **Stores / Riders**: all provinces, with filters. The review page has a document viewer, approve / reject, and
     suspend / reinstate as an override.
   - **Users**: search (name, phone, email, role) and a detail page with roles, suspend / reactivate.
   - **Settings**: My security (password, recovery codes, other sessions). Admin staff (super admin only): create,
     set permissions, remove.
   - **Audit log**.
7. **Backend Actions (shared with the API)**:
   - These Actions are reused: `ReviewOperatorApplication`, `ReviewStore`, `ReviewRider`.
   - The new ones are listed below. Admin controllers only authorize, call an Action, and render or redirect.

   | Action | What it does |
   |---|---|
   | `AssignProvinceLicense` | creates a license in PENDING_PAYMENT |
   | `RecordOfflineLicensePayment` | `manual` payment → `SettlePayment` → `ActivateLicensePayment`, the same ledger rule 7 |
   | `ChangeLicenseSuspension` | license suspension and reinstatement |
   | `ChangeStoreStatus`, `ChangeRiderStatus` | suspend / reinstate / deactivate |
   | `ChangeUserStatus` | suspend / reactivate; also revokes API tokens and web sessions |
   | `ManageAdminStaff`, `ManageOwnSession` | admin staff management; own password and sessions |
8. **Schema 13**:
   - 2FA columns on `users`.
   - the `admin_permissions` table.
   - `payments.provider` allows `manual`.
   - SYSTEM wallet kind `OFFLINE_RECEIPTS`.
   - SUSPENDED keeps the province exclusive (unique index and directory view).
   - Store/rider `status_reason`.
9. **Android**: the operator application status screen refreshes itself while waiting (every 10 s while visible)
   so a web approval shows up at once.

## Acceptance (§13 W1)
- **An admin without 2FA cannot open any page.** A Pest test runs over every admin GET route and expects a redirect
  to the setup page.
- **Admin pages change money and status only through the shared Actions**:
  - A Pest arch test forbids `DB`, query-builder writes and model `save`/`update`/`create`/`delete`/`forceFill`
    in `App\Http\Controllers\Admin`.
  - A generated test proves that an admin-staff account without permissions gets 403 on every admin route.
- **A web approval shows up in the operator's Android app**: check it on the emulator (approve in the web, and the
  app moves to "Pay").
- **Every admin action is audited**: each Action test checks its `audit_logs` row.

## Quality gate additions (§20.7)
- Backend: `npm run lint`, `npm run types`, `npm run test` (Vitest) and `npm run build`.
- Playwright E2E: log in → set up 2FA → approve an operator application → record payment (dispute and export arrive
  in W2).
- The CI `backend` job runs the JS checks too.
