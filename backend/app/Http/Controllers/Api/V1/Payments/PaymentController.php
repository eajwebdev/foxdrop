<?php

namespace App\Http\Controllers\Api\V1\Payments;

use App\Http\Controllers\Controller;
use App\Http\Resources\PaymentResource;
use App\Models\Payment;
use Illuminate\Http\Request;

/**
 * The app polls this after the checkout (docs/API.md "How a payment finishes").
 */
class PaymentController extends Controller
{
    public function show(Request $request, Payment $payment): PaymentResource
    {
        abort_unless($payment->payer_user_id === $request->user()?->id, 404);

        return PaymentResource::make($payment);
    }
}
