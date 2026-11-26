<?php

namespace App\Services\Payments;

use App\Contracts\PaymentProvider;
use App\Models\Payment;

/**
 * Local development and tests only (DECISIONS 57): the checkout is a page on this server with Pay and Cancel
 * buttons that run the same handlers as the PayMongo webhook. Production refuses to boot with this driver.
 */
final class FakeCheckoutProvider implements PaymentProvider
{
    public function name(): string
    {
        return 'fake';
    }

    public function createCheckout(Payment $payment, string $description): CheckoutSession
    {
        return new CheckoutSession('fake_'.$payment->id, route('dev.checkout.show', $payment));
    }

    public function refund(Payment $payment, string $reason): void
    {
        // Nothing to call: the local fake checkout never took real money.
    }
}
