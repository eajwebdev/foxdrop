<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Concerns\HasUuids;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Carbon;

/**
 * One attempt to pay online (license, top-up or order). Amounts are set by the server (§0 rule 4).
 *
 * @property string $id
 * @property string|null $province_id
 * @property string $purpose
 * @property string $provider
 * @property string|null $intent_id
 * @property string|null $checkout_url
 * @property int $amount_centavos
 * @property string $status
 * @property string|null $payer_user_id
 * @property string|null $license_id
 * @property string|null $order_id
 * @property string|null $provider_payment_id
 * @property Carbon|null $refunded_at
 * @property Carbon|null $expires_at
 * @property Carbon|null $created_at
 * @property Carbon|null $updated_at
 */
#[Fillable(['province_id', 'purpose', 'provider', 'intent_id', 'checkout_url', 'amount_centavos', 'status', 'payer_user_id', 'license_id', 'order_id', 'expires_at', 'raw'])]
class Payment extends Model
{
    use HasUuids;

    public const string PENDING = 'PENDING';

    public const string PAID = 'PAID';

    public const string FAILED = 'FAILED';

    public const string EXPIRED = 'EXPIRED';

    public const string PURPOSE_LICENSE = 'LICENSE';

    public const string PURPOSE_ORDER = 'ORDER';

    public const string REFUNDED = 'REFUNDED';

    /** Recorded by an admin for money received outside the gateway (DECISIONS 76). */
    public const string PROVIDER_MANUAL = 'manual';

    /**
     * The SYSTEM wallet this money came in through: offline receipts keep the gateway clearing account matching
     * PayMongo exactly (DECISIONS 76). Every ledger rule that settles a payment debits this wallet.
     */
    public function clearingWalletKind(): string
    {
        return $this->provider === self::PROVIDER_MANUAL ? 'OFFLINE_RECEIPTS' : 'GATEWAY_CLEARING';
    }

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return ['amount_centavos' => 'integer', 'expires_at' => 'datetime', 'refunded_at' => 'datetime', 'raw' => 'array'];
    }
}
