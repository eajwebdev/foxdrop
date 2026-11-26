<?php

namespace App\Actions\Payments;

use App\Actions\Licensing\ActivateLicensePayment;
use App\Actions\Ordering\OrderPaymentHandler;
use App\Contracts\PaymentPurposeHandler;
use App\Models\Payment;
use Illuminate\Contracts\Container\Container;
use Illuminate\Support\Facades\DB;
use LogicException;

/**
 * Finishes a payment once the provider (webhook, or the local fake checkout) reports the result. Safe to call more
 * than once: only a PENDING payment changes.
 */
final class SettlePayment
{
    /** @var array<string, class-string<PaymentPurposeHandler>> what each payment purpose pays for */
    private const array HANDLERS = [
        Payment::PURPOSE_LICENSE => ActivateLicensePayment::class,
        Payment::PURPOSE_ORDER => OrderPaymentHandler::class,
    ];

    public function __construct(private readonly Container $container) {}

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
            $this->handlerFor($payment->purpose)->onPaid($payment);

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
                $this->handlerFor($payment->purpose)->onFailed($payment);
            }

            return $payment;
        });
    }

    /**
     * An unknown purpose rolls the PAID mark back: the provider retries, and nothing is settled half-way.
     */
    private function handlerFor(string $purpose): PaymentPurposeHandler
    {
        $handler = self::HANDLERS[$purpose] ?? throw new LogicException("No handler for {$purpose} payments.");

        return $this->container->make($handler);
    }
}
