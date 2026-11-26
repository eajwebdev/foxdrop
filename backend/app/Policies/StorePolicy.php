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
}
