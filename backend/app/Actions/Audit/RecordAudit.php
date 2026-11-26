<?php

namespace App\Actions\Audit;

use App\Enums\AppRole;
use App\Models\User;
use Illuminate\Support\Facades\DB;

/**
 * Writes one append-only `audit_logs` row for an operator or admin change (§17). A null actor is the system
 * (scheduler, Artisan command run by an admin on the server).
 */
final class RecordAudit
{
    /**
     * @param  array<string, mixed>|null  $before
     * @param  array<string, mixed>|null  $after
     */
    public function __invoke(
        ?User $actor,
        string $action,
        string $entityType,
        string $entityId,
        ?string $provinceId = null,
        ?array $before = null,
        ?array $after = null,
    ): void {
        $request = app()->runningInConsole() ? null : request();

        DB::table('audit_logs')->insert([
            'actor_user_id' => $actor?->id,
            'actor_role' => $actor === null ? null : $this->roleOf($actor, $provinceId)?->value,
            'action' => $action,
            'entity_type' => $entityType,
            'entity_id' => $entityId,
            'province_id' => $provinceId,
            'before' => $before === null ? null : json_encode($before, JSON_THROW_ON_ERROR),
            'after' => $after === null ? null : json_encode($after, JSON_THROW_ON_ERROR),
            'ip' => $request?->ip(),
            'user_agent' => $request === null ? null : mb_substr((string) $request->userAgent(), 0, 400),
        ]);
    }

    /**
     * The role the actor used: admin first, then the operator role for this province.
     */
    private function roleOf(User $actor, ?string $provinceId): ?AppRole
    {
        foreach ($actor->roles as $grant) {
            if (in_array($grant->role, AppRole::admin(), true)) {
                return $grant->role;
            }
        }
        foreach ($actor->roles as $grant) {
            if (in_array($grant->role, AppRole::operatorTeam(), true) && $grant->province_id === $provinceId) {
                return $grant->role;
            }
        }

        return null;
    }
}
