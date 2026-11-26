<?php

namespace App\Actions\Licensing;

use App\Actions\Audit\RecordAudit;
use App\Actions\Ledger\EnsureWallet;
use App\Actions\Ledger\LedgerLine;
use App\Actions\Ledger\PostLedgerTxn;
use App\Actions\Roles\GrantRole;
use App\Contracts\PaymentPurposeHandler;
use App\Enums\AppRole;
use App\Models\Payment;
use App\Models\ProvinceLicense;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

/**
 * A license payment went through (§5.1, DECISIONS 55–56). First payment: the license becomes ACTIVE for a year,
 * the payer becomes the province's operator and gets operator wallets. Renewal: the same license is extended by a
 * year from the later of today and its end date. Ledger rule 7 records the fee as platform revenue.
 * Runs inside the payment's transaction; safe to repeat for the same payment.
 */
final class ActivateLicensePayment implements PaymentPurposeHandler
{
    public function __construct(
        private readonly GrantRole $grantRole,
        private readonly EnsureWallet $wallet,
        private readonly PostLedgerTxn $postLedger,
        private readonly RecordAudit $audit,
    ) {}

    public function onPaid(Payment $payment): void
    {
        $license = ProvinceLicense::query()->lockForUpdate()->findOrFail($payment->license_id);
        $termDays = (int) DB::table('platform_settings')->value('license_term_days');

        if ($license->status === ProvinceLicense::PENDING_PAYMENT) {
            $license->forceFill(['status' => ProvinceLicense::ACTIVE, 'start_at' => now(), 'end_at' => now()->addDays($termDays)]);
            ($this->grantRole)($license->operator_user_id, AppRole::Operator, $license->province_id);
            ($this->wallet)('OPERATOR', $license->operator_user_id, $license->province_id, 'OPERATOR_REVENUE');
            ($this->wallet)('OPERATOR', $license->operator_user_id, $license->province_id, 'OPERATOR_CASH');
        } elseif (in_array($license->status, ProvinceLicense::OPERATING, true)) {
            /** @var Carbon $endAt */
            $endAt = $license->end_at ?? now();
            $license->forceFill(['status' => ProvinceLicense::ACTIVE, 'end_at' => $endAt->max(now())->copy()->addDays($termDays)]);
        } else {
            // Paid after the license was rejected/expired/suspended: keep the money on record for a manual refund.
            Log::warning('License payment received for a license that cannot take it', ['payment' => $payment->id, 'license_status' => $license->status]);

            return;
        }

        $license->forceFill(['payment_id' => $payment->id, 'fee_centavos' => $payment->amount_centavos])->save();

        ($this->postLedger)(
            type: 'LICENSE_PAYMENT',
            idempotencyKey: 'license-payment:'.$payment->id,
            provinceId: $license->province_id,
            lines: [
                // Money taken outside the gateway clears through its own account (DECISIONS 76).
                new LedgerLine(($this->wallet)('SYSTEM', null, null, $payment->provider === Payment::PROVIDER_MANUAL ? 'OFFLINE_RECEIPTS' : 'GATEWAY_CLEARING'), -$payment->amount_centavos, 'LICENSE_FEE'),
                new LedgerLine(($this->wallet)('PLATFORM', null, null, 'PLATFORM_REVENUE'), $payment->amount_centavos, 'LICENSE_FEE'),
            ],
            refType: 'payment',
            refId: $payment->id,
        );

        ($this->audit)(null, 'license.paid', 'province_license', $license->id, $license->province_id,
            after: ['status' => $license->status, 'end_at' => $license->end_at?->toIso8601String(), 'payment_id' => $payment->id]);
    }
}
