<?php

namespace App\Actions\Partners;

use App\Actions\Audit\RecordAudit;
use App\Actions\Ledger\EnsureWallet;
use App\Actions\Roles\GrantRole;
use App\Enums\AppRole;
use App\Models\Store;
use App\Models\StoreDocument;
use App\Models\User;
use Illuminate\Support\Facades\DB;

/**
 * The operator (or an admin, overriding) approves or rejects a store application (§9.4 step 4, §9.5 step 4).
 * Approved stores go live at once (DECISIONS 59): the owner gets the store_owner role and the store's wallets.
 */
final class ReviewStore
{
    public function __construct(
        private readonly GrantRole $grantRole,
        private readonly EnsureWallet $wallet,
        private readonly RecordAudit $audit,
    ) {}

    public function __invoke(User $reviewer, Store $store, bool $approve, ?string $reason = null): Store
    {
        return DB::transaction(function () use ($reviewer, $store, $approve, $reason): Store {
            $store = Store::query()->lockForUpdate()->findOrFail($store->id);
            abort_unless($store->status === Store::SUBMITTED, 409, 'This store is not waiting for review.');

            $store->forceFill([
                'status' => $approve ? Store::ACTIVE : Store::REJECTED,
                'rejection_reason' => $approve ? null : $reason,
            ])->save();
            StoreDocument::query()->where('store_id', $store->id)->update(['status' => $approve ? 'APPROVED' : 'REJECTED']);

            if ($approve) {
                ($this->grantRole)($store->owner_user_id, AppRole::StoreOwner, $store->province_id, $store->id);
                ($this->wallet)('STORE', $store->id, $store->province_id, 'STORE_TOPUP');
                ($this->wallet)('STORE', $store->id, $store->province_id, 'STORE_PAYABLE');
            }

            ($this->audit)($reviewer, $approve ? 'store.approved' : 'store.rejected', 'store', $store->id, $store->province_id,
                after: ['status' => $store->status, 'reason' => $reason]);

            return $store;
        });
    }
}
