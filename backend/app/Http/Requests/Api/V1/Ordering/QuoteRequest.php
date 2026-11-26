<?php

namespace App\Http\Requests\Api\V1\Ordering;

use App\Models\Order;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

/**
 * A cart to price (§6). Only ids, quantities and option names: every price comes from the server.
 */
class QuoteRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /**
     * @return array<string, array<int, mixed>>
     */
    public function rules(): array
    {
        return [
            'store_id' => ['required', 'uuid'],
            'fulfillment' => ['required', Rule::in([Order::DELIVERY, Order::PICKUP])],
            'address_id' => ['required_if:fulfillment,DELIVERY', 'nullable', 'uuid'],
            'promo_code' => ['nullable', 'string', 'max:20'],
            'items' => ['required', 'array', 'min:1', 'max:50'],
            'items.*.product_id' => ['required', 'uuid'],
            'items.*.quantity' => ['required', 'integer', 'min:1', 'max:99'],
            'items.*.options' => ['sometimes', 'array', 'max:20'],
            'items.*.options.*.group' => ['required', 'string', 'max:60'],
            'items.*.options.*.name' => ['required', 'string', 'max:60'],
        ];
    }

    /**
     * @return list<array{product_id: string, quantity: int, options?: list<array{group: string, name: string}>}>
     */
    public function lines(): array
    {
        /** @var list<array{product_id: string, quantity: int|string, options?: list<array{group: string, name: string}>}> $items */
        $items = $this->validated('items');

        return array_map(fn (array $item): array => ['product_id' => $item['product_id'], 'quantity' => (int) $item['quantity'], 'options' => $item['options'] ?? []], $items);
    }
}
