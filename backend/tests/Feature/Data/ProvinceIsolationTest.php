<?php

use App\Enums\AppRole;
use App\Models\Order;
use App\Models\ProvinceLicense;
use App\Models\Rider;
use App\Models\Store;
use App\Models\User;
use App\Models\UserRole;
use App\Models\Wallet;
use Database\Seeders\DemoSeeder;
use Illuminate\Support\Facades\DB;

/*
| Province isolation (CLAUDE.md, §0 rule 5): an operator of province A reads nothing of province B. Province A is the
| demo Negros Occidental; province B (Iloilo) gets its own operator, draft store, rider, order and wallet here.
*/

beforeEach(function () {
    $this->neg = provinceId(DemoSeeder::NEGROS_OCCIDENTAL);
    $this->ilo = provinceId(ILOILO);

    $this->operatorB = User::factory()->withRole(AppRole::Operator, $this->ilo)->create();
    DB::table('province_licenses')->insert(['province_id' => $this->ilo, 'operator_user_id' => $this->operatorB->id, 'status' => 'ACTIVE', 'start_at' => now(), 'end_at' => now()->addYear(), 'fee_centavos' => 1_500_000]);
    $ownerB = User::factory()->create();
    $this->storeB = (string) DB::table('stores')->insertGetId([
        'province_id' => $this->ilo, 'owner_user_id' => $ownerB->id, 'name' => 'Iloilo Draft Store', 'category' => 'FOOD',
        'location' => DB::raw('ST_SetSRID(ST_MakePoint(122.56, 10.72), 4326)::geography'), 'address_line' => 'Calle Real', 'city' => 'Iloilo City', 'status' => 'DRAFT',
    ], 'id');
    $riderUserB = User::factory()->withRole(AppRole::Rider, $this->ilo)->create();
    $this->riderB = (string) DB::table('riders')->insertGetId(['user_id' => $riderUserB->id, 'province_id' => $this->ilo, 'vehicle_type' => 'MOTORCYCLE', 'status' => 'APPROVED'], 'id');
    DB::table('wallets')->insert(['owner_type' => 'RIDER', 'owner_id' => $this->riderB, 'province_id' => $this->ilo, 'kind' => 'RIDER_CASH']);

    $order = fn (string $code, string $province, string $store, ?string $rider) => DB::table('orders')->insert([
        'code' => $code, 'province_id' => $province, 'customer_id' => DemoSeeder::userId(1), 'store_id' => $store, 'rider_id' => $rider,
        'fulfillment' => 'DELIVERY', 'payment_method' => 'COD', 'status' => 'PLACED',
        'item_subtotal_centavos' => 17800, 'delivery_fee_centavos' => 4000, 'total_centavos' => 21800,
    ]);
    $order('FD-NEG-000001', $this->neg, DemoSeeder::storeId(1), DemoSeeder::riderId(1));
    $order('FD-ILO-000001', $this->ilo, $this->storeB, null);
});

it('shows operator A nothing from province B', function () {
    $operatorA = demoUser(6);

    expect(Order::query()->visibleTo($operatorA)->pluck('code')->all())->toBe(['FD-NEG-000001'])
        ->and(Store::query()->visibleTo($operatorA)->whereKey($this->storeB)->exists())->toBeFalse()
        ->and(Rider::query()->visibleTo($operatorA)->where('province_id', $this->ilo)->exists())->toBeFalse()
        ->and(Rider::query()->visibleTo($operatorA)->count())->toBe(3)
        ->and(Wallet::query()->visibleTo($operatorA)->where('province_id', $this->ilo)->exists())->toBeFalse()
        ->and(ProvinceLicense::query()->visibleTo($operatorA)->where('province_id', $this->ilo)->exists())->toBeFalse()
        ->and(UserRole::query()->visibleTo($operatorA)->where('province_id', $this->ilo)->exists())->toBeFalse();
});

it('shows operator B only province B', function () {
    expect(Order::query()->visibleTo($this->operatorB)->pluck('code')->all())->toBe(['FD-ILO-000001'])
        ->and(Rider::query()->visibleTo($this->operatorB)->pluck('id')->all())->toBe([$this->riderB]);
});

it('shows customers, store teams and riders only their own orders', function (int $demoUser, array $codes) {
    expect(Order::query()->visibleTo(demoUser($demoUser))->orderBy('code')->pluck('code')->all())->toBe($codes);
})->with([
    'the ordering customer' => [1, ['FD-ILO-000001', 'FD-NEG-000001']],
    'another customer' => [2, []],
    'the store owner' => [7, ['FD-NEG-000001']],
    'another store owner' => [8, []],
    'the assigned rider' => [3, ['FD-NEG-000001']],
    'another rider' => [4, []],
]);

it('shows platform admins everything', function () {
    expect(Order::query()->visibleTo(demoUser(10))->count())->toBe(2)
        ->and(Store::query()->visibleTo(demoUser(10))->whereKey($this->storeB)->exists())->toBeTrue();
});

it('shows everyone published stores of serviceable provinces, but drafts only to their province', function () {
    expect(Store::query()->published()->count())->toBe(3)
        ->and(Store::query()->visibleTo(demoUser(2))->count())->toBe(3)
        ->and(Store::query()->visibleTo($this->operatorB)->whereKey($this->storeB)->exists())->toBeTrue();
});

it('keeps secret order codes and license numbers out of serialized models', function () {
    DB::table('orders')->where('code', 'FD-NEG-000001')->update(['delivery_otp' => '4321']);
    $rider = Rider::query()->findOrFail(DemoSeeder::riderId(1));
    $rider->license_no_encrypted = 'N01-23-456789';
    $rider->save();

    expect(Order::query()->where('code', 'FD-NEG-000001')->sole()->toArray())->not->toHaveKeys(['delivery_otp', 'pickup_code'])
        ->and($rider->refresh()->toArray())->not->toHaveKey('license_no_encrypted')
        ->and(DB::table('riders')->where('id', $rider->id)->value('license_no_encrypted'))->not->toContain('456789')
        ->and($rider->license_no_encrypted)->toBe('N01-23-456789');
});
