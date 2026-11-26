<?php

use App\Support\SqlSchema;
use Illuminate\Database\Migrations\Migration;

return new class extends Migration
{
    public function up(): void
    {
        SqlSchema::run('10_money.sql');
    }

    public function down(): void
    {
        SqlSchema::forwardOnly('10_money.sql');
    }
};
