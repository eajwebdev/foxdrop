import { DeferredBlock } from '@/components/admin/deferred-block';
import { Page, Section } from '@/components/admin/page';
import { Skeleton } from '@/components/ui/skeleton';
import { usePollWhileActive } from '@/hooks/use-poll-while-active';
import { daysUntil, formatDate, formatNumber } from '@/lib/format';
import { useCan } from '@/lib/permissions';
import { cn } from '@/lib/utils';
import admin from '@/routes/admin';
import { Head, Link } from '@inertiajs/react';
import type { LucideIcon } from 'lucide-react';
import {
    Bike,
    CalendarClock,
    FileCheck2,
    Hourglass,
    Map,
    Store,
    Users,
    Wallet,
} from 'lucide-react';

type Summary = {
    attention: {
        operator_applications: number;
        stores: number;
        riders: number;
        awaiting_payment: number;
        in_grace: number;
    };
    totals: {
        active_provinces: number;
        active_stores: number;
        active_riders: number;
        users: number;
    };
    expiring: {
        id: string;
        province: { id: string; name: string | null };
        end_at: string | null;
    }[];
};

export default function Dashboard({ summary }: { summary?: Summary }) {
    // §20.4: the dashboard refreshes itself every 15 s while someone is using it.
    usePollWhileActive(15_000, ['summary']);

    return (
        <>
            <Head title="Dashboard" />
            <Page title="Dashboard" description="What needs you now, and the platform at a glance.">
                <DeferredBlock data="summary" fallback={<DashboardSkeleton />}>
                    {summary ? <DashboardBody summary={summary} /> : <DashboardSkeleton />}
                </DeferredBlock>
            </Page>
        </>
    );
}

function DashboardBody({ summary }: { summary: Summary }) {
    const can = useCan();
    const { attention, totals } = summary;

    return (
        <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                <Kpi
                    icon={Map}
                    label="Active provinces"
                    value={totals.active_provinces}
                    tone="brand"
                />
                <Kpi icon={Store} label="Active stores" value={totals.active_stores} tone="navy" />
                <Kpi icon={Bike} label="Active riders" value={totals.active_riders} tone="brand" />
                <Kpi icon={Users} label="Accounts" value={totals.users} tone="navy" />
            </div>

            <div className="grid gap-4 lg:grid-cols-3">
                <Section
                    title="Needs attention"
                    description="Waiting for a decision."
                    className="lg:col-span-2"
                >
                    <ul className="grid gap-3 sm:grid-cols-2">
                        {can('franchises.view') && (
                            <AttentionItem
                                icon={FileCheck2}
                                label="Operator applications"
                                count={attention.operator_applications}
                                href={admin.applications.index.url()}
                            />
                        )}
                        {can('franchises.view') && (
                            <AttentionItem
                                icon={Wallet}
                                label="Licenses waiting for payment"
                                count={attention.awaiting_payment}
                                href={admin.franchises.index.url()}
                            />
                        )}
                        {can('partners.view') && (
                            <AttentionItem
                                icon={Store}
                                label="Store applications"
                                count={attention.stores}
                                href={admin.stores.index.url()}
                            />
                        )}
                        {can('partners.view') && (
                            <AttentionItem
                                icon={Bike}
                                label="Rider applications"
                                count={attention.riders}
                                href={admin.riders.index.url()}
                            />
                        )}
                        {can('franchises.view') && (
                            <AttentionItem
                                icon={Hourglass}
                                label="Provinces in grace period"
                                count={attention.in_grace}
                                href={admin.franchises.index.url()}
                            />
                        )}
                    </ul>
                </Section>

                <Section
                    title="Licenses expiring soon"
                    description="Active licenses ending within 30 days."
                >
                    {summary.expiring.length === 0 ? (
                        <p className="flex items-center gap-2 text-sm text-muted-foreground">
                            <CalendarClock className="size-4" /> None in the next 30 days.
                        </p>
                    ) : (
                        <ul className="divide-y">
                            {summary.expiring.map((license) => (
                                <li
                                    key={license.id}
                                    className="flex items-center justify-between gap-2 py-2.5 text-sm"
                                >
                                    <Link
                                        href={admin.franchises.show.url(license.province.id)}
                                        className="font-medium hover:underline"
                                    >
                                        {license.province.name}
                                    </Link>
                                    <span className="text-muted-foreground">
                                        {formatDate(license.end_at)} · {daysUntil(license.end_at)}{' '}
                                        days
                                    </span>
                                </li>
                            ))}
                        </ul>
                    )}
                </Section>
            </div>
        </>
    );
}

function Kpi({
    icon: Icon,
    label,
    value,
    tone,
}: {
    icon: LucideIcon;
    label: string;
    value: number;
    tone: 'brand' | 'navy';
}) {
    return (
        <div className="flex items-center gap-4 rounded-xl border bg-card p-5">
            <div
                className={cn(
                    'flex size-12 shrink-0 items-center justify-center rounded-xl text-white',
                    tone === 'brand' ? 'bg-brand' : 'bg-navy dark:bg-secondary',
                )}
            >
                <Icon className="size-6" />
            </div>
            <div>
                <p
                    className="font-heading text-2xl font-semibold tabular-nums"
                    data-test={`kpi-${label}`}
                >
                    {formatNumber(value)}
                </p>
                <p className="text-sm text-muted-foreground">{label}</p>
            </div>
        </div>
    );
}

function AttentionItem({
    icon: Icon,
    label,
    count,
    href,
}: {
    icon: LucideIcon;
    label: string;
    count: number;
    href: string;
}) {
    return (
        <li>
            <Link
                href={href}
                className="flex items-center gap-3 rounded-lg border p-3 transition-colors hover:border-brand/40 hover:bg-accent/40"
            >
                <Icon className="size-5 text-muted-foreground" />
                <span className="flex-1 text-sm">{label}</span>
                <span
                    className={cn(
                        'min-w-8 rounded-full px-2 py-0.5 text-center text-sm font-semibold tabular-nums',
                        count > 0
                            ? 'bg-brand text-brand-foreground'
                            : 'bg-muted text-muted-foreground',
                    )}
                >
                    {count}
                </span>
            </Link>
        </li>
    );
}

function DashboardSkeleton() {
    return (
        <div className="space-y-4" aria-busy="true" aria-label="Loading dashboard">
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                {Array.from({ length: 4 }, (_, index) => (
                    <Skeleton key={index} className="h-24 rounded-xl" />
                ))}
            </div>
            <div className="grid gap-4 lg:grid-cols-3">
                <Skeleton className="h-64 rounded-xl lg:col-span-2" />
                <Skeleton className="h-64 rounded-xl" />
            </div>
        </div>
    );
}

Dashboard.layout = {
    breadcrumbs: [{ title: 'Dashboard', href: admin.dashboard.url() }],
};
