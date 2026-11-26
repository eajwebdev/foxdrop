<?php

use App\Models\Store;
use App\Models\User;
use Database\Seeders\DemoSeeder;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;
use Illuminate\Testing\TestResponse;

beforeEach(fn () => Storage::fake('kyc'));

/**
 * @param  array<string, mixed>  $overrides
 */
function applyForStore(User $owner, array $overrides = []): TestResponse
{
    return test()->withHeaders(bearer($owner) + ['Accept' => 'application/json'])->post(route('api.v1.stores.store'), $overrides + [
        'province_id' => provinceId(DemoSeeder::NEGROS_OCCIDENTAL),
        'name' => 'Kusina ni Lola',
        'category' => 'FOOD',
        'description' => 'Home-cooked Ilonggo dishes.',
        'address_line' => '18th St.',
        'barangay' => 'Barangay 5',
        'city' => 'Bacolod City',
        'phone' => '+639175550000',
        'latitude' => 10.6810,
        'longitude' => 122.9560,
        'business_permit' => photo('permit.jpg'),
        'valid_id' => photo('id.jpg'),
    ]);
}

function submittedStore(User $owner): Store
{
    applyForStore($owner)->assertCreated();

    return Store::query()->where('owner_user_id', $owner->id)->sole();
}

it('submits a store application for review', function () {
    $owner = User::factory()->create();

    applyForStore($owner)
        ->assertCreated()
        ->assertJsonPath('data.status', 'SUBMITTED')
        ->assertJsonPath('data.province.name', 'Negros Occidental')
        ->assertJsonPath('data.rejection_reason', null);

    $store = Store::query()->where('owner_user_id', $owner->id)->sole();
    expect(DB::table('store_documents')->where('store_id', $store->id)->pluck('doc_type')->sort()->values()->all())->toBe(['BUSINESS_PERMIT', 'VALID_ID'])
        ->and(Store::query()->published()->whereKey($store->id)->exists())->toBeFalse();
    $this->withHeaders(bearer($owner))->getJson(route('api.v1.stores.mine'))->assertJsonPath('data.0.name', 'Kusina ni Lola');
});

it('accepts stores only where FoxDrop is open', function () {
    applyForStore(User::factory()->create(), ['province_id' => provinceId(ILOILO)])
        ->assertConflict()->assertJsonPath('message', 'FoxDrop is not open in this province yet.');
});

it('allows one store under review at a time', function () {
    $owner = User::factory()->create();
    applyForStore($owner)->assertCreated();

    applyForStore($owner)->assertConflict()->assertJsonPath('message', 'You already have a store under review.');
});

it('validates the store application', function (array $overrides, string $field) {
    applyForStore(User::factory()->create(), $overrides)->assertUnprocessable()->assertJsonValidationErrors($field);
})->with([
    'unknown category' => [['category' => 'PETS'], 'category'],
    'no business permit' => [['business_permit' => null], 'business_permit'],
    'location outside the Philippines' => [['latitude' => 1.3, 'longitude' => 103.8], 'latitude'],
]);

it('lists stores waiting in the operator\'s province only', function () {
    submittedStore(User::factory()->create());

    $names = collect($this->withHeaders(bearer(demoUser(6)))->getJson(route('api.v1.operator.stores.index'))->assertOk()->json('data'))->pluck('name');
    expect($names->all())->toBe(["Lola Nena's Bakery", 'Kusina ni Lola']);

    $this->withHeaders(bearer(operatorOf(ILOILO)))->getJson(route('api.v1.operator.stores.index'))->assertOk()->assertJsonCount(0, 'data');
    $this->withHeaders(bearer(demoUser(1)))->getJson(route('api.v1.operator.stores.index'))->assertForbidden();
});

it('shows the reviewer the application with signed document links', function () {
    $store = submittedStore(User::factory()->create());

    $review = $this->withHeaders(bearer(demoUser(6)))->getJson(route('api.v1.operator.stores.show', $store->id))
        ->assertOk()
        ->assertJsonPath('data.location', ['lat' => 10.681, 'lng' => 122.956])
        ->assertJsonPath('data.owner.full_name', $store->owner?->full_name)
        ->assertJsonCount(2, 'data.documents');

    $url = (string) $review->json('data.documents.0.url');
    app('auth')->forgetGuards();
    $this->get($url)->assertOk()->assertHeader('Content-Type', 'image/jpeg');
    $this->get($url.'x')->assertForbidden();
    $this->travel(11)->minutes();
    $this->get($url)->assertForbidden();
});

it('hides another province\'s store from a reviewer', function () {
    $store = submittedStore(User::factory()->create());

    $this->withHeaders(bearer(operatorOf(ILOILO)))->getJson(route('api.v1.operator.stores.show', $store->id))->assertNotFound();
    $this->withHeaders(bearer(operatorOf(ILOILO)))->postJson(route('api.v1.operator.stores.approve', $store->id))->assertNotFound();
});

it('approves a store: it goes live for customers and its owner gets the store role', function () {
    $owner = User::factory()->create();
    $store = submittedStore($owner);

    $this->withHeaders(bearer(demoUser(6)))->postJson(route('api.v1.operator.stores.approve', $store->id))
        ->assertOk()->assertJsonPath('data.status', 'ACTIVE');

    expect(Store::query()->published()->whereKey($store->id)->exists())->toBeTrue()
        ->and(Store::query()->visibleTo(demoUser(2))->whereKey($store->id)->exists())->toBeTrue()
        ->and(DB::table('wallets')->where('owner_id', $store->id)->pluck('kind')->sort()->values()->all())->toBe(['STORE_PAYABLE', 'STORE_TOPUP'])
        ->and(DB::table('audit_logs')->where('action', 'store.approved')->where('entity_id', $store->id)->exists())->toBeTrue();
    $this->withHeaders(bearer($owner))->getJson(route('api.v1.me'))
        ->assertJsonFragment(['role' => 'store_owner', 'province_id' => $store->province_id, 'store_id' => $store->id]);
});

it('rejects a store with a reason: it stays hidden and can be resubmitted', function () {
    $owner = User::factory()->create();
    $store = submittedStore($owner);

    $this->withHeaders(bearer(demoUser(6)))->postJson(route('api.v1.operator.stores.reject', $store->id), ['reason' => 'Bad'])
        ->assertUnprocessable()->assertJsonValidationErrors('reason');
    $this->withHeaders(bearer(demoUser(6)))->postJson(route('api.v1.operator.stores.reject', $store->id), ['reason' => 'The business permit has expired.'])
        ->assertOk()->assertJsonPath('data.status', 'REJECTED');

    expect(Store::query()->published()->whereKey($store->id)->exists())->toBeFalse();
    $this->withHeaders(bearer($owner))->getJson(route('api.v1.stores.mine'))
        ->assertJsonPath('data.0.status', 'REJECTED')
        ->assertJsonPath('data.0.rejection_reason', 'The business permit has expired.');

    applyForStore($owner, ['name' => 'Kusina ni Lola (renewed permit)'])->assertCreated()->assertJsonPath('data.status', 'SUBMITTED')->assertJsonPath('data.id', $store->id);
    expect(Store::query()->where('owner_user_id', $owner->id)->count())->toBe(1);
});

it('reviews a store only while it waits', function () {
    $store = submittedStore(User::factory()->create());
    $this->withHeaders(bearer(demoUser(6)))->postJson(route('api.v1.operator.stores.approve', $store->id))->assertOk();

    $this->withHeaders(bearer(demoUser(6)))->postJson(route('api.v1.operator.stores.reject', $store->id), ['reason' => 'Changed my mind.'])
        ->assertConflict()->assertJsonPath('message', 'This store is not waiting for review.');
});

it('lets a platform admin approve a store in any province', function () {
    operatorOf(ILOILO);
    $owner = User::factory()->create();
    applyForStore($owner, ['province_id' => provinceId(ILOILO), 'latitude' => 10.72, 'longitude' => 122.56])->assertCreated();
    $store = Store::query()->where('owner_user_id', $owner->id)->sole();

    $this->withHeaders(bearer(demoUser(10)))->postJson(route('api.v1.operator.stores.approve', $store->id))
        ->assertOk()->assertJsonPath('data.status', 'ACTIVE');
    expect(DB::table('audit_logs')->where('action', 'store.approved')->where('actor_role', 'super_admin')->exists())->toBeTrue();
});

it('keeps an operator whose license expired read-only', function () {
    $store = submittedStore(User::factory()->create());
    DB::table('province_licenses')->where('operator_user_id', demoUser(6)->id)->update(['status' => 'EXPIRED']);

    $this->withHeaders(bearer(demoUser(6)))->postJson(route('api.v1.operator.stores.approve', $store))->assertForbidden();

    expect($store->refresh()->status)->toBe(Store::SUBMITTED);
});

it('deletes the replaced document photos when a store is resubmitted', function () {
    $owner = User::factory()->create();
    $store = submittedStore($owner);
    $oldPaths = DB::table('store_documents')->where('store_id', $store->id)->pluck('storage_path')->all();
    $this->withHeaders(bearer(demoUser(6)))->postJson(route('api.v1.operator.stores.reject', $store), ['reason' => 'Permit is expired.'])->assertOk();

    applyForStore($owner)->assertCreated();

    foreach ($oldPaths as $path) {
        Storage::disk('kyc')->assertMissing($path);
    }
    expect(DB::table('store_documents')->where('store_id', $store->id)->count())->toBe(2);
});

it('refuses photos too large to decode safely, before saving anything', function () {
    $owner = User::factory()->create();

    applyForStore($owner, ['valid_id' => UploadedFile::fake()->image('huge.jpg', 6001, 8)])
        ->assertUnprocessable()
        ->assertJsonValidationErrors(['valid_id' => 'This photo is too large. Please use one under 6000 pixels wide and high.']);

    expect(Store::query()->where('owner_user_id', $owner->id)->exists())->toBeFalse()
        ->and(Storage::disk('kyc')->allFiles())->toBe([]);
});

it('stops a former operator from reviewing once the province is licensed to someone else', function () {
    $store = submittedStore(User::factory()->create());
    $former = demoUser(6); // keeps the operator role row, but the province's current license is now another operator's
    $current = operatorOf(DemoSeeder::NEGROS_OCCIDENTAL);

    $this->withHeaders(bearer($former))->getJson(route('api.v1.operator.stores.index', ['status' => 'SUBMITTED']))->assertForbidden();
    $this->withHeaders(bearer($former))->getJson(route('api.v1.operator.stores.show', $store))->assertForbidden();
    $this->withHeaders(bearer($former))->getJson(route('api.v1.operator.riders.index'))->assertForbidden();
    $this->withHeaders(bearer($former))->postJson(route('api.v1.operator.stores.approve', $store))->assertForbidden();

    $this->withHeaders(bearer($current))->getJson(route('api.v1.operator.stores.index', ['status' => 'SUBMITTED']))
        ->assertOk()->assertJsonFragment(['id' => $store->id]);
});
