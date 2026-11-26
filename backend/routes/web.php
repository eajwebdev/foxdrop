<?php

use App\Http\Controllers\Dev\FakeCheckoutController;
use App\Http\Controllers\MediaController;
use Illuminate\Support\Facades\Route;

Route::redirect('/', '/admin');

// Public product photos (DECISIONS 92): file names are new UUIDs, so they can be cached for a year.
Route::get('media/catalog/{store}/{file}', [MediaController::class, 'catalog'])
    ->where('file', '[0-9a-f-]{36}\\.jpg')
    ->name('media.catalog');

// Where PayMongo sends the payer back after checkout; the app itself polls the payment.
Route::get('payments/return', fn () => view('payments.return', ['result' => request()->string('result')->toString()]))
    ->name('payments.return');

// Local stand-in for the PayMongo checkout page (DECISIONS 57). Never registered in production.
if (app()->environment(['local', 'testing'])) {
    Route::prefix('dev/checkout/{payment}')->name('dev.checkout.')->group(function (): void {
        Route::get('/', [FakeCheckoutController::class, 'show'])->name('show');
        Route::post('pay', [FakeCheckoutController::class, 'pay'])->name('pay');
        Route::post('cancel', [FakeCheckoutController::class, 'cancel'])->name('cancel');
    });
}

require __DIR__.'/admin.php';
