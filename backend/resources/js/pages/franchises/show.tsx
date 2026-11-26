import { ActionDialog } from '@/components/admin/action-dialog';
import { DetailList, EmptyState, Page, Section } from '@/components/admin/page';
import { StatusBadge } from '@/components/admin/status-badge';
import InputError from '@/components/input-error';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {
    daysUntil,
    formatDate,
    formatDateTime,
    formatNumber,
    formatPeso,
    humanize,
} from '@/lib/format';
import { useCan } from '@/lib/permissions';
import admin from '@/routes/admin';
import { Head, Link, setLayoutProps } from '@inertiajs/react';
import { Ban, Banknote, CircleCheck, FileCheck2, UserPlus } from 'lucide-react';

type Person = { id: string; full_name: string | null; email: string | null; phone: string | null };
type License = {
    id: string;
    status: string;
    status_reason: string | null;
    start_at: string | null;
    end_at: string | null;
    fee_centavos: number;
    application_id: string | null;
    operator: Person | null;
    payments: {
        id: string;
        provider: string;
        reference: string | null;
        amount_centavos: number;
        paid_at: string | null;
    }[];
};
type Province = {
    id: string;
    name: string;
    region_name: string;
    is_enabled: boolean;
    is_serviceable: boolean;
    is_available: boolean;
    current_license_id: string | null;
    license_fee_centavos: number;
    counts: { active_stores: number; active_riders: number };
    licenses: License[];
};

const PAYABLE = ['PENDING_PAYMENT', 'ACTIVE', 'GRACE'];
const OPERATING = ['ACTIVE', 'GRACE'];

export default function ProvinceShow({ province }: { province: Province }) {
    const can = useCan();
    const current =
        province.licenses.find((license) => license.id === province.current_license_id) ?? null;
    const history = province.licenses.filter((license) => license.id !== current?.id);
    const manage = can('franchises.manage');
    setLayoutProps({
        breadcrumbs: [
            { title: 'Franchises', href: admin.franchises.index.url() },
            { title: province.name, href: admin.franchises.show.url(province.id) },
        ],
    });

    return (
        <>
            <Head title={province.name} />
            <Page
                title={province.name}
                description={province.region_name}
                actions={
                    manage && (
                        <>
                            {province.is_available && province.is_enabled && (
                                <AssignDialog province={province} />
                            )}
                            {current && PAYABLE.includes(current.status) && (
                                <PaymentDialog
                                    license={current}
                                    fee={province.license_fee_centavos}
                                />
                            )}
                            {current && OPERATING.includes(current.status) && (
                                <ActionDialog
                                    trigger={
                                        <Button variant="outline" className="text-destructive">
                                            <Ban /> Suspend
                                        </Button>
                                    }
                                    title={`Suspend ${province.name}?`}
                                    description="Customers stop seeing the province and the operator becomes read-only. Nobody else can take the province while it is suspended."
                                    action={admin.licenses.suspend.form(current.id)}
                                    submitLabel="Suspend license"
                                    destructive
                                    reason="Reason (the operator sees this)"
                                    confirmIdentity
                                />
                            )}
                            {current?.status === 'SUSPENDED' && (
                                <ActionDialog
                                    trigger={
                                        <Button variant="outline">
                                            <CircleCheck /> Lift suspension
                                        </Button>
                                    }
                                    title="Lift the suspension?"
                                    description="The license returns to Active, or to Grace / Expired if its term ran out meanwhile."
                                    action={admin.licenses.reinstate.form(current.id)}
                                    submitLabel="Lift suspension"
                                    confirmIdentity
                                />
                            )}
                        </>
                    )
                }
            >
                <div className="grid gap-4 lg:grid-cols-3">
                    <Section title="Current license" className="lg:col-span-2">
                        {current ? (
                            <LicenseDetails license={current} />
                        ) : (
                            <EmptyState
                                icon={UserPlus}
                                title="No operator yet"
                                description={
                                    province.is_enabled
                                        ? 'Anyone can apply for this province in the FoxDrop Business app, or you can assign it.'
                                        : 'This province is switched off for FoxDrop.'
                                }
                            />
                        )}
                    </Section>
                    <Section title="Province">
                        <DetailList
                            items={[
                                {
                                    label: 'Customers',
                                    value: province.is_serviceable ? (
                                        <StatusBadge status="ACTIVE" label="Can order" />
                                    ) : (
                                        <StatusBadge status="CLOSED" label="Not served" />
                                    ),
                                },
                                {
                                    label: 'Active stores',
                                    value: formatNumber(province.counts.active_stores),
                                },
                                {
                                    label: 'Active riders',
                                    value: formatNumber(province.counts.active_riders),
                                },
                                {
                                    label: 'License fee',
                                    value: `${formatPeso(province.license_fee_centavos)} / year`,
                                },
                            ]}
                        />
                    </Section>
                </div>

                <Section
                    title="History"
                    description="Earlier licenses and applications for this province."
                >
                    {history.length === 0 ? (
                        <p className="text-sm text-muted-foreground">No earlier licenses.</p>
                    ) : (
                        <ul className="divide-y">
                            {history.map((license) => (
                                <li
                                    key={license.id}
                                    className="flex flex-wrap items-center gap-3 py-3 text-sm"
                                >
                                    <StatusBadge status={license.status} />
                                    <span className="font-medium">
                                        {license.operator?.full_name ?? 'Unknown'}
                                    </span>
                                    <span className="text-muted-foreground">
                                        {formatDate(license.start_at)} –{' '}
                                        {formatDate(license.end_at)}
                                    </span>
                                    {license.status_reason && (
                                        <span className="text-muted-foreground">
                                            · {license.status_reason}
                                        </span>
                                    )}
                                </li>
                            ))}
                        </ul>
                    )}
                </Section>
            </Page>
        </>
    );
}

function LicenseDetails({ license }: { license: License }) {
    const days = daysUntil(license.end_at);

    return (
        <div className="space-y-5">
            <DetailList
                items={[
                    { label: 'Status', value: <StatusBadge status={license.status} /> },
                    {
                        label: 'Operator',
                        value: license.operator ? (
                            <Link
                                href={admin.users.show.url(license.operator.id)}
                                className="font-medium hover:underline"
                            >
                                {license.operator.full_name ??
                                    license.operator.email ??
                                    license.operator.phone}
                            </Link>
                        ) : (
                            '—'
                        ),
                    },
                    { label: 'Started', value: formatDate(license.start_at) },
                    {
                        label: 'Ends',
                        value: license.end_at
                            ? `${formatDate(license.end_at)}${days !== null ? ` (${days} days)` : ''}`
                            : '—',
                    },
                    ...(license.status_reason
                        ? [{ label: 'Reason', value: license.status_reason }]
                        : []),
                    ...(license.application_id
                        ? [
                              {
                                  label: 'Application',
                                  value: (
                                      <Link
                                          href={admin.applications.show.url(license.application_id)}
                                          className="inline-flex items-center gap-1 hover:underline"
                                      >
                                          <FileCheck2 className="size-4" /> View application
                                      </Link>
                                  ),
                              },
                          ]
                        : []),
                ]}
            />
            <div>
                <h3 className="mb-2 text-sm font-medium">Payments</h3>
                {license.payments.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No payment yet.</p>
                ) : (
                    <ul className="divide-y rounded-lg border">
                        {license.payments.map((payment) => (
                            <li
                                key={payment.id}
                                className="flex flex-wrap items-center justify-between gap-2 px-3 py-2 text-sm"
                            >
                                <span className="font-medium tabular-nums">
                                    {formatPeso(payment.amount_centavos)}
                                </span>
                                <span className="text-muted-foreground">
                                    {payment.provider === 'manual'
                                        ? `Offline · ${payment.reference}`
                                        : humanize(payment.provider)}
                                </span>
                                <span className="text-muted-foreground">
                                    {formatDateTime(payment.paid_at)}
                                </span>
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </div>
    );
}

function AssignDialog({ province }: { province: Province }) {
    return (
        <ActionDialog
            trigger={
                <Button>
                    <UserPlus /> Assign operator
                </Button>
            }
            title={`Assign ${province.name}`}
            description="The license waits for payment: the operator pays in the app, or you record an offline payment. The account must already exist."
            action={admin.licenses.assign.form(province.id)}
            submitLabel="Assign province"
            confirmIdentity
            fields={(errors) => (
                <div className="grid gap-1.5">
                    <Label htmlFor="operator">Operator's email or mobile number</Label>
                    <Input
                        id="operator"
                        name="operator"
                        required
                        placeholder="0917 123 4567 or name@email.com"
                    />
                    <InputError message={errors.operator} />
                </div>
            )}
        />
    );
}

function PaymentDialog({ license, fee }: { license: License; fee: number }) {
    const renewal = license.status !== 'PENDING_PAYMENT';

    return (
        <ActionDialog
            trigger={
                <Button variant={renewal ? 'outline' : 'default'}>
                    <Banknote /> {renewal ? 'Record renewal payment' : 'Record payment'}
                </Button>
            }
            title={renewal ? 'Record a renewal payment' : 'Record the license payment'}
            description={`For ${formatPeso(fee)} received outside the app (bank transfer or cash). ${
                renewal
                    ? 'The license is extended by one year.'
                    : 'The license becomes active for one year.'
            }`}
            action={admin.licenses.payments.store.form(license.id)}
            submitLabel="Record payment"
            confirmIdentity
            fields={(errors) => (
                <>
                    <div className="grid gap-1.5">
                        <Label htmlFor="method">Paid by</Label>
                        <Select name="method" defaultValue="BANK_TRANSFER">
                            <SelectTrigger id="method">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="BANK_TRANSFER">Bank transfer</SelectItem>
                                <SelectItem value="CASH">Cash</SelectItem>
                                <SelectItem value="OTHER">Other</SelectItem>
                            </SelectContent>
                        </Select>
                        <InputError message={errors.method} />
                    </div>
                    <div className="grid gap-1.5">
                        <Label htmlFor="reference">Bank or receipt reference</Label>
                        <Input
                            id="reference"
                            name="reference"
                            required
                            minLength={3}
                            maxLength={60}
                            placeholder="e.g. BDO-20261126-0042"
                        />
                        <InputError message={errors.reference} />
                    </div>
                    <div className="grid gap-1.5">
                        <Label htmlFor="note">Note (optional)</Label>
                        <Input id="note" name="note" maxLength={300} />
                        <InputError message={errors.note} />
                    </div>
                </>
            )}
        />
    );
}
