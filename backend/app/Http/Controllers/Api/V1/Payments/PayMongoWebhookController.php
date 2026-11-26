<?php

namespace App\Http\Controllers\Api\V1\Payments;

use App\Actions\Payments\SettlePayment;
use App\Http\Controllers\Controller;
use App\Models\Payment;
use App\Models\WebhookEvent;
use App\Services\Payments\PayMongoSignature;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Throwable;

/**
 * PayMongo events (§10). The signature proves PayMongo sent it; each event id is processed once, so PayMongo's
 * retries are harmless.
 */
class PayMongoWebhookController extends Controller
{
    public function __invoke(Request $request, PayMongoSignature $signature, SettlePayment $settle): JsonResponse
    {
        $body = $request->getContent();
        abort_unless($signature->isValid($body, (string) $request->header('Paymongo-Signature')), 401, 'Invalid signature.');

        /** @var array<string, mixed> $event */
        $event = json_decode($body, true) ?: [];
        $eventId = data_get($event, 'data.id');
        $type = data_get($event, 'data.attributes.type');
        abort_unless(is_string($eventId) && is_string($type), 400, 'Not a PayMongo event.');

        $inserted = DB::table('webhook_events')->insertOrIgnore([
            'provider' => 'paymongo', 'event_id' => $eventId, 'event_type' => $type, 'payload' => $body,
        ]);
        $record = WebhookEvent::query()->where('provider', 'paymongo')->where('event_id', $eventId)->firstOrFail();
        if ($inserted === 0 && $record->processed_at !== null) {
            return response()->json(['status' => 'duplicate']);
        }

        try {
            $payment = $this->paymentFor($event);
            if ($payment !== null && $type === 'checkout_session.payment.paid') {
                $settle->succeeded($payment, $event);
            } elseif ($payment !== null && in_array($type, ['payment.failed', 'checkout_session.payment.failed'], true)) {
                $settle->failed($payment, $event);
            }
            $record->forceFill(['processed_at' => now(), 'error' => null])->save();
        } catch (Throwable $e) {
            $record->forceFill(['error' => mb_substr($e->getMessage(), 0, 500)])->save();
            throw $e;
        }

        return response()->json(['status' => 'ok']);
    }

    /**
     * Our payment: the checkout session id (intent_id), or the payment id we put in the metadata.
     *
     * @param  array<string, mixed>  $event
     */
    private function paymentFor(array $event): ?Payment
    {
        $resourceId = data_get($event, 'data.attributes.data.id');
        $metadataId = data_get($event, 'data.attributes.data.attributes.metadata.payment_id');

        return Payment::query()
            ->where('provider', 'paymongo')
            ->where(fn ($q) => $q->where('intent_id', is_string($resourceId) ? $resourceId : '')
                ->when(is_string($metadataId) && preg_match('/^[0-9a-f-]{36}$/i', $metadataId) === 1, fn ($q) => $q->orWhere('id', $metadataId)))
            ->first();
    }
}
