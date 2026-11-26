<?php

namespace App\Actions\Partners;

use App\Actions\Documents\ReplaceKycDocuments;
use App\Models\Province;
use App\Models\Rider;
use App\Models\User;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\DB;

/**
 * A rider registration (§5.3, §9.2 step 1, DECISIONS 61): personal details, vehicle, driver's license and photos,
 * all in one submit, straight to document review. Resubmitting after a rejection replaces everything.
 */
final class SubmitRiderApplication
{
    public function __construct(private readonly ReplaceKycDocuments $documents) {}

    /**
     * @param  array{province_id: string, full_name: string, vehicle_type: string, plate_no?: string|null, license_no: string, license_expiry: string}  $details
     * @param  array<string, UploadedFile>  $documents  keyed by rider_document_type
     */
    public function __invoke(User $user, array $details, array $documents): Rider
    {
        Province::ensureServiceable($details['province_id']);

        $existing = Rider::query()->where('user_id', $user->id)->first();
        abort_if($existing !== null && $existing->status !== Rider::REJECTED, 409, 'You already applied as a rider.');

        $licenseNo = strtoupper((string) preg_replace('/[^A-Za-z0-9]/', '', $details['license_no']));
        $photos = $this->documents->encode($documents);

        return DB::transaction(function () use ($user, $details, $photos, $existing, $licenseNo): Rider {
            $user->forceFill(['full_name' => $details['full_name']])->save();

            $rider = $existing ?? new Rider;
            $rider->forceFill([
                'user_id' => $user->id,
                'province_id' => $details['province_id'],
                'vehicle_type' => $details['vehicle_type'],
                'plate_no' => $details['plate_no'] ?? null,
                'license_no_encrypted' => $licenseNo,
                'license_no_last4' => substr($licenseNo, -4),
                'license_expiry' => $details['license_expiry'],
                'status' => Rider::DOCS_REVIEW,
                'rejection_reason' => null,
            ]);

            if ($rider->exists) {
                $this->documents->discard($rider->documents());
            }
            $rider->save();
            $this->documents->attach($rider->documents(), $rider->province_id, 'riders/'.$rider->id, $photos);

            return $rider->refresh();
        });
    }
}
