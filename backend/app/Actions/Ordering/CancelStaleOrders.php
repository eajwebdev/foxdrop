<?php

namespace App\Actions\Ordering;

use App\Models\Order;
use App\Models\Payment;
use App\Support\PlatformSettings;
use Symfony\Component\HttpKernel\Exception\HttpException;

/**
 * Every minute (§12): online orders not paid within 15 minutes are cancelled, and so are orders the store has not
 * answered within 5 minutes (refunded if paid online, §5.4). Times come from platform settings.
 */
final class CancelStaleOrders
{
    public function __construct(private readonly OrderTransition $transition) {}

    /**
     * @return array{unpaid: int, unanswered: int}
     */
    public function __invoke(): array
    {
        $unpaid = Order::query()
            ->where('status', Order::PENDING_PAYMENT)
            ->where('created_at', '<', now()->subSeconds(PlatformSettings::unpaidOrderTimeoutSeconds()))
            ->get();
        foreach ($unpaid as $order) {
            Payment::query()->where('order_id', $order->id)->where('status', Payment::PENDING)->update(['status' => Payment::EXPIRED]);
            $order->forceFill(['payment_status' => 'FAILED'])->save();
            $this->cancel($order, 'The online payment was not completed in time.');
        }

        $unanswered = Order::query()
            ->where('status', Order::PLACED)
            ->where('placed_at', '<', now()->subSeconds(PlatformSettings::storeResponseTimeoutSeconds()))
            ->get();
        foreach ($unanswered as $order) {
            $this->cancel($order, 'The store did not answer in time.');
        }

        return ['unpaid' => $unpaid->count(), 'unanswered' => $unanswered->count()];
    }

    private function cancel(Order $order, string $reason): void
    {
        try {
            ($this->transition)($order, Order::CANCELLED, null, OrderTransition::SYSTEM, ['note' => $reason]);
        } catch (HttpException) {
            // Someone moved it in the meantime (the store accepted, the payment arrived): nothing to do.
        }
    }
}
