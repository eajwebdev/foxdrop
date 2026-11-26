<?php

namespace App\Providers;

use App\Contracts\GoogleIdTokenVerifier;
use App\Contracts\SmsSender;
use App\Services\Auth\JwksGoogleIdTokenVerifier;
use App\Services\Sms\LogSmsSender;
use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Http\JsonResponse;
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
        RateLimiter::for('otp-send', function (Request $request): array {
            $phone = $request->string('phone', '', strict: true)->toString();
            $wait = (int) config('foxdrop.auth.otp.resend_after_seconds');

            return [
                // One code per phone per minute; the app shows the same countdown.
                Limit::perSecond(1, $wait)->by('otp-send:wait:'.$phone)->response(
                    fn (Request $request, array $headers): JsonResponse => response()->json([
                        'message' => "Please wait {$headers['Retry-After']} seconds before asking for a new code.",
                        'retry_after_seconds' => (int) $headers['Retry-After'],
                    ], 429, $headers),
                ),
                Limit::perHour(5)->by('otp-send:phone:'.$phone),
                Limit::perHour(600)->by('otp-send:ip:'.$request->ip()),
            ];
        });
        RateLimiter::for('otp-verify', fn (Request $request): array => [
            Limit::perHour(20)->by('otp-verify:phone:'.$request->string('phone', '', strict: true)),
            Limit::perMinute(300)->by('otp-verify:ip:'.$request->ip()),
        ]);
        RateLimiter::for('email-sign-in', function (Request $request): array {
            $email = $request->string('email', '', strict: true)->trim()->lower()->toString();

            return [
                Limit::perMinute(10)->by('email-sign-in:'.$email.'|'.$request->ip()),
                Limit::perHour(50)->by('email-sign-in:'.$email),
                Limit::perMinute(300)->by('email-sign-in:ip:'.$request->ip()),
            ];
        });
        // Google proves identity itself; this only stops floods from one machine.
        RateLimiter::for('google-sign-in', fn (Request $request): Limit => Limit::perMinute(300)->by('google-sign-in:'.$request->ip()));
    }
}
