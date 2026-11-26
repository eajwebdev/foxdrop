<?php

namespace App\Http\Resources;

use App\Models\RiderDocument;
use App\Models\StoreDocument;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;
use Illuminate\Support\Facades\URL;

/**
 * A partner document for a reviewer: a signed link that works for a few minutes without a bearer token
 * (DECISIONS 60).
 *
 * @mixin StoreDocument|RiderDocument
 */
class DocumentResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        $route = $this->resource instanceof StoreDocument ? 'api.v1.documents.store' : 'api.v1.documents.rider';

        return [
            'type' => $this->doc_type,
            'url' => URL::temporarySignedRoute($route, now()->addMinutes((int) config('foxdrop.documents.url_ttl_minutes')), ['document' => $this->id]),
            'status' => $this->status,
        ];
    }
}
