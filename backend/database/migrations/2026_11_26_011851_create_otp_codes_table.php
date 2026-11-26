<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

/**
 * One-time sign-in codes sent by SMS. Only a hash of the code is stored; a code expires, allows a few attempts, and
 * works once.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('otp_codes', function (Blueprint $table) {
            $table->uuid('id')->primary();
            $table->string('phone', 13);
            $table->string('code_hash');
            $table->unsignedSmallInteger('attempts')->default(0);
            $table->timestampTz('expires_at');
            $table->timestampTz('consumed_at')->nullable();
            $table->timestampTz('created_at')->useCurrent();
        });
        // Send and verify only look at a phone's unused codes.
        DB::statement('create index otp_codes_live_idx on otp_codes (phone, created_at desc) where consumed_at is null');
    }

    public function down(): void
    {
        Schema::dropIfExists('otp_codes');
    }
};
