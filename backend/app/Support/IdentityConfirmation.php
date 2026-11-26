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
 * while 2FA is not set up yet). A success counts for `auth.password_timeout` seconds (15 min), using Laravel's own
 * session key, so `RequirePassword` pages and dangerous actions share it.
 */
final class IdentityConfirmation
{
    private const string SESSION_KEY = 'auth.password_confirmed_at';

    public static function confirmedUntil(Request $request): ?int
    {
        $at = $request->session()->get(self::SESSION_KEY);
        $until = is_int($at) ? $at + (int) config('auth.password_timeout') : null;

        return $until !== null && $until > now()->unix() ? $until : null;
    }

    /**
     * Checks the password and code; 5 wrong tries a minute per account, then a wait.
     *
     * @throws ValidationException
     */
    public static function confirm(Request $request, User $user, string $password, ?string $code): void
    {
        $key = 'confirm-identity:'.$user->id;
        if (RateLimiter::tooManyAttempts($key, 5)) {
            throw ValidationException::withMessages(['confirm_password' => 'Too many tries. Please wait '.RateLimiter::availableIn($key).' seconds.']);
        }

        if (! self::matches($user, $password, $code)) {
            RateLimiter::hit($key);

            throw ValidationException::withMessages(['confirm_password' => 'The password or authenticator code is not right.']);
        }

        RateLimiter::clear($key);
        $request->session()->put(self::SESSION_KEY, now()->unix());
    }

    private static function matches(User $user, string $password, ?string $code): bool
    {
        if ($user->password === null || ! Hash::check($password, $user->password)) {
            return false;
        }
        if ($user->two_factor_confirmed_at === null || $user->two_factor_secret === null) {
            return true;
        }

        return $code !== null && app(TwoFactorAuthenticationProvider::class)->verify(
            Fortify::currentEncrypter()->decrypt($user->two_factor_secret),
            $code,
        );
    }
}
