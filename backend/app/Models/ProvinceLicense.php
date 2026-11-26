<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Scope;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * An operator's license for one province (§5.1). The database allows one live license per province.
 *
 * @property string $id
 * @property string $province_id
 * @property string $operator_user_id
 * @property string $status
 */
class ProvinceLicense extends Model
{
    use HasUuids;

    /** Statuses that block another license for the same province. */
    public const array LIVE_STATUSES = ['APPLIED', 'PENDING_PAYMENT', 'ACTIVE', 'GRACE'];

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
     * The license holder, its province's operator team, or platform admins.
     *
     * @param  Builder<self>  $query
     */
    #[Scope]
    protected function visibleTo(Builder $query, User $user): void
    {
        if ($user->isPlatformAdmin()) {
            return;
        }
        $query->where(fn (Builder $q) => $q
            ->where('operator_user_id', $user->id)
            ->orWhereIn('province_id', $user->operatorProvinceIds()));
    }
}
