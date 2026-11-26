<?php

use App\Models\OtpCode;
use Illuminate\Support\Facades\Schedule;

/*
| Scheduled housekeeping (`php artisan schedule:work` locally; one cron entry `schedule:run` on the VPS).
*/
Schedule::command('model:prune', ['--model' => [OtpCode::class]])->daily()->onOneServer();
Schedule::command('sanctum:prune-expired', ['--hours' => 24])->daily()->onOneServer();

// License status changes (§5.1, §12): daily 00:05 Manila time.
Schedule::command('foxdrop:licenses:advance')->dailyAt('00:05')->timezone('Asia/Manila')->withoutOverlapping()->onOneServer();

// Order timeouts (§5.4, §12): unpaid online orders after 15 min, unanswered orders after 5 min.
Schedule::command('foxdrop:orders:cancel-stale')->everyMinute()->withoutOverlapping()->onOneServer();
