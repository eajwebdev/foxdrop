<?php

use App\Support\SqlSchema;
use Illuminate\Database\Migrations\Migration;

return new class extends Migration
{
    public function up(): void
    {
        SqlSchema::run('03_provinces_settings.sql');
    }

    public function down(): void
    {
        SqlSchema::forwardOnly('03_provinces_settings.sql');
    }
};
