<?php

namespace App\Http\Resources;

use App\Models\Rider;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * The rider's own registration status (the license number is never sent back, only its last 4).
 *
 * @mixin Rider
 */
class MyRiderResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'status' => $this->status,
            'rejection_reason' => $this->rejection_reason,
            'province' => ['id' => $this->province_id, 'name' => $this->province?->name],
            'vehicle_type' => $this->vehicle_type,
            'plate_no' => $this->plate_no,
            'license_no_last4' => $this->license_no_last4,
            'license_expiry' => $this->license_expiry?->toDateString(),
        ];
    }
}
