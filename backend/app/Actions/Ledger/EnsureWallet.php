<?php

namespace App\Actions\Ledger;

use Illuminate\Support\Facades\DB;

/**
 * Returns the id of a wallet, creating it when missing (wallets are unique per owner, province and kind, §7).
 */
final class EnsureWallet
{
    public function __invoke(string $ownerType, ?string $ownerId, ?string $provinceId, string $kind): string
    {
        $key = ['owner_type' => $ownerType, 'owner_id' => $ownerId, 'province_id' => $provinceId, 'kind' => $kind];
        DB::table('wallets')->insertOrIgnore($key);

        return (string) DB::table('wallets')
            ->where('owner_type', $ownerType)
            ->where('kind', $kind)
            ->when($ownerId === null, fn ($q) => $q->whereNull('owner_id'), fn ($q) => $q->where('owner_id', $ownerId))
            ->when($provinceId === null, fn ($q) => $q->whereNull('province_id'), fn ($q) => $q->where('province_id', $provinceId))
            ->value('id');
    }
}
