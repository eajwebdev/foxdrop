<?php

namespace App\Policies;

use App\Enums\AdminPermission;
use App\Models\Province;
use App\Models\User;

/**
 * Giving a province to an operator without an application (DECISIONS 78).
 */
class ProvincePolicy
{
    public function assignLicense(User $user, Province $province): bool
    {
        return $user->hasAdminPermission(AdminPermission::FranchisesManage);
    }
}
