import type { Paginated } from '@/components/admin/data-table';
import { DataTable, TableSkeleton } from '@/components/admin/data-table';
import { DeferredBlock } from '@/components/admin/deferred-block';
import { FilterBar, SelectFilter } from '@/components/admin/filters';
import { EmptyState, Page } from '@/components/admin/page';
import { StatusBadge } from '@/components/admin/status-badge';
import { formatDateTime } from '@/lib/format';
import admin from '@/routes/admin';
import { Head } from '@inertiajs/react';
import type { ColumnDef } from '@tanstack/react-table';
import { FileCheck2 } from 'lucide-react';

type Application = {
    id: string;
    status: string;
    business_name: string;
    province: { id: string; name: string | null };
    applicant: { full_name: string | null; phone: string | null } | null;
    created_at: string | null;
};

const columns: ColumnDef<Application>[] = [
    { header: 'Business', accessorKey: 'business_name' },
    { header: 'Province', id: 'province', cell: ({ row }) => row.original.province.name },
    {
        header: 'Applicant',
        id: 'applicant',
        cell: ({ row }) => (
            <div className="leading-tight">
                <div>{row.original.applicant?.full_name ?? '—'}</div>
                <div className="text-xs text-muted-foreground">{row.original.applicant?.phone}</div>
            </div>
        ),
    },
    {
        header: 'Status',
        id: 'status',
        cell: ({ row }) => <StatusBadge status={row.original.status} />,
    },
    {
        header: 'Submitted',
        id: 'created',
        cell: ({ row }) => formatDateTime(row.original.created_at),
    },
];

export default function Applications({
    filters,
    applications,
}: {
    filters: { status: string | null };
    applications?: Paginated<Application>;
}) {
    return (
        <>
            <Head title="Operator applications" />
            <Page
                title="Operator applications"
                description="People who want to run a province. Approving lets them pay the license fee."
            >
                <div className="rounded-xl border bg-card">
                    <FilterBar>
                        <SelectFilter
                            name="status"
                            label="Status"
                            value={filters.status}
                            allLabel="All statuses"
                            options={['SUBMITTED', 'APPROVED', 'REJECTED', 'WITHDRAWN']}
                        />
                    </FilterBar>
                    <DeferredBlock data="applications" fallback={<TableSkeleton />}>
                        <DataTable
                            columns={columns}
                            rows={applications?.data ?? []}
                            meta={applications?.meta}
                            rowHref={(row) => admin.applications.show.url(row.id)}
                            empty={
                                <EmptyState
                                    icon={FileCheck2}
                                    title={
                                        filters.status === 'SUBMITTED'
                                            ? 'No applications waiting'
                                            : 'No applications'
                                    }
                                    description="New applications from the FoxDrop Business app appear here."
                                />
                            }
                        />
                    </DeferredBlock>
                </div>
            </Page>
        </>
    );
}

Applications.layout = {
    breadcrumbs: [
        { title: 'Franchises', href: admin.franchises.index.url() },
        { title: 'Operator applications', href: admin.applications.index.url() },
    ],
};
