<?php

use App\Actions\Licensing\ReviewOperatorApplication;
use App\Actions\Licensing\SubmitOperatorApplication;
use App\Models\Payment;
use App\Models\ProvinceLicense;
use App\Models\User;
use Database\Seeders\DemoSeeder;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Storage;
use Illuminate\Testing\TestResponse;

const WEBHOOK_SECRET = 'whsk_test_secret_for_foxdrop';

beforeEach(function () {
    Storage::fake('kyc');
    config(['services.paymongo.webhook_secret' => WEBHOOK_SECRET]);
});

/**
 * An applicant for Iloilo whose application an admin approved: the license waits for payment.
 *
 * @return array{User, ProvinceLicense}
 */
function approvedApplicant(): array
{
    $applicant = User::factory()->create();
    $application = app(SubmitOperatorApplication::class)($applicant, [
        'province_id' => provinceId(ILOILO), 'business_name' => 'Iloilo Swift', 'contact_phone' => '+639171112222',
    ], photo(), null);
    app(ReviewOperatorApplication::class)(demoUser(10), $application, approve: true);

    return [$applicant, ProvinceLicense::query()->where('application_id', $application->id)->sole()];
}

function startPayment(User $payer, ProvinceLicense $license): TestResponse
{
    return test()->withHeaders(bearer($payer))->postJson(route('api.v1.licenses.payments.store', $license));
}

/**
 * A PayMongo webhook request, signed like PayMongo does.
 *
 * @param  array<string, mixed>  $event
 */
function paymongoWebhook(array $event, ?int $timestamp = null, ?string $secret = null): TestResponse
{
    $body = json_encode($event, JSON_THROW_ON_ERROR);
    $t = $timestamp ?? time();
    $signature = hash_hmac('sha256', $t.'.'.$body, $secret ?? WEBHOOK_SECRET);

    return test()->call('POST', route('api.v1.webhooks.paymongo'), [], [], [], [
        'CONTENT_TYPE' => 'application/json',
        'HTTP_ACCEPT' => 'application/json',
        'HTTP_PAYMONGO_SIGNATURE' => "t={$t},te={$signature},li=",
    ], $body);
}

/**
 * @return array<string, mixed>
 */
function paidEvent(string $checkoutSessionId, string $eventId = 'evt_test_1'): array
{
    return ['data' => ['id' => $eventId, 'type' => 'event', 'attributes' => [
        'type' => 'checkout_session.payment.paid',
        'livemode' => false,
        'data' => ['id' => $checkoutSessionId, 'type' => 'checkout_session', 'attributes' => ['metadata' => []]],
    ]]];
}

it('starts a license payment and reuses the open checkout', function () {
    [$applicant, $license] = approvedApplicant();

    $first = startPayment($applicant, $license)
        ->assertCreated()
        ->assertJsonPath('data.purpose', 'LICENSE')
        ->assertJsonPath('data.status', 'PENDING')
        ->assertJsonPath('data.amount_centavos', 1_500_000);

    expect($first->json('data.checkout_url'))->toContain('/dev/checkout/')
        ->and(startPayment($applicant, $license)->json('data.id'))->toBe($first->json('data.id'));
});

it('lets only the license holder pay, and only when payable', function () {
    [, $license] = approvedApplicant();
    startPayment(User::factory()->create(), $license)->assertNotFound();

    $applicant = User::factory()->create();
    $application = app(SubmitOperatorApplication::class)($applicant, ['province_id' => provinceId('0102800000'), 'business_name' => 'Ilocos Go', 'contact_phone' => '+639171112223'], photo(), null);
    startPayment($applicant, ProvinceLicense::query()->where('application_id', $application->id)->sole())->assertConflict();
});

it('activates the license for a year when the test checkout is paid', function () {
    $this->freezeSecond();
    [$applicant, $license] = approvedApplicant();
    $payment = startPayment($applicant, $license)->json('data');

    $this->post(route('dev.checkout.pay', $payment['id']))->assertOk()->assertSee('Payment paid');
    $this->post(route('dev.checkout.pay', $payment['id']))->assertOk(); // a double tap changes nothing

    $license->refresh();
    expect($license->status)->toBe('ACTIVE')
        ->and($license->start_at?->equalTo(now()))->toBeTrue()
        ->and($license->end_at?->equalTo(now()->addDays(365)))->toBeTrue();

    $this->withHeaders(bearer($applicant))->getJson(route('api.v1.payments.show', $payment['id']))->assertJsonPath('data.status', 'PAID');
    $this->withHeaders(bearer($applicant))->getJson(route('api.v1.me'))
        ->assertJsonFragment(['role' => 'operator', 'province_id' => provinceId(ILOILO), 'store_id' => null]);
    $this->withHeaders(bearer($applicant))->getJson(route('api.v1.operator.license'))
        ->assertJsonPath('data.status', 'ACTIVE')->assertJsonPath('data.days_left', 365);

    // Ledger rule 7: GATEWAY_CLEARING −fee, PLATFORM_REVENUE +fee, once.
    $entries = DB::table('ledger_entries')->join('ledger_txns', 'ledger_txns.id', '=', 'ledger_entries.txn_id')
        ->where('ledger_txns.idempotency_key', 'license-payment:'.$payment['id'])->pluck('amount_centavos')->sort()->values()->all();
    expect($entries)->toBe([-1_500_000, 1_500_000])
        ->and(DB::table('wallets')->where('owner_id', $applicant->id)->pluck('kind')->sort()->values()->all())->toBe(['OPERATOR_CASH', 'OPERATOR_REVENUE']);

    $iloilo = collect($this->getJson(route('api.v1.provinces.index'))->json('data'))->firstWhere('psgc_code', ILOILO);
    expect($iloilo['is_serviceable'])->toBeTrue();
});

it('keeps the license waiting when the test checkout is cancelled', function () {
    [$applicant, $license] = approvedApplicant();
    $payment = startPayment($applicant, $license)->json('data');

    $this->post(route('dev.checkout.cancel', $payment['id']))->assertOk();

    expect(Payment::query()->findOrFail($payment['id'])->status)->toBe('FAILED')
        ->and($license->refresh()->status)->toBe('PENDING_PAYMENT')
        ->and(startPayment($applicant, $license)->json('data.id'))->not->toBe($payment['id']);
});

it('activates the license from a signed PayMongo webhook, once', function () {
    config(['foxdrop.payments.driver' => 'paymongo', 'services.paymongo.secret_key' => 'sk_test_123']);
    Http::fake(['api.paymongo.com/v1/checkout_sessions' => Http::response(['data' => [
        'id' => 'cs_test_abc', 'attributes' => ['checkout_url' => 'https://checkout.paymongo.com/cs_test_abc'],
    ]])]);
    [$applicant, $license] = approvedApplicant();

    startPayment($applicant, $license)->assertCreated()->assertJsonPath('data.checkout_url', 'https://checkout.paymongo.com/cs_test_abc');
    Http::assertSent(fn ($request) => $request['data']['attributes']['line_items'][0]['amount'] === 1_500_000
        && is_string($request['data']['attributes']['metadata']['payment_id']));

    paymongoWebhook(paidEvent('cs_test_abc'))->assertOk()->assertJsonPath('status', 'ok');
    paymongoWebhook(paidEvent('cs_test_abc'))->assertOk()->assertJsonPath('status', 'duplicate');

    expect($license->refresh()->status)->toBe('ACTIVE')
        ->and(DB::table('ledger_txns')->where('type', 'LICENSE_PAYMENT')->count())->toBe(1);
});

it('refuses webhooks that PayMongo did not sign', function (string $case) {
    $event = paidEvent('cs_test_abc');

    match ($case) {
        'wrong secret' => paymongoWebhook($event, secret: 'not-the-secret')->assertUnauthorized(),
        'too old' => paymongoWebhook($event, timestamp: time() - 600)->assertUnauthorized(),
        'no header' => $this->postJson(route('api.v1.webhooks.paymongo'), $event)->assertUnauthorized(),
    };
    expect(DB::table('webhook_events')->count())->toBe(0);
})->with(['wrong secret', 'too old', 'no header']);

it('answers 502 when PayMongo cannot be reached', function () {
    config(['foxdrop.payments.driver' => 'paymongo', 'services.paymongo.secret_key' => 'sk_test_123']);
    Http::fake(['api.paymongo.com/*' => Http::response('down', 500)]);
    [$applicant, $license] = approvedApplicant();

    startPayment($applicant, $license)->assertStatus(502);
});

it('renews an active license by a year from its end date', function () {
    $this->freezeSecond();
    [$applicant, $license] = approvedApplicant();
    $this->post(route('dev.checkout.pay', startPayment($applicant, $license)->json('data.id')));
    $firstEnd = $license->refresh()->end_at;

    $this->travel(300)->days();
    $this->post(route('dev.checkout.pay', startPayment($applicant, $license)->json('data.id')));

    expect($license->refresh()->end_at?->equalTo($firstEnd?->copy()->addDays(365)))->toBeTrue()
        ->and(DB::table('ledger_txns')->where('type', 'LICENSE_PAYMENT')->count())->toBe(2);
});

it('moves licenses through grace to expired, and renewing in grace starts from today', function () {
    $this->freezeSecond();
    [$applicant, $license] = approvedApplicant();
    $this->post(route('dev.checkout.pay', startPayment($applicant, $license)->json('data.id')));

    $this->travel(366)->days();
    $this->artisan('foxdrop:licenses:advance')->expectsOutputToContain('entered grace')->assertSuccessful();
    expect($license->refresh()->status)->toBe('GRACE');
    $this->withHeaders(bearer($applicant))->getJson(route('api.v1.operator.license'))
        ->assertJsonPath('data.status', 'GRACE')->assertJsonPath('data.days_left', -1)->assertJsonPath('data.can_pay', true);

    $this->post(route('dev.checkout.pay', startPayment($applicant, $license)->json('data.id')));
    expect($license->refresh()->status)->toBe('ACTIVE')
        ->and($license->end_at?->equalTo(now()->addDays(365)))->toBeTrue();
});

it('expires a license after the grace days and hides the province from customers', function () {
    $neg = ProvinceLicense::query()->where('province_id', provinceId(DemoSeeder::NEGROS_OCCIDENTAL))->where('status', 'ACTIVE')->sole();
    $neg->forceFill(['end_at' => now()->subDays(16)])->save();

    $this->artisan('foxdrop:licenses:advance')->assertSuccessful(); // ACTIVE → GRACE
    $this->artisan('foxdrop:licenses:advance')->assertSuccessful(); // GRACE past 15 days → EXPIRED

    expect($neg->refresh()->status)->toBe('EXPIRED');
    $negros = collect($this->getJson(route('api.v1.provinces.index'))->json('data'))->firstWhere('psgc_code', DemoSeeder::NEGROS_OCCIDENTAL);
    expect($negros['is_serviceable'])->toBeFalse()->and($negros['is_available'])->toBeTrue();
});

it('shows a payment only to its payer', function () {
    [$applicant, $license] = approvedApplicant();
    $id = startPayment($applicant, $license)->json('data.id');

    $this->withHeaders(bearer(User::factory()->create()))->getJson(route('api.v1.payments.show', $id))->assertNotFound();
});
