<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Support\Facades\DB;

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
     * Partner applications are only taken where customers can order (an ACTIVE or GRACE license, §5.1); the
     * province_directory view defines "serviceable" once.
     */
    public static function ensureServiceable(string $provinceId): void
    {
        $serviceable = DB::table('province_directory')->where('id', $provinceId)->value('is_serviceable');
        abort_unless($serviceable === true, 409, 'FoxDrop is not open in this province yet.');
    }

    /**
     * An operator can apply for (or be assigned) this province: enabled and without a live license (the
     * province_directory view defines it once; DECISIONS 77).
     */
    public static function isAvailable(string $provinceId): bool
    {
        return DB::table('province_directory')->where('id', $provinceId)->value('is_available') === true;
    }

    /**
     * @return HasMany<ProvinceLicense, $this>
     */
    public function licenses(): HasMany
    {
        return $this->hasMany(ProvinceLicense::class);
    }
}
