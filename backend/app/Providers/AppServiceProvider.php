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
     * §17: rate limits on sign-in. Codes are limited per phone and per IP address so one attacker cannot spam a
     * number or guess codes for many numbers.
     */
    private function configureRateLimits(): void
    {
        RateLimiter::for('otp-send', fn (Request $request): array => [
            Limit::perHour(5)->by('otp-send:phone:'.$request->input('phone')),
            Limit::perHour(30)->by('otp-send:ip:'.$request->ip()),
        ]);
        RateLimiter::for('otp-verify', fn (Request $request): array => [
            Limit::perHour(20)->by('otp-verify:phone:'.$request->input('phone')),
            Limit::perMinute(10)->by('otp-verify:ip:'.$request->ip()),
        ]);
        RateLimiter::for('sign-in', fn (Request $request): array => [
            Limit::perMinute(10)->by('sign-in:'.mb_strtolower((string) $request->input('email')).'|'.$request->ip()),
            Limit::perMinute(30)->by('sign-in:ip:'.$request->ip()),
        ]);
    }
}
