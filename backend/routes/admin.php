<?php

use App\Http\Controllers\Admin\AuditLogController;
use App\Http\Controllers\Admin\Auth\ConfirmAccessController;
use App\Http\Controllers\Admin\Auth\TwoFactorEnableController;
use App\Http\Controllers\Admin\Auth\TwoFactorSetupController;
use App\Http\Controllers\Admin\DashboardController;
use App\Http\Controllers\Admin\DocumentController;
use App\Http\Controllers\Admin\Franchises\LicenseController;
use App\Http\Controllers\Admin\Franchises\OperatorApplicationController;
use App\Http\Controllers\Admin\Franchises\ProvinceController;
use App\Http\Controllers\Admin\Partners\RiderController;
use App\Http\Controllers\Admin\Partners\StoreController;
use App\Http\Controllers\Admin\Settings\SecurityController;
use App\Http\Controllers\Admin\Settings\StaffController;
use App\Http\Controllers\Admin\Users\UserController;
use App\Http\Middleware\Admin\EnsureAdminAccount;
use App\Http\Middleware\Admin\EnsureTwoFactorEnabled;
use App\Http\Middleware\Admin\ForgetRememberMe;
use App\Http\Middleware\Admin\RequireFreshConfirmation;
use Illuminate\Auth\Middleware\RequirePassword;
use Illuminate\Session\Middleware\AuthenticateSession;
use Illuminate\Support\Facades\Route;
use Laravel\Fortify\Http\Controllers\AuthenticatedSessionController;
use Laravel\Fortify\Http\Controllers\ConfirmedTwoFactorAuthenticationController;
use Laravel\Fortify\Http\Controllers\RecoveryCodeController;
use Laravel\Fortify\Http\Controllers\TwoFactorAuthenticatedSessionController;
use Laravel\Fortify\Http\Controllers\TwoFactorQrCodeController;
use Laravel\Fortify\Http\Controllers\TwoFactorSecretKeyController;

/*
| Super Admin web (§20). Sign-in uses Fortify's controllers (routes registered here, Fortify::ignoreRoutes()); every
| page needs an active admin account with 2FA confirmed, and the permission the page names (DECISIONS 72–75).
| "confirmed" = a password + authenticator check within the last 15 minutes.
*/
Route::prefix('admin')->group(function (): void {
    Route::middleware('guest:web')->group(function (): void {
        Route::get('login', [AuthenticatedSessionController::class, 'create'])->name('login');
        Route::post('login', [AuthenticatedSessionController::class, 'store'])->middleware(['throttle:login', ForgetRememberMe::class])->name('login.store');
        Route::get('two-factor-challenge', [TwoFactorAuthenticatedSessionController::class, 'create'])->name('two-factor.login');
        Route::post('two-factor-challenge', [TwoFactorAuthenticatedSessionController::class, 'store'])->middleware('throttle:two-factor')->name('two-factor.login.store');
    });

    // AuthenticateSession: a password change or reset signs out every other session on its next request.
    Route::middleware(['auth:web', AuthenticateSession::class, EnsureAdminAccount::class])->group(function (): void {
        Route::post('logout', [AuthenticatedSessionController::class, 'destroy'])->name('logout');
        Route::get('confirm-access', [ConfirmAccessController::class, 'show'])->name('password.confirm');
        Route::post('confirm-access', [ConfirmAccessController::class, 'store'])->middleware('throttle:6,1')->name('password.confirm.store');

        // Setting up (or re-keying) the authenticator app. There is no route to turn 2FA off (DECISIONS 74).
        Route::get('two-factor/setup', TwoFactorSetupController::class)->name('admin.two-factor.setup');
        Route::middleware(RequirePassword::class)->prefix('user')->name('two-factor.')->group(function (): void {
            Route::post('two-factor-authentication', TwoFactorEnableController::class)->name('enable');
            Route::post('confirmed-two-factor-authentication', [ConfirmedTwoFactorAuthenticationController::class, 'store'])->name('confirm');
            Route::get('two-factor-qr-code', [TwoFactorQrCodeController::class, 'show'])->name('qr-code');
            Route::get('two-factor-secret-key', [TwoFactorSecretKeyController::class, 'show'])->name('secret-key');
            Route::get('two-factor-recovery-codes', [RecoveryCodeController::class, 'index'])->name('recovery-codes');
            Route::post('two-factor-recovery-codes', [RecoveryCodeController::class, 'store'])->name('regenerate-recovery-codes');
        });

        Route::middleware(EnsureTwoFactorEnabled::class)->name('admin.')->group(function (): void {
            Route::get('/', DashboardController::class)->name('dashboard');

            // Franchises: provinces, licenses, operator applications (§9.5 step 3).
            Route::middleware('can:franchises.view')->group(function (): void {
                Route::get('franchises', [ProvinceController::class, 'index'])->name('franchises.index');
                Route::get('franchises/{province}', [ProvinceController::class, 'show'])->name('franchises.show');
                Route::get('operator-applications', [OperatorApplicationController::class, 'index'])->name('applications.index');
                Route::get('operator-applications/{application}', [OperatorApplicationController::class, 'show'])->name('applications.show');
                Route::get('operator-applications/{application}/documents/{type}', [DocumentController::class, 'operatorApplication'])
                    ->whereIn('type', ['valid_id', 'business_permit'])->name('applications.document');
            });
            Route::middleware('can:franchises.manage')->group(function (): void {
                Route::post('operator-applications/{application}/approve', [OperatorApplicationController::class, 'approve'])->name('applications.approve');
                Route::post('operator-applications/{application}/reject', [OperatorApplicationController::class, 'reject'])->name('applications.reject');
                Route::middleware(RequireFreshConfirmation::class)->group(function (): void {
                    Route::post('franchises/{province}/license', [LicenseController::class, 'assign'])->name('licenses.assign');
                    Route::post('licenses/{license}/payments', [LicenseController::class, 'recordPayment'])->name('licenses.payments.store');
                    Route::post('licenses/{license}/suspend', [LicenseController::class, 'suspend'])->name('licenses.suspend');
                    Route::post('licenses/{license}/reinstate', [LicenseController::class, 'reinstate'])->name('licenses.reinstate');
                });
            });

            // Stores and riders across provinces, with operator override (§9.5 step 4).
            Route::middleware('can:partners.view')->group(function (): void {
                Route::get('stores', [StoreController::class, 'index'])->name('stores.index');
                Route::get('stores/{store}', [StoreController::class, 'show'])->name('stores.show');
                Route::get('riders', [RiderController::class, 'index'])->name('riders.index');
                Route::get('riders/{rider}', [RiderController::class, 'show'])->name('riders.show');
                Route::get('documents/stores/{document}', [DocumentController::class, 'store'])->name('documents.store');
                Route::get('documents/riders/{document}', [DocumentController::class, 'rider'])->name('documents.rider');
            });
            Route::middleware('can:partners.manage')->group(function (): void {
                Route::post('stores/{store}/approve', [StoreController::class, 'approve'])->name('stores.approve');
                Route::post('stores/{store}/reject', [StoreController::class, 'reject'])->name('stores.reject');
                Route::post('riders/{rider}/approve', [RiderController::class, 'approve'])->name('riders.approve');
                Route::post('riders/{rider}/reject', [RiderController::class, 'reject'])->name('riders.reject');
                Route::middleware(RequireFreshConfirmation::class)->group(function (): void {
                    Route::post('stores/{store}/status', [StoreController::class, 'changeStatus'])->name('stores.status');
                    Route::post('riders/{rider}/status', [RiderController::class, 'changeStatus'])->name('riders.status');
                });
            });

            // User accounts (§9.5 step 3).
            Route::middleware('can:users.view')->group(function (): void {
                Route::get('users', [UserController::class, 'index'])->name('users.index');
                Route::get('users/{user}', [UserController::class, 'show'])->name('users.show');
            });
            Route::middleware(['can:users.manage', RequireFreshConfirmation::class])->group(function (): void {
                Route::post('users/{user}/suspend', [UserController::class, 'suspend'])->name('users.suspend');
                Route::post('users/{user}/reactivate', [UserController::class, 'reactivate'])->name('users.reactivate');
            });

            Route::get('audit', AuditLogController::class)->middleware('can:audit.view')->name('audit.index');

            // My security, and admin staff (super admin only; the Actions check it too).
            Route::get('settings/security', [SecurityController::class, 'edit'])->middleware(RequirePassword::class)->name('settings.security');
            Route::put('settings/password', [SecurityController::class, 'updatePassword'])->middleware('throttle:6,1')->name('settings.password');
            Route::delete('settings/sessions', [SecurityController::class, 'endOtherSessions'])->middleware(RequireFreshConfirmation::class)->name('settings.sessions.destroy');
            Route::middleware('can:admin.staff')->group(function (): void {
                Route::get('settings/staff', [StaffController::class, 'index'])->name('staff.index');
                Route::middleware(RequireFreshConfirmation::class)->group(function (): void {
                    Route::post('settings/staff', [StaffController::class, 'store'])->name('staff.store');
                    Route::put('settings/staff/{staff}', [StaffController::class, 'update'])->name('staff.update');
                    Route::put('settings/staff/{staff}/password', [StaffController::class, 'resetPassword'])->name('staff.password');
                    Route::delete('settings/staff/{staff}/two-factor', [StaffController::class, 'resetTwoFactor'])->name('staff.two-factor');
                    Route::delete('settings/staff/{staff}', [StaffController::class, 'destroy'])->name('staff.destroy');
                });
            });
        });
    });
});
