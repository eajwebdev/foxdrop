<?php

namespace App\Services\Sms;

use App\Contracts\SmsSender;
use Illuminate\Support\Facades\Log;

/**
 * Development SMS driver: writes the message to the application log instead of sending it.
 */
final class LogSmsSender implements SmsSender
{
    public function send(string $phone, string $message): void
    {
        Log::info('SMS (log driver, not sent)', ['to' => $phone, 'message' => $message]);
    }
}
