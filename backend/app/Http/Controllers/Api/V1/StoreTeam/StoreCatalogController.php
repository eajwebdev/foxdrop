<?php

namespace App\Http\Controllers\Api\V1\StoreTeam;

use App\Actions\Catalog\ManageCatalog;
use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\StoreTeam\ProductRequest;
use App\Http\Resources\ProductResource;
use App\Models\Product;
use App\Models\ProductCategory;
use App\Models\Store;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Illuminate\Support\Facades\Gate;

/**
 * The store team's menu (§9.3): categories and products, with photos, availability and stock. Routes bind the
 * category and product to the store ({store}/products/{product} must belong together).
 */
class StoreCatalogController extends Controller
{
    public function show(Request $request, Store $store): JsonResponse
    {
        Gate::forUser($this->user($request))->authorize('manage', $store);

        return response()->json(['data' => [
            'categories' => $store->categories()->get(['id', 'name', 'sort_order']),
            'products' => $store->products()->get()->map(fn (Product $product): array => ProductResource::make($product)->resolve($request) + [
                'is_available' => $product->is_available,
                'stock' => $product->stock,
            ])->all(),
        ]]);
    }

    public function storeCategory(Request $request, Store $store, ManageCatalog $catalog): JsonResponse
    {
        $fields = $this->categoryFields($request);
        $category = $catalog->saveCategory($this->user($request), $store, null, $fields['name'], $fields['sort_order']);

        return response()->json(['data' => $category->only(['id', 'name', 'sort_order'])], 201);
    }

    public function updateCategory(Request $request, Store $store, ProductCategory $category, ManageCatalog $catalog): JsonResponse
    {
        $fields = $this->categoryFields($request);
        $category = $catalog->saveCategory($this->user($request), $store, $category, $fields['name'], $fields['sort_order']);

        return response()->json(['data' => $category->only(['id', 'name', 'sort_order'])]);
    }

    public function destroyCategory(Request $request, Store $store, ProductCategory $category, ManageCatalog $catalog): Response
    {
        $catalog->deleteCategory($this->user($request), $store, $category);

        return response()->noContent();
    }

    public function storeProduct(ProductRequest $request, Store $store, ManageCatalog $catalog): JsonResponse
    {
        $product = $catalog->saveProduct($this->user($request), $store, null, $request->fields(), $request->file('photo'));

        return $this->product($request, $product, 201);
    }

    public function updateProduct(ProductRequest $request, Store $store, Product $product, ManageCatalog $catalog): JsonResponse
    {
        $product = $catalog->saveProduct($this->user($request), $store, $product, $request->fields(), $request->file('photo'), $request->boolean('remove_photo'));

        return $this->product($request, $product);
    }

    public function availability(Request $request, Store $store, Product $product, ManageCatalog $catalog): JsonResponse
    {
        $request->validate(['is_available' => ['required', 'boolean']]);

        return $this->product($request, $catalog->setAvailability($this->user($request), $store, $product, $request->boolean('is_available')));
    }

    public function destroyProduct(Request $request, Store $store, Product $product, ManageCatalog $catalog): Response
    {
        $catalog->deleteProduct($this->user($request), $store, $product);

        return response()->noContent();
    }

    /**
     * @return array{name: string, sort_order: int}
     */
    private function categoryFields(Request $request): array
    {
        $request->validate(['name' => ['required', 'string', 'min:1', 'max:60'], 'sort_order' => ['sometimes', 'integer', 'min:0', 'max:10000']]);

        return ['name' => $request->string('name')->toString(), 'sort_order' => $request->integer('sort_order')];
    }

    private function product(Request $request, Product $product, int $status = 200): JsonResponse
    {
        return response()->json(['data' => ProductResource::make($product)->resolve($request) + [
            'is_available' => $product->is_available,
            'stock' => $product->stock,
        ]], $status);
    }

    private function user(Request $request): User
    {
        /** @var User */
        return $request->user();
    }
}
