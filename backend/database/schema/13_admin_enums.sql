-- FoxDrop · 13 · Enum values for the admin web (Phase W1). On their own: a new enum value cannot be used in the
-- transaction that adds it, and 14 uses this one.

-- Offline license payments (bank transfer / cash to the platform) clear through their own SYSTEM wallet so the
-- gateway clearing account matches PayMongo exactly (DECISIONS 76).
alter type public.wallet_kind add value if not exists 'OFFLINE_RECEIPTS';
