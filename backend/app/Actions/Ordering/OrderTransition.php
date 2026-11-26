<?php

namespace App\Actions\Ordering;

use App\Enums\AppRole;
use App\Models\Order;
use App\Models\Payment;
use App\Models\Product;
use App\Models\User;
use Illuminate\Support\Facades\DB;

/**
 * The order state machine (§5.4): the one place an order's status changes. It checks who may make each step,
 * locks the order row, stamps the milestone time and writes `order_events`. A cancellation gives back reserved stock
 * and refunds an online payment (DECISIONS 89, 90). Phase 3 covers everything up to the store's part; rider steps
 * arrive with dispatch (Phase 4).
 */
final class OrderTransition
{
    public const string CUSTOMER = 'customer';

    public const string STORE = 'store';

    public const string SYSTEM = 'system';

    public const string OPERATOR = 'operator';

    /** from => [to => who may do it] */
    private const array STEPS = [
        Order::PENDING_PAYMENT => [
            Order::PLACED => [self::SYSTEM],
            Order::CANCELLED => [self::SYSTEM, self::CUSTOMER],
        ],
        Order::PLACED => [
            Order::ACCEPTED => [self::STORE],
            Order::CANCELLED => [self::STORE, self::CUSTOMER, self::SYSTEM, self::OPERATOR],
        ],
        Order::ACCEPTED => [
            Order::PREPARING => [self::STORE],
            Order::READY_FOR_PICKUP => [self::STORE],
            Order::CANCELLED => [self::STORE, self::OPERATOR],
        ],
        Order::PREPARING => [
            Order::READY_FOR_PICKUP => [self::STORE],
            Order::CANCELLED => [self::STORE, self::OPERATOR],
        ],
        Order::READY_FOR_PICKUP => [
            Order::COMPLETED => [self::STORE], // pick-up orders, after the code check (CompletePickupOrder)
            Order::CANCELLED => [self::STORE, self::OPERATOR],
        ],
    ];

    /** The milestone column each status stamps. */
    private const array STAMPS = [
        Order::PLACED => 'placed_at',
        Order::ACCEPTED => 'accepted_at',
        Order::PREPARING => 'preparing_at',
        Order::READY_FOR_PICKUP => 'ready_at',
        Order::COMPLETED => 'completed_at',
        Order::CANCELLED => 'cancelled_at',
    ];

    public function __construct(private readonly RefundOrderPayment $refund) {}

    /**
     * Moves [order] to [to] as [as] (who the caller checked the actor is). Returns the updated order.
     *
     * @param  array{note?: string|null, prep_time_min?: int|null}  $details
     */
    public function __invoke(Order $order, string $to, ?User $actor, string $as, array $details = []): Order
    {
        $order = DB::transaction(function () use ($order, $to, $actor, $as, $details): Order {
            $order = Order::query()->lockForUpdate()->findOrFail($order->id);
            $from = $order->status;
            abort_unless(in_array($as, self::STEPS[$from][$to] ?? [], true), 409, self::refusal($from, $to));
            abort_if($to === Order::COMPLETED && $order->fulfillment !== Order::PICKUP, 409, 'Delivery orders are completed by the rider.');

            $changes = ['status' => $to, self::STAMPS[$to] => now()];
            if ($to === Order::ACCEPTED) {
                $prep = $details['prep_time_min'] ?? null;
                $changes['eta_at'] = now()->addMinutes(is_int($prep) && $prep > 0 ? $prep : $order->store()->value('prep_time_min'));
            }
            if ($to === Order::CANCELLED) {
                $changes['cancel_reason'] = $details['note'] ?? null;
                $this->releaseStock($order);
            }
            $order->forceFill($changes)->save();

            DB::table('order_events')->insert([
                'order_id' => $order->id,
                'province_id' => $order->province_id,
                'actor_user_id' => $actor?->id,
                'actor_role' => $actor === null ? null : $this->roleOf($actor, $order, $as)?->value,
                'from_status' => $from,
                'to_status' => $to,
                'note' => $details['note'] ?? null,
            ]);

            return $order;
        });

        if ($to === Order::CANCELLED && $order->payment_status === 'PAID') {
            ($this->refund)($order); // outside the lock: it calls the payment gateway
        }

        return $order->refresh();
    }

    /**
     * Gives back stock reserved when the order was placed (DECISIONS 90).
     */
    private function releaseStock(Order $order): void
    {
        $lines = DB::table('order_items')->where('order_id', $order->id)->whereNotNull('product_id')->get(['product_id', 'quantity']);
        foreach ($lines as $line) {
            Product::query()->whereKey($line->product_id)->whereNotNull('stock')->increment('stock', (int) $line->quantity);
        }
    }

    private function roleOf(User $actor, Order $order, string $as): ?AppRole
    {
        return match ($as) {
            self::CUSTOMER => AppRole::Customer,
            self::STORE => $actor->roles->first(fn ($grant) => $grant->store_id === $order->store_id)?->role,
            self::OPERATOR => $actor->isPlatformAdmin() ? $actor->roles->first(fn ($grant) => in_array($grant->role, AppRole::admin(), true))?->role : AppRole::Operator,
            default => null,
        };
    }

    private static function refusal(string $from, string $to): string
    {
        return match (true) {
            $from === Order::CANCELLED => 'This order was already cancelled.',
            $from === Order::COMPLETED => 'This order is already complete.',
            $to === Order::CANCELLED => 'This order can no longer be cancelled here.',
            default => "This order is {$from} and cannot be moved to {$to}.",
        };
    }

    /**
     * Statuses the store may move a pick-up or delivery order to from [status] (for the board's buttons).
     *
     * @return list<string>
     */
    public static function storeNextSteps(Order $order): array
    {
        $next = [];
        foreach (self::STEPS[$order->status] ?? [] as $to => $who) {
            if (in_array(self::STORE, $who, true) && ($to !== Order::COMPLETED || $order->fulfillment === Order::PICKUP)) {
                $next[] = $to;
            }
        }

        return $next;
    }

    /** The payment that a cancellation refunds, if any. */
    public static function paidPayment(Order $order): ?Payment
    {
        return Payment::query()->where('order_id', $order->id)->where('status', Payment::PAID)->latest('created_at')->first();
    }
}
