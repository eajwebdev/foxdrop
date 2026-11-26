<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

/**
 * A section of a store's menu ("Burgers & Mains"). Written only by the store team (SaveProductCategory).
 *
 * @property string $id
 * @property string $store_id
 * @property string $province_id
 * @property string $name
 * @property int $sort_order
 */
#[Fillable(['store_id', 'province_id', 'name', 'sort_order'])]
class ProductCategory extends Model
{
    use HasUuids;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return ['sort_order' => 'integer'];
    }

    /**
     * @return HasMany<Product, $this>
     */
    public function products(): HasMany
    {
        return $this->hasMany(Product::class, 'category_id')->orderBy('sort_order')->orderBy('name');
    }
}
