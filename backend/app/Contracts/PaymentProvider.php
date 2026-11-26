<?php

namespace App\Contracts;

use App\Models\Payment;
use App\Services\Payments\CheckoutSession;

/**
 * Creates a hosted checkout for a payment row (PayMongo in production, a local fake page in development).
 */
interface PaymentProvider
{
    /** The `payments.provider` value this driver writes. */
    public function name(): string;

    public function createCheckout(Payment $payment, string $description): CheckoutSession;
}
