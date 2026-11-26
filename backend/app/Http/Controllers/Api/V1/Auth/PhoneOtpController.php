<?php

namespace App\Http\Controllers\Api\V1\Auth;

use App\Actions\Auth\IssueApiToken;
use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\Auth\SendOtpRequest;
use App\Http\Requests\Api\V1\Auth\VerifyOtpRequest;
use App\Models\User;
use App\Services\Auth\OtpService;
use Illuminate\Http\JsonResponse;
use Illuminate\Validation\ValidationException;

/**
 * Phone sign-in and sign-up with a one-time SMS code (§3). New numbers become customer accounts.
 */
class PhoneOtpController extends Controller
{
    public function __construct(private readonly OtpService $otp) {}

    public function send(SendOtpRequest $request): JsonResponse
    {
        $phone = $request->string('phone')->toString();
        $wait = $this->otp->secondsUntilResend($phone);
        if ($wait > 0) {
            return response()->json(
                ['message' => "Please wait {$wait} seconds before asking for a new code.", 'retry_after_seconds' => $wait],
                429,
                ['Retry-After' => (string) $wait],
            );
        }

        $this->otp->send($phone);

        return response()->json([
            'expires_in_seconds' => (int) config('foxdrop.auth.otp.ttl_seconds'),
            'resend_after_seconds' => (int) config('foxdrop.auth.otp.resend_after_seconds'),
        ], 202);
    }

    public function verify(VerifyOtpRequest $request, IssueApiToken $issueToken): JsonResponse
    {
        $phone = $request->string('phone')->toString();
        if (! $this->otp->verify($phone, $request->string('code')->toString())) {
            throw ValidationException::withMessages(['code' => 'That code is wrong or has expired.']);
        }

        // createOrFirst: a parallel verify of the same new number (retry, double tap) finds the row instead of failing.
        $user = User::query()->createOrFirst(
            ['phone' => $phone],
            ['full_name' => $request->input('full_name'), 'phone_verified_at' => now()],
        );
        if ($user->phone_verified_at === null) {
            $user->forceFill(['phone_verified_at' => now()])->save();
        }

        return $issueToken($user, $request->string('device_name')->toString(), $user->wasRecentlyCreated ? 201 : 200);
    }
}
