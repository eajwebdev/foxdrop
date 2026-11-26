<?php

use App\Contracts\SmsSender;
use App\Enums\AppRole;
use App\Models\User;
use Database\Seeders\DemoSeeder;
use Illuminate\Foundation\Testing\LazilyRefreshDatabase;
use Illuminate\Http\UploadedFile;
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

/**
 * A provincial operator of [psgcCode] whose license is ACTIVE (or [status]).
 */
function operatorOf(string $psgcCode, string $status = 'ACTIVE'): User
{
    $provinceId = provinceId($psgcCode);
    $operator = User::factory()->withRole(AppRole::Operator, $provinceId)->create();
    DB::table('province_licenses')->where('province_id', $provinceId)->whereIn('status', ['APPLIED', 'PENDING_PAYMENT', 'ACTIVE', 'GRACE'])->update(['status' => 'EXPIRED']);
    DB::table('province_licenses')->insert([
        'province_id' => $provinceId, 'operator_user_id' => $operator->id, 'status' => $status,
        'start_at' => now()->subMonth(), 'end_at' => now()->addMonths(11), 'fee_centavos' => 1_500_000,
    ]);

    return $operator;
}

/** A photo upload (a real JPEG, so the server can re-encode it). */
function photo(string $name = 'photo.jpg'): UploadedFile
{
    return UploadedFile::fake()->image($name, 640, 480);
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
    // Sanctum remembers the first authenticated user for the whole test; forget it so each token counts.
    app('auth')->forgetGuards();

    return ['Authorization' => 'Bearer '.$user->createToken('test-device', ['*'], now()->addDay())->plainTextToken];
}
