<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;

/**
 * A line of an order: a snapshot of the product's name, price and chosen options when the order was placed.
 *
 * @property string $id
 * @property string $order_id
 * @property string|null $product_id
 * @property string $name
 * @property int $unit_price_centavos
 * @property int $quantity
 * @property list<array{group: string, name: string, price_centavos: int}> $options
 * @property int $line_total_centavos
 */
class OrderItem extends Model
{
    use HasUuids;

    public $timestamps = false;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'unit_price_centavos' => 'integer',
            'quantity' => 'integer',
            'options' => 'array',
            'line_total_centavos' => 'integer',
        ];
    }
}
