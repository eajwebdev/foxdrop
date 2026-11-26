<?php

namespace App\Actions\Auth;

use App\Http\Resources\UserResource;
use App\Models\User;
use Illuminate\Http\JsonResponse;

/**
 * Signs a user in on one device: issues a Sanctum token that expires after the configured lifetime and returns it
 * with the user's profile and roles. Suspended accounts are refused.
 */
final class IssueApiToken
{
    public function __invoke(User $user, string $deviceName, int $status = 200): JsonResponse
    {
        $user->ensureActive();

        $expiresAt = now()->addMinutes((int) config('foxdrop.auth.token_ttl_minutes'));
        $token = $user->createToken($deviceName, ['*'], $expiresAt);

        return UserResource::make($user->load('roles'))
            ->additional([
                'token' => $token->plainTextToken,
                'token_type' => 'Bearer',
                'expires_at' => $expiresAt->toIso8601String(),
            ])
            ->response()
            ->setStatusCode($status);
    }
}
