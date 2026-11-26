<?php

namespace App\Actions\Ordering;

use App\Models\Address;
use App\Models\DeliveryZone;
use App\Models\User;
use Illuminate\Validation\ValidationException;

/**
 * A customer's delivery addresses (§9.1 "addresses"). The province is the one whose delivery zone holds the pin
 * (DECISIONS 84); a pin outside every zone cannot be saved yet.
 */
final class ManageAddresses
{
    /**
     * @param  array{label?: string|null, lat: float, lng: float, address_line: string, landmark?: string|null, barangay?: string|null, city: string}  $fields
     */
    public function save(User $customer, ?Address $address, array $fields): Address
    {
        abort_if($address !== null && $address->user_id !== $customer->id, 404);
        $provinceId = DeliveryZone::provinceAt($fields['lat'], $fields['lng'])
            ?? throw ValidationException::withMessages(['location' => 'FoxDrop is not yet available in your area.']);

        $address ??= new Address(['user_id' => $customer->id]);
        $address->fill([
            'province_id' => $provinceId,
            'label' => $fields['label'] ?? 'Home',
            'location' => sprintf('SRID=4326;POINT(%F %F)', $fields['lng'], $fields['lat']), // EWKT
            'address_line' => $fields['address_line'],
            'landmark' => $fields['landmark'] ?? null,
            'barangay' => $fields['barangay'] ?? null,
            'city' => $fields['city'],
        ])->save();

        return Address::query()->withLocation()->findOrFail($address->id);
    }

    public function delete(User $customer, Address $address): void
    {
        abort_if($address->user_id !== $customer->id, 404);
        $address->delete();
    }
}
