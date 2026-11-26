<?php

namespace App\Http\Requests\Admin;

use Illuminate\Foundation\Http\FormRequest;

/**
 * Suspend / reinstate / close (stores) or deactivate (riders). A reason is needed for everything but reinstating.
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
        return [
            'status' => ['required', 'string', 'in:SUSPENDED,ACTIVE,CLOSED,APPROVED,DEACTIVATED'],
            'reason' => ['required_unless:status,ACTIVE,APPROVED', 'nullable', 'string', 'min:5', 'max:300'],
        ];
    }

    public function reason(): ?string
    {
        $reason = trim($this->string('reason')->toString());

        return $reason === '' ? null : $reason;
    }
}
