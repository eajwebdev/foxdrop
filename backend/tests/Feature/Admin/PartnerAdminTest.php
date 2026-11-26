<?php

use App\Actions\Partners\ReviewStore;
use App\Enums\AdminPermission;
use App\Models\Rider;
use App\Models\Store;
use Database\Seeders\DemoSeeder;
use Illuminate\Auth\Access\AuthorizationException;
use Illuminate\Support\Facades\DB;
use Inertia\Testing\AssertableInertia as Assert;

/**
 * Stores and riders across provinces with the admin override (§9.5 step 4, DECISIONS 79).
 */
function publishedStoreIds(): array
{
    return Store::query()->published()->pluck('id')->all();
}

it('lists store applications waiting for review by default, with filters in the URL', function () {
    $admin = adminUser(permissions: [AdminPermission::PartnersView]);

    $this->actingAs($admin, 'web')->get('/admin/stores')
        ->assertInertia(fn (Assert $page) => $page
            ->component('stores/index')
            ->where('filters.status', 'SUBMITTED')
            ->loadDeferredProps(fn (Assert $reload) => $reload
                ->has('stores.data', 1)
                ->where('stores.data.0.name', "Lola Nena's Bakery")));

    $this->get('/admin/stores?status=&q=mart')
        ->assertInertia(fn (Assert $page) => $page
            ->where('filters.status', null)
            ->loadDeferredProps(fn (Assert $reload) => $reload->where('stores.data.0.name', 'QuickMart')));
});

it('approves a store for an operator (override) and it goes live', function () {
    $admin = adminUser(permissions: [AdminPermission::PartnersView, AdminPermission::PartnersManage]);
    $storeId = DemoSeeder::storeId(11);

    $this->actingAs($admin, 'web')->get("/admin/stores/{$storeId}")
        ->assertInertia(fn (Assert $page) => $page->component('stores/show')->has('store.documents', 2));
    $this->get('/admin/documents/stores/'.DB::table('store_documents')->where('store_id', $storeId)->value('id'))
        ->assertOk()
        ->assertHeader('Cache-Control', 'no-store, private');
    $this->post("/admin/stores/{$storeId}/approve")->assertRedirect();

    expect(Store::query()->findOrFail($storeId)->status)->toBe('ACTIVE')
        ->and(publishedStoreIds())->toContain($storeId)
        ->and(DB::table('audit_logs')->where('action', 'store.approved')->where('entity_id', $storeId)->value('actor_role'))->toBe('admin_staff');
});

it('suspends, reinstates and closes a live store with a reason the owner sees', function () {
    $admin = adminUser(permissions: [AdminPermission::PartnersView, AdminPermission::PartnersManage]);
    $storeId = DemoSeeder::storeId(1);
    $this->actingAs($admin, 'web')->withSession(confirmedSession());

    // The page offers exactly the changes the model allows from ACTIVE.
    $this->get("/admin/stores/{$storeId}")->assertInertia(fn (Assert $page) => $page->where('store.status_changes', ['SUSPENDED', 'CLOSED']));

    $this->post("/admin/stores/{$storeId}/status", ['status' => 'SUSPENDED'])->assertSessionHasErrors('reason');
    $this->post("/admin/stores/{$storeId}/status", ['status' => 'SUSPENDED', 'reason' => 'Food safety complaint'])->assertSessionHasNoErrors();
    expect(Store::query()->findOrFail($storeId)->status_reason)->toBe('Food safety complaint')
        ->and(publishedStoreIds())->not->toContain($storeId);

    $this->post("/admin/stores/{$storeId}/status", ['status' => 'ACTIVE'])->assertSessionHasNoErrors();
    expect(publishedStoreIds())->toContain($storeId);

    $this->post("/admin/stores/{$storeId}/status", ['status' => 'CLOSED', 'reason' => 'Owner closed the business'])->assertSessionHasNoErrors();
    $this->post("/admin/stores/{$storeId}/status", ['status' => 'ACTIVE'])->assertRedirect(); // a closed store stays closed
    expect(Store::query()->findOrFail($storeId)->status)->toBe('CLOSED')
        ->and(DB::table('audit_logs')->where('entity_id', $storeId)->whereIn('action', ['store.suspended', 'store.reinstated', 'store.closed'])->count())->toBe(3);
});

it('approves a waiting rider and suspends a working one, who goes offline at once', function () {
    $admin = adminUser(permissions: [AdminPermission::PartnersView, AdminPermission::PartnersManage]);
    $waiting = Rider::query()->where('status', 'DOCS_REVIEW')->sole();
    $working = Rider::query()->where('status', 'APPROVED')->firstOrFail();
    DB::table('riders')->where('id', $working->id)->update(['availability' => 'ONLINE']);

    $this->actingAs($admin, 'web')->get("/admin/riders/{$waiting->id}")
        ->assertInertia(fn (Assert $page) => $page->component('riders/show')->has('rider.documents', 5));
    $this->post("/admin/riders/{$waiting->id}/approve")->assertRedirect();
    expect($waiting->refresh()->status)->toBe('APPROVED');

    $this->withSession(confirmedSession())
        ->post("/admin/riders/{$working->id}/status", ['status' => 'SUSPENDED', 'reason' => 'Cash not remitted for 3 days'])
        ->assertSessionHasNoErrors();
    expect($working->refresh()->status)->toBe('SUSPENDED')
        ->and(DB::table('riders')->where('id', $working->id)->value('availability'))->toBe('OFFLINE')
        ->and(DB::table('audit_logs')->where('action', 'rider.suspended')->where('entity_id', $working->id)->exists())->toBeTrue();
});

it('keeps admin accounts out of the app API, where there is no 2FA (DECISIONS 82)', function () {
    $admin = superAdmin();

    $this->postJson('/api/v1/auth/email/login', ['email' => $admin->email, 'password' => ADMIN_PASSWORD, 'device_name' => 'Pixel'])
        ->assertForbidden()
        ->assertJsonPath('message', 'Admin accounts sign in on the FoxDrop Admin website, not in the apps.');
    $this->postJson('/api/v1/operator/stores/'.DemoSeeder::storeId(11).'/approve', [], bearer($admin))->assertForbidden();
    expect(Store::query()->findOrFail(DemoSeeder::storeId(11))->status)->toBe('SUBMITTED');
});

it('stops admin staff without the partners permission in the shared Action too', function () {
    $viewer = adminUser(permissions: [AdminPermission::PartnersView]);

    expect(fn () => app(ReviewStore::class)($viewer, Store::query()->findOrFail(DemoSeeder::storeId(11)), true))
        ->toThrow(AuthorizationException::class);
});
