<?php

namespace App\Actions\Ledger;

/**
 * One entry of a ledger transaction: + credit / − debit on a wallet (§7 sign convention).
 */
final readonly class LedgerLine
{
    public function __construct(
        public string $walletId,
        public int $amountCentavos,
        public string $type,
    ) {}
}
