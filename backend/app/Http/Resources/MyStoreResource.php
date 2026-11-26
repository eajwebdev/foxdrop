<?php

namespace App\Http\Resources;

use App\Models\Store;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * @mixin Store
 */
class MyStoreResource extends JsonResource
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
            'rejection_reason' => $this->rejection_reason,
            'province' => ['id' => $this->province_id, 'name' => $this->province?->name],
            'created_at' => $this->created_at?->toIso8601String(),
        ];
    }
}
