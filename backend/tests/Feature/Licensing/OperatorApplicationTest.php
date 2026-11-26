<?php

use App\Models\OperatorApplication;
use App\Models\ProvinceLicense;
use App\Models\User;
use Database\Seeders\DemoSeeder;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;
use Illuminate\Testing\TestResponse;

/**
 * @param  array<string, mixed>  $overrides
 */
function applyForProvince(User $applicant, string $psgcCode = ILOILO, array $overrides = []): TestResponse
{
    return test()->withHeaders(bearer($applicant))->post(route('api.v1.operator.applications.store'), $overrides + [
        'province_id' => provinceId($psgcCode),
        'business_name' => 'Iloilo Swift Logistics',
        'registration_number' => 'DTI-0630-000999',
        'contact_phone' => '+639171112222',
        'contact_email' => 'ops@swift.example.com',
        'valid_id' => photo('valid-id.jpg'),
        'business_permit' => photo('permit.jpg'),
    ], ['Accept' => 'application/json']);
}

beforeEach(fn () => Storage::fake('kyc'));

it('applies for an available province and blocks it for others', function () {
    $applicant = User::factory()->create();

    applyForProvince($applicant)
        ->assertCreated()
        ->assertJsonPath('data.status', 'SUBMITTED')
        ->assertJsonPath('data.province.name', 'Iloilo')
        ->assertJsonPath('data.license.status', 'APPLIED')
        ->assertJsonPath('data.license.can_pay', false);

    $iloilo = collect($this->getJson(route('api.v1.provinces.index'))->json('data'))->firstWhere('psgc_code', ILOILO);
    expect($iloilo['is_available'])->toBeFalse()
        ->and(Storage::disk('kyc')->allFiles('operator-applications/'.$applicant->id))->toHaveCount(2);
});

it('makes applying for an already-licensed province impossible', function () {
    applyForProvince(User::factory()->create(), DemoSeeder::NEGROS_OCCIDENTAL)
        ->assertConflict()
        ->assertJsonPath('message', 'This province already has an operator.');
});

it('lets only the first applicant hold a province', function () {
    applyForProvince(User::factory()->create())->assertCreated();

    applyForProvince(User::factory()->create())->assertConflict()->assertJsonPath('message', 'This province already has an operator.');
    expect(Storage::disk('kyc')->allFiles())->toHaveCount(2); // the second applicant's photos were not kept
});

it('allows one open application per person', function () {
    $applicant = User::factory()->create();
    applyForProvince($applicant)->assertCreated();

    applyForProvince($applicant, '0102800000')->assertConflict()->assertJsonPath('message', 'You already have an open application or a license.');
});

it('validates the application', function (array $overrides, string $field) {
    applyForProvince(User::factory()->create(), ILOILO, $overrides)->assertUnprocessable()->assertJsonValidationErrors($field);
})->with([
    'no valid ID' => [['valid_id' => null], 'valid_id'],
    'a PDF instead of a photo' => [['valid_id' => UploadedFile::fake()->create('id.pdf', 10, 'application/pdf')], 'valid_id'],
    'a landline' => [['contact_phone' => '0342123456'], 'contact_phone'],
    'unknown province' => [['province_id' => '00000000-0000-0000-0000-000000000000'], 'province_id'],
]);

it('re-encodes uploaded photos so camera metadata is dropped', function () {
    $applicant = User::factory()->create();
    applyForProvince($applicant)->assertCreated();

    $path = OperatorApplication::query()->where('applicant_user_id', $applicant->id)->value('valid_id_path');
    $bytes = Storage::disk('kyc')->get($path);
    expect(substr((string) $bytes, 0, 3))->toBe("\xFF\xD8\xFF")
        ->and((string) $bytes)->not->toContain('Exif');
});

it('shows the applicant their latest application', function () {
    $applicant = User::factory()->create();
    $this->withHeaders(bearer($applicant))->getJson(route('api.v1.operator.applications.mine'))->assertOk()->assertExactJson(['data' => null]);

    applyForProvince($applicant);

    $this->withHeaders(bearer($applicant))->getJson(route('api.v1.operator.applications.mine'))
        ->assertOk()
        ->assertJsonPath('data.business_name', 'Iloilo Swift Logistics')
        ->assertJsonStructure(['data' => ['id', 'status', 'province' => ['id', 'name'], 'business_name', 'rejection_reason', 'created_at', 'license' => ['id', 'province', 'status', 'start_at', 'end_at', 'days_left', 'fee_centavos', 'can_pay']]]);
});

it('requires sign-in', function () {
    $this->postJson(route('api.v1.operator.applications.store'))->assertUnauthorized();
});

it('lets an admin approve an application, which then waits for payment', function () {
    $applicant = User::factory()->create();
    applyForProvince($applicant);
    $application = OperatorApplication::query()->where('applicant_user_id', $applicant->id)->sole();

    $this->artisan('foxdrop:operator-application', ['action' => 'approve', 'id' => $application->id])->assertSuccessful();

    expect($application->refresh()->status)->toBe('APPROVED')
        ->and(ProvinceLicense::query()->where('application_id', $application->id)->value('status'))->toBe('PENDING_PAYMENT')
        ->and(DB::table('audit_logs')->where('action', 'operator_application.approved')->exists())->toBeTrue();

    $this->withHeaders(bearer($applicant))->getJson(route('api.v1.operator.license'))
        ->assertOk()->assertJsonPath('data.status', 'PENDING_PAYMENT')->assertJsonPath('data.can_pay', true);
});

it('frees the province when an application is rejected', function () {
    $applicant = User::factory()->create();
    applyForProvince($applicant);
    $application = OperatorApplication::query()->where('applicant_user_id', $applicant->id)->sole();

    $this->artisan('foxdrop:operator-application', ['action' => 'reject', 'id' => $application->id])->assertExitCode(2);
    $this->artisan('foxdrop:operator-application', ['action' => 'reject', 'id' => $application->id, '--reason' => 'Business permit is not readable.'])->assertSuccessful();

    $this->withHeaders(bearer($applicant))->getJson(route('api.v1.operator.applications.mine'))
        ->assertJsonPath('data.status', 'REJECTED')
        ->assertJsonPath('data.rejection_reason', 'Business permit is not readable.');
    applyForProvince(User::factory()->create())->assertCreated();
});

it('reviews an application only once', function () {
    $applicant = User::factory()->create();
    applyForProvince($applicant);
    $id = OperatorApplication::query()->where('applicant_user_id', $applicant->id)->value('id');
    $this->artisan('foxdrop:operator-application', ['action' => 'approve', 'id' => $id])->assertSuccessful();

    $this->artisan('foxdrop:operator-application', ['action' => 'approve', 'id' => $id])
        ->expectsOutputToContain('This application was already reviewed.')
        ->assertFailed();
});
