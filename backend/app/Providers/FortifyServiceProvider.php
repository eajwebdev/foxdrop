<?php

namespace App\Providers;

use App\Actions\Audit\RecordAudit;
use App\Models\User;
use App\Support\IdentityConfirmation;
use Illuminate\Auth\Events\Failed;
use Illuminate\Auth\Events\Login;
use Illuminate\Auth\Events\Logout;
use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Event;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Support\ServiceProvider;
use Illuminate\Support\Str;
use Inertia\Inertia;
use Laravel\Fortify\Events\RecoveryCodeReplaced;
use Laravel\Fortify\Events\RecoveryCodesGenerated;
use Laravel\Fortify\Events\TwoFactorAuthenticationConfirmed;
use Laravel\Fortify\Events\TwoFactorAuthenticationFailed;
use Laravel\Fortify\Fortify;

/**
 * Admin web sign-in (§20.4, §20.6, DECISIONS 74): email + password for active admin accounts, then TOTP. Fortify's
 * own routes are off; routes/admin.php registers only the ones the admin web uses.
 */
class FortifyServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        Fortify::ignoreRoutes();
    }

    public function boot(): void
    {
        $this->configureAuthentication();
        $this->configureViews();
        $this->configureRateLimiting();
        $this->auditSignInEvents();
    }

    /**
     * Only ACTIVE admin accounts may sign in; everyone else gets the same "credentials do not match" answer, and a
     * password is always hashed so the reply time does not tell whether the email exists.
     */
    private function configureAuthentication(): void
    {
        Fortify::authenticateUsing(function (Request $request): ?User {
            $email = Str::lower($request->string('email')->trim()->toString());
            $password = $request->string('password')->toString();
            $user = $email === '' ? null : User::query()->where('email', $email)->first();

            $passwordMatches = Hash::check($password, $user->password ?? Hash::make(Str::random(32)));

            return $user !== null && $passwordMatches && $user->canUseAdminWeb() ? $user : null;
        });
    }

    private function configureViews(): void
    {
        Fortify::loginView(fn (Request $request) => Inertia::render('auth/login', [
            'status' => $request->session()->get('status'),
        ]));
        Fortify::twoFactorChallengeView(fn () => Inertia::render('auth/two-factor-challenge'));
    }

    /**
     * §20.6: 5 tries a minute per email + IP, and 20 an hour per email (a slow guess at one account locks it out
     * for the hour). The 2FA step has its own limit per pending sign-in.
     */
    private function configureRateLimiting(): void
    {
        RateLimiter::for('login', function (Request $request): array {
            $email = Str::transliterate(Str::lower($request->string(Fortify::username())->toString()));

            return [
                Limit::perMinute(5)->by('admin-login:'.$email.'|'.$request->ip()),
                Limit::perHour(20)->by('admin-login:'.$email),
            ];
        });
        RateLimiter::for('two-factor', fn (Request $request): Limit => Limit::perMinute(5)->by('admin-2fa:'.$request->session()->get('login.id')));
    }

    /**
     * Sign-in history lives in the audit log (§20.4): sign-ins, sign-outs, failed attempts on admin accounts and
     * 2FA changes.
     */
    private function auditSignInEvents(): void
    {
        $audit = fn (?User $actor, string $action, User $subject) => app(RecordAudit::class)($actor, $action, 'user', $subject->id);

        Event::listen(function (Login $event) use ($audit): void {
            if ($event->guard === 'web' && $event->user instanceof User) {
                // Password + authenticator were just checked: that counts as "confirm it's you" (DECISIONS 75).
                IdentityConfirmation::markConfirmed(request());
                $audit($event->user, 'admin.signed_in', $event->user);
            }
        });
        Event::listen(function (Logout $event) use ($audit): void {
            if ($event->guard === 'web' && $event->user instanceof User) {
                $audit($event->user, 'admin.signed_out', $event->user);
            }
        });
        // Fortify reports a failed sign-in without the user (custom authentication); look the account up by email.
        Event::listen(function (Failed $event) use ($audit): void {
            $email = $event->credentials[Fortify::username()] ?? null;
            $user = $event->guard === 'web' && is_string($email) ? User::query()->where('email', Str::lower(trim($email)))->first() : null;
            if ($user !== null && $user->isPlatformAdmin()) {
                $audit(null, 'admin.sign_in_failed', $user);
            }
        });
        Event::listen(fn (TwoFactorAuthenticationFailed $event) => $audit(null, 'admin.two_factor_failed', $event->user));
        Event::listen(fn (TwoFactorAuthenticationConfirmed $event) => $audit($event->user, 'admin.two_factor_enabled', $event->user));
        Event::listen(fn (RecoveryCodesGenerated $event) => $audit($event->user, 'admin.recovery_codes_generated', $event->user));
        Event::listen(fn (RecoveryCodeReplaced $event) => $event->user instanceof User ? $audit($event->user, 'admin.recovery_code_used', $event->user) : null);
    }
}
