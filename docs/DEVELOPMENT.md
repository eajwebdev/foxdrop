# Local development

Everything runs on one Windows PC without Docker: the Laravel API, PostgreSQL + PostGIS, and the Android emulator.

## One-time setup: backend tools (portable, no admin rights)

The tools live in `D:\devtools` (any folder works; set `FOXDROP_DEVTOOLS` to use another one). Nothing is installed
system-wide, and XAMPP's PHP is left alone.

1. **PHP 8.4** — download the "VS17 x64 Non Thread Safe" zip from windows.php.net and unzip it to `D:\devtools\php84`.
   Copy `php.ini-development` to `php.ini`, set `extension_dir = "D:\devtools\php84\ext"` and enable `curl`,
   `fileinfo`, `gd`, `intl`, `mbstring`, `openssl`, `pdo_pgsql`, `pgsql`, `pdo_sqlite`, `sodium`, `zip`, `exif`,
   `sockets`; set `memory_limit = 512M`.
2. **PostgreSQL 17** — download the "Windows x86-64 binaries" zip from EnterpriseDB and unzip it so that
   `D:\devtools\pgsql\bin\postgres.exe` exists.
3. **PostGIS** — download `postgis-bundle-pg17-<version>x64.zip` from download.osgeo.org/postgis/windows/pg17, unzip it,
   and copy its `bin`, `lib`, `share`, `gdal-data` and `utils` folders into `D:\devtools\pgsql`.
   **Then copy PostgreSQL's own `libcurl.dll`, `libcrypto-3-x64.dll`, `libssl-3-x64.dll` and `zlib1.dll` back over the
   bundle's copies** (from the EnterpriseDB zip). The bundle's `libcurl.dll` needs DLLs it does not ship, and PostGIS
   then fails with "could not load library postgis-3.dll".
4. **Database cluster** (password auth, localhost only):
   ```powershell
   D:\devtools\pgsql\bin\initdb.exe -D D:\devtools\pgdata -U postgres --pwfile=<file with a password> -A scram-sha-256 -E UTF8 --locale=C
   Add-Content D:\devtools\pgdata\postgresql.conf "listen_addresses = 'localhost'"
   . .\tools\dev\backend-env.ps1      # starts it
   psql -h localhost -U postgres -c "create role foxdrop login password '<app password>';" `
     -c "create database foxdrop owner foxdrop;" -c "create database foxdrop_test owner foxdrop;"
   psql -h localhost -U postgres -d foxdrop -c "create extension postgis;"
   psql -h localhost -U postgres -d foxdrop_test -c "create extension postgis;"
   ```
   The `foxdrop` role is not a superuser, so PostGIS is created once by `postgres`; the migrations then skip it.
5. **Backend** — in `backend/`: copy `.env.example` to `.env`, put the `foxdrop` password in `DB_PASSWORD`, then
   `php C:\ProgramData\ComposerSetup\bin\composer.phar install`, `php artisan key:generate`,
   `php artisan reverb:install` (fills the `REVERB_*` keys), `php artisan migrate:fresh --seed --drop-types --drop-views`.

## Every day

```powershell
. .\tools\dev\backend-env.ps1          # PHP 8.4 + PostgreSQL on PATH, database started
cd backend
php artisan serve --host=127.0.0.1     # API on http://127.0.0.1:8000 (the emulator reaches it as 10.0.2.2:8000)
```

Android: `local.properties` needs `FOXDROP_API_URL=http://10.0.2.2:8000/api/v1/`. Demo accounts
(`backend/database/seeders/DemoSeeder.php`): `+63 917 000 0001…0013`, code `123456`.

### Admin web (Phase W1)

Node.js 22 and npm are needed once: `cd backend; npm install`.

1. Build the pages once with `npm run build`, or run `npm run dev` beside `php artisan serve` for hot reload. Vite
   runs `php artisan wayfinder:generate`, so PHP must be on `PATH` (`backend-env.ps1` does that).
2. Open <http://127.0.0.1:8000/admin>.
3. Sign in with one of the demo accounts:

   | Account | Password | Access |
   |---|---|---|
   | `admin@foxdrop.local` | `foxdrop-local-admin` | super admin |
   | `staff@foxdrop.local` | `foxdrop-local-staff` | admin staff: franchises view, partners, users view |

   At the first sign-in you set up an authenticator app (any TOTP app; the setup key can be typed in).
4. Demo data includes:
   - Elena Ramos (`0917 000 0013`) waiting for approval to run Cebu.
   - Lola Nena's Bakery and rider Jomar Tan waiting for Negros Occidental.

On a server, the first super admin is created with `php artisan foxdrop:super-admin you@example.com --name="Your Name"`.
The same command resets a password, and with `--reset-2fa` it also resets the authenticator.

**Windows note:** Vitest loads two copies of itself when the shell's drive letter is lower case (`d:\…` vs `D:\…`)
and fails with "Cannot read properties of undefined (reading 'config')". Run `npm run test` from a shell opened on
`D:\` (PowerShell: `Set-Location D:\bckp\FoxDrop\backend`).

## Checks

| What | Command |
|---|---|
| Backend tests (real PostgreSQL, database `foxdrop_test`) | `vendor/bin/pest` |
| Static analysis (Larastan level 8) | `vendor/bin/phpstan analyse` |
| Formatting | `vendor/bin/pint --test` |
| Admin web lint / types / unit tests / build | `npm run lint`, `npm run types`, `npm run test`, `npm run build` (`npm run format` fixes formatting) |
| Admin web E2E (Playwright, against the running server with fresh demo data) | `php artisan migrate:fresh --seed --drop-types --drop-views`, then `npm run e2e` (once: `npx playwright install chromium`) |
| Android | `./gradlew check verifyRoborazziDebug` (JAVA_HOME = Android Studio's `jbr`) |
