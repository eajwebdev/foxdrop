<?php

namespace App\Http\Controllers\Admin\Users;

use App\Actions\Users\ChangeUserStatus;
use App\Http\Controllers\Admin\AdminController;
use App\Http\Requests\Admin\ReasonRequest;
use App\Models\User;
use App\Queries\Admin\UserQueries;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Inertia\Inertia;
use Inertia\Response;

/**
 * User accounts: search, detail, suspend / reactivate (§9.5 step 3).
 */
class UserController extends AdminController
{
    public function index(Request $request, UserQueries $queries): Response
    {
        $filters = [
            'q' => $this->filter($request, 'q'),
            'role' => $this->filter($request, 'role', UserQueries::roleNames()),
            'status' => $this->filter($request, 'status', [User::STATUS_ACTIVE, User::STATUS_SUSPENDED]),
        ];

        return Inertia::render('users/index', [
            'filters' => $filters,
            'roles' => UserQueries::roleNames(),
            'users' => $this->deferred(fn (): array => $queries->users($filters, $this->page($request))),
        ]);
    }

    public function show(User $user, UserQueries $queries): Response
    {
        return Inertia::render('users/show', ['user' => $queries->user($user)]);
    }

    public function suspend(ReasonRequest $request, User $user, ChangeUserStatus $change): RedirectResponse
    {
        $change->suspend($this->admin($request), $user, $request->reason());

        return $this->done('Account suspended and signed out everywhere.');
    }

    public function reactivate(Request $request, User $user, ChangeUserStatus $change): RedirectResponse
    {
        $change->reactivate($this->admin($request), $user);

        return $this->done('Account reactivated.');
    }
}
