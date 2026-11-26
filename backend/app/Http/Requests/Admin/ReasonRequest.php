<?php

namespace App\Http\Requests\Admin;

use Illuminate\Foundation\Http\FormRequest;

/**
 * Rejecting or suspending something: the reason is shown to the person it affects. The operator app's reject
 * request uses the same rule.
 */
class ReasonRequest extends FormRequest
{
    /** @var list<string> */
    public const array REASON = ['string', 'min:5', 'max:300'];

    public function authorize(): bool
    {
        return true; // the route's `can:` middleware and the Action decide
    }

    /**
     * @return array<string, array<int, mixed>>
     */
    public function rules(): array
    {
        return ['reason' => ['required', ...self::REASON]];
    }

    public function reason(): string
    {
        return $this->string('reason')->toString();
    }
}
