-- FoxDrop · 01 · Extensions and the private schema
-- PostGIS for distances and delivery zones (§3). gen_random_uuid() is built into PostgreSQL 13+.
-- `private` holds trigger functions; the API never calls them directly.

create extension if not exists postgis;
create schema if not exists private;
