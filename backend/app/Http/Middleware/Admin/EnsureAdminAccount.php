<?php

namespace App\Http\Middleware\Admin;

use App\Models\User;
use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Symfony\Component\HttpFoundation\Response;

/**
 * Every admin page: the signed-in account must still be an ACTIVE admin (§20.2). A suspended or demoted admin is
 * signed out on their next click, not when their session ends.
 */
class EnsureAdminAccount
{
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();
        if ($user instanceof User && $user->canUseAdminWeb()) {
            return $next($request);
        }

        Auth::guard('web')->logout();
        $request->session()->invalidate();
        $request->session()->regenerateToken();

        return redirect()->route('login')->with('status', 'Please sign in with an active admin account.');
    }
}
