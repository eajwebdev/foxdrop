<?php

namespace App\Models;

use App\Enums\AdminPermission;
use App\Enums\AppRole;
use App\Support\Like;
use Closure;
use Database\Factories\UserFactory;
use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Attributes\Hidden;
use Illuminate\Database\Eloquent\Attributes\Scope;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;
use Laravel\Fortify\Actions\DisableTwoFactorAuthentication;
use Laravel\Fortify\TwoFactorAuthenticatable;
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
 * @property string|null $two_factor_secret
 * @property string|null $two_factor_recovery_codes
 * @property Carbon|null $two_factor_confirmed_at
 * @property Carbon|null $created_at
 */
#[Fillable(['full_name', 'phone', 'phone_verified_at', 'email', 'email_verified_at', 'password', 'google_sub', 'avatar_url'])]
#[Hidden(['password', 'remember_token', 'google_sub', 'two_factor_secret', 'two_factor_recovery_codes'])]
class User extends Authenticatable
{
    /** @use HasFactory<UserFactory> */
    use HasApiTokens, HasFactory, HasUuids, Notifiable, TwoFactorAuthenticatable;

    public const string STATUS_ACTIVE = 'ACTIVE';

    public const string STATUS_SUSPENDED = 'SUSPENDED';

    /** @var list<string>|null */
    private ?array $currentOperatorProvinceIds = null;

    /** @var list<string>|null */
    private ?array $grantedAdminPermissions = null;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'phone_verified_at' => 'datetime',
            'email_verified_at' => 'datetime',
            'password' => 'hashed',
            'two_factor_confirmed_at' => 'datetime',
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
     * The account with this email, or this Philippine mobile number in any common spelling (0917…, 917…, +63917…).
     *
     * @param  Builder<self>  $query
     */
    #[Scope]
    protected function whereAccount(Builder $query, string $emailOrPhone): void
    {
        if (str_contains($emailOrPhone, '@')) {
            $query->where('email', mb_strtolower(trim($emailOrPhone)));

            return;
        }
        $query->where('phone', self::normalizePhone($emailOrPhone) ?? '-');
    }

    /**
     * Admin user search (§9.5 step 3): name, email or phone, case-insensitive.
     *
     * @param  Builder<self>  $query
     */
    #[Scope]
    protected function search(Builder $query, string $term): void
    {
        $like = Like::contains($term);
        $phone = self::normalizePhone($term);
        $query->where(fn (Builder $match) => $match
            ->where('full_name', 'ilike', $like)
            ->orWhere('email', 'ilike', $like)
            ->when($phone !== null, fn (Builder $byPhone) => $byPhone->orWhere('phone', $phone)));
    }

    /**
     * "0917 123 4567", "9171234567", "639171234567" or "+63 917 123 4567" → "+639171234567"; null if not a PH mobile.
     */
    public static function normalizePhone(string $value): ?string
    {
        $digits = preg_replace('/\D/', '', $value) ?? '';
        $national = match (true) {
            strlen($digits) === 12 && str_starts_with($digits, '63') => substr($digits, 2),
            strlen($digits) === 11 && str_starts_with($digits, '0') => substr($digits, 1),
            strlen($digits) === 10 => $digits,
            default => null,
        };

        return $national !== null && str_starts_with($national, '9') ? '+63'.$national : null;
    }

    /**
     * Admin accounts use only the admin web, where 2FA is required (DECISIONS 74, 82): the mobile API gives them no
     * token, and refuses any token they already hold.
     */
    public function ensureNotAdminOnApi(): void
    {
        abort_if($this->isPlatformAdmin(), 403, 'Admin accounts sign in on the FoxDrop Admin website, not in the apps.');
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

    public function isSuperAdmin(): bool
    {
        return $this->roles->contains(fn (UserRole $grant): bool => $grant->role === AppRole::SuperAdmin);
    }

    /**
     * Whether this account may do [permission] in the admin web (DECISIONS 72): a super admin always may, admin
     * staff when granted it, everyone else never.
     */
    public function hasAdminPermission(AdminPermission $permission): bool
    {
        if (! $this->isPlatformAdmin()) {
            return false;
        }

        return $this->isSuperAdmin() || in_array($permission->value, $this->adminPermissions(), true);
    }

    /**
     * The permissions granted to this admin-staff account (a super admin's are implied, not stored). Memoized.
     *
     * @return list<string>
     */
    public function adminPermissions(): array
    {
        return $this->grantedAdminPermissions ??= array_values(
            DB::table('admin_permissions')->where('user_id', $this->id)->orderBy('permission')->pluck('permission')->all(),
        );
    }

    /**
     * Signs this account out everywhere: every API token and every admin web session (database sessions) except
     * [exceptSessionId]. Password changes are also caught on the next request by the AuthenticateSession middleware.
     *
     * @return int how many sessions ended
     */
    public function endAllSessions(?string $exceptSessionId = null): int
    {
        $this->tokens()->delete(); // and any API token, so no other way in outlives a password or 2FA change

        return DB::table('sessions')
            ->where('user_id', $this->id)
            ->when($exceptSessionId !== null, fn ($query) => $query->where('id', '<>', $exceptSessionId))
            ->delete();
    }

    /**
     * Clears the authenticator (Fortify's own action) so 2FA is set up again at the next sign-in.
     */
    public function resetTwoFactor(): void
    {
        app(DisableTwoFactorAuthentication::class)($this);
    }

    /**
     * May sign in to the admin web: an ACTIVE account with an admin role.
     */
    public function canUseAdminWeb(): bool
    {
        return $this->status === self::STATUS_ACTIVE && $this->isPlatformAdmin();
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
