<?php

namespace App\Models;

use App\Models\Concerns\HasStatusChanges;
use App\Models\Concerns\ReviewedByProvinceOperators;
use App\Models\Concerns\VisibleToViewer;
use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Attributes\Hidden;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Support\Carbon;

/**
 * @property string $id
 * @property string $user_id
 * @property string $province_id
 * @property string $status
 * @property string $vehicle_type
 * @property string|null $plate_no
 * @property string|null $license_no_last4
 * @property Carbon|null $license_expiry
 * @property string|null $rejection_reason
 * @property string|null $status_reason
 * @property Carbon|null $created_at
 */
#[Hidden(['license_no_encrypted'])]
#[Fillable([
    'user_id', 'province_id', 'vehicle_type', 'plate_no', 'license_no_encrypted', 'license_no_last4', 'license_expiry', 'status',
    'rejection_reason',
])]
class Rider extends Model
{
    use HasStatusChanges, HasUuids, ReviewedByProvinceOperators, VisibleToViewer;

    public const string DOCS_REVIEW = 'DOCS_REVIEW';

    public const string APPROVED = 'APPROVED';

    public const string REJECTED = 'REJECTED';

    public const string SUSPENDED = 'SUSPENDED';

    public const string DEACTIVATED = 'DEACTIVATED';

    /** Admin / operator changes of an approved rider: target status => statuses it may come from (DECISIONS 79). */
    public const array STATUS_CHANGES = [
        self::SUSPENDED => [self::APPROVED],
        self::APPROVED => [self::SUSPENDED],
        self::DEACTIVATED => [self::APPROVED, self::SUSPENDED],
    ];

    /** Lifting a suspension returns the rider here. */
    public const string REINSTATED = self::APPROVED;

    /** Statuses a reviewer may list (§9.4 step 4). */
    public const array REVIEW_FILTERS = [self::DOCS_REVIEW, self::APPROVED, self::REJECTED, self::SUSPENDED, self::DEACTIVATED];

    /** Upload field => rider_document_type. A bicycle needs no OR/CR or vehicle photo (SubmitRiderRequest). */
    public const array DOCUMENTS = [
        'valid_id' => 'VALID_ID', 'drivers_license' => 'DRIVERS_LICENSE', 'selfie' => 'SELFIE',
        'or_cr' => 'OR_CR', 'vehicle_photo' => 'VEHICLE_PHOTO',
    ];

    /**
     * The driver's license number is encrypted at rest with the app key (§17); only the last 4 are shown.
     *
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return ['license_no_encrypted' => 'encrypted', 'license_expiry' => 'date', 'approved_at' => 'datetime'];
    }

    /**
     * @return BelongsTo<User, $this>
     */
    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    /**
     * @return BelongsTo<Province, $this>
     */
    public function province(): BelongsTo
    {
        return $this->belongsTo(Province::class);
    }

    /**
     * @return HasMany<RiderDocument, $this>
     */
    public function documents(): HasMany
    {
        return $this->hasMany(RiderDocument::class)->orderBy('doc_type');
    }

    /**
     * The rider themself, their province's operator team, or platform admins.
     *
     * @param  Builder<static>  $query
     */
    protected function visibilityRule(Builder $query, User $user): void
    {
        $query->where('user_id', $user->id)->orWhereIn('province_id', $user->operatorProvinceIds());
    }
}
