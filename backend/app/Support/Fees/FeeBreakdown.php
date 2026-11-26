<?php

namespace App\Support\Fees;

/**
 * A delivery fee and how it splits: the rider's earning and the operator's cut always add up to the fee.
 */
final readonly class FeeBreakdown
{
    public function __construct(
        public int $deliveryFeeCentavos,
        public int $riderEarningCentavos,
        public int $operatorCutCentavos,
    ) {}
}
