<?php

use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\Request;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        api: __DIR__.'/../routes/api.php',
        commands: __DIR__.'/../routes/console.php',
        channels: __DIR__.'/../routes/channels.php',
        health: '/up',
    )
    ->withMiddleware(function (Middleware $middleware): void {
        // Behind nginx/Cloudflare on the VPS the client IP (rate limits, audit) comes from X-Forwarded-For, trusted only
        // from the proxies listed in TRUSTED_PROXIES (comma-separated IPs/CIDRs, or * when the app is reachable only
        // through the proxy).
        $proxies = env('TRUSTED_PROXIES');
        if (is_string($proxies) && $proxies !== '') {
            $middleware->trustProxies(at: $proxies === '*' ? '*' : array_map(trim(...), explode(',', $proxies)));
        }
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        $exceptions->shouldRenderJsonWhen(
            fn (Request $request) => $request->is('api/*') || $request->expectsJson(),
        );
    })->create();
