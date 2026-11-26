<?php

namespace App\Http\Controllers\Admin\Partners;

use App\Actions\Partners\ChangeStoreStatus;
use App\Actions\Partners\ReviewStore;
use App\Http\Controllers\Admin\AdminController;
use App\Http\Requests\Admin\ChangePartnerStatusRequest;
use App\Http\Requests\Admin\ReasonRequest;
use App\Models\Store;
use App\Queries\Admin\FranchiseQueries;
use App\Queries\Admin\PartnerQueries;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Inertia\Inertia;
use Inertia\Response;

/**
 * Stores across provinces, with the admin override (§9.5 step 4). Same Actions as the operator's app.
 */
class StoreController extends AdminController
{
    public function index(Request $request, PartnerQueries $queries): Response
    {
        $filters = [
            'status' => $request->has('status') ? $this->filter($request, 'status', Store::REVIEW_FILTERS) : Store::SUBMITTED,
            'province' => $this->filter($request, 'province'),
            'q' => $this->filter($request, 'q'),
        ];

        return Inertia::render('stores/index', [
            'filters' => $filters,
            'statuses' => Store::REVIEW_FILTERS,
            'provinces' => FranchiseQueries::provinceOptions(),
            'stores' => $this->deferred(fn (): array => $queries->stores($filters, $this->page($request))),
        ]);
    }

    public function show(Store $store, PartnerQueries $queries): Response
    {
        return Inertia::render('stores/show', ['store' => $queries->store($store)]);
    }

    public function approve(Request $request, Store $store, ReviewStore $review): RedirectResponse
    {
        $review($this->admin($request), $store, approve: true);

        return $this->done('Store approved. It is live for customers.');
    }

    public function reject(ReasonRequest $request, Store $store, ReviewStore $review): RedirectResponse
    {
        $review($this->admin($request), $store, approve: false, reason: $request->reason());

        return $this->done('Store application rejected.');
    }

    public function changeStatus(ChangePartnerStatusRequest $request, Store $store, ChangeStoreStatus $change): RedirectResponse
    {
        $change($this->admin($request), $store, $request->string('status')->toString(), $request->reason());

        return $this->done('Store status changed.');
    }
}
