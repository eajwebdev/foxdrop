<?php

use App\Http\Controllers\Api\V1\Auth\EmailAuthController;
use App\Http\Controllers\Api\V1\Auth\GoogleAuthController;
use App\Http\Controllers\Api\V1\Auth\PhoneOtpController;
use App\Http\Controllers\Api\V1\Auth\SessionController;
use App\Http\Controllers\Api\V1\MeController;
use App\Http\Controllers\Api\V1\ProvinceController;
use App\Http\Middleware\EnsureAccountIsActive;
use Illuminate\Support\Facades\Route;

/*
| FoxDrop API v1 for the three Android apps. Every route returns JSON.
*/
Route::prefix('v1')->name('api.v1.')->group(function (): void {
    Route::get('provinces', ProvinceController::class)->name('provinces.index');

    Route::prefix('auth')->name('auth.')->group(function (): void {
        Route::post('otp/send', [PhoneOtpController::class, 'send'])->middleware('throttle:otp-send')->name('otp.send');
        Route::post('otp/verify', [PhoneOtpController::class, 'verify'])->middleware('throttle:otp-verify')->name('otp.verify');
        Route::post('email/register', [EmailAuthController::class, 'register'])->middleware('throttle:sign-in')->name('email.register');
        Route::post('email/login', [EmailAuthController::class, 'login'])->middleware('throttle:sign-in')->name('email.login');
        Route::post('google', GoogleAuthController::class)->middleware('throttle:sign-in')->name('google');
    });

    Route::middleware(['auth:sanctum', EnsureAccountIsActive::class])->group(function (): void {
        Route::get('me', MeController::class)->name('me');
        Route::post('auth/logout', [SessionController::class, 'destroy'])->name('auth.logout');
    });
});
