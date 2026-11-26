<?php

namespace App\Http\Controllers\Admin;

use App\Queries\Admin\AuditLogQuery;
use Illuminate\Http\Request;
use Inertia\Inertia;
use Inertia\Response;

/**
 * Audit log viewer (§20.4 step 9).
 */
class AuditLogController extends AdminController
{
    public function __invoke(Request $request, AuditLogQuery $query): Response
    {
        $filters = [
            'action' => $this->filter($request, 'action'),
            'entity' => $this->filter($request, 'entity'),
            'actor' => $this->filter($request, 'actor'),
            'province' => $this->filter($request, 'province'),
            'from' => $this->date($request, 'from'),
            'to' => $this->date($request, 'to'),
        ];

        return Inertia::render('audit/index', [
            'filters' => $filters,
            'entityTypes' => $this->deferred(fn (): array => AuditLogQuery::entityTypes()),
            'entries' => $this->deferred(fn (): array => $query($filters, $this->page($request))),
        ]);
    }

    private function date(Request $request, string $key): ?string
    {
        $value = $this->filter($request, $key);

        return $value !== null && preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) === 1 ? $value : null;
    }
}
