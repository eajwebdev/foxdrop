<?php

namespace App\Models;

use App\Models\Concerns\VisibleToViewer;
use Illuminate\Database\Eloquent\Attributes\Hidden;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;

/**
 * Orders are written only by server actions (place order, status transitions, dispatch); amounts are computed on the
 * server (§6). The pick-up code and delivery OTP never leave the server except to the customer who needs them.
 *
 * @property string $id
 * @property string $code
 * @property string $province_id
 * @property string $customer_id
 * @property string $store_id
 * @property string|null $rider_id
 */
#[Hidden(['pickup_code', 'delivery_otp'])]
class Order extends Model
{
    use HasUuids, VisibleToViewer;

    /**
     * Its customer, the store team, the assigned rider, the province's operator team, or platform admins.
     *
     * @param  Builder<static>  $query
     */
    protected function visibilityRule(Builder $query, User $user): void
    {
        $query->where('customer_id', $user->id)
            ->orWhereIn('store_id', $user->memberStoreIds())
            ->orWhereIn('rider_id', $user->riderIdsQuery())
            ->orWhereIn('province_id', $user->operatorProvinceIds());
    }
}
