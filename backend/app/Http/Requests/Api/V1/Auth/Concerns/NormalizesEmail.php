<?php

namespace App\Http\Requests\Api\V1\Auth\Concerns;

/**
 * Emails are stored lower-case and trimmed (the users table checks it); compare them the same way.
 */
trait NormalizesEmail
{
    protected function prepareForValidation(): void
    {
        $email = $this->input('email');
        if (is_string($email)) {
            $this->merge(['email' => mb_strtolower(trim($email))]);
        }
    }
}
