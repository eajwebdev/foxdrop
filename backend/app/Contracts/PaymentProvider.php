<?php

namespace App\Contracts;

use App\Exceptions\PaymentProviderException;
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

    /**
     * Gives the whole amount of a PAID payment back to the payer (DECISIONS 89).
     *
     * @throws PaymentProviderException when the gateway refuses or cannot be reached
     */
    public function refund(Payment $payment, string $reason): void;
}
