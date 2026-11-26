<?php

namespace App\Http\Resources;

use App\Models\Payment;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * @mixin Payment
 */
class PaymentResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'purpose' => $this->purpose,
            'status' => $this->status,
            'amount_centavos' => $this->amount_centavos,
            'checkout_url' => $this->checkout_url,
            'expires_at' => $this->expires_at?->toIso8601String(),
        ];
    }
}
