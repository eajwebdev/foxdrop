<?php

namespace App\Http\Requests\Api\V1\Auth;

use App\Rules\PhilippineMobile;
use Illuminate\Foundation\Http\FormRequest;

class SendOtpRequest extends FormRequest
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
        ];
    }
}
