<?php

namespace App\Http\Controllers\Api\V1\Operator;

use App\Actions\Territory\UpdateProvinceStoreFee;
use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\Operator\UpdateProvinceSettingsRequest;
use App\Models\User;
use App\Support\OperatorContext;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

/**
 * Province base settings the operator controls (§9.4 steps 3–4): the monthly store fee for now.
 */
class ProvinceSettingsController extends Controller
{
    public function show(Request $request): JsonResponse
    {
        /** @var User $user */
        $user = $request->user();

        return $this->respond(OperatorContext::for($user)->provinceId);
    }

    public function update(UpdateProvinceSettingsRequest $request, UpdateProvinceStoreFee $updateStoreFee): JsonResponse
    {
        /** @var User $user */
        $user = $request->user();
        $operator = OperatorContext::for($user);
        $updateStoreFee($operator, $request->integer('store_fee_centavos'));

        return $this->respond($operator->provinceId);
    }

    private function respond(string $provinceId): JsonResponse
    {
        $province = DB::table('provinces')
            ->where('id', $provinceId)
            ->select(['id', 'name', 'store_fee_centavos', 'store_fee_grace_days'])
            ->selectRaw('ST_Y(center::geometry) as lat, ST_X(center::geometry) as lng')
            ->first();
        abort_if($province === null, 404);

        return response()->json(['data' => [
            'id' => $province->id,
            'name' => $province->name,
            'center' => $province->lat === null ? null : ['lat' => (float) $province->lat, 'lng' => (float) $province->lng],
            'store_fee_centavos' => (int) $province->store_fee_centavos,
            'store_fee_grace_days' => (int) $province->store_fee_grace_days,
        ]]);
    }
}
