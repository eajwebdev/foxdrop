<?php

namespace App\Http\Controllers\Api\V1\Ordering;

use App\Actions\Ordering\ManageAddresses;
use App\Http\Controllers\Controller;
use App\Http\Requests\Api\V1\Ordering\AddressRequest;
use App\Http\Resources\AddressResource;
use App\Models\Address;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;
use Illuminate\Http\Response;

/**
 * The customer's saved delivery addresses (§9.1).
 */
class AddressController extends Controller
{
    public function index(Request $request): AnonymousResourceCollection
    {
        return AddressResource::collection(
            Address::query()->withLocation()->where('user_id', $this->user($request)->id)->orderByDesc('updated_at')->get(),
        );
    }

    public function store(AddressRequest $request, ManageAddresses $addresses): AddressResource
    {
        return AddressResource::make($addresses->save($this->user($request), null, $request->fields()));
    }

    public function update(AddressRequest $request, Address $address, ManageAddresses $addresses): AddressResource
    {
        return AddressResource::make($addresses->save($this->user($request), $address, $request->fields()));
    }

    public function destroy(Request $request, Address $address, ManageAddresses $addresses): Response
    {
        $addresses->delete($this->user($request), $address);

        return response()->noContent();
    }

    private function user(Request $request): User
    {
        /** @var User */
        return $request->user();
    }
}
