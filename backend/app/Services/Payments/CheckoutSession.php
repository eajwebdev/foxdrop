<?php

namespace App\Services\Payments;

/**
 * A hosted checkout: the provider's id for it and the page the customer pays on.
 */
final readonly class CheckoutSession
{
    public function __construct(
        public string $intentId,
        public string $checkoutUrl,
    ) {}
}
