-- FoxDrop · 15 · Storage buckets (§3, §17)
-- Public: store and product images. Private: KYC documents, proof-of-delivery photos, receipts — served only through
-- short-lived signed URLs created server-side (≤ 5 min, §17).

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values
  ('store-media', 'store-media', true, 5242880, array['image/jpeg', 'image/png', 'image/webp']),
  ('product-media', 'product-media', true, 5242880, array['image/jpeg', 'image/png', 'image/webp']),
  ('kyc-docs', 'kyc-docs', false, 10485760, array['image/jpeg', 'image/png', 'image/webp', 'application/pdf']),
  ('pod', 'pod', false, 5242880, array['image/jpeg', 'image/webp']),
  ('receipts', 'receipts', false, 5242880, array['image/jpeg', 'image/png', 'image/webp', 'application/pdf'])
on conflict (id) do update
  set public = excluded.public,
      file_size_limit = excluded.file_size_limit,
      allowed_mime_types = excluded.allowed_mime_types;

-- KYC uploads (operator applications, rider documents): a user may only touch their own folder `<user_id>/...`.
-- Reviewers (operators, admins) read them through signed URLs made by Edge Functions.
create policy kyc_docs_own_read on storage.objects for select to authenticated using (
  bucket_id = 'kyc-docs' and (storage.foldername(name))[1] = (select auth.uid())::text
);
create policy kyc_docs_own_insert on storage.objects for insert to authenticated with check (
  bucket_id = 'kyc-docs' and (storage.foldername(name))[1] = (select auth.uid())::text
);
create policy kyc_docs_own_delete on storage.objects for delete to authenticated using (
  bucket_id = 'kyc-docs' and (storage.foldername(name))[1] = (select auth.uid())::text
);

-- store-media / product-media writes arrive with catalog management (Phase 3); pod (Phase 4) and receipts
-- (Phase 5) are written by Edge Functions only. No client policies exist for them yet, so clients cannot write.
