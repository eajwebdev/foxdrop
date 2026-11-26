<?php

namespace App\Http\Controllers\Api\V1\Partners;

use App\Actions\Partners\SubmitStore;
use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\Partners\SubmitStoreRequest;
use App\Http\Resources\MyStoreResource;
use App\Models\Store;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;

/**
 * "Open a store" in the business app: apply, then follow the application.
 */
class StoreApplicationController extends Controller
{
    public function store(SubmitStoreRequest $request, SubmitStore $submit): JsonResponse
    {
        /** @var User $user */
        $user = $request->user();
        $optional = fn (string $key): ?string => $request->filled($key) ? $request->string($key)->toString() : null;
        $details = [
            'province_id' => $request->string('province_id')->toString(),
            'name' => $request->string('name')->toString(),
            'category' => $request->string('category')->toString(),
            'description' => $optional('description'),
            'address_line' => $request->string('address_line')->toString(),
            'barangay' => $optional('barangay'),
            'city' => $request->string('city')->toString(),
            'phone' => $optional('phone'),
            'latitude' => $request->float('latitude'),
            'longitude' => $request->float('longitude'),
        ];
        $store = $submit($user, $details, [
            'BUSINESS_PERMIT' => $request->file('business_permit'),
            'VALID_ID' => $request->file('valid_id'),
        ]);

        return MyStoreResource::make($store->load('province'))->response()->setStatusCode(201);
    }

    public function mine(Request $request): AnonymousResourceCollection
    {
        return MyStoreResource::collection(
            Store::query()->with('province')
                ->where('owner_user_id', $request->user()?->id)
                ->orderByDesc('created_at')
                ->orderBy('id')
                ->get(),
        );
    }
}
