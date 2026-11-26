<?php

namespace App\Actions\Catalog;

use App\Actions\Documents\StoreKycImage;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;

/**
 * A product photo (DECISIONS 92): re-encoded like a KYC photo (drops EXIF/GPS, size capped) but stored on the public
 * disk under catalog/<store>/, because customers who are not signed in see it too.
 */
final class StoreProductImage
{
    public function __construct(private readonly StoreKycImage $images) {}

    /**
     * @return string the path on the `public` disk
     */
    public function __invoke(UploadedFile $file, string $storeId): string
    {
        $path = 'catalog/'.$storeId.'/'.Str::uuid7().'.jpg';
        Storage::disk('public')->put($path, $this->images->encode($file, 'photo'));
        DB::afterRollBack(fn () => Storage::disk('public')->delete($path));

        return $path;
    }

    public function deleteAfterCommit(?string $path): void
    {
        if ($path !== null) {
            DB::afterCommit(fn () => Storage::disk('public')->delete($path));
        }
    }
}
