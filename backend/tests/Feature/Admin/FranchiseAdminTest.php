<?php

use App\Actions\Licensing\ReviewOperatorApplication;
use App\Enums\AdminPermission;
use App\Models\OperatorApplication;
use App\Models\Payment;
use App\Models\ProvinceLicense;
use App\Models\User;
use Database\Seeders\DemoSeeder;
use Illuminate\Support\Facades\DB;
use Inertia\Testing\AssertableInertia as Assert;
use Symfony\Component\HttpKernel\Exception\HttpException;

/**
 * Franchises in the admin web (§9.5 step 3): operator applications, assigning, paying offline, suspending.
 */
function auditRow(string $action, string $entityId): ?object
{
    return DB::table('audit_logs')->where('action', $action)->where('entity_id', $entityId)->latest('id')->first();
}

function ledgerSumFor(string $paymentId): int
{
    return (int) DB::table('ledger_entries')
        ->join('ledger_txns', 'ledger_txns.id', '=', 'ledger_entries.txn_id')
        ->where('ledger_txns.idempotency_key', 'license-payment:'.$paymentId)
        ->sum('ledger_entries.amount_centavos');
}

it('lists every province with its license', function () {
    $this->actingAs(superAdmin(), 'web')->get('/admin/franchises')
        ->assertInertia(fn (Assert $page) => $page
            ->component('franchises/index')
            ->missing('provinces') // deferred: the page shell first
            ->loadDeferredProps(fn (Assert $reload) => $reload
                ->has('provinces', 83)
                ->where('provinces', fn ($provinces) => collect($provinces)->firstWhere('name', 'Negros Occidental')['license']['status'] === 'ACTIVE')));
});

it('approves an operator application and the applicant sees it in the app right away (W1 acceptance)', function () {
    $admin = superAdmin();
    $applicant = demoUser(13);

    $this->getJson('/api/v1/operator/applications/mine', bearer($applicant))->assertJsonPath('data.status', 'SUBMITTED');

    $this->actingAs($admin, 'web')->get('/admin/operator-applications')
        ->assertInertia(fn (Assert $page) => $page->component('franchises/applications/index')
            ->loadDeferredProps(fn (Assert $reload) => $reload->where('applications.data.0.id', DemoSeeder::CEBU_APPLICATION)));
    $this->get('/admin/operator-applications/'.DemoSeeder::CEBU_APPLICATION)
        ->assertInertia(fn (Assert $page) => $page->component('franchises/applications/show')->has('application.documents', 2));
    $this->post('/admin/operator-applications/'.DemoSeeder::CEBU_APPLICATION.'/approve')->assertRedirect();

    // The same API the Android app polls (DECISIONS 80) now offers payment.
    $this->getJson('/api/v1/operator/applications/mine', bearer($applicant))
        ->assertJsonPath('data.status', 'APPROVED')
        ->assertJsonPath('data.license.status', 'PENDING_PAYMENT')
        ->assertJsonPath('data.license.can_pay', true);
    expect(auditRow('operator_application.approved', DemoSeeder::CEBU_APPLICATION)?->actor_user_id)->toBe($admin->id);
});

it('rejects an operator application with a reason and frees the province', function () {
    $this->actingAs(superAdmin(), 'web')
        ->post('/admin/operator-applications/'.DemoSeeder::CEBU_APPLICATION.'/reject', ['reason' => 'Business permit is not readable.'])
        ->assertRedirect();

    $this->getJson('/api/v1/operator/applications/mine', bearer(demoUser(13)))
        ->assertJsonPath('data.status', 'REJECTED')
        ->assertJsonPath('data.rejection_reason', 'Business permit is not readable.');
    expect(DB::table('province_directory')->where('id', provinceId('0702200000'))->value('is_available'))->toBeTrue();
});

it('needs the franchises permission to review, also through the shared Action', function () {
    $viewer = adminUser(permissions: [AdminPermission::FranchisesView]);

    $this->actingAs($viewer, 'web')->get('/admin/operator-applications/'.DemoSeeder::CEBU_APPLICATION)->assertOk();
    $this->postJson('/admin/operator-applications/'.DemoSeeder::CEBU_APPLICATION.'/approve')->assertForbidden();

    expect(fn () => app(ReviewOperatorApplication::class)(
        $viewer, OperatorApplication::query()->findOrFail(DemoSeeder::CEBU_APPLICATION), true,
    ))->toThrow(HttpException::class);
});

it('assigns a free province, records the offline payment and activates the license with ledger rule 7', function () {
    $admin = superAdmin();
    $operator = User::factory()->create();
    $aklan = provinceId('0600400000');
    $this->actingAs($admin, 'web')->withSession(confirmedSession());

    $this->post("/admin/franchises/{$aklan}/license", ['operator' => '0'.substr((string) $operator->phone, 3)])->assertSessionHasNoErrors();
    $license = ProvinceLicense::query()->where('province_id', $aklan)->sole();
    expect($license->status)->toBe('PENDING_PAYMENT')
        ->and(auditRow('license.assigned', $license->id))->not->toBeNull();

    $this->post("/admin/licenses/{$license->id}/payments", ['method' => 'BANK_TRANSFER', 'reference' => 'BDO-20261126-0042'])->assertSessionHasNoErrors();

    $license->refresh();
    $payment = Payment::query()->where('license_id', $license->id)->sole();
    expect($license->status)->toBe('ACTIVE')
        ->and((int) round($license->start_at?->diffInDays($license->end_at)))->toBe(365)
        ->and($payment->provider)->toBe('manual')
        ->and($payment->status)->toBe('PAID')
        ->and(ledgerSumFor($payment->id))->toBe(0)
        ->and(DB::table('wallets')->where('kind', 'OFFLINE_RECEIPTS')->value('balance_centavos'))->toBe(-1_500_000)
        ->and($operator->fresh()?->operatorProvinceIds())->toBe([$aklan])
        ->and(auditRow('license.offline_payment_recorded', $license->id)?->actor_user_id)->toBe($admin->id);

    // The same bank reference cannot be recorded twice.
    $this->post("/admin/licenses/{$license->id}/payments", ['method' => 'BANK_TRANSFER', 'reference' => 'BDO-20261126-0042'])->assertRedirect();
    expect(Payment::query()->where('license_id', $license->id)->count())->toBe(1);
});

it('renews a running license by a year from its end date', function () {
    $license = ProvinceLicense::query()->where('province_id', provinceId(DemoSeeder::NEGROS_OCCIDENTAL))->where('status', 'ACTIVE')->sole();
    $endBefore = $license->end_at;

    $this->actingAs(superAdmin(), 'web')->withSession(confirmedSession())
        ->post("/admin/licenses/{$license->id}/payments", ['method' => 'CASH', 'reference' => 'OR-1001'])
        ->assertSessionHasNoErrors();

    expect($license->refresh()->end_at?->toDateString())->toBe($endBefore?->copy()->addDays(365)->toDateString());
});

it('will not assign a taken province or give a second province to one operator', function () {
    $admin = superAdmin();
    $this->actingAs($admin, 'web')->withSession(confirmedSession());

    $this->post('/admin/franchises/'.provinceId(DemoSeeder::NEGROS_OCCIDENTAL).'/license', ['operator' => (string) User::factory()->create()->phone])->assertRedirect();
    $this->post('/admin/franchises/'.provinceId('0600400000').'/license', ['operator' => (string) demoUser(6)->phone])->assertRedirect();
    $this->post('/admin/franchises/'.provinceId('0600400000').'/license', ['operator' => 'nobody@example.com'])->assertSessionHasErrors('operator');

    expect(ProvinceLicense::query()->where('province_id', provinceId('0600400000'))->exists())->toBeFalse();
});

it('suspends a license: customers lose the province, the operator turns read-only and nobody else can take it', function () {
    $admin = superAdmin();
    $neg = provinceId(DemoSeeder::NEGROS_OCCIDENTAL);
    $license = ProvinceLicense::query()->where('province_id', $neg)->where('status', 'ACTIVE')->sole();

    $this->actingAs($admin, 'web')->withSession(confirmedSession())
        ->post("/admin/licenses/{$license->id}/suspend", ['reason' => 'Unpaid rider remittances'])
        ->assertSessionHasNoErrors();

    expect($license->refresh()->status)->toBe('SUSPENDED')
        ->and($license->status_reason)->toBe('Unpaid rider remittances')
        ->and(DB::table('province_directory')->where('id', $neg)->value('is_serviceable'))->toBeFalse()
        ->and(DB::table('province_directory')->where('id', $neg)->value('is_available'))->toBeFalse()
        ->and(auditRow('license.suspended', $license->id))->not->toBeNull();
    $this->postJson('/api/v1/operator/zones', ['name' => 'Town', 'center' => ['lat' => 10.67, 'lng' => 122.95], 'radius_km' => 3], bearer(demoUser(6)))
        ->assertForbidden();

    $this->actingAs($admin, 'web')->post("/admin/licenses/{$license->id}/reinstate")->assertSessionHasNoErrors();
    expect($license->refresh()->status)->toBe('ACTIVE')
        ->and($license->status_reason)->toBeNull()
        ->and(DB::table('province_directory')->where('id', $neg)->value('is_serviceable'))->toBeTrue();
});
