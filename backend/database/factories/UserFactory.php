<?php

namespace Database\Factories;

use App\Enums\AppRole;
use App\Models\User;
use Illuminate\Database\Eloquent\Factories\Factory;
use Illuminate\Support\Facades\DB;

/**
 * @extends Factory<User>
 */
class UserFactory extends Factory
{
    /**
     * @return array<string, mixed>
     */
    public function definition(): array
    {
        return [
            'full_name' => fake()->name(),
            'phone' => '+639'.fake()->unique()->numerify('#########'),
            'phone_verified_at' => now(),
            'email' => null,
            'password' => null,
        ];
    }

    /**
     * An email + password account.
     */
    public function withEmail(string $password = 'password123'): static
    {
        return $this->state(fn (): array => [
            'phone' => null,
            'phone_verified_at' => null,
            'email' => fake()->unique()->safeEmail(),
            'password' => $password,
        ]);
    }

    /**
     * Two-factor authentication confirmed with [secret] (base32), as an admin who finished the setup.
     */
    public function withTwoFactor(string $secret): static
    {
        return $this->state(fn (): array => [
            'two_factor_secret' => encrypt($secret),
            'two_factor_recovery_codes' => encrypt(json_encode(['recovery-code-1', 'recovery-code-2'])),
            'two_factor_confirmed_at' => now(),
        ]);
    }

    public function suspended(): static
    {
        return $this->state(fn (): array => ['status' => User::STATUS_SUSPENDED]);
    }

    /**
     * Adds a role grant (the customer role comes from the database trigger).
     */
    public function withRole(AppRole $role, ?string $provinceId = null, ?string $storeId = null): static
    {
        return $this->afterCreating(function (User $user) use ($role, $provinceId, $storeId): void {
            DB::table('user_roles')->insert([
                'user_id' => $user->id, 'role' => $role->value, 'province_id' => $provinceId, 'store_id' => $storeId,
            ]);
        });
    }
}
