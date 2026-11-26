<?php

use App\Models\OtpCode;
use Illuminate\Support\Facades\Schedule;

/*
| Scheduled housekeeping (`php artisan schedule:work` locally; one cron entry `schedule:run` on the VPS).
*/
Schedule::command('model:prune', ['--model' => [OtpCode::class]])->daily()->onOneServer();
Schedule::command('sanctum:prune-expired', ['--hours' => 24])->daily()->onOneServer();
