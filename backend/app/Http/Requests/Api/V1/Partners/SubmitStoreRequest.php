<?php

namespace App\Http\Requests\Api\V1\Partners;

use App\Http\Requests\Api\V1\Concerns\PhotoRules;
use App\Rules\PhilippineMobile;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class SubmitStoreRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    use PhotoRules;

    /**
     * @return array<string, array<int, mixed>>
     */
    public function rules(): array
    {
        return [
            'province_id' => ['required', 'uuid', 'exists:provinces,id'],
            'name' => ['required', 'string', 'min:2', 'max:120'],
            'category' => ['required', Rule::in(['FOOD', 'GROCERIES', 'MEDICINE', 'MORE'])],
            'description' => ['nullable', 'string', 'max:1000'],
            'address_line' => ['required', 'string', 'max:300'],
            'barangay' => ['nullable', 'string', 'max:120'],
            'city' => ['required', 'string', 'max:120'],
            'phone' => ['nullable', 'string', new PhilippineMobile],
            'latitude' => ['required', 'numeric', 'between:4,22'],
            'longitude' => ['required', 'numeric', 'between:116,127'],
            'business_permit' => $this->photo(),
            'valid_id' => $this->photo(),
        ];
    }
}
