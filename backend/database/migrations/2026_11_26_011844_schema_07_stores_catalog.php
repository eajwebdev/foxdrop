<?php

use App\Support\SqlSchema;
use Illuminate\Database\Migrations\Migration;

return new class extends Migration
{
    public function up(): void
    {
        SqlSchema::run('07_stores_catalog.sql');
    }

    public function down(): void
    {
        SqlSchema::forwardOnly('07_stores_catalog.sql');
    }
};
