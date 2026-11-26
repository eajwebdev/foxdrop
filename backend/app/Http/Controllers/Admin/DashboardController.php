<?php

namespace App\Http\Controllers\Admin;

use App\Queries\Admin\DashboardSummary;
use Inertia\Inertia;
use Inertia\Response;

class DashboardController extends AdminController
{
    public function __invoke(DashboardSummary $summary): Response
    {
        return Inertia::render('dashboard', ['summary' => $this->deferred(fn (): array => $summary())]);
    }
}
