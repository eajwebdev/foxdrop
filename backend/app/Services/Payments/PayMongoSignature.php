<?php

namespace App\Services\Payments;

/**
 * Checks the `Paymongo-Signature` header: `t=<unix time>,te=<test signature>,li=<live signature>`, where a signature
 * is HMAC-SHA256 of "<t>.<raw body>" with the webhook secret. Events older than 5 minutes are refused (replays).
 */
final class PayMongoSignature
{
    private const int TOLERANCE_SECONDS = 300;

    public function isValid(string $body, string $header): bool
    {
        $secret = (string) config('services.paymongo.webhook_secret');
        if ($secret === '' || $header === '') {
            return false;
        }

        $parts = [];
        foreach (explode(',', $header) as $pair) {
            [$key, $value] = array_pad(explode('=', trim($pair), 2), 2, '');
            $parts[$key] = $value;
        }
        $timestamp = $parts['t'] ?? '';
        if (! ctype_digit($timestamp) || abs(time() - (int) $timestamp) > self::TOLERANCE_SECONDS) {
            return false;
        }

        $livemode = (bool) data_get(json_decode($body, true), 'data.attributes.livemode', false);
        $given = $parts[$livemode ? 'li' : 'te'] ?? '';
        $expected = hash_hmac('sha256', $timestamp.'.'.$body, $secret);

        return $given !== '' && hash_equals($expected, $given);
    }
}
