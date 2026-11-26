<?php

namespace App\Actions\Ordering;

use App\Contracts\DistanceProvider;
use App\Models\Address;
use App\Models\DeliveryRateCard;
use App\Models\DeliveryZone;
use App\Models\Order;
use App\Models\Product;
use App\Models\Promo;
use App\Models\Province;
use App\Models\Quote;
use App\Models\Store;
use App\Models\User;
use App\Support\Fees\DeliveryFeeCalculator;
use App\Support\Fees\FeeBreakdown;
use App\Support\Fees\RateCardTerms;
use App\Support\Money;
use App\Support\PlatformSettings;
use Illuminate\Validation\ValidationException;

/**
 * The server's price for a cart (§6, DECISIONS 86): items re-priced from the catalog, options checked, the delivery
 * fee from the province's rate card and the road distance, the promo, and how the money splits. Every amount the
 * phone shows comes from here; placing the order uses this quote and nothing the phone sends.
 */
final class CalculateQuote
{
    public function __construct(
        private readonly DistanceProvider $distance,
        private readonly DeliveryFeeCalculator $fees,
    ) {}

    /**
     * @param  list<array{product_id: string, quantity: int, options?: list<array{group: string, name: string}>}>  $lines
     */
    public function __invoke(User $customer, Store $store, string $fulfillment, array $lines, ?Address $address, ?string $promoCode): Quote
    {
        $store = Store::query()->withLocation()->published()->find($store->id) ?? self::fail('store_id', 'This store is not on FoxDrop right now.');
        if (! $store->isOpenAt(now())) {
            self::fail('store_id', $store->status === Store::PAUSED ? 'This store is not taking orders right now.' : 'This store is closed right now.');
        }
        if ($fulfillment === Order::PICKUP && ! $store->accepts_pickup) {
            self::fail('fulfillment', 'This store does not offer pick-up.');
        }

        $items = $this->priceItems($store, $lines);
        $subtotal = array_sum(array_column($items, 'line_total_centavos'));
        $card = DeliveryRateCard::currentFor($store->province_id);
        $distanceKm = $source = $fee = null;
        if ($fulfillment === Order::DELIVERY) {
            [$distanceKm, $source, $fee] = $this->delivery($store, $card, $address ?? self::fail('address_id', 'Choose where to deliver.'));
        }
        $deliveryFee = $fee === null ? 0 : $fee->deliveryFeeCentavos;

        $promo = $promoCode === null || trim($promoCode) === '' ? null : $this->promo($customer, $store, strtoupper(trim($promoCode)), $subtotal, $fulfillment);
        $discount = $promo === null ? 0 : $this->discount($promo, $subtotal, $deliveryFee);

        $province = Province::query()->findOrFail($store->province_id);
        $platformCommission = Money::share($subtotal, PlatformSettings::platformCommissionBps());
        $operatorCommission = Money::share($subtotal, min($province->operator_commission_bps, PlatformSettings::operatorCommissionCapBps()));
        $storeNet = $subtotal - $platformCommission - $operatorCommission - ($promo?->scope === 'STORE' ? $discount : 0);

        return Quote::query()->create([
            'province_id' => $store->province_id,
            'customer_id' => $customer->id,
            'store_id' => $store->id,
            'fulfillment' => $fulfillment,
            // EWKT text: PostGIS reads it straight into the geography column.
            'dropoff_location' => $address === null || $fulfillment === Order::PICKUP ? null : sprintf('SRID=4326;POINT(%F %F)', $address->lng(), $address->lat()),
            'distance_km' => $distanceKm,
            'distance_source' => $source,
            'item_subtotal_centavos' => $subtotal,
            'delivery_fee_centavos' => $deliveryFee,
            'discount_centavos' => $discount,
            'total_centavos' => $subtotal + $deliveryFee - $discount,
            'rate_card_id' => $card->exists ? $card->id : null,
            'rate_card_snapshot' => $fulfillment === Order::DELIVERY ? $card->only(DeliveryRateCard::TERMS) : null,
            'promo_id' => $promo?->id,
            'items' => $items,
            'split' => [
                'platform_commission_centavos' => $platformCommission,
                'operator_commission_centavos' => $operatorCommission,
                'rider_earning_centavos' => $fee === null ? 0 : $fee->riderEarningCentavos,
                'operator_delivery_cut_centavos' => $fee === null ? 0 : $fee->operatorCutCentavos,
                'store_net_centavos' => $storeNet,
                'discount_owner' => $promo?->scope,
                'address_id' => $address?->id,
            ],
            'expires_at' => now()->addMinutes((int) config('foxdrop.ordering.quote_ttl_minutes')),
        ]);
    }

    /**
     * Each line re-priced from the catalog: base price plus the chosen options, which must exist and respect each
     * group's "required" and "max".
     *
     * @param  list<array{product_id: string, quantity: int, options?: list<array{group: string, name: string}>}>  $lines
     * @return list<array{product_id: string, name: string, unit_price_centavos: int, quantity: int, options: list<array{group: string, name: string, price_centavos: int}>, line_total_centavos: int}>
     */
    private function priceItems(Store $store, array $lines): array
    {
        $products = Product::query()->where('store_id', $store->id)->whereIn('id', array_column($lines, 'product_id'))->get()->keyBy('id');
        $wanted = [];
        $items = [];
        foreach ($lines as $index => $line) {
            $product = $products->get($line['product_id']) ?? self::fail("items.$index.product_id", 'This item is no longer on the menu.');
            if (! $product->isOrderable()) {
                self::fail("items.$index.product_id", "{$product->name} is not available right now.");
            }
            $wanted[$product->id] = ($wanted[$product->id] ?? 0) + $line['quantity'];
            if ($product->stock !== null && $wanted[$product->id] > $product->stock) {
                self::fail("items.$index.quantity", "Only {$product->stock} {$product->name} left.");
            }

            $chosen = $this->options($product, $line['options'] ?? [], $index);
            $unit = $product->price_centavos + array_sum(array_column($chosen, 'price_centavos'));
            $items[] = [
                'product_id' => $product->id,
                'name' => $product->name,
                'unit_price_centavos' => $unit,
                'quantity' => $line['quantity'],
                'options' => $chosen,
                'line_total_centavos' => $unit * $line['quantity'],
            ];
        }

        return $items;
    }

    /**
     * @param  list<array{group: string, name: string}>  $picked
     * @return list<array{group: string, name: string, price_centavos: int}>
     */
    private function options(Product $product, array $picked, int $index): array
    {
        $chosen = [];
        foreach ($product->options as $group) {
            $names = array_column(array_filter($picked, fn (array $o): bool => $o['group'] === $group['group']), 'name');
            if (count($names) > $group['max'] || ($group['required'] && $names === [])) {
                self::fail("items.$index.options", "Choose {$group['group']} for {$product->name}.");
            }
            foreach ($names as $name) {
                $choice = collect($group['choices'])->firstWhere('name', $name) ?? self::fail("items.$index.options", "{$name} is not an option for {$product->name}.");
                $chosen[] = ['group' => $group['group'], 'name' => $choice['name'], 'price_centavos' => (int) $choice['price_centavos']];
            }
        }
        if (count($chosen) !== count($picked)) {
            self::fail("items.$index.options", "Some options are not available for {$product->name}.");
        }

        return $chosen;
    }

    /**
     * Distance and fee to the customer's address, which must be theirs, inside a zone of the store's province and
     * within the card's maximum distance.
     *
     * @return array{0: float, 1: string, 2: FeeBreakdown}
     */
    private function delivery(Store $store, DeliveryRateCard $card, Address $address): array
    {
        if (! DeliveryZone::covers($store->province_id, $address->lat(), $address->lng())) {
            self::fail('address_id', 'This store does not deliver to that address.');
        }
        $distance = $this->distance->between(
            (float) $store->getAttribute('location_lat'), (float) $store->getAttribute('location_lng'), $address->lat(), $address->lng(),
        );
        $km = ceil(round($distance->km * 10, 6)) / 10;
        $fee = $this->fees->quote(new RateCardTerms(
            $card->base_fare_centavos, $card->base_km, $card->rate_per_km_centavos, $card->min_fee_centavos, $card->max_km, $card->rider_share_bps,
        ), $km) ?? self::fail('address_id', "That address is too far from {$store->name} for delivery.");

        return [$km, $distance->source, $fee];
    }

    private function promo(User $customer, Store $store, string $code, int $subtotal, string $fulfillment): Promo
    {
        $promo = Promo::query()
            ->where('code', $code)
            ->where('is_active', true)
            ->where(fn ($scope) => $scope->where('scope', 'PLATFORM')
                ->orWhere(fn ($province) => $province->where('scope', 'PROVINCE')->where('province_id', $store->province_id))
                ->orWhere(fn ($own) => $own->where('scope', 'STORE')->where('store_id', $store->id)))
            ->first() ?? self::fail('promo_code', 'This promo code does not work here.');

        if (now()->lt($promo->starts_at) || now()->gte($promo->ends_at)) {
            self::fail('promo_code', 'This promo code is not active right now.');
        }
        if ($subtotal < $promo->min_subtotal_centavos) {
            self::fail('promo_code', sprintf('This promo needs at least ₱%s worth of items.', number_format($promo->min_subtotal_centavos / 100, 2)));
        }
        if ($promo->type === Promo::FREE_DELIVERY && $fulfillment !== Order::DELIVERY) {
            self::fail('promo_code', 'This promo is for delivery orders.');
        }
        $uses = fn () => Order::query()->whereNot('status', Order::CANCELLED)->whereIn('quote_id', Quote::query()->where('promo_id', $promo->id)->select('id'));
        if ($promo->usage_limit !== null && $uses()->count() >= $promo->usage_limit) {
            self::fail('promo_code', 'This promo has been used up.');
        }
        if ($uses()->where('customer_id', $customer->id)->count() >= $promo->per_user_limit) {
            self::fail('promo_code', 'You have already used this promo.');
        }

        return $promo;
    }

    private function discount(Promo $promo, int $subtotal, int $deliveryFee): int
    {
        $discount = match ($promo->type) {
            Promo::PERCENT => Money::share($subtotal, $promo->value),
            Promo::FIXED => $promo->value,
            default => $deliveryFee, // FREE_DELIVERY
        };
        if ($promo->max_discount_centavos !== null) {
            $discount = min($discount, $promo->max_discount_centavos);
        }

        return min($discount, $subtotal + $deliveryFee);
    }

    /**
     * @throws ValidationException
     */
    private static function fail(string $field, string $message): never
    {
        throw ValidationException::withMessages([$field => $message]);
    }
}
