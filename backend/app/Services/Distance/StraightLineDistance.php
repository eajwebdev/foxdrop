<?php

namespace App\Services\Distance;

use App\Contracts\DistanceProvider;

/**
 * Great-circle distance × a road factor (DECISIONS 85): used until a Google Routes key is configured, and when the
 * Routes API cannot be reached.
 */
final class StraightLineDistance implements DistanceProvider
{
    private const float EARTH_RADIUS_KM = 6371.0088;

    public function between(float $fromLat, float $fromLng, float $toLat, float $toLng): Distance
    {
        $dLat = deg2rad($toLat - $fromLat);
        $dLng = deg2rad($toLng - $fromLng);
        $a = sin($dLat / 2) ** 2 + cos(deg2rad($fromLat)) * cos(deg2rad($toLat)) * sin($dLng / 2) ** 2;
        $km = 2 * self::EARTH_RADIUS_KM * asin(min(1.0, sqrt($a)));

        return new Distance($km * (float) config('foxdrop.ordering.road_factor'), 'straight_line');
    }
}
