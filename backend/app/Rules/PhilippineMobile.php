<?php

namespace App\Rules;

use Closure;
use Illuminate\Contracts\Validation\ValidationRule;

/**
 * A Philippine mobile number in E.164 form, e.g. +639171234567 (§0: +63 only).
 */
class PhilippineMobile implements ValidationRule
{
    public function validate(string $attribute, mixed $value, Closure $fail): void
    {
        if (! is_string($value) || preg_match('/^\+63\d{10}$/', $value) !== 1) {
            $fail('Enter a valid Philippine mobile number.');
        }
    }
}
