<?php

namespace App\Http\Controllers\Api\V1\Operator;

use App\Actions\Partners\ReviewStore;
use App\Http\Resources\StoreReviewResource;
use App\Http\Resources\StoreSummaryResource;
use App\Models\Store;
use App\Models\User;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Collection;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * Store applications ("approve stores", §9.4 step 4).
 *
 * @extends PartnerReviewController<Store>
 */
class StoreReviewController extends PartnerReviewController
{
    public function __construct(private readonly ReviewStore $reviewStore) {}

    protected function statusFilters(): array
    {
        return Store::REVIEW_FILTERS;
    }

    protected function pendingStatus(): string
    {
        return Store::SUBMITTED;
    }

    protected function applicantRelation(): string
    {
        return 'owner';
    }

    protected function reviewable(User $reviewer): Builder
    {
        return Store::query()->withLocation()->reviewableBy($reviewer);
    }

    protected function decide(User $reviewer, Model $partner, bool $approve, ?string $reason): Model
    {
        return ($this->reviewStore)($reviewer, $partner, $approve, $reason);
    }

    protected function summaries(Collection $partners): AnonymousResourceCollection
    {
        return StoreSummaryResource::collection($partners);
    }

    protected function detail(Model $partner): JsonResource
    {
        return StoreReviewResource::make($partner);
    }
}
