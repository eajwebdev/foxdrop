<?php

namespace App\Actions\Partners;

use App\Actions\Audit\RecordAudit;
use App\Models\Rider;
use App\Models\Store;
use App\Models\User;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Gate;

/**
 * Suspends, reinstates or ends a live store or rider (§5.2, §5.3, §9.5 step 4, DECISIONS 79). Which change is
 * allowed from which status is the model's `STATUS_CHANGES`. Suspended and closed stores leave the customer app
 * at once (only ACTIVE and PAUSED are published); a rider who is no longer APPROVED goes offline so dispatch never
 * offers them a job. The reason is kept for the partner.
 */
final class ChangePartnerStatus
{
    public function __construct(private readonly RecordAudit $audit) {}

    public function __invoke(User $actor, Store|Rider $partner, string $to, ?string $reason = null): Store|Rider
    {
        Gate::forUser($actor)->authorize('review', $partner);

        return DB::transaction(function () use ($actor, $partner, $to, $reason): Store|Rider {
            $partner = $partner::query()->lockForUpdate()->findOrFail($partner->id);
            abort_unless($partner->canChangeStatusTo($to), 409, "A {$partner->status} ".self::kind($partner)." cannot become {$to}.");
            $before = ['status' => $partner->status];
            $reinstating = $to === $partner::REINSTATED;

            $partner->forceFill([
                'status' => $to,
                'status_reason' => $reinstating ? null : $reason,
                ...($partner instanceof Rider ? ['availability' => 'OFFLINE'] : []),
            ])->save();

            ($this->audit)($actor, self::kind($partner).'.'.($reinstating ? 'reinstated' : strtolower($to)), self::kind($partner), $partner->id,
                $partner->province_id, before: $before, after: ['status' => $to, 'reason' => $reason]);

            return $partner;
        });
    }

    private static function kind(Store|Rider $partner): string
    {
        return $partner instanceof Store ? 'store' : 'rider';
    }
}
