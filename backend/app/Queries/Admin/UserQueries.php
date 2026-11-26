<?php

namespace App\Queries\Admin;

use App\Enums\AppRole;
use App\Models\AuditLog;
use App\Models\Province;
use App\Models\Rider;
use App\Models\Store;
use App\Models\User;
use App\Models\UserRole;
use Illuminate\Database\Eloquent\Builder;

/**
 * Every account (customers, riders, store teams, operators, admins) for the Users pages (§9.5 step 3).
 */
final class UserQueries
{
    /**
     * @param  array{q?: string|null, role?: string|null, status?: string|null}  $filters
     * @return array<string, mixed>
     */
    public function users(array $filters, int $page): array
    {
        $rows = User::query()
            ->with('roles')
            ->when($filters['q'] ?? null, fn (Builder $query, string $term) => $query->search($term))
            ->when($filters['role'] ?? null, fn (Builder $query, string $role) => $query->whereHas('roles', fn (Builder $grant) => $grant->where('role', $role)))
            ->when($filters['status'] ?? null, fn (Builder $query, string $status) => $query->where('status', $status))
            ->orderByDesc('created_at')->orderBy('id')
            ->paginate(25, page: $page);

        return Present::page($rows, fn (User $user): array => [
            ...Present::person($user) ?? [],
            'status' => $user->status,
            'roles' => array_values(array_unique($user->roles->map(fn (UserRole $grant): string => $grant->role->value)->all())),
            'created_at' => Present::date($user->created_at),
        ]);
    }

    /**
     * @return array<string, mixed>
     */
    public function user(User $user): array
    {
        $user->loadMissing('roles');
        $provinces = Province::query()->whereIn('id', $user->roles->pluck('province_id')->filter())->pluck('name', 'id');
        $stores = Store::query()->whereIn('id', $user->roles->pluck('store_id')->filter())->pluck('name', 'id');
        $rider = Rider::query()->where('user_id', $user->id)->first(['id', 'status']);

        return [
            ...Present::person($user) ?? [],
            'status' => $user->status,
            'created_at' => Present::date($user->created_at),
            'is_admin' => $user->isPlatformAdmin(),
            'two_factor_enabled' => $user->hasEnabledTwoFactorAuthentication(),
            'roles' => array_values($user->roles->map(fn (UserRole $grant): array => [
                'role' => $grant->role->value,
                'province' => $grant->province_id === null ? null : ['id' => $grant->province_id, 'name' => $provinces[$grant->province_id] ?? null],
                'store' => $grant->store_id === null ? null : ['id' => $grant->store_id, 'name' => $stores[$grant->store_id] ?? null],
            ])->all()),
            'rider' => $rider === null ? null : ['id' => $rider->id, 'status' => $rider->status],
            'history' => array_values(AuditLog::query()
                ->where('entity_type', 'user')->where('entity_id', $user->id)
                ->orderByDesc('id')->limit(10)
                ->get(['action', 'created_at', 'after'])
                ->map(fn (AuditLog $entry): array => ['action' => $entry->action, 'at' => Present::date($entry->created_at), 'after' => $entry->after])
                ->all()),
        ];
    }

    /**
     * @return list<string>
     */
    public static function roleNames(): array
    {
        return array_map(fn (AppRole $role): string => $role->value, AppRole::cases());
    }
}
