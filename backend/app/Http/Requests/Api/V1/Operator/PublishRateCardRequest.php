<?php

namespace App\Http\Requests\Api\V1\Operator;

use Illuminate\Foundation\Http\FormRequest;

class PublishRateCardRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /**
     * @return array<string, array<int, mixed>>
     */
    public function rules(): array
    {
        return [
            'base_fare_centavos' => ['required', 'integer', 'between:0,100000'],
            'base_km' => ['required', 'numeric', 'between:0,20', 'multiple_of:0.1'],
            'rate_per_km_centavos' => ['required', 'integer', 'between:0,100000'],
            'min_fee_centavos' => ['required', 'integer', 'between:0,100000'],
            'max_km' => ['required', 'numeric', 'between:1,50', 'multiple_of:0.1'],
            'rider_share_bps' => ['required', 'integer', 'between:0,10000'],
            'night_surcharge_centavos' => ['sometimes', 'integer', 'between:0,100000'],
            'bad_weather_surcharge_centavos' => ['sometimes', 'integer', 'between:0,100000'],
            'extra_stop_centavos' => ['sometimes', 'integer', 'between:0,100000'],
        ];
    }
}
