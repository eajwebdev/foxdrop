import type { Paginated } from '@/components/admin/data-table';
import { DataTable, TableSkeleton } from '@/components/admin/data-table';
import { DeferredBlock } from '@/components/admin/deferred-block';
import { FilterBar, SearchFilter, SelectFilter } from '@/components/admin/filters';
import { EmptyState, Page } from '@/components/admin/page';
import { StatusBadge } from '@/components/admin/status-badge';
import { formatDateTime, humanize } from '@/lib/format';
import admin from '@/routes/admin';
import type { Option } from '@/types';
import { Head } from '@inertiajs/react';
import type { ColumnDef } from '@tanstack/react-table';
import { Store } from 'lucide-react';

type StoreRow = {
    id: string;
    name: string;
    category: string;
    status: string;
    city: string;
    province: { id: string; name: string | null };
    owner: { full_name: string | null; phone: string | null } | null;
    created_at: string | null;
};

const columns: ColumnDef<StoreRow>[] = [
    { header: 'Store', accessorKey: 'name' },
    { header: 'Category', id: 'category', cell: ({ row }) => humanize(row.original.category) },
    {
        header: 'Where',
        id: 'where',
        cell: ({ row }) => (
            <div className="leading-tight">
                <div>{row.original.city}</div>
                <div className="text-xs text-muted-foreground">{row.original.province.name}</div>
            </div>
        ),
    },
    { header: 'Owner', id: 'owner', cell: ({ row }) => row.original.owner?.full_name ?? '—' },
    {
        header: 'Status',
        id: 'status',
        cell: ({ row }) => <StatusBadge status={row.original.status} />,
    },
    {
        header: 'Applied',
        id: 'created',
        cell: ({ row }) => formatDateTime(row.original.created_at),
    },
];

export default function Stores({
    filters,
    statuses,
    provinces,
    stores,
}: {
    filters: { status: string | null; province: string | null; q: string | null };
    statuses: string[];
    provinces: Option[];
    stores?: Paginated<StoreRow>;
}) {
    return (
        <>
            <Head title="Stores" />
            <Page
                title="Stores"
                description="Store applications and live stores in every province. You can step in for an operator."
            >
                <div className="rounded-xl border bg-card">
                    <FilterBar>
                        <SearchFilter value={filters.q} placeholder="Search store name" />
                        <SelectFilter
                            name="status"
                            label="Status"
                            value={filters.status}
                            allLabel="All statuses"
                            options={statuses}
                        />
                        <SelectFilter
                            name="province"
                            label="Province"
                            value={filters.province}
                            allLabel="All provinces"
                            options={provinces}
                        />
                    </FilterBar>
                    <DeferredBlock data="stores" fallback={<TableSkeleton />}>
                        <DataTable
                            columns={columns}
                            rows={stores?.data ?? []}
                            meta={stores?.meta}
                            rowHref={(row) => admin.stores.show.url(row.id)}
                            empty={
                                <EmptyState
                                    icon={Store}
                                    title={
                                        filters.status === 'SUBMITTED'
                                            ? 'No store applications waiting'
                                            : 'No stores match'
                                    }
                                    description="Change the filters to see other stores."
                                />
                            }
                        />
                    </DeferredBlock>
                </div>
            </Page>
        </>
    );
}

Stores.layout = {
    breadcrumbs: [{ title: 'Stores', href: admin.stores.index.url() }],
};
