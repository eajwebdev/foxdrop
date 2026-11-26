<?php

use App\Models\DeliveryZone;
use App\Models\User;
use Database\Seeders\DemoSeeder;
use Illuminate\Support\Facades\DB;

/** The demo operator of Negros Occidental (0006). */
function negrosOperator(): User
{
    return demoUser(6);
}

/**
 * @param  array<string, mixed>  $overrides
 * @return array<string, mixed>
 */
function rateCard(array $overrides = []): array
{
    return $overrides + [
        'base_fare_centavos' => 4000, 'base_km' => 2.0, 'rate_per_km_centavos' => 1000, 'min_fee_centavos' => 4000,
        'max_km' => 15.0, 'rider_share_bps' => 8000,
    ];
}

it('keeps territory screens for operators', function (string $method, string $route) {
    $this->withHeaders(bearer(demoUser(1)))->json($method, route($route))->assertForbidden()
        ->assertJsonPath('message', 'This needs a provincial operator account.');
})->with([
    ['GET', 'api.v1.operator.zones.index'],
    ['GET', 'api.v1.operator.rate-card.show'],
    ['GET', 'api.v1.operator.province.show'],
]);

it('adds a circle zone and lists it with the seeded zone', function () {
    $this->withHeaders(bearer(negrosOperator()))->postJson(route('api.v1.operator.zones.store'), [
        'name' => 'Silay & Talisay', 'center' => ['lat' => 10.7969, 'lng' => 122.9749], 'radius_km' => 5,
    ])
        ->assertCreated()
        ->assertJsonPath('data.name', 'Silay & Talisay')
        ->assertJsonPath('data.radius_km', 5)
        ->assertJsonPath('data.center', ['lat' => 10.7969, 'lng' => 122.9749]);

    $zones = collect($this->withHeaders(bearer(negrosOperator()))->getJson(route('api.v1.operator.zones.index'))->assertOk()->json('data'));
    $circle = $zones->firstWhere('name', 'Silay & Talisay');
    expect($zones->pluck('name')->all())->toBe(['Metro Bacolod', 'Silay & Talisay'])
        ->and($circle['area_km2'])->toBeGreaterThan(78.0)->toBeLessThan(79.0) // π × 5² ≈ 78.5 km²
        ->and($zones->firstWhere('name', 'Metro Bacolod')['center'])->toBeNull()
        ->and(DB::table('audit_logs')->where('action', 'delivery_zone.created')->exists())->toBeTrue();
});

it('adds a polygon zone', function () {
    $this->withHeaders(bearer(negrosOperator()))->postJson(route('api.v1.operator.zones.store'), [
        'name' => 'Downtown', 'polygon' => [[122.94, 10.66], [122.96, 10.66], [122.96, 10.68], [122.94, 10.68]],
    ])->assertCreated()->assertJsonPath('data.center', null)->assertJsonPath('data.radius_km', null);
});

it('validates zones', function (array $body, string $field) {
    $this->withHeaders(bearer(negrosOperator()))->postJson(route('api.v1.operator.zones.store'), $body + ['name' => 'Zone'])
        ->assertUnprocessable()->assertJsonValidationErrors($field);
})->with([
    'too small' => [['center' => ['lat' => 10.7, 'lng' => 122.9], 'radius_km' => 0.2], 'radius_km'],
    'outside the Philippines' => [['center' => ['lat' => 35.6, 'lng' => 139.7], 'radius_km' => 5], 'center.lat'],
    'circle and polygon at once' => [['center' => ['lat' => 10.7, 'lng' => 122.9], 'radius_km' => 5, 'polygon' => [[122.9, 10.6], [123.0, 10.6], [123.0, 10.7]]], 'center'],
    'two-point polygon' => [['polygon' => [[122.9, 10.6], [123.0, 10.6]]], 'polygon'],
]);

it('deactivates a zone of its own province only', function () {
    $zone = DeliveryZone::query()->where('name', 'Metro Bacolod')->sole();

    $this->withHeaders(bearer(operatorOf(ILOILO)))->deleteJson(route('api.v1.operator.zones.destroy', $zone->id))->assertNotFound();
    $this->withHeaders(bearer(negrosOperator()))->deleteJson(route('api.v1.operator.zones.destroy', $zone->id))->assertNoContent();

    expect($zone->refresh()->is_active)->toBeFalse();
    $this->withHeaders(bearer(negrosOperator()))->getJson(route('api.v1.operator.zones.index'))->assertJsonCount(0, 'data');
});

it('shows only the operator\'s own province zones', function () {
    $this->withHeaders(bearer(operatorOf(ILOILO)))->getJson(route('api.v1.operator.zones.index'))->assertOk()->assertJsonCount(0, 'data');
});

it('publishes a new rate card version', function () {
    $this->withHeaders(bearer(negrosOperator()))->getJson(route('api.v1.operator.rate-card.show'))
        ->assertOk()->assertJsonPath('data.rate_per_km_centavos', 1000);

    $this->withHeaders(bearer(negrosOperator()))->postJson(route('api.v1.operator.rate-cards.store'), rateCard(['rate_per_km_centavos' => 1200]))
        ->assertCreated()->assertJsonPath('data.rate_per_km_centavos', 1200);

    $this->travel(1)->second();
    $this->withHeaders(bearer(negrosOperator()))->getJson(route('api.v1.operator.rate-card.show'))
        ->assertJsonPath('data.rate_per_km_centavos', 1200)
        ->assertJsonStructure(['data' => ['id', 'base_fare_centavos', 'base_km', 'rate_per_km_centavos', 'min_fee_centavos', 'max_km', 'rider_share_bps', 'night_surcharge_centavos', 'bad_weather_surcharge_centavos', 'extra_stop_centavos', 'effective_from']]);
    expect(DB::table('delivery_rate_cards')->where('province_id', provinceId(DemoSeeder::NEGROS_OCCIDENTAL))->count())->toBe(2);
});

it('shows the platform default card to a province that never published one', function () {
    $this->withHeaders(bearer(operatorOf(ILOILO)))->getJson(route('api.v1.operator.rate-card.show'))
        ->assertOk()->assertJsonPath('data.id', null)->assertJsonPath('data.base_fare_centavos', 4000);
});

it('validates rate cards', function (array $overrides, string $field) {
    $this->withHeaders(bearer(negrosOperator()))->postJson(route('api.v1.operator.rate-cards.store'), rateCard($overrides))
        ->assertUnprocessable()->assertJsonValidationErrors($field);
})->with([
    'rider share over 100 %' => [['rider_share_bps' => 12000], 'rider_share_bps'],
    'negative fare' => [['base_fare_centavos' => -1], 'base_fare_centavos'],
    'km not in tenths' => [['base_km' => 2.05], 'base_km'],
]);

it('previews fees with the server formula', function () {
    $this->withHeaders(bearer(negrosOperator()))->postJson(route('api.v1.operator.rate-cards.preview'), rateCard(['distances_km' => [1, 3, 5, 10, 16]]))
        ->assertOk()
        ->assertExactJson(['data' => [
            ['distance_km' => 1, 'delivery_fee_centavos' => 4000, 'rider_earning_centavos' => 3200, 'operator_cut_centavos' => 800],
            ['distance_km' => 3, 'delivery_fee_centavos' => 5000, 'rider_earning_centavos' => 4000, 'operator_cut_centavos' => 1000],
            ['distance_km' => 5, 'delivery_fee_centavos' => 7000, 'rider_earning_centavos' => 5600, 'operator_cut_centavos' => 1400],
            ['distance_km' => 10, 'delivery_fee_centavos' => 12000, 'rider_earning_centavos' => 9600, 'operator_cut_centavos' => 2400],
            ['distance_km' => 16, 'delivery_fee_centavos' => null, 'rider_earning_centavos' => null, 'operator_cut_centavos' => null],
        ]]);
});

it('changes the store fee and records it', function () {
    $this->withHeaders(bearer(negrosOperator()))->getJson(route('api.v1.operator.province.show'))
        ->assertOk()->assertJsonPath('data.name', 'Negros Occidental')->assertJsonPath('data.store_fee_centavos', 1500)
        ->assertJsonPath('data.center', ['lat' => 10.6765, 'lng' => 122.9509]);

    $this->withHeaders(bearer(negrosOperator()))->patchJson(route('api.v1.operator.province.update'), ['store_fee_centavos' => 2000])
        ->assertOk()->assertJsonPath('data.store_fee_centavos', 2000);
    expect(DB::table('audit_logs')->where('action', 'province.store_fee_changed')->value('after'))->toContain('2000');
});

it('makes an expired operator read-only', function () {
    $expired = operatorOf(ILOILO, 'EXPIRED');

    $this->withHeaders(bearer($expired))->getJson(route('api.v1.operator.rate-card.show'))->assertOk();
    $this->withHeaders(bearer($expired))->postJson(route('api.v1.operator.rate-cards.store'), rateCard())
        ->assertForbidden()->assertJsonPath('message', 'Your province license is not active. Renew it to make changes.');
    $this->withHeaders(bearer($expired))->patchJson(route('api.v1.operator.province.update'), ['store_fee_centavos' => 0])->assertForbidden();
});
