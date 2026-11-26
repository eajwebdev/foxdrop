import { DocumentGallery } from '@/components/admin/document-gallery';
import { DetailList, Page, Section } from '@/components/admin/page';
import { PartnerStatusActions } from '@/components/admin/partner-status-actions';
import { ReviewActions } from '@/components/admin/review-actions';
import { StatusBadge } from '@/components/admin/status-badge';
import { formatDateTime, humanize } from '@/lib/format';
import { useCan } from '@/lib/permissions';
import admin from '@/routes/admin';
import { Head, Link, setLayoutProps } from '@inertiajs/react';
import { MapPin } from 'lucide-react';

type StoreDetail = {
    id: string;
    name: string;
    category: string;
    status: string;
    description: string | null;
    address_line: string;
    barangay: string | null;
    city: string;
    phone: string | null;
    location: { lat: number; lng: number } | null;
    rejection_reason: string | null;
    status_reason: string | null;
    province: { id: string; name: string | null };
    owner: {
        id: string;
        full_name: string | null;
        email: string | null;
        phone: string | null;
    } | null;
    created_at: string | null;
    documents: { type: string; url: string; status: string }[];
};

export default function StoreShow({ store }: { store: StoreDetail }) {
    const can = useCan();
    setLayoutProps({
        breadcrumbs: [
            { title: 'Stores', href: admin.stores.index.url() },
            { title: store.name, href: admin.stores.show.url(store.id) },
        ],
    });
    const statusAction = admin.stores.status.form(store.id);
    const live = store.status === 'ACTIVE' || store.status === 'PAUSED';

    return (
        <>
            <Head title={store.name} />
            <Page
                title={store.name}
                description={
                    <span className="inline-flex flex-wrap items-center gap-2">
                        {humanize(store.category)} · {store.city}, {store.province.name}
                        <StatusBadge status={store.status} />
                    </span>
                }
                actions={
                    can('partners.manage') && (
                        <>
                            {store.status === 'SUBMITTED' && (
                                <ReviewActions
                                    subject="this store"
                                    approve={admin.stores.approve.form(store.id)}
                                    reject={admin.stores.reject.form(store.id)}
                                    approveLabel="Approve store"
                                    approveHint="The store goes live for customers right away"
                                />
                            )}
                            <PartnerStatusActions
                                action={statusAction}
                                changes={[
                                    ...(live
                                        ? [
                                              {
                                                  to: 'SUSPENDED',
                                                  label: 'Suspend',
                                                  title: `Suspend ${store.name}?`,
                                                  description:
                                                      'Customers stop seeing the store until it is reinstated.',
                                                  destructive: true,
                                              },
                                          ]
                                        : []),
                                    ...(store.status === 'SUSPENDED'
                                        ? [
                                              {
                                                  to: 'ACTIVE',
                                                  label: 'Reinstate',
                                                  title: `Reinstate ${store.name}?`,
                                                  description:
                                                      'The store is live for customers again.',
                                                  reason: false,
                                              },
                                          ]
                                        : []),
                                    ...(live || store.status === 'SUSPENDED'
                                        ? [
                                              {
                                                  to: 'CLOSED',
                                                  label: 'Close store',
                                                  title: `Close ${store.name} for good?`,
                                                  description:
                                                      'A closed store cannot be reopened; the owner would apply again.',
                                                  destructive: true,
                                              },
                                          ]
                                        : []),
                                ]}
                            />
                        </>
                    )
                }
            >
                <div className="grid gap-4 lg:grid-cols-3">
                    <Section title="Store" className="lg:col-span-2">
                        <DetailList
                            items={[
                                {
                                    label: 'Address',
                                    value: [store.address_line, store.barangay, store.city]
                                        .filter(Boolean)
                                        .join(', '),
                                },
                                {
                                    label: 'Pin',
                                    value: store.location ? (
                                        <span className="inline-flex items-center gap-1 tabular-nums">
                                            <MapPin className="size-4 text-brand" />
                                            {store.location.lat.toFixed(5)},{' '}
                                            {store.location.lng.toFixed(5)}
                                        </span>
                                    ) : (
                                        '—'
                                    ),
                                },
                                { label: 'Store phone', value: store.phone },
                                { label: 'Applied', value: formatDateTime(store.created_at) },
                                { label: 'Description', value: store.description },
                                ...(store.rejection_reason
                                    ? [{ label: 'Rejection reason', value: store.rejection_reason }]
                                    : []),
                                ...(store.status_reason
                                    ? [{ label: 'Status reason', value: store.status_reason }]
                                    : []),
                            ]}
                        />
                    </Section>
                    <Section title="Owner">
                        {store.owner ? (
                            <DetailList
                                items={[
                                    {
                                        label: 'Name',
                                        value: (
                                            <Link
                                                href={admin.users.show.url(store.owner.id)}
                                                className="font-medium hover:underline"
                                            >
                                                {store.owner.full_name ?? 'Unnamed'}
                                            </Link>
                                        ),
                                    },
                                    { label: 'Phone', value: store.owner.phone },
                                    { label: 'Email', value: store.owner.email },
                                ]}
                            />
                        ) : (
                            <p className="text-sm text-muted-foreground">Unknown owner.</p>
                        )}
                    </Section>
                </div>
                <Section
                    title="Documents"
                    description="Business permit and valid ID. Shown only to admins; never cached by the browser."
                >
                    <DocumentGallery documents={store.documents} />
                </Section>
            </Page>
        </>
    );
}
