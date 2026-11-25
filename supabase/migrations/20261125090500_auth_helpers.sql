-- FoxDrop · 05 · Role helpers for RLS and the JWT roles claim (§5, §17)
--
-- RLS decisions use the user_roles TABLE (always current: a suspended operator loses access on the next query).
-- The JWT `app_roles` claim is a convenience for the apps' routing only and can be up to one token-lifetime stale.
-- Helpers are SECURITY DEFINER (they read user_roles, which RLS would otherwise filter) with an empty search_path,
-- live in the non-exposed `private` schema, and only ever answer questions about the CURRENT user.

create or replace function private.has_role(check_role public.app_role)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.user_roles ur where ur.user_id = (select auth.uid()) and ur.role = check_role
  );
$$;

create or replace function private.is_platform_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.user_roles ur
    where ur.user_id = (select auth.uid()) and ur.role in ('super_admin', 'admin_staff')
  );
$$;

-- Provinces the current user runs as operator or operator staff.
create or replace function private.operator_province_ids()
returns setof uuid
language sql
stable
security definer
set search_path = ''
as $$
  select ur.province_id from public.user_roles ur
  where ur.user_id = (select auth.uid()) and ur.role in ('operator', 'operator_staff');
$$;

-- Stores the current user owns or works for.
create or replace function private.member_store_ids()
returns setof uuid
language sql
stable
security definer
set search_path = ''
as $$
  select ur.store_id from public.user_roles ur
  where ur.user_id = (select auth.uid()) and ur.role in ('store_owner', 'store_staff');
$$;

-- Only the province's main operator (not staff) — for actions such as changing rate cards later.
create or replace function private.is_operator_of(target_province uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.user_roles ur
    where ur.user_id = (select auth.uid()) and ur.role = 'operator' and ur.province_id = target_province
  );
$$;

revoke all on function private.has_role(public.app_role) from public;
revoke all on function private.is_platform_admin() from public;
revoke all on function private.operator_province_ids() from public;
revoke all on function private.member_store_ids() from public;
revoke all on function private.is_operator_of(uuid) from public;
grant execute on function private.has_role(public.app_role) to authenticated, service_role;
grant execute on function private.is_platform_admin() to authenticated, service_role;
grant execute on function private.operator_province_ids() to authenticated, service_role;
grant execute on function private.member_store_ids() to authenticated, service_role;
grant execute on function private.is_operator_of(uuid) to authenticated, service_role;

-- Supabase Auth hook (config.jsonc auth.hook.custom_access_token): adds
--   "app_roles": [{"role": "operator", "province_id": "...", "store_id": null}, ...]
-- to every access token so the apps can route to the right home (§5, business app role routing).
create or replace function public.custom_access_token_hook(event jsonb)
returns jsonb
language plpgsql
stable
set search_path = ''
as $$
declare
  claims jsonb := coalesce(event -> 'claims', '{}'::jsonb);
  roles jsonb;
begin
  select coalesce(
           jsonb_agg(jsonb_build_object('role', ur.role, 'province_id', ur.province_id, 'store_id', ur.store_id)
                     order by ur.role),
           '[]'::jsonb)
    into roles
    from public.user_roles ur
   where ur.user_id = (event ->> 'user_id')::uuid;

  return jsonb_set(event, '{claims}', claims || jsonb_build_object('app_roles', roles));
end;
$$;

-- Only Supabase Auth may run the hook (it is in `public`, so lock it down completely for API roles).
revoke all on function public.custom_access_token_hook(jsonb) from public, anon, authenticated;
grant execute on function public.custom_access_token_hook(jsonb) to supabase_auth_admin;
grant usage on schema public to supabase_auth_admin;
grant select on table public.user_roles to supabase_auth_admin;
