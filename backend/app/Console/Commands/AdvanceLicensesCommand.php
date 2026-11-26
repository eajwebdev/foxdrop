<?php

namespace App\Console\Commands;

use App\Actions\Licensing\AdvanceLicenseLifecycle;
use Illuminate\Console\Attributes\Description;
use Illuminate\Console\Attributes\Signature;
use Illuminate\Console\Command;

#[Signature('foxdrop:licenses:advance')]
#[Description('Move expired licenses to GRACE and finished grace periods to EXPIRED (§5.1)')]
class AdvanceLicensesCommand extends Command
{
    public function handle(AdvanceLicenseLifecycle $advance): int
    {
        $result = $advance();
        $this->info("{$result['grace']} license(s) entered grace, {$result['expired']} expired.");

        return self::SUCCESS;
    }
}
