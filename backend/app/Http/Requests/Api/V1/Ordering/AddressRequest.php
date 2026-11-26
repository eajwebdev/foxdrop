<?php

namespace App\Http\Requests\Api\V1\Ordering;

use Illuminate\Foundation\Http\FormRequest;

class AddressRequest extends FormRequest
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
            'label' => ['nullable', 'string', 'max:40'],
            'location' => ['required', 'array'],
            'location.lat' => ['required', 'numeric', 'between:4,22'], // the Philippines
            'location.lng' => ['required', 'numeric', 'between:116,127'],
            'address_line' => ['required', 'string', 'min:3', 'max:300'],
            'landmark' => ['nullable', 'string', 'max:200'],
            'barangay' => ['nullable', 'string', 'max:120'],
            'city' => ['required', 'string', 'min:2', 'max:120'],
        ];
    }

    /**
     * @return array{label: string|null, lat: float, lng: float, address_line: string, landmark: string|null, barangay: string|null, city: string}
     */
    public function fields(): array
    {
        return [
            'label' => $this->input('label'),
            'lat' => (float) $this->input('location.lat'),
            'lng' => (float) $this->input('location.lng'),
            'address_line' => (string) $this->input('address_line'),
            'landmark' => $this->input('landmark'),
            'barangay' => $this->input('barangay'),
            'city' => (string) $this->input('city'),
        ];
    }
}
