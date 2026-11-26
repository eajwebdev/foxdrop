<?php

namespace App\Models;

use App\Enums\AppRole;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Carbon;

/**
 * One row of the append-only audit trail (§17, DECISIONS 73), read by the audit log viewer. Rows are written only by
 * App\Actions\Audit\RecordAudit; the database forbids updates and deletes.
 *
 * @property int $id
 * @property string|null $actor_user_id
 * @property AppRole|null $actor_role
 * @property string $action
 * @property string $entity_type
 * @property string|null $entity_id
 * @property string|null $province_id
 * @property array<string, mixed>|null $before
 * @property array<string, mixed>|null $after
 * @property string|null $ip
 * @property string|null $user_agent
 * @property Carbon $created_at
 */
class AuditLog extends Model
{
    public const UPDATED_AT = null;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'actor_role' => AppRole::class,
            'before' => 'array',
            'after' => 'array',
            'created_at' => 'datetime',
        ];
    }
}
