<?php

namespace App\Http\Controllers\Api\V1\Ordering;

use App\Http\Controllers\Controller;
use App\Http\Resources\ProductResource;
use App\Http\Resources\StoreCardResource;
use App\Http\Resources\StoreMenuResource;
use App\Models\DeliveryZone;
use App\Models\Product;
use App\Queries\CatalogQueries;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Browsing stores and their menus (§9.1 steps 2–3). Public: signed-out visitors can look too. Where the customer is
 * decides the province (DECISIONS 84); outside every delivery zone the answer says FoxDrop is not there yet.
 */
class BrowseController extends Controller
{
    public function stores(Request $request, CatalogQueries $catalog): JsonResponse
    {
        ['lat' => $lat, 'lng' => $lng, 'category' => $category, 'q' => $term] = $request->validate([
            'lat' => ['required', 'numeric', 'between:4,22'],
            'lng' => ['required', 'numeric', 'between:116,127'],
            'category' => ['nullable', 'in:FOOD,GROCERIES,MEDICINE,MORE'],
            'q' => ['nullable', 'string', 'max:60'],
        ]) + ['category' => null, 'q' => null];

        $provinceId = DeliveryZone::provinceAt((float) $lat, (float) $lng);
        $stores = $provinceId === null ? collect() : $catalog->stores($provinceId, (float) $lat, (float) $lng, $category, $term);

        return response()->json([
            'data' => StoreCardResource::collection($stores)->resolve($request),
            'meta' => ['serviceable' => $provinceId !== null, 'province_id' => $provinceId],
        ]);
    }

    public function show(Request $request, string $store, CatalogQueries $catalog): StoreMenuResource
    {
        $location = $request->validate(['lat' => ['nullable', 'numeric'], 'lng' => ['nullable', 'numeric']]);
        $page = $catalog->storePage($store, isset($location['lat']) ? (float) $location['lat'] : null, isset($location['lng']) ? (float) $location['lng'] : null);

        return StoreMenuResource::make($page ?? abort(404));
    }

    public function search(Request $request, CatalogQueries $catalog): JsonResponse
    {
        ['lat' => $lat, 'lng' => $lng, 'q' => $term] = $request->validate([
            'lat' => ['required', 'numeric', 'between:4,22'],
            'lng' => ['required', 'numeric', 'between:116,127'],
            'q' => ['required', 'string', 'min:2', 'max:60'],
        ]);
        $provinceId = DeliveryZone::provinceAt((float) $lat, (float) $lng);
        if ($provinceId === null) {
            return response()->json(['data' => ['stores' => [], 'products' => []], 'meta' => ['serviceable' => false]]);
        }

        return response()->json([
            'data' => [
                'stores' => StoreCardResource::collection($catalog->stores($provinceId, (float) $lat, (float) $lng, null, $term))->resolve($request),
                'products' => $catalog->products($provinceId, $term)->map(fn (Product $product): array => ProductResource::make($product)->resolve($request) + [
                    'store' => ['id' => $product->store_id, 'name' => $product->store?->name],
                ])->all(),
            ],
            'meta' => ['serviceable' => true],
        ]);
    }
}
