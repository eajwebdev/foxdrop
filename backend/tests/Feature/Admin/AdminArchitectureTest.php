<?php

use Illuminate\Support\Facades\File;

/**
 * W1 acceptance (§13, §20.2): admin pages change money and status only through the shared Actions. Admin controllers
 * may read (through app/Queries/Admin) but never write a table themselves.
 */
arch('admin controllers never touch the database directly')
    ->expect('App\Http\Controllers\Admin')
    ->not->toUse(['Illuminate\Support\Facades\DB', 'Illuminate\Database\Query\Builder']);

arch('admin controllers stay thin and extend the shared base')
    ->expect('App\Http\Controllers\Admin')
    ->classes()
    ->toExtend('App\Http\Controllers\Controller');

it('finds no model or query writes in admin controllers', function () {
    $writes = '/->(save|update|delete|forceFill|fill|insert|upsert|increment|decrement|forceDelete|restore|push|touch)\(|::(create|insert|update|upsert|destroy|forceCreate|query\(\)->(update|delete|insert))\(/';
    $offenders = [];

    foreach (File::allFiles(app_path('Http/Controllers/Admin')) as $file) {
        foreach (explode("\n", $file->getContents()) as $number => $line) {
            if (preg_match($writes, $line) === 1) {
                $offenders[] = $file->getRelativePathname().':'.($number + 1).'  '.trim($line);
            }
        }
    }

    expect($offenders)->toBe([]);
});
