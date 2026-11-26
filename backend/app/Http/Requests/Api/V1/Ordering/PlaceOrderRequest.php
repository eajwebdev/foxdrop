<?php

namespace App\Http\Requests\Api\V1\Ordering;

use App\Models\Order;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

/**
 * Placing an order: the quote and how to pay. Amounts are never accepted (DECISIONS 86).
 */
class PlaceOrderRequest extends FormRequest
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
            'quote_id' => ['required', 'uuid'],
            'payment_method' => ['required', Rule::in([Order::COD, Order::ONLINE])],
            'notes' => ['nullable', 'string', 'max:500'],
        ];
    }

    /** The `Idempotency-Key` header (8–100 characters), so a retried tap returns the same order. */
    public function idempotencyKey(): ?string
    {
        $key = $this->header('Idempotency-Key');

        return is_string($key) && strlen($key) >= 8 && strlen($key) <= 100 ? $key : null;
    }
}
