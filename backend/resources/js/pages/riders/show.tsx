import { DocumentGallery } from '@/components/admin/document-gallery';
import { DetailList, Page, Section } from '@/components/admin/page';
import { PartnerStatusActions } from '@/components/admin/partner-status-actions';
import { ReviewActions } from '@/components/admin/review-actions';
import { StatusBadge } from '@/components/admin/status-badge';
import { formatDate, formatDateTime, humanize } from '@/lib/format';
import { useCan } from '@/lib/permissions';
import admin from '@/routes/admin';
import { Head, Link, setLayoutProps } from '@inertiajs/react';

type RiderDetail = {
    id: string;
    status: string;
    vehicle_type: string;
    plate_no: string | null;
    license_no_last4: string | null;
    license_expiry: string | null;
    rejection_reason: string | null;
    status_reason: string | null;
    province: { id: string; name: string | null };
    person: {
        id: string;
        full_name: string | null;
        email: string | null;
        phone: string | null;
    } | null;
    created_at: string | null;
    documents: { type: string; url: string; status: string }[];
};

export default function RiderShow({ rider }: { rider: RiderDetail }) {
    const can = useCan();
    const name = rider.person?.full_name ?? 'Rider';
    setLayoutProps({
        breadcrumbs: [
            { title: 'Riders', href: admin.riders.index.url() },
            { title: name, href: admin.riders.show.url(rider.id) },
        ],
    });

    return (
        <>
            <Head title={name} />
            <Page
                title={name}
                description={
                    <span className="inline-flex flex-wrap items-center gap-2">
                        {humanize(rider.vehicle_type)} rider in {rider.province.name}
                        <StatusBadge status={rider.status} />
                    </span>
                }
                actions={
                    can('partners.manage') && (
                        <>
                            {rider.status === 'DOCS_REVIEW' && (
                                <ReviewActions
                                    subject="this rider"
                                    approve={admin.riders.approve.form(rider.id)}
                                    reject={admin.riders.reject.form(rider.id)}
                                    approveLabel="Approve rider"
                                    approveHint="The rider can go online and take jobs"
                                />
                            )}
                            <PartnerStatusActions
                                action={admin.riders.status.form(rider.id)}
                                changes={[
                                    ...(rider.status === 'APPROVED'
                                        ? [
                                              {
                                                  to: 'SUSPENDED',
                                                  label: 'Suspend',
                                                  title: `Suspend ${name}?`,
                                                  description:
                                                      'They go offline at once and get no jobs until reinstated.',
                                                  destructive: true,
                                              },
                                          ]
                                        : []),
                                    ...(rider.status === 'SUSPENDED'
                                        ? [
                                              {
                                                  to: 'APPROVED',
                                                  label: 'Reinstate',
                                                  title: `Reinstate ${name}?`,
                                                  description:
                                                      'They can go online and take jobs again.',
                                                  reason: false,
                                              },
                                          ]
                                        : []),
                                    ...(rider.status === 'APPROVED' || rider.status === 'SUSPENDED'
                                        ? [
                                              {
                                                  to: 'DEACTIVATED',
                                                  label: 'Deactivate',
                                                  title: `Deactivate ${name} for good?`,
                                                  description:
                                                      'A deactivated rider cannot ride for FoxDrop again without a new application.',
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
                    <Section title="Rider" className="lg:col-span-2">
                        <DetailList
                            items={[
                                {
                                    label: 'Account',
                                    value: rider.person ? (
                                        <Link
                                            href={admin.users.show.url(rider.person.id)}
                                            className="font-medium hover:underline"
                                        >
                                            {rider.person.full_name ?? rider.person.phone}
                                        </Link>
                                    ) : (
                                        '—'
                                    ),
                                },
                                { label: 'Phone', value: rider.person?.phone },
                                { label: 'Vehicle', value: humanize(rider.vehicle_type) },
                                { label: 'Plate', value: rider.plate_no },
                                {
                                    label: "Driver's license",
                                    value: rider.license_no_last4
                                        ? `•••• ${rider.license_no_last4}`
                                        : '—',
                                },
                                {
                                    label: 'License expires',
                                    value: formatDate(rider.license_expiry),
                                },
                                { label: 'Applied', value: formatDateTime(rider.created_at) },
                                ...(rider.rejection_reason
                                    ? [{ label: 'Rejection reason', value: rider.rejection_reason }]
                                    : []),
                                ...(rider.status_reason
                                    ? [{ label: 'Status reason', value: rider.status_reason }]
                                    : []),
                            ]}
                        />
                    </Section>
                    <Section title="Province">
                        <DetailList
                            items={[
                                {
                                    label: 'Province',
                                    value: (
                                        <Link
                                            href={admin.franchises.show.url(rider.province.id)}
                                            className="hover:underline"
                                        >
                                            {rider.province.name}
                                        </Link>
                                    ),
                                },
                            ]}
                        />
                    </Section>
                </div>
                <Section
                    title="Documents"
                    description="Valid ID, driver's license, selfie and vehicle papers. Admins only; never cached."
                >
                    <DocumentGallery documents={rider.documents} />
                </Section>
            </Page>
        </>
    );
}
