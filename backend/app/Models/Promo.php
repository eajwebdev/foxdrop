<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Carbon;

/**
 * A promo code ("Exclusive Promos & Deals", §9.1). Its owner pays the discount (§6, §7 rule 3): a STORE promo comes
 * out of the store's net, a PROVINCE promo out of the operator's revenue, a PLATFORM promo out of FoxDrop's.
 *
 * @property string $id
 * @property string $code
 * @property string $scope
 * @property string|null $province_id
 * @property string|null $store_id
 * @property string $type
 * @property int $value
 * @property int|null $max_discount_centavos
 * @property int $min_subtotal_centavos
 * @property int|null $usage_limit
 * @property int $per_user_limit
 * @property Carbon $starts_at
 * @property Carbon $ends_at
 * @property bool $is_active
 */
class Promo extends Model
{
    use HasUuids;

    public const string PERCENT = 'PERCENT';

    public const string FIXED = 'FIXED';

    public const string FREE_DELIVERY = 'FREE_DELIVERY';

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'value' => 'integer',
            'max_discount_centavos' => 'integer',
            'min_subtotal_centavos' => 'integer',
            'usage_limit' => 'integer',
            'per_user_limit' => 'integer',
            'starts_at' => 'datetime',
            'ends_at' => 'datetime',
            'is_active' => 'boolean',
        ];
    }
}
