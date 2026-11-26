<?php

namespace App\Http\Controllers\Api\V1\Payments;

use App\Actions\Licensing\StartLicensePayment;
use App\Http\Controllers\Controller;
use App\Http\Resources\PaymentResource;
use App\Models\ProvinceLicense;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Pay or renew a province license (§9.4 steps 2 and 8). Returns the checkout to open in a Custom Tab.
 */
class LicensePaymentController extends Controller
{
    public function __invoke(Request $request, ProvinceLicense $license, StartLicensePayment $start): JsonResponse
    {
        /** @var User $user */
        $user = $request->user();

        return PaymentResource::make($start($user, $license))->response()->setStatusCode(201);
    }
}
