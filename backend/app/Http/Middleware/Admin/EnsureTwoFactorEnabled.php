<?php

namespace App\Http\Middleware\Admin;

use App\Models\User;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * 2FA is required (§20.4, DECISIONS 74): until an admin has confirmed an authenticator app, every admin page sends
 * them to the setup page.
 */
class EnsureTwoFactorEnabled
{
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();
        if ($user instanceof User && $user->two_factor_confirmed_at !== null) {
            return $next($request);
        }

        return redirect()->route('admin.two-factor.setup');
    }
}
