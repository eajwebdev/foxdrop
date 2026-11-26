<?php

namespace App\Actions\Users;

use App\Actions\Audit\RecordAudit;
use App\Actions\Roles\GrantRole;
use App\Enums\AdminPermission;
use App\Enums\AppRole;
use App\Models\User;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Gate;

/**
 * Admin staff accounts and what they may do (§9.5 steps 1 and 3, DECISIONS 72). Only the super admin manages them;
 * there is no public registration. A new staff member sets up 2FA at their first sign-in.
 */
final class ManageAdminStaff
{
    public function __construct(
        private readonly GrantRole $grantRole,
        private readonly RecordAudit $audit,
    ) {}

    /**
     * @param  list<AdminPermission>  $permissions
     */
    public function create(User $superAdmin, string $fullName, string $email, string $password, array $permissions): User
    {
        Gate::forUser($superAdmin)->authorize('admin.staff');

        return DB::transaction(function () use ($superAdmin, $fullName, $email, $password, $permissions): User {
            $email = mb_strtolower(trim($email));
            abort_if(User::query()->where('email', $email)->exists(), 409, 'An account with this email already exists.');

            $staff = User::query()->create(['full_name' => $fullName, 'email' => $email, 'password' => $password]);
            ($this->grantRole)($staff->id, AppRole::AdminStaff);
            $this->writePermissions($superAdmin, $staff, $permissions);

            ($this->audit)($superAdmin, 'admin_staff.created', 'user', $staff->id, after: ['permissions' => $this->names($permissions)]);

            return $staff;
        });
    }

    /**
     * @param  list<AdminPermission>  $permissions
     */
    public function updatePermissions(User $superAdmin, User $staff, array $permissions): void
    {
        $this->ensureStaff($superAdmin, $staff);

        DB::transaction(function () use ($superAdmin, $staff, $permissions): void {
            $before = $staff->adminPermissions();
            DB::table('admin_permissions')->where('user_id', $staff->id)->delete();
            $this->writePermissions($superAdmin, $staff, $permissions);

            ($this->audit)($superAdmin, 'admin_staff.permissions_changed', 'user', $staff->id,
                before: ['permissions' => $before], after: ['permissions' => $this->names($permissions)]);
        });
    }

    public function resetPassword(User $superAdmin, User $staff, string $password): void
    {
        $this->ensureStaff($superAdmin, $staff);

        DB::transaction(function () use ($superAdmin, $staff, $password): void {
            $staff->forceFill(['password' => $password])->save();
            $staff->endWebSessions();
            ($this->audit)($superAdmin, 'admin_staff.password_reset', 'user', $staff->id);
        });
    }

    /**
     * A staff member lost their authenticator and their recovery codes: they set 2FA up again at the next sign-in.
     */
    public function resetTwoFactor(User $superAdmin, User $staff): void
    {
        $this->ensureStaff($superAdmin, $staff);

        DB::transaction(function () use ($superAdmin, $staff): void {
            $staff->resetTwoFactor();
            $staff->endWebSessions();
            ($this->audit)($superAdmin, 'admin_staff.two_factor_reset', 'user', $staff->id);
        });
    }

    /**
     * The account stays (it may be a customer too); it loses admin access and its web sessions.
     */
    public function remove(User $superAdmin, User $staff): void
    {
        $this->ensureStaff($superAdmin, $staff);

        DB::transaction(function () use ($superAdmin, $staff): void {
            $before = $staff->adminPermissions();
            DB::table('admin_permissions')->where('user_id', $staff->id)->delete();
            DB::table('user_roles')->where('user_id', $staff->id)->where('role', AppRole::AdminStaff->value)->delete();
            $staff->endWebSessions();

            ($this->audit)($superAdmin, 'admin_staff.removed', 'user', $staff->id, before: ['permissions' => $before]);
        });
    }

    private function ensureStaff(User $superAdmin, User $staff): void
    {
        Gate::forUser($superAdmin)->authorize('manageStaff', $staff);
    }

    /**
     * @param  list<AdminPermission>  $permissions
     */
    private function writePermissions(User $superAdmin, User $staff, array $permissions): void
    {
        DB::table('admin_permissions')->insert(array_map(fn (string $name): array => [
            'user_id' => $staff->id,
            'permission' => $name,
            'granted_by' => $superAdmin->id,
        ], $this->names($permissions)));
    }

    /**
     * @param  list<AdminPermission>  $permissions
     * @return list<string>
     */
    private function names(array $permissions): array
    {
        return array_values(array_unique(array_map(fn (AdminPermission $permission): string => $permission->value, $permissions)));
    }
}
