<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;

/**
 * @property string $id
 * @property string $rider_id
 * @property string $doc_type
 * @property string $storage_path
 * @property string $status
 */
#[Fillable(['rider_id', 'province_id', 'doc_type', 'storage_path', 'status'])]
class RiderDocument extends Model
{
    use HasUuids;

    public const null UPDATED_AT = null;
}
