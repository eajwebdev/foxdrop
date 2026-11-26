<?php

namespace App\Models\Concerns;

use App\Models\User;
use Illuminate\Database\Eloquent\Attributes\Scope;
use Illuminate\Database\Eloquent\Builder;

/**
 * Province isolation for API reads (§0 rule 5): `Model::query()->visibleTo($user)` keeps the rows [user] may see.
 * Platform admins see everything; each model states its own rule in [visibilityRule]. Every API endpoint that reads a
 * provincial model goes through this scope (DECISIONS 38).
 */
trait VisibleToViewer
{
    /**
     * @param  Builder<static>  $query
     */
    abstract protected function visibilityRule(Builder $query, User $user): void;

    /**
     * @param  Builder<static>  $query
     */
    #[Scope]
    protected function visibleTo(Builder $query, User $user): void
    {
        if ($user->isPlatformAdmin()) {
            return;
        }
        $query->where(fn (Builder $rule) => $this->visibilityRule($rule, $user));
    }
}
