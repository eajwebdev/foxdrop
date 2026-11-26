<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

/**
 * A PSGC province (82 + NCR). One licensed operator per province (§5.1).
 *
 * @property string $id
 * @property string $psgc_code
 * @property string $name
 * @property string|null $short_code
 * @property string $region_name
 * @property bool $is_enabled
 */
class Province extends Model
{
    use HasUuids;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return ['is_enabled' => 'boolean'];
    }

    /**
     * @return HasMany<ProvinceLicense, $this>
     */
    public function licenses(): HasMany
    {
        return $this->hasMany(ProvinceLicense::class);
    }
}
