<?php

namespace App\Http\Requests\Api\V1\StoreTeam;

use App\Http\Requests\Api\V1\Concerns\PhotoRules;
use Illuminate\Foundation\Http\FormRequest;

/**
 * A product (§9.3 catalog): sent as multipart so a photo can come along. `options` is a JSON string of choice groups.
 */
class ProductRequest extends FormRequest
{
    use PhotoRules;

    public function authorize(): bool
    {
        return true;
    }

    protected function prepareForValidation(): void
    {
        $options = $this->input('options');
        if (is_string($options)) {
            $this->merge(['options' => json_decode($options, true) ?? 'invalid']);
        }
    }

    /**
     * @return array<string, array<int, mixed>>
     */
    public function rules(): array
    {
        return [
            'name' => ['required', 'string', 'min:1', 'max:120'],
            'description' => ['nullable', 'string', 'max:1000'],
            'price_centavos' => ['required', 'integer', 'min:100', 'max:10000000'], // ₱1 – ₱100,000
            'category_id' => ['nullable', 'uuid'],
            'is_available' => ['sometimes', 'boolean'],
            'stock' => ['nullable', 'integer', 'min:0', 'max:100000'],
            'sort_order' => ['sometimes', 'integer', 'min:0', 'max:10000'],
            'photo' => $this->photo(required: false),
            'remove_photo' => ['sometimes', 'boolean'],
            'options' => ['sometimes', 'array', 'max:10'],
            'options.*.group' => ['required', 'string', 'min:1', 'max:60', 'distinct'],
            'options.*.required' => ['required', 'boolean'],
            'options.*.max' => ['required', 'integer', 'min:1', 'max:20'],
            'options.*.choices' => ['required', 'array', 'min:1', 'max:20'],
            'options.*.choices.*.name' => ['required', 'string', 'min:1', 'max:60'],
            'options.*.choices.*.price_centavos' => ['required', 'integer', 'min:0', 'max:1000000'],
        ];
    }

    /**
     * @return array{name: string, description: string|null, price_centavos: int, category_id: string|null, is_available: bool, stock: int|null, options: list<array<string, mixed>>, sort_order: int}
     */
    public function fields(): array
    {
        /** @var list<array{group: string, required: bool|string, max: int|string, choices: list<array{name: string, price_centavos: int|string}>}> $options */
        $options = $this->validated('options', []);
        $stock = $this->input('stock');

        return [
            'name' => (string) $this->input('name'),
            'description' => $this->input('description'),
            'price_centavos' => (int) $this->input('price_centavos'),
            'category_id' => $this->input('category_id'),
            'is_available' => $this->boolean('is_available', true),
            'stock' => $stock === null ? null : (int) $stock,
            'options' => array_map(fn (array $group): array => [
                'group' => $group['group'],
                'required' => filter_var($group['required'], FILTER_VALIDATE_BOOL),
                'max' => (int) $group['max'],
                'choices' => array_map(fn (array $choice): array => ['name' => $choice['name'], 'price_centavos' => (int) $choice['price_centavos']], $group['choices']),
            ], $options),
            'sort_order' => $this->integer('sort_order'),
        ];
    }
}
