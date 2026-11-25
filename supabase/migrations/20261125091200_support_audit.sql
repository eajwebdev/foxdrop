-- FoxDrop · 12 · Support, disputes, notifications, device tokens, audit log, funnel events (§8, §9.5, §10)

create table public.support_tickets (
  id uuid primary key default gen_random_uuid(),
  province_id uuid references public.provinces (id),
  created_by uuid not null references auth.users (id) on delete cascade,
  requester_role public.app_role not null,
  order_id uuid references public.orders (id) on delete set null,
  subject text not null check (char_length(subject) between 3 and 140),
  status public.ticket_status not null default 'OPEN',
  assigned_to uuid references auth.users (id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index support_tickets_creator_idx on public.support_tickets (created_by, created_at desc);
create index support_tickets_province_idx on public.support_tickets (province_id, status, created_at desc);
create trigger support_tickets_updated_at before update on public.support_tickets
  for each row execute function private.set_updated_at();

create table public.ticket_messages (
  id uuid primary key default gen_random_uuid(),
  ticket_id uuid not null references public.support_tickets (id) on delete cascade,
  province_id uuid references public.provinces (id), -- copied from the ticket by trigger; never from the client
  sender_id uuid not null references auth.users (id) on delete cascade,
  body text not null check (char_length(body) between 1 and 4000),
  attachment_path text,
  created_at timestamptz not null default now()
);
create index ticket_messages_ticket_idx on public.ticket_messages (ticket_id, created_at);
create index ticket_messages_province_idx on public.ticket_messages (province_id) where province_id is not null;

create or replace function private.ticket_message_province()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  select t.province_id into new.province_id from public.support_tickets t where t.id = new.ticket_id;
  return new;
end;
$$;
create trigger ticket_messages_province before insert or update of ticket_id on public.ticket_messages
  for each row execute function private.ticket_message_province();

create table public.disputes (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null,
  province_id uuid not null,
  opened_by uuid not null references auth.users (id),
  ticket_id uuid references public.support_tickets (id) on delete set null,
  reason text not null check (char_length(reason) between 3 and 1000),
  status public.dispute_status not null default 'OPEN',
  resolution public.dispute_resolution,
  amount_centavos bigint check (amount_centavos > 0),
  resolved_by uuid references auth.users (id) on delete set null,
  resolved_at timestamptz,
  ledger_txn_id uuid references public.ledger_txns (id),
  notes text check (char_length(notes) <= 2000),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint disputes_order_fk
    foreign key (order_id, province_id) references public.orders (id, province_id) on delete cascade,
  constraint disputes_resolved_fields check (status = 'OPEN' or (resolution is not null and resolved_at is not null))
);
create index disputes_province_idx on public.disputes (province_id, status, created_at desc);
create trigger disputes_updated_at before update on public.disputes
  for each row execute function private.set_updated_at();

create table public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  province_id uuid references public.provinces (id),
  type text not null check (char_length(type) <= 60),
  title text not null check (char_length(title) <= 140),
  body text not null check (char_length(body) <= 1000),
  data jsonb not null default '{}'::jsonb,
  read_at timestamptz,
  created_at timestamptz not null default now()
);
create index notifications_user_idx on public.notifications (user_id, created_at desc);

create table public.device_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  token text not null unique check (char_length(token) between 20 and 4096),
  app text not null check (app in ('customer', 'rider', 'business')),
  platform text not null default 'android' check (platform = 'android'),
  last_seen_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);
create index device_tokens_user_idx on public.device_tokens (user_id);

-- Every admin/operator write (§17). Append-only; written by server functions.
create table public.audit_logs (
  id bigint generated always as identity primary key,
  -- No FK: audit rows are append-only and must outlive deleted accounts (an FK action would have to UPDATE them).
  actor_user_id uuid,
  actor_role public.app_role,
  action text not null check (char_length(action) <= 80),
  entity_type text not null check (char_length(entity_type) <= 60),
  entity_id text,
  province_id uuid references public.provinces (id),
  before jsonb,
  after jsonb,
  ip inet,
  user_agent text check (char_length(user_agent) <= 400),
  created_at timestamptz not null default now()
);
create index audit_logs_entity_idx on public.audit_logs (entity_type, entity_id, created_at desc);
create index audit_logs_actor_idx on public.audit_logs (actor_user_id, created_at desc);
create index audit_logs_province_idx on public.audit_logs (province_id, created_at desc);
create trigger audit_logs_append_only before update or delete on public.audit_logs
  for each row execute function private.forbid_change();

-- Funnel events (§10 observability). No personal data in `properties`.
create table public.app_events (
  id bigint generated always as identity primary key,
  user_id uuid references auth.users (id) on delete set null,
  app text not null check (app in ('customer', 'rider', 'business', 'admin')),
  name text not null check (name ~ '^[a-z][a-z0-9_]{2,59}$'),
  properties jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
create index app_events_name_time_idx on public.app_events (name, created_at desc);
