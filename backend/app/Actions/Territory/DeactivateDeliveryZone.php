<?php

namespace App\Actions\Territory;

use App\Actions\Audit\RecordAudit;
use App\Models\DeliveryZone;
use App\Support\OperatorContext;
use Illuminate\Support\Facades\DB;

/**
 * Takes a coverage zone of the operator's province out of service (§6). The row stays: past orders refer to it.
 */
final class DeactivateDeliveryZone
{
    public function __construct(private readonly RecordAudit $audit) {}

    public function __invoke(OperatorContext $operator, string $zoneId): void
    {
        $operator->ensureCanChange();

        DB::transaction(function () use ($operator, $zoneId): void {
            $zone = DeliveryZone::query()->where('province_id', $operator->provinceId)->lockForUpdate()->findOrFail($zoneId);
            $zone->forceFill(['is_active' => false])->save();
            ($this->audit)($operator->user, 'delivery_zone.deactivated', 'delivery_zone', $zone->id, $operator->provinceId);
        });
    }
}
