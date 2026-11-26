<?php

namespace App\Actions\Ordering;

use App\Contracts\PaymentProvider;
use App\Models\Address;
use App\Models\Order;
use App\Models\Payment;
use App\Models\Product;
use App\Models\Province;
use App\Models\Quote;
use App\Models\Store;
use App\Models\User;
use App\Support\PlatformSettings;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;
use Throwable;

/**
 * Turns a quote into an order (§5.4, §6, DECISIONS 86): only the quote's amounts are used. Re-checks the store and
 * stock under lock, reserves stock, makes the codes, and for online payment opens the checkout. A retry with the same
 * Idempotency-Key returns the same order.
 */
final class PlaceOrder
{
    public function __construct(
        private readonly PaymentProvider $provider,
        private readonly OrderTransition $transition,
    ) {}

    /**
     * @return array{order: Order, payment: Payment|null, created: bool}
     */
    public function __invoke(User $customer, string $quoteId, string $paymentMethod, ?string $notes, ?string $idempotencyKey): array
    {
        $previous = $this->previousOrder($customer, $quoteId, $idempotencyKey);
        if ($previous !== null) {
            return ['order' => $previous, 'payment' => $previous->payment, 'created' => false];
        }

        [$order, $payment] = DB::transaction(function () use ($customer, $quoteId, $paymentMethod, $notes, $idempotencyKey): array {
            $quote = Quote::query()->lockForUpdate()->find($quoteId);
            if ($quote === null || $quote->customer_id !== $customer->id) {
                self::fail('quote_id', 'This price check was not found. Please review your cart again.');
            }
            if ($quote->used_at !== null || $quote->expires_at->isPast()) {
                self::fail('quote_id', 'Prices may have changed. Please review your cart again.');
            }
            $store = Store::query()->lockForUpdate()->findOrFail($quote->store_id);
            if (! $store->isOpenAt(now())) {
                self::fail('quote_id', 'This store is not taking orders right now.');
            }
            if ($paymentMethod === Order::COD && ! $store->accepts_cod) {
                self::fail('payment_method', 'This store takes online payment only.');
            }
            $this->reserveStock($quote);

            $order = $this->createOrder($customer, $store, $quote, $paymentMethod, $notes, $idempotencyKey);
            $quote->forceFill(['used_at' => now()])->save();

            $payment = $paymentMethod === Order::ONLINE ? Payment::query()->create([
                'province_id' => $order->province_id,
                'purpose' => Payment::PURPOSE_ORDER,
                'provider' => $this->provider->name(),
                'amount_centavos' => $order->total_centavos,
                'status' => Payment::PENDING,
                'payer_user_id' => $customer->id,
                'order_id' => $order->id,
                'expires_at' => now()->addSeconds(PlatformSettings::unpaidOrderTimeoutSeconds()),
            ]) : null;

            return [$order, $payment];
        });

        if ($payment !== null) {
            $this->openCheckout($order, $payment);
        }

        return ['order' => $order->refresh(), 'payment' => $payment?->refresh(), 'created' => true];
    }

    private function previousOrder(User $customer, string $quoteId, ?string $idempotencyKey): ?Order
    {
        return Order::query()
            ->where('customer_id', $customer->id)
            ->where(fn ($match) => $match->where('quote_id', $quoteId)
                ->when($idempotencyKey !== null, fn ($byKey) => $byKey->orWhere('idempotency_key', $idempotencyKey)))
            ->first();
    }

    /**
     * Tracked stock is taken now and given back if the order is cancelled (DECISIONS 90).
     */
    private function reserveStock(Quote $quote): void
    {
        $wanted = [];
        foreach ($quote->items as $item) {
            $wanted[$item['product_id']] = ($wanted[$item['product_id']] ?? 0) + $item['quantity'];
        }
        $products = Product::query()->whereIn('id', array_keys($wanted))->lockForUpdate()->get()->keyBy('id');
        foreach ($wanted as $productId => $quantity) {
            $product = $products->get($productId);
            if ($product === null || ! $product->is_available) {
                self::fail('quote_id', 'An item in your cart is no longer available. Please review your cart.');
            }
            if ($product->stock !== null) {
                if ($product->stock < $quantity) {
                    self::fail('quote_id', "Only {$product->stock} {$product->name} left. Please review your cart.");
                }
                $product->decrement('stock', $quantity);
            }
        }
    }

    private function createOrder(User $customer, Store $store, Quote $quote, string $paymentMethod, ?string $notes, ?string $idempotencyKey): Order
    {
        $split = $quote->split ?? [];
        $address = $quote->fulfillment === Order::DELIVERY && isset($split['address_id'])
            ? Address::query()->find($split['address_id'])
            : null;
        $sequence = (int) DB::selectOne("select nextval('public.order_code_seq') as n")->n;
        $code = sprintf('FD-%s-%06d', Province::shortCode($store->province_id), $sequence);
        $isOnline = $paymentMethod === Order::ONLINE;

        $order = new Order;
        $order->forceFill([
            'code' => $code,
            'province_id' => $store->province_id,
            'customer_id' => $customer->id,
            'store_id' => $store->id,
            'quote_id' => $quote->id,
            'idempotency_key' => $idempotencyKey,
            'fulfillment' => $quote->fulfillment,
            'payment_method' => $paymentMethod,
            'payment_status' => $isOnline ? 'PENDING' : 'UNPAID',
            'status' => $isOnline ? Order::PENDING_PAYMENT : Order::PLACED,
            'placed_at' => $isOnline ? null : now(),
            'item_subtotal_centavos' => $quote->item_subtotal_centavos,
            'delivery_fee_centavos' => $quote->delivery_fee_centavos,
            'discount_centavos' => $quote->discount_centavos,
            'total_centavos' => $quote->total_centavos,
            'platform_commission_centavos' => $split['platform_commission_centavos'] ?? 0,
            'operator_commission_centavos' => $split['operator_commission_centavos'] ?? 0,
            'rider_earning_centavos' => $split['rider_earning_centavos'] ?? 0,
            'operator_delivery_cut_centavos' => $split['operator_delivery_cut_centavos'] ?? 0,
            'store_net_centavos' => $split['store_net_centavos'] ?? $quote->item_subtotal_centavos,
            'rate_card_snapshot' => $quote->rate_card_snapshot,
            'distance_km' => $quote->distance_km,
            'dropoff_snapshot' => $address === null ? null : [
                'label' => $address->label,
                'address_line' => $address->address_line,
                'landmark' => $address->landmark,
                'barangay' => $address->barangay,
                'city' => $address->city,
                'contact_name' => $customer->full_name,
                'contact_phone' => $customer->phone,
            ],
            'dropoff_location' => $quote->fulfillment === Order::DELIVERY
                ? DB::table('quotes')->where('id', $quote->id)->selectRaw('ST_AsEWKT(dropoff_location) as point')->value('point')
                : null,
            'pickup_code' => $quote->fulfillment === Order::PICKUP ? self::code() : null,
            'delivery_otp' => $quote->fulfillment === Order::DELIVERY ? self::code() : null,
            'notes' => $notes,
        ])->save();

        DB::table('order_items')->insert(array_map(fn (array $item): array => [
            'order_id' => $order->id,
            'province_id' => $order->province_id,
            'product_id' => $item['product_id'],
            'name' => $item['name'],
            'unit_price_centavos' => $item['unit_price_centavos'],
            'quantity' => $item['quantity'],
            'options' => json_encode($item['options'], JSON_THROW_ON_ERROR),
            'line_total_centavos' => $item['line_total_centavos'],
        ], $quote->items));
        DB::table('order_events')->insert([
            'order_id' => $order->id,
            'province_id' => $order->province_id,
            'actor_user_id' => $customer->id,
            'actor_role' => 'customer',
            'from_status' => null,
            'to_status' => $order->status,
            'note' => $isOnline ? 'Waiting for online payment' : 'Cash on delivery',
        ]);

        return $order;
    }

    /**
     * Opens the hosted checkout after the order is saved. If the gateway cannot start one, the order is cancelled
     * (giving back the stock) so nothing is left half-made.
     */
    private function openCheckout(Order $order, Payment $payment): void
    {
        try {
            $session = $this->provider->createCheckout($payment, "FoxDrop order {$order->code}");
        } catch (Throwable $e) {
            $payment->forceFill(['status' => Payment::FAILED])->save();
            ($this->transition)($order, Order::CANCELLED, null, OrderTransition::SYSTEM, ['note' => 'Online payment could not start.']);

            throw $e;
        }
        $payment->forceFill(['intent_id' => $session->intentId, 'checkout_url' => $session->checkoutUrl])->save();
    }

    /** A 4-digit code the customer shows (pick-up) or tells the rider (delivery). */
    private static function code(): string
    {
        return str_pad((string) random_int(0, 9999), 4, '0', STR_PAD_LEFT);
    }

    /**
     * @throws ValidationException
     */
    private static function fail(string $field, string $message): never
    {
        throw ValidationException::withMessages([$field => $message]);
    }
}
