# Owner to-do

Things only the owner can do. The agent keeps building everything that does not depend on them.
Tick an item (`[x]`) when done and tell the agent in your next message.

## Needed soon (blocks verifying Phase 1)

- [ ] **Install Docker Desktop** (https://www.docker.com/products/docker-desktop/). It runs the local Supabase
      database for development and the database tests. Enable "Use WSL 2" during install, then restart Windows.
      Check: `docker version` in PowerShell shows both Client and Server.
- [ ] **Install the Supabase CLI.** Easiest on Windows: install Scoop (https://scoop.sh), then
      `scoop bucket add supabase https://github.com/supabase/scoop-bucket.git` and `scoop install supabase`.
      Check: `supabase --version` shows 3.4 or newer.
- [ ] **Install Deno** (Edge Functions, Phase 2/3): `irm https://deno.land/install.ps1 | iex`. Check: `deno --version`.
- [ ] **Create a GitHub repository** (private) and tell the agent its URL, so the code is backed up and the checks run
      on every change. Do not paste any password or token in the chat; the agent will ask you to log in with
      `gh auth login` in your own terminal.

## Before the admin web (Phase W1)

- [ ] **Upgrade PHP to 8.3 or newer.** XAMPP has 8.2.12. Easiest: install Laravel Herd for Windows
      (https://herd.laravel.com/windows) which brings PHP 8.4, Composer and Node.
- [ ] **Redis** for queues: Herd Pro includes it; otherwise run it in Docker (`docker run -p 6379:6379 redis:7`).

## Accounts (when the agent asks; keys go only where it says, never in the chat)

- [ ] Supabase project (Phase 1, free tier is fine) → put URL + anon key in `local.properties`.
- [ ] Google Cloud project with Maps SDK for Android + Routes API (Phase 2).
- [ ] PayMongo account in test mode (Phase 2).
- [ ] Firebase project (Phase 3).
- [ ] SMS provider for real phones (before real users test). Local development does not need it.

## Optional

- [ ] Android SDK command-line tools (Android Studio › SDK Manager › SDK Tools › "Android SDK Command-line Tools").
      Not needed now; useful for CI parity.
