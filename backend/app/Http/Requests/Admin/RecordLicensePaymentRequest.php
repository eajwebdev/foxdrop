<?php

namespace App\Http\Requests\Admin;

use App\Actions\Licensing\RecordOfflineLicensePayment;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class RecordLicensePaymentRequest extends FormRequest
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
            'method' => ['required', Rule::in(RecordOfflineLicensePayment::METHODS)],
            // The bank or receipt reference; recorded once (DECISIONS 76).
            'reference' => ['required', 'string', 'min:3', 'max:60', 'regex:/^[A-Za-z0-9][A-Za-z0-9 \/._-]*$/'],
            'note' => ['nullable', 'string', 'max:300'],
        ];
    }
}
