<?php

namespace App\Queries;

use App\Models\Product;
use App\Models\Store;
use App\Support\Like;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Collection;

/**
 * What customers browse (§9.1 step 2): published stores of the province they are in, nearest and open first.
 */
final class CatalogQueries
{
    /**
     * Stores of [provinceId], optionally of one category or matching a word (store name or something it sells).
     *
     * @return Collection<int, Store>
     */
    public function stores(string $provinceId, float $lat, float $lng, ?string $category, ?string $term): Collection
    {
        return $this->published($provinceId, $lat, $lng)
            ->when($category, fn (Builder $query, string $value) => $query->where('category', $value))
            ->when($term, fn (Builder $query, string $value) => $query->where(fn (Builder $match) => $match
                ->where('name', 'ilike', Like::contains($value))
                ->orWhereIn('id', Product::query()->where('is_available', true)->where('name', 'ilike', Like::contains($value))->select('store_id'))))
            ->orderByRaw("case when status = 'ACTIVE' then 0 else 1 end")
            ->orderBy('distance_m')
            ->limit(50)
            ->get();
    }

    /**
     * Products matching a word in the stores of [provinceId], with their store (§9.1 "search stores + products").
     *
     * @return Collection<int, Product>
     */
    public function products(string $provinceId, string $term): Collection
    {
        return Product::query()
            ->with('store:id,name,status,opening_hours')
            ->whereIn('store_id', Store::query()->published()->where('province_id', $provinceId)->select('id'))
            ->where('is_available', true)
            ->where('name', 'ilike', Like::contains($term))
            ->orderBy('name')
            ->limit(30)
            ->get();
    }

    /**
     * One store's page with its menu (available or sold out), or null if it is not published.
     */
    public function storePage(string $storeId, ?float $lat, ?float $lng): ?Store
    {
        $query = Store::query()->published()->whereKey($storeId)->with(['categories', 'products']);
        if ($lat !== null && $lng !== null) {
            $query->select('stores.*')->selectRaw('ST_Distance(location, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography) as distance_m', [$lng, $lat]);
        }

        return $query->first();
    }

    /**
     * @return Builder<Store>
     */
    private function published(string $provinceId, float $lat, float $lng): Builder
    {
        return Store::query()
            ->published()
            ->where('province_id', $provinceId)
            ->select('stores.*')
            ->selectRaw('ST_Distance(location, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography) as distance_m', [$lng, $lat]);
    }
}
