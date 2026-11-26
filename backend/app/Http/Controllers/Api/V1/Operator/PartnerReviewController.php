<?php

namespace App\Http\Controllers\Api\V1\Operator;

use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\Operator\RejectApplicationRequest;
use App\Models\User;
use App\Support\OperatorContext;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Collection;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;
use Illuminate\Http\Resources\Json\JsonResource;
use Illuminate\Validation\Rule;

/**
 * "Onboard local partners" (§9.4 step 4): the review queue, detail, approve and reject for one partner type, in the
 * reviewer's province (platform admins: every province). Another province's application is "not found", so its
 * existence is not shared.
 *
 * @template TPartner of Model
 */
abstract class PartnerReviewController extends Controller
{
    /**
     * @return list<string>
     */
    abstract protected function statusFilters(): array;

    abstract protected function pendingStatus(): string;

    /** The relation holding the applicant shown in lists ('owner' for stores, 'user' for riders). */
    abstract protected function applicantRelation(): string;

    /**
     * Applications this reviewer may see.
     *
     * @return Builder<TPartner>
     */
    abstract protected function reviewable(User $reviewer): Builder;

    /**
     * Runs the review Action and returns the reviewed row.
     *
     * @param  TPartner  $partner
     * @return TPartner
     */
    abstract protected function decide(User $reviewer, Model $partner, bool $approve, ?string $reason): Model;

    /**
     * @param  Collection<int, TPartner>  $partners
     */
    abstract protected function summaries(Collection $partners): AnonymousResourceCollection;

    /**
     * @param  TPartner  $partner
     */
    abstract protected function detail(Model $partner): JsonResource;

    public function index(Request $request): AnonymousResourceCollection
    {
        $reviewer = $this->reviewer($request);
        $status = $request->validate(['status' => ['sometimes', Rule::in($this->statusFilters())]])['status'] ?? $this->pendingStatus();

        return $this->summaries(
            $this->reviewable($reviewer)->with($this->applicantRelation())
                ->where('status', $status)
                ->orderBy('created_at')
                ->orderBy('id')
                ->get(),
        );
    }

    public function show(Request $request, string $partner): JsonResource
    {
        return $this->detail($this->find($this->reviewer($request), $partner));
    }

    public function approve(Request $request, string $partner): JsonResource
    {
        return $this->review($this->reviewer($request), $partner, approve: true, reason: null);
    }

    public function reject(RejectApplicationRequest $request, string $partner): JsonResource
    {
        return $this->review($this->reviewer($request), $partner, approve: false, reason: $request->string('reason')->toString());
    }

    private function review(User $reviewer, string $id, bool $approve, ?string $reason): JsonResource
    {
        $partner = $this->find($reviewer, $id);
        $reviewed = $this->decide($reviewer, $partner, $approve, $reason);

        // Answer from the row already loaded: take the new status, reload only the documents (their status changed).
        $partner->forceFill($reviewed->getAttributes())->syncOriginal();
        $partner->load('documents');

        return $this->detail($partner);
    }

    /**
     * @return TPartner
     */
    private function find(User $reviewer, string $id): Model
    {
        return $this->reviewable($reviewer)->with([$this->applicantRelation(), 'documents'])->findOrFail($id);
    }

    private function reviewer(Request $request): User
    {
        /** @var User $user */
        $user = $request->user();
        OperatorContext::ensureReviewer($user);

        return $user;
    }
}
