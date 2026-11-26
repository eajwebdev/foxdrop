<?php

namespace App\Contracts;

/**
 * Sends a text message to a +63 number. Local development logs it; production uses a real SMS provider.
 */
interface SmsSender
{
    public function send(string $phone, string $message): void;
}
