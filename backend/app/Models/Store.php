<?php

namespace App\Models;

use App\Models\Concerns\HasStatusChanges;
use App\Models\Concerns\ReviewedByProvinceOperators;
use App\Models\Concerns\VisibleToViewer;
use Carbon\CarbonInterface;
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
 * @property int $prep_time_min
 * @property bool $accepts_cod
 * @property bool $accepts_pickup
 * @property array<string, list<array{0: string, 1: string}>> $opening_hours
 * @property float $rating_avg
 * @property int $rating_count
 * @property string|null $logo_path
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
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'prep_time_min' => 'integer',
            'accepts_cod' => 'boolean',
            'accepts_pickup' => 'boolean',
            'opening_hours' => 'array',
            'rating_avg' => 'float',
            'rating_count' => 'integer',
        ];
    }

    /**
     * Open for orders now: ACTIVE (not PAUSED), and inside today's opening hours in Asia/Manila. A store without
     * opening hours is open whenever it is ACTIVE.
     */
    public function isOpenAt(CarbonInterface $at): bool
    {
        if ($this->status !== self::ACTIVE) {
            return false;
        }
        $hours = $this->opening_hours;
        if ($hours === []) {
            return true;
        }
        $local = $at->copy()->setTimezone('Asia/Manila');
        $now = $local->format('H:i');
        foreach ($hours[strtolower($local->format('D'))] ?? [] as [$opens, $closes]) {
            if ($opens <= $now && $now < $closes) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return HasMany<ProductCategory, $this>
     */
    public function categories(): HasMany
    {
        return $this->hasMany(ProductCategory::class)->orderBy('sort_order')->orderBy('name');
    }

    /**
     * @return HasMany<Product, $this>
     */
    public function products(): HasMany
    {
        return $this->hasMany(Product::class)->orderBy('sort_order')->orderBy('name');
    }

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
