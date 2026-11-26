<?php

namespace App\Http\Controllers\Admin\Franchises;

use App\Http\Controllers\Admin\AdminController;
use App\Models\Province;
use App\Queries\Admin\FranchiseQueries;
use Inertia\Inertia;
use Inertia\Response;

/**
 * Franchises: every province with its license (§9.5 step 3).
 */
class ProvinceController extends AdminController
{
    public function index(FranchiseQueries $queries): Response
    {
        return Inertia::render('franchises/index', ['provinces' => $this->deferred(fn (): array => $queries->provinces())]);
    }

    public function show(Province $province, FranchiseQueries $queries): Response
    {
        return Inertia::render('franchises/show', ['province' => $queries->province($province)]);
    }
}
