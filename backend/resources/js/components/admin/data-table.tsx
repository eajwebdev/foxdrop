import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table';
import { formatNumber } from '@/lib/format';
import { visitWithQuery } from '@/lib/utils';
import { Link } from '@inertiajs/react';
import type { ColumnDef } from '@tanstack/react-table';
import { flexRender, getCoreRowModel, useReactTable } from '@tanstack/react-table';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import type { ReactNode } from 'react';

export type PageMeta = { page: number; last_page: number; per_page: number; total: number };
export type Paginated<T> = { data: T[]; meta: PageMeta };

/**
 * Server-paginated table (§20.1 TanStack Table): filters and the page live in the URL, so a list can be shared
 * and the back button works. Rows link to their detail page.
 */
export function DataTable<T>({
    columns,
    rows,
    meta,
    rowHref,
    empty,
}: {
    columns: ColumnDef<T>[];
    rows: T[];
    meta?: PageMeta;
    rowHref?: (row: T) => string;
    empty: ReactNode;
}) {
    const table = useReactTable({
        data: rows,
        columns,
        getCoreRowModel: getCoreRowModel(),
        manualPagination: true,
    });

    if (rows.length === 0) {
        return <>{empty}</>;
    }

    return (
        <div>
            <Table>
                <TableHeader className="bg-muted/40">
                    {table.getHeaderGroups().map((group) => (
                        <TableRow key={group.id} className="hover:bg-transparent">
                            {group.headers.map((header) => (
                                <TableHead key={header.id}>
                                    {header.isPlaceholder
                                        ? null
                                        : flexRender(
                                              header.column.columnDef.header,
                                              header.getContext(),
                                          )}
                                </TableHead>
                            ))}
                        </TableRow>
                    ))}
                </TableHeader>
                <TableBody>
                    {table.getRowModel().rows.map((row) => (
                        <TableRow
                            key={row.id}
                            className={rowHref ? 'relative cursor-pointer' : undefined}
                        >
                            {row.getVisibleCells().map((cell, index) => (
                                <TableCell key={cell.id}>
                                    {index === 0 && rowHref ? (
                                        <Link
                                            href={rowHref(row.original)}
                                            className="font-medium after:absolute after:inset-0 focus-visible:outline-none"
                                        >
                                            {flexRender(
                                                cell.column.columnDef.cell,
                                                cell.getContext(),
                                            )}
                                        </Link>
                                    ) : (
                                        flexRender(cell.column.columnDef.cell, cell.getContext())
                                    )}
                                </TableCell>
                            ))}
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
            {meta && <Pagination meta={meta} />}
        </div>
    );
}

function Pagination({ meta }: { meta: PageMeta }) {
    const go = (page: number) => visitWithQuery({ page: String(page) });
    const from = meta.total === 0 ? 0 : (meta.page - 1) * meta.per_page + 1;
    const to = Math.min(meta.page * meta.per_page, meta.total);

    return (
        <div className="flex items-center justify-between gap-3 border-t px-3 py-3 text-sm text-muted-foreground">
            <span>
                {formatNumber(from)}–{formatNumber(to)} of {formatNumber(meta.total)}
            </span>
            <div className="flex gap-1">
                <Button
                    variant="outline"
                    size="sm"
                    disabled={meta.page <= 1}
                    onClick={() => go(meta.page - 1)}
                    aria-label="Previous page"
                >
                    <ChevronLeft />
                </Button>
                <Button
                    variant="outline"
                    size="sm"
                    disabled={meta.page >= meta.last_page}
                    onClick={() => go(meta.page + 1)}
                    aria-label="Next page"
                >
                    <ChevronRight />
                </Button>
            </div>
        </div>
    );
}

/** Skeleton rows while a deferred list loads. */
export function TableSkeleton({ rows = 6 }: { rows?: number }) {
    return (
        <div className="space-y-3 p-3" aria-busy="true" aria-label="Loading">
            {Array.from({ length: rows }, (_, row) => (
                <div key={row} className="flex gap-4">
                    {Array.from({ length: 5 }, (_, column) => (
                        <Skeleton key={column} className="h-5 flex-1" />
                    ))}
                </div>
            ))}
        </div>
    );
}
