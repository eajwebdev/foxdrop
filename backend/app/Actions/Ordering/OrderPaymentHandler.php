<?php

namespace App\Actions\Ordering;

use App\Contracts\PaymentPurposeHandler;
use App\Models\Order;
use App\Models\Payment;
use Illuminate\Support\Facades\DB;

/**
 * What an order's online payment means (§5.4): paid → the order is PLACED and the store's alarm rings; failed →
 * the order is cancelled. A payment that arrives after the order was already cancelled (too late) is refunded.
 * Runs inside SettlePayment's transaction; safe to repeat.
 */
final class OrderPaymentHandler implements PaymentPurposeHandler
{
    public function __construct(private readonly OrderTransition $transition) {}

    public function onPaid(Payment $payment): void
    {
        $order = Order::query()->lockForUpdate()->findOrFail($payment->order_id);
        $order->forceFill(['payment_status' => 'PAID'])->save();

        if ($order->status === Order::PENDING_PAYMENT) {
            ($this->transition)($order, Order::PLACED, null, OrderTransition::SYSTEM, ['note' => 'Payment received']);
        } elseif ($order->status === Order::CANCELLED) {
            DB::afterCommit(fn () => app(RefundOrderPayment::class)($order->refresh()));
        }
    }

    public function onFailed(Payment $payment): void
    {
        $order = Order::query()->findOrFail($payment->order_id);
        $order->forceFill(['payment_status' => 'FAILED'])->save();
        if ($order->status === Order::PENDING_PAYMENT) {
            ($this->transition)($order, Order::CANCELLED, null, OrderTransition::SYSTEM, ['note' => 'The payment did not go through.']);
        }
    }
}
