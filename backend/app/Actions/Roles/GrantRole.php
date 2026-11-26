<?php

namespace App\Actions\Roles;

use App\Enums\AppRole;
use Illuminate\Support\Facades\DB;

/**
 * Gives a user a role (idempotent). The apps see it on their next `/me`.
 */
final class GrantRole
{
    public function __invoke(string $userId, AppRole $role, ?string $provinceId = null, ?string $storeId = null): void
    {
        DB::table('user_roles')->insertOrIgnore([
            'user_id' => $userId,
            'role' => $role->value,
            'province_id' => $provinceId,
            'store_id' => $storeId,
        ]);
    }
}
