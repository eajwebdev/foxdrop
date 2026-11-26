<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Scope;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Facades\DB;

/**
 * A coverage area of a province (§6). `area` is the polygon used for coverage checks; circle zones also keep their
 * centre and radius so the app can show and edit them.
 *
 * @property string $id
 * @property string $province_id
 * @property string $name
 * @property bool $is_active
 */
class DeliveryZone extends Model
{
    use HasUuids;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return ['is_active' => 'boolean'];
    }

    /**
     * The serviceable province whose active zone holds this point, or null: "Not yet available in your area"
     * (DECISIONS 84). Where zones of two provinces overlap, the one whose zone is smallest wins (the most specific).
     */
    public static function provinceAt(float $lat, float $lng): ?string
    {
        $provinceId = self::query()
            ->where('is_active', true)
            ->whereIn('province_id', DB::table('province_directory')->where('is_serviceable', true)->select('id'))
            ->whereRaw('ST_Covers(area, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography)', [$lng, $lat])
            ->orderByRaw('ST_Area(area)')
            ->value('province_id');

        return is_string($provinceId) ? $provinceId : null;
    }

    /**
     * Whether an active zone of [provinceId] holds the point (a delivery must stay inside the store's province).
     */
    public static function covers(string $provinceId, float $lat, float $lng): bool
    {
        return self::query()
            ->where('province_id', $provinceId)
            ->where('is_active', true)
            ->whereRaw('ST_Covers(area, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography)', [$lng, $lat])
            ->exists();
    }

    /**
     * Adds the circle centre, radius and the area in km² for the API.
     *
     * @param  Builder<static>  $query
     */
    #[Scope]
    protected function withGeometry(Builder $query): void
    {
        $query->select(['id', 'province_id', 'name', 'is_active', 'radius_m', 'created_at'])
            ->selectRaw('ST_Y(center::geometry) as center_lat, ST_X(center::geometry) as center_lng')
            ->selectRaw('round((ST_Area(area) / 1000000)::numeric, 2) as area_km2');
    }
}
