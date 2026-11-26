<?php

namespace App\Actions\Partners;

use App\Actions\Audit\RecordAudit;
use App\Actions\Ledger\EnsureWallet;
use App\Actions\Roles\GrantRole;
use App\Enums\AppRole;
use App\Models\Rider;
use App\Models\RiderDocument;
use App\Models\User;
use Illuminate\Support\Facades\DB;

/**
 * The operator (or an admin, overriding) approves or rejects a rider (§9.4 step 4). Approval gives the rider role
 * for the province and the rider's wallets (earnings, cash on hand).
 */
final class ReviewRider
{
    public function __construct(
        private readonly GrantRole $grantRole,
        private readonly EnsureWallet $wallet,
        private readonly RecordAudit $audit,
    ) {}

    public function __invoke(User $reviewer, Rider $rider, bool $approve, ?string $reason = null): Rider
    {
        return DB::transaction(function () use ($reviewer, $rider, $approve, $reason): Rider {
            $rider = Rider::query()->lockForUpdate()->findOrFail($rider->id);
            abort_unless($rider->status === Rider::DOCS_REVIEW, 409, 'This rider is not waiting for review.');

            $rider->forceFill([
                'status' => $approve ? Rider::APPROVED : Rider::REJECTED,
                'rejection_reason' => $approve ? null : $reason,
                'approved_by' => $approve ? $reviewer->id : null,
                'approved_at' => $approve ? now() : null,
            ])->save();
            RiderDocument::query()->where('rider_id', $rider->id)->update([
                'status' => $approve ? 'APPROVED' : 'REJECTED',
                'reviewed_by' => $reviewer->id,
                'reviewed_at' => now(),
            ]);

            if ($approve) {
                ($this->grantRole)($rider->user_id, AppRole::Rider, $rider->province_id);
                ($this->wallet)('RIDER', $rider->id, $rider->province_id, 'RIDER_EARNINGS');
                ($this->wallet)('RIDER', $rider->id, $rider->province_id, 'RIDER_CASH');
            }

            ($this->audit)($reviewer, $approve ? 'rider.approved' : 'rider.rejected', 'rider', $rider->id, $rider->province_id,
                after: ['status' => $rider->status, 'reason' => $reason]);

            return $rider;
        });
    }
}
