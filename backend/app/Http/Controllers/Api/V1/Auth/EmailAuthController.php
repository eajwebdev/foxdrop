<?php

namespace App\Http\Controllers\Api\V1\Auth;

use App\Actions\Auth\IssueApiToken;
use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\Auth\EmailLoginRequest;
use App\Http\Requests\Api\V1\Auth\EmailRegisterRequest;
use App\Models\User;
use Illuminate\Database\UniqueConstraintViolationException;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\ValidationException;

/**
 * Email + password sign-up and sign-in (§3).
 */
class EmailAuthController extends Controller
{
    public function register(EmailRegisterRequest $request, IssueApiToken $issueToken): JsonResponse
    {
        try {
            $user = User::query()->create($request->safe()->only(['full_name', 'email', 'password']));
        } catch (UniqueConstraintViolationException) {
            // Registered a moment ago by a parallel request (double tap); same answer as the validation rule.
            throw ValidationException::withMessages(['email' => __('validation.unique', ['attribute' => 'email'])]);
        }

        return $issueToken($user, $request->string('device_name')->toString(), 201);
    }

    public function login(EmailLoginRequest $request, IssueApiToken $issueToken): JsonResponse
    {
        $user = User::query()->where('email', $request->string('email')->toString())->first();
        if ($user === null || $user->password === null || ! Hash::check($request->string('password')->toString(), $user->password)) {
            throw ValidationException::withMessages(['email' => 'The email or password is incorrect.']);
        }

        return $issueToken($user, $request->string('device_name')->toString());
    }
}
