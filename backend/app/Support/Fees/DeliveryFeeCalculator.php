<?php

namespace App\Support\Fees;

/**
 * The delivery fee formula (MASTER_PROMPT §6, docs/API.md), in integer centavos so splits always add up:
 *
 *   fee = max(min_fee, base_fare + ceil_to_0.1(max(0, km − base_km)) × rate_per_km)
 *   rider_earning = fee × rider_share_bps / 10000 (half-up), operator_cut = fee − rider_earning
 *
 * Distances are counted in tenths of a km, rounded up. Beyond `max_km` the province does not deliver (null).
 * Surcharges (night, bad weather, extra stops) are added by the order quote, not here.
 */
final class DeliveryFeeCalculator
{
    public function quote(RateCardTerms $card, float $distanceKm): ?FeeBreakdown
    {
        $tenths = $this->tenthsRoundedUp($distanceKm);
        if ($tenths > $this->tenthsRoundedUp($card->maxKm)) {
            return null;
        }

        $extraTenths = max(0, $tenths - (int) round($card->baseKm * 10));
        $distancePart = intdiv($extraTenths * $card->ratePerKmCentavos + 5, 10);
        $fee = max($card->minFeeCentavos, $card->baseFareCentavos + $distancePart);
        $riderEarning = intdiv($fee * $card->riderShareBps + 5_000, 10_000);

        return new FeeBreakdown($fee, $riderEarning, $fee - $riderEarning);
    }

    /**
     * Whole tenths of a km, rounded up; tiny float noise (2.0000000001) does not add a tenth.
     */
    private function tenthsRoundedUp(float $km): int
    {
        return (int) ceil(round(max(0.0, $km) * 10, 6));
    }
}
