<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Carbon;

/**
 * A version of a province's delivery prices (§6). Never edited: a change is a new row (database trigger).
 *
 * @property string $id
 * @property string $province_id
 * @property int $base_fare_centavos
 * @property float $base_km
 * @property int $rate_per_km_centavos
 * @property int $min_fee_centavos
 * @property float $max_km
 * @property int $rider_share_bps
 * @property int $night_surcharge_centavos
 * @property int $bad_weather_surcharge_centavos
 * @property int $extra_stop_centavos
 * @property Carbon|null $effective_from
 */
#[Fillable([
    'province_id', 'effective_from', 'created_by', 'base_fare_centavos', 'base_km', 'rate_per_km_centavos', 'min_fee_centavos',
    'max_km', 'rider_share_bps', 'night_surcharge_centavos', 'bad_weather_surcharge_centavos', 'extra_stop_centavos',
])]
class DeliveryRateCard extends Model
{
    use HasUuids;

    /** The fields an operator sets. */
    public const array TERMS = [
        'base_fare_centavos', 'base_km', 'rate_per_km_centavos', 'min_fee_centavos', 'max_km', 'rider_share_bps',
        'night_surcharge_centavos', 'bad_weather_surcharge_centavos', 'extra_stop_centavos',
    ];

    /** Platform defaults (§14) for a province that has never published a card. */
    public const array DEFAULTS = [
        'base_fare_centavos' => 4000, 'base_km' => 2.0, 'rate_per_km_centavos' => 1000, 'min_fee_centavos' => 4000,
        'max_km' => 15.0, 'rider_share_bps' => 8000, 'night_surcharge_centavos' => 0,
        'bad_weather_surcharge_centavos' => 0, 'extra_stop_centavos' => 0,
    ];

    public const null UPDATED_AT = null;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'base_fare_centavos' => 'integer', 'base_km' => 'float', 'rate_per_km_centavos' => 'integer',
            'min_fee_centavos' => 'integer', 'max_km' => 'float', 'rider_share_bps' => 'integer',
            'night_surcharge_centavos' => 'integer', 'bad_weather_surcharge_centavos' => 'integer',
            'extra_stop_centavos' => 'integer', 'effective_from' => 'datetime',
        ];
    }

    /**
     * The card in effect now for [provinceId], or an unsaved card with the platform defaults.
     */
    public static function currentFor(string $provinceId): self
    {
        return self::query()
            ->where('province_id', $provinceId)
            ->where('effective_from', '<=', now())
            ->orderByDesc('effective_from')
            ->orderByDesc('created_at')
            ->first()
            ?? new self(self::DEFAULTS + ['province_id' => $provinceId]);
    }
}
