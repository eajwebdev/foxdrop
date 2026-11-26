<?php

use App\Enums\AppRole;
use App\Models\User;
use Illuminate\Support\Facades\DB;

it('requires a token for the profile', function () {
    $this->getJson(route('api.v1.me'))->assertUnauthorized();
});

it('returns the user with every role, oldest first', function () {
    $province = DB::table('provinces')->insertGetId(['psgc_code' => '0604500000', 'name' => 'Negros Occidental', 'region_code' => '0600000000', 'region_name' => 'Region VI'], 'id');
    $user = User::factory()->withRole(AppRole::Operator, $province)->create();

    $this->withHeaders(bearer($user))->getJson(route('api.v1.me'))
        ->assertOk()
        ->assertExactJson(['data' => [
            'id' => $user->id, 'full_name' => $user->full_name, 'phone' => $user->phone, 'email' => null, 'avatar_url' => null,
            'roles' => [
                ['role' => 'customer', 'province_id' => null, 'store_id' => null],
                ['role' => 'operator', 'province_id' => $province, 'store_id' => null],
            ],
        ]]);
});

it('signs out only this device', function () {
    $user = User::factory()->create();
    $thisDevice = bearer($user);
    $otherDevice = bearer($user);

    $this->withHeaders($thisDevice)->postJson(route('api.v1.auth.logout'))->assertNoContent();

    app('auth')->forgetGuards();
    $this->withHeaders($thisDevice)->getJson(route('api.v1.me'))->assertUnauthorized();
    app('auth')->forgetGuards();
    $this->withHeaders($otherDevice)->getJson(route('api.v1.me'))->assertOk();
});

it('stops accepting a token after it expires', function () {
    $user = User::factory()->create();
    $token = $user->createToken('Pixel', ['*'], now()->addMinute())->plainTextToken;

    $this->travel(2)->minutes();

    $this->withToken($token)->getJson(route('api.v1.me'))->assertUnauthorized();
});

it('locks a suspended account out of the API', function () {
    $user = User::factory()->create();
    $headers = bearer($user);
    $user->forceFill(['status' => User::STATUS_SUSPENDED])->save();

    $this->withHeaders($headers)->getJson(route('api.v1.me'))->assertForbidden();
});
