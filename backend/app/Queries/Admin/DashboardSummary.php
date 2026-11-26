<?php

namespace App\Queries\Admin;

use App\Models\OperatorApplication;
use App\Models\ProvinceLicense;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\DB;

/**
 * W1 dashboard: what needs the admin now, and the platform at a glance (§20.4 step 2; full KPIs and charts in W2).
 * The page polls every 15 s, so the numbers are computed once per 15 s for every admin, in one query per table.
 */
final class DashboardSummary
{
    /**
     * @return array<string, mixed>
     */
    public function __invoke(): array
    {
        return Cache::remember('admin.dashboard.summary', now()->addSeconds(15), fn (): array => $this->compute());
    }

    /**
     * @return array<string, mixed>
     */
    private function compute(): array
    {
        $licenses = ProvinceLicense::query()->select('status', DB::raw('count(*) as total'))->groupBy('status')->pluck('total', 'status');
        // One scan per table for both numbers.
        $stores = DB::table('stores')->selectRaw("count(*) filter (where status = 'SUBMITTED') as waiting, count(*) filter (where status in ('ACTIVE', 'PAUSED')) as active")->first();
        $riders = DB::table('riders')->selectRaw("count(*) filter (where status = 'DOCS_REVIEW') as waiting, count(*) filter (where status = 'APPROVED') as active")->first();
        $count = fn (string $status): int => (int) ($licenses[$status] ?? 0);

        return [
            'attention' => [
                'operator_applications' => OperatorApplication::query()->where('status', OperatorApplication::SUBMITTED)->count(),
                'stores' => (int) ($stores->waiting ?? 0),
                'riders' => (int) ($riders->waiting ?? 0),
                'awaiting_payment' => $count(ProvinceLicense::PENDING_PAYMENT),
                'in_grace' => $count(ProvinceLicense::GRACE),
            ],
            'totals' => [
                'active_provinces' => $count(ProvinceLicense::ACTIVE) + $count(ProvinceLicense::GRACE),
                'active_stores' => (int) ($stores->active ?? 0),
                'active_riders' => (int) ($riders->active ?? 0),
                'users' => DB::table('users')->count(), // exact; the 15 s cache keeps it to one scan for every admin
            ],
            'expiring' => ProvinceLicense::query()
                ->with(['province:id,name'])
                ->where('status', ProvinceLicense::ACTIVE)
                ->where('end_at', '<', now()->addDays(30))
                ->orderBy('end_at')
                ->limit(5)
                ->get()
                ->map(fn (ProvinceLicense $license): array => [
                    'id' => $license->id,
                    'province' => Present::province($license),
                    'end_at' => Present::date($license->end_at),
                ])->all(),
        ];
    }
}
