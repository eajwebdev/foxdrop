<?php

namespace App\Providers;

use App\Contracts\DistanceProvider;
use App\Contracts\GoogleIdTokenVerifier;
use App\Contracts\PaymentProvider;
use App\Contracts\SmsSender;
use App\Enums\AdminPermission;
use App\Models\OperatorApplication;
use App\Models\ProductCategory;
use App\Models\ProvinceLicense;
use App\Models\User;
use App\Services\Auth\JwksGoogleIdTokenVerifier;
use App\Services\Distance\GoogleRoutesDistance;
use App\Services\Distance\StraightLineDistance;
use App\Services\Payments\FakeCheckoutProvider;
use App\Services\Payments\PayMongoCheckoutProvider;
use App\Services\Sms\LogSmsSender;
use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Gate;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Support\Facades\Route;
use Illuminate\Support\ServiceProvider;
use Illuminate\Validation\Rules\Password;
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
        $this->app->bind(PaymentProvider::class, function (): PaymentProvider {
            return match (config('foxdrop.payments.driver')) {
                'fake' => new FakeCheckoutProvider,
                'paymongo' => new PayMongoCheckoutProvider,
                default => throw new RuntimeException('Unknown PAYMENT_DRIVER: '.config('foxdrop.payments.driver')),
            };
        });
        // DECISIONS 85: Google driving distance when a key is set, the straight line × road factor otherwise.
        $this->app->bind(DistanceProvider::class, fn (): DistanceProvider => filled(config('services.google_routes.key'))
            ? $this->app->make(GoogleRoutesDistance::class)
            : new StraightLineDistance);
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
        $this->configureAdminGates();
        $this->configureRouteParameters();
        // Admin passwords (§20.6): long, mixed case with a number. Breach checks would send hashes to a third party.
        Password::defaults(fn (): Password => Password::min(12)->letters()->mixedCase()->numbers());
    }

    /**
     * Admin web permissions (DECISIONS 72): every AdminPermission is a Gate ability (`can:partners.view`), and
     * admin staff management is the super admin's alone.
     */
    private function configureAdminGates(): void
    {
        Gate::before(function (User $user, string $ability): ?bool {
            $permission = AdminPermission::tryFrom($ability);

            return $permission === null ? null : $user->hasAdminPermission($permission);
        });
        Gate::define('admin.staff', fn (User $user): bool => $user->isSuperAdmin());
    }

    /**
     * Ids are UUIDs: anything else is a 404 before it reaches PostgreSQL. Admin routes name their models here.
     */
    private function configureRouteParameters(): void
    {
        $uuid = '[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}';
        foreach (['province', 'application', 'license', 'store', 'rider', 'user', 'staff', 'document', 'payment', 'zone', 'order', 'address', 'category', 'product'] as $parameter) {
            Route::pattern($parameter, $uuid);
        }
        Route::model('application', OperatorApplication::class);
        Route::model('license', ProvinceLicense::class);
        Route::model('staff', User::class);
        Route::model('category', ProductCategory::class);
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
        if (config('foxdrop.payments.driver') === 'fake') {
            throw new RuntimeException('PAYMENT_DRIVER=fake takes no real money; configure PayMongo for production.');
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
        // Photo uploads (applications): a handful per hour is plenty for a real applicant.
        RateLimiter::for('uploads', fn (Request $request): Limit => Limit::perHour(20)->by('uploads:'.($request->user()?->getAuthIdentifier() ?? $request->ip())));
        // Browsing is public: generous per IP (carrier NAT), it only stops scraping floods.
        RateLimiter::for('browse', fn (Request $request): Limit => Limit::perMinute(240)->by('browse:'.$request->ip()));
        // A person checks prices and places orders a few times a minute at most.
        RateLimiter::for('quotes', fn (Request $request): Limit => Limit::perMinute(30)->by('quotes:'.($request->user()?->getAuthIdentifier() ?? $request->ip())));
        // Pick-up codes also lock per order after 5 wrong tries (DECISIONS 87); this stops trying many orders.
        RateLimiter::for('pickup-codes', fn (Request $request): Limit => Limit::perMinute(20)->by('pickup-codes:'.($request->user()?->getAuthIdentifier() ?? $request->ip())));
        // Google proves identity itself; this only stops floods from one machine.
        RateLimiter::for('google-sign-in', fn (Request $request): Limit => Limit::perMinute(300)->by('google-sign-in:'.$request->ip()));
    }
}
