<?php

namespace App\Http\Resources;

use App\Models\Product;
use App\Models\ProductCategory;
use App\Models\Store;
use Illuminate\Http\Request;

/**
 * A store page (§9.1 step 3): the card plus its menu in category order. Products without a category come last
 * under "More". Load `categories` and `products` first.
 *
 * @mixin Store
 */
class StoreMenuResource extends StoreCardResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        $products = $this->products;
        $sections = [];
        foreach ($this->categories as $category) {
            /** @var ProductCategory $category */
            $inside = $products->where('category_id', $category->id)->values();
            if ($inside->isNotEmpty()) {
                $sections[] = ['id' => $category->id, 'name' => $category->name, 'products' => ProductResource::collection($inside)->resolve($request)];
            }
        }
        $loose = $products->filter(fn (Product $product): bool => $product->category_id === null)->values();
        if ($loose->isNotEmpty()) {
            $sections[] = ['id' => null, 'name' => 'More', 'products' => ProductResource::collection($loose)->resolve($request)];
        }

        return parent::toArray($request) + ['phone' => $this->resource->getAttribute('phone'), 'sections' => $sections];
    }
}
