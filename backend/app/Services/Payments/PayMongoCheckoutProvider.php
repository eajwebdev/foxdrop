<?php

namespace App\Services\Payments;

use App\Contracts\PaymentProvider;
use App\Exceptions\PaymentProviderException;
use App\Models\Payment;
use Illuminate\Http\Client\ConnectionException;
use Illuminate\Http\Client\RequestException;
use Illuminate\Support\Facades\Http;

/**
 * PayMongo Checkout Sessions (GCash, Maya, cards). The session's metadata carries our payment id; the webhook
 * `checkout_session.payment.paid` completes the payment.
 */
final class PayMongoCheckoutProvider implements PaymentProvider
{
    public function name(): string
    {
        return 'paymongo';
    }

    public function createCheckout(Payment $payment, string $description): CheckoutSession
    {
        $secret = (string) config('services.paymongo.secret_key');
        if ($secret === '') {
            throw PaymentProviderException::notConfigured();
        }

        try {
            $session = Http::withBasicAuth($secret, '')
                ->acceptJson()
                ->timeout(15)
                ->post(config('services.paymongo.base_url').'/checkout_sessions', [
                    'data' => ['attributes' => [
                        'line_items' => [[
                            'name' => $description,
                            'amount' => $payment->amount_centavos,
                            'currency' => 'PHP',
                            'quantity' => 1,
                        ]],
                        'payment_method_types' => ['gcash', 'paymaya', 'card'],
                        'description' => $description,
                        'metadata' => ['payment_id' => $payment->id],
                        'success_url' => route('payments.return', ['result' => 'success']),
                        'cancel_url' => route('payments.return', ['result' => 'cancelled']),
                        'send_email_receipt' => false,
                    ]],
                ])
                ->throw()
                ->json('data');
        } catch (ConnectionException|RequestException) {
            throw PaymentProviderException::unavailable();
        }

        $id = $session['id'] ?? null;
        $url = $session['attributes']['checkout_url'] ?? null;
        if (! is_string($id) || ! is_string($url)) {
            throw PaymentProviderException::unavailable();
        }

        return new CheckoutSession($id, $url);
    }
}
