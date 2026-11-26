<?php

namespace App\Http\Controllers\Api\V1\StoreTeam;

use App\Actions\Catalog\ManageCatalog;
use App\Actions\Ordering\CompletePickupOrder;
use App\Actions\Ordering\OrderTransition;
use App\Http\Controllers\Controller;
use App\Http\Resources\StoreOrderResource;
use App\Models\Order;
use App\Models\Store;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Gate;

/**
 * The store's order board (§9.3): New / Preparing / Ready / Done, the buttons that move an order, and the store's
 * open / pause and order settings. Every step goes through OrderTransition.
 */
class StoreOrderController extends Controller
{
    /** Board columns and the statuses each shows. */
    private const array BOARD = [
        'new' => [Order::PLACED],
        'preparing' => [Order::ACCEPTED, Order::PREPARING],
        'ready' => [Order::READY_FOR_PICKUP, 'RIDER_ASSIGNED', 'RIDER_AT_STORE'],
        'done' => [Order::COMPLETED, Order::CANCELLED, 'PICKED_UP', 'ARRIVING', 'DELIVERED'],
    ];

    public function index(Request $request, Store $store): JsonResponse
    {
        Gate::forUser($this->user($request))->authorize('manage', $store);
        $column = $request->validate(['board' => ['required', 'in:new,preparing,ready,done']])['board'];

        $orders = Order::query()->with(['items', 'customer:id,full_name'])
            ->where('store_id', $store->id)
            ->whereIn('status', self::BOARD[$column])
            ->when($column === 'done', fn ($query) => $query->where('created_at', '>=', now()->subDay()))
            ->orderBy($column === 'done' ? 'updated_at' : 'created_at', $column === 'done' ? 'desc' : 'asc')
            ->limit(100)
            ->get();

        return response()->json([
            'data' => StoreOrderResource::collection($orders)->resolve($request),
            // Cheap counts for the tabs and the alarm (DECISIONS 88).
            'meta' => ['counts' => collect(self::BOARD)->except('done')->map(fn (array $statuses): int => Order::query()
                ->where('store_id', $store->id)->whereIn('status', $statuses)->count())->all()],
        ]);
    }

    public function show(Request $request, Store $store, Order $order): StoreOrderResource
    {
        Gate::forUser($this->user($request))->authorize('manage', $store);

        return StoreOrderResource::make($order->load(['items', 'customer:id,full_name']));
    }

    public function accept(Request $request, Store $store, Order $order, OrderTransition $transition): StoreOrderResource
    {
        $prep = $request->validate(['prep_time_min' => ['nullable', 'integer', 'min:1', 'max:240']])['prep_time_min'] ?? null;

        return $this->move($request, $store, $order, Order::ACCEPTED, $transition, ['prep_time_min' => $prep === null ? null : (int) $prep]);
    }

    public function reject(Request $request, Store $store, Order $order, OrderTransition $transition): StoreOrderResource
    {
        $reason = $request->validate(['reason' => ['required', 'string', 'min:3', 'max:300']])['reason'];

        return $this->move($request, $store, $order, Order::CANCELLED, $transition, ['note' => (string) $reason]);
    }

    public function preparing(Request $request, Store $store, Order $order, OrderTransition $transition): StoreOrderResource
    {
        return $this->move($request, $store, $order, Order::PREPARING, $transition);
    }

    public function ready(Request $request, Store $store, Order $order, OrderTransition $transition): StoreOrderResource
    {
        return $this->move($request, $store, $order, Order::READY_FOR_PICKUP, $transition);
    }

    public function complete(Request $request, Store $store, Order $order, CompletePickupOrder $complete): StoreOrderResource
    {
        Gate::forUser($this->user($request))->authorize('manage', $store);
        $code = $request->validate(['pickup_code' => ['required', 'digits:4']])['pickup_code'];

        return StoreOrderResource::make($complete($order, $this->user($request), (string) $code)->load(['items', 'customer:id,full_name']));
    }

    public function settings(Request $request, Store $store, ManageCatalog $catalog): JsonResponse
    {
        $settings = $request->validate([
            'is_open' => ['sometimes', 'boolean'],
            'prep_time_min' => ['sometimes', 'integer', 'min:1', 'max:240'],
            'accepts_cod' => ['sometimes', 'boolean'],
            'accepts_pickup' => ['sometimes', 'boolean'],
        ]);
        $store = $catalog->updateSettings($this->user($request), $store, $settings);

        return response()->json(['data' => [
            'id' => $store->id,
            'status' => $store->status,
            'is_open' => $store->status === Store::ACTIVE,
            'prep_time_min' => $store->prep_time_min,
            'accepts_cod' => $store->accepts_cod,
            'accepts_pickup' => $store->accepts_pickup,
        ]]);
    }

    /**
     * @param  array{note?: string|null, prep_time_min?: int|null}  $details
     */
    private function move(Request $request, Store $store, Order $order, string $to, OrderTransition $transition, array $details = []): StoreOrderResource
    {
        Gate::forUser($this->user($request))->authorize('manage', $store);

        return StoreOrderResource::make($transition($order, $to, $this->user($request), OrderTransition::STORE, $details)->load(['items', 'customer:id,full_name']));
    }

    private function user(Request $request): User
    {
        /** @var User */
        return $request->user();
    }
}
