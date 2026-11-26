<?php

namespace App\Http\Controllers\Api\V1\Partners;

use App\Actions\Partners\SubmitRiderApplication;
use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\Partners\SubmitRiderRequest;
use App\Http\Resources\MyRiderResource;
use App\Models\Rider;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Rider registration (§9.2 step 1) and its status screen.
 */
class RiderApplicationController extends Controller
{
    private const array DOCUMENTS = [
        'valid_id' => 'VALID_ID', 'drivers_license' => 'DRIVERS_LICENSE', 'selfie' => 'SELFIE',
        'or_cr' => 'OR_CR', 'vehicle_photo' => 'VEHICLE_PHOTO',
    ];

    public function store(SubmitRiderRequest $request, SubmitRiderApplication $submit): JsonResponse
    {
        /** @var User $user */
        $user = $request->user();
        $documents = [];
        foreach (self::DOCUMENTS as $field => $type) {
            $file = $request->file($field);
            if ($file !== null && ! is_array($file)) {
                $documents[$type] = $file;
            }
        }
        $details = [
            'province_id' => $request->string('province_id')->toString(),
            'full_name' => $request->string('full_name')->toString(),
            'vehicle_type' => $request->string('vehicle_type')->toString(),
            'plate_no' => $request->filled('plate_no') ? $request->string('plate_no')->toString() : null,
            'license_no' => $request->string('license_no')->toString(),
            'license_expiry' => $request->string('license_expiry')->toString(),
        ];

        return MyRiderResource::make($submit($user, $details, $documents)->load('province'))->response()->setStatusCode(201);
    }

    public function me(Request $request): JsonResponse
    {
        $rider = Rider::query()->with('province')->where('user_id', $request->user()?->id)->first();

        return response()->json(['data' => $rider === null ? null : MyRiderResource::make($rider)]);
    }
}
