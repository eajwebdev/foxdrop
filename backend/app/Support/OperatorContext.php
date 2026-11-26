<?php

namespace App\Support;

use App\Enums\AppRole;
use App\Models\ProvinceLicense;
use App\Models\User;
use App\Models\UserRole;

/**
 * The province an operator-team member manages, and whether they may change things there (§5.1: GRACE keeps
 * operating, EXPIRED is read-only).
 *
 * An operator role only counts while the province's latest license is theirs: when a license expires and the province
 * is licensed to someone else, the former operator keeps the role row (history) but no longer reaches the province.
 */
final readonly class OperatorContext
{
    private function __construct(
        public User $user,
        public string $provinceId,
        public ?ProvinceLicense $license,
    ) {}

    /**
     * The first province the user currently runs as operator or operator staff; 403 for everyone else.
     */
    public static function for(User $user): self
    {
        foreach ($user->roles as $grant) {
            if (! in_array($grant->role, AppRole::operatorTeam(), true) || $grant->province_id === null) {
                continue;
            }
            $license = self::latestLicense($grant->province_id);
            if (self::grantStillCounts($grant, $license)) {
                return new self($user, $grant->province_id, $license);
            }
        }
        abort(403, 'This needs a provincial operator account.');
    }

    /**
     * Writes need this province's license to be ACTIVE or in GRACE.
     */
    public function ensureCanChange(): void
    {
        abort_unless(
            $this->license !== null && in_array($this->license->status, ProvinceLicense::OPERATING, true),
            403,
            'Your province license is not active. Renew it to make changes.',
        );
    }

    /**
     * Partner reviews: platform admins may review anywhere (override); an operator team member only in the province
     * they currently run, and only while its license is operating.
     */
    public static function ensureCanReview(User $reviewer, string $provinceId): void
    {
        if ($reviewer->isPlatformAdmin()) {
            return;
        }
        $context = self::for($reviewer);
        abort_unless($context->provinceId === $provinceId, 404);
        $context->ensureCanChange();
    }

    /**
     * The province's current license: the live one if there is one (at most one, by the unique index), else the one
     * that ended last.
     */
    private static function latestLicense(string $provinceId): ?ProvinceLicense
    {
        return ProvinceLicense::query()
            ->where('province_id', $provinceId)
            ->where('status', '<>', 'REJECTED')
            ->orderByRaw("status in ('APPLIED', 'PENDING_PAYMENT', 'ACTIVE', 'GRACE') desc")
            ->orderByDesc('end_at')
            ->orderByDesc('created_at')
            ->first();
    }

    /**
     * The operator must hold the province's latest license. Operator staff (Phase 6) work for whoever holds it, so a
     * staff grant counts while the province has a license at all.
     */
    private static function grantStillCounts(UserRole $grant, ?ProvinceLicense $license): bool
    {
        if ($license === null) {
            return false;
        }

        return $grant->role !== AppRole::Operator || $license->operator_user_id === $grant->user_id;
    }
}
