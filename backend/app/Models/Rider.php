<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Hidden;
use Illuminate\Database\Eloquent\Attributes\Scope;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * @property string $id
 * @property string $user_id
 * @property string $province_id
 * @property string $status
 */
#[Hidden(['license_no_encrypted'])]
class Rider extends Model
{
    use HasUuids;

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
     * The rider themself, their province's operator team, or platform admins.
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
            ->where('user_id', $user->id)
            ->orWhereIn('province_id', $user->operatorProvinceIds()));
    }
}
