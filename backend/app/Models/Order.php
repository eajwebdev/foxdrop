<?php

namespace App\Models;

use App\Models\Concerns\VisibleToViewer;
use Illuminate\Database\Eloquent\Attributes\Hidden;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;
use Illuminate\Support\Carbon;

/**
 * Orders are written only by server actions (PlaceOrder, OrderTransition, dispatch); amounts are computed on the
 * server (§6). The pick-up code and delivery OTP never leave the server except to the customer who needs them.
 *
 * @property string $id
 * @property string $code
 * @property string $province_id
 * @property string $customer_id
 * @property string $store_id
 * @property string|null $rider_id
 * @property string|null $quote_id
 * @property string $fulfillment
 * @property string $payment_method
 * @property string $payment_status
 * @property string $status
 * @property int $item_subtotal_centavos
 * @property int $delivery_fee_centavos
 * @property int $discount_centavos
 * @property int $total_centavos
 * @property int $platform_commission_centavos
 * @property int $operator_commission_centavos
 * @property int $rider_earning_centavos
 * @property int $operator_delivery_cut_centavos
 * @property int $store_net_centavos
 * @property float|null $distance_km
 * @property array<string, mixed>|null $dropoff_snapshot
 * @property string|null $pickup_code
 * @property string|null $delivery_otp
 * @property int $pickup_code_attempts
 * @property Carbon|null $pickup_code_locked_until
 * @property string|null $notes
 * @property Carbon|null $eta_at
 * @property Carbon|null $placed_at
 * @property Carbon|null $accepted_at
 * @property Carbon|null $preparing_at
 * @property Carbon|null $ready_at
 * @property Carbon|null $completed_at
 * @property Carbon|null $cancelled_at
 * @property string|null $cancel_reason
 * @property Carbon|null $created_at
 */
#[Hidden(['pickup_code', 'delivery_otp'])]
class Order extends Model
{
    use HasUuids, VisibleToViewer;

    public const string PENDING_PAYMENT = 'PENDING_PAYMENT';

    public const string PLACED = 'PLACED';

    public const string ACCEPTED = 'ACCEPTED';

    public const string PREPARING = 'PREPARING';

    public const string READY_FOR_PICKUP = 'READY_FOR_PICKUP';

    public const string COMPLETED = 'COMPLETED';

    public const string CANCELLED = 'CANCELLED';

    public const string DELIVERY = 'DELIVERY';

    public const string PICKUP = 'PICKUP';

    public const string COD = 'COD';

    public const string ONLINE = 'ONLINE';

    /** Still going: not COMPLETED, CANCELLED or DISPUTED. */
    public const array ACTIVE_STATUSES = [
        self::PENDING_PAYMENT, self::PLACED, self::ACCEPTED, self::PREPARING, self::READY_FOR_PICKUP,
        'RIDER_ASSIGNED', 'RIDER_AT_STORE', 'PICKED_UP', 'ARRIVING', 'DELIVERED',
    ];

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'item_subtotal_centavos' => 'integer',
            'delivery_fee_centavos' => 'integer',
            'discount_centavos' => 'integer',
            'total_centavos' => 'integer',
            'platform_commission_centavos' => 'integer',
            'operator_commission_centavos' => 'integer',
            'rider_earning_centavos' => 'integer',
            'operator_delivery_cut_centavos' => 'integer',
            'store_net_centavos' => 'integer',
            'distance_km' => 'float',
            'dropoff_snapshot' => 'array',
            'rate_card_snapshot' => 'array',
            'pickup_code_attempts' => 'integer',
            'pickup_code_locked_until' => 'datetime',
            'eta_at' => 'datetime',
            'placed_at' => 'datetime',
            'accepted_at' => 'datetime',
            'preparing_at' => 'datetime',
            'ready_at' => 'datetime',
            'rider_assigned_at' => 'datetime',
            'picked_up_at' => 'datetime',
            'delivered_at' => 'datetime',
            'completed_at' => 'datetime',
            'cancelled_at' => 'datetime',
        ];
    }

    /**
     * @return BelongsTo<Store, $this>
     */
    public function store(): BelongsTo
    {
        return $this->belongsTo(Store::class);
    }

    /**
     * @return BelongsTo<User, $this>
     */
    public function customer(): BelongsTo
    {
        return $this->belongsTo(User::class, 'customer_id');
    }

    /**
     * @return HasMany<OrderItem, $this>
     */
    public function items(): HasMany
    {
        return $this->hasMany(OrderItem::class)->orderBy('name');
    }

    /**
     * @return HasMany<OrderEvent, $this>
     */
    public function events(): HasMany
    {
        return $this->hasMany(OrderEvent::class)->orderBy('created_at')->orderBy('id');
    }

    /**
     * The latest online payment for this order.
     *
     * @return HasOne<Payment, $this>
     */
    public function payment(): HasOne
    {
        return $this->hasOne(Payment::class)->latestOfMany('created_at');
    }

    public function isActive(): bool
    {
        return in_array($this->status, self::ACTIVE_STATUSES, true);
    }

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
