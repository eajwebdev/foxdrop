<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasOne;
use Illuminate\Support\Carbon;

/**
 * A request to become the licensed operator of one province (§5.1, §9.4 step 1).
 *
 * @property string $id
 * @property string $applicant_user_id
 * @property string $province_id
 * @property string $business_name
 * @property string $status
 * @property string|null $rejection_reason
 * @property string|null $registration_number
 * @property string $contact_phone
 * @property string|null $contact_email
 * @property string|null $valid_id_path
 * @property string|null $business_permit_path
 * @property string|null $reviewed_by
 * @property Carbon|null $reviewed_at
 * @property Carbon|null $created_at
 */
#[Fillable(['applicant_user_id', 'province_id', 'business_name', 'registration_number', 'contact_phone', 'contact_email', 'valid_id_path', 'business_permit_path', 'status'])]
class OperatorApplication extends Model
{
    use HasUuids;

    public const string SUBMITTED = 'SUBMITTED';

    public const string APPROVED = 'APPROVED';

    public const string REJECTED = 'REJECTED';

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return ['reviewed_at' => 'datetime'];
    }

    /**
     * @return BelongsTo<Province, $this>
     */
    public function province(): BelongsTo
    {
        return $this->belongsTo(Province::class);
    }

    /**
     * @return BelongsTo<User, $this>
     */
    public function applicant(): BelongsTo
    {
        return $this->belongsTo(User::class, 'applicant_user_id');
    }

    /**
     * @return HasOne<ProvinceLicense, $this>
     */
    public function license(): HasOne
    {
        return $this->hasOne(ProvinceLicense::class, 'application_id');
    }
}
