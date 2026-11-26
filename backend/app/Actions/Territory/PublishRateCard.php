<?php

namespace App\Actions\Territory;

use App\Actions\Audit\RecordAudit;
use App\Models\DeliveryRateCard;
use App\Support\OperatorContext;

/**
 * Publishes a new version of the province's delivery prices, effective now (§6). Old versions stay: orders keep a
 * snapshot of the card they were priced with.
 */
final class PublishRateCard
{
    public function __construct(private readonly RecordAudit $audit) {}

    /**
     * @param  array<string, int|float>  $terms  the DeliveryRateCard::TERMS fields
     */
    public function __invoke(OperatorContext $operator, array $terms): DeliveryRateCard
    {
        $operator->ensureCanChange();

        $card = DeliveryRateCard::query()->create($terms + [
            'province_id' => $operator->provinceId,
            'effective_from' => now(),
            'created_by' => $operator->user->id,
        ]);
        ($this->audit)($operator->user, 'rate_card.published', 'delivery_rate_card', $card->id, $operator->provinceId, after: $terms);

        return $card;
    }
}
