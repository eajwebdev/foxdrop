<?php

namespace App\Services\Auth;

use App\Contracts\GoogleIdTokenVerifier;
use App\Exceptions\GoogleSignInException;
use Firebase\JWT\JWK;
use Firebase\JWT\JWT;
use Illuminate\Http\Client\ConnectionException;
use Illuminate\Http\Client\RequestException;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Throwable;
use UnexpectedValueException;

/**
 * Verifies Google ID tokens against Google's published signing keys (cached for an hour, refetched once when a token
 * names a key we have not seen). Each token is accepted once: its nonce is remembered until the token expires, so a
 * captured request cannot be replayed for more API tokens.
 */
final class JwksGoogleIdTokenVerifier implements GoogleIdTokenVerifier
{
    private const array ISSUERS = ['accounts.google.com', 'https://accounts.google.com'];

    private const string KEYS_CACHE = 'google-id-token-jwks';

    public function verify(string $idToken, string $rawNonce): GoogleIdentity
    {
        /** @var list<string> $clientIds */
        $clientIds = config('foxdrop.google.client_ids');
        if ($clientIds === []) {
            throw GoogleSignInException::notConfigured();
        }

        $claims = $this->decode($idToken);

        $valid = in_array($claims['iss'] ?? null, self::ISSUERS, true)
            && in_array($claims['aud'] ?? null, $clientIds, true)
            && is_string($claims['sub'] ?? null) && $claims['sub'] !== ''
            && is_string($claims['nonce'] ?? null)
            && hash_equals($claims['nonce'], hash('sha256', $rawNonce))
            && is_int($claims['exp'] ?? null);
        if (! $valid) {
            throw GoogleSignInException::invalidToken();
        }

        // First use wins; a replay of the same token (same nonce) is refused until the token has expired anyway.
        if (! Cache::add('google-id-token-used:'.$claims['nonce'], true, max(60, $claims['exp'] - time() + 60))) {
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
     * @return array<string, mixed>
     */
    private function decode(string $idToken): array
    {
        try {
            return (array) JWT::decode($idToken, JWK::parseKeySet($this->signingKeys(), 'RS256'));
        } catch (UnexpectedValueException $e) {
            if (! str_contains($e->getMessage(), '"kid"')) {
                throw GoogleSignInException::invalidToken();
            }
            // Google rotated its keys since we cached them: fetch once more.
            Cache::forget(self::KEYS_CACHE);
        } catch (GoogleSignInException $e) {
            throw $e;
        } catch (Throwable) {
            throw GoogleSignInException::invalidToken();
        }

        try {
            return (array) JWT::decode($idToken, JWK::parseKeySet($this->signingKeys(), 'RS256'));
        } catch (GoogleSignInException $e) {
            throw $e;
        } catch (Throwable) {
            throw GoogleSignInException::invalidToken();
        }
    }

    /**
     * Google's JWKS document, e.g. `{"keys": [{"kty": "RSA", ...}]}`.
     *
     * @return array<string, mixed>
     */
    private function signingKeys(): array
    {
        return Cache::remember(self::KEYS_CACHE, now()->addHour(), function (): array {
            try {
                $jwks = Http::timeout(5)->retry(2, 200)->get((string) config('foxdrop.google.jwks_url'))->throw()->json();
            } catch (ConnectionException|RequestException) {
                throw GoogleSignInException::unavailable();
            }

            return is_array($jwks) && is_array($jwks['keys'] ?? null) ? $jwks : throw GoogleSignInException::unavailable();
        });
    }
}
