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

        return Inertia::render('auth/confirm-password', ['needsCode' => $user->hasEnabledTwoFactorAuthentication()]);
    }

    public function store(Request $request): RedirectResponse
    {
        /** @var User $user */
        $user = $request->user();
        IdentityConfirmation::confirmFromRequest($request, $user);

        return redirect()->intended(route('admin.dashboard'));
    }
}
