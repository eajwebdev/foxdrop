<?php

namespace App\Models;

use App\Models\Concerns\VisibleToViewer;
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
    use HasUuids, VisibleToViewer;

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
     * @param  Builder<static>  $query
     */
    protected function visibilityRule(Builder $query, User $user): void
    {
        $query->whereIn('province_id', $user->operatorProvinceIds())
            ->orWhere(fn (Builder $store) => $store->where('owner_type', 'STORE')->whereIn('owner_id', $user->memberStoreIds()))
            ->orWhere(fn (Builder $rider) => $rider->where('owner_type', 'RIDER')->whereIn('owner_id', $user->riderIdsQuery()));
    }
}
