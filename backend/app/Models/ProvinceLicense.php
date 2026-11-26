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
 * @property string|null $status_reason
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

    public const string SUSPENDED = 'SUSPENDED';

    /** A payment is accepted in these states (first payment, or renewal). */
    public const array PAYABLE = [self::PENDING_PAYMENT, self::ACTIVE, self::GRACE];

    /** The operator runs the province (GRACE: operations continue, §5.1). */
    public const array OPERATING = [self::ACTIVE, self::GRACE];

    /** At most one license per province may be in these states (unique index province_licenses_one_live, DECISIONS 77). */
    public const array LIVE = [self::APPLIED, self::PENDING_PAYMENT, self::ACTIVE, self::GRACE, self::SUSPENDED];

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return ['start_at' => 'datetime', 'end_at' => 'datetime', 'fee_centavos' => 'integer'];
    }

    /**
     * The running status the term allows today (§5.1): ACTIVE until `end_at`, then GRACE for [graceDays], then
     * EXPIRED. The one place this rule lives: the daily job and lifting a suspension both use it.
     */
    public function statusForTerm(int $graceDays): string
    {
        $endAt = $this->end_at ?? now();

        return match (true) {
            $endAt->isFuture() => self::ACTIVE,
            $endAt->copy()->addDays($graceDays)->isFuture() => self::GRACE,
            default => self::EXPIRED,
        };
    }

    /**
     * Whether [userId] already holds a live license (or an application) for any province: one province per operator.
     */
    public static function heldLiveBy(string $userId): bool
    {
        return static::query()->where('operator_user_id', $userId)->whereIn('status', self::LIVE)->exists();
    }

    /**
     * What an admin may do with this license now (the admin web shows only these buttons; the Actions check again).
     *
     * @return array{can_pay: bool, can_suspend: bool, can_reinstate: bool}
     */
    public function adminChanges(): array
    {
        return [
            'can_pay' => in_array($this->status, self::PAYABLE, true),
            'can_suspend' => in_array($this->status, self::OPERATING, true),
            'can_reinstate' => $this->status === self::SUSPENDED,
        ];
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
