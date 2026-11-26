<?php

namespace App\Policies;

use App\Models\Rider;
use App\Models\User;
use Illuminate\Auth\Access\Response;

class RiderPolicy extends PartnerPolicy
{
    /**
     * Approve, reject, suspend, reinstate or deactivate.
     */
    public function review(User $user, Rider $rider): Response
    {
        return $this->decide($user, $rider, $rider->user_id);
    }
}
