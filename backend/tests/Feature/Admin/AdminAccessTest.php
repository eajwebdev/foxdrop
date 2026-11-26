<?php

use App\Enums\AdminPermission;
use App\Enums\AppRole;
use App\Models\ProvinceLicense;
use App\Models\Rider;
use App\Models\RiderDocument;
use App\Models\Store;
use App\Models\StoreDocument;
use App\Models\User;
use Database\Seeders\DemoSeeder;
use Illuminate\Routing\Route as RoutingRoute;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Route;
use Inertia\Testing\AssertableInertia as Assert;
use PragmaRX\Google2FA\Google2FA;

/**
 * Who may open the admin web (§20.4, §20.6, DECISIONS 72–75) and the W1 acceptance checks on access.
 */

/**
 * Every admin page and action, with real ids from the demo data in place of the route parameters.
 *
 * @return list<array{method: string, uri: string, name: string, middleware: list<string>}>
 */
function adminRoutes(): array
{
    $ids = [
        'province' => provinceId(DemoSeeder::NEGROS_OCCIDENTAL),
        'application' => DemoSeeder::CEBU_APPLICATION,
        'license' => ProvinceLicense::query()->value('id'),
        'store' => DemoSeeder::storeId(11),
        'rider' => Rider::query()->value('id'),
        'user' => DemoSeeder::userId(1),
        'staff' => DemoSeeder::userId(14),
        'document' => null, // filled per route below
        'type' => 'valid_id',
    ];

    $routes = [];
    foreach (Route::getRoutes()->getRoutes() as $route) {
        /** @var RoutingRoute $route */
        $name = (string) $route->getName();
        if (! str_starts_with($name, 'admin.') || $name === 'admin.two-factor.setup') {
            continue;
        }
        $params = $ids;
        $params['document'] = $name === 'admin.documents.rider' ? RiderDocument::query()->value('id') : StoreDocument::query()->value('id');
        $uri = preg_replace_callback('/\{(\w+)\}/', fn (array $m): string => (string) $params[$m[1]], $route->uri());
        $routes[] = [
            'method' => $route->methods()[0],
            'uri' => '/'.$uri,
            'name' => $name,
            'middleware' => array_values(array_map('strval', $route->gatherMiddleware())),
        ];
    }

    return $routes;
}

it('sends guests to the sign-in page', function () {
    $this->get('/admin')->assertRedirect('/admin/login');
    $this->get('/admin/franchises')->assertRedirect('/admin/login');
    $this->get('/admin/login')->assertOk()->assertInertia(fn (Assert $page) => $page->component('auth/login'));
});

it('lets only active admin accounts sign in', function () {
    $customer = User::factory()->withEmail(ADMIN_PASSWORD)->create();
    $suspendedAdmin = User::factory()->withEmail(ADMIN_PASSWORD)->withRole(AppRole::SuperAdmin)->suspended()->create();

    foreach ([$customer, $suspendedAdmin] as $user) {
        $this->post('/admin/login', ['email' => $user->email, 'password' => ADMIN_PASSWORD])->assertSessionHasErrors('email');
        $this->assertGuest('web');
    }

    $admin = adminUser(superAdmin: true, twoFactor: false);
    $this->post('/admin/login', ['email' => strtoupper((string) $admin->email), 'password' => ADMIN_PASSWORD])->assertRedirect('/admin');
    $this->assertAuthenticatedAs($admin, 'web');
    expect(DB::table('audit_logs')->where('action', 'admin.signed_in')->where('actor_user_id', $admin->id)->exists())->toBeTrue();
});

it('audits failed sign-ins on admin accounts and locks out after 5 tries a minute', function () {
    $admin = adminUser(superAdmin: true);

    foreach (range(1, 5) as $try) {
        $this->post('/admin/login', ['email' => $admin->email, 'password' => 'wrong-password'])->assertSessionHasErrors('email');
    }
    // The 6th try is refused before the password is even checked (the admin web shows it as a toast).
    $this->post('/admin/login', ['email' => $admin->email, 'password' => ADMIN_PASSWORD])->assertRedirect();
    $this->assertGuest('web');
    expect(session('login.id'))->toBeNull();

    expect(DB::table('audit_logs')->where('action', 'admin.sign_in_failed')->where('entity_id', $admin->id)->count())->toBe(5);
});

it('never sets a remember-me cookie, so sessions end after 30 idle minutes', function () {
    $admin = adminUser(superAdmin: true, twoFactor: false);

    $response = $this->post('/admin/login', ['email' => $admin->email, 'password' => ADMIN_PASSWORD, 'remember' => 'on']);

    $response->assertRedirect('/admin');
    expect(collect($response->headers->getCookies())->contains(fn ($cookie) => str_starts_with($cookie->getName(), 'remember_web')))->toBeFalse()
        ->and(config('session.lifetime'))->toBe(30);
});

it('asks for the authenticator code after the password', function () {
    $admin = superAdmin();

    $this->post('/admin/login', ['email' => $admin->email, 'password' => ADMIN_PASSWORD])->assertRedirect('/admin/two-factor-challenge');
    $this->assertGuest('web');

    $this->post('/admin/two-factor-challenge', ['code' => '000000'])->assertSessionHasErrors('code');
    $this->post('/admin/two-factor-challenge', ['code' => totp()])->assertRedirect('/admin');
    $this->assertAuthenticatedAs($admin, 'web');
});

it('keeps an admin without 2FA on the setup page for every admin page (W1 acceptance)', function () {
    $admin = adminUser(superAdmin: true, twoFactor: false);

    foreach (adminRoutes() as $route) {
        $response = $this->actingAs($admin, 'web')->call($route['method'], $route['uri']);
        expect($response->status())->toBe(302, "{$route['method']} {$route['uri']}");
        expect($response->headers->get('Location'))->toEndWith('/admin/two-factor/setup');
    }

    $this->actingAs($admin, 'web')->get('/admin/two-factor/setup')
        ->assertOk()
        ->assertInertia(fn (Assert $page) => $page->component('auth/two-factor-setup')->where('enabled', false));
});

it('sets up 2FA with an authenticator code and shows the recovery codes once', function () {
    $admin = adminUser(superAdmin: true, twoFactor: false);
    $this->actingAs($admin, 'web')->withSession(confirmedSession());

    $this->post('/admin/user/two-factor-authentication')->assertRedirect();
    $secret = decrypt((string) $admin->fresh()?->two_factor_secret);
    $this->getJson('/admin/user/two-factor-qr-code')->assertOk()->assertJsonStructure(['svg', 'url']);

    $this->from('/admin/two-factor/setup')
        ->post('/admin/user/confirmed-two-factor-authentication', ['code' => (new Google2FA)->getCurrentOtp($secret)])
        ->assertRedirect('/admin/two-factor/setup');

    expect($admin->fresh()?->two_factor_confirmed_at)->not->toBeNull()
        ->and(DB::table('audit_logs')->where('action', 'admin.two_factor_enabled')->where('entity_id', $admin->id)->exists())->toBeTrue();
    $this->get('/admin')->assertOk();
});

it('cannot turn 2FA off or swap the secret once it is confirmed', function () {
    $admin = superAdmin();
    $before = $admin->two_factor_secret;

    $this->actingAs($admin, 'web')->withSession(confirmedSession())
        ->post('/admin/user/two-factor-authentication', ['force' => true])->assertRedirect();
    $this->delete('/admin/user/two-factor-authentication')->assertStatus(405);

    expect($admin->fresh()?->two_factor_secret)->toBe($before);
});

it('gives an admin-staff account without permissions 403 on every permissioned admin route (W1 acceptance)', function () {
    $staff = adminUser(permissions: []);
    $checked = 0;

    foreach (adminRoutes() as $route) {
        $permissioned = collect($route['middleware'])->contains(fn (string $m): bool => str_starts_with($m, 'can:'));
        if (! $permissioned) {
            continue;
        }
        $response = $this->actingAs($staff, 'web')->withSession(confirmedSession())
            ->call($route['method'], $route['uri'], [], [], [], ['HTTP_ACCEPT' => 'application/json']);
        expect($response->status())->toBe(403, "{$route['method']} {$route['uri']} ({$route['name']})");
        $checked++;
    }

    expect($checked)->toBeGreaterThan(30);
});

it('opens every admin page for the super admin', function () {
    $admin = superAdmin();

    $pages = collect(adminRoutes())->filter(fn (array $route): bool => $route['method'] === 'GET');
    foreach ($pages as $route) {
        $status = $this->actingAs($admin, 'web')->withSession(confirmedSession())->get($route['uri'])->baseResponse->getStatusCode();
        expect($status)->toBe(200, $route['uri']);
    }
});

it('hides what a staff member may not do and lets them do what they may', function () {
    $staff = adminUser(permissions: [AdminPermission::PartnersView]);

    $this->actingAs($staff, 'web')->get('/admin')
        ->assertOk()
        ->assertInertia(fn (Assert $page) => $page
            ->component('dashboard')
            ->where('auth.user.permissions', ['partners.view'])
            ->where('auth.user.is_super_admin', false));
    $this->get('/admin/stores')->assertOk()->assertInertia(fn (Assert $page) => $page->component('stores/index'));
    $this->get('/admin/franchises')->assertForbidden();
    $this->get('/admin/settings/staff')->assertForbidden();
});

it('signs a suspended or demoted admin out on their next click', function () {
    $admin = adminUser(permissions: [AdminPermission::PartnersView]);
    $this->actingAs($admin, 'web')->get('/admin/stores')->assertOk();

    DB::table('users')->where('id', $admin->id)->update(['status' => 'SUSPENDED']);

    $this->actingAs($admin->fresh() ?? $admin, 'web')->get('/admin/stores')->assertRedirect('/admin/login');
    $this->assertGuest('web');
});

it('asks for password and authenticator code before a dangerous action', function () {
    $admin = superAdmin();
    $store = Store::query()->findOrFail(DemoSeeder::storeId(1));

    $this->actingAs($admin, 'web')->from('/admin/stores/'.$store->id)
        ->post("/admin/stores/{$store->id}/status", ['status' => 'SUSPENDED', 'reason' => 'Health permit expired'])
        ->assertSessionHasErrors('confirm_password');
    $this->post("/admin/stores/{$store->id}/status", [
        'status' => 'SUSPENDED', 'reason' => 'Health permit expired', 'confirm_password' => ADMIN_PASSWORD, 'confirm_code' => '123456',
    ])->assertSessionHasErrors('confirm_password');
    expect($store->refresh()->status)->toBe('ACTIVE');

    $this->post("/admin/stores/{$store->id}/status", [
        'status' => 'SUSPENDED', 'reason' => 'Health permit expired', 'confirm_password' => ADMIN_PASSWORD, 'confirm_code' => totp(),
    ])->assertSessionHasNoErrors()->assertRedirect();

    expect($store->refresh()->status)->toBe('SUSPENDED')
        ->and(session('auth.password_confirmed_at'))->not->toBeNull();
});

it('sends security headers and a nonce-based Content Security Policy', function () {
    $response = $this->get('/admin/login');

    $csp = (string) $response->headers->get('Content-Security-Policy');
    expect($csp)->toContain("script-src 'self' 'nonce-")
        ->and($csp)->toContain("frame-ancestors 'none'")
        ->and($csp)->toContain("object-src 'none'")
        ->and($response->headers->get('X-Frame-Options'))->toBe('DENY')
        ->and($response->headers->get('X-Content-Type-Options'))->toBe('nosniff')
        ->and($response->headers->get('Referrer-Policy'))->toBe('strict-origin-when-cross-origin');
    preg_match("/'nonce-([^']+)'/", $csp, $nonce);
    expect($response->getContent())->toContain('nonce="'.$nonce[1].'"');
});

it('turns a refused change into an error toast instead of an error page', function () {
    $admin = superAdmin();
    $store = Store::query()->findOrFail(DemoSeeder::storeId(1)); // already ACTIVE: not waiting for review

    $this->actingAs($admin, 'web')->from('/admin/stores/'.$store->id)
        ->post("/admin/stores/{$store->id}/approve")
        ->assertRedirect('/admin/stores/'.$store->id);

    $this->get('/admin/stores/00000000-0000-4000-8000-999999999999')
        ->assertNotFound()
        ->assertInertia(fn (Assert $page) => $page->component('error')->where('status', 404)->where('message', null)); // never "No query results for model …"
});
