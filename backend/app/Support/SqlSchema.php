<?php

namespace App\Support;

use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\File;
use RuntimeException;

/**
 * Runs a hand-written PostgreSQL schema file from database/schema. The schema keeps business rules in the database
 * (checks, append-only triggers, the zero-sum ledger, one live license per province), which Laravel's schema builder
 * cannot express.
 */
final class SqlSchema
{
    public static function run(string $file): void
    {
        // A schema file holds many statements (functions, triggers); PDO::exec runs them as one script.
        DB::connection()->getPdo()->exec(File::get(database_path('schema/'.$file)));
    }

    /**
     * Schema files are forward-only: a fix is a new migration. `migrate:fresh` rebuilds a development database.
     */
    public static function forwardOnly(string $file): never
    {
        throw new RuntimeException("{$file} is forward-only; write a new migration or run migrate:fresh locally.");
    }
}
