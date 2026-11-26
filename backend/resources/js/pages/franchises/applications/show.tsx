import { DocumentGallery } from '@/components/admin/document-gallery';
import { DetailList, Page, Section } from '@/components/admin/page';
import { ReviewActions } from '@/components/admin/review-actions';
import { StatusBadge } from '@/components/admin/status-badge';
import { formatDateTime } from '@/lib/format';
import { useCan } from '@/lib/permissions';
import admin from '@/routes/admin';
import type { Person } from '@/types';
import { Head, Link, setLayoutProps } from '@inertiajs/react';

type Application = {
    id: string;
    status: string;
    business_name: string;
    registration_number: string | null;
    contact_phone: string;
    contact_email: string | null;
    rejection_reason: string | null;
    province: { id: string; name: string | null };
    applicant: Person | null;
    reviewed_by: Person | null;
    reviewed_at: string | null;
    created_at: string | null;
    license: { id: string; status: string } | null;
    documents: { type: string; url: string }[];
};

export default function ApplicationShow({ application }: { application: Application }) {
    const can = useCan();
    setLayoutProps({
        breadcrumbs: [
            { title: 'Franchises', href: admin.franchises.index.url() },
            { title: 'Operator applications', href: admin.applications.index.url() },
            { title: application.business_name, href: admin.applications.show.url(application.id) },
        ],
    });

    return (
        <>
            <Head title={application.business_name} />
            <Page
                title={application.business_name}
                description={
                    <span className="inline-flex flex-wrap items-center gap-2">
                        Wants to run{' '}
                        <Link
                            href={admin.franchises.show.url(application.province.id)}
                            className="font-medium text-foreground hover:underline"
                        >
                            {application.province.name}
                        </Link>
                        <StatusBadge status={application.status} />
                    </span>
                }
                actions={
                    application.status === 'SUBMITTED' &&
                    can('franchises.manage') && (
                        <ReviewActions
                            subject="this application"
                            approve={admin.applications.approve.form(application.id)}
                            reject={admin.applications.reject.form(application.id)}
                            approveLabel="Approve"
                            approveHint="The applicant can then pay the license fee in the app"
                        />
                    )
                }
            >
                <div className="grid gap-4 lg:grid-cols-3">
                    <Section title="Applicant" className="lg:col-span-2">
                        <DetailList
                            items={[
                                {
                                    label: 'Name',
                                    value: application.applicant ? (
                                        <Link
                                            href={admin.users.show.url(application.applicant.id)}
                                            className="font-medium hover:underline"
                                        >
                                            {application.applicant.full_name ??
                                                application.applicant.phone}
                                        </Link>
                                    ) : (
                                        '—'
                                    ),
                                },
                                { label: 'Contact phone', value: application.contact_phone },
                                { label: 'Contact email', value: application.contact_email },
                                {
                                    label: 'DTI / SEC number',
                                    value: application.registration_number,
                                },
                                {
                                    label: 'Submitted',
                                    value: formatDateTime(application.created_at),
                                },
                            ]}
                        />
                    </Section>
                    <Section title="Decision">
                        <DetailList
                            items={[
                                {
                                    label: 'Status',
                                    value: <StatusBadge status={application.status} />,
                                },
                                {
                                    label: 'License',
                                    value: application.license ? (
                                        <StatusBadge status={application.license.status} />
                                    ) : (
                                        '—'
                                    ),
                                },
                                {
                                    label: 'Reviewed by',
                                    value:
                                        application.reviewed_by?.full_name ??
                                        (application.reviewed_at ? 'Server command' : '—'),
                                },
                                {
                                    label: 'Reviewed',
                                    value: formatDateTime(application.reviewed_at),
                                },
                                ...(application.rejection_reason
                                    ? [{ label: 'Reason', value: application.rejection_reason }]
                                    : []),
                            ]}
                        />
                    </Section>
                </div>
                <Section
                    title="Documents"
                    description="Shown only to admins; never cached by the browser."
                >
                    <DocumentGallery documents={application.documents} />
                </Section>
            </Page>
        </>
    );
}
