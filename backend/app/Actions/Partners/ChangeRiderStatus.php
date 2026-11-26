<?php

namespace App\Actions\Partners;

use App\Actions\Audit\RecordAudit;
use App\Models\Rider;
use App\Models\User;
use App\Support\OperatorContext;
use Illuminate\Support\Facades\DB;

/**
 * Suspends, reinstates or deactivates an approved rider (§5.3, §9.5 step 4, DECISIONS 79). A rider who stops being
 * APPROVED goes offline at once, so dispatch never offers them a job. The reason is kept for the rider.
 */
final class ChangeRiderStatus
{
    /** @var array<string, list<string>> target status => statuses it may come from */
    private const array FROM = [
        Rider::SUSPENDED => [Rider::APPROVED],
        Rider::APPROVED => [Rider::SUSPENDED],
        Rider::DEACTIVATED => [Rider::APPROVED, Rider::SUSPENDED],
    ];

    public function __construct(private readonly RecordAudit $audit) {}

    public function __invoke(User $actor, Rider $rider, string $to, ?string $reason = null): Rider
    {
        abort_unless(array_key_exists($to, self::FROM), 422, 'Unknown rider status.');
        OperatorContext::ensureCanReview($actor, $rider->province_id);

        return DB::transaction(function () use ($actor, $rider, $to, $reason): Rider {
            $rider = Rider::query()->lockForUpdate()->findOrFail($rider->id);
            abort_unless(in_array($rider->status, self::FROM[$to], true), 409, "A {$rider->status} rider cannot become {$to}.");
            $before = ['status' => $rider->status];

            $rider->forceFill([
                'status' => $to,
                'status_reason' => $to === Rider::APPROVED ? null : $reason,
                'availability' => 'OFFLINE',
            ])->save();

            ($this->audit)($actor, 'rider.'.strtolower($to === Rider::APPROVED ? 'reinstated' : $to), 'rider', $rider->id, $rider->province_id,
                before: $before, after: ['status' => $to, 'reason' => $reason]);

            return $rider;
        });
    }
}
