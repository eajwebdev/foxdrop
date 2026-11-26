<?php

/*
| FoxDrop settings. Values come from the environment; secrets never live in code (MASTER_PROMPT §17).
*/

return [

    'auth' => [
        // Sign-in codes sent by SMS (§3).
        'otp' => [
            'length' => 6,
            'ttl_seconds' => 300,
            'resend_after_seconds' => 60,
            'max_attempts' => 5,
        ],

        /*
         * Fixed codes for demo phone numbers ("+639170000001:123456,..."). Honoured only in the local and testing
         * environments, never in production (AppServiceProvider refuses to boot with it set there).
         */
        'test_otps' => collect(explode(',', (string) env('FOXDROP_TEST_OTPS', '')))
            ->filter(fn (string $pair): bool => str_contains($pair, ':'))
            ->mapWithKeys(function (string $pair): array {
                [$phone, $code] = explode(':', trim($pair), 2);

                return [$phone => $code];
            })
            ->all(),

        // A mobile token lasts 90 days; signing in again issues a new one.
        'token_ttl_minutes' => (int) env('FOXDROP_TOKEN_TTL_MINUTES', 60 * 24 * 90),
    ],

    'google' => [
        // OAuth client IDs whose ID tokens we accept (the Android apps request tokens for the web client ID).
        'client_ids' => array_values(array_filter(explode(',', (string) env('GOOGLE_CLIENT_IDS', '')))),
        'jwks_url' => 'https://www.googleapis.com/oauth2/v3/certs',
    ],

    'payments' => [
        // `fake` = local checkout page (local/testing only), `paymongo` = PayMongo Checkout Sessions.
        'driver' => env('PAYMENT_DRIVER', 'fake'),
        'checkout_ttl_minutes' => 30,
    ],

    'ordering' => [
        // Straight-line km × this when no Google Routes key is set (DECISIONS 85).
        'road_factor' => 1.25,
        // A quote can be turned into an order this long.
        'quote_ttl_minutes' => 10,
        // Store "prep time" plus this many minutes per km gives the ETA range customers see.
        'minutes_per_km' => 3,
        // Wrong pick-up codes before the store has to wait (DECISIONS 87).
        'pickup_code_attempts' => 5,
        'pickup_code_lock_minutes' => 10,
    ],

    'documents' => [
        // Signed document links for reviewers stay valid this long.
        'url_ttl_minutes' => 10,
        // The apps send ~1600 px JPEGs (well under 1 MB); 4 MB leaves room for other clients.
        'max_kilobytes' => 4096,
        // Photos wider or higher than this are refused before decoding (memory).
        'max_source_pixels' => 6000,
        // Uploaded photos are re-encoded to JPEG no larger than this (drops EXIF/GPS).
        'max_pixels' => 2000,
    ],

    'sms' => [
        // `log` writes the message to the log (local only). A real provider is added before real users test.
        'driver' => env('SMS_DRIVER', 'log'),
    ],

];
