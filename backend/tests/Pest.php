<?php

use App\Contracts\SmsSender;
use App\Models\User;
use Illuminate\Foundation\Testing\LazilyRefreshDatabase;
use Tests\TestCase;

pest()->extend(TestCase::class)
    ->use(LazilyRefreshDatabase::class)
    ->in('Feature');

/**
 * Records SMS messages instead of sending them.
 */
final class FakeSmsSender implements SmsSender
{
    /** @var list<array{phone: string, message: string}> */
    public array $sent = [];

    public function send(string $phone, string $message): void
    {
        $this->sent[] = ['phone' => $phone, 'message' => $message];
    }

    public function lastCodeFor(string $phone): ?string
    {
        foreach (array_reverse($this->sent) as $sms) {
            if ($sms['phone'] === $phone && preg_match('/\b(\d{6})\b/', $sms['message'], $match) === 1) {
                return $match[1];
            }
        }

        return null;
    }
}

function fakeSms(): FakeSmsSender
{
    $fake = new FakeSmsSender;
    app()->instance(SmsSender::class, $fake);

    return $fake;
}

/**
 * A bearer header for a fresh token of [user].
 *
 * @return array{Authorization: string}
 */
function bearer(User $user): array
{
    return ['Authorization' => 'Bearer '.$user->createToken('test-device', ['*'], now()->addDay())->plainTextToken];
}
