<?php

namespace App\Http\Controllers\Api\V1\Operator;

use App\Actions\Territory\CreateDeliveryZone;
use App\Actions\Territory\DeactivateDeliveryZone;
use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\Operator\CreateZoneRequest;
use App\Http\Resources\ZoneResource;
use App\Models\DeliveryZone;
use App\Models\User;
use App\Support\OperatorContext;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;
use Illuminate\Http\Response;

/**
 * Coverage zones of the operator's province (§9.4 step 3, §6).
 */
class ZoneController extends Controller
{
    public function index(Request $request): AnonymousResourceCollection
    {
        $operator = OperatorContext::for($this->user($request));

        return ZoneResource::collection(
            DeliveryZone::query()->withGeometry()
                ->where('province_id', $operator->provinceId)
                ->where('is_active', true)
                ->orderBy('created_at')
                ->orderBy('id')
                ->get(),
        );
    }

    public function store(CreateZoneRequest $request, CreateDeliveryZone $create): JsonResponse
    {
        $operator = OperatorContext::for($this->user($request));
        /** @var array{lat: float, lng: float}|null $center */
        $center = $request->has('center') ? ['lat' => (float) $request->input('center.lat'), 'lng' => (float) $request->input('center.lng')] : null;
        /** @var list<array{0: float, 1: float}>|null $polygon */
        $polygon = $request->has('polygon')
            ? array_map(fn (array $point): array => [(float) $point[0], (float) $point[1]], (array) $request->input('polygon'))
            : null;

        $zone = $create($operator, $request->string('name')->toString(), $center, $request->has('radius_km') ? (float) $request->input('radius_km') : null, $polygon);

        return ZoneResource::make(DeliveryZone::query()->withGeometry()->findOrFail($zone->id))->response()->setStatusCode(201);
    }

    public function destroy(Request $request, string $zone, DeactivateDeliveryZone $deactivate): Response
    {
        $deactivate(OperatorContext::for($this->user($request)), $zone);

        return response()->noContent();
    }

    private function user(Request $request): User
    {
        /** @var User */
        return $request->user();
    }
}
