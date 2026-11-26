import InputError from '@/components/input-error';
import PasswordInput from '@/components/password-input';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { usePage } from '@inertiajs/react';
import { ShieldCheck } from 'lucide-react';

type Errors = Partial<Record<'confirm_password' | 'confirm_code', string>>;

/**
 * "Confirm it's you" (DECISIONS 75): password + authenticator code. Dangerous-action dialogs show these only when the
 * last confirmation is older than 15 minutes; the server checks either way.
 */
export function useNeedsConfirmation(): boolean {
    const until = usePage().props.auth.confirmedUntil;

    return until === null || until * 1000 <= Date.now();
}

/** The two fields themselves (also the whole form on the "Confirm it's you" page). */
export function IdentityFields({
    errors,
    needsCode = true,
    autoFocus = false,
}: {
    errors: Errors;
    needsCode?: boolean;
    autoFocus?: boolean;
}) {
    return (
        <>
            <div className="grid gap-1.5">
                <Label htmlFor="confirm_password">Your password</Label>
                <PasswordInput
                    id="confirm_password"
                    name="confirm_password"
                    autoComplete="current-password"
                    autoFocus={autoFocus}
                    required
                />
                <InputError message={errors.confirm_password} />
            </div>
            {needsCode && (
                <div className="grid gap-1.5">
                    <Label htmlFor="confirm_code">Authenticator code</Label>
                    <Input
                        id="confirm_code"
                        name="confirm_code"
                        inputMode="numeric"
                        autoComplete="one-time-code"
                        pattern="[0-9]{6}"
                        maxLength={6}
                        placeholder="6-digit code"
                        required
                    />
                    <InputError message={errors.confirm_code} />
                </div>
            )}
        </>
    );
}

/** Inside a dangerous action's dialog: only when a fresh confirmation is needed. */
export function ConfirmIdentityFields({ errors }: { errors: Errors }) {
    const needed = useNeedsConfirmation();
    // The server decides: if it asked for a confirmation (e.g. this browser's clock is behind), show the fields.
    if (!needed && !errors.confirm_password && !errors.confirm_code) {
        return null;
    }

    return (
        <fieldset className="space-y-3 rounded-lg border border-dashed p-3">
            <legend className="flex items-center gap-1.5 px-1 text-xs font-medium text-muted-foreground">
                <ShieldCheck className="size-3.5" /> Confirm it's you
            </legend>
            <IdentityFields errors={errors} />
        </fieldset>
    );
}
