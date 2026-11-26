<?php

namespace App\Http\Resources;

use App\Actions\Ordering\OrderTransition;
use App\Models\Order;
use App\Models\OrderItem;
use App\Support\PlatformSettings;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * An order on the store's board (§9.3): what to prepare, how it is paid, when it must be answered, and which buttons
 * the store may press now. The customer's first name only; never their phone, address or codes.
 * Load `items` and `customer`.
 *
 * @mixin Order
 */
class StoreOrderResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        $firstName = trim(explode(' ', (string) $this->customer?->full_name)[0]);

        return [
            'id' => $this->id,
            'code' => $this->code,
            'status' => $this->status,
            'fulfillment' => $this->fulfillment,
            'payment_method' => $this->payment_method,
            'payment_status' => $this->payment_status,
            'customer_name' => $firstName === '' ? 'Customer' : $firstName,
            'items' => $this->items->map(fn (OrderItem $item): array => [
                'name' => $item->name,
                'quantity' => $item->quantity,
                'options' => $item->options,
                'line_total_centavos' => $item->line_total_centavos,
            ])->all(),
            'item_subtotal_centavos' => $this->item_subtotal_centavos,
            'store_net_centavos' => $this->store_net_centavos,
            // What the customer hands over at the counter for a pick-up order paid at the store.
            'collect_centavos' => $this->fulfillment === Order::PICKUP && $this->payment_method === Order::COD ? $this->total_centavos : 0,
            'notes' => $this->notes,
            'placed_at' => $this->placed_at?->toIso8601String(),
            'answer_by' => $this->status === Order::PLACED ? $this->placed_at?->copy()->addSeconds(PlatformSettings::storeResponseTimeoutSeconds())->toIso8601String() : null,
            'eta_at' => $this->eta_at?->toIso8601String(),
            'next_steps' => OrderTransition::storeNextSteps($this->resource),
            'cancel_reason' => $this->cancel_reason,
        ];
    }
}
