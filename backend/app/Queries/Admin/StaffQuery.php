<?php

namespace App\Queries\Admin;

use App\Enums\AppRole;
use App\Models\AuditLog;
use App\Models\User;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

/**
 * Admin accounts for the staff page: role, permissions, 2FA and last sign-in (from the audit log).
 */
final class StaffQuery
{
    /**
     * @return list<array<string, mixed>>
     */
    public function __invoke(): array
    {
        $admins = User::query()
            ->whereHas('roles', fn ($grant) => $grant->whereIn('role', array_map(fn (AppRole $role): string => $role->value, AppRole::admin())))
            ->with('roles')
            ->orderBy('full_name')->orderBy('id')
            ->get();
        $permissions = DB::table('admin_permissions')->whereIn('user_id', $admins->pluck('id'))->orderBy('permission')->get()->groupBy('user_id');
        $lastSignIn = AuditLog::query()
            ->where('action', 'admin.signed_in')
            ->whereIn('actor_user_id', $admins->pluck('id'))
            ->selectRaw('actor_user_id, max(created_at) as last_sign_in_at')
            ->groupBy('actor_user_id')
            ->pluck('last_sign_in_at', 'actor_user_id');

        return array_values($admins->map(fn (User $admin): array => [
            ...Present::person($admin) ?? [],
            'status' => $admin->status,
            'is_super_admin' => $admin->isSuperAdmin(),
            'permissions' => $admin->isSuperAdmin() ? [] : array_values(($permissions[$admin->id] ?? collect())->pluck('permission')->all()),
            'two_factor_enabled' => $admin->hasEnabledTwoFactorAuthentication(),
            'last_sign_in_at' => isset($lastSignIn[$admin->id]) ? Carbon::parse($lastSignIn[$admin->id])->toIso8601String() : null,
        ])->all());
    }
}
