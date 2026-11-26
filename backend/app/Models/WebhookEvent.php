<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Carbon;

/**
 * A received provider event; (provider, event_id) is unique, so each event is processed once (§10).
 *
 * @property string $id
 * @property Carbon|null $processed_at
 */
#[Fillable(['provider', 'event_id', 'event_type', 'payload', 'processed_at', 'error'])]
class WebhookEvent extends Model
{
    use HasUuids;

    public $timestamps = false;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return ['payload' => 'array', 'processed_at' => 'datetime'];
    }
}
