<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;

/**
 * @property string $id
 * @property string $store_id
 * @property string $doc_type
 * @property string $storage_path
 * @property string $status
 */
#[Fillable(['store_id', 'province_id', 'doc_type', 'storage_path', 'status'])]
class StoreDocument extends Model
{
    use HasUuids;
}
