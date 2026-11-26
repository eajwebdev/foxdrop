<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    /**
     * Provinces everywhere; demo data only on a developer machine or in tests.
     */
    public function run(): void
    {
        $this->call(ProvinceSeeder::class);

        if (app()->environment(['local', 'testing'])) {
            $this->call(DemoSeeder::class);
        }
    }
}
