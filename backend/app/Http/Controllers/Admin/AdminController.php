<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Models\User;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Inertia\DeferProp;
use Inertia\Inertia;

/**
 * Admin controllers stay thin (§20.3): authorize (route middleware), call one Action, redirect back with a toast.
 * They never write tables themselves (tests/Feature/Admin/AdminArchitectureTest.php).
 */
abstract class AdminController extends Controller
{
    protected function admin(Request $request): User
    {
        /** @var User */
        return $request->user();
    }

    protected function done(string $message): RedirectResponse
    {
        Inertia::flash('toast', ['type' => 'success', 'message' => $message]);

        return back();
    }

    /**
     * A list loaded after the page shell (skeleton first, §20.3). A failure shows the page's error state with a
     * retry instead of breaking the whole page.
     */
    protected function deferred(callable $load): DeferProp
    {
        return Inertia::defer($load, rescue: true);
    }

    protected function page(Request $request): int
    {
        return max(1, $request->integer('page', 1));
    }

    /**
     * A filter value from the query string, or null when empty or not one of [allowed].
     *
     * @param  list<string>|null  $allowed
     */
    protected function filter(Request $request, string $key, ?array $allowed = null): ?string
    {
        $value = trim($request->string($key)->toString());
        if ($value === '' || mb_strlen($value) > 100) {
            return null;
        }

        return $allowed === null || in_array($value, $allowed, true) ? $value : null;
    }
}
