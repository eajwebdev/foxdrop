import { ActionDialog } from '@/components/admin/action-dialog';
import { Page, Section } from '@/components/admin/page';
import { SettingsTabs } from '@/components/admin/settings-tabs';
import InputError from '@/components/input-error';
import PasswordInput from '@/components/password-input';
import TwoFactorRecoveryCodes from '@/components/two-factor-recovery-codes';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Spinner } from '@/components/ui/spinner';
import { useTwoFactorAuth } from '@/hooks/use-two-factor-auth';
import { formatDateTime } from '@/lib/format';
import admin from '@/routes/admin';
import { Form, Head } from '@inertiajs/react';
import { LogOut, Monitor } from 'lucide-react';

type Session = { current: boolean; ip: string | null; user_agent: string; last_active_at: string };

export default function Security({
    sessions,
    passwordRules,
}: {
    sessions: Session[];
    passwordRules: string;
}) {
    const { recoveryCodesList, fetchRecoveryCodes, errors } = useTwoFactorAuth();
    const others = sessions.filter((session) => !session.current).length;

    return (
        <>
            <Head title="My security" />
            <Page
                title="Settings"
                description="Your password, authenticator and signed-in devices."
            >
                <SettingsTabs />
                <div className="grid gap-4 lg:grid-cols-2">
                    <Section
                        title="Password"
                        description="At least 12 characters with upper and lower case letters and a number."
                    >
                        <Form
                            {...admin.settings.password.form()}
                            options={{ preserveScroll: true }}
                            resetOnSuccess
                            resetOnError
                            className="space-y-4"
                        >
                            {({ processing, errors: formErrors }) => (
                                <>
                                    <div className="grid gap-1.5">
                                        <Label htmlFor="current_password">Current password</Label>
                                        <PasswordInput
                                            id="current_password"
                                            name="current_password"
                                            autoComplete="current-password"
                                        />
                                        <InputError message={formErrors.current_password} />
                                    </div>
                                    <div className="grid gap-1.5">
                                        <Label htmlFor="password">New password</Label>
                                        <PasswordInput
                                            id="password"
                                            name="password"
                                            autoComplete="new-password"
                                            passwordrules={passwordRules}
                                        />
                                        <InputError message={formErrors.password} />
                                    </div>
                                    <div className="grid gap-1.5">
                                        <Label htmlFor="password_confirmation">
                                            Repeat new password
                                        </Label>
                                        <PasswordInput
                                            id="password_confirmation"
                                            name="password_confirmation"
                                            autoComplete="new-password"
                                        />
                                    </div>
                                    <Button disabled={processing}>
                                        {processing && <Spinner />}
                                        Change password
                                    </Button>
                                </>
                            )}
                        </Form>
                    </Section>

                    <div className="space-y-4">
                        <Section
                            title="Two-factor authentication"
                            description="Required for every admin; it cannot be turned off."
                        >
                            <TwoFactorRecoveryCodes
                                recoveryCodesList={recoveryCodesList}
                                fetchRecoveryCodes={fetchRecoveryCodes}
                                errors={errors}
                            />
                            <p className="mt-3 text-xs text-muted-foreground">
                                Lost your phone? Sign in with a recovery code. If you have none
                                left, the super admin can reset your authenticator.
                            </p>
                        </Section>

                        <Section
                            title="Signed-in devices"
                            actions={
                                others > 0 && (
                                    <ActionDialog
                                        trigger={
                                            <Button variant="outline" size="sm">
                                                <LogOut /> Sign out other devices
                                            </Button>
                                        }
                                        title="Sign out your other devices?"
                                        description="Every other browser signed in to your admin account is signed out now."
                                        action={admin.settings.sessions.destroy.form()}
                                        submitLabel="Sign them out"
                                        confirmIdentity
                                    />
                                )
                            }
                        >
                            <ul className="divide-y text-sm">
                                {sessions.map((session, index) => (
                                    <li key={index} className="flex items-start gap-3 py-2.5">
                                        <Monitor className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
                                        <div className="min-w-0 flex-1">
                                            <p className="truncate">
                                                {session.user_agent || 'Unknown browser'}
                                            </p>
                                            <p className="text-xs text-muted-foreground">
                                                {session.ip ?? 'Unknown IP'} ·{' '}
                                                {session.current
                                                    ? 'This device'
                                                    : `Active ${formatDateTime(session.last_active_at)}`}
                                            </p>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        </Section>
                    </div>
                </div>
            </Page>
        </>
    );
}

Security.layout = {
    breadcrumbs: [{ title: 'Settings', href: admin.settings.security.url() }],
};
