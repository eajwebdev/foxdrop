<?php

namespace App\Http\Requests\Api\V1\Operator;

/**
 * The rate card fields of a new version, plus the distances to quote (1–10 values).
 */
class PreviewRateCardRequest extends PublishRateCardRequest
{
    /**
     * @return array<string, array<int, mixed>>
     */
    public function rules(): array
    {
        return parent::rules() + [
            'distances_km' => ['required', 'array', 'min:1', 'max:10'],
            'distances_km.*' => ['required', 'numeric', 'between:0,100'],
        ];
    }
}
