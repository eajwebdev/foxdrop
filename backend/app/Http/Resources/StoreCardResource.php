<?php

namespace App\Http\Resources;

use App\Models\Store;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * A store in the customer's lists (§9.1 step 2): rating, category, distance, ETA range and whether it is open now.
 * `distance_km` and the ETA are present when the list was asked for a location.
 *
 * @mixin Store
 */
class StoreCardResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        $meters = $this->resource->getAttribute('distance_m');
        $km = $meters === null ? null : round((float) $meters / 1000, 1);
        $travel = $km === null ? 0 : (int) ceil($km * (int) config('foxdrop.ordering.minutes_per_km'));
        $etaMin = $this->prep_time_min + $travel;

        return [
            'id' => $this->id,
            'name' => $this->name,
            'category' => $this->category,
            'description' => $this->resource->getAttribute('description'),
            'address_line' => $this->address_line,
            'city' => $this->city,
            'rating_avg' => round($this->rating_avg, 1),
            'rating_count' => $this->rating_count,
            'distance_km' => $km,
            'eta_min' => $etaMin,
            'eta_max' => $etaMin + 15,
            'is_open' => $this->isOpenAt(now()),
            'accepts_cod' => $this->accepts_cod,
            'accepts_pickup' => $this->accepts_pickup,
            'prep_time_min' => $this->prep_time_min,
        ];
    }
}
