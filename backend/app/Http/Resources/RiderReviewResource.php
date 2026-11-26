<?php

namespace App\Http\Resources;

use App\Models\Rider;
use Illuminate\Http\Request;

/**
 * @mixin Rider
 */
class RiderReviewResource extends RiderSummaryResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return parent::toArray($request) + [
            'license_no_last4' => $this->license_no_last4,
            'license_expiry' => $this->license_expiry?->toDateString(),
            'rejection_reason' => $this->rejection_reason,
            'documents' => DocumentResource::collection($this->documents),
        ];
    }
}
