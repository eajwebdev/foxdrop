import type { Paginated } from '@/components/admin/data-table';
import { DataTable, TableSkeleton } from '@/components/admin/data-table';
import { DeferredBlock } from '@/components/admin/deferred-block';
import { applyFilter, FilterBar, SearchFilter, SelectFilter } from '@/components/admin/filters';
import { EmptyState, Page } from '@/components/admin/page';
import { Input } from '@/components/ui/input';
import { formatDateTime, humanize } from '@/lib/format';
import admin from '@/routes/admin';
import { Head } from '@inertiajs/react';
import type { ColumnDef } from '@tanstack/react-table';
import { ChevronDown, ScrollText } from 'lucide-react';
import { useState } from 'react';

type Entry = {
    id: number;
    action: string;
    entity_type: string;
    entity_id: string | null;
    actor: { full_name: string | null; email: string | null; phone: string | null } | null;
    actor_role: string | null;
    province: { id: string; name: string | null } | null;
    before: Record<string, unknown> | null;
    after: Record<string, unknown> | null;
    ip: string | null;
    user_agent: string | null;
    created_at: string;
};

const columns: ColumnDef<Entry>[] = [
    {
        header: 'When',
        id: 'when',
        cell: ({ row }) => (
            <span className="whitespace-nowrap">{formatDateTime(row.original.created_at)}</span>
        ),
    },
    {
        header: 'Who',
        id: 'who',
        cell: ({ row }) =>
            row.original.actor ? (
                <div className="leading-tight">
                    <div>
                        {row.original.actor.full_name ??
                            row.original.actor.email ??
                            row.original.actor.phone}
                    </div>
                    {row.original.actor_role && (
                        <div className="text-xs text-muted-foreground">
                            {humanize(row.original.actor_role)}
                        </div>
                    )}
                </div>
            ) : (
                <span className="text-muted-foreground">System</span>
            ),
    },
    {
        header: 'What',
        id: 'what',
        cell: ({ row }) => <span className="font-medium">{humanize(row.original.action)}</span>,
    },
    { header: 'Province', id: 'province', cell: ({ row }) => row.original.province?.name ?? '—' },
    { header: 'Details', id: 'details', cell: ({ row }) => <EntryDetails entry={row.original} /> },
];

export default function AuditLog({
    filters,
    entityTypes,
    entries,
}: {
    filters: {
        action: string | null;
        entity: string | null;
        actor: string | null;
        from: string | null;
        to: string | null;
    };
    entityTypes?: string[];
    entries?: Paginated<Entry>;
}) {
    return (
        <>
            <Head title="Audit log" />
            <Page
                title="Audit log"
                description="Every admin and operator change, sign-in and payment decision. Entries can never be edited or deleted."
            >
                <div className="rounded-xl border bg-card">
                    <FilterBar>
                        <SearchFilter
                            name="actor"
                            value={filters.actor}
                            placeholder="Who (name, email, phone)"
                        />
                        <SearchFilter
                            name="action"
                            value={filters.action}
                            placeholder="Action starts with… e.g. license"
                        />
                        <SelectFilter
                            name="entity"
                            label="Entity"
                            value={filters.entity}
                            allLabel="All entities"
                            options={entityTypes ?? []}
                        />
                        <DateFilter name="from" value={filters.from} label="From" />
                        <DateFilter name="to" value={filters.to} label="To" />
                    </FilterBar>
                    <DeferredBlock
                        data={['entries', 'entityTypes']}
                        fallback={<TableSkeleton rows={10} />}
                    >
                        <DataTable
                            columns={columns}
                            rows={entries?.data ?? []}
                            meta={entries?.meta}
                            empty={
                                <EmptyState
                                    icon={ScrollText}
                                    title="No entries match"
                                    description="Widen the dates or clear a filter."
                                />
                            }
                        />
                    </DeferredBlock>
                </div>
            </Page>
        </>
    );
}

function DateFilter({ name, value, label }: { name: string; value: string | null; label: string }) {
    return (
        <label className="flex items-center gap-2 text-sm text-muted-foreground">
            {label}
            <Input
                type="date"
                defaultValue={value ?? ''}
                onChange={(event) => applyFilter(name, event.target.value || null)}
                className="w-40"
            />
        </label>
    );
}

/** Before → after values, and where the change came from. */
function EntryDetails({ entry }: { entry: Entry }) {
    const [open, setOpen] = useState(false);
    const keys = Array.from(
        new Set([...Object.keys(entry.before ?? {}), ...Object.keys(entry.after ?? {})]),
    );

    return (
        <div className="relative z-10 text-sm">
            <button
                type="button"
                onClick={() => setOpen(!open)}
                className="inline-flex items-center gap-1 text-muted-foreground hover:text-foreground"
                aria-expanded={open}
            >
                {keys.length > 0
                    ? `${keys.length} value${keys.length === 1 ? '' : 's'}`
                    : 'Details'}
                <ChevronDown
                    className={`size-4 transition-transform ${open ? 'rotate-180' : ''}`}
                />
            </button>
            {open && (
                <div className="mt-2 space-y-2 rounded-md border bg-muted/40 p-2 text-xs">
                    {keys.length > 0 && (
                        <table className="w-full">
                            <tbody>
                                {keys.map((key) => (
                                    <tr key={key} className="align-top">
                                        <td className="pr-2 font-medium">{humanize(key)}</td>
                                        <td className="pr-2 text-muted-foreground line-through">
                                            {show(entry.before?.[key])}
                                        </td>
                                        <td>{show(entry.after?.[key])}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                    <p className="text-muted-foreground">
                        {humanize(entry.entity_type)} {entry.entity_id} · {entry.ip ?? 'no IP'}
                    </p>
                </div>
            )}
        </div>
    );
}

function show(value: unknown): string {
    if (value === undefined || value === null) {
        return '—';
    }

    return typeof value === 'object' ? JSON.stringify(value) : String(value);
}

AuditLog.layout = {
    breadcrumbs: [{ title: 'Audit log', href: admin.audit.index.url() }],
};
