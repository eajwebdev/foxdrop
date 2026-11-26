<?php

use App\Support\SqlSchema;
use Illuminate\Database\Migrations\Migration;

return new class extends Migration
{
    public function up(): void
    {
        SqlSchema::run('11_support_audit.sql');
    }

    public function down(): void
    {
        SqlSchema::forwardOnly('11_support_audit.sql');
    }
};
