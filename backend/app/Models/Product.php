<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * Something a store sells. `options` are choice groups, e.g.
 * [{"group":"Size","required":true,"max":1,"choices":[{"name":"Large","price_centavos":2000}]}].
 * A null `stock` is not tracked (DECISIONS 90).
 *
 * @property string $id
 * @property string $store_id
 * @property string $province_id
 * @property string|null $category_id
 * @property string $name
 * @property string|null $description
 * @property int $price_centavos
 * @property string|null $image_path
 * @property bool $is_available
 * @property int|null $stock
 * @property list<array{group: string, required: bool, max: int, choices: list<array{name: string, price_centavos: int}>}> $options
 * @property int $sort_order
 */
#[Fillable(['store_id', 'province_id', 'category_id', 'name', 'description', 'price_centavos', 'image_path', 'is_available', 'stock', 'options', 'sort_order'])]
class Product extends Model
{
    use HasUuids;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'price_centavos' => 'integer',
            'is_available' => 'boolean',
            'stock' => 'integer',
            'options' => 'array',
            'sort_order' => 'integer',
        ];
    }

    /**
     * @return BelongsTo<Store, $this>
     */
    public function store(): BelongsTo
    {
        return $this->belongsTo(Store::class);
    }

    /**
     * Product photos are public (DECISIONS 92), served by the `media.catalog` route with a long cache: every new photo
     * gets a new file name.
     */
    public function imageUrl(): ?string
    {
        return $this->image_path === null ? null : url('media/'.$this->image_path);
    }

    /**
     * Whether a customer can order it now (available, and in stock when stock is tracked).
     */
    public function isOrderable(): bool
    {
        return $this->is_available && ($this->stock === null || $this->stock > 0);
    }
}
