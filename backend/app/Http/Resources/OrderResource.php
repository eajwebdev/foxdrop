<?php

namespace App\Http\Resources;

use App\Models\Order;
use App\Models\OrderEvent;
use App\Models\OrderItem;
use App\Models\Payment;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * An order as its customer sees it (§9.1 steps 5–7): amounts, timeline and, while it is going, the code they need
 * (pick-up code at the counter, delivery OTP for the rider). Load `store`, `items`, `events`, `payment`.
 *
 * @mixin Order
 */
class OrderResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        $active = $this->isActive();
        /** @var Payment|null $payment */
        $payment = $this->payment;

        return [
            'id' => $this->id,
            'code' => $this->code,
            'status' => $this->status,
            'fulfillment' => $this->fulfillment,
            'payment_method' => $this->payment_method,
            'payment_status' => $this->payment_status,
            'store' => ['id' => $this->store_id, 'name' => $this->store?->name, 'phone' => $this->store?->getAttribute('phone')],
            'items' => $this->whenLoaded('items', fn () => $this->items->map(fn (OrderItem $item): array => [
                'name' => $item->name,
                'quantity' => $item->quantity,
                'options' => $item->options,
                'unit_price_centavos' => $item->unit_price_centavos,
                'line_total_centavos' => $item->line_total_centavos,
            ])->all()),
            'item_subtotal_centavos' => $this->item_subtotal_centavos,
            'delivery_fee_centavos' => $this->delivery_fee_centavos,
            'discount_centavos' => $this->discount_centavos,
            'total_centavos' => $this->total_centavos,
            'dropoff' => $this->dropoff_snapshot,
            'notes' => $this->notes,
            'eta_at' => $this->eta_at?->toIso8601String(),
            'cancel_reason' => $this->cancel_reason,
            'can_cancel' => in_array($this->status, [Order::PENDING_PAYMENT, Order::PLACED], true),
            // The codes go only to the customer, and only while the order is going (§9.1 step 7).
            'pickup_code' => $active && $this->fulfillment === Order::PICKUP ? $this->pickup_code : null,
            'delivery_otp' => $active && $this->fulfillment === Order::DELIVERY ? $this->delivery_otp : null,
            'payment' => $payment === null ? null : [
                'id' => $payment->id,
                'status' => $payment->status,
                'checkout_url' => $payment->status === Payment::PENDING ? $payment->checkout_url : null,
            ],
            'timeline' => $this->whenLoaded('events', fn () => $this->events->map(fn (OrderEvent $event): array => [
                'status' => $event->to_status,
                'at' => $event->created_at->toIso8601String(),
                'note' => $event->note,
            ])->all()),
            'created_at' => $this->created_at?->toIso8601String(),
        ];
    }
}
