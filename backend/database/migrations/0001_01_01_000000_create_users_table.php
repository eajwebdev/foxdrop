<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

/**
 * Identity for every FoxDrop account (customers, riders, store teams, operators, admins). A person signs in with a
 * phone code, email + password, or Google; roles live in `user_roles` (database/schema/04_identity.sql).
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('users', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('full_name', 120)->nullable();
            $table->string('phone', 13)->nullable()->unique(); // E.164, +63 only (§0)
            $table->timestampTz('phone_verified_at')->nullable();
            $table->string('email', 254)->nullable()->unique(); // stored lower-case
            $table->timestampTz('email_verified_at')->nullable();
            $table->string('password')->nullable();
            $table->string('google_sub', 255)->nullable()->unique();
            $table->text('avatar_url')->nullable();
            $table->uuid('default_address_id')->nullable(); // FK added in 04_identity.sql
            $table->string('status', 12)->default('ACTIVE');
            $table->rememberToken();
            $table->timestampsTz();
        });
        DB::statement('alter table users alter column id set default gen_random_uuid()');
        DB::statement("alter table users add constraint users_phone_format check (phone ~ '^\\+63\\d{10}$')");
        DB::statement('alter table users add constraint users_email_lower check (email = lower(email))');
        DB::statement('alter table users add constraint users_has_login check (num_nonnulls(phone, email, google_sub) >= 1)');
        DB::statement("alter table users add constraint users_status check (status in ('ACTIVE', 'SUSPENDED'))");

        Schema::create('password_reset_tokens', function (Blueprint $table) {
            $table->string('email')->primary();
            $table->string('token');
            $table->timestampTz('created_at')->nullable();
        });

        Schema::create('sessions', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->foreignUuid('user_id')->nullable()->index();
            $table->string('ip_address', 45)->nullable();
            $table->text('user_agent')->nullable();
            $table->longText('payload');
            $table->integer('last_activity')->index();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('sessions');
        Schema::dropIfExists('password_reset_tokens');
        Schema::dropIfExists('users');
    }
};
