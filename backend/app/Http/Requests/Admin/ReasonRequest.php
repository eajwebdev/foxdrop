<?php

namespace App\Http\Requests\Admin;

use Illuminate\Foundation\Http\FormRequest;

/**
 * Rejecting or suspending something: the reason is shown to the person it affects.
 */
class ReasonRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true; // the route's `can:` middleware and the Action decide
    }

    /**
     * @return array<string, array<int, mixed>>
     */
    public function rules(): array
    {
        return ['reason' => ['required', 'string', 'min:5', 'max:300']];
    }

    public function reason(): string
    {
        return trim($this->string('reason')->toString());
    }
}
