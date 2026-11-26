<?php

use App\Models\Rider;
use App\Models\User;
use Database\Seeders\DemoSeeder;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;
use Illuminate\Testing\TestResponse;

beforeEach(fn () => Storage::fake('kyc'));

/**
 * @param  array<string, mixed>  $overrides
 */
function applyAsRider(User $user, array $overrides = []): TestResponse
{
    return test()->withHeaders(bearer($user) + ['Accept' => 'application/json'])->post(route('api.v1.riders.store'), array_filter($overrides + [
        'province_id' => provinceId(DemoSeeder::NEGROS_OCCIDENTAL),
        'full_name' => 'Marco Lopez',
        'vehicle_type' => 'MOTORCYCLE',
        'plate_no' => 'neg 4321',
        'license_no' => 'N01-23-456789',
        'license_expiry' => now()->addYear()->toDateString(),
        'valid_id' => photo('id.jpg'),
        'drivers_license' => photo('license.jpg'),
        'selfie' => photo('selfie.jpg'),
        'or_cr' => photo('orcr.jpg'),
        'vehicle_photo' => photo('bike.jpg'),
    ], fn ($value) => $value !== null));
}

it('registers a rider straight into document review', function () {
    $user = User::factory()->create();

    $response = applyAsRider($user)
        ->assertCreated()
        ->assertJsonPath('data.status', 'DOCS_REVIEW')
        ->assertJsonPath('data.plate_no', 'NEG 4321')
        ->assertJsonPath('data.license_no_last4', '6789')
        ->assertJsonPath('data.province.name', 'Negros Occidental');

    $rider = Rider::query()->where('user_id', $user->id)->sole();
    expect($response->content())->not->toContain('N0123456789')
        ->and(DB::table('riders')->where('id', $rider->id)->value('license_no_encrypted'))->not->toContain('456789')
        ->and($rider->license_no_encrypted)->toBe('N0123456789')
        ->and($user->refresh()->full_name)->toBe('Marco Lopez')
        ->and(DB::table('rider_documents')->where('rider_id', $rider->id)->count())->toBe(5);
    $this->withHeaders(bearer($user))->getJson(route('api.v1.riders.me'))->assertJsonPath('data.id', $rider->id);
});

it('needs no plate or vehicle papers for a bicycle', function () {
    applyAsRider(User::factory()->create(), ['vehicle_type' => 'BICYCLE', 'plate_no' => null, 'or_cr' => null, 'vehicle_photo' => null])
        ->assertCreated()->assertJsonPath('data.plate_no', null);
});

it('validates the rider registration', function (array $overrides, string $field, ?string $message) {
    $response = applyAsRider(User::factory()->create(), $overrides)->assertUnprocessable()->assertJsonValidationErrors($field);
    if ($message !== null) {
        $response->assertJsonValidationErrors([$field => $message]);
    }
})->with([
    'motorcycle without OR/CR' => [['or_cr' => null], 'or_cr', null],
    'expired license' => [['license_expiry' => '2020-01-01'], 'license_expiry', 'This driver\'s license has expired.'],
    'odd plate' => [['plate_no' => '!!'], 'plate_no', 'Enter the plate number as shown, e.g. NEG 1234.'],
    'no selfie' => [['selfie' => null], 'selfie', null],
]);

it('registers a rider once', function () {
    $user = User::factory()->create();
    applyAsRider($user)->assertCreated();

    applyAsRider($user)->assertConflict()->assertJsonPath('message', 'You already applied as a rider.');
});

it('accepts riders only where FoxDrop is open', function () {
    applyAsRider(User::factory()->create(), ['province_id' => provinceId(ILOILO)])->assertConflict();
});

it('shows no rider record before registering', function () {
    $this->withHeaders(bearer(User::factory()->create()))->getJson(route('api.v1.riders.me'))->assertOk()->assertExactJson(['data' => null]);
});

it('lists riders waiting in the operator\'s province with their documents', function () {
    applyAsRider(User::factory()->create());

    $list = collect($this->withHeaders(bearer(demoUser(6)))->getJson(route('api.v1.operator.riders.index'))->assertOk()->json('data'));
    expect($list->pluck('full_name')->all())->toBe(['Jomar Tan', 'Marco Lopez']);

    $this->withHeaders(bearer(demoUser(6)))->getJson(route('api.v1.operator.riders.show', $list->last()['id']))
        ->assertOk()->assertJsonCount(5, 'data.documents')->assertJsonPath('data.license_no_last4', '6789');
    $this->withHeaders(bearer(operatorOf(ILOILO)))->getJson(route('api.v1.operator.riders.index'))->assertJsonCount(0, 'data');
    $this->withHeaders(bearer(operatorOf(ILOILO)))->getJson(route('api.v1.operator.riders.show', $list->last()['id']))->assertNotFound();
});

it('approves a rider: rider role and wallets', function () {
    $user = User::factory()->create();
    applyAsRider($user);
    $rider = Rider::query()->where('user_id', $user->id)->sole();

    $this->withHeaders(bearer(demoUser(6)))->postJson(route('api.v1.operator.riders.approve', $rider->id))
        ->assertOk()->assertJsonPath('data.status', 'APPROVED');

    $this->withHeaders(bearer($user))->getJson(route('api.v1.me'))->assertJsonFragment(['role' => 'rider', 'province_id' => $rider->province_id, 'store_id' => null]);
    expect(DB::table('wallets')->where('owner_id', $rider->id)->pluck('kind')->sort()->values()->all())->toBe(['RIDER_CASH', 'RIDER_EARNINGS'])
        ->and($rider->refresh()->approved_at)->not->toBeNull();
});

it('rejects a rider with a reason and accepts a corrected registration', function () {
    $user = User::factory()->create();
    applyAsRider($user);
    $rider = Rider::query()->where('user_id', $user->id)->sole();

    $this->withHeaders(bearer(demoUser(6)))->postJson(route('api.v1.operator.riders.reject', $rider->id), ['reason' => 'The selfie is blurred.'])
        ->assertOk()->assertJsonPath('data.status', 'REJECTED');
    $this->withHeaders(bearer($user))->getJson(route('api.v1.riders.me'))
        ->assertJsonPath('data.status', 'REJECTED')->assertJsonPath('data.rejection_reason', 'The selfie is blurred.');

    applyAsRider($user)->assertCreated()->assertJsonPath('data.status', 'DOCS_REVIEW')->assertJsonPath('data.rejection_reason', null);
    expect(DB::table('rider_documents')->where('rider_id', $rider->id)->count())->toBe(5);
});

it('lets an admin approve a rider in any province (admin web; admins have no API access)', function () {
    $this->actingAs(superAdmin(), 'web')->post(route('admin.riders.approve', DemoSeeder::riderId(12)))->assertRedirect();

    expect(Rider::query()->findOrFail(DemoSeeder::riderId(12))->status)->toBe('APPROVED');
});
