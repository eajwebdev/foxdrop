<?php

namespace App\Models\Concerns;

use App\Models\User;
use Illuminate\Database\Eloquent\Attributes\Scope;
use Illuminate\Database\Eloquent\Builder;

/**
 * Partner applications (stores, riders) are reviewed by the operator team of their province, or by a platform
 * admin for any province (override, MASTER_PROMPT §9.5 step 4).
 *
 * @property string $province_id
 */
trait ReviewedByProvinceOperators
{
    public function isReviewableBy(User $user): bool
    {
        return $user->isPlatformAdmin() || in_array($this->province_id, $user->operatorProvinceIds(), true);
    }

    /**
     * @param  Builder<static>  $query
     */
    #[Scope]
    protected function reviewableBy(Builder $query, User $user): void
    {
        if (! $user->isPlatformAdmin()) {
            $query->whereIn('province_id', $user->operatorProvinceIds());
        }
    }
}
