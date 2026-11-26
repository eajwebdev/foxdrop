<?php

namespace App\Models;

use App\Models\Concerns\HasStatusChanges;
use App\Models\Concerns\ReviewedByProvinceOperators;
use App\Models\Concerns\VisibleToViewer;
use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Attributes\Scope;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

/**
 * @property string $id
 * @property string $province_id
 * @property string $owner_user_id
 * @property string $name
 * @property string $status
 * @property string|null $rejection_reason
 * @property string|null $status_reason
 * @property string $category
 * @property string $address_line
 * @property string $city
 * @property Carbon|null $created_at
 */
#[Fillable([
    'province_id', 'owner_user_id', 'name', 'category', 'description', 'location', 'address_line', 'barangay', 'city', 'phone',
    'status', 'rejection_reason',
])]
class Store extends Model
{
    use HasStatusChanges, HasUuids, ReviewedByProvinceOperators, VisibleToViewer;

    public const string SUBMITTED = 'SUBMITTED';

    public const string ACTIVE = 'ACTIVE';

    public const string REJECTED = 'REJECTED';

    public const string SUSPENDED = 'SUSPENDED';

    public const string CLOSED = 'CLOSED';

    public const string PAUSED = 'PAUSED';

    /** Admin / operator changes of a live store: target status => statuses it may come from (DECISIONS 79). */
    public const array STATUS_CHANGES = [
        self::SUSPENDED => [self::ACTIVE, self::PAUSED],
        self::ACTIVE => [self::SUSPENDED],
        self::CLOSED => [self::ACTIVE, self::PAUSED, self::SUSPENDED],
    ];

    /** Lifting a suspension returns the store here. */
    public const string REINSTATED = self::ACTIVE;

    /** A storefront customers can see (§5.2). */
    public const array PUBLISHED_STATUSES = [self::ACTIVE, self::PAUSED];

    /** Statuses a reviewer may list (§9.4 step 4). */
    public const array REVIEW_FILTERS = [self::SUBMITTED, self::ACTIVE, self::PAUSED, self::SUSPENDED, self::REJECTED, self::CLOSED];

    /**
     * @return BelongsTo<Province, $this>
     */
    public function province(): BelongsTo
    {
        return $this->belongsTo(Province::class);
    }

    /**
     * All columns plus the location as latitude/longitude.
     *
     * @param  Builder<static>  $query
     */
    #[Scope]
    protected function withLocation(Builder $query): void
    {
        $query->select('stores.*')
            ->selectRaw('ST_Y(location::geometry) as location_lat, ST_X(location::geometry) as location_lng');
    }

    /**
     * @return BelongsTo<User, $this>
     */
    public function owner(): BelongsTo
    {
        return $this->belongsTo(User::class, 'owner_user_id');
    }

    /**
     * @return HasMany<StoreDocument, $this>
     */
    public function documents(): HasMany
    {
        return $this->hasMany(StoreDocument::class)->orderBy('doc_type');
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
