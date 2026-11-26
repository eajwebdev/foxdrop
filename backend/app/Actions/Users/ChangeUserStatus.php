<?php

namespace App\Actions\Users;

use App\Actions\Audit\RecordAudit;
use App\Models\User;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Gate;

/**
 * Suspends or reactivates any account (§9.5 step 3, DECISIONS 79). A suspension ends every API token and web session
 * at once. Nobody suspends themselves, and only the super admin may suspend another admin.
 */
final class ChangeUserStatus
{
    public function __construct(private readonly RecordAudit $audit) {}

    public function suspend(User $admin, User $user, string $reason): User
    {
        Gate::forUser($admin)->authorize('changeStatus', $user);

        return $this->change($admin, $user, User::STATUS_SUSPENDED, 'user.suspended', $reason, function (User $user): void {
            $user->endAllSessions();
        });
    }

    public function reactivate(User $admin, User $user): User
    {
        Gate::forUser($admin)->authorize('changeStatus', $user);

        return $this->change($admin, $user, User::STATUS_ACTIVE, 'user.reactivated');
    }

    /**
     * @param  (callable(User): void)|null  $afterChange
     */
    private function change(User $admin, User $user, string $status, string $action, ?string $reason = null, ?callable $afterChange = null): User
    {
        return DB::transaction(function () use ($admin, $user, $status, $action, $reason, $afterChange): User {
            $user = User::query()->lockForUpdate()->findOrFail($user->id);
            abort_if($user->status === $status, 409, $status === User::STATUS_SUSPENDED ? 'This account is already suspended.' : 'This account is already active.');
            $before = ['status' => $user->status];

            $user->forceFill(['status' => $status])->save();
            if ($afterChange !== null) {
                $afterChange($user);
            }

            ($this->audit)($admin, $action, 'user', $user->id, before: $before, after: ['status' => $status, 'reason' => $reason]);

            return $user;
        });
    }
}
