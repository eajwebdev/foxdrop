<?php

namespace App\Actions\Licensing;

use App\Actions\Audit\RecordAudit;
use App\Enums\AdminPermission;
use App\Models\ProvinceLicense;
use App\Models\User;
use App\Support\AdminGuard;
use Illuminate\Support\Facades\DB;

/**
 * The admin suspends a running license or lifts the suspension (§5.1, DECISIONS 77). A suspended province keeps its
 * operator (nobody else can apply) but customers cannot order there and the operator is read-only. Lifting it
 * returns the status the term allows today: ACTIVE, GRACE or EXPIRED.
 */
final class ChangeLicenseSuspension
{
    public function __construct(private readonly RecordAudit $audit) {}

    public function suspend(User $admin, ProvinceLicense $license, string $reason): ProvinceLicense
    {
        return $this->change($admin, $license, function (ProvinceLicense $license) use ($reason): void {
            abort_unless(in_array($license->status, ProvinceLicense::OPERATING, true), 409, 'Only an active or grace-period license can be suspended.');
            $license->forceFill(['status' => ProvinceLicense::SUSPENDED, 'status_reason' => $reason]);
        }, 'license.suspended');
    }

    public function reinstate(User $admin, ProvinceLicense $license): ProvinceLicense
    {
        return $this->change($admin, $license, function (ProvinceLicense $license): void {
            abort_unless($license->status === ProvinceLicense::SUSPENDED, 409, 'This license is not suspended.');
            $graceDays = (int) DB::table('platform_settings')->value('license_grace_days');
            $endAt = $license->end_at ?? now();
            $status = match (true) {
                $endAt->isFuture() => ProvinceLicense::ACTIVE,
                $endAt->copy()->addDays($graceDays)->isFuture() => ProvinceLicense::GRACE,
                default => ProvinceLicense::EXPIRED,
            };
            $license->forceFill(['status' => $status, 'status_reason' => null]);
        }, 'license.reinstated');
    }

    /**
     * @param  callable(ProvinceLicense): void  $apply
     */
    private function change(User $admin, ProvinceLicense $license, callable $apply, string $action): ProvinceLicense
    {
        AdminGuard::ensure($admin, AdminPermission::FranchisesManage);

        return DB::transaction(function () use ($admin, $license, $apply, $action): ProvinceLicense {
            $license = ProvinceLicense::query()->lockForUpdate()->findOrFail($license->id);
            $before = ['status' => $license->status];
            $apply($license);
            $license->save();

            ($this->audit)($admin, $action, 'province_license', $license->id, $license->province_id,
                before: $before, after: ['status' => $license->status, 'reason' => $license->status_reason]);

            return $license;
        });
    }
}
