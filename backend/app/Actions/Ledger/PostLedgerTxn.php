<?php

namespace App\Actions\Ledger;

use Illuminate\Support\Facades\DB;
use InvalidArgumentException;

/**
 * Posts one double-entry transaction (§7). Idempotent by key: posting the same key again returns the first
 * transaction and writes nothing. The entries must sum to zero; the database checks it again at commit.
 */
final class PostLedgerTxn
{
    /**
     * @param  list<LedgerLine>  $lines
     */
    public function __invoke(
        string $type,
        string $idempotencyKey,
        ?string $provinceId,
        array $lines,
        ?string $refType = null,
        ?string $refId = null,
        ?string $createdBy = null,
        ?string $memo = null,
    ): string {
        if (count($lines) < 2 || array_sum(array_map(fn (LedgerLine $line): int => $line->amountCentavos, $lines)) !== 0) {
            throw new InvalidArgumentException("Ledger txn {$idempotencyKey} must have entries that sum to zero.");
        }

        return DB::transaction(function () use ($type, $idempotencyKey, $provinceId, $lines, $refType, $refId, $createdBy, $memo): string {
            $existing = DB::table('ledger_txns')->where('idempotency_key', $idempotencyKey)->value('id');
            if ($existing !== null) {
                return (string) $existing;
            }

            $txnId = (string) DB::table('ledger_txns')->insertGetId([
                'type' => $type,
                'idempotency_key' => $idempotencyKey,
                'province_id' => $provinceId,
                'created_by' => $createdBy,
                'memo' => $memo,
            ], 'id');

            DB::table('ledger_entries')->insert(array_map(fn (LedgerLine $line): array => [
                'txn_id' => $txnId,
                'wallet_id' => $line->walletId,
                'amount_centavos' => $line->amountCentavos,
                'type' => $line->type,
                'ref_type' => $refType,
                'ref_id' => $refId,
                'province_id' => $provinceId,
            ], $lines));

            return $txnId;
        });
    }
}
