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
 * Verifies Google ID tokens against Google's published signing keys (cached for an hour, refetched when a token names a
 * key we have not seen and the cached keys are over 5 minutes old). Each token is accepted once: its nonce is remembered until the token expires, so a
 * captured request cannot be replayed for more API tokens.
 */
final class JwksGoogleIdTokenVerifier implements GoogleIdTokenVerifier
{
    private const array ISSUERS = ['accounts.google.com', 'https://accounts.google.com'];

    private const string KEYS_CACHE = 'google-id-token-jwks';

    private const string UNAVAILABLE_CACHE = 'google-id-token-jwks-unavailable';

    private const int REFETCH_AFTER_SECONDS = 300;

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
    private function decode(string $idToken, bool $mayRefetchKeys = true): array
    {
        try {
            return (array) JWT::decode($idToken, JWK::parseKeySet($this->signingKeys()['jwks'], 'RS256'));
        } catch (GoogleSignInException $e) {
            throw $e;
        } catch (UnexpectedValueException $e) {
            // An unknown key id: Google may have rotated its keys since we cached them. Refetch at most every
            // 5 minutes, so forged key ids cannot make every request call Google.
            $unknownKey = str_contains($e->getMessage(), '"kid"');
            if ($mayRefetchKeys && $unknownKey && $this->signingKeys()['fetched_at'] < time() - self::REFETCH_AFTER_SECONDS) {
                Cache::forget(self::KEYS_CACHE);

                return $this->decode($idToken, mayRefetchKeys: false);
            }
            throw GoogleSignInException::invalidToken();
        } catch (Throwable) {
            throw GoogleSignInException::invalidToken();
        }
    }

    /**
     * Google's JWKS document (`{"keys": [...]}`) and when we fetched it. While Google cannot be reached, the failure is
     * remembered for 30 seconds so waiting requests do not pile up on it.
     *
     * @return array{jwks: array<string, mixed>, fetched_at: int}
     */
    private function signingKeys(): array
    {
        if (Cache::has(self::UNAVAILABLE_CACHE)) {
            throw GoogleSignInException::unavailable();
        }

        return Cache::remember(self::KEYS_CACHE, now()->addHour(), function (): array {
            try {
                $jwks = Http::timeout(5)->retry(2, 200)->get((string) config('foxdrop.google.jwks_url'))->throw()->json();
            } catch (ConnectionException|RequestException) {
                $jwks = null;
            }
            if (! is_array($jwks) || ! is_array($jwks['keys'] ?? null)) {
                Cache::put(self::UNAVAILABLE_CACHE, true, now()->addSeconds(30));
                throw GoogleSignInException::unavailable();
            }

            return ['jwks' => $jwks, 'fetched_at' => time()];
        });
    }
}
