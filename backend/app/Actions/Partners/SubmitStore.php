<?php

namespace App\Actions\Partners;

use App\Actions\Documents\ReplaceKycDocuments;
use App\Models\Province;
use App\Models\Store;
use App\Models\User;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\DB;

/**
 * A store application (§5.2, §9.4 step 4): details, the store's location and its documents, waiting for the
 * province operator. Resubmitting after a rejection updates the rejected store (DECISIONS 59).
 */
final class SubmitStore
{
    public function __construct(private readonly ReplaceKycDocuments $documents) {}

    /**
     * @param  array{province_id: string, name: string, category: string, description?: string|null, address_line: string, barangay?: string|null, city: string, phone?: string|null, latitude: float, longitude: float}  $details
     * @param  array<string, UploadedFile>  $documents  keyed by document type (BUSINESS_PERMIT, VALID_ID)
     */
    public function __invoke(User $owner, array $details, array $documents): Store
    {
        Province::ensureServiceable($details['province_id']);

        $mine = Store::query()->where('owner_user_id', $owner->id);
        abort_if((clone $mine)->where('status', Store::SUBMITTED)->exists(), 409, 'You already have a store under review.');
        $rejected = (clone $mine)->where('status', Store::REJECTED)->latest('updated_at')->first();

        $photos = $this->documents->encode($documents);

        return DB::transaction(function () use ($owner, $details, $photos, $rejected): Store {
            $store = $rejected ?? new Store;
            $store->forceFill([
                'province_id' => $details['province_id'],
                'owner_user_id' => $owner->id,
                'name' => $details['name'],
                'category' => $details['category'],
                'description' => $details['description'] ?? null,
                'address_line' => $details['address_line'],
                'barangay' => $details['barangay'] ?? null,
                'city' => $details['city'],
                'phone' => $details['phone'] ?? null,
                'location' => sprintf('SRID=4326;POINT(%F %F)', $details['longitude'], $details['latitude']),
                'status' => Store::SUBMITTED,
                'rejection_reason' => null,
            ]);

            if ($store->exists) {
                $this->documents->discard($store->documents());
            }
            $store->save();
            $this->documents->attach($store->documents(), $store->province_id, 'stores/'.$store->id, $photos);

            return $store->refresh();
        });
    }
}
