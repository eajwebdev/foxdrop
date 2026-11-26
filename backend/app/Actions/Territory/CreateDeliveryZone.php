<?php

namespace App\Actions\Territory;

use App\Actions\Audit\RecordAudit;
use App\Models\DeliveryZone;
use App\Support\OperatorContext;
use Illuminate\Support\Facades\DB;

/**
 * Adds a coverage zone (§6, DECISIONS 58): a circle (centre + radius, kept for editing) or a polygon. Both are
 * stored as a PostGIS multipolygon in `area`, which coverage checks use.
 */
final class CreateDeliveryZone
{
    public function __construct(private readonly RecordAudit $audit) {}

    /**
     * @param  array{lat: float, lng: float}|null  $center
     * @param  list<array{0: float, 1: float}>|null  $polygon  [[lng, lat], …]
     */
    public function __invoke(OperatorContext $operator, string $name, ?array $center, ?float $radiusKm, ?array $polygon): DeliveryZone
    {
        $operator->ensureCanChange();

        $id = DB::transaction(function () use ($operator, $name, $center, $radiusKm, $polygon): string {
            if ($center !== null && $radiusKm !== null) {
                $radiusMeters = (int) round($radiusKm * 1000);
                $id = DB::selectOne(
                    'insert into delivery_zones (province_id, name, center, radius_m, area, created_by)
                     values (?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?,
                             ST_Multi(ST_Buffer(ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?)::geometry)::geography, ?)
                     returning id',
                    [$operator->provinceId, $name, $center['lng'], $center['lat'], $radiusMeters, $center['lng'], $center['lat'], $radiusMeters, $operator->user->id],
                )->id;
            } else {
                $ring = $polygon ?? [];
                if ($ring[0] !== $ring[count($ring) - 1]) {
                    $ring[] = $ring[0];
                }
                $geoJson = json_encode(['type' => 'Polygon', 'coordinates' => [$ring]], JSON_THROW_ON_ERROR);
                $id = DB::selectOne(
                    'insert into delivery_zones (province_id, name, area, created_by)
                     values (?, ?, ST_Multi(ST_CollectionExtract(ST_MakeValid(ST_SetSRID(ST_GeomFromGeoJSON(?), 4326)), 3))::geography, ?)
                     returning id',
                    [$operator->provinceId, $name, $geoJson, $operator->user->id],
                )->id;
            }

            ($this->audit)($operator->user, 'delivery_zone.created', 'delivery_zone', $id, $operator->provinceId, after: ['name' => $name]);

            return (string) $id;
        });

        return DeliveryZone::query()->findOrFail($id);
    }
}
