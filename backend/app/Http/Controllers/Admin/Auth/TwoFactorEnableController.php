<?php

namespace App\Http\Controllers\Admin\Auth;

use App\Http\Controllers\Controller;
use App\Models\User;
use Illuminate\Http\Request;
use Laravel\Fortify\Actions\EnableTwoFactorAuthentication;
use Laravel\Fortify\Contracts\TwoFactorEnabledResponse;

/**
 * Starts the authenticator setup (Fortify's enable step). "Start over" (force) is only allowed while the setup is not
 * confirmed: once 2FA is on, a new secret would count as confirmed without ever being scanned.
 */
class TwoFactorEnableController extends Controller
{
    public function __invoke(Request $request, EnableTwoFactorAuthentication $enable): TwoFactorEnabledResponse
    {
        /** @var User $user */
        $user = $request->user();
        $enable($user, ! $user->hasEnabledTwoFactorAuthentication() && $request->boolean('force'));

        return app(TwoFactorEnabledResponse::class);
    }
}
