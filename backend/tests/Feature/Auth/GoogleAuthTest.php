<?php

use App\Contracts\GoogleIdTokenVerifier;
use App\Exceptions\GoogleSignInException;
use App\Models\User;
use App\Services\Auth\GoogleIdentity;
use Illuminate\Testing\TestResponse;

function fakeGoogle(?GoogleIdentity $identity): void
{
    app()->instance(GoogleIdTokenVerifier::class, new class($identity) implements GoogleIdTokenVerifier
    {
        public function __construct(private readonly ?GoogleIdentity $identity) {}

        public function verify(string $idToken, string $rawNonce): GoogleIdentity
        {
            return $this->identity ?? throw GoogleSignInException::invalidToken();
        }
    });
}

function signInWithGoogle(): TestResponse
{
    return test()->postJson(route('api.v1.auth.google'), [
        'id_token' => 'header.payload.signature', 'nonce' => str_repeat('n', 32), 'device_name' => 'Pixel',
    ]);
}

it('creates an account for a new Google user', function () {
    fakeGoogle(new GoogleIdentity('google-sub-1', 'ana@gmail.com', 'Ana Villanueva', 'https://example.com/a.png'));

    signInWithGoogle()->assertCreated()->assertJsonPath('data.email', 'ana@gmail.com')->assertJsonPath('data.full_name', 'Ana Villanueva');
    expect(User::query()->sole()->email_verified_at)->not->toBeNull();
});

it('signs a returning Google user into the same account', function () {
    $user = User::factory()->create(['google_sub' => 'google-sub-1']);
    fakeGoogle(new GoogleIdentity('google-sub-1', null, null, null));

    signInWithGoogle()->assertOk()->assertJsonPath('data.id', $user->id);
});

it('links Google to a verified email account and keeps its password', function () {
    $user = User::factory()->withEmail('secret123')->create(['email' => 'ana@gmail.com', 'email_verified_at' => now()]);
    fakeGoogle(new GoogleIdentity('google-sub-1', 'ana@gmail.com', 'Ana', null));

    signInWithGoogle()->assertOk()->assertJsonPath('data.id', $user->id);
    expect($user->refresh()->google_sub)->toBe('google-sub-1')->and($user->password)->not->toBeNull();
});

it('takes over an unverified email account for the proven Google owner', function () {
    $squatter = User::factory()->withEmail('secret123')->create(['email' => 'ana@gmail.com', 'email_verified_at' => null]);
    $squatter->createToken('squatter-phone');
    fakeGoogle(new GoogleIdentity('google-sub-1', 'ana@gmail.com', 'Ana', null));

    signInWithGoogle()->assertOk()->assertJsonPath('data.id', $squatter->id);

    $squatter->refresh();
    expect($squatter->password)->toBeNull()
        ->and($squatter->email_verified_at)->not->toBeNull()
        ->and($squatter->tokens()->pluck('name')->all())->toBe(['Pixel']);
});

it('rejects a token Google did not issue', function () {
    fakeGoogle(null);

    signInWithGoogle()->assertUnprocessable()->assertJsonPath('message', 'We could not verify your Google account. Please try again.');
});

it('says Google sign-in is unavailable when no client ID is configured', function () {
    config(['foxdrop.google.client_ids' => []]);

    signInWithGoogle()->assertServiceUnavailable();
});

it('refuses to move an email to a second Google account', function () {
    $user = User::factory()->create(['email' => 'ana@gmail.com', 'email_verified_at' => now(), 'google_sub' => 'google-sub-1']);
    fakeGoogle(new GoogleIdentity('google-sub-2', 'ana@gmail.com', 'Ana', null));

    signInWithGoogle()->assertConflict();
    expect($user->refresh()->google_sub)->toBe('google-sub-1');
});
