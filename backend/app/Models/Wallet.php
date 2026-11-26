<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Scope;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;

/**
 * A ledger wallet (§7). The balance changes only through ledger entries (database trigger).
 *
 * @property string $id
 * @property string $owner_type
 * @property string|null $owner_id
 * @property string|null $province_id
 * @property string $kind
 * @property int $balance_centavos
 */
class Wallet extends Model
{
    use HasUuids;

    public const null UPDATED_AT = null;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return ['balance_centavos' => 'integer'];
    }

    /**
     * The wallet's store team or rider, its province's operator team, or platform admins.
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
            ->whereIn('province_id', $user->operatorProvinceIds())
            ->orWhere(fn (Builder $s) => $s->where('owner_type', 'STORE')->whereIn('owner_id', $user->memberStoreIds()))
            ->orWhere(fn (Builder $r) => $r->where('owner_type', 'RIDER')
                ->whereIn('owner_id', Rider::query()->where('user_id', $user->id)->select('id'))));
    }
}
