<?php

namespace App\Support\Fees;

/**
 * The parts of a delivery rate card the fee formula needs (§6). Money in centavos, distances in km.
 */
final readonly class RateCardTerms
{
    public function __construct(
        public int $baseFareCentavos,
        public float $baseKm,
        public int $ratePerKmCentavos,
        public int $minFeeCentavos,
        public float $maxKm,
        public int $riderShareBps,
    ) {}

    /**
     * @param  array{base_fare_centavos: int|string, base_km: float|int|string, rate_per_km_centavos: int|string, min_fee_centavos: int|string, max_km: float|int|string, rider_share_bps: int|string}  $card
     */
    public static function fromArray(array $card): self
    {
        return new self(
            baseFareCentavos: (int) $card['base_fare_centavos'],
            baseKm: (float) $card['base_km'],
            ratePerKmCentavos: (int) $card['rate_per_km_centavos'],
            minFeeCentavos: (int) $card['min_fee_centavos'],
            maxKm: (float) $card['max_km'],
            riderShareBps: (int) $card['rider_share_bps'],
        );
    }
}
