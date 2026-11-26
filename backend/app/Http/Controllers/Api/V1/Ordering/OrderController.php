<?php

namespace App\Http\Controllers\Api\V1\Ordering;

use App\Actions\Ordering\CalculateQuote;
use App\Actions\Ordering\OrderTransition;
use App\Actions\Ordering\PlaceOrder;
use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\Ordering\PlaceOrderRequest;
use App\Http\Requests\Api\V1\Ordering\QuoteRequest;
use App\Http\Resources\OrderResource;
use App\Http\Resources\QuoteResource;
use App\Models\Address;
use App\Models\Order;
use App\Models\Store;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;

/**
 * The customer's side of ordering (§9.1 steps 4–7): price check, placing, history, detail and cancel.
 */
class OrderController extends Controller
{
    public function quote(QuoteRequest $request, CalculateQuote $calculate): JsonResponse
    {
        $customer = $this->user($request);
        $address = $request->filled('address_id')
            ? Address::query()->withLocation()->where('user_id', $customer->id)->find($request->string('address_id')->toString()) ?? abort(422, 'Choose one of your saved addresses.')
            : null;
        $store = Store::query()->find($request->string('store_id')->toString()) ?? abort(404);

        $quote = $calculate($customer, $store, $request->string('fulfillment')->toString(), $request->lines(), $address, $request->input('promo_code'));

        return QuoteResource::make($quote)->response()->setStatusCode(201);
    }

    public function store(PlaceOrderRequest $request, PlaceOrder $place): JsonResponse
    {
        $notes = $request->input('notes');
        $result = $place($this->user($request), $request->string('quote_id')->toString(), $request->string('payment_method')->toString(), is_string($notes) ? $notes : null, $request->idempotencyKey());

        return $this->resource($result['order'])->response()->setStatusCode($result['created'] ? 201 : 200);
    }

    public function index(Request $request): AnonymousResourceCollection
    {
        return OrderResource::collection(
            Order::query()->with(['store:id,name,phone', 'payment'])->where('customer_id', $this->user($request)->id)
                ->orderByDesc('created_at')->limit(50)->get(),
        );
    }

    public function show(Request $request, Order $order): OrderResource
    {
        abort_unless($order->customer_id === $this->user($request)->id, 404);

        return $this->resource($order);
    }

    public function cancel(Request $request, Order $order, OrderTransition $transition): OrderResource
    {
        abort_unless($order->customer_id === $this->user($request)->id, 404);
        $transition($order, Order::CANCELLED, $this->user($request), OrderTransition::CUSTOMER, ['note' => 'Cancelled by the customer.']);

        return $this->resource($order);
    }

    private function resource(Order $order): OrderResource
    {
        return OrderResource::make($order->refresh()->load(['store:id,name,phone', 'items', 'events', 'payment']));
    }

    private function user(Request $request): User
    {
        /** @var User */
        return $request->user();
    }
}
