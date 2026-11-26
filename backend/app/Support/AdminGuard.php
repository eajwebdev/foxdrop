<?php

namespace App\Support;

use App\Enums\AdminPermission;
use App\Models\User;
use Illuminate\Support\Facades\Gate;

/**
 * Permission checks inside the shared Actions (§20.2): the admin web checks the same permission in its routes, and
 * this stops an admin-staff account without it from doing the same through the API or another caller.
 */
final class AdminGuard
{
    public static function ensure(User $admin, AdminPermission $permission): void
    {
        // Through the Gate, like the routes' `can:` middleware, so both always ask the same question.
        abort_unless(Gate::forUser($admin)->allows($permission->value), 403, 'You do not have permission to do this.');
    }

    public static function ensureSuperAdmin(User $admin): void
    {
        abort_unless(Gate::forUser($admin)->allows('admin.staff'), 403, 'Only the super admin can do this.');
    }
}
