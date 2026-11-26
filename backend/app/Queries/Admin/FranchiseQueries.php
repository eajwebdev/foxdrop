<?php

namespace App\Queries\Admin;

use App\Models\OperatorApplication;
use App\Models\Payment;
use App\Models\Province;
use App\Models\ProvinceLicense;
use App\Models\Rider;
use App\Models\Store;
use App\Models\User;
use App\Support\PlatformSettings;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Support\Facades\DB;

/**
 * Provinces, their licenses and operator applications for the Franchises pages (§9.5 step 3).
 */
final class FranchiseQueries
{
    /**
     * All provinces (82 + NCR) with their current license; the page filters and sorts them.
     *
     * @return list<array<string, mixed>>
     */
    public function provinces(): array
    {
        $directory = DB::table('province_directory')->orderBy('name')->get(['id', 'name', 'region_name', 'is_serviceable', 'is_available']);
        $enabled = Province::query()->pluck('is_enabled', 'id');
        /** @var list<string> $ids */
        $ids = $directory->pluck('id')->all();
        $licenses = ProvinceLicense::currentByProvince($ids);
        $operators = Present::peopleById(array_map(fn (ProvinceLicense $license): string => $license->operator_user_id, array_values($licenses)));

        return array_values($directory->map(function (object $row) use ($enabled, $licenses, $operators): array {
            $license = $licenses[$row->id] ?? null;

            return [
                'id' => $row->id,
                'name' => $row->name,
                'region_name' => $row->region_name,
                'is_enabled' => (bool) ($enabled[$row->id] ?? false),
                'is_serviceable' => (bool) $row->is_serviceable,
                'is_available' => (bool) $row->is_available,
                'license' => $license === null ? null : [
                    'id' => $license->id,
                    'status' => $license->status,
                    'end_at' => Present::date($license->end_at),
                    'operator' => Present::person($operators->get($license->operator_user_id)),
                ],
            ];
        })->all());
    }

    /**
     * @return array<string, mixed>
     */
    public function province(Province $province): array
    {
        $flags = DB::table('province_directory')->where('id', $province->id)->first(['is_serviceable', 'is_available']);
        $licenses = ProvinceLicense::query()->where('province_id', $province->id)->orderByDesc('created_at')->get();
        $people = Present::peopleById($licenses->pluck('operator_user_id'));
        $payments = Payment::query()
            ->whereIn('license_id', $licenses->pluck('id'))
            ->where('status', Payment::PAID)
            ->orderByDesc('created_at')
            ->get()
            ->groupBy('license_id');
        $current = ProvinceLicense::currentByProvince([$province->id])[$province->id] ?? null;

        return [
            'id' => $province->id,
            'name' => $province->name,
            'region_name' => $province->region_name,
            'is_enabled' => $province->is_enabled,
            'is_serviceable' => (bool) ($flags->is_serviceable ?? false),
            'is_available' => (bool) ($flags->is_available ?? false),
            'current_license_id' => $current?->id,
            'license_fee_centavos' => PlatformSettings::licenseFeeCentavos(),
            'counts' => [
                'active_stores' => Store::query()->where('province_id', $province->id)->whereIn('status', Store::PUBLISHED_STATUSES)->count(),
                'active_riders' => Rider::query()->where('province_id', $province->id)->where('status', Rider::APPROVED)->count(),
            ],
            'licenses' => array_values($licenses->map(fn (ProvinceLicense $license): array => [
                'id' => $license->id,
                'status' => $license->status,
                'status_reason' => $license->status_reason,
                ...$license->adminChanges(),
                'start_at' => Present::date($license->start_at),
                'end_at' => Present::date($license->end_at),
                'fee_centavos' => $license->fee_centavos,
                'application_id' => $license->application_id,
                'operator' => Present::person($people->get($license->operator_user_id)),
                'payments' => array_values(($payments->get($license->id) ?? collect())->map(fn (Payment $payment): array => [
                    'id' => $payment->id,
                    'provider' => $payment->provider,
                    'reference' => $payment->provider === Payment::PROVIDER_MANUAL ? $payment->intent_id : null,
                    'amount_centavos' => $payment->amount_centavos,
                    'paid_at' => Present::date($payment->updated_at),
                ])->all()),
            ])->all()),
        ];
    }

    /**
     * @return array<string, mixed>
     */
    public function applications(?string $status, int $page): array
    {
        $rows = OperatorApplication::query()
            ->with(['province:id,name', 'applicant'])
            ->when($status !== null, fn (Builder $query) => $query->where('status', $status))
            ->orderByDesc('created_at')->orderBy('id')
            ->paginate(25, page: $page);

        return Present::page($rows, fn (OperatorApplication $application): array => [
            'id' => $application->id,
            'status' => $application->status,
            'business_name' => $application->business_name,
            'province' => Present::province($application),
            'applicant' => Present::person($application->applicant),
            'created_at' => Present::date($application->created_at),
        ]);
    }

    /**
     * @return array<string, mixed>
     */
    public function application(OperatorApplication $application): array
    {
        $application->loadMissing(['province:id,name', 'applicant']);
        $license = ProvinceLicense::query()->where('application_id', $application->id)->first();
        $reviewer = $application->reviewed_by === null ? null : User::query()->find($application->reviewed_by);

        return [
            'id' => $application->id,
            'status' => $application->status,
            'business_name' => $application->business_name,
            'registration_number' => $application->registration_number,
            'contact_phone' => $application->contact_phone,
            'contact_email' => $application->contact_email,
            'rejection_reason' => $application->rejection_reason,
            'province' => Present::province($application),
            'applicant' => Present::person($application->applicant),
            'reviewed_by' => Present::person($reviewer),
            'reviewed_at' => Present::date($application->reviewed_at),
            'created_at' => Present::date($application->created_at),
            'license' => $license === null ? null : ['id' => $license->id, 'status' => $license->status],
            'documents' => array_values(array_filter([
                $application->valid_id_path === null ? null : ['type' => 'VALID_ID', 'url' => route('admin.applications.document', [$application, 'valid_id'])],
                $application->business_permit_path === null ? null : ['type' => 'BUSINESS_PERMIT', 'url' => route('admin.applications.document', [$application, 'business_permit'])],
            ])),
        ];
    }

    /**
     * Provinces that have (or had) a license, for list filters.
     *
     * @return list<array{value: string, label: string}>
     */
    public static function provinceOptions(): array
    {
        return array_values(Province::query()
            ->whereIn('id', ProvinceLicense::query()->select('province_id'))
            ->orderBy('name')
            ->get(['id', 'name'])
            ->map(fn (Province $province): array => ['value' => $province->id, 'label' => $province->name])
            ->all());
    }
}
