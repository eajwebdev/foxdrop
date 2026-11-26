<?php

it('lists every province and marks only licensed ones serviceable', function () {
    $provinces = collect($this->getJson(route('api.v1.provinces.index'))->assertOk()->json('data'));

    expect($provinces)->toHaveCount(83)
        ->and($provinces->where('is_serviceable', true)->pluck('name')->all())->toBe(['Negros Occidental']);
});
