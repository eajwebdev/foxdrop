<?php

namespace App\Http\Resources;

use App\Models\OperatorApplication;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * @mixin OperatorApplication
 */
class OperatorApplicationResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'status' => $this->status,
            'province' => ['id' => $this->province_id, 'name' => $this->province?->name],
            'business_name' => $this->business_name,
            'rejection_reason' => $this->rejection_reason,
            'created_at' => $this->created_at?->toIso8601String(),
            'license' => $this->license === null ? null : LicenseResource::make($this->license),
        ];
    }
}
