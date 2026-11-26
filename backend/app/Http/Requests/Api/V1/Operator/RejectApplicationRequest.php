<?php

namespace App\Http\Requests\Api\V1\Operator;

use App\Http\Requests\Admin\ReasonRequest;
use Illuminate\Foundation\Http\FormRequest;

class RejectApplicationRequest extends FormRequest
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
        return ['reason' => ['required', ...ReasonRequest::REASON]]; // same rule as the admin web
    }
}
