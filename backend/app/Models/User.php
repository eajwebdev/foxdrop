<?php

namespace App\Models;

use App\Enums\AppRole;
use Closure;
use Database\Factories\UserFactory;
use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Attributes\Hidden;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

/**
 * A FoxDrop account. Roles come from `user_roles`; every new account gets the customer role from a database trigger.
 *
 * @property string $id
 * @property string|null $full_name
 * @property string|null $phone
 * @property string|null $email
 * @property string|null $google_sub
 * @property string $status
 */
#[Fillable(['full_name', 'phone', 'phone_verified_at', 'email', 'email_verified_at', 'password', 'google_sub', 'avatar_url'])]
#[Hidden(['password', 'remember_token', 'google_sub'])]
class User extends Authenticatable
{
    /** @use HasFactory<UserFactory> */
    use HasApiTokens, HasFactory, HasUuids, Notifiable;

    public const string STATUS_ACTIVE = 'ACTIVE';

    public const string STATUS_SUSPENDED = 'SUSPENDED';

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'phone_verified_at' => 'datetime',
            'email_verified_at' => 'datetime',
            'password' => 'hashed',
        ];
    }

    /**
     * @return HasMany<UserRole, $this>
     */
    public function roles(): HasMany
    {
        return $this->hasMany(UserRole::class)->orderBy('created_at')->orderBy('role')->orderBy('id');
    }

    /**
     * @return HasOne<Rider, $this>
     */
    public function rider(): HasOne
    {
        return $this->hasOne(Rider::class);
    }

    public function isSuspended(): bool
    {
        return $this->status === self::STATUS_SUSPENDED;
    }

    public function isPlatformAdmin(): bool
    {
        return $this->roles->contains(fn (UserRole $grant): bool => in_array($grant->role, AppRole::admin(), true));
    }

    /**
     * Provinces this user runs as operator or operator staff.
     *
     * @return list<string>
     */
    public function operatorProvinceIds(): array
    {
        return $this->scopeIds(AppRole::operatorTeam(), fn (UserRole $grant): ?string => $grant->province_id);
    }

    /**
     * Stores this user owns or works for.
     *
     * @return list<string>
     */
    public function memberStoreIds(): array
    {
        return $this->scopeIds(AppRole::storeTeam(), fn (UserRole $grant): ?string => $grant->store_id);
    }

    /**
     * @param  list<AppRole>  $roles
     * @param  Closure(UserRole): ?string  $scopeOf
     * @return list<string>
     */
    private function scopeIds(array $roles, Closure $scopeOf): array
    {
        return array_values($this->roles
            ->filter(fn (UserRole $grant): bool => in_array($grant->role, $roles, true))
            ->map($scopeOf)
            ->filter(fn (?string $id): bool => $id !== null)
            ->unique()
            ->all());
    }
}
