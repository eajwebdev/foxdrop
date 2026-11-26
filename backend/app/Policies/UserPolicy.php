<?php

namespace App\Policies;

use App\Enums\AdminPermission;
use App\Enums\AppRole;
use App\Models\User;
use Illuminate\Auth\Access\Response;

/**
 * Admin changes to accounts (DECISIONS 72, 79): suspending and reactivating, and managing admin staff.
 */
class UserPolicy
{
    /**
     * Suspend or reactivate: needs the users permission; nobody changes their own account, and only the super admin
     * changes another admin's.
     */
    public function changeStatus(User $admin, User $user): Response
    {
        return match (true) {
            ! $admin->hasAdminPermission(AdminPermission::UsersManage) => Response::deny('You do not have permission to do this.'),
            $admin->is($user) => Response::deny('You cannot change your own account here.'),
            $user->isPlatformAdmin() && ! $admin->isSuperAdmin() => Response::deny('Only the super admin can change another admin\'s account.'),
            default => Response::allow(),
        };
    }

    /**
     * Permissions, password reset, 2FA reset and removal of an admin-staff account: the super admin only.
     */
    public function manageStaff(User $admin, User $staff): Response
    {
        if (! $admin->isSuperAdmin()) {
            return Response::deny('Only the super admin can do this.');
        }

        return $staff->roles->contains('role', AppRole::AdminStaff) && ! $staff->isSuperAdmin()
            ? Response::allow()
            : Response::denyAsNotFound();
    }
}
