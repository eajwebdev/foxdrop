<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Models\OperatorApplication;
use App\Models\RiderDocument;
use App\Models\StoreDocument;
use Illuminate\Support\Facades\Storage;
use Symfony\Component\HttpFoundation\StreamedResponse;

/**
 * KYC photos for the admin document viewer (§20.4 step 4). Behind the admin session and the page's permission
 * instead of a signed link, and never cached by the browser (DECISIONS 68).
 */
class DocumentController extends Controller
{
    public function operatorApplication(OperatorApplication $application, string $type): StreamedResponse
    {
        $path = $type === 'valid_id' ? $application->valid_id_path : $application->business_permit_path;

        return $this->stream($path ?? abort(404));
    }

    public function store(StoreDocument $document): StreamedResponse
    {
        return $this->stream($document->storage_path);
    }

    public function rider(RiderDocument $document): StreamedResponse
    {
        return $this->stream($document->storage_path);
    }

    private function stream(string $path): StreamedResponse
    {
        abort_unless(Storage::disk('kyc')->exists($path), 404);

        return Storage::disk('kyc')->response($path, null, [
            'Content-Type' => 'image/jpeg',
            'Cache-Control' => 'private, no-store',
            'X-Content-Type-Options' => 'nosniff',
        ]);
    }
}
