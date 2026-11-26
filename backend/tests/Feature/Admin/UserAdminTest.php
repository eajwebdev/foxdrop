<?php

use App\Actions\Users\ManageAdminStaff;
use App\Enums\AdminPermission;
use App\Enums\AppRole;
use App\Models\User;
use Illuminate\Support\Facades\DB;
use Inertia\Testing\AssertableInertia as Assert;
use Symfony\Component\HttpKernel\Exception\HttpException;

/**
 * Accounts and admin staff (§9.5 steps 1 and 3, DECISIONS 72, 79).
 */
it('finds accounts by name, any phone spelling or email', function () {
    $admin = adminUser(permissions: [AdminPermission::UsersView]);
    $this->actingAs($admin, 'web');

    foreach (['Juan Dela', '0917 000 0001', '+639170000001', '9170000001'] as $term) {
        $this->get('/admin/users?q='.urlencode($term))
            ->assertInertia(fn (Assert $page) => $page
                ->loadDeferredProps(fn (Assert $reload) => $reload->where('users.data.0.full_name', 'Juan Dela Cruz')));
    }
    $this->get('/admin/users?role=rider')
        ->assertInertia(fn (Assert $page) => $page->loadDeferredProps(fn (Assert $reload) => $reload->where('users.meta.total', 3))); // the 3 approved demo riders (Jomar is still waiting)
});

it('suspends an account and ends every app session at once', function () {
    $admin = adminUser(permissions: [AdminPermission::UsersView, AdminPermission::UsersManage]);
    $customer = demoUser(1);
    $headers = bearer($customer);

    $this->actingAs($admin, 'web')->withSession(confirmedSession())
        ->post("/admin/users/{$customer->id}/suspend", ['reason' => 'Fake orders reported by stores'])
        ->assertSessionHasNoErrors();

    expect($customer->refresh()->status)->toBe('SUSPENDED')
        ->and($customer->tokens()->count())->toBe(0)
        ->and(DB::table('audit_logs')->where('action', 'user.suspended')->where('entity_id', $customer->id)->exists())->toBeTrue();
    app('auth')->forgetGuards();
    $this->getJson('/api/v1/me', $headers)->assertUnauthorized();

    $this->actingAs($admin, 'web')->post("/admin/users/{$customer->id}/reactivate")->assertSessionHasNoErrors();
    expect($customer->refresh()->status)->toBe('ACTIVE');
});

it('lets nobody suspend themselves, and only the super admin suspend an admin', function () {
    $staff = adminUser(permissions: [AdminPermission::UsersManage]);
    $otherAdmin = adminUser(permissions: []);
    $this->actingAs($staff, 'web')->withSession(confirmedSession());

    $this->post("/admin/users/{$staff->id}/suspend", ['reason' => 'Testing myself'])->assertRedirect();
    $this->post("/admin/users/{$otherAdmin->id}/suspend", ['reason' => 'Not allowed for staff'])->assertRedirect();
    expect($staff->refresh()->status)->toBe('ACTIVE')->and($otherAdmin->refresh()->status)->toBe('ACTIVE');

    $this->flushSession(); // a new browser: AuthenticateSession ties a session to one account's password
    $this->actingAs(superAdmin(), 'web')->withSession(confirmedSession())
        ->post("/admin/users/{$otherAdmin->id}/suspend", ['reason' => 'Left the company'])->assertSessionHasNoErrors();
    expect($otherAdmin->refresh()->status)->toBe('SUSPENDED');
});

it('lets the super admin create staff, change their permissions, reset them and remove them', function () {
    $super = superAdmin();
    $this->actingAs($super, 'web')->withSession(confirmedSession());

    $this->get('/admin/settings/staff')->assertInertia(fn (Assert $page) => $page->component('settings/staff')->has('permissions', 7));
    $this->post('/admin/settings/staff', [
        'full_name' => 'Carla Reyes', 'email' => 'Carla@FoxDrop.ph', 'password' => 'Strong-pass-2026', 'password_confirmation' => 'Strong-pass-2026',
        'permissions' => ['partners.view', 'partners.manage'],
    ])->assertSessionHasNoErrors();

    $staff = User::query()->where('email', 'carla@foxdrop.ph')->sole();
    expect($staff->roles->pluck('role')->all())->toContain(AppRole::AdminStaff)
        ->and($staff->adminPermissions())->toBe(['partners.manage', 'partners.view'])
        ->and($staff->two_factor_confirmed_at)->toBeNull();

    $this->put("/admin/settings/staff/{$staff->id}", ['permissions' => ['audit.view']])->assertSessionHasNoErrors();
    expect($staff->fresh()?->adminPermissions())->toBe(['audit.view']);
    $this->put("/admin/settings/staff/{$staff->id}", [])->assertSessionHasNoErrors();
    expect($staff->fresh()?->adminPermissions())->toBe([]);

    $this->put("/admin/settings/staff/{$staff->id}/password", ['password' => 'weak', 'password_confirmation' => 'weak'])->assertSessionHasErrors('password');
    $this->put("/admin/settings/staff/{$staff->id}/password", ['password' => 'New-strong-2026', 'password_confirmation' => 'New-strong-2026'])->assertSessionHasNoErrors();

    $this->delete("/admin/settings/staff/{$staff->id}")->assertSessionHasNoErrors();
    expect($staff->fresh()?->isPlatformAdmin())->toBeFalse()
        ->and(DB::table('audit_logs')->where('entity_id', $staff->id)->where('action', 'like', 'admin_staff.%')->count())->toBe(5);
});

it('keeps admin staff management to the super admin, also through the Action', function () {
    $staff = adminUser(permissions: [AdminPermission::UsersManage]);

    $this->actingAs($staff, 'web')->get('/admin/settings/staff')->assertForbidden();
    expect(fn () => app(ManageAdminStaff::class)->create($staff, 'X', 'x@example.com', 'Strong-pass-2026', []))
        ->toThrow(HttpException::class);
});

it('changes my own password and signs my other devices out', function () {
    $admin = superAdmin();
    DB::table('sessions')->insert(['id' => 'other-device', 'user_id' => $admin->id, 'payload' => '', 'last_activity' => now()->unix()]);

    $this->actingAs($admin, 'web')->withSession(confirmedSession())->get('/admin/settings/security')
        ->assertInertia(fn (Assert $page) => $page->component('settings/security')->has('sessions'));
    $this->put('/admin/settings/password', ['current_password' => 'wrong', 'password' => 'Brand-new-2026', 'password_confirmation' => 'Brand-new-2026'])
        ->assertSessionHasErrors('current_password');
    $this->put('/admin/settings/password', ['current_password' => ADMIN_PASSWORD, 'password' => 'Brand-new-2026', 'password_confirmation' => 'Brand-new-2026'])
        ->assertSessionHasNoErrors();

    expect(DB::table('sessions')->where('id', 'other-device')->exists())->toBeFalse()
        ->and(DB::table('audit_logs')->where('action', 'admin.password_changed')->where('entity_id', $admin->id)->exists())->toBeTrue();
});

it('shows the audit log with filters', function () {
    $admin = adminUser(permissions: [AdminPermission::AuditView]);
    DB::table('audit_logs')->insert(['actor_user_id' => $admin->id, 'action' => 'license.suspended', 'entity_type' => 'province_license', 'entity_id' => 'x', 'after' => json_encode(['reason' => 'Test'])]);

    $this->actingAs($admin, 'web')->get('/admin/audit?action=license')
        ->assertInertia(fn (Assert $page) => $page
            ->component('audit/index')
            ->loadDeferredProps(fn (Assert $reload) => $reload
                ->where('entries.data.0.action', 'license.suspended')
                ->where('entries.data.0.after.reason', 'Test')));
});
