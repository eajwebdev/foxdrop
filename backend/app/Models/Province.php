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
 * @property int $operator_commission_bps
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
     * The three-letter code in order numbers (FD-NEG-000123). A province without one gets one the first time it takes
     * an order: the first letters of its name, then other letters of it until the code is free.
     */
    public static function shortCode(string $provinceId): string
    {
        $province = self::query()->findOrFail($provinceId);
        if (is_string($province->short_code) && $province->short_code !== '') {
            return $province->short_code;
        }

        $letters = strtoupper(preg_replace('/[^A-Za-z]/', '', $province->name) ?? '');
        $letters = str_pad($letters, 3, 'X');
        foreach (range(2, strlen($letters) - 1) as $third) {
            $code = substr($letters, 0, 2).$letters[$third];
            if (self::query()->where('short_code', $code)->doesntExist()) {
                $province->forceFill(['short_code' => $code])->save();

                return $code;
            }
        }
        foreach (range('A', 'Z') as $third) { // every letter of the name is taken: any free code with its first two
            $code = substr($letters, 0, 2).$third;
            if (self::query()->where('short_code', $code)->doesntExist()) {
                $province->forceFill(['short_code' => $code])->save();

                return $code;
            }
        }

        throw new \RuntimeException("No free order code for {$province->name}.");
    }

    /**
     * @return HasMany<ProvinceLicense, $this>
     */
    public function licenses(): HasMany
    {
        return $this->hasMany(ProvinceLicense::class);
    }
}
