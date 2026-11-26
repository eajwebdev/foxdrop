import { ActionDialog } from '@/components/admin/action-dialog';
import { DetailList, Page, Section } from '@/components/admin/page';
import { StatusBadge } from '@/components/admin/status-badge';
import { Button } from '@/components/ui/button';
import { formatDate, formatDateTime, humanize } from '@/lib/format';
import { useCan } from '@/lib/permissions';
import admin from '@/routes/admin';
import { Head, Link, setLayoutProps, usePage } from '@inertiajs/react';
import { Ban, CircleCheck } from 'lucide-react';

type UserDetail = {
    id: string;
    full_name: string | null;
    email: string | null;
    phone: string | null;
    status: string;
    created_at: string | null;
    is_admin: boolean;
    two_factor_enabled: boolean;
    roles: {
        role: string;
        province: { id: string; name: string | null } | null;
        store: { id: string; name: string | null } | null;
    }[];
    rider: { id: string; status: string } | null;
    history: { action: string; at: string; after: Record<string, unknown> | null }[];
};

export default function UserShow({ user }: { user: UserDetail }) {
    const can = useCan();
    const me = usePage().props.auth.user;
    const name = user.full_name ?? user.phone ?? user.email ?? 'Account';
    const mayChange =
        can('users.manage') && me?.id !== user.id && (!user.is_admin || me?.is_super_admin);
    setLayoutProps({
        breadcrumbs: [
            { title: 'Users', href: admin.users.index.url() },
            { title: name, href: admin.users.show.url(user.id) },
        ],
    });

    return (
        <>
            <Head title={name} />
            <Page
                title={name}
                description={<StatusBadge status={user.status} />}
                actions={
                    mayChange &&
                    (user.status === 'ACTIVE' ? (
                        <ActionDialog
                            trigger={
                                <Button variant="outline" className="text-destructive">
                                    <Ban /> Suspend account
                                </Button>
                            }
                            title={`Suspend ${name}?`}
                            description="They are signed out of every app and device at once and cannot sign in until reactivated."
                            action={admin.users.suspend.form(user.id)}
                            submitLabel="Suspend account"
                            destructive
                            reason="Reason (kept in the audit log)"
                            confirmIdentity
                        />
                    ) : (
                        <ActionDialog
                            trigger={
                                <Button variant="outline">
                                    <CircleCheck /> Reactivate
                                </Button>
                            }
                            title={`Reactivate ${name}?`}
                            description="They can sign in again."
                            action={admin.users.reactivate.form(user.id)}
                            submitLabel="Reactivate"
                            confirmIdentity
                        />
                    ))
                }
            >
                <div className="grid gap-4 lg:grid-cols-3">
                    <Section title="Account" className="lg:col-span-2">
                        <DetailList
                            items={[
                                { label: 'Mobile', value: user.phone },
                                { label: 'Email', value: user.email },
                                { label: 'Joined', value: formatDate(user.created_at) },
                                ...(user.is_admin
                                    ? [
                                          {
                                              label: 'Two-factor',
                                              value: user.two_factor_enabled
                                                  ? 'On'
                                                  : 'Not set up yet',
                                          },
                                      ]
                                    : []),
                                ...(user.rider
                                    ? [
                                          {
                                              label: 'Rider record',
                                              value: (
                                                  <Link
                                                      href={admin.riders.show.url(user.rider.id)}
                                                      className="hover:underline"
                                                  >
                                                      <StatusBadge status={user.rider.status} />
                                                  </Link>
                                              ),
                                          },
                                      ]
                                    : []),
                            ]}
                        />
                    </Section>
                    <Section title="Roles">
                        <ul className="space-y-2 text-sm">
                            {user.roles.map((grant, index) => (
                                <li key={index} className="flex flex-wrap items-center gap-1.5">
                                    <span className="rounded-md bg-muted px-1.5 py-0.5 font-medium">
                                        {humanize(grant.role)}
                                    </span>
                                    {grant.store && (
                                        <Link
                                            href={admin.stores.show.url(grant.store.id)}
                                            className="hover:underline"
                                        >
                                            {grant.store.name}
                                        </Link>
                                    )}
                                    {grant.province && !grant.store && (
                                        <Link
                                            href={admin.franchises.show.url(grant.province.id)}
                                            className="hover:underline"
                                        >
                                            {grant.province.name}
                                        </Link>
                                    )}
                                </li>
                            ))}
                        </ul>
                    </Section>
                </div>
                <Section title="Recent account changes" description="From the audit log.">
                    {user.history.length === 0 ? (
                        <p className="text-sm text-muted-foreground">Nothing recorded yet.</p>
                    ) : (
                        <ul className="divide-y text-sm">
                            {user.history.map((entry, index) => (
                                <li
                                    key={index}
                                    className="flex flex-wrap justify-between gap-2 py-2"
                                >
                                    <span>{humanize(entry.action)}</span>
                                    <span className="text-muted-foreground">
                                        {formatDateTime(entry.at)}
                                    </span>
                                </li>
                            ))}
                        </ul>
                    )}
                </Section>
            </Page>
        </>
    );
}
