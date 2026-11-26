<?php

namespace App\Console\Commands;

use App\Actions\Ordering\CancelStaleOrders;
use Illuminate\Console\Attributes\Description;
use Illuminate\Console\Attributes\Signature;
use Illuminate\Console\Command;

#[Signature('foxdrop:orders:cancel-stale')]
#[Description('Cancel unpaid online orders (15 min) and orders the store did not answer (5 min)')]
class CancelStaleOrdersCommand extends Command
{
    public function handle(CancelStaleOrders $cancel): int
    {
        $result = $cancel();
        $this->info("Cancelled {$result['unpaid']} unpaid and {$result['unanswered']} unanswered orders.");

        return self::SUCCESS;
    }
}
