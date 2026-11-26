<?php

use Database\Seeders\DemoSeeder;
use Illuminate\Support\Facades\DB;

it('lists every province and marks only licensed ones serviceable', function () {
    $provinces = collect($this->getJson(route('api.v1.provinces.index'))->assertOk()->json('data'));

    expect($provinces)->toHaveCount(83)
        ->and($provinces->where('is_serviceable', true)->pluck('name')->all())->toBe(['Negros Occidental']);
});

it('says where an operator can still apply', function () {
    $provinces = collect($this->getJson(route('api.v1.provinces.index'))->json('data'))->keyBy('psgc_code');

    expect($provinces[ILOILO]['is_available'])->toBeTrue()
        ->and($provinces[DemoSeeder::NEGROS_OCCIDENTAL]['is_available'])->toBeFalse()
        ->and($provinces['0702200000']['is_available'])->toBeFalse() // Cebu: an application is waiting for the admin
        ->and($provinces->where('is_available', true))->toHaveCount(81);
});

it('keeps a suspended province closed to new applicants (DECISIONS 77)', function () {
    DB::table('province_licenses')->where('province_id', provinceId(DemoSeeder::NEGROS_OCCIDENTAL))->update(['status' => 'SUSPENDED']);

    $negros = collect($this->getJson(route('api.v1.provinces.index'))->json('data'))->firstWhere('psgc_code', DemoSeeder::NEGROS_OCCIDENTAL);

    expect($negros['is_available'])->toBeFalse()->and($negros['is_serviceable'])->toBeFalse();
});
