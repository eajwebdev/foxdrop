<?php

use App\Models\OtpCode;
use App\Models\User;
use Illuminate\Support\Facades\DB;
use Illuminate\Testing\TestResponse;

const PHONE = '+639171234567';

function sendCode(string $phone = PHONE): TestResponse
{
    return test()->postJson(route('api.v1.auth.otp.send'), ['phone' => $phone]);
}

function verifyCode(string $code, string $phone = PHONE): TestResponse
{
    return test()->postJson(route('api.v1.auth.otp.verify'), [
        'phone' => $phone, 'code' => $code, 'device_name' => 'Pixel 9', 'full_name' => 'Juan Dela Cruz',
    ]);
}

it('texts a 6-digit code and stores only its hash', function () {
    $sms = fakeSms();

    sendCode()->assertAccepted()->assertExactJson(['expires_in_seconds' => 300, 'resend_after_seconds' => 60]);

    $code = $sms->lastCodeFor(PHONE);
    expect($code)->toMatch('/^\d{6}$/')
        ->and(OtpCode::query()->where('phone', PHONE)->sole()->code_hash)->not->toContain($code);
});

it('rejects numbers that are not Philippine mobiles', function () {
    sendCode('09171234567')->assertUnprocessable()->assertJsonValidationErrors(['phone' => 'Enter a valid Philippine mobile number.']);
});

it('makes the caller wait before sending another code', function () {
    fakeSms();
    $this->freezeTime();
    sendCode()->assertAccepted();

    $this->travel(20)->seconds();
    sendCode()->assertTooManyRequests()->assertJsonPath('retry_after_seconds', 40)->assertHeader('Retry-After', '40');

    $this->travel(41)->seconds();
    sendCode()->assertAccepted();
});

it('signs up a new number as a customer with a working token', function () {
    $sms = fakeSms();
    sendCode();

    $response = verifyCode($sms->lastCodeFor(PHONE))
        ->assertCreated()
        ->assertJsonPath('data.phone', PHONE)
        ->assertJsonPath('data.full_name', 'Juan Dela Cruz')
        ->assertJsonPath('data.roles', [['role' => 'customer', 'province_id' => null, 'store_id' => null]])
        ->assertJsonPath('token_type', 'Bearer');

    $this->withToken($response->json('token'))->getJson(route('api.v1.me'))->assertOk()->assertJsonPath('data.phone', PHONE);
});

it('signs an existing number in without creating a second account', function () {
    $sms = fakeSms();
    $user = User::factory()->create(['phone' => PHONE]);
    sendCode();

    verifyCode($sms->lastCodeFor(PHONE))->assertOk()->assertJsonPath('data.id', $user->id);
    expect(User::query()->where('phone', PHONE)->count())->toBe(1);
});

it('rejects a wrong code without saying why', function () {
    fakeSms();
    sendCode();

    verifyCode('000000')->assertUnprocessable()->assertJsonValidationErrors(['code' => 'That code is wrong or has expired.']);
});

it('accepts a code only once', function () {
    $sms = fakeSms();
    sendCode();
    $code = $sms->lastCodeFor(PHONE);

    verifyCode($code)->assertCreated();
    verifyCode($code)->assertUnprocessable();
});

it('expires a code after 5 minutes', function () {
    $sms = fakeSms();
    $this->freezeTime();
    sendCode();

    $this->travel(301)->seconds();
    verifyCode($sms->lastCodeFor(PHONE))->assertUnprocessable();
});

it('locks a code after 5 wrong attempts', function () {
    $sms = fakeSms();
    sendCode();
    $code = $sms->lastCodeFor(PHONE);

    foreach (range(1, 5) as $attempt) {
        verifyCode($code === '111111' ? '222222' : '111111')->assertUnprocessable();
    }
    verifyCode($code)->assertUnprocessable();
});

it('retires the previous code when a new one is sent', function () {
    $sms = fakeSms();
    $this->freezeTime();
    sendCode();
    $first = $sms->lastCodeFor(PHONE);
    $this->travel(61)->seconds();
    sendCode();

    expect(OtpCode::query()->whereNull('consumed_at')->count())->toBe(1);
    if ($first !== $sms->lastCodeFor(PHONE)) {
        verifyCode($first)->assertUnprocessable();
    }
});

it('uses fixed demo codes without sending an SMS in the testing environment', function () {
    $sms = fakeSms();
    config(['foxdrop.auth.test_otps' => [PHONE => '123456']]);

    sendCode()->assertAccepted();

    expect($sms->sent)->toBe([]);
    verifyCode('123456')->assertCreated();
});

it('refuses suspended accounts', function () {
    $sms = fakeSms();
    User::factory()->suspended()->create(['phone' => PHONE]);
    sendCode();

    verifyCode($sms->lastCodeFor(PHONE))->assertForbidden();
    expect(DB::table('personal_access_tokens')->count())->toBe(0);
});

it('limits how many codes one number can request', function () {
    fakeSms();
    $this->freezeTime();
    foreach (range(1, 5) as $i) {
        sendCode()->assertAccepted();
        $this->travel(61)->seconds();
    }

    sendCode()->assertTooManyRequests();
});

it('answers 422, not a crash, when the phone is sent as a list', function () {
    $this->postJson(route('api.v1.auth.otp.send'), ['phone' => ['+639171234567']])->assertUnprocessable();
});

it('lets many people behind one carrier IP sign in', function () {
    $sms = fakeSms();
    foreach (range(1, 40) as $n) {
        sendCode(sprintf('+6391712%05d', $n))->assertAccepted();
    }

    expect($sms->sent)->toHaveCount(40);
});
