<?php

use App\Http\Controllers\Api\V1\Auth\EmailAuthController;
use App\Http\Controllers\Api\V1\Auth\GoogleAuthController;
use App\Http\Controllers\Api\V1\Auth\PhoneOtpController;
use App\Http\Controllers\Api\V1\Auth\SessionController;
use App\Http\Controllers\Api\V1\DocumentController;
use App\Http\Controllers\Api\V1\MeController;
use App\Http\Controllers\Api\V1\Operator\LicenseController;
use App\Http\Controllers\Api\V1\Operator\OperatorApplicationController;
use App\Http\Controllers\Api\V1\Operator\ProvinceSettingsController;
use App\Http\Controllers\Api\V1\Operator\RateCardController;
use App\Http\Controllers\Api\V1\Operator\RiderReviewController;
use App\Http\Controllers\Api\V1\Operator\StoreReviewController;
use App\Http\Controllers\Api\V1\Operator\ZoneController;
use App\Http\Controllers\Api\V1\Ordering\AddressController;
use App\Http\Controllers\Api\V1\Ordering\BrowseController;
use App\Http\Controllers\Api\V1\Ordering\OrderController;
use App\Http\Controllers\Api\V1\Partners\RiderApplicationController;
use App\Http\Controllers\Api\V1\Partners\StoreApplicationController;
use App\Http\Controllers\Api\V1\Payments\LicensePaymentController;
use App\Http\Controllers\Api\V1\Payments\PaymentController;
use App\Http\Controllers\Api\V1\Payments\PayMongoWebhookController;
use App\Http\Controllers\Api\V1\ProvinceController;
use App\Http\Controllers\Api\V1\StoreTeam\StoreCatalogController;
use App\Http\Controllers\Api\V1\StoreTeam\StoreOrderController;
use App\Http\Middleware\EnsureAccountIsActive;
use Illuminate\Support\Facades\Route;

/*
| FoxDrop API v1 for the three Android apps (docs/API.md). Every route returns JSON.
*/
Route::prefix('v1')->name('api.v1.')->group(function (): void {
    Route::get('provinces', ProvinceController::class)->name('provinces.index');

    // Browsing (§9.1 steps 2–3), also for signed-out visitors.
    Route::middleware('throttle:browse')->group(function (): void {
        Route::get('stores', [BrowseController::class, 'stores'])->name('stores.index');
        Route::get('stores/{store}', [BrowseController::class, 'show'])->name('stores.show');
        Route::get('search', [BrowseController::class, 'search'])->name('search');
    });

    Route::prefix('auth')->name('auth.')->group(function (): void {
        Route::post('otp/send', [PhoneOtpController::class, 'send'])->middleware('throttle:otp-send')->name('otp.send');
        Route::post('otp/verify', [PhoneOtpController::class, 'verify'])->middleware('throttle:otp-verify')->name('otp.verify');
        Route::post('email/register', [EmailAuthController::class, 'register'])->middleware('throttle:email-sign-in')->name('email.register');
        Route::post('email/login', [EmailAuthController::class, 'login'])->middleware('throttle:email-sign-in')->name('email.login');
        Route::post('google', GoogleAuthController::class)->middleware('throttle:google-sign-in')->name('google');
    });

    // Payment provider callbacks (signature-checked, no user).
    Route::post('webhooks/paymongo', PayMongoWebhookController::class)->name('webhooks.paymongo');

    // Partner document photos for reviewers: signed 10-minute links (DECISIONS 60).
    Route::middleware('signed')->name('documents.')->group(function (): void {
        Route::get('documents/stores/{document}', [DocumentController::class, 'store'])->name('store');
        Route::get('documents/riders/{document}', [DocumentController::class, 'rider'])->name('rider');
    });

    Route::middleware(['auth:sanctum', EnsureAccountIsActive::class])->group(function (): void {
        Route::get('me', MeController::class)->name('me');
        Route::post('auth/logout', [SessionController::class, 'destroy'])->name('auth.logout');

        // Becoming an operator, paying and renewing the license (§9.4 steps 1, 2, 8).
        Route::post('operator/applications', [OperatorApplicationController::class, 'store'])->middleware('throttle:uploads')->name('operator.applications.store');
        Route::get('operator/applications/mine', [OperatorApplicationController::class, 'mine'])->name('operator.applications.mine');
        Route::get('operator/license', LicenseController::class)->name('operator.license');
        Route::post('licenses/{license}/payments', LicensePaymentController::class)->name('licenses.payments.store');
        Route::get('payments/{payment}', [PaymentController::class, 'show'])->name('payments.show');

        // Ordering (§9.1 steps 4–7).
        Route::get('addresses', [AddressController::class, 'index'])->name('addresses.index');
        Route::post('addresses', [AddressController::class, 'store'])->name('addresses.store');
        Route::put('addresses/{address}', [AddressController::class, 'update'])->name('addresses.update');
        Route::delete('addresses/{address}', [AddressController::class, 'destroy'])->name('addresses.destroy');
        Route::post('quotes', [OrderController::class, 'quote'])->middleware('throttle:quotes')->name('quotes.store');
        Route::post('orders', [OrderController::class, 'store'])->middleware('throttle:quotes')->name('orders.store');
        Route::get('orders', [OrderController::class, 'index'])->name('orders.index');
        Route::get('orders/{order}', [OrderController::class, 'show'])->name('orders.show');
        Route::post('orders/{order}/cancel', [OrderController::class, 'cancel'])->name('orders.cancel');

        // The store team (§9.3): menu, settings, order board. Categories, products and orders must belong to {store}.
        Route::prefix('my-stores/{store}')->name('my-stores.')->scopeBindings()->group(function (): void {
            Route::get('catalog', [StoreCatalogController::class, 'show'])->name('catalog');
            Route::post('categories', [StoreCatalogController::class, 'storeCategory'])->name('categories.store');
            Route::put('categories/{category}', [StoreCatalogController::class, 'updateCategory'])->name('categories.update');
            Route::delete('categories/{category}', [StoreCatalogController::class, 'destroyCategory'])->name('categories.destroy');
            Route::post('products', [StoreCatalogController::class, 'storeProduct'])->middleware('throttle:uploads')->name('products.store');
            Route::post('products/{product}', [StoreCatalogController::class, 'updateProduct'])->middleware('throttle:uploads')->name('products.update');
            Route::patch('products/{product}/availability', [StoreCatalogController::class, 'availability'])->name('products.availability');
            Route::delete('products/{product}', [StoreCatalogController::class, 'destroyProduct'])->name('products.destroy');
            Route::patch('settings', [StoreOrderController::class, 'settings'])->name('settings');
            Route::get('orders', [StoreOrderController::class, 'index'])->name('orders.index');
            Route::get('orders/{order}', [StoreOrderController::class, 'show'])->name('orders.show');
            Route::post('orders/{order}/accept', [StoreOrderController::class, 'accept'])->name('orders.accept');
            Route::post('orders/{order}/reject', [StoreOrderController::class, 'reject'])->name('orders.reject');
            Route::post('orders/{order}/preparing', [StoreOrderController::class, 'preparing'])->name('orders.preparing');
            Route::post('orders/{order}/ready', [StoreOrderController::class, 'ready'])->name('orders.ready');
            Route::post('orders/{order}/complete', [StoreOrderController::class, 'complete'])->middleware('throttle:pickup-codes')->name('orders.complete');
        });

        // Territory of the operator's province (§9.4 steps 3, 5).
        Route::prefix('operator')->name('operator.')->group(function (): void {
            Route::get('zones', [ZoneController::class, 'index'])->name('zones.index');
            Route::post('zones', [ZoneController::class, 'store'])->name('zones.store');
            Route::delete('zones/{zone}', [ZoneController::class, 'destroy'])->name('zones.destroy');
            Route::get('rate-card', [RateCardController::class, 'show'])->name('rate-card.show');
            Route::post('rate-cards', [RateCardController::class, 'store'])->name('rate-cards.store');
            Route::post('rate-cards/preview', [RateCardController::class, 'preview'])->name('rate-cards.preview');
            Route::get('province', [ProvinceSettingsController::class, 'show'])->name('province.show');
            Route::patch('province', [ProvinceSettingsController::class, 'update'])->name('province.update');

            // Partner reviews (§9.4 step 4).
            Route::get('stores', [StoreReviewController::class, 'index'])->name('stores.index');
            Route::get('stores/{store}', [StoreReviewController::class, 'show'])->name('stores.show');
            Route::post('stores/{store}/approve', [StoreReviewController::class, 'approve'])->name('stores.approve');
            Route::post('stores/{store}/reject', [StoreReviewController::class, 'reject'])->name('stores.reject');
            Route::get('riders', [RiderReviewController::class, 'index'])->name('riders.index');
            Route::get('riders/{rider}', [RiderReviewController::class, 'show'])->name('riders.show');
            Route::post('riders/{rider}/approve', [RiderReviewController::class, 'approve'])->name('riders.approve');
            Route::post('riders/{rider}/reject', [RiderReviewController::class, 'reject'])->name('riders.reject');
        });

        // Partner applications (§5.2, §5.3).
        Route::post('stores', [StoreApplicationController::class, 'store'])->middleware('throttle:uploads')->name('stores.store');
        Route::get('stores/mine', [StoreApplicationController::class, 'mine'])->name('stores.mine');
        Route::post('riders', [RiderApplicationController::class, 'store'])->middleware('throttle:uploads')->name('riders.store');
        Route::get('riders/me', [RiderApplicationController::class, 'me'])->name('riders.me');
    });
});
