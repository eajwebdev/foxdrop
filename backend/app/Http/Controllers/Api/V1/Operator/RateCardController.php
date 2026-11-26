<?php

namespace App\Http\Controllers\Api\V1\Operator;

use App\Actions\Territory\PublishRateCard;
use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\Operator\PreviewRateCardRequest;
use App\Http\Requests\Api\V1\Operator\PublishRateCardRequest;
use App\Http\Resources\RateCardResource;
use App\Models\DeliveryRateCard;
use App\Models\User;
use App\Support\Fees\DeliveryFeeCalculator;
use App\Support\Fees\RateCardTerms;
use App\Support\OperatorContext;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * "Set your own delivery rates per KM" (§9.4 step 5): the current card, a new version, and a live preview computed
 * by the same calculator the order quote uses.
 */
class RateCardController extends Controller
{
    public function show(Request $request): RateCardResource
    {
        /** @var User $user */
        $user = $request->user();

        return RateCardResource::make(DeliveryRateCard::currentFor(OperatorContext::for($user)->provinceId));
    }

    public function store(PublishRateCardRequest $request, PublishRateCard $publish): JsonResponse
    {
        /** @var User $user */
        $user = $request->user();
        $terms = $request->safe()->only(DeliveryRateCard::TERMS);

        return RateCardResource::make($publish(OperatorContext::for($user), $terms))->response()->setStatusCode(201);
    }

    public function preview(PreviewRateCardRequest $request, DeliveryFeeCalculator $calculator): JsonResponse
    {
        /** @var User $user */
        $user = $request->user();
        OperatorContext::for($user);

        $terms = new RateCardTerms(
            baseFareCentavos: $request->integer('base_fare_centavos'),
            baseKm: $request->float('base_km'),
            ratePerKmCentavos: $request->integer('rate_per_km_centavos'),
            minFeeCentavos: $request->integer('min_fee_centavos'),
            maxKm: $request->float('max_km'),
            riderShareBps: $request->integer('rider_share_bps'),
        );

        /** @var list<int|float|string> $distances */
        $distances = $request->input('distances_km');

        return response()->json(['data' => array_map(function (int|float|string $km) use ($calculator, $terms): array {
            $fee = $calculator->quote($terms, (float) $km);

            return [
                'distance_km' => (float) $km,
                'delivery_fee_centavos' => $fee?->deliveryFeeCentavos,
                'rider_earning_centavos' => $fee?->riderEarningCentavos,
                'operator_cut_centavos' => $fee?->operatorCutCentavos,
            ];
        }, $distances)]);
    }
}
