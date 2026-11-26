<?php

namespace App\Http\Requests\Admin;

use App\Models\Rider;
use App\Models\Store;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

/**
 * Suspend / reinstate / close (stores) or deactivate (riders). Every change but reinstating needs a reason; which
 * change is allowed now is the model's STATUS_CHANGES, checked by the Action.
 */
class ChangePartnerStatusRequest extends FormRequest
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
        $reinstated = [Store::REINSTATED, Rider::REINSTATED];

        return [
            'status' => ['required', 'string', Rule::in([...array_keys(Store::STATUS_CHANGES), ...array_keys(Rider::STATUS_CHANGES)])],
            'reason' => [Rule::requiredIf(fn (): bool => ! in_array($this->input('status'), $reinstated, true)), 'nullable', ...ReasonRequest::REASON],
        ];
    }

    public function reason(): ?string
    {
        $reason = $this->input('reason');

        return is_string($reason) ? $reason : null;
    }
}
