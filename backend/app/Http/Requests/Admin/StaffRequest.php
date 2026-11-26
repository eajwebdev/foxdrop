<?php

namespace App\Http\Requests\Admin;

use App\Enums\AdminPermission;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;
use Illuminate\Validation\Rules\Password;

/**
 * Creating an admin-staff account, or changing what it may do (PUT sends only `permissions`).
 */
class StaffRequest extends FormRequest
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
        $creating = $this->isMethod('post');

        return [
            'full_name' => $creating ? ['required', 'string', 'min:2', 'max:120'] : ['prohibited'],
            'email' => $creating ? ['required', 'email:rfc', 'max:254'] : ['prohibited'],
            'password' => $creating ? ['required', 'confirmed', Password::defaults()] : ['prohibited'],
            'permissions' => ['nullable', 'array'], // no boxes ticked = no permissions
            'permissions.*' => ['string', Rule::enum(AdminPermission::class)],
        ];
    }

    /**
     * @return list<AdminPermission>
     */
    public function permissions(): array
    {
        /** @var list<string> $names */
        $names = array_values((array) $this->input('permissions', []));

        return array_map(fn (string $name): AdminPermission => AdminPermission::from($name), $names);
    }
}
