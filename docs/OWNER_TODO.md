# Owner to-do

Things only the owner can do. The agent keeps building everything that does not depend on them.
Tick an item (`[x]`) when done and tell the agent in your next message.

## Needed soon

- [ ] **Create a GitHub repository** (private) and tell the agent its URL, so the code is backed up and the checks run
      on every change. Do not paste any password or token in the chat; the agent will ask you to log in with
      `gh auth login` in your own terminal.

The local backend no longer needs Docker, the Supabase CLI or Deno. Portable PHP 8.4 and PostgreSQL 17 + PostGIS are
set up in `D:\devtools` (see `docs/DEVELOPMENT.md`); keep that folder.

## Accounts (when the agent asks; keys go only where it says, never in the chat)

- [ ] **Hostinger VPS** for the backend (before real users test; Phase 7 deploy). KVM 2 (2 vCPU, 8 GB) or larger,
      Ubuntu 24.04, in Singapore if offered (closest to the Philippines). It runs the API, the admin web, PostgreSQL +
      PostGIS, Reverb and the queue workers. No MySQL database is needed.
- [ ] **Google sign-in** (optional now, needed before launch): in Google Cloud › APIs & Services › Credentials create
      1) an OAuth client of type **Web application** — put its client ID in `local.properties` as
      `GOOGLE_WEB_CLIENT_ID=…` and in `backend/.env` as `GOOGLE_CLIENT_IDS=…` (no client secret is needed);
      2) one OAuth client of type **Android** for each app (`ph.foxdrop.customer`, `ph.foxdrop.rider`,
      `ph.foxdrop.business`, plus the `.debug` variants) with your keystore's SHA-1
      (Android Studio › Gradle › signingReport). Until then the button says "coming soon".
- [ ] Google Cloud project with Maps SDK for Android + Routes API (Phase 2).
- [ ] PayMongo account in test mode (Phase 2).
- [ ] Firebase project (Phase 3).
- [ ] SMS provider for real phones (before real users test), for example Semaphore (PH) or Twilio. Local development
      does not need it: codes for the demo numbers are fixed, and other codes are written to the backend log.

## Admin web

- [ ] Install an authenticator app on your phone (Google Authenticator, Microsoft Authenticator, 1Password…). The
      admin web requires it at the first sign-in. Keep the 8 recovery codes it shows somewhere safe, offline.
- [ ] On the VPS (Phase 7) the agent creates your super admin account with `php artisan foxdrop:super-admin`; you
      choose the password there yourself.

## Optional

- [ ] Android SDK command-line tools (Android Studio › SDK Manager › SDK Tools › "Android SDK Command-line Tools").
      Not needed now; useful for CI parity.
