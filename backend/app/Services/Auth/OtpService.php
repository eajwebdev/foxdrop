<?php

namespace App\Services\Auth;

use App\Contracts\SmsSender;
use App\Models\OtpCode;
use Illuminate\Support\Facades\DB;

/**
 * Phone sign-in codes (§3), sized and timed by config/foxdrop.php (6 digits, 5 minutes, 5 attempts, one use). Only an
 * HMAC of the code is stored. Demo numbers can have fixed codes in local and testing environments only.
 */
final class OtpService
{
    public function __construct(private readonly SmsSender $sms) {}

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
            $minutes = intdiv($this->config('ttl_seconds'), 60);
            $this->sms->send($phone, "Your FoxDrop code is {$code}. It expires in {$minutes} minutes. Never share this code.");
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

            $matches = hash_equals($otp->code_hash, $this->hash($phone, $code));
            $otp->forceFill(['attempts' => $otp->attempts + 1, 'consumed_at' => $matches ? now() : null])->save();

            return $matches;
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
