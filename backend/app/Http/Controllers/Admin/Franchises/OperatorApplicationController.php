<?php

namespace App\Http\Controllers\Admin\Franchises;

use App\Actions\Licensing\ReviewOperatorApplication;
use App\Http\Controllers\Admin\AdminController;
use App\Http\Requests\Admin\ReasonRequest;
use App\Models\OperatorApplication;
use App\Queries\Admin\FranchiseQueries;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Inertia\Inertia;
use Inertia\Response;

/**
 * Operator applications (§9.5 step 3). Approve / reject run the same Action as the Artisan command (DECISIONS 55).
 */
class OperatorApplicationController extends AdminController
{
    private const array STATUSES = [OperatorApplication::SUBMITTED, OperatorApplication::APPROVED, OperatorApplication::REJECTED, 'WITHDRAWN'];

    public function index(Request $request, FranchiseQueries $queries): Response
    {
        $status = $request->has('status') ? $this->filter($request, 'status', self::STATUSES) : OperatorApplication::SUBMITTED;

        return Inertia::render('franchises/applications/index', [
            'filters' => ['status' => $status],
            'applications' => $this->deferred(fn (): array => $queries->applications($status, $this->page($request))),
        ]);
    }

    public function show(OperatorApplication $application, FranchiseQueries $queries): Response
    {
        return Inertia::render('franchises/applications/show', ['application' => $queries->application($application)]);
    }

    public function approve(Request $request, OperatorApplication $application, ReviewOperatorApplication $review): RedirectResponse
    {
        $review($this->admin($request), $application, approve: true);

        return $this->done('Application approved. The applicant can now pay the license fee.');
    }

    public function reject(ReasonRequest $request, OperatorApplication $application, ReviewOperatorApplication $review): RedirectResponse
    {
        $review($this->admin($request), $application, approve: false, reason: $request->reason());

        return $this->done('Application rejected. The province is open again.');
    }
}
