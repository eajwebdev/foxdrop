<?php

namespace App\Http\Requests\Admin;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rules\Password;

/**
 * A new password: the admin's own (with the current one), or a staff member's set by the super admin.
 */
class PasswordRequest extends FormRequest
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
            'current_password' => $this->routeIs('admin.settings.password') ? ['required', 'string', 'current_password:web'] : ['prohibited'],
            'password' => ['required', 'confirmed', Password::defaults()],
        ];
    }
}
