# FoxDrop API (v1)

Base URL: `{FOXDROP_API_URL}` = `https://<host>/api/v1/` (local emulator: `http://10.0.2.2:8000/api/v1/`).

## Conventions
- **Format**: JSON, snake_case. Every request sends `Accept: application/json`. File uploads use
  `multipart/form-data`.
- **Auth**: `Authorization: Bearer <token>` (Sanctum). Signed-out routes are marked *public*.
- **Money** is integer centavos (`*_centavos`). Percentages are basis points (`*_bps`, 8000 = 80 %). Distances are
  km as decimals. Timestamps are ISO-8601 UTC.
- **Errors** use Laravel's shape `{ "message": "...", "errors": { "field": ["..."] } }`:

  | Status | Meaning |
  |---|---|
  | 401 | token missing, expired or revoked |
  | 403 | not allowed (also a suspended account) |
  | 404 | not found or not visible to you |
  | 409 | conflict with current state, e.g. province already taken |
  | 422 | validation |
  | 429 | rate limited (`Retry-After`) |
  | 502 | an upstream such as Google or PayMongo is unreachable |
  | 503 | a feature is not configured on this server |
- **Lists** are `{ "data": [...] }`. Pagination is added when lists can grow (Phase 3+).

## Sign-in (Phase 1b)
| Method | Path | Body | Result |
|---|---|---|---|
| POST | `auth/otp/send` *public* | `phone` (+63…) | 202 `{expires_in_seconds, resend_after_seconds}`; 429 with `retry_after_seconds` |
| POST | `auth/otp/verify` *public* | `phone, code, device_name, full_name?` | 200/201 `SignedIn` |
| POST | `auth/email/register` *public* | `full_name, email, password, device_name` | 201 `SignedIn` |
| POST | `auth/email/login` *public* | `email, password, device_name` | 200 `SignedIn` |
| POST | `auth/google` *public* | `id_token, nonce, device_name` | 200/201 `SignedIn`; 409 email linked to another Google account; 502/503 |
| POST | `auth/logout` | — | 204 |
| GET | `me` | — | `{data: User}` |

- `SignedIn` = `{data: User, token, token_type: "Bearer", expires_at}`.
- `User` = `{id, full_name, phone, email, avatar_url, roles: [{role, province_id, store_id}]}`.

## Provinces
| Method | Path | Result |
|---|---|---|
| GET | `provinces` *public* | `{data: [{id, psgc_code, name, region_name, is_serviceable, is_available}]}` |

- `is_serviceable`: customers can order there (enabled, with an ACTIVE or GRACE license).
- `is_available`: an operator can apply (enabled, with no live license).

## Operator license (Phase 2)
| Method | Path | Body | Result |
|---|---|---|---|
| POST | `operator/applications` | multipart: `province_id, business_name, registration_number?, contact_phone, contact_email?, valid_id` (image), `business_permit?` (image) | 201 `{data: OperatorApplication}`; 409 province not available / you already have an open application or a license |
| GET | `operator/applications/mine` | — | `{data: OperatorApplication \| null}` (latest) |
| GET | `operator/license` | — | `{data: License}`; 404 when you hold none |
| POST | `licenses/{license}/payments` | — | 201 `{data: Payment}` (holder only; license PENDING_PAYMENT, ACTIVE or GRACE; returns the open PENDING payment if there is one); 409 otherwise |
| GET | `payments/{payment}` | — | `{data: Payment}` (payer only) |

- **`OperatorApplication`**: `{id, status: SUBMITTED|APPROVED|REJECTED|WITHDRAWN, province: {id, name}, business_name, rejection_reason, created_at, license: License|null}`.
- **`License`**: `{id, province: {id, name}, status: APPLIED|PENDING_PAYMENT|ACTIVE|GRACE|EXPIRED|SUSPENDED|REJECTED, start_at, end_at, days_left, fee_centavos, can_pay}`.
  - `days_left` is the number of whole days until `end_at` (negative in GRACE), or null.
  - `can_pay` is true when a payment would be accepted now.
- **`Payment`**: `{id, purpose: LICENSE|TOPUP|ORDER, status: PENDING|PAID|FAILED|EXPIRED, amount_centavos, checkout_url, expires_at}`.

### How a payment finishes
- The app opens `checkout_url` in a Custom Tab, then polls `payments/{id}` (after 2, 2, 3 and 5 s, then every 8 s, for about 2 min), or until the
  status is no longer PENDING.
- **PayMongo**: `POST webhooks/paymongo` (public, signature-checked) marks the payment PAID, then activates or
  extends the license.
- **Local / testing (fake checkout)**: `checkout_url` is `{APP_URL}/dev/checkout/{payment}`. That web page has Pay
  and Cancel buttons that run the same success or failure handling as the webhook.

## Territory (Phase 2, operator of the province)
| Method | Path | Body | Result |
|---|---|---|---|
| GET | `operator/zones` | — | `{data: [Zone]}` (active zones of your province) |
| POST | `operator/zones` | `{name, center: {lat, lng}, radius_km}` (0.5–30) or `{name, polygon: [[lng, lat], …]}` (≥ 3 points) | 201 `{data: Zone}` |
| DELETE | `operator/zones/{zone}` | — | 204 (deactivates) |
| GET | `operator/rate-card` | — | `{data: RateCard}` (current) |
| POST | `operator/rate-cards` | RateCard fields (no `id`) | 201 `{data: RateCard}` (new version, effective now) |
| POST | `operator/rate-cards/preview` | RateCard fields + `distances_km: [number]` (1–10 values) | `{data: [{distance_km, delivery_fee_centavos, rider_earning_centavos, operator_cut_centavos}]}` |
| GET | `operator/province` | — | `{data: {id, name, center: {lat, lng}\|null, store_fee_centavos, store_fee_grace_days}}` |
| PATCH | `operator/province` | `{store_fee_centavos}` (0–100000) | `{data: …}` |

- **`Zone`**: `{id, name, is_active, center: {lat, lng}|null, radius_km|null, area_km2}`.
- **`RateCard`**: `{id, base_fare_centavos, base_km, rate_per_km_centavos, min_fee_centavos, max_km, rider_share_bps, night_surcharge_centavos, bad_weather_surcharge_centavos, extra_stop_centavos, effective_from}`.
- **Fee formula (§6)**:

  ```
  fee = max(min_fee, base_fare + ceil_to_0.1(max(0, km − base_km)) × rate_per_km)
  ```

  - Rounding is half-up to centavos.
  - Distances over `max_km` are not served (`delivery_fee_centavos: null`).
  - `rider_earning = fee × rider_share_bps / 10000` (half-up), and `operator_cut = fee − rider_earning`.

## Partner onboarding (Phase 2)
### Stores
| Method | Path | Body | Result |
|---|---|---|---|
| POST | `stores` | multipart: `province_id, name, category: FOOD|GROCERIES|MEDICINE|MORE, description?, address_line, barangay?, city, phone?, latitude, longitude, business_permit` (image), `valid_id` (image) | 201 `{data: MyStore}`; 409 province not serviceable or you already have a store under review |
| GET | `stores/mine` | — | `{data: [MyStore]}` |
| GET | `operator/stores?status=SUBMITTED` | — | `{data: [StoreSummary]}` (your province; also `ACTIVE`, `REJECTED`, …) |
| GET | `operator/stores/{store}` | — | `{data: StoreReview}` |
| POST | `operator/stores/{store}/approve` | — | `{data: StoreReview}` (SUBMITTED → ACTIVE) |
| POST | `operator/stores/{store}/reject` | `{reason}` (5–300 chars) | `{data: StoreReview}` (SUBMITTED → REJECTED) |

- **`MyStore`**: `{id, name, category, status, rejection_reason, province: {id, name}, created_at}`.
- **`StoreSummary`**: `{id, name, category, status, address_line, city, owner: {full_name, phone}, created_at}`.
- **`StoreReview`**: `StoreSummary` + `{description, barangay, phone, location: {lat, lng}, rejection_reason, documents: [Document]}`.
- **`Document`**: `{type, url (signed, 10 min), status}`.
- **Resubmit**: posting `stores` again while your latest store is REJECTED updates that store back to SUBMITTED.

### Riders
| Method | Path | Body | Result |
|---|---|---|---|
| POST | `riders` | multipart: `province_id, full_name, vehicle_type: MOTORCYCLE|BICYCLE|TRICYCLE|CAR, plate_no?` (required unless BICYCLE), `license_no, license_expiry` (YYYY-MM-DD, future), `valid_id, drivers_license, selfie` (images), `or_cr, vehicle_photo` (images, required unless BICYCLE) | 201 `{data: MyRider}` (→ DOCS_REVIEW); 409 already applied / approved, or province not serviceable |
| GET | `riders/me` | — | `{data: MyRider \| null}` |
| GET | `operator/riders?status=DOCS_REVIEW` | — | `{data: [RiderSummary]}` |
| GET | `operator/riders/{rider}` | — | `{data: RiderReview}` |
| POST | `operator/riders/{rider}/approve` | — | `{data: RiderReview}` (DOCS_REVIEW → APPROVED) |
| POST | `operator/riders/{rider}/reject` | `{reason}` | `{data: RiderReview}` (→ REJECTED) |

- **`MyRider`**: `{id, status: APPLIED|DOCS_REVIEW|APPROVED|REJECTED|SUSPENDED|DEACTIVATED, rejection_reason, province: {id, name}, vehicle_type, plate_no, license_no_last4, license_expiry}`.
- **`RiderSummary`**: `{id, status, full_name, phone, vehicle_type, plate_no, created_at}`.
- **`RiderReview`**: `RiderSummary` + `{license_no_last4, license_expiry, rejection_reason, documents: [Document]}`.
- **Resubmit**: posting `riders` again while REJECTED replaces the documents and returns the rider to DOCS_REVIEW.

### Who may review
- The operator team of the store's or rider's province reviews.
- Platform admins may review any province (override).
- Every review writes an `audit_logs` row.
- Approval grants the role (`store_owner` with the store, `rider` with the province) and creates the wallets.
- The apps pick up the new role from `/me`.
