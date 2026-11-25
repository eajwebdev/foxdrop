-- FoxDrop · 01 · Extensions
-- PostGIS for distances and delivery zones (§3), pgcrypto for gen_random_uuid() and PII encryption (§17).
-- Supabase convention: extensions live in the `extensions` schema, not `public`.

create extension if not exists postgis with schema extensions;
create extension if not exists pgcrypto with schema extensions;

-- Private schema for SECURITY DEFINER helpers. It is NOT exposed through the Data API (config.jsonc api.schemas),
-- so nothing here can be called as an RPC.
create schema if not exists private;
revoke all on schema private from public;
grant usage on schema private to anon, authenticated, service_role;
