<?php

namespace App\Console\Commands;

use App\Actions\Audit\RecordAudit;
use App\Actions\Roles\GrantRole;
use App\Enums\AppRole;
use App\Models\User;
use Illuminate\Console\Attributes\Description;
use Illuminate\Console\Attributes\Signature;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;
use Illuminate\Validation\Rules\Password;

/**
 * The server-side way in to the admin web (DECISIONS 74): creates the first super admin on a fresh server, or resets
 * a super admin's password / authenticator when nobody else can. Runs on the VPS only (SSH), so it is the system.
 */
#[Signature('foxdrop:super-admin {email : The super admin\'s email} {--name= : Full name for a new account} {--reset-2fa : Make them set up 2FA again at the next sign-in}')]
#[Description('Create a super admin, or reset a super admin\'s password or 2FA')]
class SuperAdminCommand extends Command
{
    public function handle(GrantRole $grantRole, RecordAudit $audit): int
    {
        $email = mb_strtolower(trim((string) $this->argument('email')));
        $password = (string) $this->secret('New password (at least 12 characters, upper and lower case and a number)');
        $validator = Validator::make(['email' => $email, 'password' => $password], [
            'email' => ['required', 'email:rfc', 'max:254'],
            'password' => ['required', Password::defaults()],
        ]);
        if ($validator->fails()) {
            foreach ($validator->errors()->all() as $error) {
                $this->error($error);
            }

            return self::FAILURE;
        }

        DB::transaction(function () use ($email, $password, $grantRole, $audit): void {
            $user = User::query()->where('email', $email)->first();
            $created = $user === null;
            $user ??= User::query()->create(['full_name' => (string) ($this->option('name') ?: 'FoxDrop Admin'), 'email' => $email]);
            $user->forceFill(array_filter([
                'password' => $password,
                'status' => User::STATUS_ACTIVE,
                'email_verified_at' => $user->email_verified_at ?? now(),
            ]))->save();
            if ($this->option('reset-2fa')) {
                $user->forceFill(['two_factor_secret' => null, 'two_factor_recovery_codes' => null, 'two_factor_confirmed_at' => null])->save();
            }
            $grantRole($user->id, AppRole::SuperAdmin);
            DB::table('sessions')->where('user_id', $user->id)->delete();

            $audit(null, $created ? 'admin.super_admin_created' : 'admin.super_admin_reset', 'user', $user->id,
                after: ['reset_2fa' => (bool) $this->option('reset-2fa')]);
        });

        $this->info("{$email} can sign in at ".url('/admin').' and must set up 2FA if it is not on yet.');

        return self::SUCCESS;
    }
}
