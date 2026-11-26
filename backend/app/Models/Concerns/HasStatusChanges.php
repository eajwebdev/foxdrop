<?php

namespace App\Models\Concerns;

/**
 * Admin / operator status changes of a live partner (DECISIONS 79), defined once on the model: the using class lists
 * `STATUS_CHANGES` (target status => statuses it may come from) and `REINSTATED` (the status that lifts a
 * suspension). The Action enforces it; the admin web shows only the changes it allows.
 *
 * @property string $status
 */
trait HasStatusChanges
{
    /**
     * The statuses this record may be changed to right now.
     *
     * @return list<string>
     */
    public function statusChanges(): array
    {
        return array_keys(array_filter(static::STATUS_CHANGES, fn (array $from): bool => in_array($this->status, $from, true)));
    }

    public function canChangeStatusTo(string $status): bool
    {
        return in_array($status, $this->statusChanges(), true);
    }
}
