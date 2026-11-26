<?php

namespace App\Models;

use App\Models\Concerns\VisibleToViewer;
use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Support\Carbon;

/**
 * An operator's license for one province (§5.1). The database allows one live license per province.
 *
 * @property string $id
 * @property string $province_id
 * @property string $operator_user_id
 * @property string $status
 * @property Carbon|null $start_at
 * @property Carbon|null $end_at
 * @property int $fee_centavos
 * @property string|null $application_id
 */
#[Fillable(['province_id', 'operator_user_id', 'application_id', 'status', 'start_at', 'end_at', 'fee_centavos', 'payment_id'])]
class ProvinceLicense extends Model
{
    use HasUuids, VisibleToViewer;

    public const string APPLIED = 'APPLIED';

    public const string PENDING_PAYMENT = 'PENDING_PAYMENT';

    public const string ACTIVE = 'ACTIVE';

    public const string GRACE = 'GRACE';

    public const string EXPIRED = 'EXPIRED';

    public const string REJECTED = 'REJECTED';

    /** A payment is accepted in these states (first payment, or renewal). */
    public const array PAYABLE = [self::PENDING_PAYMENT, self::ACTIVE, self::GRACE];

    /** The operator runs the province (GRACE: operations continue, §5.1). */
    public const array OPERATING = [self::ACTIVE, self::GRACE];

    /** At most one license per province may be in these states (unique index province_licenses_one_live). */
    public const array LIVE = [self::APPLIED, self::PENDING_PAYMENT, self::ACTIVE, self::GRACE];

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return ['start_at' => 'datetime', 'end_at' => 'datetime', 'fee_centavos' => 'integer'];
    }

    /**
     * @return BelongsTo<Province, $this>
     */
    public function province(): BelongsTo
    {
        return $this->belongsTo(Province::class);
    }

    /**
     * Each province's current license: the live one if there is one, else the one that ended last (never a
     * rejected application). Keyed by province id; provinces without one are absent.
     *
     * @param  list<string>  $provinceIds
     * @return array<string, self>
     */
    public static function currentByProvince(array $provinceIds): array
    {
        if ($provinceIds === []) {
            return [];
        }

        $current = [];
        $licenses = static::query()
            ->whereIn('province_id', $provinceIds)
            ->where('status', '<>', self::REJECTED)
            ->orderByRaw('status in ('.implode(', ', array_map(fn (string $s): string => "'$s'", self::LIVE)).') desc')
            ->orderByDesc('end_at')
            ->orderByDesc('created_at')
            ->get();
        foreach ($licenses as $license) {
            $current[$license->province_id] ??= $license;
        }

        return $current;
    }

    /**
     * The license holder, its province's operator team, or platform admins.
     *
     * @param  Builder<static>  $query
     */
    protected function visibilityRule(Builder $query, User $user): void
    {
        $query->where('operator_user_id', $user->id)->orWhereIn('province_id', $user->operatorProvinceIds());
    }
}
