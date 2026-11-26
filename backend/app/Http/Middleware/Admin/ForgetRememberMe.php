<?php

namespace App\Http\Middleware\Admin;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Admin sessions end after 30 idle minutes (§20.4); a "remember me" cookie would outlive that, so the sign-in routes
 * never honour one, whatever the form sends.
 */
class ForgetRememberMe
{
    public function handle(Request $request, Closure $next): Response
    {
        $request->merge(['remember' => '0']);

        return $next($request);
    }
}
