<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Scope;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;

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
