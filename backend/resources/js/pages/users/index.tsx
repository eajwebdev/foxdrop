import type { Paginated } from '@/components/admin/data-table';
import { DataTable, TableSkeleton } from '@/components/admin/data-table';
import { DeferredBlock } from '@/components/admin/deferred-block';
import { FilterBar, SearchFilter, SelectFilter } from '@/components/admin/filters';
import { EmptyState, Page } from '@/components/admin/page';
import { StatusBadge } from '@/components/admin/status-badge';
import { formatDate, humanize } from '@/lib/format';
import admin from '@/routes/admin';
import { Head } from '@inertiajs/react';
import type { ColumnDef } from '@tanstack/react-table';
import { UserSearch } from 'lucide-react';

type UserRow = {
    id: string;
    full_name: string | null;
    email: string | null;
    phone: string | null;
    status: string;
    roles: string[];
    created_at: string | null;
};

const columns: ColumnDef<UserRow>[] = [
    {
        header: 'Name',
        id: 'name',
        cell: ({ row }) =>
            row.original.full_name ?? <span className="text-muted-foreground">No name</span>,
    },
    {
        header: 'Contact',
        id: 'contact',
        cell: ({ row }) => (
            <div className="leading-tight">
                <div>{row.original.phone ?? row.original.email}</div>
                {row.original.phone && row.original.email && (
                    <div className="text-xs text-muted-foreground">{row.original.email}</div>
                )}
            </div>
        ),
    },
    {
        header: 'Roles',
        id: 'roles',
        cell: ({ row }) => (
            <div className="flex flex-wrap gap-1">
                {row.original.roles.map((role) => (
                    <span key={role} className="rounded-md bg-muted px-1.5 py-0.5 text-xs">
                        {humanize(role)}
                    </span>
                ))}
            </div>
        ),
    },
    {
        header: 'Status',
        id: 'status',
        cell: ({ row }) => <StatusBadge status={row.original.status} />,
    },
    { header: 'Joined', id: 'created', cell: ({ row }) => formatDate(row.original.created_at) },
];

export default function Users({
    filters,
    roles,
    users,
}: {
    filters: { q: string | null; role: string | null; status: string | null };
    roles: string[];
    users?: Paginated<UserRow>;
}) {
    return (
        <>
            <Head title="Users" />
            <Page
                title="Users"
                description="Every FoxDrop account: customers, riders, store teams, operators and admins."
            >
                <div className="rounded-xl border bg-card">
                    <FilterBar>
                        <SearchFilter
                            value={filters.q}
                            placeholder="Name, email or mobile number"
                        />
                        <SelectFilter
                            name="role"
                            label="Role"
                            value={filters.role}
                            allLabel="All roles"
                            options={roles}
                        />
                        <SelectFilter
                            name="status"
                            label="Status"
                            value={filters.status}
                            allLabel="Any status"
                            options={['ACTIVE', 'SUSPENDED']}
                        />
                    </FilterBar>
                    <DeferredBlock data="users" fallback={<TableSkeleton />}>
                        <DataTable
                            columns={columns}
                            rows={users?.data ?? []}
                            meta={users?.meta}
                            rowHref={(row) => admin.users.show.url(row.id)}
                            empty={
                                <EmptyState
                                    icon={UserSearch}
                                    title="No accounts match"
                                    description="Try another name, number or filter."
                                />
                            }
                        />
                    </DeferredBlock>
                </div>
            </Page>
        </>
    );
}

Users.layout = {
    breadcrumbs: [{ title: 'Users', href: admin.users.index.url() }],
};
