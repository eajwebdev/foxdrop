<?php

namespace App\Http\Resources;

use App\Models\Store;
use Illuminate\Http\Request;

/**
 * A store application with everything the reviewer needs; the location comes from Store::withLocation().
 *
 * @mixin Store
 */
class StoreReviewResource extends StoreSummaryResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        $lat = $this->resource->getAttribute('location_lat');

        return parent::toArray($request) + [
            'description' => $this->resource->getAttribute('description'),
            'barangay' => $this->resource->getAttribute('barangay'),
            'phone' => $this->resource->getAttribute('phone'),
            'location' => $lat === null ? null : ['lat' => (float) $lat, 'lng' => (float) $this->resource->getAttribute('location_lng')],
            'rejection_reason' => $this->rejection_reason,
            'documents' => DocumentResource::collection($this->documents),
        ];
    }
}
