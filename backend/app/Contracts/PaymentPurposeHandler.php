<?php

namespace App\Contracts;

use App\Models\Payment;

/**
 * What a payment pays for: called once, inside the transaction that marks the payment PAID. Must be safe to repeat
 * for the same payment.
 */
interface PaymentPurposeHandler
{
    public function onPaid(Payment $payment): void;

    /**
     * The payment failed or expired (called once, after it is marked so).
     */
    public function onFailed(Payment $payment): void;
}
