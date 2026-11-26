<?php

namespace App\Support;

use App\Models\ProvinceLicense;
use App\Models\User;

/**
 * The province an operator-team member manages, and whether they may change things there (§5.1: GRACE keeps
 * operating, EXPIRED is read-only). Who currently runs a province is decided in one place,
 * User::operatorProvinceIds() (DECISIONS 65).
 */
final readonly class OperatorContext
{
    private function __construct(
        public User $user,
        public string $provinceId,
        public ?ProvinceLicense $license,
    ) {}

    /**
     * The province the user currently runs as operator or operator staff; 403 for everyone else.
     */
    public static function for(User $user): self
    {
        $provinceId = $user->operatorProvinceIds()[0] ?? null;
        abort_if($provinceId === null, 403, 'This needs a provincial operator account.');

        return new self($user, $provinceId, ProvinceLicense::currentByProvince([$provinceId])[$provinceId] ?? null);
    }

    /**
     * Partner review screens: platform admins (any province) or a current operator team member; 403 for others.
     */
    public static function ensureReviewer(User $user): void
    {
        abort_unless($user->isPlatformAdmin() || $user->operatorProvinceIds() !== [], 403, 'This needs a provincial operator account.');
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
}
