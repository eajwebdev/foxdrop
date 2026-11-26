<?php

namespace App\Models;

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
 * @property Carbon|null $created_at
 */
#[Hidden(['license_no_encrypted'])]
#[Fillable([
    'user_id', 'province_id', 'vehicle_type', 'plate_no', 'license_no_encrypted', 'license_no_last4', 'license_expiry', 'status',
    'rejection_reason',
])]
class Rider extends Model
{
    use HasUuids, ReviewedByProvinceOperators, VisibleToViewer;

    public const string DOCS_REVIEW = 'DOCS_REVIEW';

    public const string APPROVED = 'APPROVED';

    public const string REJECTED = 'REJECTED';

    /** Documents every rider submits; a bicycle needs no OR/CR or vehicle photo. */
    public const array REQUIRED_DOCUMENTS = ['VALID_ID', 'DRIVERS_LICENSE', 'SELFIE'];

    public const array MOTOR_VEHICLE_DOCUMENTS = ['OR_CR', 'VEHICLE_PHOTO'];

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
