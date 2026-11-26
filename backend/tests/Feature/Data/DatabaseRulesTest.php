<?php

use App\Models\User;
use Database\Seeders\DatabaseSeeder;
use Database\Seeders\DemoSeeder;
use Illuminate\Database\QueryException;
use Illuminate\Support\Facades\DB;

/*
| Business rules the database itself enforces (§5.1, §7, CLAUDE.md "ledger entries are never edited or deleted, and
| each transaction sums to zero"). They hold whatever code path writes the rows.
*/

beforeEach(function () {
    $this->seed(DatabaseSeeder::class);
    $this->neg = (string) DB::table('provinces')->where('psgc_code', DemoSeeder::NEGROS_OCCIDENTAL)->value('id');
});

/**
 * Inserts a ledger transaction with entries of [amounts] (centavos) against the demo rider cash and operator cash
 * wallets, then forces the deferred zero-sum check.
 *
 * @param  list<int>  $amounts
 */
function postLedger(array $amounts, string $key = 'test-txn-0001'): string
{
    $wallets = DB::table('wallets')->whereIn('kind', ['RIDER_CASH', 'OPERATOR_CASH'])->where('province_id', test()->neg)
        ->orderBy('kind')->limit(2)->pluck('id')->all();
    $txn = (string) DB::table('ledger_txns')->insertGetId(['type' => 'TEST_POSTING', 'idempotency_key' => $key, 'province_id' => test()->neg], 'id');
    foreach ($amounts as $i => $amount) {
        DB::table('ledger_entries')->insert(['txn_id' => $txn, 'wallet_id' => $wallets[$i % 2], 'amount_centavos' => $amount, 'type' => 'TEST', 'province_id' => test()->neg]);
    }
    DB::statement('set constraints all immediate');

    return $txn;
}

it('gives every new account the customer role', function () {
    $user = User::factory()->create();

    expect(DB::table('user_roles')->where('user_id', $user->id)->pluck('role')->all())->toBe(['customer']);
});

it('refuses a second live license for the same province', function () {
    DB::table('province_licenses')->insert(['province_id' => $this->neg, 'operator_user_id' => DemoSeeder::userId(2), 'status' => 'APPLIED', 'fee_centavos' => 1_500_000]);
})->throws(QueryException::class, 'province_licenses_one_live');

it('allows a new license once the old one has expired', function () {
    DB::table('province_licenses')->where('province_id', $this->neg)->update(['status' => 'EXPIRED']);

    DB::table('province_licenses')->insert(['province_id' => $this->neg, 'operator_user_id' => DemoSeeder::userId(2), 'status' => 'APPLIED', 'fee_centavos' => 1_500_000]);

    expect(DB::table('province_licenses')->where('province_id', $this->neg)->count())->toBe(2);
});

it('refuses a role without the scope it needs', function (array $row) {
    DB::table('user_roles')->insert(['user_id' => DemoSeeder::userId(1)] + $row);
})->with([
    'operator without province' => [['role' => 'operator']],
    'store owner without store' => [['role' => 'store_owner', 'province_id' => null]],
    'customer with a province' => [['role' => 'customer', 'province_id' => '00000000-0000-0000-0000-000000000000']],
])->throws(QueryException::class);

it('posts a balanced ledger transaction and moves wallet balances', function () {
    postLedger([2500, -2500]);

    expect(DB::table('wallets')->where('province_id', $this->neg)->whereIn('kind', ['RIDER_CASH', 'OPERATOR_CASH'])->sum('balance_centavos'))->toBe(0)
        ->and(DB::table('wallets')->where('province_id', $this->neg)->whereIn('kind', ['RIDER_CASH', 'OPERATOR_CASH'])->where('balance_centavos', '<>', 0)->count())->toBe(2);
});

it('rejects a ledger transaction that does not sum to zero', function () {
    postLedger([2500, -2000]);
})->throws(QueryException::class, 'does not balance');

it('never lets ledger rows change', function (string $sql) {
    postLedger([2500, -2500]);

    DB::statement($sql);
})->with([
    'update an entry' => ['update ledger_entries set amount_centavos = 1'],
    'delete an entry' => ['delete from ledger_entries'],
    'update a transaction' => ["update ledger_txns set memo = 'edited'"],
    'truncate entries' => ['truncate ledger_entries cascade'],
])->throws(QueryException::class, 'append-only');

it('refuses an order whose total does not add up', function () {
    DB::table('orders')->insert([
        'code' => 'FD-NEG-000999', 'province_id' => $this->neg, 'customer_id' => DemoSeeder::userId(1), 'store_id' => DemoSeeder::storeId(1),
        'fulfillment' => 'DELIVERY', 'payment_method' => 'COD', 'status' => 'PLACED',
        'item_subtotal_centavos' => 10000, 'delivery_fee_centavos' => 4000, 'total_centavos' => 99,
    ]);
})->throws(QueryException::class, 'orders_total');

it('refuses a store-owned row that claims another province', function () {
    $ilo = (string) DB::table('provinces')->where('psgc_code', '0603000000')->value('id');

    DB::table('products')->insert(['store_id' => DemoSeeder::storeId(1), 'province_id' => $ilo, 'name' => 'Smuggled', 'price_centavos' => 100]);
})->throws(QueryException::class, 'products_store_fk');

it('refuses a ledger entry that claims another province than its wallet', function () {
    $ilo = (string) DB::table('provinces')->where('psgc_code', '0603000000')->value('id');
    $wallet = DB::table('wallets')->where('province_id', $this->neg)->where('kind', 'RIDER_CASH')->value('id');
    $txn = DB::table('ledger_txns')->insertGetId(['type' => 'TEST_POSTING', 'idempotency_key' => 'test-txn-province', 'province_id' => $ilo], 'id');

    DB::table('ledger_entries')->insert(['txn_id' => $txn, 'wallet_id' => $wallet, 'amount_centavos' => 100, 'type' => 'TEST', 'province_id' => $ilo]);
})->throws(QueryException::class, 'does not match wallet province');

it('refuses a favourite that claims another province than its store', function () {
    $ilo = (string) DB::table('provinces')->where('psgc_code', '0603000000')->value('id');

    DB::table('favorites')->insert(['user_id' => DemoSeeder::userId(1), 'province_id' => $ilo, 'store_id' => DemoSeeder::storeId(1)]);
})->throws(QueryException::class, 'favorites_store_fk');
