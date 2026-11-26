<?php

namespace App\Policies;

use App\Models\Store;
use App\Models\User;
use Illuminate\Auth\Access\Response;

class StorePolicy extends PartnerPolicy
{
    /**
     * Approve, reject, suspend, reinstate or close.
     */
    public function review(User $user, Store $store): Response
    {
        return $this->decide($user, $store, $store->owner_user_id);
    }

    /**
     * The store's own team runs its menu, settings and orders (§9.3) while the store is live (ACTIVE or PAUSED) or
     * suspended (to keep the menu ready); never once it is closed.
     */
    public function manage(User $user, Store $store): Response
    {
        if (! in_array($store->id, $user->memberStoreIds(), true)) {
            return Response::denyAsNotFound();
        }

        return in_array($store->status, [Store::ACTIVE, Store::PAUSED, Store::SUSPENDED], true)
            ? Response::allow()
            : Response::deny('This store is not open on FoxDrop.');
    }
}
