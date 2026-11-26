<?php

use App\Support\Fees\DeliveryFeeCalculator;
use App\Support\Fees\RateCardTerms;

/** The §14 default card: ₱40 covers the first 2 km, then ₱10/km, minimum ₱40, up to 15 km, rider 80 %. */
function defaultCard(): RateCardTerms
{
    return new RateCardTerms(4000, 2.0, 1000, 4000, 15.0, 8000);
}

it('prices the MASTER_PROMPT §6 examples', function (float $km, ?int $fee) {
    expect((new DeliveryFeeCalculator)->quote(defaultCard(), $km)?->deliveryFeeCentavos)->toBe($fee);
})->with([
    '1 km (base fare covers it)' => [1.0, 4000],
    '3 km' => [3.0, 5000],
    '5 km' => [5.0, 7000],
    '10 km' => [10.0, 12000],
    'exactly the maximum' => [15.0, 17000],
    'past the maximum' => [15.1, null],
    '2.05 km rounds up to 2.1 km' => [2.05, 4100],
    'float noise does not add a tenth' => [2.0000000001, 4000],
]);

it('splits the fee between rider and operator so they add up exactly', function () {
    $fee = (new DeliveryFeeCalculator)->quote(new RateCardTerms(4000, 2.0, 1000, 4000, 15.0, 7777), 3.3);

    expect($fee?->deliveryFeeCentavos)->toBe(5300)
        ->and($fee?->riderEarningCentavos)->toBe(4122) // 5300 × 0.7777 = 4121.81 → half-up
        ->and($fee?->riderEarningCentavos + $fee?->operatorCutCentavos)->toBe(5300);
});

it('never charges less than the minimum fee', function () {
    expect((new DeliveryFeeCalculator)->quote(new RateCardTerms(1000, 0.0, 500, 6000, 20.0, 8000), 1.0)?->deliveryFeeCentavos)->toBe(6000);
});

it('rounds half a centavo up', function () {
    // 0.1 km × 15 centavos/km = 1.5 centavos → 2
    expect((new DeliveryFeeCalculator)->quote(new RateCardTerms(0, 0.0, 15, 0, 5.0, 10000), 0.1)?->deliveryFeeCentavos)->toBe(2);
});
