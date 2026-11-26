<?php

use App\Contracts\SmsSender;
use App\Models\User;
use Database\Seeders\DemoSeeder;
use Illuminate\Foundation\Testing\LazilyRefreshDatabase;
use Illuminate\Support\Facades\DB;
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

/** Iloilo: the second province in isolation tests (province B; Negros Occidental is the demo province A). */
const ILOILO = '0603000000';

function provinceId(string $psgcCode): string
{
    return (string) DB::table('provinces')->where('psgc_code', $psgcCode)->value('id');
}

/** A seeded demo account (database/seeders/DemoSeeder.php): 1–2 customers, 3–5 riders, 6 operator, 7–9 store owners, 10 admin. */
function demoUser(int $n): User
{
    return User::query()->findOrFail(DemoSeeder::userId($n));
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
