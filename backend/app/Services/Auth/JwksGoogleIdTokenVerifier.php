<?php

namespace App\Services\Auth;

use App\Contracts\GoogleIdTokenVerifier;
use App\Exceptions\GoogleSignInException;
use Firebase\JWT\JWK;
use Firebase\JWT\JWT;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Throwable;

/**
 * Verifies Google ID tokens against Google's published signing keys (cached for an hour).
 */
final class JwksGoogleIdTokenVerifier implements GoogleIdTokenVerifier
{
    private const array ISSUERS = ['accounts.google.com', 'https://accounts.google.com'];

    public function verify(string $idToken, string $rawNonce): GoogleIdentity
    {
        /** @var list<string> $clientIds */
        $clientIds = config('foxdrop.google.client_ids');
        if ($clientIds === []) {
            throw GoogleSignInException::notConfigured();
        }

        try {
            $claims = (array) JWT::decode($idToken, JWK::parseKeySet($this->signingKeys(), 'RS256'));
        } catch (Throwable) {
            throw GoogleSignInException::invalidToken();
        }

        $valid = in_array($claims['iss'] ?? null, self::ISSUERS, true)
            && in_array($claims['aud'] ?? null, $clientIds, true)
            && is_string($claims['sub'] ?? null) && $claims['sub'] !== ''
            && is_string($claims['nonce'] ?? null)
            && hash_equals($claims['nonce'], hash('sha256', $rawNonce));
        if (! $valid) {
            throw GoogleSignInException::invalidToken();
        }

        $emailVerified = ($claims['email_verified'] ?? false) === true;

        return new GoogleIdentity(
            subject: $claims['sub'],
            email: $emailVerified && is_string($claims['email'] ?? null) ? mb_strtolower($claims['email']) : null,
            name: is_string($claims['name'] ?? null) ? $claims['name'] : null,
            pictureUrl: is_string($claims['picture'] ?? null) ? $claims['picture'] : null,
        );
    }

    /**
     * Google's JWKS document, e.g. `{"keys": [{"kty": "RSA", ...}]}`.
     *
     * @return array<string, mixed>
     */
    private function signingKeys(): array
    {
        return Cache::remember('google-id-token-jwks', now()->addHour(), function (): array {
            $jwks = Http::timeout(5)->retry(2, 200)->get((string) config('foxdrop.google.jwks_url'))->throw()->json();

            return is_array($jwks) && is_array($jwks['keys'] ?? null) ? $jwks : throw GoogleSignInException::invalidToken();
        });
    }
}
