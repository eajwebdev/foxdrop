<?php

namespace App\Services\Distance;

use App\Contracts\DistanceProvider;
use Illuminate\Http\Client\ConnectionException;
use Illuminate\Http\Client\RequestException;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

/**
 * Driving distance from the Google Routes API (§6). If Google cannot answer, the quote still works with the
 * straight-line estimate, so checkout never stops because of a map service.
 */
final class GoogleRoutesDistance implements DistanceProvider
{
    public function __construct(private readonly StraightLineDistance $fallback) {}

    public function between(float $fromLat, float $fromLng, float $toLat, float $toLng): Distance
    {
        try {
            $meters = Http::withHeaders([
                'X-Goog-Api-Key' => (string) config('services.google_routes.key'),
                'X-Goog-FieldMask' => 'routes.distanceMeters',
            ])
                ->timeout(5)
                ->post((string) config('services.google_routes.url'), [
                    'origin' => ['location' => ['latLng' => ['latitude' => $fromLat, 'longitude' => $fromLng]]],
                    'destination' => ['location' => ['latLng' => ['latitude' => $toLat, 'longitude' => $toLng]]],
                    'travelMode' => 'TWO_WHEELER',
                    'routingPreference' => 'TRAFFIC_UNAWARE',
                ])
                ->throw()
                ->json('routes.0.distanceMeters');
        } catch (ConnectionException|RequestException $e) {
            Log::warning('Google Routes unavailable; using the straight-line distance', ['error' => $e->getMessage()]);

            return $this->fallback->between($fromLat, $fromLng, $toLat, $toLng);
        }

        return is_numeric($meters)
            ? new Distance(((float) $meters) / 1000, 'routes')
            : $this->fallback->between($fromLat, $fromLng, $toLat, $toLng);
    }
}
