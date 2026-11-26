<?php

namespace App\Actions\Licensing;

use App\Actions\Audit\RecordAudit;
use App\Models\ProvinceLicense;
use App\Support\PlatformSettings;

/**
 * Daily license status changes (§5.1, §12): ACTIVE past its end date → GRACE (operations continue, banner shown);
 * GRACE past the grace days → EXPIRED (province hidden from customers, operator read-only). The rule itself is
 * ProvinceLicense::statusForTerm().
 */
final class AdvanceLicenseLifecycle
{
    public function __construct(private readonly RecordAudit $audit) {}

    /**
     * @return array{grace: int, expired: int}
     */
    public function __invoke(): array
    {
        $graceDays = PlatformSettings::licenseGraceDays();
        $counts = [ProvinceLicense::GRACE => 0, ProvinceLicense::EXPIRED => 0];

        $due = ProvinceLicense::query()->whereIn('status', ProvinceLicense::OPERATING)->where('end_at', '<', now())->get();
        foreach ($due as $license) {
            $status = $license->statusForTerm($graceDays);
            if ($status === $license->status) {
                continue;
            }
            $license->forceFill(['status' => $status])->save();
            $counts[$status]++;
            ($this->audit)(null, 'license.'.strtolower($status), 'province_license', $license->id, $license->province_id);
        }

        return ['grace' => $counts[ProvinceLicense::GRACE], 'expired' => $counts[ProvinceLicense::EXPIRED]];
    }
}
