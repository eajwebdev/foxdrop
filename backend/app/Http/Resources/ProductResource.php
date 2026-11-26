<?php

namespace App\Http\Resources;

use App\Models\Product;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * A product as customers see it. The store team's view adds `stock` and the raw `is_available` switch.
 *
 * @mixin Product
 */
class ProductResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'category_id' => $this->category_id,
            'name' => $this->name,
            'description' => $this->description,
            'price_centavos' => $this->price_centavos,
            'image_url' => $this->imageUrl(),
            'is_orderable' => $this->isOrderable(),
            'options' => $this->options,
            'sort_order' => $this->sort_order,
        ];
    }
}
