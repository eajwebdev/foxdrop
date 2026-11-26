<?php

use App\Http\Middleware\HandleAppearance;
use App\Http\Middleware\HandleInertiaRequests;
use App\Http\Middleware\SecurityHeaders;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\Middleware\AddLinkHeadersForPreloadedAssets;
use Illuminate\Http\Request;
use Inertia\Inertia;
use Spatie\Csp\AddCspHeaders;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpKernel\Exception\HttpExceptionInterface;

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
            $middleware->trustProxies(at: $proxies); // the middleware parses "*" and comma lists itself
        }

        // Admin web (§20): theme cookies stay readable by the page; security headers + CSP on every web response.
        $middleware->encryptCookies(except: ['appearance', 'sidebar_state']);
        $middleware->web(append: [
            SecurityHeaders::class,
            AddCspHeaders::class,
            HandleAppearance::class,
            HandleInertiaRequests::class,
            AddLinkHeadersForPreloadedAssets::class,
        ]);
        $middleware->redirectGuestsTo(fn () => route('login'));
        $middleware->redirectUsersTo(fn () => route('admin.dashboard'));
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        $exceptions->shouldRenderJsonWhen(
            fn (Request $request) => $request->is('api/*') || $request->expectsJson(),
        );

        // Admin web: a refused change (403/409 from an Action) returns to the page with an error toast; a page that
        // cannot open shows the branded error page instead of Laravel's.
        $exceptions->respond(function (Response $response, Throwable $e, Request $request): Response {
            $status = $response->getStatusCode();
            if (! $request->is('admin', 'admin/*') || $request->expectsJson() || ! in_array($status, [403, 404, 409, 419, 429, 500, 503], true)) {
                return $response;
            }
            if ($status === 500 && config('app.debug')) {
                return $response; // keep the debug page locally
            }
            // Only messages our own code wrote for people (abort() in Actions: 403/409) are shown; framework messages
            // such as "No query results for model …" (a wrapped ModelNotFoundException) never reach the page.
            $message = match (true) {
                $status === 429 => 'Too many tries. Please wait a minute and try again.',
                in_array($status, [403, 409], true) && $e instanceof HttpExceptionInterface && $e->getPrevious() === null && $e->getMessage() !== '' => $e->getMessage(),
                default => null,
            };

            if (! $request->isMethod('GET') && in_array($status, [403, 409, 429], true)) {
                Inertia::flash('toast', ['type' => 'error', 'message' => $message ?? 'That did not work. Please try again.']);

                return back();
            }
            if ($status === 419) {
                return redirect()->route('login')->with('status', 'Your session ended. Please sign in again.');
            }

            return Inertia::render('error', ['status' => $status, 'message' => $status === 500 ? null : $message])
                ->toResponse($request)
                ->setStatusCode($status);
        });
    })->create();
