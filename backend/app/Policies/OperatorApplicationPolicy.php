<?php

namespace App\Policies;

use App\Enums\AdminPermission;
use App\Models\OperatorApplication;
use App\Models\User;

/**
 * Approving or rejecting an operator application is the platform's call (DECISIONS 55).
 */
class OperatorApplicationPolicy
{
    public function review(User $user, OperatorApplication $application): bool
    {
        return $user->hasAdminPermission(AdminPermission::FranchisesManage);
    }
}
