<?php

namespace App\Queries\Admin;

use App\Models\Rider;
use App\Models\RiderDocument;
use App\Models\Store;
use App\Models\StoreDocument;
use App\Support\Like;
use Illuminate\Database\Eloquent\Builder;

/**
 * Stores and riders across every province for the admin approvals pages (§9.5 step 4).
 */
final class PartnerQueries
{
    /**
     * @param  array{status?: string|null, province?: string|null, q?: string|null}  $filters
     * @return array<string, mixed>
     */
    public function stores(array $filters, int $page): array
    {
        $rows = Store::query()
            ->with(['province:id,name', 'owner'])
            ->when($filters['status'] ?? null, fn (Builder $query, string $status) => $query->where('status', $status))
            ->when($filters['province'] ?? null, fn (Builder $query, string $province) => $query->where('province_id', $province))
            ->when($filters['q'] ?? null, fn (Builder $query, string $term) => $query->where('name', 'ilike', Like::contains($term)))
            ->orderByDesc('created_at')->orderBy('id')
            ->paginate(25, page: $page);

        return Present::page($rows, fn (Store $store): array => [
            'id' => $store->id,
            'name' => $store->name,
            'category' => $store->category,
            'status' => $store->status,
            'city' => $store->city,
            'province' => Present::province($store),
            'owner' => Present::person($store->owner),
            'created_at' => Present::date($store->created_at),
        ]);
    }

    /**
     * @return array<string, mixed>
     */
    public function store(Store $store): array
    {
        $store = Store::query()->withLocation()->with(['province:id,name', 'owner', 'documents'])->findOrFail($store->id);
        $lat = $store->getAttribute('location_lat');

        return [
            'id' => $store->id,
            'name' => $store->name,
            'category' => $store->category,
            'status' => $store->status,
            'description' => $store->getAttribute('description'),
            'address_line' => $store->address_line,
            'barangay' => $store->getAttribute('barangay'),
            'city' => $store->city,
            'phone' => $store->getAttribute('phone'),
            'location' => $lat === null ? null : ['lat' => (float) $lat, 'lng' => (float) $store->getAttribute('location_lng')],
            'rejection_reason' => $store->rejection_reason,
            'status_reason' => $store->status_reason,
            'status_changes' => $store->statusChanges(),
            'province' => Present::province($store),
            'owner' => Present::person($store->owner),
            'created_at' => Present::date($store->created_at),
            'documents' => array_values($store->documents->map(fn (StoreDocument $document): array => [
                'type' => $document->doc_type,
                'status' => $document->status,
                'url' => route('admin.documents.store', $document),
            ])->all()),
        ];
    }

    /**
     * @param  array{status?: string|null, province?: string|null, q?: string|null}  $filters
     * @return array<string, mixed>
     */
    public function riders(array $filters, int $page): array
    {
        $rows = Rider::query()
            ->with(['province:id,name', 'user'])
            ->when($filters['status'] ?? null, fn (Builder $query, string $status) => $query->where('status', $status))
            ->when($filters['province'] ?? null, fn (Builder $query, string $province) => $query->where('province_id', $province))
            ->when($filters['q'] ?? null, fn (Builder $query, string $term) => $query->where(fn (Builder $match) => $match
                ->where('plate_no', 'ilike', Like::contains($term))
                ->orWhereHas('user', fn (Builder $user) => $user->search($term))))
            ->orderByDesc('created_at')->orderBy('id')
            ->paginate(25, page: $page);

        return Present::page($rows, fn (Rider $rider): array => [
            'id' => $rider->id,
            'status' => $rider->status,
            'vehicle_type' => $rider->vehicle_type,
            'plate_no' => $rider->plate_no,
            'province' => Present::province($rider),
            'person' => Present::person($rider->user),
            'created_at' => Present::date($rider->created_at),
        ]);
    }

    /**
     * @return array<string, mixed>
     */
    public function rider(Rider $rider): array
    {
        $rider->loadMissing(['province:id,name', 'user', 'documents']);

        return [
            'id' => $rider->id,
            'status' => $rider->status,
            'vehicle_type' => $rider->vehicle_type,
            'plate_no' => $rider->plate_no,
            'license_no_last4' => $rider->license_no_last4,
            'license_expiry' => $rider->license_expiry?->toDateString(),
            'rejection_reason' => $rider->rejection_reason,
            'status_reason' => $rider->status_reason,
            'status_changes' => $rider->statusChanges(),
            'province' => Present::province($rider),
            'person' => Present::person($rider->user),
            'created_at' => Present::date($rider->created_at),
            'documents' => array_values($rider->documents->map(fn (RiderDocument $document): array => [
                'type' => $document->doc_type,
                'status' => $document->status,
                'url' => route('admin.documents.rider', $document),
            ])->all()),
        ];
    }
}
