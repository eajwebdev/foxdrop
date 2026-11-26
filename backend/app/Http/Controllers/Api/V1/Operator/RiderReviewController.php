<?php

namespace App\Http\Controllers\Api\V1\Operator;

use App\Actions\Partners\ReviewRider;
use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\Operator\RejectApplicationRequest;
use App\Http\Resources\RiderReviewResource;
use App\Http\Resources\RiderSummaryResource;
use App\Models\Rider;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;
use Illuminate\Validation\Rule;

/**
 * "Recruit and verify riders" (§9.4 step 4) for the operator's province; admins see every province.
 */
class RiderReviewController extends Controller
{
    public function index(Request $request): AnonymousResourceCollection
    {
        $user = $this->reviewer($request);
        $status = $request->validate(['status' => ['sometimes', Rule::in(['DOCS_REVIEW', 'APPROVED', 'REJECTED', 'SUSPENDED', 'DEACTIVATED'])]])['status'] ?? Rider::DOCS_REVIEW;

        return RiderSummaryResource::collection(
            Rider::query()->reviewableBy($user)->with('user')
                ->where('status', $status)
                ->orderBy('created_at')
                ->orderBy('id')
                ->get(),
        );
    }

    public function show(Request $request, string $rider): RiderReviewResource
    {
        return RiderReviewResource::make($this->find($this->reviewer($request), $rider));
    }

    public function approve(Request $request, string $rider, ReviewRider $review): RiderReviewResource
    {
        $user = $this->reviewer($request);
        $review($user, $this->find($user, $rider), approve: true);

        return RiderReviewResource::make($this->find($user, $rider));
    }

    public function reject(RejectApplicationRequest $request, string $rider, ReviewRider $review): RiderReviewResource
    {
        $user = $this->reviewer($request);
        $review($user, $this->find($user, $rider), approve: false, reason: $request->string('reason')->toString());

        return RiderReviewResource::make($this->find($user, $rider));
    }

    private function find(User $user, string $id): Rider
    {
        return Rider::query()->reviewableBy($user)->with('user', 'documents')->findOrFail($id);
    }

    private function reviewer(Request $request): User
    {
        /** @var User $user */
        $user = $request->user();
        abort_unless($user->isPlatformAdmin() || $user->operatorProvinceIds() !== [], 403, 'This needs a provincial operator account.');

        return $user;
    }
}
