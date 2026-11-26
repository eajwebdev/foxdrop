import AlertError from '@/components/alert-error';
import InputError from '@/components/input-error';
import { Button } from '@/components/ui/button';
import { InputOTP, InputOTPGroup, InputOTPSlot } from '@/components/ui/input-otp';
import { Skeleton } from '@/components/ui/skeleton';
import { Spinner } from '@/components/ui/spinner';
import { useClipboard } from '@/hooks/use-clipboard';
import { OTP_MAX_LENGTH, useTwoFactorAuth } from '@/hooks/use-two-factor-auth';
import { logout } from '@/routes';
import admin from '@/routes/admin';
import { confirm, enable } from '@/routes/two-factor';
import { Form, Head, Link } from '@inertiajs/react';
import { REGEXP_ONLY_DIGITS } from 'input-otp';
import { Check, Copy, KeyRound, ShieldCheck } from 'lucide-react';
import { useEffect, useState } from 'react';

type Props = { pending: boolean; enabled: boolean };

/**
 * Required 2FA (DECISIONS 74): the only page an admin sees until an authenticator app is confirmed.
 * Step 1 start → step 2 scan + confirm a code → step 3 save the recovery codes.
 */
export default function TwoFactorSetup({ pending, enabled }: Props) {
    const auth = useTwoFactorAuth();
    const { fetchSetupData, fetchRecoveryCodes } = auth;
    const [started, setStarted] = useState(pending);

    useEffect(() => {
        if (enabled) {
            void fetchRecoveryCodes();
        } else if (started) {
            void fetchSetupData();
        }
    }, [enabled, started, fetchSetupData, fetchRecoveryCodes]);

    return (
        <>
            <Head title="Set up two-factor authentication" />

            {enabled ? (
                <RecoveryCodesStep codes={auth.recoveryCodesList} errors={auth.errors} />
            ) : started ? (
                <ScanStep
                    qrCodeSvg={auth.qrCodeSvg}
                    setupKey={auth.manualSetupKey}
                    errors={auth.errors}
                />
            ) : (
                <div className="space-y-4">
                    <ol className="space-y-2 text-sm text-muted-foreground">
                        <li>
                            1. Install an authenticator app (Google Authenticator, Microsoft
                            Authenticator, 1Password…).
                        </li>
                        <li>2. Scan the QR code we show you.</li>
                        <li>3. Type the 6-digit code it gives you.</li>
                    </ol>
                    <Form {...enable.form()} onSuccess={() => setStarted(true)}>
                        {({ processing }) => (
                            <Button className="w-full" disabled={processing} data-test="start-2fa">
                                {processing ? <Spinner /> : <ShieldCheck />}
                                Set up authenticator
                            </Button>
                        )}
                    </Form>
                </div>
            )}

            <div className="text-center">
                <Link
                    href={logout()}
                    as="button"
                    className="text-sm text-muted-foreground underline-offset-4 hover:underline"
                >
                    Sign out
                </Link>
            </div>
        </>
    );
}

function ScanStep({
    qrCodeSvg,
    setupKey,
    errors,
}: {
    qrCodeSvg: string | null;
    setupKey: string | null;
    errors: string[];
}) {
    const [code, setCode] = useState('');
    const [copied, copy] = useClipboard();

    if (errors.length > 0) {
        return <AlertError errors={errors} />;
    }

    return (
        <div className="space-y-5">
            <div className="mx-auto aspect-square w-56 rounded-xl border bg-white p-3">
                {qrCodeSvg ? (
                    <div
                        className="size-full [&_svg]:size-full"
                        data-test="2fa-qr"
                        dangerouslySetInnerHTML={{ __html: qrCodeSvg }}
                    />
                ) : (
                    <Skeleton className="size-full" />
                )}
            </div>
            <div className="flex items-center gap-2 rounded-lg border bg-muted/50 p-2 text-sm">
                <KeyRound className="size-4 shrink-0 text-muted-foreground" />
                <code className="flex-1 truncate font-mono" data-test="2fa-secret">
                    {setupKey ?? '…'}
                </code>
                {setupKey && (
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        aria-label="Copy setup key"
                        onClick={() => copy(setupKey)}
                    >
                        {copied === setupKey ? <Check /> : <Copy />}
                    </Button>
                )}
            </div>
            <Form {...confirm.form()} resetOnError className="space-y-3">
                {({ processing, errors: formErrors }) => (
                    <>
                        <div className="flex flex-col items-center gap-2">
                            <InputOTP
                                name="code"
                                maxLength={OTP_MAX_LENGTH}
                                pattern={REGEXP_ONLY_DIGITS}
                                onChange={setCode}
                                autoFocus
                            >
                                <InputOTPGroup>
                                    {Array.from({ length: OTP_MAX_LENGTH }, (_, index) => (
                                        <InputOTPSlot key={index} index={index} />
                                    ))}
                                </InputOTPGroup>
                            </InputOTP>
                            <InputError
                                message={
                                    (formErrors as Record<string, string>)[
                                        'confirmTwoFactorAuthentication.code'
                                    ] ?? formErrors.code
                                }
                            />
                        </div>
                        <Button
                            className="w-full"
                            disabled={processing || code.length < OTP_MAX_LENGTH}
                            data-test="confirm-2fa"
                        >
                            {processing && <Spinner />}
                            Confirm code
                        </Button>
                    </>
                )}
            </Form>
        </div>
    );
}

function RecoveryCodesStep({ codes, errors }: { codes: string[]; errors: string[] }) {
    const [copied, copy] = useClipboard();

    return (
        <div className="space-y-4">
            <p className="text-sm text-muted-foreground">
                Two-factor authentication is on. Save these recovery codes somewhere safe: each one
                signs you in once if you lose your phone.
            </p>
            {errors.length > 0 ? (
                <AlertError errors={errors} />
            ) : (
                <div
                    className="grid grid-cols-2 gap-1 rounded-lg border bg-muted/50 p-3 font-mono text-sm"
                    data-test="recovery-codes"
                >
                    {codes.length === 0
                        ? Array.from({ length: 8 }, (_, index) => (
                              <Skeleton key={index} className="h-5" />
                          ))
                        : codes.map((code) => <span key={code}>{code}</span>)}
                </div>
            )}
            <div className="flex gap-2">
                <Button
                    type="button"
                    variant="outline"
                    className="flex-1"
                    onClick={() => copy(codes.join('\n'))}
                    disabled={codes.length === 0}
                >
                    {copied ? <Check /> : <Copy />} Copy codes
                </Button>
                <Button asChild className="flex-1" data-test="continue-dashboard">
                    <Link href={admin.dashboard()}>Continue</Link>
                </Button>
            </div>
        </div>
    );
}

TwoFactorSetup.layout = {
    title: 'Protect your account',
    description: 'Admin accounts need two-factor authentication before anything else.',
};
