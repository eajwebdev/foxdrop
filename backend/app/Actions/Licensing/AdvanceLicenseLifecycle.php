<?php

namespace App\Actions\Licensing;

use App\Actions\Audit\RecordAudit;
use App\Models\ProvinceLicense;
use Illuminate\Support\Facades\DB;

/**
 * Daily license status changes (§5.1, §12): ACTIVE past its end date → GRACE (operations continue, banner shown);
 * GRACE past the grace days → EXPIRED (province hidden from customers, operator read-only).
 */
final class AdvanceLicenseLifecycle
{
    public function __construct(private readonly RecordAudit $audit) {}

    /**
     * @return array{grace: int, expired: int}
     */
    public function __invoke(): array
    {
        $graceDays = (int) DB::table('platform_settings')->value('license_grace_days');

        $toGrace = ProvinceLicense::query()->where('status', ProvinceLicense::ACTIVE)->where('end_at', '<', now())->get();
        $toExpired = ProvinceLicense::query()->where('status', ProvinceLicense::GRACE)->where('end_at', '<', now()->subDays($graceDays))->get();

        foreach ([[$toGrace, ProvinceLicense::GRACE], [$toExpired, ProvinceLicense::EXPIRED]] as [$licenses, $status]) {
            foreach ($licenses as $license) {
                $license->forceFill(['status' => $status])->save();
                ($this->audit)(null, 'license.'.strtolower($status), 'province_license', $license->id, $license->province_id);
            }
        }

        return ['grace' => $toGrace->count(), 'expired' => $toExpired->count()];
    }
}
