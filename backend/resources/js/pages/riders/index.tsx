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
import { Bike } from 'lucide-react';

type RiderRow = {
    id: string;
    status: string;
    vehicle_type: string;
    plate_no: string | null;
    province: { id: string; name: string | null };
    person: { full_name: string | null; phone: string | null } | null;
    created_at: string | null;
};

const columns: ColumnDef<RiderRow>[] = [
    {
        header: 'Rider',
        id: 'name',
        cell: ({ row }) => (
            <div className="leading-tight">
                <div>{row.original.person?.full_name ?? 'Unnamed'}</div>
                <div className="text-xs font-normal text-muted-foreground">
                    {row.original.person?.phone}
                </div>
            </div>
        ),
    },
    {
        header: 'Vehicle',
        id: 'vehicle',
        cell: ({ row }) =>
            `${humanize(row.original.vehicle_type)}${row.original.plate_no ? ` · ${row.original.plate_no}` : ''}`,
    },
    { header: 'Province', id: 'province', cell: ({ row }) => row.original.province.name },
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

export default function Riders({
    filters,
    statuses,
    provinces,
    riders,
}: {
    filters: { status: string | null; province: string | null; q: string | null };
    statuses: string[];
    provinces: Option[];
    riders?: Paginated<RiderRow>;
}) {
    return (
        <>
            <Head title="Riders" />
            <Page
                title="Riders"
                description="Rider applications and riders in every province. You can step in for an operator."
            >
                <div className="rounded-xl border bg-card">
                    <FilterBar>
                        <SearchFilter value={filters.q} placeholder="Search name, phone or plate" />
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
                    <DeferredBlock data="riders" fallback={<TableSkeleton />}>
                        <DataTable
                            columns={columns}
                            rows={riders?.data ?? []}
                            meta={riders?.meta}
                            rowHref={(row) => admin.riders.show.url(row.id)}
                            empty={
                                <EmptyState
                                    icon={Bike}
                                    title={
                                        filters.status === 'DOCS_REVIEW'
                                            ? 'No rider applications waiting'
                                            : 'No riders match'
                                    }
                                    description="Change the filters to see other riders."
                                />
                            }
                        />
                    </DeferredBlock>
                </div>
            </Page>
        </>
    );
}

Riders.layout = {
    breadcrumbs: [{ title: 'Riders', href: admin.riders.index.url() }],
};
