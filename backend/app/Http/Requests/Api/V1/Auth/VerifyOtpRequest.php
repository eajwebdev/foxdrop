<?php

namespace App\Http\Requests\Api\V1\Auth;

use App\Rules\PhilippineMobile;
use Illuminate\Foundation\Http\FormRequest;

class VerifyOtpRequest extends FormRequest
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
            'phone' => ['required', 'string', new PhilippineMobile],
            'code' => ['required', 'string', 'digits:'.config('foxdrop.auth.otp.length')],
            'device_name' => ['required', 'string', 'max:120'],
            'full_name' => ['nullable', 'string', 'max:120'],
        ];
    }
}
