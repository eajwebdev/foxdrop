<?php

use App\Models\User;

it('registers an email account and signs it in', function () {
    $this->postJson(route('api.v1.auth.email.register'), [
        'full_name' => 'Maria Santos', 'email' => '  Maria@Example.com ', 'password' => 'secret123', 'device_name' => 'Pixel',
    ])
        ->assertCreated()
        ->assertJsonPath('data.email', 'maria@example.com')
        ->assertJsonPath('data.roles.0.role', 'customer');

    expect(User::query()->where('email', 'maria@example.com')->sole()->password)->not->toBe('secret123');
});

it('refuses a taken email, regardless of letter case', function () {
    User::factory()->create(['email' => 'maria@example.com']);

    $this->postJson(route('api.v1.auth.email.register'), [
        'full_name' => 'Maria', 'email' => 'MARIA@example.com', 'password' => 'secret123', 'device_name' => 'Pixel',
    ])->assertUnprocessable()->assertJsonValidationErrors('email');
});

it('requires a password with letters and numbers, at least 8 long', function (string $password) {
    $this->postJson(route('api.v1.auth.email.register'), [
        'full_name' => 'Maria', 'email' => 'maria@example.com', 'password' => $password, 'device_name' => 'Pixel',
    ])->assertUnprocessable()->assertJsonValidationErrors('password');
})->with(['short1', 'onlyletters', '12345678']);

it('signs in with the right password', function () {
    $user = User::factory()->withEmail('secret123')->create();

    $this->postJson(route('api.v1.auth.email.login'), ['email' => strtoupper($user->email), 'password' => 'secret123', 'device_name' => 'Pixel'])
        ->assertOk()
        ->assertJsonPath('data.id', $user->id)
        ->assertJsonStructure(['token', 'expires_at']);
});

it('gives one message for a wrong password and an unknown email', function (string $email) {
    User::factory()->withEmail('secret123')->create(['email' => 'maria@example.com']);

    $this->postJson(route('api.v1.auth.email.login'), ['email' => $email, 'password' => 'wrong-pass1', 'device_name' => 'Pixel'])
        ->assertUnprocessable()
        ->assertJsonValidationErrors(['email' => 'The email or password is incorrect.']);
})->with(['maria@example.com', 'nobody@example.com']);

it('does not let a phone-only account sign in with an empty password', function () {
    User::factory()->create(['email' => 'phone-user@example.com', 'password' => null]);

    $this->postJson(route('api.v1.auth.email.login'), ['email' => 'phone-user@example.com', 'password' => 'anything1', 'device_name' => 'Pixel'])
        ->assertUnprocessable();
});

it('slows down password guessing', function () {
    User::factory()->withEmail('secret123')->create(['email' => 'maria@example.com']);
    foreach (range(1, 10) as $i) {
        $this->postJson(route('api.v1.auth.email.login'), ['email' => 'maria@example.com', 'password' => 'wrong-pass1', 'device_name' => 'Pixel']);
    }

    $this->postJson(route('api.v1.auth.email.login'), ['email' => 'maria@example.com', 'password' => 'secret123', 'device_name' => 'Pixel'])
        ->assertTooManyRequests();
});

it('answers 422, not a crash, when a field is sent as a list', function () {
    $this->postJson(route('api.v1.auth.email.login'), ['email' => ['a@b.ph'], 'password' => 'secret123', 'device_name' => 'Pixel'])
        ->assertUnprocessable()
        ->assertJsonValidationErrors('email');
});
