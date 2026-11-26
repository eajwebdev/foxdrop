<?php

namespace App\Actions\Territory;

use App\Actions\Audit\RecordAudit;
use App\Support\OperatorContext;
use Illuminate\Support\Facades\DB;

/**
 * Sets the monthly fee the province's stores pay its operator (§9.4 step 4), audited with the old and new value.
 */
final class UpdateProvinceStoreFee
{
    public function __construct(private readonly RecordAudit $audit) {}

    public function __invoke(OperatorContext $operator, int $feeCentavos): void
    {
        $operator->ensureCanChange();

        DB::transaction(function () use ($operator, $feeCentavos): void {
            $province = DB::table('provinces')->where('id', $operator->provinceId)->lockForUpdate();
            $before = $province->value('store_fee_centavos');
            $province->update(['store_fee_centavos' => $feeCentavos]);
            ($this->audit)($operator->user, 'province.store_fee_changed', 'province', $operator->provinceId, $operator->provinceId,
                before: ['store_fee_centavos' => $before], after: ['store_fee_centavos' => $feeCentavos]);
        });
    }
}
