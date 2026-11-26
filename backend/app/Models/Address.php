<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Attributes\Scope;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;

/**
 * A customer's saved delivery address. Its province is the one whose active delivery zone holds the pin
 * (DECISIONS 84).
 *
 * @property string $id
 * @property string $user_id
 * @property string $province_id
 * @property string $label
 * @property string $address_line
 * @property string|null $landmark
 * @property string|null $barangay
 * @property string $city
 */
#[Fillable(['user_id', 'province_id', 'label', 'location', 'address_line', 'landmark', 'barangay', 'city'])]
class Address extends Model
{
    use HasUuids;

    /**
     * All columns plus the pin as latitude/longitude.
     *
     * @param  Builder<static>  $query
     */
    #[Scope]
    protected function withLocation(Builder $query): void
    {
        $query->select('addresses.*')
            ->selectRaw('ST_Y(location::geometry) as location_lat, ST_X(location::geometry) as location_lng');
    }

    public function lat(): float
    {
        return (float) $this->getAttribute('location_lat');
    }

    public function lng(): float
    {
        return (float) $this->getAttribute('location_lng');
    }
}
