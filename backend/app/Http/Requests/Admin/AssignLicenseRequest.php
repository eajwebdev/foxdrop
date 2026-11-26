<?php

namespace App\Http\Requests\Admin;

use App\Models\User;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Validator;

/**
 * Assigning a province: the future operator is an existing account, found by email or +63 mobile number.
 */
class AssignLicenseRequest extends FormRequest
{
    private ?User $operator = null;

    public function authorize(): bool
    {
        return true;
    }

    /**
     * @return array<string, array<int, mixed>>
     */
    public function rules(): array
    {
        return ['operator' => ['required', 'string', 'max:254']];
    }

    /**
     * @return array<int, callable(Validator): void>
     */
    public function after(): array
    {
        return [function (Validator $validator): void {
            $this->operator = User::query()->whereAccount($this->string('operator')->toString())->first();
            if ($this->operator === null) {
                $validator->errors()->add('operator', 'No account has this email or mobile number.');
            }
        }];
    }

    public function operator(): User
    {
        return $this->operator ?? abort(422);
    }
}
