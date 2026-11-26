<?php

namespace App\Actions\Payments;

use App\Actions\Licensing\ActivateLicensePayment;
use App\Models\Payment;
use Illuminate\Support\Facades\DB;

/**
 * Finishes a payment once the provider (webhook, or the local fake checkout) reports the result. Safe to call more
 * than once: only a PENDING payment changes.
 */
final class SettlePayment
{
    public function __construct(private readonly ActivateLicensePayment $activateLicense) {}

    /**
     * @param  array<string, mixed>|null  $raw  the provider's event, kept for support
     */
    public function succeeded(Payment $payment, ?array $raw = null): Payment
    {
        return DB::transaction(function () use ($payment, $raw): Payment {
            $payment = Payment::query()->lockForUpdate()->findOrFail($payment->id);
            if ($payment->status !== Payment::PENDING) {
                return $payment;
            }

            $payment->forceFill(['status' => Payment::PAID, 'raw' => $raw])->save();
            if ($payment->purpose === Payment::PURPOSE_LICENSE) {
                ($this->activateLicense)($payment);
            }

            return $payment;
        });
    }

    /**
     * @param  array<string, mixed>|null  $raw
     */
    public function failed(Payment $payment, ?array $raw = null): Payment
    {
        return DB::transaction(function () use ($payment, $raw): Payment {
            $payment = Payment::query()->lockForUpdate()->findOrFail($payment->id);
            if ($payment->status === Payment::PENDING) {
                $payment->forceFill(['status' => Payment::FAILED, 'raw' => $raw])->save();
            }

            return $payment;
        });
    }
}
