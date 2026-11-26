<?php

namespace App\Policies;

use App\Enums\AdminPermission;
use App\Models\ProvinceLicense;
use App\Models\User;

/**
 * Admin changes to a province license: record an offline payment, suspend, reinstate (DECISIONS 76–77).
 */
class ProvinceLicensePolicy
{
    public function manage(User $user, ProvinceLicense $license): bool
    {
        return $user->hasAdminPermission(AdminPermission::FranchisesManage);
    }
}
