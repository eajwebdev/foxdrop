<?php

namespace App\Actions\Documents;

use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use Illuminate\Validation\ValidationException;

/**
 * Saves an uploaded ID/permit/selfie photo on the private `kyc` disk (DECISIONS 60). The image is decoded and
 * re-encoded as JPEG, which drops EXIF data (camera GPS) and anything that is not really an image, and is scaled
 * down to the configured maximum size.
 */
final class StoreKycImage
{
    /**
     * @return string the path on the `kyc` disk
     */
    public function __invoke(UploadedFile $file, string $folder, string $field): string
    {
        $image = @imagecreatefromstring((string) file_get_contents($file->getRealPath()));
        if ($image === false) {
            throw ValidationException::withMessages([$field => 'This file is not a photo we can read. Please upload a JPG or PNG.']);
        }

        $image = $this->scaledDown($image, (int) config('foxdrop.documents.max_pixels'));
        ob_start();
        imagejpeg($image, null, 85);
        $jpeg = (string) ob_get_clean();
        imagedestroy($image);

        $path = $folder.'/'.Str::lower($field).'-'.Str::uuid7().'.jpg';
        Storage::disk('kyc')->put($path, $jpeg);

        return $path;
    }

    private function scaledDown(\GdImage $image, int $maxPixels): \GdImage
    {
        $width = imagesx($image);
        $height = imagesy($image);
        $scale = $maxPixels / max($width, $height);
        if ($scale >= 1) {
            return $image;
        }

        $scaled = imagescale($image, max(1, (int) round($width * $scale)), max(1, (int) round($height * $scale)));
        if ($scaled === false) {
            return $image;
        }
        imagedestroy($image);

        return $scaled;
    }
}
