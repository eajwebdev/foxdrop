<?php

namespace App\Contracts;

use App\Services\Distance\Distance;

/**
 * Road distance from a store to a delivery address (§6, DECISIONS 85).
 */
interface DistanceProvider
{
    public function between(float $fromLat, float $fromLng, float $toLat, float $toLng): Distance;
}
