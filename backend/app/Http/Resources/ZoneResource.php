<?php

namespace App\Http\Resources;

use App\Models\DeliveryZone;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * A coverage zone; the geometry columns come from DeliveryZone::withGeometry().
 *
 * @mixin DeliveryZone
 */
class ZoneResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        $lat = $this->resource->getAttribute('center_lat');
        $lng = $this->resource->getAttribute('center_lng');
        $radius = $this->resource->getAttribute('radius_m');

        return [
            'id' => $this->id,
            'name' => $this->name,
            'is_active' => $this->is_active,
            'center' => $lat === null ? null : ['lat' => (float) $lat, 'lng' => (float) $lng],
            'radius_km' => $radius === null ? null : $radius / 1000,
            'area_km2' => (float) $this->resource->getAttribute('area_km2'),
        ];
    }
}
