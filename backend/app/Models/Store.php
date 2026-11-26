<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Scope;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * @property string $id
 * @property string $province_id
 * @property string $owner_user_id
 * @property string $name
 * @property string $status
 */
class Store extends Model
{
    use HasUuids;

    /** A storefront customers can see (§5.2). */
    public const array PUBLISHED_STATUSES = ['ACTIVE', 'PAUSED'];

    /**
     * @return BelongsTo<Province, $this>
     */
    public function province(): BelongsTo
    {
        return $this->belongsTo(Province::class);
    }

    /**
     * Published stores in a serviceable province (§9.1 browsing, also for signed-out visitors).
     *
     * @param  Builder<self>  $query
     */
    #[Scope]
    protected function published(Builder $query): void
    {
        $query->whereIn('status', self::PUBLISHED_STATUSES)
            ->whereIn('province_id', Province::query()
                ->where('is_enabled', true)
                ->whereHas('licenses', fn (Builder $l) => $l->whereIn('status', ['ACTIVE', 'GRACE']))
                ->select('id'));
    }

    /**
     * Every store a team member, the province's operators, or a platform admin may manage, plus published stores.
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
            ->whereIn('id', $user->memberStoreIds())
            ->orWhereIn('province_id', $user->operatorProvinceIds())
            ->orWhere(fn (Builder $p) => $p->published()));
    }
}
