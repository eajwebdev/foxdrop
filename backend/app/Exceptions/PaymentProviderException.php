<?php

namespace App\Exceptions;

use Illuminate\Http\JsonResponse;
use RuntimeException;

/**
 * The payment gateway is not set up here (503) or could not be reached (502).
 */
final class PaymentProviderException extends RuntimeException
{
    private function __construct(string $message, private readonly int $status)
    {
        parent::__construct($message);
    }

    public static function notConfigured(): self
    {
        return new self('Online payment is not set up on this server yet.', 503);
    }

    public static function unavailable(): self
    {
        return new self('The payment service is unavailable right now. Please try again in a moment.', 502);
    }

    public function render(): JsonResponse
    {
        return response()->json(['message' => $this->getMessage()], $this->status);
    }
}
