<?php

namespace App\Http\Middleware;

use App\Enums\AdminPermission;
use App\Models\User;
use App\Support\IdentityConfirmation;
use Illuminate\Http\Request;
use Inertia\Middleware;

/**
 * Props every admin page gets: who is signed in, what they may do (the sidebar hides the rest; the server still
 * checks), and until when their "confirm it's you" holds.
 */
class HandleInertiaRequests extends Middleware
{
    /**
     * @var string
     */
    protected $rootView = 'app';

    /**
     * @return array<string, mixed>
     */
    public function share(Request $request): array
    {
        $user = $request->user();

        return [
            ...parent::share($request),
            'name' => config('app.name'),
            'auth' => [
                'user' => $user instanceof User ? [
                    'id' => $user->id,
                    'full_name' => $user->full_name,
                    'email' => $user->email,
                    'is_super_admin' => $user->isSuperAdmin(),
                    'permissions' => array_values(array_map(
                        fn (AdminPermission $permission): string => $permission->value,
                        array_filter(AdminPermission::cases(), fn (AdminPermission $permission): bool => $user->hasAdminPermission($permission)),
                    )),
                    'two_factor_enabled' => $user->two_factor_confirmed_at !== null,
                ] : null,
                'confirmedUntil' => $user instanceof User ? IdentityConfirmation::confirmedUntil($request) : null,
            ],
            'sidebarOpen' => ! $request->hasCookie('sidebar_state') || $request->cookie('sidebar_state') === 'true',
        ];
    }
}
