<?php

namespace App\Queries\Admin;

use App\Models\Province;
use App\Models\User;
use Carbon\CarbonInterface;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;
use Illuminate\Database\Eloquent\Collection;
use Illuminate\Database\Eloquent\Model;

/**
 * Small shapes shared by the admin pages (dates as ISO-8601 UTC; the page shows them in Asia/Manila).
 */
final class Present
{
    public static function date(?CarbonInterface $at): ?string
    {
        return $at?->toIso8601String();
    }

    /**
     * @return array{id: string, full_name: string|null, email: string|null, phone: string|null}|null
     */
    public static function person(?User $user): ?array
    {
        return $user === null ? null : [
            'id' => $user->id,
            'full_name' => $user->full_name,
            'email' => $user->email,
            'phone' => $user->phone,
        ];
    }

    /**
     * The province a record belongs to, with its `province` relation (select `id,name`) loaded.
     *
     * @return array{id: string, name: string|null}
     */
    public static function province(Model $record): array
    {
        /** @var Province|null $province */
        $province = $record->getRelationValue('province');

        return ['id' => (string) $record->getAttribute('province_id'), 'name' => $province?->name];
    }

    /**
     * People by id, for showing several rows' actors or operators with one query.
     *
     * @param  iterable<string|null>  $ids
     * @return Collection<string, User>
     */
    public static function peopleById(iterable $ids): Collection
    {
        return User::query()->whereIn('id', collect($ids)->filter()->unique()->values())->get()->keyBy('id');
    }

    /**
     * A page of rows plus what the table footer needs.
     *
     * @template TItem
     *
     * @param  LengthAwarePaginator<int, TItem>  $page
     * @param  callable(TItem): array<string, mixed>  $row
     * @return array{data: list<array<string, mixed>>, meta: array{page: int, last_page: int, per_page: int, total: int}}
     */
    public static function page(LengthAwarePaginator $page, callable $row): array
    {
        return [
            'data' => array_values(array_map($row, $page->items())),
            'meta' => [
                'page' => $page->currentPage(),
                'last_page' => $page->lastPage(),
                'per_page' => $page->perPage(),
                'total' => $page->total(),
            ],
        ];
    }
}
