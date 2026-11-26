<?php

namespace App\Services\Auth;

use App\Contracts\SmsSender;
use App\Models\OtpCode;
use Illuminate\Support\Facades\DB;

/**
 * Phone sign-in codes (§3). A code is 6 digits, expires after 5 minutes, allows 5 attempts and works once. Only an
 * HMAC of the code is stored. Demo numbers can have fixed codes in local and testing environments only.
 */
final class OtpService
{
    public function __construct(private readonly SmsSender $sms) {}

    /**
     * Seconds the caller must wait before another code may be sent to this phone (0 = may send now).
     */
    public function secondsUntilResend(string $phone): int
    {
        $latest = OtpCode::query()->where('phone', $phone)->latest('created_at')->first();
        if ($latest === null) {
            return 0;
        }
        $readyAt = $latest->created_at->copy()->addSeconds($this->config('resend_after_seconds'));

        return max(0, (int) ceil(now()->diffInSeconds($readyAt, false)));
    }

    /**
     * Sends a new code and retires every earlier one for this phone.
     */
    public function send(string $phone): void
    {
        $testCode = $this->testCodeFor($phone);
        $code = $testCode ?? $this->randomCode();

        DB::transaction(function () use ($phone, $code): void {
            OtpCode::query()->where('phone', $phone)->whereNull('consumed_at')->update(['consumed_at' => now()]);
            OtpCode::query()->create([
                'phone' => $phone,
                'code_hash' => $this->hash($phone, $code),
                'expires_at' => now()->addSeconds($this->config('ttl_seconds')),
            ]);
        });

        if ($testCode === null) {
            $this->sms->send($phone, "Your FoxDrop code is {$code}. It expires in 5 minutes. Never share this code.");
        }
    }

    /**
     * True when the code matches the phone's latest unused, unexpired code. Each try counts as an attempt.
     */
    public function verify(string $phone, string $code): bool
    {
        return DB::transaction(function () use ($phone, $code): bool {
            $otp = OtpCode::query()
                ->where('phone', $phone)
                ->whereNull('consumed_at')
                ->latest('created_at')
                ->lockForUpdate()
                ->first();

            if ($otp === null || $otp->expires_at->isPast() || $otp->attempts >= $this->config('max_attempts')) {
                return false;
            }

            $otp->increment('attempts');
            if (! hash_equals($otp->code_hash, $this->hash($phone, $code))) {
                return false;
            }
            $otp->forceFill(['consumed_at' => now()])->save();

            return true;
        });
    }

    private function testCodeFor(string $phone): ?string
    {
        if (! app()->environment(['local', 'testing'])) {
            return null;
        }

        return config('foxdrop.auth.test_otps')[$phone] ?? null;
    }

    private function randomCode(): string
    {
        $length = $this->config('length');

        return str_pad((string) random_int(0, 10 ** $length - 1), $length, '0', STR_PAD_LEFT);
    }

    private function hash(string $phone, string $code): string
    {
        return hash_hmac('sha256', $phone.'|'.$code, (string) config('app.key'));
    }

    private function config(string $key): int
    {
        return (int) config('foxdrop.auth.otp.'.$key);
    }
}
