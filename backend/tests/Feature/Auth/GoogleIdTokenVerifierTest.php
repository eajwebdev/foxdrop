<?php

use App\Exceptions\GoogleSignInException;
use App\Services\Auth\JwksGoogleIdTokenVerifier;
use Firebase\JWT\JWT;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;

const CLIENT_ID = 'foxdrop-web.apps.googleusercontent.com';
const RAW_NONCE = 'raw-nonce-from-the-app-0123456789';

/**
 * OpenSSL options; Windows PHP builds need their bundled openssl.cnf to generate and export keys.
 *
 * @return array<string, string>
 */
function opensslConfig(): array
{
    $windowsConfig = dirname(PHP_BINARY).'/extras/ssl/openssl.cnf';

    return PHP_OS_FAMILY === 'Windows' && is_file($windowsConfig) ? ['config' => $windowsConfig] : [];
}

/**
 * A fresh RSA private key as PEM, plus the key itself.
 *
 * @return array{OpenSSLAsymmetricKey, string}
 */
function newRsaKey(): array
{
    $key = openssl_pkey_new(['private_key_bits' => 2048, 'private_key_type' => OPENSSL_KEYTYPE_RSA] + opensslConfig())
        ?: throw new RuntimeException('Could not generate a test RSA key.');
    openssl_pkey_export($key, $pem, null, opensslConfig());

    return [$key, $pem];
}

/**
 * Google's JWKS document publishing [key] under [kid].
 *
 * @return array{keys: list<array<string, string>>}
 */
function jwksFor(string $kid, OpenSSLAsymmetricKey $key): array
{
    $details = openssl_pkey_get_details($key) ?: throw new RuntimeException('no key details');

    return ['keys' => [[
        'kty' => 'RSA', 'alg' => 'RS256', 'use' => 'sig', 'kid' => $kid,
        'n' => JWT::urlsafeB64Encode($details['rsa']['n']), 'e' => JWT::urlsafeB64Encode($details['rsa']['e']),
    ]]];
}

/**
 * Publishes a throwaway RSA key as Google's JWKS and returns a signer for test ID tokens.
 *
 * @return Closure(array<string, mixed>): string
 */
function fakeGoogleKeys(): Closure
{
    [$key, $pem] = newRsaKey();
    Http::fake(['https://www.googleapis.com/oauth2/v3/certs' => Http::response(jwksFor('test-key', $key))]);
    config(['foxdrop.google.client_ids' => [CLIENT_ID]]);

    return fn (array $overrides): string => JWT::encode(array_merge([
        'iss' => 'https://accounts.google.com',
        'aud' => CLIENT_ID,
        'sub' => '1234567890',
        'email' => 'Ana@Gmail.com',
        'email_verified' => true,
        'name' => 'Ana Villanueva',
        'nonce' => hash('sha256', RAW_NONCE),
        'iat' => time(),
        'exp' => time() + 600,
    ], $overrides), $pem, 'RS256', 'test-key');
}

it('accepts a valid Google ID token', function () {
    $sign = fakeGoogleKeys();

    $identity = app(JwksGoogleIdTokenVerifier::class)->verify($sign([]), RAW_NONCE);

    expect($identity->subject)->toBe('1234567890')
        ->and($identity->email)->toBe('ana@gmail.com')
        ->and($identity->name)->toBe('Ana Villanueva');
});

it('ignores an email Google has not verified', function () {
    $sign = fakeGoogleKeys();

    expect(app(JwksGoogleIdTokenVerifier::class)->verify($sign(['email_verified' => false]), RAW_NONCE)->email)->toBeNull();
});

it('rejects tokens that are not for FoxDrop or not from this sign-in', function (array $overrides, string $nonce) {
    $sign = fakeGoogleKeys();

    app(JwksGoogleIdTokenVerifier::class)->verify($sign($overrides), $nonce);
})->with([
    'another app' => [['aud' => 'someone-else.apps.googleusercontent.com'], RAW_NONCE],
    'another issuer' => [['iss' => 'https://evil.example.com'], RAW_NONCE],
    'replayed nonce' => [[], 'a-different-raw-nonce-0123456789'],
    'expired' => [['exp' => time() - 120, 'iat' => time() - 900], RAW_NONCE],
])->throws(GoogleSignInException::class);

it('rejects a token signed with someone else\'s key', function () {
    fakeGoogleKeys();
    [, $pem] = newRsaKey();
    $forged = JWT::encode(['iss' => 'https://accounts.google.com', 'aud' => CLIENT_ID, 'sub' => 'x', 'nonce' => hash('sha256', RAW_NONCE), 'exp' => time() + 600], $pem, 'RS256', 'test-key');

    app(JwksGoogleIdTokenVerifier::class)->verify($forged, RAW_NONCE);
})->throws(GoogleSignInException::class);

it('accepts each Google token only once', function () {
    $token = fakeGoogleKeys()([]);
    app(JwksGoogleIdTokenVerifier::class)->verify($token, RAW_NONCE);

    app(JwksGoogleIdTokenVerifier::class)->verify($token, RAW_NONCE);
})->throws(GoogleSignInException::class);

it('fetches Google\'s keys again when a token uses a key it has not seen', function () {
    config(['foxdrop.google.client_ids' => [CLIENT_ID]]);
    [$oldKey] = newRsaKey();
    [$newKey, $newPem] = newRsaKey();
    // After the cached (old) key set fails, the verifier fetches once more and gets the rotated set.
    Http::fake(['https://www.googleapis.com/oauth2/v3/certs' => Http::response(jwksFor('rotated-key', $newKey))]);
    Cache::put('google-id-token-jwks', ['jwks' => jwksFor('old-key', $oldKey), 'fetched_at' => time() - 600], now()->addHour()); // what we cached before rotation
    $token = JWT::encode([
        'iss' => 'https://accounts.google.com', 'aud' => CLIENT_ID, 'sub' => '42', 'nonce' => hash('sha256', RAW_NONCE),
        'iat' => time(), 'exp' => time() + 600,
    ], $newPem, 'RS256', 'rotated-key');

    expect(app(JwksGoogleIdTokenVerifier::class)->verify($token, RAW_NONCE)->subject)->toBe('42');
});
it('tells the app to retry later when Google\'s keys cannot be fetched, without hammering Google', function () {
    config(['foxdrop.google.client_ids' => [CLIENT_ID]]);
    Http::fake(['https://www.googleapis.com/oauth2/v3/certs' => Http::response('down', 500)]);
    $signIn = fn () => $this->postJson(route('api.v1.auth.google'), ['id_token' => 'a.b.c', 'nonce' => str_repeat('n', 32), 'device_name' => 'Pixel']);

    $signIn()->assertStatus(502)->assertJsonPath('message', 'Google sign-in is unavailable right now. Please try again in a moment.');
    $calls = count(Http::recorded());
    $signIn()->assertStatus(502);

    expect(Http::recorded())->toHaveCount($calls);
});
