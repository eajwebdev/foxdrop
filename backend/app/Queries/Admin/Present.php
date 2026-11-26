<?php

namespace App\Queries\Admin;

use App\Models\User;
use Carbon\CarbonInterface;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;

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
