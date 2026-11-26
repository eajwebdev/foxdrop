<?php

namespace App\Exceptions;

use Illuminate\Http\JsonResponse;
use RuntimeException;

/**
 * Google sign-in failed: not configured on this server (503) or the token was not acceptable (422).
 */
final class GoogleSignInException extends RuntimeException
{
    private function __construct(string $message, private readonly int $status)
    {
        parent::__construct($message);
    }

    public static function notConfigured(): self
    {
        return new self('Google sign-in is not set up on this server yet.', 503);
    }

    public static function invalidToken(): self
    {
        return new self('We could not verify your Google account. Please try again.', 422);
    }

    public function render(): JsonResponse
    {
        return response()->json(['message' => $this->getMessage()], $this->status);
    }
}
