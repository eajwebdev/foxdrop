<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Carbon;

/**
 * The server's price for a cart (§6, DECISIONS 86): valid for 10 minutes and turned into at most one order.
 * `items` holds the priced lines; `split` the commissions, rider earning and operator cut.
 *
 * @property string $id
 * @property string $province_id
 * @property string $customer_id
 * @property string $store_id
 * @property string $fulfillment
 * @property float|null $distance_km
 * @property string|null $distance_source
 * @property int $item_subtotal_centavos
 * @property int $delivery_fee_centavos
 * @property int $discount_centavos
 * @property int $total_centavos
 * @property string|null $rate_card_id
 * @property array<string, mixed>|null $rate_card_snapshot
 * @property string|null $promo_id
 * @property list<array{product_id: string, name: string, unit_price_centavos: int, quantity: int, options: list<array{group: string, name: string, price_centavos: int}>, line_total_centavos: int}> $items
 * @property array{platform_commission_centavos: int, operator_commission_centavos: int, rider_earning_centavos: int, operator_delivery_cut_centavos: int, store_net_centavos: int, discount_owner?: string|null, address_id?: string|null}|null $split
 * @property array<string, mixed>|null $dropoff_snapshot
 * @property Carbon $expires_at
 * @property Carbon|null $used_at
 */
#[Fillable([
    'province_id', 'customer_id', 'store_id', 'fulfillment', 'dropoff_location', 'distance_km', 'distance_source',
    'item_subtotal_centavos', 'delivery_fee_centavos', 'discount_centavos', 'total_centavos', 'rate_card_id',
    'rate_card_snapshot', 'promo_id', 'items', 'split', 'expires_at',
])]
class Quote extends Model
{
    use HasUuids;

    public const null UPDATED_AT = null;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'distance_km' => 'float',
            'item_subtotal_centavos' => 'integer',
            'delivery_fee_centavos' => 'integer',
            'discount_centavos' => 'integer',
            'total_centavos' => 'integer',
            'rate_card_snapshot' => 'array',
            'items' => 'array',
            'split' => 'array',
            'expires_at' => 'datetime',
            'used_at' => 'datetime',
        ];
    }
}
