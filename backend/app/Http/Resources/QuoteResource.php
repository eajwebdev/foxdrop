<?php

namespace App\Http\Resources;

use App\Models\Quote;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * The checkout summary (§9.1 step 5): every amount the phone shows. The split between store, rider, operator and
 * platform stays on the server.
 *
 * @mixin Quote
 */
class QuoteResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'store_id' => $this->store_id,
            'fulfillment' => $this->fulfillment,
            'items' => $this->items,
            'item_subtotal_centavos' => $this->item_subtotal_centavos,
            'delivery_fee_centavos' => $this->delivery_fee_centavos,
            'discount_centavos' => $this->discount_centavos,
            'total_centavos' => $this->total_centavos,
            'distance_km' => $this->distance_km,
            'promo_applied' => $this->promo_id !== null,
            'expires_at' => $this->expires_at->toIso8601String(),
        ];
    }
}
