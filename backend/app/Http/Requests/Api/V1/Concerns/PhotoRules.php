<?php

namespace App\Http\Requests\Api\V1\Concerns;

/**
 * Upload rules for ID, permit and selfie photos (DECISIONS 60). The server re-encodes them anyway.
 */
trait PhotoRules
{
    /**
     * @return list<string>
     */
    protected function photo(bool $required = true): array
    {
        return [$required ? 'required' : 'nullable', 'file', 'image', 'mimes:jpg,jpeg,png,webp', 'max:'.config('foxdrop.documents.max_kilobytes')];
    }
}
