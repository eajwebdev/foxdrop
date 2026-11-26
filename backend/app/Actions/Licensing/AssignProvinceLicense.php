<?php

namespace App\Actions\Licensing;

use App\Actions\Audit\RecordAudit;
use App\Enums\AdminPermission;
use App\Models\Province;
use App\Models\ProvinceLicense;
use App\Models\User;
use App\Support\AdminGuard;
use App\Support\PlatformSettings;
use Illuminate\Database\UniqueConstraintViolationException;
use Illuminate\Support\Facades\DB;

/**
 * The admin gives a free province to an existing account without an application (§9.5 step 3, DECISIONS 78), e.g.
 * after a deal made offline or when an expired province is re-assigned. The license waits for payment: the operator
 * pays in the app, or the admin records the offline payment.
 */
final class AssignProvinceLicense
{
    public function __construct(private readonly RecordAudit $audit) {}

    public function __invoke(User $admin, Province $province, User $operator): ProvinceLicense
    {
        AdminGuard::ensure($admin, AdminPermission::FranchisesManage);
        abort_unless($operator->status === User::STATUS_ACTIVE, 409, 'This account is suspended.');
        abort_if($operator->isPlatformAdmin(), 409, 'Admin accounts cannot run a province.');

        return DB::transaction(function () use ($admin, $province, $operator): ProvinceLicense {
            abort_unless(Province::isAvailable($province->id), 409, 'This province already has an operator or is not enabled.');
            abort_if(ProvinceLicense::heldLiveBy($operator->id), 409, 'This account already holds a license or an open application.');

            try {
                $license = ProvinceLicense::query()->create([
                    'province_id' => $province->id,
                    'operator_user_id' => $operator->id,
                    'status' => ProvinceLicense::PENDING_PAYMENT,
                    'fee_centavos' => PlatformSettings::licenseFeeCentavos(),
                ]);
            } catch (UniqueConstraintViolationException) {
                abort(409, 'This province already has an operator.'); // someone applied in the same instant
            }

            ($this->audit)($admin, 'license.assigned', 'province_license', $license->id, $province->id,
                after: ['operator_user_id' => $operator->id, 'status' => $license->status]);

            return $license;
        });
    }
}
