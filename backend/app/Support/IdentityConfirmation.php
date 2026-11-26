<?php

namespace App\Support;

use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Validation\ValidationException;
use Laravel\Fortify\Contracts\TwoFactorAuthenticationProvider;
use Laravel\Fortify\Fortify;

/**
 * "Confirm it's you" (§20.6, DECISIONS 75): the admin's password plus a current authenticator code (password only
 * while 2FA is not set up yet). A success counts for `auth.password_timeout` seconds (15 min) through Laravel's own
 * password-confirmation session mark, so `RequirePassword` pages and dangerous actions share it. Signing in (password
 * + code) counts as a confirmation too.
 */
final class IdentityConfirmation
{
    /** The form fields the confirm page and the in-dialog confirmation send. */
    private const array RULES = [
        'confirm_password' => ['required', 'string', 'max:200'],
        'confirm_code' => ['nullable', 'string', 'max:20'],
    ];

    public static function confirmedUntil(Request $request): ?int
    {
        $at = $request->session()->get('auth.password_confirmed_at');
        $until = is_int($at) ? $at + (int) config('auth.password_timeout') : null;

        return $until !== null && $until > now()->unix() ? $until : null;
    }

    public static function markConfirmed(Request $request): void
    {
        $request->session()->passwordConfirmed();
    }

    /**
     * Validates and checks the password and code the request carries; 5 wrong tries a minute per account.
     *
     * @throws ValidationException
     */
    public static function confirmFromRequest(Request $request, User $user): void
    {
        $request->validate(self::RULES, ['confirm_password.required' => 'Confirm it\'s you: enter your password and authenticator code.']);

        $key = 'confirm-identity:'.$user->id;
        if (RateLimiter::tooManyAttempts($key, 5)) {
            throw ValidationException::withMessages(['confirm_password' => 'Too many tries. Please wait '.RateLimiter::availableIn($key).' seconds.']);
        }

        $code = $request->input('confirm_code');
        if (! self::matches($user, $request->string('confirm_password')->toString(), is_string($code) ? $code : null)) {
            RateLimiter::hit($key);

            throw ValidationException::withMessages(['confirm_password' => 'The password or authenticator code is not right.']);
        }

        RateLimiter::clear($key);
        self::markConfirmed($request);
    }

    private static function matches(User $user, string $password, ?string $code): bool
    {
        if ($user->password === null || ! Hash::check($password, $user->password)) {
            return false;
        }
        if (! $user->hasEnabledTwoFactorAuthentication()) {
            return true;
        }

        return $code !== null && app(TwoFactorAuthenticationProvider::class)->verify(
            Fortify::currentEncrypter()->decrypt((string) $user->two_factor_secret),
            $code,
        );
    }
}
