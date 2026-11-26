<?php

use App\Enums\AppRole;
use App\Models\User;
use Illuminate\Support\Facades\Hash;

/**
 * The server-side way in (DECISIONS 74): creating the first super admin and recovering one.
 */
it('creates a super admin who must set up 2FA', function () {
    $this->artisan('foxdrop:super-admin', ['email' => 'Owner@FoxDrop.ph', '--name' => 'Platform Owner'])
        ->expectsQuestion('New password (at least 12 characters, upper and lower case and a number)', 'Owner-pass-2026')
        ->assertSuccessful();

    $owner = User::query()->where('email', 'owner@foxdrop.ph')->sole();
    expect($owner->isSuperAdmin())->toBeTrue()
        ->and(Hash::check('Owner-pass-2026', (string) $owner->password))->toBeTrue()
        ->and($owner->two_factor_confirmed_at)->toBeNull();
});

it('resets a locked-out super admin\'s password and authenticator', function () {
    $admin = superAdmin();

    $this->artisan('foxdrop:super-admin', ['email' => $admin->email, '--reset-2fa' => true])
        ->expectsQuestion('New password (at least 12 characters, upper and lower case and a number)', 'Recovered-2026')
        ->assertSuccessful();

    $admin->refresh();
    expect(Hash::check('Recovered-2026', (string) $admin->password))->toBeTrue()
        ->and($admin->two_factor_confirmed_at)->toBeNull()
        ->and($admin->roles->pluck('role')->all())->toContain(AppRole::SuperAdmin);
});

it('refuses a weak password', function () {
    $this->artisan('foxdrop:super-admin', ['email' => 'owner@foxdrop.ph'])
        ->expectsQuestion('New password (at least 12 characters, upper and lower case and a number)', 'short')
        ->assertFailed();

    expect(User::query()->where('email', 'owner@foxdrop.ph')->exists())->toBeFalse();
});

it('will not promote an account someone registered through the app, unless told to', function () {
    $squatter = User::factory()->withEmail('Squatter-pass-2026')->create(['email' => 'owner@foxdrop.ph']);
    $squatter->createToken('phone');

    $this->artisan('foxdrop:super-admin', ['email' => 'owner@foxdrop.ph'])
        ->expectsQuestion('New password (at least 12 characters, upper and lower case and a number)', 'Owner-pass-2026')
        ->assertFailed();
    expect($squatter->fresh()?->isSuperAdmin())->toBeFalse();

    $this->artisan('foxdrop:super-admin', ['email' => 'owner@foxdrop.ph', '--promote-existing' => true])
        ->expectsQuestion('New password (at least 12 characters, upper and lower case and a number)', 'Owner-pass-2026')
        ->assertSuccessful();
    expect($squatter->fresh()?->isSuperAdmin())->toBeTrue()
        ->and($squatter->tokens()->count())->toBe(0); // whatever was signed in before is signed out
});
