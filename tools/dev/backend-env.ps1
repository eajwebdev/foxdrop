# FoxDrop local backend environment (Windows, portable tools, no Docker).
# Usage from the repo root:  . .\tools\dev\backend-env.ps1
#
# Expects the portable tools in D:\devtools (override with $env:FOXDROP_DEVTOOLS):
#   php84\   PHP 8.4 NTS x64 (windows.php.net) with php.ini enabling pdo_pgsql, pgsql, intl, sodium, zip, ...
#   pgsql\   PostgreSQL 17 binaries (EnterpriseDB zip) with the PostGIS bundle copied in
#   pgdata\  the database cluster (initdb), listening on localhost:5432 only
# See docs/DEVELOPMENT.md for the one-time setup.

$devtools = if ($env:FOXDROP_DEVTOOLS) { $env:FOXDROP_DEVTOOLS } else { 'D:\devtools' }
$php = Join-Path $devtools 'php84'
$pg = Join-Path $devtools 'pgsql\bin'
$data = Join-Path $devtools 'pgdata'

foreach ($path in @($php, $pg, $data)) {
    if (-not (Test-Path $path)) { throw "Missing $path. See docs/DEVELOPMENT.md." }
}

$env:PATH = "$php;$pg;$env:PATH"

& (Join-Path $pg 'pg_isready.exe') -h localhost -p 5432 -q
if ($LASTEXITCODE -ne 0) {
    # Detached, so the database keeps running after this shell closes.
    Start-Process -FilePath (Join-Path $pg 'pg_ctl.exe') -ArgumentList '-D', $data, '-l', (Join-Path $data 'server.log'), 'start' -WindowStyle Hidden
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Milliseconds 500
        & (Join-Path $pg 'pg_isready.exe') -h localhost -p 5432 -q
        if ($LASTEXITCODE -eq 0) { break }
    }
}

php -v | Select-Object -First 1
& (Join-Path $pg 'pg_isready.exe') -h localhost -p 5432
