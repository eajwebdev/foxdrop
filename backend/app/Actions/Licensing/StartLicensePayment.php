<?php

namespace App\Actions\Licensing;

use App\Contracts\PaymentProvider;
use App\Models\Payment;
use App\Models\ProvinceLicense;
use App\Models\User;
use App\Support\PlatformSettings;
use Throwable;

/**
 * Starts paying the license fee (§9.4 steps 2 and 8): the first payment or a renewal. An open checkout is reused,
 * so tapping "Pay" twice does not create two payments.
 */
final class StartLicensePayment
{
    public function __construct(private readonly PaymentProvider $provider) {}

    public function __invoke(User $payer, ProvinceLicense $license): Payment
    {
        abort_unless($license->operator_user_id === $payer->id, 404);
        abort_unless(in_array($license->status, ProvinceLicense::PAYABLE, true), 409, 'This license cannot be paid now.');

        $open = Payment::query()
            ->where('license_id', $license->id)
            ->where('status', Payment::PENDING)
            ->where('expires_at', '>', now())
            ->whereNotNull('checkout_url')
            ->latest('created_at')
            ->first();
        if ($open !== null) {
            return $open;
        }

        $payment = Payment::query()->create([
            'province_id' => $license->province_id,
            'purpose' => Payment::PURPOSE_LICENSE,
            'provider' => $this->provider->name(),
            'amount_centavos' => PlatformSettings::licenseFeeCentavos(),
            'status' => Payment::PENDING,
            'payer_user_id' => $payer->id,
            'license_id' => $license->id,
            'expires_at' => now()->addMinutes((int) config('foxdrop.payments.checkout_ttl_minutes')),
        ]);

        try {
            $session = $this->provider->createCheckout($payment, 'FoxDrop province license (1 year)');
        } catch (Throwable $e) {
            // The gateway never opened a checkout: close this attempt so the next tap starts a fresh one.
            $payment->forceFill(['status' => Payment::FAILED])->save();

            throw $e;
        }
        $payment->forceFill(['intent_id' => $session->intentId, 'checkout_url' => $session->checkoutUrl])->save();

        return $payment;
    }
}
