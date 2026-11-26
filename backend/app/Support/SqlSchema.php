<?php

namespace App\Support;

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\File;
use RuntimeException;

/**
 * Hand-written PostgreSQL schema files in database/schema. They keep business rules in the database (checks,
 * append-only triggers, the zero-sum ledger, one live license per province), which Laravel's schema builder cannot
 * express. Each file is one migration: `return SqlSchema::migration('01_extensions.sql');`.
 */
final class SqlSchema
{
    /**
     * A forward-only migration that runs [file]: a fix is a new migration; `migrate:fresh` rebuilds a dev database.
     */
    public static function migration(string $file): Migration
    {
        return new class($file) extends Migration
        {
            public function __construct(private readonly string $file) {}

            public function up(): void
            {
                // A schema file holds many statements (functions, triggers); PDO::exec runs them as one script.
                DB::connection()->getPdo()->exec(File::get(database_path('schema/'.$this->file)));
            }

            public function down(): void
            {
                throw new RuntimeException("{$this->file} is forward-only; write a new migration or run migrate:fresh locally.");
            }
        };
    }
}
