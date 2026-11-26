<?php

use Database\Seeders\DemoSeeder;

it('lists every province and marks only licensed ones serviceable', function () {
    $provinces = collect($this->getJson(route('api.v1.provinces.index'))->assertOk()->json('data'));

    expect($provinces)->toHaveCount(83)
        ->and($provinces->where('is_serviceable', true)->pluck('name')->all())->toBe(['Negros Occidental']);
});

it('says where an operator can still apply', function () {
    $provinces = collect($this->getJson(route('api.v1.provinces.index'))->json('data'))->keyBy('psgc_code');

    expect($provinces[ILOILO]['is_available'])->toBeTrue()
        ->and($provinces[DemoSeeder::NEGROS_OCCIDENTAL]['is_available'])->toBeFalse()
        ->and($provinces->where('is_available', true))->toHaveCount(82);
});
