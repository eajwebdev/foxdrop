<?php

namespace App\Providers;

use App\Contracts\GoogleIdTokenVerifier;
use App\Contracts\SmsSender;
use App\Services\Auth\JwksGoogleIdTokenVerifier;
use App\Services\Sms\LogSmsSender;
use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Support\ServiceProvider;
use RuntimeException;

class AppServiceProvider extends ServiceProvider
{
    /**
     * @var array<class-string, class-string>
     */
    public array $bindings = [
        GoogleIdTokenVerifier::class => JwksGoogleIdTokenVerifier::class,
    ];

    public function register(): void
    {
        $this->app->bind(SmsSender::class, function (): SmsSender {
            return match (config('foxdrop.sms.driver')) {
                'log' => new LogSmsSender,
                default => throw new RuntimeException('Unknown SMS_DRIVER: '.config('foxdrop.sms.driver')),
            };
        });
    }

    public function boot(): void
    {
        $this->refuseUnsafeProductionConfig();
        $this->configureRateLimits();
    }

    /**
     * Fixed demo codes and a log-only SMS driver must never reach production.
     */
    private function refuseUnsafeProductionConfig(): void
    {
        if (! $this->app->environment('production')) {
            return;
        }
        if (config('foxdrop.auth.test_otps') !== []) {
            throw new RuntimeException('FOXDROP_TEST_OTPS must be empty in production.');
        }
        if (config('foxdrop.sms.driver') === 'log') {
            throw new RuntimeException('SMS_DRIVER=log cannot send real codes; configure an SMS provider for production.');
        }
    }

    /**
     * §17: rate limits on sign-in. The tight limits are per phone number / email, so nobody can spam one number or
     * guess one account's password. Per-IP limits are loose on purpose: Philippine mobile networks put many
     * subscribers behind one carrier IP (CGNAT), so they only stop bulk abuse from a single machine.
     */
    private function configureRateLimits(): void
    {
        RateLimiter::for('otp-send', fn (Request $request): array => [
            Limit::perHour(5)->by('otp-send:phone:'.$this->stringInput($request, 'phone')),
            Limit::perHour(600)->by('otp-send:ip:'.$request->ip()),
        ]);
        RateLimiter::for('otp-verify', fn (Request $request): array => [
            Limit::perHour(20)->by('otp-verify:phone:'.$this->stringInput($request, 'phone')),
            Limit::perMinute(300)->by('otp-verify:ip:'.$request->ip()),
        ]);
        RateLimiter::for('sign-in', fn (Request $request): array => [
            Limit::perMinute(10)->by('sign-in:'.mb_strtolower($this->stringInput($request, 'email')).'|'.$request->ip()),
            Limit::perHour(50)->by('sign-in:email:'.mb_strtolower($this->stringInput($request, 'email'))),
            Limit::perMinute(300)->by('sign-in:ip:'.$request->ip()),
        ]);
    }

    /**
     * A request field as a string; anything else (an array sent by a misbehaving client) counts as empty, so the
     * limiter never crashes before validation can answer 422.
     */
    private function stringInput(Request $request, string $key): string
    {
        $value = $request->input($key);

        return is_string($value) ? $value : '';
    }
}
