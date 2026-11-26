<?php

namespace App\Actions\Partners;

use App\Actions\Audit\RecordAudit;
use App\Models\Store;
use App\Models\User;
use App\Support\OperatorContext;
use Illuminate\Support\Facades\DB;

/**
 * Suspends, reinstates or closes a live store (§5.2, §9.5 step 4, DECISIONS 79). Suspended and closed stores leave
 * the customer app at once (only ACTIVE and PAUSED are published). The reason is kept for the store owner.
 */
final class ChangeStoreStatus
{
    /** @var array<string, list<string>> target status => statuses it may come from */
    private const array FROM = [
        Store::SUSPENDED => [Store::ACTIVE, 'PAUSED'],
        Store::ACTIVE => [Store::SUSPENDED],
        Store::CLOSED => [Store::ACTIVE, 'PAUSED', Store::SUSPENDED],
    ];

    public function __construct(private readonly RecordAudit $audit) {}

    public function __invoke(User $actor, Store $store, string $to, ?string $reason = null): Store
    {
        abort_unless(array_key_exists($to, self::FROM), 422, 'Unknown store status.');
        OperatorContext::ensureCanReview($actor, $store->province_id);

        return DB::transaction(function () use ($actor, $store, $to, $reason): Store {
            $store = Store::query()->lockForUpdate()->findOrFail($store->id);
            abort_unless(in_array($store->status, self::FROM[$to], true), 409, "A {$store->status} store cannot become {$to}.");
            $before = ['status' => $store->status];

            $store->forceFill(['status' => $to, 'status_reason' => $to === Store::ACTIVE ? null : $reason])->save();

            ($this->audit)($actor, 'store.'.strtolower($to === Store::ACTIVE ? 'reinstated' : $to), 'store', $store->id, $store->province_id,
                before: $before, after: ['status' => $to, 'reason' => $reason]);

            return $store;
        });
    }
}
