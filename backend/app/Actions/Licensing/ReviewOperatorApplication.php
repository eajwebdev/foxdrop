<?php

namespace App\Actions\Licensing;

use App\Actions\Audit\RecordAudit;
use App\Models\OperatorApplication;
use App\Models\ProvinceLicense;
use App\Models\User;
use Illuminate\Support\Facades\DB;

/**
 * Admin decision on an operator application (§9.5 step 3, DECISIONS 55). Approve → the license waits for payment;
 * reject → the province is free again. Until the admin web (Phase W1) this runs from an Artisan command.
 */
final class ReviewOperatorApplication
{
    public function __construct(private readonly RecordAudit $audit) {}

    public function __invoke(?User $admin, OperatorApplication $application, bool $approve, ?string $reason = null): OperatorApplication
    {
        return DB::transaction(function () use ($admin, $application, $approve, $reason): OperatorApplication {
            $application = OperatorApplication::query()->lockForUpdate()->findOrFail($application->id);
            abort_unless($application->status === OperatorApplication::SUBMITTED, 409, 'This application was already reviewed.');

            $application->forceFill([
                'status' => $approve ? OperatorApplication::APPROVED : OperatorApplication::REJECTED,
                'rejection_reason' => $approve ? null : $reason,
                'reviewed_by' => $admin?->id,
                'reviewed_at' => now(),
            ])->save();

            ProvinceLicense::query()
                ->where('application_id', $application->id)
                ->where('status', ProvinceLicense::APPLIED)
                ->update(['status' => $approve ? ProvinceLicense::PENDING_PAYMENT : ProvinceLicense::REJECTED]);

            ($this->audit)($admin, $approve ? 'operator_application.approved' : 'operator_application.rejected',
                'operator_application', $application->id, $application->province_id, after: ['reason' => $reason]);

            return $application;
        });
    }
}
