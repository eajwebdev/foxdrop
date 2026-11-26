<?php

namespace App\Http\Resources;

use App\Models\Rider;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * @mixin Rider
 */
class RiderSummaryResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'status' => $this->status,
            'full_name' => $this->user?->full_name,
            'phone' => $this->user?->phone,
            'vehicle_type' => $this->vehicle_type,
            'plate_no' => $this->plate_no,
            'created_at' => $this->created_at?->toIso8601String(),
        ];
    }
}
