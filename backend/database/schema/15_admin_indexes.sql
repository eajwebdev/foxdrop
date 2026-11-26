-- FoxDrop · 15 · Indexes for the admin web lists (Phase W1 /simplify): newest-first user list, and name/email search
-- with `ilike '%…%'` (trigram indexes; a plain btree cannot serve a contains-search).

create extension if not exists pg_trgm;

create index users_created_idx on public.users (created_at desc, id);
create index users_full_name_trgm_idx on public.users using gin (full_name gin_trgm_ops);
create index users_email_trgm_idx on public.users using gin (email gin_trgm_ops);
