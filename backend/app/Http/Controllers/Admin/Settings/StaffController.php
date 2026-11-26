<?php

namespace App\Http\Controllers\Admin\Settings;

use App\Actions\Users\ManageAdminStaff;
use App\Enums\AdminPermission;
use App\Http\Controllers\Admin\AdminController;
use App\Http\Requests\Admin\PasswordRequest;
use App\Http\Requests\Admin\StaffRequest;
use App\Models\User;
use App\Queries\Admin\StaffQuery;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rules\Password;
use Inertia\Inertia;
use Inertia\Response;

/**
 * Admin staff and their permissions (super admin only; §9.5 steps 1 and 3, DECISIONS 72).
 */
class StaffController extends AdminController
{
    public function index(StaffQuery $query): Response
    {
        return Inertia::render('settings/staff', [
            'staff' => $query(),
            'permissions' => array_map(fn (AdminPermission $permission): array => ['value' => $permission->value, 'label' => $permission->label()], AdminPermission::cases()),
            'passwordRules' => Password::defaults()->toPasswordRulesString(),
        ]);
    }

    public function store(StaffRequest $request, ManageAdminStaff $manage): RedirectResponse
    {
        $manage->create($this->admin($request), $request->string('full_name')->toString(), $request->string('email')->toString(), $request->string('password')->toString(), $request->permissions());

        return $this->done('Staff account created. They set up 2FA when they first sign in.');
    }

    public function update(StaffRequest $request, User $staff, ManageAdminStaff $manage): RedirectResponse
    {
        $manage->updatePermissions($this->admin($request), $staff, $request->permissions());

        return $this->done('Permissions saved.');
    }

    public function resetPassword(PasswordRequest $request, User $staff, ManageAdminStaff $manage): RedirectResponse
    {
        $manage->resetPassword($this->admin($request), $staff, $request->string('password')->toString());

        return $this->done('Password reset. Share it with them in person or by phone.');
    }

    public function resetTwoFactor(Request $request, User $staff, ManageAdminStaff $manage): RedirectResponse
    {
        $manage->resetTwoFactor($this->admin($request), $staff);

        return $this->done('2FA reset. They set it up again at their next sign-in.');
    }

    public function destroy(Request $request, User $staff, ManageAdminStaff $manage): RedirectResponse
    {
        $manage->remove($this->admin($request), $staff);

        return $this->done('Admin access removed.');
    }
}
