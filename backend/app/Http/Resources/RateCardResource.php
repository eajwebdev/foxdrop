<?php

namespace App\Http\Resources;

use App\Models\DeliveryRateCard;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * @mixin DeliveryRateCard
 */
class RateCardResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return ['id' => $this->id]
            + collect(DeliveryRateCard::TERMS)->mapWithKeys(fn (string $field): array => [$field => $this->{$field}])->all()
            + ['effective_from' => $this->effective_from?->toIso8601String()];
    }
}
