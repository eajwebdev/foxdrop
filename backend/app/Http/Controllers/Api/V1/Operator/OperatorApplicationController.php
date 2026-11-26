<?php

namespace App\Http\Controllers\Api\V1\Operator;

use App\Actions\Licensing\SubmitOperatorApplication;
use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\Operator\SubmitOperatorApplicationRequest;
use App\Http\Resources\OperatorApplicationResource;
use App\Models\OperatorApplication;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Becoming a provincial operator (§9.4 step 1): apply, then follow the application.
 */
class OperatorApplicationController extends Controller
{
    public function store(SubmitOperatorApplicationRequest $request, SubmitOperatorApplication $submit): JsonResponse
    {
        /** @var User $user */
        $user = $request->user();
        $application = $submit(
            $user,
            [
                'province_id' => $request->string('province_id')->toString(),
                'business_name' => $request->string('business_name')->toString(),
                'registration_number' => $request->filled('registration_number') ? $request->string('registration_number')->toString() : null,
                'contact_phone' => $request->string('contact_phone')->toString(),
                'contact_email' => $request->filled('contact_email') ? $request->string('contact_email')->toString() : null,
            ],
            $request->file('valid_id'),
            $request->file('business_permit'),
        );

        return OperatorApplicationResource::make($application->load('province', 'license.province'))->response()->setStatusCode(201);
    }

    public function mine(Request $request): JsonResponse
    {
        $application = OperatorApplication::query()
            ->with('province', 'license.province')
            ->where('applicant_user_id', $request->user()?->id)
            ->latest('created_at')
            ->latest('id')
            ->first();

        return response()->json(['data' => $application === null ? null : OperatorApplicationResource::make($application)]);
    }
}
