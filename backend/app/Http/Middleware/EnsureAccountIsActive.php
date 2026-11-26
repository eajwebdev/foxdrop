<?php

namespace App\Http\Middleware;

use App\Models\User;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Suspended accounts keep their tokens but can no longer use the API.
 */
class EnsureAccountIsActive
{
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();
        abort_if($user instanceof User && $user->isSuspended(), 403, 'This account is suspended. Please contact FoxDrop support.');

        return $next($request);
    }
}
