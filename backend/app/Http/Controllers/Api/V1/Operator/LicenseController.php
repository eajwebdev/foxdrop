<?php

namespace App\Http\Controllers\Api\V1\Operator;

use App\Http\Controllers\Controller;
use App\Http\Resources\LicenseResource;
use App\Models\ProvinceLicense;
use Illuminate\Http\Request;

/**
 * The operator's license card (§9.4 step 8): status, days left, whether it can be paid or renewed.
 */
class LicenseController extends Controller
{
    public function __invoke(Request $request): LicenseResource
    {
        $license = ProvinceLicense::query()
            ->with('province')
            ->where('operator_user_id', $request->user()?->id)
            ->where('status', '<>', ProvinceLicense::REJECTED)
            ->latest('created_at')
            ->latest('id')
            ->firstOrFail();

        return LicenseResource::make($license);
    }
}
