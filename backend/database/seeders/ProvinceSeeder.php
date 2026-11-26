<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\File;
use RuntimeException;

/**
 * All 82 provinces + NCR from the PSGC list (psgc.cloud mirror of PSA data). Reference data: safe in production.
 */
class ProvinceSeeder extends Seeder
{
    public function run(): void
    {
        $columns = ['psgc_code', 'name', 'region_code', 'region_name'];
        $lines = array_values(array_filter(explode("\n", File::get(database_path('data/psgc_provinces.csv')))));
        if (str_getcsv($lines[0], escape: '') !== $columns) {
            throw new RuntimeException('psgc_provinces.csv must start with the header '.implode(',', $columns));
        }

        $rows = array_map(
            fn (string $line): array => array_combine($columns, array_map(strval(...), str_getcsv($line, escape: ''))),
            array_slice($lines, 1),
        );

        DB::table('provinces')->upsert($rows, ['psgc_code'], ['name', 'region_code', 'region_name']);
    }
}
