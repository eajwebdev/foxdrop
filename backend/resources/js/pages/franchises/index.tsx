import { DataTable, TableSkeleton } from '@/components/admin/data-table';
import { DeferredBlock } from '@/components/admin/deferred-block';
import { EmptyState, Page } from '@/components/admin/page';
import { StatusBadge } from '@/components/admin/status-badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group';
import { daysUntil, formatDate } from '@/lib/format';
import admin from '@/routes/admin';
import { Head, Link } from '@inertiajs/react';
import type { ColumnDef } from '@tanstack/react-table';
import { FileCheck2, MapPinOff } from 'lucide-react';
import { useMemo, useState } from 'react';

export type ProvinceRow = {
    id: string;
    name: string;
    region_name: string;
    is_enabled: boolean;
    is_serviceable: boolean;
    is_available: boolean;
    license: {
        id: string;
        status: string;
        end_at: string | null;
        operator: { full_name: string | null; email: string | null; phone: string | null } | null;
    } | null;
};

type View = 'all' | 'live' | 'available' | 'attention';

/** Province status as one word: the license status, or Available / Disabled when there is none. */
export function provinceStatus(row: ProvinceRow): string {
    if (!row.is_enabled) {
        return 'DISABLED';
    }
    if (row.is_available) {
        return 'AVAILABLE';
    }

    return row.license?.status ?? 'AVAILABLE';
}

const columns: ColumnDef<ProvinceRow>[] = [
    { header: 'Province', accessorKey: 'name' },
    {
        header: 'Region',
        accessorKey: 'region_name',
        cell: ({ row }) => (
            <span className="text-muted-foreground">{row.original.region_name}</span>
        ),
    },
    {
        header: 'Status',
        id: 'status',
        cell: ({ row }) => <StatusBadge status={provinceStatus(row.original)} />,
    },
    {
        header: 'Operator',
        id: 'operator',
        cell: ({ row }) =>
            row.original.license?.operator?.full_name ?? (
                <span className="text-muted-foreground">—</span>
            ),
    },
    {
        header: 'License ends',
        id: 'end',
        cell: ({ row }) => {
            const end = row.original.license?.end_at;
            const days = daysUntil(end);

            return end ? (
                <span
                    className={days !== null && days <= 30 ? 'font-medium text-warning' : undefined}
                >
                    {formatDate(end)}
                </span>
            ) : (
                <span className="text-muted-foreground">—</span>
            );
        },
    },
];

export default function Franchises({ provinces }: { provinces?: ProvinceRow[] }) {
    return (
        <>
            <Head title="Franchises" />
            <Page
                title="Franchises"
                description="Every province, its license and operator (₱15,000 a year, one operator per province)."
                actions={
                    <Button asChild variant="outline">
                        <Link href={admin.applications.index()}>
                            <FileCheck2 /> Operator applications
                        </Link>
                    </Button>
                }
            >
                <div className="rounded-xl border bg-card">
                    <DeferredBlock data="provinces" fallback={<TableSkeleton rows={10} />}>
                        <ProvinceTable provinces={provinces ?? []} />
                    </DeferredBlock>
                </div>
            </Page>
        </>
    );
}

function ProvinceTable({ provinces }: { provinces: ProvinceRow[] }) {
    const [view, setView] = useState<View>('all');
    const [query, setQuery] = useState('');

    const counts = useMemo(
        () => ({
            all: provinces.length,
            live: provinces.filter((p) => p.is_serviceable).length,
            available: provinces.filter((p) => p.is_available).length,
            attention: provinces.filter((p) => needsAttention(p)).length,
        }),
        [provinces],
    );
    const rows = provinces.filter((province) => {
        const matchesView =
            view === 'all' ||
            (view === 'live' && province.is_serviceable) ||
            (view === 'available' && province.is_available) ||
            (view === 'attention' && needsAttention(province));
        const term = query.trim().toLowerCase();

        return (
            matchesView &&
            (!term || `${province.name} ${province.region_name}`.toLowerCase().includes(term))
        );
    });

    return (
        <>
            <div className="flex flex-col gap-2 border-b p-3 sm:flex-row sm:items-center">
                <ToggleGroup
                    type="single"
                    value={view}
                    onValueChange={(value) => value && setView(value as View)}
                    variant="outline"
                    className="flex-wrap"
                >
                    <ToggleGroupItem value="all">All ({counts.all})</ToggleGroupItem>
                    <ToggleGroupItem value="live">Live ({counts.live})</ToggleGroupItem>
                    <ToggleGroupItem value="available">
                        Available ({counts.available})
                    </ToggleGroupItem>
                    <ToggleGroupItem value="attention">
                        Needs attention ({counts.attention})
                    </ToggleGroupItem>
                </ToggleGroup>
                <Input
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    placeholder="Search province or region"
                    aria-label="Search province or region"
                    className="sm:ml-auto sm:w-64"
                />
            </div>
            <DataTable
                columns={columns}
                rows={rows}
                rowHref={(row) => admin.franchises.show.url(row.id)}
                empty={
                    <EmptyState
                        icon={MapPinOff}
                        title="No provinces match"
                        description="Try another filter or search."
                    />
                }
            />
        </>
    );
}

function needsAttention(province: ProvinceRow): boolean {
    const status = province.license?.status;
    const days = daysUntil(province.license?.end_at);

    return (
        status === 'APPLIED' ||
        status === 'PENDING_PAYMENT' ||
        status === 'GRACE' ||
        status === 'SUSPENDED' ||
        (status === 'ACTIVE' && days !== null && days <= 30)
    );
}

Franchises.layout = {
    breadcrumbs: [{ title: 'Franchises', href: admin.franchises.index.url() }],
};
