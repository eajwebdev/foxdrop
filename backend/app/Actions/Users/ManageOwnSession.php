<?php

namespace App\Actions\Users;

use App\Actions\Audit\RecordAudit;
use App\Models\User;
use Illuminate\Support\Facades\DB;

/**
 * An admin's own password and web sessions (§20.4 "log out other devices"). A new password also ends every other
 * session, so whoever knew the old one is signed out.
 */
final class ManageOwnSession
{
    public function __construct(private readonly RecordAudit $audit) {}

    public function changePassword(User $user, string $password, string $currentSessionId): void
    {
        DB::transaction(function () use ($user, $password, $currentSessionId): void {
            $user->forceFill(['password' => $password])->save();
            $user->endWebSessions($currentSessionId);
            ($this->audit)($user, 'admin.password_changed', 'user', $user->id);
        });
    }

    /**
     * @return int how many sessions were ended
     */
    public function endOtherSessions(User $user, string $currentSessionId): int
    {
        return DB::transaction(function () use ($user, $currentSessionId): int {
            $ended = $user->endWebSessions($currentSessionId);
            ($this->audit)($user, 'admin.other_sessions_ended', 'user', $user->id, after: ['sessions' => $ended]);

            return $ended;
        });
    }
}
