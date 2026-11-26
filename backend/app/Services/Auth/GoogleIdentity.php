<?php

namespace App\Services\Auth;

/**
 * The verified facts from a Google ID token. [email] is set only when Google says the address is verified.
 */
final readonly class GoogleIdentity
{
    public function __construct(
        public string $subject,
        public ?string $email,
        public ?string $name,
        public ?string $pictureUrl,
    ) {}
}
