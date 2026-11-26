<?php

namespace App\Http\Controllers\Admin\Settings;

use App\Actions\Users\ManageOwnSession;
use App\Http\Controllers\Admin\AdminController;
use App\Http\Requests\Admin\PasswordRequest;
use App\Queries\Admin\SessionQuery;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rules\Password;
use Inertia\Inertia;
use Inertia\Response;

/**
 * My security: password, authenticator recovery codes, signed-in devices (§20.4 step 1).
 */
class SecurityController extends AdminController
{
    public function edit(Request $request, SessionQuery $sessions): Response
    {
        return Inertia::render('settings/security', [
            'sessions' => $sessions($this->admin($request), $request->session()->getId()),
            'passwordRules' => Password::defaults()->toPasswordRulesString(),
        ]);
    }

    public function updatePassword(PasswordRequest $request, ManageOwnSession $manage): RedirectResponse
    {
        $manage->changePassword($this->admin($request), $request->string('password')->toString(), $request->session()->getId());

        return $this->done('Password changed. Your other devices were signed out.');
    }

    public function endOtherSessions(Request $request, ManageOwnSession $manage): RedirectResponse
    {
        $ended = $manage->endOtherSessions($this->admin($request), $request->session()->getId());

        return $this->done($ended === 1 ? '1 other device was signed out.' : "{$ended} other devices were signed out.");
    }
}
