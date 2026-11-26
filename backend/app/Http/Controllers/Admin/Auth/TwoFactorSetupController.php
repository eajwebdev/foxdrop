<?php

namespace App\Http\Controllers\Admin\Auth;

use App\Http\Controllers\Controller;
use App\Models\User;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Inertia\Inertia;
use Inertia\Response;
use Laravel\Fortify\Fortify;

/**
 * The only page an admin without 2FA can open (DECISIONS 74): scan the QR code, confirm a code, save the recovery
 * codes. Once 2FA is confirmed it moves on to the dashboard.
 */
class TwoFactorSetupController extends Controller
{
    public function __invoke(Request $request): Response|RedirectResponse
    {
        /** @var User $user */
        $user = $request->user();
        // Right after confirming, the page stays once more to show the recovery codes.
        $justConfirmed = $request->session()->get('status') === Fortify::TWO_FACTOR_AUTHENTICATION_CONFIRMED;
        if ($user->two_factor_confirmed_at !== null && ! $justConfirmed) {
            return redirect()->route('admin.dashboard');
        }

        return Inertia::render('auth/two-factor-setup', [
            'pending' => $user->two_factor_secret !== null && $user->two_factor_confirmed_at === null,
            'enabled' => $user->two_factor_confirmed_at !== null,
        ]);
    }
}
