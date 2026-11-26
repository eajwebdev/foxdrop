<?php

namespace App\Http\Resources;

use App\Models\User;
use App\Models\UserRole;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * The signed-in user as the apps see it. Roles drive which home each app shows (§5 role routing).
 *
 * @mixin User
 */
class UserResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'full_name' => $this->full_name,
            'phone' => $this->phone,
            'email' => $this->email,
            'avatar_url' => $this->avatar_url,
            'roles' => $this->whenLoaded('roles', fn () => $this->roles->map(fn (UserRole $grant): array => [
                'role' => $grant->role->value,
                'province_id' => $grant->province_id,
                'store_id' => $grant->store_id,
            ])->values()->all()),
        ];
    }
}
