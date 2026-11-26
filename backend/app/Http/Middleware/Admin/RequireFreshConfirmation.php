<?php

namespace App\Http\Middleware\Admin;

use App\Models\User;
use App\Support\IdentityConfirmation;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Dangerous admin actions (DECISIONS 75): unless the admin confirmed it was them in the last 15 minutes, the same
 * request must carry `confirm_password` and `confirm_code`. The page's dialog shows those fields only when needed
 * (the shared `auth.confirmedUntil` prop), so there is no detour through another page.
 */
class RequireFreshConfirmation
{
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();
        if ($user instanceof User && IdentityConfirmation::confirmedUntil($request) === null) {
            $request->validate(
                ['confirm_password' => ['required', 'string', 'max:200'], 'confirm_code' => ['nullable', 'string', 'max:20']],
                ['confirm_password.required' => 'Confirm it\'s you: enter your password and authenticator code.'],
            );
            IdentityConfirmation::confirm($request, $user, $request->string('confirm_password')->toString(), $request->input('confirm_code'));
        }

        return $next($request);
    }
}
