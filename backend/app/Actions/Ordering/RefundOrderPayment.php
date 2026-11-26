<?php

namespace App\Actions\Ordering;

use App\Contracts\PaymentProvider;
use App\Exceptions\PaymentProviderException;
use App\Models\Order;
use App\Models\Payment;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

/**
 * Gives an online payment back when its order is cancelled (§5.4 "auto refund if online", DECISIONS 89). Online
 * orders have no ledger entries before they complete (§7 rule 1), so the refund is recorded on `payments` and the
 * order only. If the gateway cannot refund right now the order stays PAID and the failure is logged for support.
 */
final class RefundOrderPayment
{
    public function __construct(private readonly PaymentProvider $provider) {}

    public function __invoke(Order $order): bool
    {
        $payment = OrderTransition::paidPayment($order);
        if ($payment === null) {
            return false;
        }

        try {
            $this->provider->refund($payment, "Order {$order->code} was cancelled: ".($order->cancel_reason ?? 'no reason given'));
        } catch (PaymentProviderException $e) {
            Log::error('Order refund failed; it needs a manual refund', ['order' => $order->id, 'payment' => $payment->id, 'error' => $e->getMessage()]);

            return false;
        }

        DB::transaction(function () use ($order, $payment): void {
            $payment->forceFill(['status' => Payment::REFUNDED, 'refunded_at' => now()])->save();
            $order->forceFill(['payment_status' => 'REFUNDED'])->save();
        });

        return true;
    }
}
