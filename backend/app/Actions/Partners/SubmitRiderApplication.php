<?php

namespace App\Actions\Partners;

use App\Actions\Documents\StoreKycImage;
use App\Models\Rider;
use App\Models\RiderDocument;
use App\Models\User;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\DB;

/**
 * A rider registration (§5.3, §9.2 step 1, DECISIONS 61): personal details, vehicle, driver's license and photos,
 * all in one submit, straight to document review. Resubmitting after a rejection replaces everything.
 */
final class SubmitRiderApplication
{
    public function __construct(private readonly StoreKycImage $storeImage) {}

    /**
     * @param  array{province_id: string, full_name: string, vehicle_type: string, plate_no?: string|null, license_no: string, license_expiry: string}  $details
     * @param  array<string, UploadedFile>  $documents  keyed by rider_document_type
     */
    public function __invoke(User $user, array $details, array $documents): Rider
    {
        $serviceable = DB::table('province_directory')->where('id', $details['province_id'])->value('is_serviceable');
        abort_unless((bool) $serviceable, 409, 'FoxDrop is not open in this province yet.');

        $existing = Rider::query()->where('user_id', $user->id)->first();
        abort_if($existing !== null && $existing->status !== Rider::REJECTED, 409, 'You already applied as a rider.');

        $licenseNo = strtoupper((string) preg_replace('/[^A-Za-z0-9]/', '', $details['license_no']));

        return DB::transaction(function () use ($user, $details, $documents, $existing, $licenseNo): Rider {
            $user->forceFill(['full_name' => $details['full_name']])->save();

            $attributes = [
                'user_id' => $user->id,
                'province_id' => $details['province_id'],
                'vehicle_type' => $details['vehicle_type'],
                'plate_no' => $details['plate_no'] ?? null,
                'license_no_encrypted' => $licenseNo,
                'license_no_last4' => substr($licenseNo, -4),
                'license_expiry' => $details['license_expiry'],
                'status' => Rider::DOCS_REVIEW,
                'rejection_reason' => null,
            ];

            if ($existing !== null) {
                // Documents point at (rider, province): drop them before the rider may move to another province.
                RiderDocument::query()->where('rider_id', $existing->id)->delete();
                $existing->forceFill($attributes)->save();
                $rider = $existing;
            } else {
                $rider = Rider::query()->create($attributes);
            }

            foreach ($documents as $type => $file) {
                RiderDocument::query()->create([
                    'rider_id' => $rider->id,
                    'province_id' => $rider->province_id,
                    'doc_type' => $type,
                    'storage_path' => ($this->storeImage)($file, 'riders/'.$rider->id, strtolower($type)),
                ]);
            }

            return $rider->refresh();
        });
    }
}
