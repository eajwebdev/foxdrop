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

}
