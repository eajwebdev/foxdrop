<?php

namespace App\Support;

/**
 * Money is integer centavos (§0). Percentages are basis points; shares round half-up (§6).
 */
final class Money
{
    /**
     * [bps] basis points of [centavos], rounded half-up (8000 bps of 4000 = 3200).
     */
    public static function share(int $centavos, int $bps): int
    {
        return intdiv($centavos * $bps + 5_000, 10_000);
    }
}
