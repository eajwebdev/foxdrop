<?php

namespace App\Http\Controllers\Api\V1\Operator;

use App\Actions\Partners\ReviewStore;
use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\Operator\RejectApplicationRequest;
use App\Http\Resources\StoreReviewResource;
use App\Http\Resources\StoreSummaryResource;
use App\Models\Store;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;
use Illuminate\Validation\Rule;

/**
 * "Onboard local partners: approve stores" (§9.4 step 4) for the operator's province; admins see every province.
 */
class StoreReviewController extends Controller
{
    public function index(Request $request): AnonymousResourceCollection
    {
        $user = $this->reviewer($request);
        $status = $request->validate(['status' => ['sometimes', Rule::in(['SUBMITTED', 'ACTIVE', 'PAUSED', 'SUSPENDED', 'REJECTED', 'CLOSED'])]])['status'] ?? Store::SUBMITTED;

        return StoreSummaryResource::collection(
            Store::query()->reviewableBy($user)->with('owner')
                ->where('status', $status)
                ->orderBy('created_at')
                ->orderBy('id')
                ->get(),
        );
    }

    public function show(Request $request, string $store): StoreReviewResource
    {
        return StoreReviewResource::make($this->find($this->reviewer($request), $store));
    }

    public function approve(Request $request, string $store, ReviewStore $review): StoreReviewResource
    {
        $user = $this->reviewer($request);
        $review($user, $this->find($user, $store), approve: true);

        return StoreReviewResource::make($this->find($user, $store));
    }

    public function reject(RejectApplicationRequest $request, string $store, ReviewStore $review): StoreReviewResource
    {
        $user = $this->reviewer($request);
        $review($user, $this->find($user, $store), approve: false, reason: $request->string('reason')->toString());

        return StoreReviewResource::make($this->find($user, $store));
    }

    /**
     * Another province's store is "not found", not "forbidden": its existence is not shared.
     */
    private function find(User $user, string $id): Store
    {
        return Store::query()->withLocation()->reviewableBy($user)->with('owner', 'documents')->findOrFail($id);
    }

    private function reviewer(Request $request): User
    {
        /** @var User $user */
        $user = $request->user();
        abort_unless($user->isPlatformAdmin() || $user->operatorProvinceIds() !== [], 403, 'This needs a provincial operator account.');

        return $user;
    }
}
