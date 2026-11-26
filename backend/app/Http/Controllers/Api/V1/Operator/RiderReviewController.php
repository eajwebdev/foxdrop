<?php

namespace App\Http\Controllers\Api\V1\Operator;

use App\Actions\Partners\ReviewRider;
use App\Http\Resources\RiderReviewResource;
use App\Http\Resources\RiderSummaryResource;
use App\Models\Rider;
use App\Models\User;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Collection;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * Rider applications ("recruit and verify riders", §9.4 step 4).
 *
 * @extends PartnerReviewController<Rider>
 */
class RiderReviewController extends PartnerReviewController
{
    public function __construct(private readonly ReviewRider $reviewRider) {}

    protected function statusFilters(): array
    {
        return Rider::REVIEW_FILTERS;
    }

    protected function pendingStatus(): string
    {
        return Rider::DOCS_REVIEW;
    }

    protected function applicantRelation(): string
    {
        return 'user';
    }

    protected function reviewable(User $reviewer): Builder
    {
        return Rider::query()->reviewableBy($reviewer);
    }

    protected function decide(User $reviewer, Model $partner, bool $approve, ?string $reason): Model
    {
        return ($this->reviewRider)($reviewer, $partner, $approve, $reason);
    }

    protected function summaries(Collection $partners): AnonymousResourceCollection
    {
        return RiderSummaryResource::collection($partners);
    }

    protected function detail(Model $partner): JsonResource
    {
        return RiderReviewResource::make($partner);
    }
}
