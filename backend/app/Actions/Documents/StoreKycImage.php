<?php

namespace App\Actions\Documents;

use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use Illuminate\Validation\ValidationException;

/**
 * Saves an uploaded ID/permit/selfie photo on the private `kyc` disk (DECISIONS 60). The image is decoded and
 * re-encoded as JPEG, which drops EXIF data (camera GPS) and anything that is not really an image, and is scaled
 * down to the configured maximum size.
 *
 * Encoding is the slow part: callers encode before opening a database transaction, then [put] inside it.
 */
final class StoreKycImage
{
    /**
     * Encodes and saves in one step (for callers without a transaction around it).
     *
     * @return string the path on the `kyc` disk
     */
    public function __invoke(UploadedFile $file, string $folder, string $field): string
    {
        return $this->put($folder, $field, $this->encode($file, $field));
    }

    /**
     * The photo as a clean JPEG. Photos with absurd dimensions are refused before decoding (they would need hundreds
     * of MB of memory); the apps send at most 1600 px.
     *
     * @return string JPEG bytes
     */
    public function encode(UploadedFile $file, string $field): string
    {
        $size = @getimagesize($file->getRealPath());
        if ($size === false) {
            throw ValidationException::withMessages([$field => 'This file is not a photo we can read. Please upload a JPG or PNG.']);
        }
        $maxSource = (int) config('foxdrop.documents.max_source_pixels');
        if ($size[0] > $maxSource || $size[1] > $maxSource) {
            throw ValidationException::withMessages([$field => "This photo is too large. Please use one under {$maxSource} pixels wide and high."]);
        }

        $image = @imagecreatefromstring((string) file_get_contents($file->getRealPath()));
        if ($image === false) {
            throw ValidationException::withMessages([$field => 'This file is not a photo we can read. Please upload a JPG or PNG.']);
        }

        $image = $this->scaledDown($image, (int) config('foxdrop.documents.max_pixels'));
        ob_start();
        imagejpeg($image, null, 85);
        $jpeg = (string) ob_get_clean();
        imagedestroy($image);

        return $jpeg;
    }

    /**
     * Writes an encoded photo. If the surrounding transaction rolls back, the file is removed again.
     *
     * @return string the path on the `kyc` disk
     */
    public function put(string $folder, string $field, string $jpeg): string
    {
        $path = $folder.'/'.Str::lower($field).'-'.Str::uuid7().'.jpg';
        Storage::disk('kyc')->put($path, $jpeg);
        DB::afterRollBack(fn () => Storage::disk('kyc')->delete($path));

        return $path;
    }

    /**
     * Deletes photos a resubmission replaced, once the new ones are safely committed.
     *
     * @param  list<string>  $paths
     */
    public function deleteAfterCommit(array $paths): void
    {
        if ($paths !== []) {
            DB::afterCommit(fn () => Storage::disk('kyc')->delete($paths));
        }
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
