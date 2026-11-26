<?php

namespace App\Actions\Licensing;

use App\Actions\Documents\StoreKycImage;
use App\Models\OperatorApplication;
use App\Models\ProvinceLicense;
use App\Models\User;
use Illuminate\Database\UniqueConstraintViolationException;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;

/**
 * Applies for a province (§9.4 step 1). The license is created as APPLIED in the same transaction, so the
 * one-live-license index (§5.1) makes a second application for the same province impossible.
 */
final class SubmitOperatorApplication
{
    public function __construct(private readonly StoreKycImage $storeImage) {}

    /**
     * @param  array{province_id: string, business_name: string, registration_number?: string|null, contact_phone: string, contact_email?: string|null}  $details
     */
    public function __invoke(User $applicant, array $details, UploadedFile $validId, ?UploadedFile $businessPermit): OperatorApplication
    {
        $province = DB::table('province_directory')->where('id', $details['province_id'])->first(['is_available']);
        abort_if($province === null || ! $province->is_available, 409, 'This province already has an operator.');
        abort_if($this->hasOpenApplicationOrLicense($applicant), 409, 'You already have an open application or a license.');

        $folder = 'operator-applications/'.$applicant->id;
        $paths = [
            'valid_id_path' => ($this->storeImage)($validId, $folder, 'valid_id'),
            'business_permit_path' => $businessPermit === null ? null : ($this->storeImage)($businessPermit, $folder, 'business_permit'),
        ];

        try {
            return DB::transaction(function () use ($applicant, $details, $paths): OperatorApplication {
                $application = OperatorApplication::query()->create([
                    'applicant_user_id' => $applicant->id,
                    'province_id' => $details['province_id'],
                    'business_name' => $details['business_name'],
                    'registration_number' => $details['registration_number'] ?? null,
                    'contact_phone' => $details['contact_phone'],
                    'contact_email' => $details['contact_email'] ?? null,
                    'status' => OperatorApplication::SUBMITTED,
                ] + $paths);

                ProvinceLicense::query()->create([
                    'province_id' => $details['province_id'],
                    'operator_user_id' => $applicant->id,
                    'application_id' => $application->id,
                    'status' => ProvinceLicense::APPLIED,
                    'fee_centavos' => (int) DB::table('platform_settings')->value('license_fee_centavos'),
                ]);

                return $application;
            });
        } catch (UniqueConstraintViolationException) {
            Storage::disk('kyc')->delete(array_filter($paths));
            abort(409, 'This province already has an operator.');
        }
    }

    private function hasOpenApplicationOrLicense(User $applicant): bool
    {
        return OperatorApplication::query()
            ->where('applicant_user_id', $applicant->id)
            ->where('status', OperatorApplication::SUBMITTED)
            ->exists()
            || ProvinceLicense::query()
                ->where('operator_user_id', $applicant->id)
                ->whereIn('status', [ProvinceLicense::APPLIED, ...ProvinceLicense::PAYABLE])
                ->exists();
    }
}
