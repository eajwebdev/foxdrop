<?php

namespace App\Queries\Admin;

use App\Models\AuditLog;
use App\Models\User;
use App\Support\Like;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\DB;

/**
 * The audit log viewer (§20.4 step 9): who, what, when, from where, and the before/after values.
 */
final class AuditLogQuery
{
    /**
     * @param  array{action?: string|null, entity?: string|null, actor?: string|null, province?: string|null, from?: string|null, to?: string|null}  $filters
     * @return array<string, mixed>
     */
    public function __invoke(array $filters, int $page): array
    {
        $rows = AuditLog::query()
            ->when($filters['action'] ?? null, fn (Builder $query, string $action) => $query->where('action', 'like', Like::startsWith($action)))
            ->when($filters['entity'] ?? null, fn (Builder $query, string $entity) => $query->where('entity_type', $entity))
            ->when($filters['actor'] ?? null, fn (Builder $query, string $actor) => $query->whereIn('actor_user_id', User::query()->search($actor)->select('id')))
            ->when($filters['province'] ?? null, fn (Builder $query, string $province) => $query->where('province_id', $province))
            ->when($filters['from'] ?? null, fn (Builder $query, string $from) => $query->where('created_at', '>=', Carbon::parse($from, 'Asia/Manila')->startOfDay()->utc()))
            ->when($filters['to'] ?? null, fn (Builder $query, string $to) => $query->where('created_at', '<=', Carbon::parse($to, 'Asia/Manila')->endOfDay()->utc()))
            ->orderByDesc('id') // identity column: the primary key gives newest first without a sort
            ->paginate(50, page: $page);

        $entries = collect($rows->items());
        $actors = Present::peopleById($entries->pluck('actor_user_id'));
        $provinces = DB::table('provinces')->whereIn('id', $entries->pluck('province_id')->filter()->unique())->pluck('name', 'id');

        return Present::page($rows, fn (AuditLog $entry): array => [
            'id' => $entry->id,
            'action' => $entry->action,
            'entity_type' => $entry->entity_type,
            'entity_id' => $entry->entity_id,
            'actor' => $entry->actor_user_id === null ? null : Present::person($actors->get($entry->actor_user_id)),
            'actor_role' => $entry->actor_role?->value,
            'province' => $entry->province_id === null ? null : ['id' => $entry->province_id, 'name' => $provinces[$entry->province_id] ?? null],
            'before' => $entry->before,
            'after' => $entry->after,
            'ip' => $entry->ip,
            'user_agent' => $entry->user_agent,
            'created_at' => $entry->created_at->toIso8601String(),
        ]);
    }

    /**
     * The entity types the filter offers.
     *
     * @return list<string>
     */
    public static function entityTypes(): array
    {
        // A distinct scan of the whole log; the list changes only when a new kind of change ships.
        return Cache::remember('admin.audit.entity-types', now()->addMinutes(10), fn (): array => array_values(
            AuditLog::query()->distinct()->orderBy('entity_type')->pluck('entity_type')->all(),
        ));
    }
}
