<?php

namespace App\Http\Requests\Api\V1\Operator;

use App\Http\Requests\Api\V1\Concerns\PhotoRules;
use App\Rules\PhilippineMobile;
use Illuminate\Foundation\Http\FormRequest;

class SubmitOperatorApplicationRequest extends FormRequest
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
            'business_name' => ['required', 'string', 'min:2', 'max:160'],
            'registration_number' => ['nullable', 'string', 'max:60'],
            'contact_phone' => ['required', 'string', new PhilippineMobile],
            'contact_email' => ['nullable', 'string', 'email', 'max:254'],
            'valid_id' => $this->photo(),
            'business_permit' => $this->photo(required: false),
        ];
    }
}
