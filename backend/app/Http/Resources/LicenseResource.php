<?php

namespace App\Http\Resources;

use App\Models\ProvinceLicense;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * A province license as the operator sees it (docs/API.md `License`).
 *
 * @mixin ProvinceLicense
 */
class LicenseResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'province' => ['id' => $this->province_id, 'name' => $this->province?->name],
            'status' => $this->status,
            'start_at' => $this->start_at?->toIso8601String(),
            'end_at' => $this->end_at?->toIso8601String(),
            'days_left' => $this->end_at === null ? null : (int) floor(now()->diffInDays($this->end_at, false)),
            'fee_centavos' => $this->fee_centavos,
            'can_pay' => in_array($this->status, ProvinceLicense::PAYABLE, true),
        ];
    }
}
