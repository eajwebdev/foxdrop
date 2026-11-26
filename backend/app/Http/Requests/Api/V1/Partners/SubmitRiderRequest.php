<?php

namespace App\Http\Requests\Api\V1\Partners;

use App\Http\Requests\Api\V1\Concerns\PhotoRules;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class SubmitRiderRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    use PhotoRules;

    /**
     * Bicycles need no plate, OR/CR or vehicle photo (DECISIONS 61).
     *
     * @return array<string, array<int, mixed>>
     */
    public function rules(): array
    {
        $motorVehicle = $this->input('vehicle_type') !== 'BICYCLE';

        return [
            'province_id' => ['required', 'uuid', 'exists:provinces,id'],
            'full_name' => ['required', 'string', 'min:2', 'max:120'],
            'vehicle_type' => ['required', Rule::in(['MOTORCYCLE', 'BICYCLE', 'TRICYCLE', 'CAR'])],
            'plate_no' => [$motorVehicle ? 'required' : 'nullable', 'string', 'regex:/^[A-Za-z0-9 -]{2,12}$/'],
            'license_no' => ['required', 'string', 'regex:/^[A-Za-z0-9-]{5,20}$/'],
            'license_expiry' => ['required', 'date_format:Y-m-d', 'after:today'],
            'valid_id' => $this->photo(),
            'drivers_license' => $this->photo(),
            'selfie' => $this->photo(),
            'or_cr' => $this->photo(required: $motorVehicle),
            'vehicle_photo' => $this->photo(required: $motorVehicle),
        ];
    }

    /**
     * @return array<string, string>
     */
    public function messages(): array
    {
        return [
            'plate_no.regex' => 'Enter the plate number as shown, e.g. NEG 1234.',
            'license_no.regex' => 'Enter the license number as shown on the card.',
            'license_expiry.after' => 'This driver\'s license has expired.',
        ];
    }

    protected function prepareForValidation(): void
    {
        $plate = $this->input('plate_no');
        if (is_string($plate)) {
            $this->merge(['plate_no' => $plate === '' ? null : strtoupper(trim($plate))]);
        }
    }
}
