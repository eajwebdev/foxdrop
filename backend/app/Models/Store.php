<?php

namespace App\Models;

use App\Models\Concerns\VisibleToViewer;
use Illuminate\Database\Eloquent\Attributes\Scope;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Support\Facades\DB;

/**
 * @property string $id
 * @property string $province_id
 * @property string $owner_user_id
 * @property string $name
 * @property string $status
 */
class Store extends Model
{
    use HasUuids, VisibleToViewer;

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
        // "Serviceable" is defined once, in the province_directory view (05_licensing.sql).
        $query->whereIn('status', self::PUBLISHED_STATUSES)
            ->whereIn('province_id', DB::table('province_directory')->where('is_serviceable', true)->select('id'));
    }

    /**
     * Every store a team member, the province's operators, or a platform admin may manage, plus published stores.
     *
     * @param  Builder<static>  $query
     */
    protected function visibilityRule(Builder $query, User $user): void
    {
        $query->whereIn('id', $user->memberStoreIds())
            ->orWhereIn('province_id', $user->operatorProvinceIds())
            ->orWhere(fn (Builder $published) => $published->published());
    }
}
