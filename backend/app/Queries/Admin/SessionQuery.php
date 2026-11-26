<?php

namespace App\Queries\Admin;

use App\Models\User;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

/**
 * The admin's own signed-in browsers (database session driver), newest first.
 */
final class SessionQuery
{
    /**
     * @return list<array{current: bool, ip: string|null, user_agent: string, last_active_at: string}>
     */
    public function __invoke(User $user, string $currentSessionId): array
    {
        return array_values(DB::table('sessions')
            ->where('user_id', $user->id)
            ->orderByDesc('last_activity')
            ->get(['id', 'ip_address', 'user_agent', 'last_activity'])
            ->map(fn (object $session): array => [
                'current' => $session->id === $currentSessionId,
                'ip' => $session->ip_address,
                'user_agent' => mb_substr((string) $session->user_agent, 0, 200),
                'last_active_at' => Carbon::createFromTimestamp((int) $session->last_activity)->toIso8601String(),
            ])
            ->all());
    }
}
