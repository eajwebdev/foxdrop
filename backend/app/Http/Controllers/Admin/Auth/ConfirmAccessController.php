<?php

namespace App\Http\Controllers\Admin\Auth;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Support\IdentityConfirmation;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Inertia\Inertia;
use Inertia\Response;

/**
 * The "Confirm it's you" page that `RequirePassword` sends admins to (security settings, 2FA re-keying).
 */
class ConfirmAccessController extends Controller
{
    public function show(Request $request): Response
    {
        /** @var User $user */
        $user = $request->user();

        return Inertia::render('auth/confirm-password', ['needsCode' => $user->two_factor_confirmed_at !== null]);
    }

    public function store(Request $request): RedirectResponse
    {
        $request->validate(['confirm_password' => ['required', 'string', 'max:200'], 'confirm_code' => ['nullable', 'string', 'max:20']]);
        /** @var User $user */
        $user = $request->user();
        IdentityConfirmation::confirm($request, $user, $request->string('confirm_password')->toString(), $request->input('confirm_code'));

        return redirect()->intended(route('admin.dashboard'));
    }
}
