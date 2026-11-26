<?php

namespace App\Http\Controllers\Api\V1\Auth;

use App\Actions\Auth\IssueApiToken;
use App\Contracts\GoogleIdTokenVerifier;
use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\Auth\GoogleSignInRequest;
use App\Models\User;
use App\Services\Auth\GoogleIdentity;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\DB;

/**
 * "Continue with Google" (§3): the app sends the ID token from Credential Manager and the raw nonce it generated.
 */
class GoogleAuthController extends Controller
{
    public function __invoke(GoogleSignInRequest $request, GoogleIdTokenVerifier $verifier, IssueApiToken $issueToken): JsonResponse
    {
        $identity = $verifier->verify($request->string('id_token')->toString(), $request->string('nonce')->toString());
        $user = DB::transaction(fn (): User => $this->findOrCreate($identity));

        return $issueToken($user, $request->string('device_name')->toString(), $user->wasRecentlyCreated ? 201 : 200);
    }

    /**
     * Links by Google subject first, then by Google-verified email. Linking to an account whose email was never
     * verified removes its password and signs out its devices: whoever set that password did not prove they own the
     * address, and Google just proved this person does.
     */
    private function findOrCreate(GoogleIdentity $identity): User
    {
        $user = User::query()->where('google_sub', $identity->subject)->first();
        if ($user !== null) {
            return $user;
        }

        $byEmail = $identity->email === null ? null : User::query()->where('email', $identity->email)->lockForUpdate()->first();
        if ($byEmail !== null) {
            if ($byEmail->email_verified_at === null) {
                $byEmail->password = null;
                $byEmail->tokens()->delete();
            }
            $byEmail->forceFill(['google_sub' => $identity->subject, 'email_verified_at' => $byEmail->email_verified_at ?? now()])->save();

            return $byEmail;
        }

        return User::query()->create([
            'full_name' => $identity->name,
            'email' => $identity->email,
            'email_verified_at' => $identity->email === null ? null : now(),
            'google_sub' => $identity->subject,
            'avatar_url' => $identity->pictureUrl,
        ]);
    }
}
