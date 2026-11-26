<?php

namespace App\Http\Controllers;

use Illuminate\Support\Facades\Storage;
use Symfony\Component\HttpFoundation\StreamedResponse;

/**
 * Public product photos (DECISIONS 92). Each photo has its own new file name, so browsers and the apps may keep it
 * for a year.
 */
class MediaController extends Controller
{
    public function catalog(string $store, string $file): StreamedResponse
    {
        $path = "catalog/{$store}/{$file}";
        abort_unless(Storage::disk('public')->exists($path), 404);

        return Storage::disk('public')->response($path, null, [
            'Content-Type' => 'image/jpeg',
            'Cache-Control' => 'public, max-age=31536000, immutable',
            'X-Content-Type-Options' => 'nosniff',
        ]);
    }
}
