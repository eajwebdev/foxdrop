<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\RiderDocument;
use App\Models\StoreDocument;
use Illuminate\Support\Facades\Storage;
use Symfony\Component\HttpFoundation\StreamedResponse;

/**
 * Partner document photos behind 10-minute signed links (the `signed` middleware checks them; DECISIONS 60).
 */
class DocumentController extends Controller
{
    public function store(string $document): StreamedResponse
    {
        return $this->stream(StoreDocument::query()->findOrFail($document)->storage_path);
    }

    public function rider(string $document): StreamedResponse
    {
        return $this->stream(RiderDocument::query()->findOrFail($document)->storage_path);
    }

    private function stream(string $path): StreamedResponse
    {
        return Storage::disk('kyc')->response($path, null, [
            'Content-Type' => 'image/jpeg',
            'Cache-Control' => 'private, max-age=600',
            'X-Content-Type-Options' => 'nosniff',
        ]);
    }
}
