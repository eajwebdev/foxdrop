<?php

namespace App\Models;

use App\Enums\AppRole;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Carbon;

/**
 * One status change of an order (§5.4), append-only: who, when, from, to, and a note. The customer's timeline is
 * built from these.
 *
 * @property int $id
 * @property string $order_id
 * @property string|null $actor_user_id
 * @property AppRole|null $actor_role
 * @property string|null $from_status
 * @property string $to_status
 * @property string|null $note
 * @property Carbon $created_at
 */
class OrderEvent extends Model
{
    public const null UPDATED_AT = null;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return ['actor_role' => AppRole::class, 'created_at' => 'datetime'];
    }
}
