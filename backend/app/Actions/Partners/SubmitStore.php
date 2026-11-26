<?php

namespace App\Actions\Partners;

use App\Actions\Documents\StoreKycImage;
use App\Models\Store;
use App\Models\StoreDocument;
use App\Models\User;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\DB;

/**
 * A store application (§5.2, §9.4 step 4): details, the store's location and its documents, waiting for the
 * province operator. Resubmitting after a rejection updates the rejected store (DECISIONS 59).
 */
final class SubmitStore
{
    public function __construct(private readonly StoreKycImage $storeImage) {}

    /**
     * @param  array{province_id: string, name: string, category: string, description?: string|null, address_line: string, barangay?: string|null, city: string, phone?: string|null, latitude: float, longitude: float}  $details
     * @param  array<string, UploadedFile>  $documents  keyed by document type (BUSINESS_PERMIT, VALID_ID)
     */
    public function __invoke(User $owner, array $details, array $documents): Store
    {
        $serviceable = DB::table('province_directory')->where('id', $details['province_id'])->value('is_serviceable');
        abort_unless((bool) $serviceable, 409, 'FoxDrop is not open in this province yet.');

        $mine = Store::query()->where('owner_user_id', $owner->id);
        abort_if((clone $mine)->where('status', Store::SUBMITTED)->exists(), 409, 'You already have a store under review.');
        $rejected = (clone $mine)->where('status', Store::REJECTED)->latest('updated_at')->first();

        return DB::transaction(function () use ($owner, $details, $documents, $rejected): Store {
            $attributes = [
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
            ];

            if ($rejected !== null) {
                // Documents point at (store, province): drop them before the store may move to another province.
                $replaced = StoreDocument::query()->where('store_id', $rejected->id);
                $this->storeImage->deleteAfterCommit(array_values($replaced->get()->map(fn (StoreDocument $doc): string => $doc->storage_path)->all()));
                $replaced->delete();
                $rejected->forceFill($attributes)->save();
                $store = $rejected;
            } else {
                $store = Store::query()->create($attributes);
            }

            foreach ($documents as $type => $file) {
                StoreDocument::query()->create([
                    'store_id' => $store->id,
                    'province_id' => $store->province_id,
                    'doc_type' => $type,
                    'storage_path' => ($this->storeImage)($file, 'stores/'.$store->id, strtolower($type)),
                ]);
            }

            return $store->refresh();
        });
    }
}
