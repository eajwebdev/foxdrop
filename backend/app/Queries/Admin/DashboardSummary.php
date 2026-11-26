<?php

namespace App\Queries\Admin;

use App\Models\OperatorApplication;
use App\Models\ProvinceLicense;
use App\Models\Rider;
use App\Models\Store;
use Illuminate\Support\Facades\DB;

/**
 * W1 dashboard: what needs the admin now, and the platform at a glance (§20.4 step 2; full KPIs and charts in W2).
 */
final class DashboardSummary
{
    /**
     * @return array<string, mixed>
     */
    public function __invoke(): array
    {
        $licenseCounts = ProvinceLicense::query()->select('status', DB::raw('count(*) as total'))->groupBy('status')->pluck('total', 'status');
        $expiring = ProvinceLicense::query()
            ->with(['province:id,name'])
            ->where('status', ProvinceLicense::ACTIVE)
            ->where('end_at', '<', now()->addDays(30))
            ->orderBy('end_at')
            ->limit(5)
            ->get();

        return [
            'attention' => [
                'operator_applications' => OperatorApplication::query()->where('status', OperatorApplication::SUBMITTED)->count(),
                'stores' => Store::query()->where('status', Store::SUBMITTED)->count(),
                'riders' => Rider::query()->where('status', Rider::DOCS_REVIEW)->count(),
                'awaiting_payment' => (int) ($licenseCounts[ProvinceLicense::PENDING_PAYMENT] ?? 0),
                'in_grace' => (int) ($licenseCounts[ProvinceLicense::GRACE] ?? 0),
            ],
            'totals' => [
                'active_provinces' => (int) ($licenseCounts[ProvinceLicense::ACTIVE] ?? 0) + (int) ($licenseCounts[ProvinceLicense::GRACE] ?? 0),
                'active_stores' => Store::query()->whereIn('status', Store::PUBLISHED_STATUSES)->count(),
                'active_riders' => Rider::query()->where('status', Rider::APPROVED)->count(),
                'users' => DB::table('users')->count(),
            ],
            'expiring' => $expiring->map(fn (ProvinceLicense $license): array => [
                'id' => $license->id,
                'province' => ['id' => $license->province_id, 'name' => $license->province?->name],
                'end_at' => Present::date($license->end_at),
            ])->all(),
        ];
    }
}
