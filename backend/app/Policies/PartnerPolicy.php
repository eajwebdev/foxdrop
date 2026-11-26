<?php

namespace App\Policies;

use App\Enums\AdminPermission;
use App\Models\ProvinceLicense;
use App\Models\Rider;
use App\Models\Store;
use App\Models\User;
use Illuminate\Auth\Access\Response;

/**
 * Who may review or change a store or rider (§9.4 step 4, §9.5 step 4): a platform admin with the partners permission
 * anywhere (override), or the operator team of its province while the license runs (ACTIVE or GRACE, §5.1). Nobody
 * decides on their own application (DECISIONS 69). Used by the API and the admin web alike.
 */
abstract class PartnerPolicy
{
    protected function decide(User $user, Store|Rider $partner, string $ownerId): Response
    {
        if ($user->isPlatformAdmin()) {
            return $user->hasAdminPermission(AdminPermission::PartnersManage)
                ? Response::allow()
                : Response::deny('You do not have permission to do this.');
        }

        $provinceIds = $user->operatorProvinceIds();
        if ($provinceIds === []) {
            return Response::deny('This needs a provincial operator account.');
        }
        if ($provinceIds[0] !== $partner->province_id) {
            return Response::denyAsNotFound();
        }
        $license = ProvinceLicense::currentByProvince([$partner->province_id])[$partner->province_id] ?? null;
        if ($license === null || ! in_array($license->status, ProvinceLicense::OPERATING, true)) {
            return Response::deny('Your province license is not active. Renew it to make changes.');
        }

        return $ownerId === $user->id ? Response::deny('You cannot review your own application.') : Response::allow();
    }
}
