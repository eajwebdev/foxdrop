<?php

namespace App\Actions\Catalog;

use App\Models\Product;
use App\Models\ProductCategory;
use App\Models\Store;
use App\Models\User;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Gate;
use Illuminate\Validation\ValidationException;

/**
 * The store team's menu (§9.3 "catalog CRUD"): categories and products with photo, price, options, availability and
 * stock. Only the store's own team may change it (StorePolicy::manage).
 */
final class ManageCatalog
{
    public function __construct(private readonly StoreProductImage $images) {}

    public function saveCategory(User $user, Store $store, ?ProductCategory $category, string $name, int $sortOrder): ProductCategory
    {
        Gate::forUser($user)->authorize('manage', $store);
        $category ??= new ProductCategory(['store_id' => $store->id, 'province_id' => $store->province_id]);
        $category->fill(['name' => $name, 'sort_order' => $sortOrder])->save();

        return $category;
    }

    /**
     * Its products stay, without a category.
     */
    public function deleteCategory(User $user, Store $store, ProductCategory $category): void
    {
        Gate::forUser($user)->authorize('manage', $store);
        $category->delete();
    }

    /**
     * @param  array{name: string, description?: string|null, price_centavos: int, category_id?: string|null, is_available?: bool, stock?: int|null, options?: list<array<string, mixed>>, sort_order?: int}  $fields
     */
    public function saveProduct(User $user, Store $store, ?Product $product, array $fields, ?UploadedFile $photo, bool $removePhoto = false): Product
    {
        Gate::forUser($user)->authorize('manage', $store);
        $categoryId = $fields['category_id'] ?? null;
        if ($categoryId !== null && ProductCategory::query()->where('store_id', $store->id)->whereKey($categoryId)->doesntExist()) {
            throw ValidationException::withMessages(['category_id' => 'Choose one of this store\'s categories.']);
        }

        return DB::transaction(function () use ($store, $product, $fields, $photo, $removePhoto): Product {
            $product ??= new Product(['store_id' => $store->id, 'province_id' => $store->province_id]);
            $oldPhoto = $product->image_path;
            $product->fill([
                'name' => $fields['name'],
                'description' => $fields['description'] ?? null,
                'price_centavos' => $fields['price_centavos'],
                'category_id' => $fields['category_id'] ?? null,
                'is_available' => $fields['is_available'] ?? true,
                'stock' => $fields['stock'] ?? null,
                'options' => $fields['options'] ?? [],
                'sort_order' => $fields['sort_order'] ?? 0,
            ]);
            if ($photo !== null) {
                $product->image_path = ($this->images)($photo, $store->id);
            } elseif ($removePhoto) {
                $product->image_path = null;
            }
            $product->save();
            if ($oldPhoto !== $product->image_path) {
                $this->images->deleteAfterCommit($oldPhoto);
            }

            return $product;
        });
    }

    public function setAvailability(User $user, Store $store, Product $product, bool $available): Product
    {
        Gate::forUser($user)->authorize('manage', $store);
        $product->forceFill(['is_available' => $available])->save();

        return $product;
    }

    /**
     * Past orders keep their own copy of the name and price (order_items), so a product can go.
     */
    public function deleteProduct(User $user, Store $store, Product $product): void
    {
        Gate::forUser($user)->authorize('manage', $store);
        DB::transaction(function () use ($product): void {
            $this->images->deleteAfterCommit($product->image_path);
            $product->delete();
        });
    }

    /**
     * Open / pause and the store's order settings (§9.3 "opening hours & pause store", "set prep time").
     *
     * @param  array{is_open?: bool, prep_time_min?: int, accepts_cod?: bool, accepts_pickup?: bool}  $settings
     */
    public function updateSettings(User $user, Store $store, array $settings): Store
    {
        Gate::forUser($user)->authorize('manage', $store);
        $changes = array_intersect_key($settings, array_flip(['prep_time_min', 'accepts_cod', 'accepts_pickup']));
        if (array_key_exists('is_open', $settings)) {
            abort_unless(in_array($store->status, [Store::ACTIVE, Store::PAUSED], true), 409, 'This store is '.strtolower($store->status).' and cannot be opened here.');
            $changes['status'] = $settings['is_open'] ? Store::ACTIVE : Store::PAUSED;
        }
        $store->forceFill($changes)->save();

        return $store;
    }
}
