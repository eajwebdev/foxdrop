<?php

namespace App\Actions\Ordering;

use App\Models\Order;
use App\Models\User;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

/**
 * The customer collects a pick-up order (§5.4): the store types the 4-digit code the customer shows. Five wrong
 * codes lock the order for 10 minutes (DECISIONS 87), so the code cannot be guessed at the counter.
 */
final class CompletePickupOrder
{
    public function __construct(private readonly OrderTransition $transition) {}

    public function __invoke(Order $order, User $staff, string $code): Order
    {
        $matches = DB::transaction(function () use ($order, $code): bool {
            $order = Order::query()->lockForUpdate()->findOrFail($order->id);
            abort_unless($order->fulfillment === Order::PICKUP && $order->status === Order::READY_FOR_PICKUP, 409, 'This order is not ready to be collected.');
            if ($order->pickup_code_locked_until?->isFuture()) {
                throw ValidationException::withMessages(['pickup_code' => 'Too many wrong codes. Try again after '.$order->pickup_code_locked_until->timezone('Asia/Manila')->format('g:i A').'.']);
            }
            if (hash_equals((string) $order->pickup_code, $code)) {
                $order->forceFill(['pickup_code_attempts' => 0, 'pickup_code_locked_until' => null])->save();

                return true;
            }

            $attempts = $order->pickup_code_attempts + 1;
            $locked = $attempts >= (int) config('foxdrop.ordering.pickup_code_attempts');
            $order->forceFill([
                'pickup_code_attempts' => $locked ? 0 : $attempts,
                'pickup_code_locked_until' => $locked ? now()->addMinutes((int) config('foxdrop.ordering.pickup_code_lock_minutes')) : null,
            ])->save();

            return false;
        });
        if (! $matches) {
            throw ValidationException::withMessages(['pickup_code' => 'That code is not right. Ask the customer to show the code in their FoxDrop app.']);
        }

        return ($this->transition)($order, Order::COMPLETED, $staff, OrderTransition::STORE, ['note' => 'Collected by the customer']);
    }
}
