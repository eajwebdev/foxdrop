<?php

namespace App\Http\Controllers\Admin\Partners;

use App\Actions\Partners\ChangePartnerStatus;
use App\Actions\Partners\ReviewRider;
use App\Http\Controllers\Admin\AdminController;
use App\Http\Requests\Admin\ChangePartnerStatusRequest;
use App\Http\Requests\Admin\ReasonRequest;
use App\Models\Rider;
use App\Queries\Admin\FranchiseQueries;
use App\Queries\Admin\PartnerQueries;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Inertia\Inertia;
use Inertia\Response;

/**
 * Riders across provinces, with the admin override (§9.5 step 4). Same Actions as the operator's app.
 */
class RiderController extends AdminController
{
    public function index(Request $request, PartnerQueries $queries): Response
    {
        $filters = [
            'status' => $request->has('status') ? $this->filter($request, 'status', Rider::REVIEW_FILTERS) : Rider::DOCS_REVIEW,
            'province' => $this->filter($request, 'province'),
            'q' => $this->filter($request, 'q'),
        ];

        return Inertia::render('riders/index', [
            'filters' => $filters,
            'statuses' => Rider::REVIEW_FILTERS,
            'provinces' => fn (): array => FranchiseQueries::provinceOptions(), // skipped on partial reloads
            'riders' => $this->deferred(fn (): array => $queries->riders($filters, $this->page($request))),
        ]);
    }

    public function show(Rider $rider, PartnerQueries $queries): Response
    {
        return Inertia::render('riders/show', ['rider' => $queries->rider($rider)]);
    }

    public function approve(Request $request, Rider $rider, ReviewRider $review): RedirectResponse
    {
        $review($this->admin($request), $rider, approve: true);

        return $this->done('Rider approved.');
    }

    public function reject(ReasonRequest $request, Rider $rider, ReviewRider $review): RedirectResponse
    {
        $review($this->admin($request), $rider, approve: false, reason: $request->reason());

        return $this->done('Rider application rejected.');
    }

    public function changeStatus(ChangePartnerStatusRequest $request, Rider $rider, ChangePartnerStatus $change): RedirectResponse
    {
        $change($this->admin($request), $rider, $request->string('status')->toString(), $request->reason());

        return $this->done('Rider status changed.');
    }
}
