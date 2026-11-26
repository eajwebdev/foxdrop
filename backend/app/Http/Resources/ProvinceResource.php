<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * A row of the public province directory: "FoxDrop is coming soon to your province" when not serviceable (§9.1).
 *
 * @property string $id
 * @property string $psgc_code
 * @property string $name
 * @property string $region_name
 * @property bool $is_serviceable
 */
class ProvinceResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'psgc_code' => $this->psgc_code,
            'name' => $this->name,
            'region_name' => $this->region_name,
            'is_serviceable' => (bool) $this->is_serviceable,
        ];
    }
}
