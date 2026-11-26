<?php

namespace App\Models;

use App\Enums\AppRole;
use Closure;
use Database\Factories\UserFactory;
use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Attributes\Hidden;
use Illuminate\Database\Eloquent\Builder;
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

    public const string STATUS_SUSPENDED = 'SUSPENDED';

    /** @var list<string>|null */
    private ?array $currentOperatorProvinceIds = null;

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

    /**
     * Stops a suspended account at sign-in and on every API call (403 with a message the apps show).
     */
    public function ensureActive(): void
    {
        abort_if($this->status === self::STATUS_SUSPENDED, 403, 'This account is suspended. Please contact FoxDrop support.');
    }

    public function isPlatformAdmin(): bool
    {
        return $this->roles->contains(fn (UserRole $grant): bool => in_array($grant->role, AppRole::admin(), true));
    }

    /**
     * Provinces this user currently runs as operator or operator staff (DECISIONS 65): an operator grant counts only
     * while the province's current license is theirs; a staff grant while the province has a license at all. Every
     * operator check (visibility scopes, reviews, OperatorContext) reads this one list. Memoized per instance.
     *
     * @return list<string>
     */
    public function operatorProvinceIds(): array
    {
        return $this->currentOperatorProvinceIds ??= $this->resolveOperatorProvinceIds();
    }

    /**
     * @return list<string>
     */
    private function resolveOperatorProvinceIds(): array
    {
        $grants = [];
        foreach ($this->roles as $grant) {
            if ($grant->province_id !== null && in_array($grant->role, AppRole::operatorTeam(), true)) {
                $grants[$grant->province_id] ??= $grant;
            }
        }
        if ($grants === []) {
            return [];
        }
        $licenses = ProvinceLicense::currentByProvince(array_keys($grants));

        $provinceIds = [];
        foreach ($grants as $provinceId => $grant) {
            $license = $licenses[$provinceId] ?? null;
            if ($license !== null && ($grant->role !== AppRole::Operator || $license->operator_user_id === $grant->user_id)) {
                $provinceIds[$license->province_id] = $license->province_id;
            }
        }

        return array_values($provinceIds);
    }

    /**
     * The id of this user's rider record, as a subquery (empty when they are not a rider).
     *
     * @return Builder<Rider>
     */
    public function riderIdsQuery(): Builder
    {
        return Rider::query()->where('user_id', $this->id)->select('id');
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
