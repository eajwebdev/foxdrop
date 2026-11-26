<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Hidden;
use Illuminate\Database\Eloquent\Attributes\Scope;
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
    use HasUuids;

    /**
     * Its customer, the store team, the assigned rider, the province's operator team, or platform admins.
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
            ->where('customer_id', $user->id)
            ->orWhereIn('store_id', $user->memberStoreIds())
            ->orWhereIn('rider_id', Rider::query()->where('user_id', $user->id)->select('id'))
            ->orWhereIn('province_id', $user->operatorProvinceIds()));
    }
}
