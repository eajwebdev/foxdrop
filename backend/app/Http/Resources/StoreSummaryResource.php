<?php

namespace App\Http\Resources;

use App\Models\Store;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * A store in the operator's review list.
 *
 * @mixin Store
 */
class StoreSummaryResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'name' => $this->name,
            'category' => $this->category,
            'status' => $this->status,
            'address_line' => $this->address_line,
            'city' => $this->city,
            'owner' => ['full_name' => $this->owner?->full_name, 'phone' => $this->owner?->phone],
            'created_at' => $this->created_at?->toIso8601String(),
        ];
    }
}
