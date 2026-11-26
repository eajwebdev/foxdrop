<?php

namespace App\Actions\Licensing;

use App\Actions\Audit\RecordAudit;
use App\Actions\Payments\SettlePayment;
use App\Models\Payment;
use App\Models\ProvinceLicense;
use App\Models\User;
use App\Support\PlatformSettings;
use Illuminate\Database\UniqueConstraintViolationException;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Gate;

/**
 * The admin records a license fee paid outside the gateway (bank transfer, cash; §20.4 "record license payments",
 * DECISIONS 76). It becomes a `manual` payment that settles through the same SettlePayment → ActivateLicensePayment
 * path as PayMongo: first payment activates, a later one renews, and ledger rule 7 posts the fee.
 */
final class RecordOfflineLicensePayment
{
    public const array METHODS = ['BANK_TRANSFER', 'CASH', 'OTHER'];

    public function __construct(
        private readonly SettlePayment $settle,
        private readonly RecordAudit $audit,
    ) {}

    public function __invoke(User $admin, ProvinceLicense $license, string $method, string $reference, ?string $note = null): Payment
    {
        Gate::forUser($admin)->authorize('manage', $license);

        return DB::transaction(function () use ($admin, $license, $method, $reference, $note): Payment {
            $license = ProvinceLicense::query()->lockForUpdate()->findOrFail($license->id);
            abort_unless(in_array($license->status, ProvinceLicense::PAYABLE, true), 409, 'This license cannot take a payment now.');
            // An open online checkout could still be paid and would then count as a second term: wait for it to end.
            $openCheckout = Payment::query()
                ->where('license_id', $license->id)
                ->where('status', Payment::PENDING)
                ->where('provider', '<>', Payment::PROVIDER_MANUAL)
                ->where('expires_at', '>', now())
                ->orderByDesc('expires_at')
                ->first();
            abort_if($openCheckout !== null, 409, 'The operator has an online checkout open until '
                .$openCheckout?->expires_at?->timezone('Asia/Manila')->format('g:i A').'. Record the payment after that.');

            try {
                $payment = Payment::query()->create([
                    'province_id' => $license->province_id,
                    'purpose' => Payment::PURPOSE_LICENSE,
                    'provider' => Payment::PROVIDER_MANUAL,
                    'intent_id' => $reference, // unique per provider: one transfer is recorded once
                    'amount_centavos' => PlatformSettings::licenseFeeCentavos(),
                    'status' => Payment::PENDING,
                    'payer_user_id' => $license->operator_user_id,
                    'license_id' => $license->id,
                ]);
            } catch (UniqueConstraintViolationException) {
                abort(409, 'A payment with this reference was already recorded.');
            }

            $payment = $this->settle->succeeded($payment, ['method' => $method, 'reference' => $reference, 'note' => $note, 'recorded_by' => $admin->id]);

            ($this->audit)($admin, 'license.offline_payment_recorded', 'province_license', $license->id, $license->province_id,
                before: ['status' => $license->status, 'end_at' => $license->end_at?->toIso8601String()],
                after: ['payment_id' => $payment->id, 'method' => $method, 'reference' => $reference, 'amount_centavos' => $payment->amount_centavos]);

            return $payment;
        });
    }
}
