import { ActionDialog } from '@/components/admin/action-dialog';
import { Page, Section } from '@/components/admin/page';
import { SettingsTabs } from '@/components/admin/settings-tabs';
import { StatusBadge } from '@/components/admin/status-badge';
import InputError from '@/components/input-error';
import PasswordInput from '@/components/password-input';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { formatDateTime } from '@/lib/format';
import admin from '@/routes/admin';
import { Head, usePage } from '@inertiajs/react';
import { KeyRound, ShieldOff, UserCog, UserMinus, UserPlus } from 'lucide-react';

type Permission = { value: string; label: string };
type Staff = {
    id: string;
    full_name: string | null;
    email: string | null;
    status: string;
    is_super_admin: boolean;
    permissions: string[];
    two_factor_enabled: boolean;
    last_sign_in_at: string | null;
};

export default function StaffPage({
    staff,
    permissions,
    passwordRules,
}: {
    staff: Staff[];
    permissions: Permission[];
    passwordRules: string;
}) {
    const me = usePage().props.auth.user;

    return (
        <>
            <Head title="Admin staff" />
            <Page
                title="Settings"
                description="Admin accounts and what each one may do. Only the super admin sees this page."
                actions={
                    <CreateStaffDialog permissions={permissions} passwordRules={passwordRules} />
                }
            >
                <SettingsTabs />
                <Section>
                    <ul className="divide-y">
                        {staff.map((person) => (
                            <li
                                key={person.id}
                                className="flex flex-col gap-3 py-4 first:pt-0 last:pb-0 md:flex-row md:items-center"
                            >
                                <div className="min-w-0 flex-1 space-y-1">
                                    <p className="flex flex-wrap items-center gap-2 font-medium">
                                        {person.full_name ?? person.email}
                                        {person.is_super_admin && (
                                            <StatusBadge status="AVAILABLE" label="Super admin" />
                                        )}
                                        {person.status !== 'ACTIVE' && (
                                            <StatusBadge status={person.status} />
                                        )}
                                        {!person.two_factor_enabled && (
                                            <StatusBadge status="PENDING" label="2FA not set up" />
                                        )}
                                    </p>
                                    <p className="text-sm text-muted-foreground">
                                        {person.email} · last sign-in{' '}
                                        {formatDateTime(person.last_sign_in_at)}
                                    </p>
                                    {!person.is_super_admin && (
                                        <p className="text-xs text-muted-foreground">
                                            {person.permissions.length === 0
                                                ? 'No permissions yet'
                                                : permissions
                                                      .filter((permission) =>
                                                          person.permissions.includes(
                                                              permission.value,
                                                          ),
                                                      )
                                                      .map((permission) => permission.label)
                                                      .join(' · ')}
                                        </p>
                                    )}
                                </div>
                                {!person.is_super_admin && person.id !== me?.id && (
                                    <div className="flex flex-wrap gap-2">
                                        <ActionDialog
                                            trigger={
                                                <Button variant="outline" size="sm">
                                                    <UserCog /> Permissions
                                                </Button>
                                            }
                                            title={`What may ${person.full_name ?? 'they'} do?`}
                                            description="Changes apply on their next click."
                                            action={admin.staff.update.form.put(person.id)}
                                            submitLabel="Save permissions"
                                            confirmIdentity
                                            fields={(errors) => (
                                                <PermissionChecklist
                                                    permissions={permissions}
                                                    checked={person.permissions}
                                                    error={errors.permissions}
                                                />
                                            )}
                                        />
                                        <ActionDialog
                                            trigger={
                                                <Button variant="outline" size="sm">
                                                    <KeyRound /> Reset password
                                                </Button>
                                            }
                                            title="Set a new password"
                                            description="Their other sessions are signed out. Tell them the new password in person or by phone."
                                            action={admin.staff.password.form.put(person.id)}
                                            submitLabel="Reset password"
                                            confirmIdentity
                                            fields={(errors) => (
                                                <NewPasswordFields
                                                    passwordRules={passwordRules}
                                                    error={errors.password}
                                                />
                                            )}
                                        />
                                        {person.two_factor_enabled && (
                                            <ActionDialog
                                                trigger={
                                                    <Button variant="outline" size="sm">
                                                        <ShieldOff /> Reset 2FA
                                                    </Button>
                                                }
                                                title="Reset their authenticator?"
                                                description="Use this when they lost their phone and their recovery codes. They set up 2FA again at their next sign-in."
                                                action={admin.staff.twoFactor.form.delete(
                                                    person.id,
                                                )}
                                                submitLabel="Reset 2FA"
                                                destructive
                                                confirmIdentity
                                            />
                                        )}
                                        <ActionDialog
                                            trigger={
                                                <Button
                                                    variant="outline"
                                                    size="sm"
                                                    className="text-destructive"
                                                >
                                                    <UserMinus /> Remove
                                                </Button>
                                            }
                                            title="Remove admin access?"
                                            description="Their account stays (it may be a customer account too) but it can no longer open the admin."
                                            action={admin.staff.destroy.form.delete(person.id)}
                                            submitLabel="Remove access"
                                            destructive
                                            confirmIdentity
                                        />
                                    </div>
                                )}
                            </li>
                        ))}
                    </ul>
                </Section>
            </Page>
        </>
    );
}

function CreateStaffDialog({
    permissions,
    passwordRules,
}: {
    permissions: Permission[];
    passwordRules: string;
}) {
    return (
        <ActionDialog
            trigger={
                <Button>
                    <UserPlus /> Add staff
                </Button>
            }
            title="Add an admin staff account"
            description="They sign in with this email and password, then set up two-factor authentication."
            action={admin.staff.store.form()}
            submitLabel="Create account"
            confirmIdentity
            fields={(errors) => (
                <>
                    <div className="grid gap-1.5">
                        <Label htmlFor="full_name">Full name</Label>
                        <Input
                            id="full_name"
                            name="full_name"
                            required
                            minLength={2}
                            maxLength={120}
                        />
                        <InputError message={errors.full_name} />
                    </div>
                    <div className="grid gap-1.5">
                        <Label htmlFor="email">Work email</Label>
                        <Input id="email" name="email" type="email" required />
                        <InputError message={errors.email} />
                    </div>
                    <NewPasswordFields passwordRules={passwordRules} error={errors.password} />
                    <PermissionChecklist
                        permissions={permissions}
                        checked={[]}
                        error={errors.permissions}
                    />
                </>
            )}
        />
    );
}

function NewPasswordFields({ passwordRules, error }: { passwordRules: string; error?: string }) {
    return (
        <>
            <div className="grid gap-1.5">
                <Label htmlFor="password">Password</Label>
                <PasswordInput
                    id="password"
                    name="password"
                    autoComplete="new-password"
                    passwordrules={passwordRules}
                    required
                />
                <InputError message={error} />
            </div>
            <div className="grid gap-1.5">
                <Label htmlFor="password_confirmation">Repeat password</Label>
                <PasswordInput
                    id="password_confirmation"
                    name="password_confirmation"
                    autoComplete="new-password"
                    required
                />
            </div>
        </>
    );
}

function PermissionChecklist({
    permissions,
    checked,
    error,
}: {
    permissions: Permission[];
    checked: string[];
    error?: string;
}) {
    return (
        <fieldset className="space-y-2">
            <legend className="mb-1 text-sm font-medium">Permissions</legend>
            {permissions.map((permission) => (
                <label key={permission.value} className="flex items-start gap-2 text-sm">
                    <Checkbox
                        name="permissions[]"
                        value={permission.value}
                        defaultChecked={checked.includes(permission.value)}
                    />
                    <span>{permission.label}</span>
                </label>
            ))}
            <InputError message={error} />
        </fieldset>
    );
}

StaffPage.layout = {
    breadcrumbs: [
        { title: 'Settings', href: admin.settings.security.url() },
        { title: 'Admin staff', href: admin.staff.index.url() },
    ],
};
