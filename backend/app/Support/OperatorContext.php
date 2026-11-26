<?php

namespace App\Support;

use App\Models\ProvinceLicense;
use App\Models\User;

/**
 * The province an operator-team member manages, and whether they may change things there (§5.1: GRACE keeps
 * operating, EXPIRED is read-only).
 */
final readonly class OperatorContext
{
    private function __construct(
        public User $user,
        public string $provinceId,
    ) {}

    /**
     * The first province the user runs as operator or operator staff; 403 for everyone else.
     */
    public static function for(User $user): self
    {
        $provinceId = $user->operatorProvinceIds()[0] ?? null;
        abort_if($provinceId === null, 403, 'This needs a provincial operator account.');

        return new self($user, $provinceId);
    }

    /**
     * Writes need a license that is ACTIVE or in GRACE for this province.
     */
    public function ensureCanChange(): void
    {
        $operating = ProvinceLicense::query()
            ->where('province_id', $this->provinceId)
            ->whereIn('status', ProvinceLicense::OPERATING)
            ->exists();
        abort_unless($operating, 403, 'Your province license is not active. Renew it to make changes.');
    }
}
