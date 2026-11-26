<?php

namespace App\Http\Requests\Api\V1\Operator;

use Illuminate\Foundation\Http\FormRequest;

class CreateZoneRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /**
     * A circle (`center` + `radius_km`) or a polygon (`polygon`: [[lng, lat], …]), inside the Philippines.
     *
     * @return array<string, array<int, mixed>>
     */
    public function rules(): array
    {
        return [
            'name' => ['required', 'string', 'min:1', 'max:80'],
            'center' => ['required_without:polygon', 'prohibits:polygon', 'array:lat,lng'],
            'center.lat' => ['required_with:center', 'numeric', 'between:4,22'],
            'center.lng' => ['required_with:center', 'numeric', 'between:116,127'],
            'radius_km' => ['required_with:center', 'numeric', 'between:0.5,30'],
            'polygon' => ['required_without:center', 'array', 'min:3', 'max:500'],
            'polygon.*' => ['array:0,1', 'size:2'],
            'polygon.*.0' => ['required', 'numeric', 'between:116,127'],
            'polygon.*.1' => ['required', 'numeric', 'between:4,22'],
        ];
    }
}
