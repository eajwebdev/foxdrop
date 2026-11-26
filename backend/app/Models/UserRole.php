<?php

namespace App\Models;

use App\Enums\AppRole;
use App\Models\Concerns\VisibleToViewer;
use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * One role a user holds, optionally scoped to a province or a store (§5). The database checks that each role has the
 * right scope and that a store role points at a store in the same province.
 *
 * @property string $id
 * @property string $user_id
 * @property AppRole $role
 * @property string|null $province_id
 * @property string|null $store_id
 */
#[Fillable(['user_id', 'role', 'province_id', 'store_id', 'granted_by'])]
class UserRole extends Model
{
    use HasUuids, VisibleToViewer;

    public const null UPDATED_AT = null;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return ['role' => AppRole::class];
    }

    /**
     * @return BelongsTo<User, $this>
     */
    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    /**
     * Own grants, grants inside the operator's provinces, or everything for platform admins.
     *
     * @param  Builder<static>  $query
     */
    protected function visibilityRule(Builder $query, User $user): void
    {
        $query->where('user_id', $user->id)->orWhereIn('province_id', $user->operatorProvinceIds());
    }
}
