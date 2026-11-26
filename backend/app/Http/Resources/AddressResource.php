<?php

namespace App\Http\Resources;

use App\Models\Address;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * @mixin Address
 */
class AddressResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'label' => $this->label,
            'address_line' => $this->address_line,
            'landmark' => $this->landmark,
            'barangay' => $this->barangay,
            'city' => $this->city,
            'location' => ['lat' => $this->lat(), 'lng' => $this->lng()],
            'province_id' => $this->province_id,
        ];
    }
}
