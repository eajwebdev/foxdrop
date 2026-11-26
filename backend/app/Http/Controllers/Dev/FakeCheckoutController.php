<?php

namespace App\Http\Controllers\Dev;

use App\Actions\Payments\SettlePayment;
use App\Http\Controllers\Controller;
use App\Models\Payment;
use Illuminate\Contracts\View\View;

/**
 * The local stand-in for PayMongo's checkout page (DECISIONS 57). Registered only in local and testing.
 */
class FakeCheckoutController extends Controller
{
    public function show(Payment $payment): View
    {
        return view('dev.checkout', ['payment' => $payment, 'done' => null]);
    }

    public function pay(Payment $payment, SettlePayment $settle): View
    {
        return view('dev.checkout', ['payment' => $settle->succeeded($payment), 'done' => 'paid']);
    }

    public function cancel(Payment $payment, SettlePayment $settle): View
    {
        return view('dev.checkout', ['payment' => $settle->failed($payment), 'done' => 'cancelled']);
    }
}
